package com.izhaanintellect.pasa.bot

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.LicenseManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sovereign Telegram Interactive Command Console Manager.
 *
 * Implements the comprehensive 6-Hub Executive Telegram UI natively on Android.
 * Runs 100% on-device in Sovereign Mode with zero dependency on central servers.
 */
@Singleton
class TelegramMenuManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val licenseManager: LicenseManager
) {

    data class MenuResponse(
        val text: String,
        val keyboard: InlineKeyboardMarkup
    )

    /**
     * Builds the Executive Dashboard with live on-device telemetry badges.
     */
    fun buildMainMenu(): MenuResponse {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = ComponentName(context, PasaDeviceAdmin::class.java)
        val isDeviceOwner = dpm?.isDeviceOwnerApp(context.packageName) == true
        val isAdmin = dpm?.isAdminActive(adminComponent) == true

        // Battery telemetry
        val batteryInfo = getBatteryTelemetry()
        val batteryText = "${batteryInfo.first}%${if (batteryInfo.second) " ⚡ Charging" else ""}"

        // Network telemetry
        val networkText = getNetworkTelemetry()

        // License status badge
        val licenseBadge = when {
            licenseManager.isPaidLicense() -> "💎 Pro Lifetime Active"
            licenseManager.isTrialActive() -> {
                val daysLeft = (licenseManager.getTrialRemainingMs() / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
                "⏳ 7-Day Trial (${daysLeft}d left)"
            }
            else -> "🔒 Trial Expired (License Required)"
        }

        // Security posture badge
        val securityBadge = when {
            isDeviceOwner -> "👑 Knox Device Owner (ASTRA Active)"
            isAdmin -> "🛡️ Device Admin Active"
            else -> "⚠️ Standard Permissions"
        }

        val text = buildString {
            appendLine("🛡️ <b>PASA SENTINEL — EXECUTIVE CONSOLE</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📱 <b>Target:</b> ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
            appendLine("⚡ <b>Status:</b> 🟢 Online (Sovereign Direct-Poll)")
            appendLine("🔐 <b>Security:</b> $securityBadge")
            appendLine("🔋 <b>Battery:</b> $batteryText")
            appendLine("📶 <b>Network:</b> $networkText")
            appendLine("🔑 <b>License:</b> $licenseBadge")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("<i>Select an Operational Command Hub below:</i>")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("📍 Location & RF Hub", callbackData = "menu:location_hub"),
                    InlineKeyboardButton("📸 Forensics Suite", callbackData = "menu:forensics_hub")
                ),
                listOf(
                    InlineKeyboardButton("🚨 Lockdown & Siren Hub", callbackData = "menu:lockdown_hub"),
                    InlineKeyboardButton("👑 Knox Device Owner", callbackData = "menu:device_owner_hub")
                ),
                listOf(
                    InlineKeyboardButton("🛡️ Autonomous Traps", callbackData = "menu:traps_hub"),
                    InlineKeyboardButton("📇 Extraction & Logs", callbackData = "menu:data_hub")
                ),
                listOf(
                    InlineKeyboardButton("🛡️ Cyber Defense Suite", callbackData = "menu:cyber_hub"),
                    InlineKeyboardButton("📊 Telemetry Status", callbackData = "cmd:status")
                ),
                listOf(
                    InlineKeyboardButton("🔄 Check OTA Update", callbackData = "cmd:check_update"),
                    InlineKeyboardButton("📲 SMS Fallback Guide", callbackData = "menu:sms_help")
                ),
                listOf(
                    InlineKeyboardButton("🔑 License & Pro", callbackData = "menu:license"),
                    InlineKeyboardButton("📖 Help Cheatsheet", callbackData = "cmd:help")
                )
            )
        )

        return MenuResponse(text, keyboard)
    }

    /**
     * Renders any of the Hubs, sub-menus, or wizard helper cards.
     */
    fun resolveMenu(menuKey: String): MenuResponse {
        return when (menuKey) {
            "menu:main", "menu:dashboard" -> buildMainMenu()
            "menu:location_hub" -> buildLocationHub()
            "menu:forensics_hub" -> buildForensicsHub()
            "menu:lockdown_hub" -> buildLockdownHub()
            "menu:device_owner_hub" -> buildDeviceOwnerHub()
            "menu:cyber_hub" -> buildCyberHub()
            "menu:traps_hub" -> buildTrapsHub()
            "menu:data_hub" -> buildDataHub()
            "menu:screen" -> buildScreenSubmenu()
            "menu:video" -> buildVideoSubmenu()
            "menu:record" -> buildRecordSubmenu()
            "menu:ring" -> buildRingSubmenu()
            "menu:track" -> buildTrackSubmenu()
            "menu:stealth" -> buildStealthSubmenu()
            "menu:duress" -> buildDuressSubmenu()
            "menu:shred" -> buildShredSubmenu()
            "menu:message" -> buildMessageSubmenu()
            "menu:wipe" -> buildWipeSubmenu()
            "menu:sms_help" -> buildSmsHelpSubmenu()
            "menu:license" -> buildLicenseSubmenu()
            // Wizards with 1-tap copyable templates
            // Wizards with 1-tap copyable templates and interactive quick buttons
            "wizard:lost_mode" -> buildWizardCard(
                title = "🛡️ Lost Mode Kiosk Lockdown",
                desc = "Engages full-screen Kiosk defense overlay, disables keyguard biometrics, and turns off screen.",
                syntax = "/lock lost <master_password> [message]",
                example = "/lock lost MySecretPass123 Device reported stolen! Call 01700000000",
                note = "To release Lost Mode remotely later, send <code>/unlock &lt;master_password&gt;</code>.",
                parentHub = "menu:lockdown_hub",
                quickActions = listOf(
                    listOf(
                        InlineKeyboardButton("🔒 Instant Lock (No Pass)", callbackData = "cmd:lock:instant")
                    )
                )
            )
            "wizard:set_os_pin" -> buildWizardCard(
                title = "🔑 Knox Hardware OS Lock PIN Reset",
                desc = "Overwrites forgotten or thief lockscreen PIN using Knox escrow tokens without wiping data.",
                syntax = "/set_os_pin <new_pin>",
                example = "/set_os_pin 7391",
                note = "Requires Knox Device Owner. If Master Password is set, send <code>/set_os_pin &lt;masterPass&gt; &lt;newPin&gt;</code> or send <code>/auth &lt;masterPass&gt;</code> first.",
                parentHub = "menu:device_owner_hub",
                quickActions = listOf(
                    listOf(
                        InlineKeyboardButton("🔐 Arm Escrow Token", callbackData = "cmd:escrow:arm"),
                        InlineKeyboardButton("📊 Escrow Status", callbackData = "cmd:escrow:status")
                    )
                )
            )
            "wizard:lock_app" -> buildWizardCard(
                title = "🧊 Smart Application Lockout",
                desc = "Completely conceals and freezes target apps from launcher and memory.",
                syntax = "/lock_app <target>",
                example = "/lock_app gallery\n/lock_app phone\n/lock_app files\n/lock_app com.whatsapp",
                note = "Available shortcuts: <code>gallery</code>, <code>phone</code>, <code>files</code>, or any package name.",
                parentHub = "menu:device_owner_hub",
                quickActions = listOf(
                    listOf(
                        InlineKeyboardButton("🧊 Freeze Gallery", callbackData = "cmd:lock_app:gallery"),
                        InlineKeyboardButton("🧊 Freeze Phone", callbackData = "cmd:lock_app:phone"),
                        InlineKeyboardButton("🧊 Freeze Files", callbackData = "cmd:lock_app:files")
                    ),
                    listOf(
                        InlineKeyboardButton("📦 View Frozen Apps", callbackData = "cmd:frozen")
                    )
                )
            )
            "wizard:unlock_app" -> buildWizardCard(
                title = "☀️ Restore Locked Application",
                desc = "Unfreezes and restores hidden applications back to the app drawer.",
                syntax = "/unlock_app <target>",
                example = "/unlock_app gallery\n/unlock_app phone\n/unlock_app files\n/unlock_app com.whatsapp",
                note = "Restores app icon and unfreezes app processes immediately.",
                parentHub = "menu:device_owner_hub",
                quickActions = listOf(
                    listOf(
                        InlineKeyboardButton("☀️ Restore Gallery", callbackData = "cmd:unlock_app:gallery"),
                        InlineKeyboardButton("☀️ Restore Phone", callbackData = "cmd:unlock_app:phone"),
                        InlineKeyboardButton("☀️ Restore Files", callbackData = "cmd:unlock_app:files")
                    ),
                    listOf(
                        InlineKeyboardButton("📦 View Frozen Apps", callbackData = "cmd:frozen")
                    )
                )
            )
            "wizard:call" -> buildWizardCard(
                title = "📞 Remote Cellular Outbound Calling",
                desc = "Remotely commands phone to place an outbound phone call via selected SIM slot.",
                syntax = "/call <number> [sim1|sim2] [speaker|earpiece]",
                example = "/call +8801700000000\n/call +8801700000000 sim1 speaker\n/call +8801700000000 sim2 speaker\n/call +8801700000000 earpiece",
                note = "Requires Device Owner or CALL_PHONE permission. Inspect available SIM slots below.",
                parentHub = "menu:data_hub",
                quickActions = listOf(
                    listOf(
                        InlineKeyboardButton("📊 Available SIM Accounts", callbackData = "cmd:call:status"),
                        InlineKeyboardButton("📶 SIM Telemetry", callbackData = "cmd:sim")
                    )
                )
            )
            "wizard:sendsms" -> buildWizardCard(
                title = "✉️ Direct Cellular Outbound SMS",
                desc = "Transmits an outbound SMS directly through the device's cellular radio.",
                syntax = "/sendsms [sim1|sim2] <number> <message>",
                example = "/sendsms +8801700000000 Emergency: Verify device location.\n/sendsms sim1 +8801700000000 Ping from PASA\n/sendsms sim2 +8801700000000 Ping from PASA",
                note = "Can be used to expose unknown SIM phone number via caller ID.",
                parentHub = "menu:data_hub",
                quickActions = listOf(
                    listOf(
                        InlineKeyboardButton("📶 Active SIMs & Signal", callbackData = "cmd:sim")
                    )
                )
            )
            "wizard:getfile" -> buildWizardCard(
                title = "📁 Download Storage File",
                desc = "Extracts any document, photo, or recording directly to Telegram (up to 50MB).",
                syntax = "/getfile <file_number_or_path>",
                example = "/getfile 1\n/getfile /sdcard/Download/secret.pdf",
                note = "Tip: Tap 'Browse Storage Files' below to see numbered files with 1-tap download IDs!",
                parentHub = "menu:data_hub",
                quickActions = listOf(
                    listOf(
                        InlineKeyboardButton("📂 Browse Storage Files", callbackData = "cmd:list_files")
                    )
                )
            )
            "wizard:wifi_connect" -> buildWizardCard(
                title = "📶 Emergency Wi-Fi Auto-Provisioning",
                desc = "Remotely connects phone to a specific Wi-Fi network while device is locked.",
                syntax = "/wifi_connect <ssid> [password]",
                example = "/wifi_connect HomeNetwork SecretPass123\n/wifi_connect OpenGuestNetwork\n/wifi_connect status\n/wifi_connect on\n/wifi_connect off",
                note = "Works on Android 10+ using WifiNetworkSpecifier and Android 8-9 using WifiConfiguration.",
                parentHub = "menu:device_owner_hub",
                quickActions = listOf(
                    listOf(
                        InlineKeyboardButton("📶 Wi-Fi Status", callbackData = "cmd:wifi_connect:status"),
                        InlineKeyboardButton("🟢 Turn ON", callbackData = "cmd:wifi_connect:on"),
                        InlineKeyboardButton("🔴 Turn OFF", callbackData = "cmd:wifi_connect:off")
                    )
                )
            )
            "wizard:lockscreen_info" -> buildWizardCard(
                title = "📱 Lockscreen Emergency Banner",
                desc = "Permanently displays recovery contact text onto the Android OS lockscreen.",
                syntax = "/lockscreen_info <message>",
                example = "/lockscreen_info Reward if returned: Call 01700000000",
                note = "To clear the banner, send <code>/lockscreen_info clear</code>.",
                parentHub = "menu:device_owner_hub",
                quickActions = listOf(
                    listOf(
                        InlineKeyboardButton("❌ Clear Banner", callbackData = "cmd:lockscreen_info:clear")
                    )
                )
            )
            "wizard:autolock" -> buildWizardCard(
                title = "⏱️ Screen Inactivity Autolock Timeout",
                desc = "Enforces maximum screen inactivity policy before device automatically locks.",
                syntax = "/autolock <seconds>",
                example = "/autolock 30",
                note = "Send <code>/autolock default</code> to restore system default timeout.",
                parentHub = "menu:device_owner_hub",
                quickActions = listOf(
                    listOf(
                        InlineKeyboardButton("⏱️ 15s", callbackData = "cmd:autolock:15"),
                        InlineKeyboardButton("⏱️ 30s", callbackData = "cmd:autolock:30"),
                        InlineKeyboardButton("⏱️ 60s", callbackData = "cmd:autolock:60"),
                        InlineKeyboardButton("⏱️ Default", callbackData = "cmd:autolock:default")
                    )
                )
            )
            "wizard:sim_phone" -> buildWizardCard(
                title = "📱 Set Emergency SMS Recipient",
                desc = "Phone number that receives automatic emergency SMS alerts if SIM is pulled or swapped.",
                syntax = "/sim_lock phone <number>",
                example = "/sim_lock phone +8801700000000",
                note = "Contains thief IMEI, carrier, and Google Maps GPS fix upon unauthorized SIM change.",
                parentHub = "menu:traps_hub"
            )
            "wizard:deadman" -> buildWizardCard(
                title = "💀 Anti-Forensic Dead Man's Switch",
                desc = "Autonomous timer that executes containment/shredding if phone stays disconnected without owner check-in.",
                syntax = "/deadman arm <hours> [wipe|lock]",
                example = "/deadman arm 24 lock\n/deadman arm 48 lock\n/deadman disarm",
                note = "To disarm, send <code>/deadman disarm</code>.",
                parentHub = "menu:traps_hub",
                quickActions = listOf(
                    listOf(
                        InlineKeyboardButton("📊 Deadman Status", callbackData = "cmd:deadman:status"),
                        InlineKeyboardButton("💀 Arm 24h Lock", callbackData = "cmd:deadman:arm:24"),
                        InlineKeyboardButton("🔓 Disarm", callbackData = "cmd:deadman:disarm")
                    )
                )
            )
            "wizard:thermal" -> buildWizardCard(
                title = "🔥 Thermal Anomaly Trap (Anti-EDL)",
                desc = "Detects hardware heat-gun backplate ungluing (>48°C) by forensic technicians.",
                syntax = "/thermal arm [celsius_threshold]",
                example = "/thermal arm 48\n/thermal arm 52\n/thermal disarm",
                note = "Severs USB data signaling pins immediately and enforces Kiosk defense.",
                parentHub = "menu:traps_hub",
                quickActions = listOf(
                    listOf(
                        InlineKeyboardButton("📊 Thermal Status", callbackData = "cmd:thermal:status"),
                        InlineKeyboardButton("🔥 Arm (48°C)", callbackData = "cmd:thermal:arm:48"),
                        InlineKeyboardButton("🔓 Disarm", callbackData = "cmd:thermal:disarm")
                    )
                )
            )
            "wizard:retire" -> buildWizardCard(
                title = "🗑️ Secure PASA Retirement",
                desc = "Permanently decommissions PASA — releases all Knox Device Owner privileges, shreds all credentials, and silently self-uninstalls. Irreversible.",
                syntax = "/retire <master_password> confirm",
                example = "/retire MySecretPassword confirm",
                note = "⚠️ Requires Master Password AND the word 'confirm'. Active session auth is NOT accepted. This action cannot be undone.",
                parentHub = "menu:main",
                quickActions = listOf(
                    listOf(
                        InlineKeyboardButton("👑 Device Owner Status", callbackData = "cmd:device_owner"),
                        InlineKeyboardButton("🛡️ Anti-Tamper Status", callbackData = "cmd:antitamper:status")
                    )
                )
            )
            "wizard:pause" -> buildWizardCard(
                title = "⏸️ PASA Dormant Mode",
                desc = "Suspends all monitoring (polling, traps, tracking, guards) while preserving Knox Device Owner restrictions. Fully reversible via /resume.",
                syntax = "/pause [master_password]",
                example = "/pause\n/resume <master_password>",
                note = "Knox restrictions (Anti-Tamper, USB lock, camera lock, DNS) remain ACTIVE while dormant. PASA will not auto-restart until /resume is sent.",
                parentHub = "menu:main",
                quickActions = listOf(
                    listOf(
                        InlineKeyboardButton("⏸️ Pause PASA", callbackData = "cmd:pause"),
                        InlineKeyboardButton("▶️ Resume PASA", callbackData = "cmd:resume"),
                        InlineKeyboardButton("📊 Pause Status", callbackData = "cmd:pause:status")
                    )
                )
            )
            else -> buildMainMenu()
        }
    }

    // ── Hub 1: Location & RF ──────────────────────────────────────────────────

    private fun buildLocationHub(): MenuResponse {
        val text = buildString {
            appendLine("📍 <b>Location & Cellular RF Telemetry Hub</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Acquire high-precision satellite positioning, cell tower triangulation, and cellular RF telemetry:")
            appendLine()
            appendLine("• <b>Instant GPS Fix:</b> Forcibly powers on hardware GNSS radio and generates pinpoint Google Maps coordinate link.")
            appendLine("• <b>Cell Tower Triangulation:</b> Reads multi-SIM MCC, MNC, LAC/TAC, CID, and RSSI (dBm) for zero-satellite indoor tracking.")
            appendLine("• <b>Continuous Live Tracking:</b> Periodic telemetry beacon (2 min / 5 min intervals).")
            appendLine("• <b>Safezone Geofence:</b> Arms radial boundary alerting upon perimeter departure.")
            appendLine("• <b>SIM Telemetry:</b> Reports active carriers, SIM slots, subscription IDs, and signal strengths.")
            appendLine()
            appendLine("<i>Select action:</i>")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("📍 Instant GPS Fix", callbackData = "cmd:locate"),
                    InlineKeyboardButton("📡 Cell Tower Triangulation", callbackData = "cmd:tower")
                ),
                listOf(
                    InlineKeyboardButton("🛰️ Live Tracking Menu", callbackData = "menu:track"),
                    InlineKeyboardButton("🌐 Safezone Geofence", callbackData = "cmd:geofence")
                ),
                listOf(
                    InlineKeyboardButton("📶 SIM & Radio Telemetry", callbackData = "cmd:sim"),
                    InlineKeyboardButton("🛡️ SIM Swap Guard", callbackData = "cmd:sim_lock")
                ),
                listOf(
                    InlineKeyboardButton("🔙 Back to Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    // ── Hub 2: Covert Forensics ──────────────────────────────────────────────

    private fun buildForensicsHub(): MenuResponse {
        val text = buildString {
            appendLine("📸 <b>Covert Forensics & Surveillance Suite</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Headless, zero-display surveillance streams direct-to-Telegram with instant memory shredding:")
            appendLine()
            appendLine("• <b>Stealth Photos:</b> Zero-blackout front selfie or rear lens capture.")
            appendLine("• <b>Silent Screenshot:</b> Non-intrusive Accessibility capture without dialogs.")
            appendLine("• <b>Screen Burst:</b> 5–10 frame composite storyboard showing intruder behavior.")
            appendLine("• <b>HD Screenrecord:</b> Covert MP4 screen capture (15s–30s).")
            appendLine("• <b>Stealth Video:</b> Silent CameraX headless video recording.")
            appendLine("• <b>Livestream:</b> Near-realtime progressive camera stream.")
            appendLine("• <b>Ambient Mic:</b> High-fidelity PCM/AAC room wiretap.")
            appendLine("• <b>Clipboard:</b> Read live clipboard memory.")
            appendLine()
            appendLine("<i>Select forensic tool:</i>")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("🤳 Front Selfie", callbackData = "cmd:snap:front"),
                    InlineKeyboardButton("📷 Rear Camera", callbackData = "cmd:snap:back"),
                    InlineKeyboardButton("📸 Dual Snap", callbackData = "cmd:snap:both")
                ),
                listOf(
                    InlineKeyboardButton("📱 Screenshot", callbackData = "cmd:screenshot"),
                    InlineKeyboardButton("🎞️ Screen Burst (5f)", callbackData = "cmd:screen_burst:5")
                ),
                listOf(
                    InlineKeyboardButton("🎥 Record Video (15s)", callbackData = "cmd:video:front:15"),
                    InlineKeyboardButton("📹 Screen Record (15s)", callbackData = "cmd:screenrecord:15")
                ),
                listOf(
                    InlineKeyboardButton("🎙️ Ambient Mic (30s)", callbackData = "cmd:record:30"),
                    InlineKeyboardButton("🔴 Live Stream (5f)", callbackData = "cmd:livestream:front:5")
                ),
                listOf(
                    InlineKeyboardButton("📋 Read Clipboard", callbackData = "cmd:clipboard"),
                    InlineKeyboardButton("🖼️ Recent Photos (3)", callbackData = "cmd:gallery_latest:3")
                ),
                listOf(
                    InlineKeyboardButton("⚙️ Video & Mic Menus", callbackData = "menu:video"),
                    InlineKeyboardButton("🔙 Back to Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    // ── Hub 3: Lockdown & Siren ──────────────────────────────────────────────

    private fun buildLockdownHub(): MenuResponse {
        val text = buildString {
            appendLine("🚨 <b>Emergency Lockdown & Alarm Hub</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Instant physical lockout, tactile localization, and deception defenses:")
            appendLine()
            appendLine("• <b>Instant Lock:</b> Immediately turns off display and locks keyguard (zero password required).")
            appendLine("• <b>Lost Mode Kiosk:</b> High-security defense: disables biometrics, blocks touch, displays recovery banner.")
            appendLine("• <b>Knox PIN Reset:</b> Resets Android OS hardware lockscreen PIN using escrow tokens.")
            appendLine("• <b>Emergency Siren:</b> Sounds deafening 100% volume siren bypassing silent & vibrate.")
            appendLine("• <b>Tactile Locator:</b> Secret rhythmic vibration pulses without audible noise.")
            appendLine("• <b>Fake Shutdown:</b> Simulates OEM power-off with 0-nit blackout canvas.")
            appendLine()
            appendLine("<i>Select lockout action:</i>")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("🔒 Instant Lock", callbackData = "cmd:lock:instant"),
                    InlineKeyboardButton("🔓 Remote Unlock", callbackData = "cmd:unlock")
                ),
                listOf(
                    InlineKeyboardButton("🛡️ Lost Mode Kiosk", callbackData = "wizard:lost_mode"),
                    InlineKeyboardButton("🔑 Reset OS PIN", callbackData = "wizard:set_os_pin")
                ),
                listOf(
                    InlineKeyboardButton("💬 Lockscreen Alert", callbackData = "menu:message"),
                    InlineKeyboardButton("🕶️ Fake Shutdown", callbackData = "cmd:fakeshutdown")
                ),
                listOf(
                    InlineKeyboardButton("🚨 Sound Siren (30s)", callbackData = "cmd:ring:30"),
                    InlineKeyboardButton("🔕 Stop Siren", callbackData = "cmd:ring_stop")
                ),
                listOf(
                    InlineKeyboardButton("📳 Tactile SOS Vibrate", callbackData = "cmd:vibrate_pulse:sos"),
                    InlineKeyboardButton("☀️ Wake Device", callbackData = "cmd:wake")
                ),
                listOf(
                    InlineKeyboardButton("🔙 Back to Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    // ── Hub 4: Enterprise Knox Device Owner Suite ────────────────────────────

    private fun buildDeviceOwnerHub(): MenuResponse {
        val text = buildString {
            appendLine("👑 <b>Enterprise Knox Device Owner Suite</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Low-level hardware policies, HAL-level killswitches, and anti-forensics:")
            appendLine()
            appendLine("• <b>Hardware Camera Lock:</b> Disables all front and rear cameras OS-wide.")
            appendLine("• <b>Bluetooth Lock:</b> Disallows all Bluetooth pairings and file transfers.")
            appendLine("• <b>Hardware Mic Mute:</b> Mutes all audio recording at the HAL level.")
            appendLine("• <b>USB Data Pin Killswitch:</b> Cuts data pins (defeats GrayKey, Cellebrite, juice-jacking).")
            appendLine("• <b>Lockscreen Info:</b> Pins permanent owner contact info to OS keyguard.")
            appendLine("• <b>Autolock:</b> Enforces custom screen inactivity timeout policy.")
            appendLine("• <b>Wi-Fi Provisioning:</b> Forces connection to known Wi-Fi while locked.")
            appendLine("• <b>Security Audit:</b> Inspects kernel security logs (ADB shells, KeyStore tampered).")
            appendLine("• <b>Encrypted DNS:</b> Enforces system-wide DNS-over-TLS (Quad9/Cloudflare).")
            appendLine("• <b>Remote Reboot:</b> Restarts phone hardware remotely.")
            appendLine()
            appendLine("<i>Select Device Owner policy:</i>")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("📷 Cam Lock ON", callbackData = "cmd:camera_lock:on"),
                    InlineKeyboardButton("🔓 Cam Lock OFF", callbackData = "cmd:camera_lock:off")
                ),
                listOf(
                    InlineKeyboardButton("📡 BT Lock ON", callbackData = "cmd:bluetooth_lock:on"),
                    InlineKeyboardButton("🔓 BT Lock OFF", callbackData = "cmd:bluetooth_lock:off")
                ),
                listOf(
                    InlineKeyboardButton("🔇 Mic Mute ON", callbackData = "cmd:mic_mute:on"),
                    InlineKeyboardButton("🔊 Mic Mute OFF", callbackData = "cmd:mic_mute:off")
                ),
                listOf(
                    InlineKeyboardButton("🔌 USB Lock ON", callbackData = "cmd:usb_lock:on"),
                    InlineKeyboardButton("🔓 USB Lock OFF", callbackData = "cmd:usb_lock:off")
                ),
                listOf(
                    InlineKeyboardButton("🔑 Knox OS PIN Reset", callbackData = "wizard:set_os_pin"),
                    InlineKeyboardButton("📱 Lockscreen Banner", callbackData = "wizard:lockscreen_info")
                ),
                listOf(
                    InlineKeyboardButton("⏱️ Screen Autolock", callbackData = "wizard:autolock"),
                    InlineKeyboardButton("📶 Auto-Connect Wi-Fi", callbackData = "wizard:wifi_connect")
                ),
                listOf(
                    InlineKeyboardButton("📑 Security Audit Log", callbackData = "cmd:security_audit"),
                    InlineKeyboardButton("🔄 Remote Reboot", callbackData = "cmd:reboot")
                ),
                listOf(
                    InlineKeyboardButton("🛡️ Anti-Tamper ON", callbackData = "cmd:antitamper:on"),
                    InlineKeyboardButton("🌐 Encrypted DNS (Quad9)", callbackData = "cmd:dns:quad9")
                ),
                listOf(
                    InlineKeyboardButton("👑 Verify DO Status", callbackData = "cmd:device_owner"),
                    InlineKeyboardButton("✨ Self-Heal Permissions", callbackData = "cmd:self_heal")
                ),
                listOf(
                    InlineKeyboardButton("🧊 Freeze App", callbackData = "wizard:lock_app"),
                    InlineKeyboardButton("☀️ Restore App", callbackData = "wizard:unlock_app")
                ),
                listOf(
                    InlineKeyboardButton("🔙 Back to Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    // ── Advanced Mobile Cyber Defense Suite Hub ──────────────────────────────

    private fun buildCyberHub(): MenuResponse {
        val text = buildString {
            appendLine("🛡️ <b>Advanced Mobile Cyber Defense Suite</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Next-generation shields against advanced persistent threats, mobile spyware, and banking trojans:")
            appendLine()
            appendLine("• <b>Accessibility Trojan Shield:</b> Restricts accessibility services to PASA only via Knox policies (neutralizes SharkBot/Hook/Godfather).")
            appendLine("• <b>Locked USB Killswitch:</b> Automatically drops USB data pins when screen locks (anti-Cellebrite/GrayKey).")
            appendLine("• <b>Anti-2G / IMSI Shield:</b> Disables 2G cellular baseband modem radio & monitors rogue Stingray traps.")
            appendLine("• <b>Crypto Clipper Trap:</b> Real-time clipboard monitor detecting Bitcoin/EVM/Tron address swaps.")
            appendLine("• <b>Sideload & Install Lock:</b> Blocks rogue APK installations & unknown sources via Device Owner.")
            appendLine("• <b>Ransomware Canary Guard:</b> Deploys tripwire files; automatically freezes all apps upon file encryption.")
            appendLine("• <b>2FA / OTP Interception Guard:</b> Audits Notification Access & neutralizes rogue notification listeners.")
            appendLine()
            appendLine("<i>Select Cyber Defense action:</i>")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("🛡️ A11y Shield ON", callbackData = "cmd:a11y_shield:lock"),
                    InlineKeyboardButton("🔓 A11y Shield OFF", callbackData = "cmd:a11y_shield:unlock")
                ),
                listOf(
                    InlineKeyboardButton("🔌 USB Autolock ON", callbackData = "cmd:usb_autolock:enable"),
                    InlineKeyboardButton("🔓 USB Autolock OFF", callbackData = "cmd:usb_autolock:disable")
                ),
                listOf(
                    InlineKeyboardButton("📡 Anti-2G Shield ON", callbackData = "cmd:anti_2g:enable"),
                    InlineKeyboardButton("📻 Anti-2G OFF", callbackData = "cmd:anti_2g:disable")
                ),
                listOf(
                    InlineKeyboardButton("📋 Clipper Trap ON", callbackData = "cmd:clipper_guard:enable"),
                    InlineKeyboardButton("📋 Clipper Trap OFF", callbackData = "cmd:clipper_guard:disable")
                ),
                listOf(
                    InlineKeyboardButton("📦 Block Sideloads", callbackData = "cmd:app_install_lock:unknown_only"),
                    InlineKeyboardButton("🛑 Freeze All Installs", callbackData = "cmd:app_install_lock:block_all")
                ),
                listOf(
                    InlineKeyboardButton("🪤 Arm Canary Trap", callbackData = "cmd:canary_guard:arm"),
                    InlineKeyboardButton("🔍 Audit Canaries", callbackData = "cmd:canary_guard:check")
                ),
                listOf(
                    InlineKeyboardButton("📬 2FA Guard Audit", callbackData = "cmd:otp_guard:audit"),
                    InlineKeyboardButton("🛑 2FA Auto-Neutralize", callbackData = "cmd:otp_guard:auto_neutralize:on")
                ),
                listOf(
                    InlineKeyboardButton("🔙 Back to Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    // ── Hub 5: Traps & Edge Defense ──────────────────────────────────────────

    private fun buildTrapsHub(): MenuResponse {
        val text = buildString {
            appendLine("🛡️ <b>Autonomous Sensor Traps & Anti-Theft Hub</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Autonomous on-device edge detection triggers instantly without remote commands:")
            appendLine()
            appendLine("• <b>Kinetic Snatch Trap:</b> Accelerometer detects violent grabs (>2.6G), locks phone, snaps mugshot, and alerts.")
            appendLine("• <b>Charger Disconnect Trap:</b> Alerts immediately if AC power is unplugged while locked.")
            appendLine("• <b>Pocket / Bag Extraction:</b> Triggers if proximity sensor is uncovered while locked without unlock.")
            appendLine("• <b>Failed Pattern Guard:</b> Snaps stealth selfies upon repeated wrong PIN / pattern attempts.")
            appendLine("• <b>SIM Swap Guard:</b> Locks device if unknown SIM card is inserted.")
            appendLine("• <b>App Network Firewall:</b> Isolates malicious background apps from outbound telemetry.")
            appendLine("• <b>Battery Drain Alert:</b> Alerts on rapid battery drain detecting background wiretaps.")
            appendLine("• <b>Dead Man's Switch:</b> Irreversible cryptographic auto-destruct if held offline without owner heartbeat.")
            appendLine("• <b>Thermal Anomaly Trap:</b> Detects heat-gun back-cover ungluing (>48°C), severs USB pins & locks Kiosk.")
            appendLine("• <b>Decoy Duress PIN:</b> Coercion unlock that opens sterile decoy OS and broadcasts SOS.")
            appendLine()
            appendLine("<i>Manage sensor traps:</i>")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("🟢 Arm All Traps", callbackData = "cmd:trap:on"),
                    InlineKeyboardButton("🔴 Disarm All Traps", callbackData = "cmd:trap:off")
                ),
                listOf(
                    InlineKeyboardButton("🏃 Snatch Trap", callbackData = "cmd:trap:snatch"),
                    InlineKeyboardButton("🔌 Charger Trap", callbackData = "cmd:trap:charger")
                ),
                listOf(
                    InlineKeyboardButton("👁️ Pattern Guard (3 tries)", callbackData = "cmd:pattern_guard:enable"),
                    InlineKeyboardButton("🛡️ SIM Swap Guard", callbackData = "cmd:sim_lock:enable")
                ),
                listOf(
                    InlineKeyboardButton("📱 Set Emergency SMS Phone", callbackData = "wizard:sim_phone"),
                    InlineKeyboardButton("📊 Traps Telemetry Status", callbackData = "cmd:trap:status")
                ),
                listOf(
                    InlineKeyboardButton("🧱 App Firewall ON", callbackData = "cmd:app_firewall:enable"),
                    InlineKeyboardButton("🔋 Battery Alert ON", callbackData = "cmd:battery_alert:enable")
                ),
                listOf(
                    InlineKeyboardButton("💀 Dead Man Switch", callbackData = "wizard:deadman"),
                    InlineKeyboardButton("🔥 Thermal Trap", callbackData = "wizard:thermal")
                ),
                listOf(
                    InlineKeyboardButton("🆘 Decoy Duress PIN", callbackData = "menu:duress"),
                    InlineKeyboardButton("🔍 Anti-Tamper Scan", callbackData = "cmd:tamper_detect")
                ),
                listOf(
                    InlineKeyboardButton("👁️ Stealth Launcher Cloak", callbackData = "menu:stealth"),
                    InlineKeyboardButton("🔙 Back to Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    // ── Hub 6: Extraction, Telephony & Logs ──────────────────────────────────

    private fun buildDataHub(): MenuResponse {
        val text = buildString {
            appendLine("📇 <b>Extraction, Telemetry & System Logs Hub</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Extract forensic data, inspect communications, manage apps, and review audit trails:")
            appendLine()
            appendLine("• <b>Contacts:</b> Query device address book names & phone numbers.")
            appendLine("• <b>Call History:</b> View recent incoming, outgoing, and missed calls.")
            appendLine("• <b>SMS Inbox:</b> View recent SMS text messages and 2FA OTP codes.")
            appendLine("• <b>Direct Outbound SMS:</b> Dispatch SMS message via SIM slot directly.")
            appendLine("• <b>Audit Trail:</b> Inspect recent command execution history.")
            appendLine("• <b>Installed Apps:</b> List installed applications and package identifiers.")
            appendLine("• <b>Network Telemetry:</b> Current IP, Wi-Fi SSID, and cellular network status.")
            appendLine("• <b>Storage Explorer:</b> Browse phone storage files with 1-tap download numbers.")
            appendLine()
            appendLine("<i>Select extraction option:</i>")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("👥 Contacts", callbackData = "cmd:contacts"),
                    InlineKeyboardButton("📞 Call Log", callbackData = "cmd:call_log")
                ),
                listOf(
                    InlineKeyboardButton("💬 SMS Inbox", callbackData = "cmd:sms_log"),
                    InlineKeyboardButton("✉️ Send Outbound SMS", callbackData = "wizard:sendsms")
                ),
                listOf(
                    InlineKeyboardButton("📜 Command Audit Trail", callbackData = "cmd:history"),
                    InlineKeyboardButton("📦 Installed Apps", callbackData = "cmd:apps")
                ),
                listOf(
                    InlineKeyboardButton("🌐 Network Telemetry", callbackData = "cmd:network"),
                    InlineKeyboardButton("📲 SMS TOTP Setup", callbackData = "cmd:smssetup")
                ),
                listOf(
                    InlineKeyboardButton("📂 Browse Storage Files", callbackData = "cmd:list_files"),
                    InlineKeyboardButton("📁 Download File", callbackData = "wizard:getfile")
                ),
                listOf(
                    InlineKeyboardButton("📞 Remote Outbound Call", callbackData = "wizard:call"),
                    InlineKeyboardButton("🗑️ File Shredder", callbackData = "menu:shred")
                ),
                listOf(
                    InlineKeyboardButton("⚠️ Remote Factory Wipe", callbackData = "menu:wipe"),
                    InlineKeyboardButton("🔙 Back to Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    // ── Sub-Menus ────────────────────────────────────────────────────────────

    private fun buildScreenSubmenu(): MenuResponse {
        val text = buildString {
            appendLine("📱 <b>Covert Screen Surveillance Suite</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Capture real-time intruder screen activity silently with zero popups:")
            appendLine()
            appendLine("• <b>Silent Screenshot:</b> Native A11y capture (API 30+). Returns full-res PNG directly to chat.")
            appendLine("• <b>Screen Burst:</b> Stitches 5–10 rapid frames over 10s into a multi-frame storyboard grid image.")
            appendLine("• <b>Screen Recording:</b> Covert HD MP4 video via Device Owner shell (15–30s).")
            appendLine()
            appendLine("<i>Choose capture action:</i>")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("📸 Instant Screenshot", callbackData = "cmd:screenshot")
                ),
                listOf(
                    InlineKeyboardButton("🎞️ Screen Burst (5 frames)", callbackData = "cmd:screen_burst:5"),
                    InlineKeyboardButton("🎞️ Screen Burst (10 frames)", callbackData = "cmd:screen_burst:10")
                ),
                listOf(
                    InlineKeyboardButton("🎥 Record Screen (15s)", callbackData = "cmd:screenrecord:15"),
                    InlineKeyboardButton("🎥 Record Screen (30s)", callbackData = "cmd:screenrecord:30")
                ),
                listOf(
                    InlineKeyboardButton("🔙 Back to Forensics", callbackData = "menu:forensics_hub"),
                    InlineKeyboardButton("🏠 Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    private fun buildVideoSubmenu(): MenuResponse {
        val text = buildString {
            appendLine("🎥 <b>Stealth Video & Live Stream</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Record short clips or stream live camera video directly to Telegram:")
            appendLine()
            appendLine("• <b>Silent Video:</b> Direct-to-Telegram MP4 clip with zero display flicker.")
            appendLine("• <b>Livestream:</b> Progressive live stream delivered to chat in real-time.")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("🔴 Live Stream (Front)", callbackData = "cmd:livestream:front:5"),
                    InlineKeyboardButton("🔴 Live Stream (Rear)", callbackData = "cmd:livestream:back:5")
                ),
                listOf(
                    InlineKeyboardButton("🤳 Front Clip (15s)", callbackData = "cmd:video:front:15"),
                    InlineKeyboardButton("📷 Rear Clip (15s)", callbackData = "cmd:video:back:15")
                ),
                listOf(
                    InlineKeyboardButton("🤳 Front Clip (30s)", callbackData = "cmd:video:front:30"),
                    InlineKeyboardButton("📷 Rear Clip (30s)", callbackData = "cmd:video:back:30")
                ),
                listOf(
                    InlineKeyboardButton("🛑 Stop Active Stream", callbackData = "cmd:stopstream")
                ),
                listOf(
                    InlineKeyboardButton("🔙 Back to Forensics", callbackData = "menu:forensics_hub"),
                    InlineKeyboardButton("🏠 Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    private fun buildRecordSubmenu(): MenuResponse {
        val text = buildString {
            appendLine("🎙️ <b>Ambient Microphone Forensics</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Record room audio wiretap in high-fidelity AAC directly to chat:")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("⏱️ 15 Seconds", callbackData = "cmd:record:15"),
                    InlineKeyboardButton("⏱️ 30 Seconds", callbackData = "cmd:record:30"),
                    InlineKeyboardButton("⏱️ 60 Seconds", callbackData = "cmd:record:60")
                ),
                listOf(
                    InlineKeyboardButton("🛑 Stop Active Recording", callbackData = "cmd:record:stop")
                ),
                listOf(
                    InlineKeyboardButton("🔙 Back to Forensics", callbackData = "menu:forensics_hub"),
                    InlineKeyboardButton("🏠 Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    private fun buildRingSubmenu(): MenuResponse {
        val text = buildString {
            appendLine("🔊 <b>Emergency Alarm Siren</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Trigger maximum volume siren (overrides silent and vibrate modes):")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("🚨 Sound Siren (30s)", callbackData = "cmd:ring:30"),
                    InlineKeyboardButton("🚨 Sound Siren (60s)", callbackData = "cmd:ring:60")
                ),
                listOf(
                    InlineKeyboardButton("🔕 Silence Siren", callbackData = "cmd:ring_stop")
                ),
                listOf(
                    InlineKeyboardButton("🔙 Back to Lockdown", callbackData = "menu:lockdown_hub"),
                    InlineKeyboardButton("🏠 Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    private fun buildTrackSubmenu(): MenuResponse {
        val text = buildString {
            appendLine("📍 <b>Continuous Live GPS Tracking</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Stream periodic satellite location updates directly to Telegram:")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("▶️ Track Every 2 min", callbackData = "cmd:track:2"),
                    InlineKeyboardButton("▶️ Track Every 5 min", callbackData = "cmd:track:5")
                ),
                listOf(
                    InlineKeyboardButton("⏹️ Stop Live Tracking", callbackData = "cmd:track_stop")
                ),
                listOf(
                    InlineKeyboardButton("🔙 Back to Location", callbackData = "menu:location_hub"),
                    InlineKeyboardButton("🏠 Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    private fun buildStealthSubmenu(): MenuResponse {
        val text = buildString {
            appendLine("👁️ <b>App Icon Stealth & Launcher Cloaking</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Control whether PASA Sentinel is visible in the phone’s app drawer:")
            appendLine()
            appendLine("• <b>Hide Icon:</b> Strips the app icon from the launcher & app drawer completely. All background defense remains 100% active.")
            appendLine("• <b>Show Icon:</b> Restores the app icon back to the launcher & app drawer.")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("🔇 Hide App Icon", callbackData = "cmd:stealth:hide"),
                    InlineKeyboardButton("👁️ Show App Icon", callbackData = "cmd:stealth:show")
                ),
                listOf(
                    InlineKeyboardButton("🔄 Toggle Visibility", callbackData = "cmd:stealth")
                ),
                listOf(
                    InlineKeyboardButton("🔙 Back to Traps", callbackData = "menu:traps_hub"),
                    InlineKeyboardButton("🏠 Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    private fun buildDuressSubmenu(): MenuResponse {
        val text = buildString {
            appendLine("🔑 <b>Emergency Duress Mode</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("If forced by an intruder to unlock your phone, entering your decoy Duress PIN:")
            appendLine()
            appendLine("1. Closes the lock overlay as if successfully unlocked")
            appendLine("2. <b>Sterile Sandbox Decoy OS:</b> Instantly vanishes banking, crypto, and private messengers")
            appendLine("3. Silently captures front & rear stealth selfies")
            appendLine("4. Obtains high-accuracy GPS coordinates")
            appendLine("5. Transmits an urgent 🚨 SOS Beacon to Telegram!")
            appendLine()
            appendLine("📝 <b>How to Configure:</b>")
            appendLine("Send: <code>/duress_pin &lt;4-8 digit PIN&gt;</code>")
            appendLine("Example: <code>/duress_pin 9182</code>")
            appendLine()
            appendLine("ℹ️ <i>If Master Password is set, send /auth &lt;password&gt; first or include it: /duress_pin &lt;password&gt; &lt;pin&gt;</i>")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("📊 Check Duress Status", callbackData = "cmd:duress_pin:status"),
                    InlineKeyboardButton("🔐 Arm Escrow Token", callbackData = "cmd:escrow:arm")
                ),
                listOf(
                    InlineKeyboardButton("🔒 Lock Screen to Arm", callbackData = "cmd:lock:instant"),
                    InlineKeyboardButton("❌ Clear Duress PIN", callbackData = "cmd:duress_pin:clear")
                ),
                listOf(
                    InlineKeyboardButton("🔙 Back to Traps", callbackData = "menu:traps_hub"),
                    InlineKeyboardButton("🏠 Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    private fun buildShredSubmenu(): MenuResponse {
        val text = buildString {
            appendLine("🗑️ <b>Cryptographic File Shredder</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Permanently overwrite confidential files with multi-pass PRNG noise and zero-fill before deletion.")
            appendLine()
            appendLine("Select target directory to sanitize:")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("📥 Shred Downloads", callbackData = "cmd:shred:downloads"),
                    InlineKeyboardButton("📄 Shred Documents", callbackData = "cmd:shred:documents")
                ),
                listOf(
                    InlineKeyboardButton("📸 Shred Camera Roll", callbackData = "cmd:shred:camera")
                ),
                listOf(
                    InlineKeyboardButton("🔙 Back to Extraction", callbackData = "menu:data_hub"),
                    InlineKeyboardButton("🏠 Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    private fun buildMessageSubmenu(): MenuResponse {
        val text = buildString {
            appendLine("💬 <b>Display Screen Alert Message</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Broadcast an urgent lost-mode alert or emergency contact banner directly over the phone's lockscreen.")
            appendLine()
            appendLine("Tap a quick preset below or type <code>/message &lt;text&gt;</code>:")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("📱 \"Lost phone! Please call owner.\"", callbackData = "cmd:message:Lost phone! Please call owner.")
                ),
                listOf(
                    InlineKeyboardButton("⚠️ \"Stolen device! Police GPS active.\"", callbackData = "cmd:message:Stolen device! Police GPS active.")
                ),
                listOf(
                    InlineKeyboardButton("💰 \"Reward offered! Call owner.\"", callbackData = "cmd:message:Reward offered! Call owner.")
                ),
                listOf(
                    InlineKeyboardButton("🔙 Back to Lockdown", callbackData = "menu:lockdown_hub"),
                    InlineKeyboardButton("🏠 Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    private fun buildWipeSubmenu(): MenuResponse {
        val text = buildString {
            appendLine("⚠️ <b>DESTRUCTIVE ACTION: REMOTE FACTORY WIPE</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("This will <b>permanently and irreversibly erase</b> all photos, chats, accounts, and cryptographic keys on the device.")
            appendLine()
            appendLine("🛡️ <b>Two-Step Safety Verification:</b>")
            appendLine("To prevent accidental execution, wiping requires authentication with your Master Passphrase.")
            appendLine()
            appendLine("If you are absolutely certain, send:")
            appendLine("<code>/wipe_confirm &lt;MasterPassword&gt;</code>")
            appendLine()
            appendLine("<i>Example:</i> <code>/wipe_confirm MySecretPass123</code>")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("❌ Cancel (Safe)", callbackData = "menu:data_hub")
                ),
                listOf(
                    InlineKeyboardButton("🏠 Return to Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    private fun buildSmsHelpSubmenu(): MenuResponse {
        val text = buildString {
            appendLine("📲 <b>PASA Air-Gapped Cellular SMS Cheatsheet</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("When device has NO Internet (no Wi-Fi, no mobile data), PASA operates air-gapped via GSM cellular SMS radio.")
            appendLine("<b>Format:</b> <code>PASA &lt;pin&gt; &lt;command&gt; [args]</code>")
            appendLine()
            appendLine("📡 <b>Location & Diagnostics:</b>")
            appendLine("• <code>PASA &lt;pin&gt; /locate</code> — Force GPS on & return Maps pin")
            appendLine("• <code>PASA &lt;pin&gt; /status</code> — Battery, screen, DO & trap status")
            appendLine("• <code>PASA &lt;pin&gt; /security_audit</code> — Query OS kernel security log")
            appendLine()
            appendLine("🚨 <b>Lockdown, Blackout & Siren:</b>")
            appendLine("• <code>PASA &lt;pin&gt; /lock</code> — Instant screen lock & keyguard")
            appendLine("• <code>PASA &lt;pin&gt; /lock lost [msg]</code> — Full Knox Kiosk Lost Mode")
            appendLine("• <code>PASA &lt;pin&gt; /unlock</code> — Release screen lock & Lost Mode")
            appendLine("• <code>PASA &lt;pin&gt; /ring 60</code> — Max-volume emergency alarm siren")
            appendLine("• <code>PASA &lt;pin&gt; /vibrate_pulse pulse</code> — Silent tactile vibration locator")
            appendLine("• <code>PASA &lt;pin&gt; /fakeshutdown</code> — 0-nit stealth black canvas")
            appendLine("• <code>PASA &lt;pin&gt; /wake</code> — Awaken screen from blackout")
            appendLine()
            appendLine("👑 <b>Knox Device Owner & Hardware Controls:</b>")
            appendLine("• <code>PASA &lt;pin&gt; /set_os_pin &lt;new_pin&gt;</code> — Reset OS lockscreen PIN")
            appendLine("• <code>PASA &lt;pin&gt; /camera_lock on|off</code> — Hardware camera killswitch")
            appendLine("• <code>PASA &lt;pin&gt; /usb_lock on|off</code> — Disable USB data pins")
            appendLine("• <code>PASA &lt;pin&gt; /bluetooth_lock on|off</code> — Block Bluetooth sharing")
            appendLine("• <code>PASA &lt;pin&gt; /mic_mute on|off</code> — Hardware audio mute (HAL level)")
            appendLine("• <code>PASA &lt;pin&gt; /wifi_connect &lt;ssid&gt; [pass]</code> — Connect Wi-Fi while locked")
            appendLine("• <code>PASA &lt;pin&gt; /lockscreen_info &lt;text&gt;</code> — Pin contact to lockscreen")
            appendLine("• <code>PASA &lt;pin&gt; /autolock &lt;seconds&gt;</code> — Inactivity autolock timeout")
            appendLine("• <code>PASA &lt;pin&gt; /lock_app &lt;target&gt;</code> — Freeze sensitive app")
            appendLine("• <code>PASA &lt;pin&gt; /unlock_app &lt;target&gt;</code> — Restore frozen app")
            appendLine("• <code>PASA &lt;pin&gt; /app_uninstall &lt;pkg&gt;</code> — Silently remove package")
            appendLine("• <code>PASA &lt;pin&gt; /reboot</code> — Remotely restart device hardware")
            appendLine()
            appendLine("🛡️ <b>Anti-Theft Traps & Hardening:</b>")
            appendLine("• <code>PASA &lt;pin&gt; /sim_tray_lock arm|release</code> — Cryptographic SIM lock")
            appendLine("• <code>PASA &lt;pin&gt; /antitamper on|off</code> — Safe boot & reset lockdown")
            appendLine("• <code>PASA &lt;pin&gt; /biometrics on|off</code> — Coercion biometric killswitch")
            appendLine("• <code>PASA &lt;pin&gt; /deadman arm 24</code> — Dead man's switch timer")
            appendLine("• <code>PASA &lt;pin&gt; /thermal arm 48</code> — Heat-gun backplate trap")
            appendLine()
            appendLine("📞 <b>Air-Gapped Telephony & Surveillance:</b>")
            appendLine("• <code>PASA &lt;pin&gt; /call &lt;phone&gt; [sim1|sim2]</code> — Outbound phone call")
            appendLine("• <code>PASA &lt;pin&gt; /sendsms &lt;phone&gt; &lt;msg&gt;</code> — Send SMS via device SIM")
            appendLine("• <code>PASA &lt;pin&gt; /snap front|back</code> — Capture stealth mugshot")
            appendLine()
            appendLine("🔐 <b>Credentials & Remote Wipe:</b>")
            appendLine("• <code>PASA &lt;pin&gt; /set_master_pin &lt;new_pin&gt;</code> — Rotate master PIN")
            appendLine("• <code>PASA &lt;pin&gt; /wipe</code> — 2-step authenticated wipe")
            appendLine("• <code>PASA &lt;pin&gt; /wipe_confirm &lt;masterPass&gt;</code> — Confirm wipe")
            appendLine()
            appendLine("💡 <b>Dual-SIM:</b> Replies are sent back automatically via the receiving SIM.")
            appendLine("🔐 <b>TOTP Setup:</b> Send <code>/smssetup</code> to enroll in Google Authenticator!")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("📲 Enroll TOTP", callbackData = "cmd:smssetup"),
                    InlineKeyboardButton("🔙 Back to Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    private fun buildLicenseSubmenu(): MenuResponse {
        val isPaid = licenseManager.isPaidLicense()
        val isTrial = licenseManager.isTrialActive()
        val daysLeft = (licenseManager.getTrialRemainingMs() / (1000 * 60 * 60 * 24)).coerceAtLeast(0)

        val statusText = when {
            isPaid -> "💎 <b>Pro Lifetime License Active</b>\nAll 87+ advanced security, covert forensics, and Knox Device Owner tools unlocked permanently."
            isTrial -> "⏳ <b>7-Day Free Trial Active (${daysLeft} days remaining)</b>\nAll features fully unlocked during trial period. Activate a Pro license anytime."
            else -> "🔒 <b>Trial Expired</b>\nFeatures are locked. Activate your Pro key below to restore full protection."
        }

        val text = buildString {
            appendLine("🔑 <b>PASA Sentinel Commercial Licensing</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine(statusText)
            appendLine()
            appendLine("<b>How to Activate a License Key:</b>")
            appendLine("Send: <code>/license PASA-PRO-XXXX-XXXX</code>")
            appendLine()
            appendLine("🌐 <b>Purchase Pro or Enterprise:</b>")
            appendLine("<a href=\"https://pasa.izhaanintellect.fun/#pricing\">https://pasa.izhaanintellect.fun/#pricing</a>")
        }

        val keyboard = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("🛒 Buy Pro License", url = "https://pasa.izhaanintellect.fun/#pricing")
                ),
                listOf(
                    InlineKeyboardButton("🔙 Back to Dashboard", callbackData = "menu:main")
                )
            )
        )
        return MenuResponse(text, keyboard)
    }

    // ── Helper Wizard Card ───────────────────────────────────────────────────

    private fun buildWizardCard(
        title: String,
        desc: String,
        syntax: String,
        example: String,
        note: String,
        parentHub: String,
        quickActions: List<List<InlineKeyboardButton>> = emptyList()
    ): MenuResponse {
        val text = buildString {
            appendLine("<b>$title</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine(desc)
            appendLine()
            appendLine("📝 <b>Format:</b>")
            appendLine("<code>$syntax</code>")
            appendLine()
            appendLine("💡 <b>Tap to Copy & Send:</b>")
            example.lines().forEach { line ->
                appendLine("<code>$line</code>")
            }
            appendLine()
            if (note.isNotBlank()) {
                appendLine("ℹ️ <i>$note</i>")
                appendLine()
            }
            appendLine("<i>Copy the command above, edit if needed, and send it into this chat!</i>")
        }

        val rows = mutableListOf<List<InlineKeyboardButton>>()
        if (quickActions.isNotEmpty()) {
            rows.addAll(quickActions)
        }
        rows.add(
            listOf(
                InlineKeyboardButton("🔙 Back to Hub", callbackData = parentHub),
                InlineKeyboardButton("🏠 Dashboard", callbackData = "menu:main")
            )
        )

        val keyboard = InlineKeyboardMarkup(inlineKeyboard = rows)
        return MenuResponse(text, keyboard)
    }

    // ── Telemetry Utilities ──────────────────────────────────────────────────

    private fun getBatteryTelemetry(): Pair<Int, Boolean> {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 0
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            Pair(pct, isCharging)
        } catch (_: Exception) {
            Pair(0, false)
        }
    }

    private fun getNetworkTelemetry(): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return "Unknown"
            val network = cm.activeNetwork ?: return "Offline"
            val caps = cm.getNetworkCapabilities(network) ?: return "Connected"

            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi Active"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular (Mobile Data)"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Online"
            }
        } catch (_: Exception) {
            "Active"
        }
    }

    // ── Telegram Bot API Cloud Command Menu Sync ──────────────────────────────

    fun getOfficialBotCommands(): List<BotCommand> {
        return listOf(
            BotCommand("menu", "📱 Open interactive touchscreen control panel"),
            BotCommand("help", "📖 Show full help manual & command guide"),
            BotCommand("status", "📊 Live battery, storage, RAM & sensor telemetry"),
            BotCommand("auth", "🔓 Authenticate 15-minute administrative session"),
            BotCommand("logout", "🔒 Lock active administrative session"),
            BotCommand("selftest", "🩺 Run 9-point security, GPS & sensor audit"),
            BotCommand("info", "ℹ️ Hardware specs, SIM details & OS version"),
            BotCommand("locate", "📍 Acquire instant GPS fix & Google Maps pin"),
            BotCommand("tower", "📡 Cell tower triangulation & signal RF telemetry"),
            BotCommand("sim", "📶 Active SIM slots, carrier name & signal RF"),
            BotCommand("sim_lock", "🛡️ SIM swap guard & ICCID whitelist lock"),
            BotCommand("sim_tray_lock", "🔒 Cryptographic SIM tray lock (brick upon swap)"),
            BotCommand("sendsms", "✉️ Send outbound SMS directly via SIM slot"),
            BotCommand("call", "📞 Remotely place outbound cellular phone call"),
            BotCommand("track", "🛰️ Start or stop continuous live GPS tracking"),
            BotCommand("geofence", "🌐 Configure safe zone radius & breach alerts"),
            BotCommand("snap", "📸 Capture stealth photo (front, rear, or both)"),
            BotCommand("livestream", "🔴 Stream near-live camera video to Telegram"),
            BotCommand("stopstream", "⏹️ Stop active camera live stream"),
            BotCommand("screenshot", "📱 Silent full-screen capture via Accessibility"),
            BotCommand("screen_burst", "🎞️ Rapid 5-10 frame montage of intruder activity"),
            BotCommand("screenrecord", "🎥 Covert HD MP4 screen recording (5-60s)"),
            BotCommand("video", "📹 Record stealth camera video (1-60s)"),
            BotCommand("record", "🎙️ Record ambient microphone audio clip"),
            BotCommand("clipboard", "📋 Read current device clipboard text"),
            BotCommand("lock", "🔒 Lock screen with custom PIN & emergency banner"),
            BotCommand("lock_message", "💬 Set urgent alert message on lockscreen"),
            BotCommand("set_os_pin", "🔐 Overwrite hardware OS lockscreen PIN (Device Owner)"),
            BotCommand("set_master_pin", "🔑 Set cryptographic master PIN for remote control"),
            BotCommand("escrow", "🔐 Arm or check Knox hardware escrow password token"),
            BotCommand("duress_pin", "🆘 Set decoy coercion PIN for emergency SOS"),
            BotCommand("unlock", "🔓 Dismiss Lost Mode & unlock device screen"),
            BotCommand("fakeshutdown", "🕶️ Fake shutdown: blackout screen & silent traps"),
            BotCommand("wake", "☀️ Restore device from Fake Shutdown blackout"),
            BotCommand("ring", "🚨 Trigger or stop maximum volume emergency siren"),
            BotCommand("vibrate_pulse", "📳 Locate device silently via tactile vibrations"),
            BotCommand("trap", "🛡️ Arm sensor traps (snatch, charger, pocket)"),
            BotCommand("pattern_guard", "👁️ Failed pattern/PIN intrusion monitor & mugshot"),
            BotCommand("app_firewall", "🧱 Block RAT & spyware network outbound telemetry"),
            BotCommand("battery_alert", "🔋 Monitor abnormal drain & charging disconnects"),
            BotCommand("a11y_shield", "🛡️ Accessibility Trojan shield & auto-defense"),
            BotCommand("usb_autolock", "🔌 Locked-state USB killswitch (Cellebrite blocker)"),
            BotCommand("anti_2g", "📡 Anti-2G / IMSI-Catcher Stingray shield"),
            BotCommand("clipper_guard", "🪙 Crypto address clipboard hijacking trap"),
            BotCommand("app_install_lock", "🚫 Sideload & unauthorized APK install lockdown"),
            BotCommand("canary_guard", "🪤 Ransomware canary tripwire honeypot guard"),
            BotCommand("otp_guard", "🛡️ 2FA / OTP notification interception guard"),
            BotCommand("deadman", "💀 Anti-forensic dead man's switch timer"),
            BotCommand("thermal", "🔥 Thermal anomaly trap (anti-EDL/heat gun)"),
            BotCommand("harden_boot", "🔒 Lock recovery mode & prevent unauthorized reset"),
            BotCommand("tamper_detect", "🔍 Scan for root, debuggers, hooks & emulators"),
            BotCommand("device_owner", "👑 Check Device Owner & Kiosk hardware lock"),
            BotCommand("antitamper", "🛡️ Safe boot, airplane mode & factory reset lock"),
            BotCommand("usb_lock", "🔌 Cut USB data signaling pins (charge only)"),
            BotCommand("camera_lock", "📷 Hardware camera killswitch (anti-spy lockout)"),
            BotCommand("bluetooth_lock", "📡 Hardware Bluetooth & sharing killswitch"),
            BotCommand("mic_mute", "🔇 Hardware master audio mute (HAL level)"),
            BotCommand("lockscreen_info", "📱 Pin contact/recovery info to OS lockscreen"),
            BotCommand("autolock", "⏱️ Enforce screen inactivity autolock timeout"),
            BotCommand("wifi_connect", "📶 Emergency Wi-Fi auto-provisioning while locked"),
            BotCommand("security_audit", "📑 Inspect kernel OS security audit logs"),
            BotCommand("notification", "🔕 Permanent notification drawer suppression"),
            BotCommand("self_heal", "✨ Permanently lock app permissions as managed"),
            BotCommand("freeze", "🧊 Vanish banking & private apps into shadow vault"),
            BotCommand("unfreeze", "🔥 Restore hidden applications to launcher"),
            BotCommand("frozen", "📦 List currently frozen shadow vault apps"),
            BotCommand("lock_app", "🧊 Freeze gallery, phone, files, or sensitive app"),
            BotCommand("unlock_app", "☀️ Restore locked/hidden application"),
            BotCommand("biometrics", "🚫 Biometric coercion killswitch (forces Master PIN)"),
            BotCommand("dns", "🛡️ Enforce system-wide Private DNS-over-TLS"),
            BotCommand("reboot", "🔄 Remotely restart phone hardware (Device Owner)"),
            BotCommand("stealth", "👁️ Hide or restore PASA app icon in launcher"),
            BotCommand("contacts", "👥 Search device address book contacts"),
            BotCommand("call_log", "📞 View incoming and outgoing call history"),
            BotCommand("sms_log", "💬 View recent SMS inbox messages"),
            BotCommand("history", "📜 View recent command audit execution trail"),
            BotCommand("network", "🌐 Current IP, Wi-Fi SSID & cell carrier info"),
            BotCommand("apps", "📦 List installed applications"),
            BotCommand("app_uninstall", "❌ Silently uninstall package (Device Owner)"),
            BotCommand("gallery_latest", "🖼️ Extract recent photos from camera roll"),
            BotCommand("getfile", "📁 Download file from storage directly to Telegram"),
            BotCommand("list_files", "📂 Browse files in device storage directory"),
            BotCommand("shred", "🗑️ Cryptographically shred sensitive files"),
            BotCommand("smssetup", "📲 Enroll TOTP for secure offline SMS commands"),
            BotCommand("sms_help", "📲 Air-gapped cellular SMS command manual & cheat sheet"),
            BotCommand("license", "🔑 Check Pro license status or activate key"),
            BotCommand("check_update", "🔄 Check for OTA app updates"),
            BotCommand("message", "📢 Display urgent fullscreen alert on device"),
            BotCommand("dead_drop", "☁️ Backup evidence to encrypted local/cloud vault"),
            BotCommand("wipe", "⚠️ Emergency remote factory reset (requires auth)"),
            BotCommand("pause", "⏸️ Suspend all PASA monitoring activities (dormant mode)"),
            BotCommand("resume", "▶️ Wake PASA from dormant mode — restore full operation"),
            BotCommand("retire", "🗑️ Securely decommission PASA and remove Device Owner")
        )
    }

    suspend fun syncBotCommands(telegramApi: TelegramApi, force: Boolean = false): Boolean {
        val token = preferencesManager.botToken
        if (token.isBlank()) return false
        val lastSync = preferencesManager.lastBotCommandsSyncTimestamp
        val now = System.currentTimeMillis()
        if (!force && now - lastSync < 24 * 60 * 60 * 1000L) {
            return true
        }
        return try {
            val commands = getOfficialBotCommands()
            val resp = telegramApi.setMyCommands(token, SetMyCommandsRequest(commands))
            if (resp.ok) {
                preferencesManager.lastBotCommandsSyncTimestamp = now
                android.util.Log.i("PASA_Menu", "Successfully synced ${commands.size} official bot commands to Telegram cloud menu.")
                true
            } else {
                android.util.Log.w("PASA_Menu", "Failed to sync bot commands: ${resp.description}")
                false
            }
        } catch (e: Exception) {
            android.util.Log.w("PASA_Menu", "Exception syncing bot commands: ${e.message}")
            false
        }
    }
}

