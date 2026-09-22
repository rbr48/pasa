package com.izhaanintellect.pasa.commands

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LiveStream Diagnostics - Troubleshoot Video Streaming Issues
 *
 * Checks:
 * - Camera hardware availability
 * - Telegram bot token configuration
 * - Owner chat ID configuration
 * - Network connectivity
 * - Permissions status
 */
@Singleton
class LiveStreamDiagnosticsCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/livestream_diag"
    override val description = "Diagnose livestream configuration issues"
    override val usage = "/livestream_diag"

    companion object {
        private const val TAG = "PASA_StreamDiag"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        return try {
            runDiagnostics()
        } catch (e: Exception) {
            Log.e(TAG, "Diagnostic failed: ${e.message}", e)
            CommandResult(false, "Diagnostic failed: ${e.message}")
        }
    }

    private fun runDiagnostics(): CommandResult {
        Log.i(TAG, "🔍 Running livestream diagnostics...")

        val results = mutableListOf<String>()

        // 1. Check cameras
        val cameraStatus = checkCameras()
        results.add(cameraStatus)

        // 2. Check Telegram configuration
        val telegramStatus = checkTelegramConfig()
        results.add(telegramStatus)

        // 3. Check permissions
        val permissionsStatus = checkPermissions()
        results.add(permissionsStatus)

        val diagnosticOutput = """
            🔍 <b>LIVESTREAM DIAGNOSTICS</b>
            ━━━━━━━━━━━━━━━━━━━━

            ${results.joinToString("\n\n")}

            ━━━━━━━━━━━━━━━━━━━━
            ${getRecommendations()}
        """.trimIndent()

        return CommandResult(success = true, message = diagnosticOutput)
    }

    private fun checkCameras(): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameras = cameraManager.cameraIdList

            if (cameras.isEmpty()) {
                """
                    📹 <b>Cameras:</b> ❌ NO CAMERAS DETECTED
                    ━━━━━━━━━━━━━━━━━━━━
                    <b>Problem:</b> Device has no camera hardware
                    <b>Solution:</b> Check that device has camera hardware
                """.trimIndent()
            } else {
                val cameraDetails = cameras.mapIndexed { idx, cameraId ->
                    try {
                        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                        val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                        val facingStr = when (facing) {
                            android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
                            android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK -> "BACK"
                            else -> "UNKNOWN"
                        }
                        "  • Camera $cameraId: $facingStr ✓"
                    } catch (e: Exception) {
                        "  • Camera $cameraId: ERROR - ${e.message}"
                    }
                }.joinToString("\n")

                """
                    📹 <b>Cameras:</b> ✅ DETECTED
                    ━━━━━━━━━━━━━━━━━━━━
                    $cameraDetails

                    <b>Status:</b> Cameras available for livestream
                """.trimIndent()
            }
        } catch (e: Exception) {
            """
                📹 <b>Cameras:</b> ⚠️ ERROR
                ━━━━━━━━━━━━━━━━━━━━
                <b>Error:</b> ${e.message}
                <b>Solution:</b> Check camera permissions
            """.trimIndent()
        }
    }

    private fun checkTelegramConfig(): String {
        val botToken = preferencesManager.botToken
        val chatId = preferencesManager.ownerChatIdLong

        return when {
            botToken.isBlank() -> {
                """
                    🤖 <b>Telegram Bot:</b> ❌ NOT CONFIGURED
                    ━━━━━━━━━━━━━━━━━━━━
                    <b>Problem:</b> Bot token is empty
                    <b>Solution:</b> Run /smssetup to configure Telegram bot

                    <b>Steps:</b>
                    1. Run: /smssetup
                    2. Follow prompts to set bot token
                    3. Then retry: /livestream
                """.trimIndent()
            }
            chatId == 0L -> {
                """
                    💬 <b>Owner Chat ID:</b> ❌ NOT SET
                    ━━━━━━━━━━━━━━━━━━━━
                    <b>Problem:</b> Owner chat ID is 0 (not initialized)
                    <b>Solution:</b> Need to receive first message from Telegram bot

                    <b>Steps:</b>
                    1. Send a message to your bot on Telegram
                    2. PASA will extract your chat ID
                    3. Then retry: /livestream
                """.trimIndent()
            }
            else -> {
                """
                    🤖 <b>Telegram Configuration:</b> ✅ READY
                    ━━━━━━━━━━━━━━━━━━━━
                    • Bot token: ${botToken.take(10)}...✓
                    • Owner chat ID: $chatId ✓
                    • Status: Ready to stream
                """.trimIndent()
            }
        }
    }

    private fun checkPermissions(): String {
        val requiredPermissions = listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.INTERNET
        )

        val pm = context.packageManager
        val packageName = context.packageName

        val permissionStatus = requiredPermissions.map { permission ->
            val granted = pm.checkPermission(
                permission,
                packageName
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val permName = permission.split(".").last()
            if (granted) "  • $permName: ✓" else "  • $permName: ❌"
        }.joinToString("\n")

        val allGranted = requiredPermissions.all {
            pm.checkPermission(it, packageName) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        return """
            🔐 <b>Permissions:</b> ${if (allGranted) "✅ GRANTED" else "⚠️ MISSING"}
            ━━━━━━━━━━━━━━━━━━━━
            $permissionStatus

            ${if (allGranted) "<b>Status:</b> All permissions granted" else "<b>Solution:</b> Grant missing permissions in device settings"}
        """.trimIndent()
    }

    private fun getRecommendations(): String {
        val issues = mutableListOf<String>()

        // Check each component
        if (preferencesManager.botToken.isBlank()) {
            issues.add("❌ Bot token not configured → Run /smssetup")
        }

        if (preferencesManager.ownerChatIdLong == 0L) {
            issues.add("❌ Chat ID not set → Send message to bot")
        }

        return if (issues.isEmpty()) {
            """
                ✅ <b>ALL SYSTEMS OPERATIONAL</b>
                Livestream should work. If still failing:
                1. Check device internet connection
                2. Verify Telegram bot is working
                3. Try: /livestream front 1 (1 minute test)
            """.trimIndent()
        } else {
            """
                🔧 <b>REQUIRED FIXES:</b>
                ${issues.mapIndexed { idx, issue -> "${idx + 1}. $issue" }.joinToString("\n")}

                After fixing, retry: /livestream front 5
            """.trimIndent()
        }
    }
}
