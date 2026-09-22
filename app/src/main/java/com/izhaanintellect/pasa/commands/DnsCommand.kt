package com.izhaanintellect.pasa.commands

import android.content.Context
import android.os.Build
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enforces system-wide encrypted Private DNS (DNS-over-TLS) via Device Owner.
 * Neutralizes ISP surveillance, cell carrier query logging, and evil-twin Wi-Fi redirection.
 */
@Singleton
class DnsCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/dns"
    override val description = "Lock system-wide encrypted DNS-over-TLS [Device Owner / Android 10+]"
    override val usage = "/dns [quad9|cloudflare|adguard|off|<custom_host>|status]"

    companion object {
        private val POPULAR_PROVIDERS = mapOf(
            "quad9" to "dns.quad9.net",
            "cloudflare" to "one.one.one.one",
            "adguard" to "dns.adguard-dns.com",
            "google" to "dns.google"
        )
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Device Owner Required:</b> System Private DNS enforcement requires Device Owner.\n" +
                        "Run <code>/device_owner</code> for activation instructions."
            )
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return CommandResult(
                success = false,
                message = "❌ <b>Android 10+ Required:</b> Global Private DNS controls require Android 10+ (API 29+).\n" +
                        "Your device is running Android ${Build.VERSION.RELEASE}."
            )
        }

        val param = args.firstOrNull()?.lowercase() ?: "status"

        return when (param) {
            "status" -> {
                val (mode, host) = PasaDeviceAdmin.getGlobalPrivateDns(context)
                val modeStr = when (mode) {
                    3 -> "🔒 <b>SPECIFIED HOST (Strict Encrypted DoT)</b>\n• Host: <code>$host</code>"
                    2 -> "⚡ <b>OPPORTUNISTIC (Automatic)</b>"
                    1 -> "⚠️ <b>OFF</b>"
                    else -> "Unknown / Default"
                }

                CommandResult(
                    success = true,
                    message = "🌐 <b>System Private DNS Status</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            modeStr + "\n\n" +
                            "💡 <i>Providers:</i>\n" +
                            "• <code>/dns quad9</code> (Quad9 Privacy & Threat Block)\n" +
                            "• <code>/dns cloudflare</code> (Cloudflare 1.1.1.1 Fast)\n" +
                            "• <code>/dns adguard</code> (AdGuard Ad & Tracker Block)\n" +
                            "• <code>/dns off</code> (Restore automatic mode)"
                )
            }

            "off", "auto", "opportunistic" -> {
                val (ok, text) = PasaDeviceAdmin.setGlobalPrivateDns(context, "off")
                CommandResult(ok, text)
            }

            else -> {
                val targetHost = POPULAR_PROVIDERS[param] ?: param
                val (ok, text) = PasaDeviceAdmin.setGlobalPrivateDns(context, targetHost)
                CommandResult(ok, text)
            }
        }
    }
}
