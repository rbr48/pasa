package com.izhaanintellect.pasa.security

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OtpInterceptionGuardManager.
 * Audits and polices the android.permission.BIND_NOTIFICATION_LISTENER_SERVICE privilege.
 * Banking trojans and stalkerware exploit Notification Access to silently steal 2FA OTPs,
 * banking push notifications, and private messages.
 *
 * Legitimate banking apps do NOT use Notification Listener (they use SMS Retriever API or direct SMS).
 * This manager catches rogue apps that have enabled notification interception.
 */
@Singleton
class OtpInterceptionGuardManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val telegramApi: TelegramApi
) {
    companion object {
        private const val TAG = "PASA_OtpGuard"

        /**
         * Static baseline whitelist for well-known legitimate listeners.
         * The primary defence is the FLAG_SYSTEM runtime check in [isSystemApp] which
         * automatically trusts ALL pre-installed OEM apps (Motorola, Samsung, Xiaomi,
         * OnePlus, Oppo, etc.) without needing them listed here explicitly.
         * Only user-sideloaded / Play-installed packages can ever be flagged as rogue.
         */
        val TRUSTED_SYSTEM_LISTENERS = setOf(
            // Android OS internals
            "com.android.systemui",
            "com.android.settings",
            // Google core
            "com.google.android.gms",
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.nexuslauncher",
            // Google Wear / Android Auto (may not always carry FLAG_SYSTEM on all OEM builds)
            "com.google.android.wearable.app",
            "com.google.android.apps.wear.companion",
            "com.google.android.projection.gearhead",
            // Motorola OEM system services
            "com.motorola.uxcore",
            "com.motorola.launcher3",
            "com.motorola.mobiledesktop",
            "com.motorola.motolights",
            "com.motorola.systemui",
            "com.motorola.ccc",
            // Samsung
            "com.samsung.android.app.watchmanager",
            "com.samsung.android.app.galaxywatch",
            // Xiaomi / MIUI
            "com.miui.notification",
            "com.xiaomi.wearable",
            "com.xiaomi.smarthome",
            // Huawei
            "com.huawei.health",
            "com.huawei.intelligent",
            // OnePlus / Oppo / ColorOS
            "com.oneplus.launcher",
            "com.oppo.launcher",
            "com.coloros.notificationmanager",
            // Fitness trackers / Wearables
            "com.garmin.android.apps.connectmobile",
            "com.fitbit.FitbitMobile",
            "com.samsung.android.app.galaxybuds",
        )

        /**
         * Returns true if [pkg] is a pre-installed system app.
         * FLAG_SYSTEM  → shipped in the vendor/system partition.
         * FLAG_UPDATED_SYSTEM_APP → was a system app, later updated via Play Store.
         * Both are unconditionally trusted — a factory-installed OEM app is legitimate
         * regardless of whether it received a Play update.
         */
        fun isSystemApp(context: Context, pkg: String): Boolean {
            return try {
                val ai = context.packageManager.getApplicationInfo(pkg, 0)
                val systemFlag = android.content.pm.ApplicationInfo.FLAG_SYSTEM
                val updatedFlag = android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
                (ai.flags and systemFlag) != 0 || (ai.flags and updatedFlag) != 0
            } catch (_: Exception) {
                false // Package not found — flag it as potentially rogue
            }
        }
    }

    private var contentObserver: ContentObserver? = null
    private var isMonitoring = false
    private var lastAuditedListeners = emptySet<String>()

    fun startMonitoring() {
        if (!prefs.isOtpGuardEnabled) return
        if (isMonitoring) return

        val uri = Settings.Secure.getUriFor("enabled_notification_listeners")
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                auditListeners(triggeredByEvent = true)
            }
        }

        try {
            context.contentResolver.registerContentObserver(uri, false, contentObserver!!)
            isMonitoring = true
            Log.i(TAG, "OtpInterceptionGuardManager armed with ContentObserver.")
            auditListeners(triggeredByEvent = false)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register notification listener observer: ${e.message}")
        }
    }

    fun stopMonitoring() {
        if (isMonitoring && contentObserver != null) {
            try {
                context.contentResolver.unregisterContentObserver(contentObserver!!)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Error unregistering observer: ${e.message}")
            }
            contentObserver = null
            isMonitoring = false
            android.util.Log.i(TAG, "OtpInterceptionGuardManager stopped.")
        }
    }

    /** Exposes the application context for use in companion-object helpers called from commands. */
    fun getContext(): Context = context

    fun getActiveNotificationListeners(): List<String> {
        val raw = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
        if (raw.isBlank()) return emptyList()

        return raw.split(":")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.substringBefore("/") }
            .distinct()
    }

    fun auditListeners(triggeredByEvent: Boolean = false): List<String> {
        val currentListeners = getActiveNotificationListeners().toSet()
        val staticTrusted = TRUSTED_SYSTEM_LISTENERS + setOf(context.packageName) + prefs.otpGuardWhitelist

        // Three-tier trust: static whitelist → user whitelist → runtime system-app flag.
        // A package is rogue only if it fails ALL three checks, ensuring pre-installed OEM
        // packages (Motorola, Samsung, Xiaomi, OnePlus, etc.) are never falsely flagged.
        val rogueListeners = currentListeners.filter { pkg ->
            !staticTrusted.contains(pkg) && !isSystemApp(context, pkg)
        }.toSet()


        if (rogueListeners.isNotEmpty() && (triggeredByEvent || rogueListeners != lastAuditedListeners)) {
            lastAuditedListeners = rogueListeners
            Log.w(TAG, "⚠️ ROGUE NOTIFICATION LISTENERS DETECTED: $rogueListeners")

            for (pkg in rogueListeners) {
                var neutralized = false
                if (prefs.isOtpGuardAutoNeutralize && PasaDeviceAdmin.isDeviceOwner(context)) {
                    try {
                        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                        val component = PasaDeviceAdmin.getComponentName(context)
                        val failed = dpm.setPackagesSuspended(component, arrayOf(pkg), true)
                        neutralized = failed.isEmpty()
                        Log.i(TAG, "Auto-neutralized rogue listener package $pkg: suspended=$neutralized")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to auto-suspend rogue listener $pkg: ${e.message}")
                    }
                }

                if (prefs.isConfigured()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val alertMsg = "⚠️ <b>ROGUE NOTIFICATION LISTENER DETECTED!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                    "🚨 <b>Suspicious Package:</b> <code>$pkg</code>\n\n" +
                                    "⚠️ <b>Threat Rationale:</b> This application has been granted Android Notification Access.\n" +
                                    "It can silently read all incoming:\n" +
                                    "• 2FA SMS & WhatsApp OTPs\n" +
                                    "• Bank transaction debit/credit alerts\n" +
                                    "• Private chat messages\n\n" +
                                    if (neutralized) "🛑 <b>Auto-Neutralize:</b> Knox Device Owner has SUSPENDED this package immediately.\n"
                                    else "💡 <i>Review this app immediately. Whitelist if trusted:</i> <code>/otp_guard whitelist $pkg</code>"

                            telegramApi.sendMessage(
                                token = prefs.botToken,
                                request = SendMessageRequest(
                                    chatId = prefs.ownerChatIdLong,
                                    text = alertMsg
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to send rogue listener alert: ${e.message}")
                        }
                    }
                }
            }
        }

        return rogueListeners.toList()
    }
}
