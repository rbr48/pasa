package com.izhaanintellect.pasa.ui

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast

import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.izhaanintellect.pasa.R
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.databinding.ActivitySetupBinding
import com.izhaanintellect.pasa.service.PasaService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Setup and initialization activity for PASA.
 * Guides administrator through bot credentials, device admin authorization, and permissions.
 */
@AndroidEntryPoint
class SetupActivity : AppCompatActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var ringCommand: com.izhaanintellect.pasa.commands.RingCommand
    @Inject lateinit var screenshotManager: com.izhaanintellect.pasa.camera.ScreenshotManager

    private lateinit var binding: ActivitySetupBinding
    private val viewModel: SetupViewModel by viewModels()

    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateUI()
        if (binding.layoutDashboard.visibility == android.view.View.VISIBLE) {
            updateDashboardUI()
        }
    }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        updateUI()
        if (binding.layoutDashboard.visibility == android.view.View.VISIBLE) {
            updateDashboardUI()
        }
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        updateUI()
        if (binding.layoutDashboard.visibility == android.view.View.VISIBLE) {
            updateDashboardUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
        setupDashboardObservers()

        binding.etServerUrl.setText(viewModel.getSavedServerUrl().ifBlank { PreferencesManager.DEFAULT_SERVER_URL })
        if (preferencesManager.botToken.isNotBlank()) {
            binding.etBotToken.setText(preferencesManager.botToken)
        } else {
            binding.etBotToken.hint = "Enter your Telegram Bot Token"
        }

        if (preferencesManager.isSetupComplete) {
            PasaService.start(this)
            showDashboard()
        } else {
            showSetupWizard()
        }
    }

    private fun setupClickListeners() {
        // Developer easter egg: 5 taps on logo reveals/hides custom VPS Control Plane setup
        var logoTapCount = 0
        var lastLogoTapTime = 0L
        binding.ivLogo.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastLogoTapTime > 2000) {
                logoTapCount = 1
            } else {
                logoTapCount++
            }
            lastLogoTapTime = now
            if (logoTapCount >= 5) {
                logoTapCount = 0
                val isVisible = binding.cardVpsSetup.visibility == android.view.View.VISIBLE
                binding.cardVpsSetup.visibility = if (isVisible) android.view.View.GONE else android.view.View.VISIBLE
                val msg = if (!isVisible) "⚙️ Advanced VPS Gateway settings revealed" else "🔒 VPS settings hidden (using default cloud gateway)"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        // Test VPS Backend Connection
        binding.btnTestServer.setOnClickListener {
            val serverUrl = binding.etServerUrl.text.toString().trim()
            if (serverUrl.isBlank()) {
                binding.tilServerUrl.error = "Server URL cannot be empty"
                return@setOnClickListener
            }
            binding.tilServerUrl.error = null
            binding.btnTestServer.isEnabled = false
            binding.btnTestServer.text = "Checking..."

            lifecycleScope.launch {
                val result = viewModel.testServerConnection(serverUrl)
                binding.btnTestServer.isEnabled = true
                binding.btnTestServer.text = "Verify VPS Connection"

                result.onSuccess { msg ->
                    Toast.makeText(this@SetupActivity, "✅ $msg", Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(this@SetupActivity, "❌ VPS Error: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Test Bot Token Connection
        binding.btnTestConnection.setOnClickListener {
            val token = binding.etBotToken.text.toString().trim()
            if (token.isBlank()) {
                binding.tilBotToken.error = getString(R.string.error_empty_token)
                return@setOnClickListener
            }
            binding.tilBotToken.error = null
            binding.btnTestConnection.isEnabled = false
            binding.btnTestConnection.text = "Verifying..."

            lifecycleScope.launch {
                val result = viewModel.testBotConnection(token)
                binding.btnTestConnection.isEnabled = true
                binding.btnTestConnection.text = "Verify Bot Connection"

                result.onSuccess { botName ->
                    Toast.makeText(this@SetupActivity, "✅ Connected to @$botName", Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(this@SetupActivity, "❌ Connection failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Activate Device Admin
        binding.btnActivateAdmin.setOnClickListener {
            val componentName = PasaDeviceAdmin.getComponentName(this)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.device_admin_description)
                )
            }
            deviceAdminLauncher.launch(intent)
        }

        // Grant Permissions
        binding.btnGrantPermissions.setOnClickListener {
            val permissions = mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            permissionsLauncher.launch(permissions.toTypedArray())
        }

        // Disable Battery Optimization
        binding.btnBatteryOptimization.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        startActivity(intent)
                    } catch (e2: Exception) {
                        Toast.makeText(this, "Please allow unrestricted battery in Settings > Apps > PASA", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Display Over Other Apps (Overlay for Lockscreen Stealth Capture)
        binding.btnOverlayPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Enable 'Allow display over other apps' in Settings", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Enable Accessibility Service (Screenshots / Silent Video)
        binding.btnAccessibilityPermission.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, "Enable 'PASA Sentinel' in Downloaded / Installed apps", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Open Settings > Accessibility > PASA Sentinel", Toast.LENGTH_LONG).show()
            }
        }

        // Enable OEM Autostart & Anti-Kill Whitelist
        binding.btnOemAutostart.setOnClickListener {
            com.izhaanintellect.pasa.util.OemProtectionHelper.openOemAutostartSettings(this)
        }

        // Read Terms & Conditions Link
        binding.tvReadTermsLink.setOnClickListener {
            showTermsDialog()
        }

        // Method 2: Instant Pairing via @Pas_agent_bot
        binding.btnQuickPair.setOnClickListener {
            if (!binding.cbAcceptTerms.isChecked) {
                Toast.makeText(this, getString(R.string.terms_required_error), Toast.LENGTH_LONG).show()
                binding.cbAcceptTerms.requestFocus()
                return@setOnClickListener
            }
            showSovereignAdvisoryDialog()
        }

        // Activate Button
        binding.btnActivate.setOnClickListener {
            activatePasa()
        }
    }


    private fun activatePasa() {
        if (!binding.cbAcceptTerms.isChecked) {
            Toast.makeText(this, getString(R.string.terms_required_error), Toast.LENGTH_LONG).show()
            binding.cbAcceptTerms.requestFocus()
            return
        }

        val serverUrl = binding.etServerUrl.text.toString().trim().ifBlank { PreferencesManager.DEFAULT_SERVER_URL }
        val botToken = binding.etBotToken.text.toString().trim()
        val chatId = binding.etChatId.text.toString().trim()
        val password = binding.etMasterPassword.text.toString()
        val email = binding.etBackupEmail.text.toString().trim()
        val stealthMode = binding.switchStealth.isChecked

        binding.tilServerUrl.error = null

        if (botToken.isEmpty()) {
            binding.tilBotToken.error = getString(R.string.error_empty_token)
            return
        }
        binding.tilBotToken.error = null

        if (chatId.isEmpty() || chatId.toLongOrNull() == null) {
            binding.tilChatId.error = getString(R.string.error_invalid_chat_id)
            return
        }
        binding.tilChatId.error = null

        if (password.length < 8) {
            binding.tilMasterPassword.error = getString(R.string.error_short_password)
            return
        }
        binding.tilMasterPassword.error = null

        if (email.isEmpty()) {
            binding.tilBackupEmail.error = getString(R.string.error_empty_email)
            return
        }
        binding.tilBackupEmail.error = null

        val saved = viewModel.validateAndSave(botToken, chatId, password, email, stealthMode, serverUrl)
        if (!saved) {
            Toast.makeText(this, "Configuration validation failed", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnActivate.isEnabled = false
        binding.btnActivate.text = "Activating..."

        lifecycleScope.launch {
            // Register with VPS backend (and generate hardware-backed StrongBox/TEE key)
            viewModel.registerDeviceWithBackend(applicationContext)

            // Deliver activation confirmation
            viewModel.sendSetupConfirmation()

            // Start guardian background service
            PasaService.start(this@SetupActivity)

            if (stealthMode) {
                val componentName = ComponentName(this@SetupActivity, SetupActivity::class.java)
                packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            val modeToast = if (preferencesManager.useBackendServer) "Linked to VPS Gateway" else "Direct Telegram Sovereign Mode"
            Toast.makeText(
                this@SetupActivity,
                "🛡️ PASA Guardian Active ($modeToast)",
                Toast.LENGTH_LONG
            ).show()

            binding.btnActivate.isEnabled = true
            binding.btnActivate.text = getString(R.string.btn_activate)
            showDashboard()
        }
    }

    override fun onResume() {
        super.onResume()
        if (binding.layoutDashboard.visibility == android.view.View.VISIBLE) {
            updateDashboardUI()
        } else {
            updateUI()
        }
    }

    private fun showDashboard() {
        binding.layoutSetupWizard.visibility = android.view.View.GONE
        binding.layoutDashboard.visibility = android.view.View.VISIBLE
        updateDashboardUI()
    }

    private fun showSetupWizard() {
        binding.layoutDashboard.visibility = android.view.View.GONE
        binding.layoutSetupWizard.visibility = android.view.View.VISIBLE
        updateUI()
    }

    private fun setupDashboardObservers() {
        // Observe WorkManager pending uploads
        lifecycleScope.launch {
            viewModel.pendingUploadsFlow.collect { uploads ->
                val pendingCount = uploads.count { it.status != "COMPLETED" }
                binding.tvDashVaultQueue.text = "📦 WorkManager Queue: $pendingCount pending uploads (${uploads.size} total in log)"
            }
        }

        // Observe command execution audit trail
        lifecycleScope.launch {
            viewModel.commandLogsFlow.collect { logs ->
                if (logs.isEmpty()) {
                    binding.tvDashAuditLogs.text = "No commands executed yet."
                } else {
                    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    binding.tvDashAuditLogs.text = logs.take(8).joinToString("\n") { log ->
                        val timeStr = sdf.format(java.util.Date(log.timestamp))
                        val statusEmoji = if (log.status == "SUCCESS") "✅" else "❌"
                        "[$timeStr] $statusEmoji ${log.command} (${log.status})"
                    }
                }
            }
        }

        // Dashboard buttons
        binding.btnDashActivateAdmin.setOnClickListener {
            val componentName = PasaDeviceAdmin.getComponentName(this)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.device_admin_description)
                )
            }
            deviceAdminLauncher.launch(intent)
        }

        binding.btnDashBatteryWhitelist.setOnClickListener {
            com.izhaanintellect.pasa.util.BatteryOptimizationHelper.openOemBackgroundSettings(this)
            updateDashboardUI()
        }

        binding.btnDashFlushVault.setOnClickListener {
            viewModel.flushUploadQueue()
            Toast.makeText(this, "⚡ WorkManager upload queue flushed", Toast.LENGTH_SHORT).show()
        }

        binding.btnDashEmergencyBeacon.setOnClickListener {
            lifecycleScope.launch {
                val res = ringCommand.execute(listOf("15"), 0L)
                Toast.makeText(this@SetupActivity, res.message, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDashRestartService.setOnClickListener {
            viewModel.restartGuardianService()
            Toast.makeText(this, "🔄 Guardian service restarted", Toast.LENGTH_SHORT).show()
            updateDashboardUI()
        }

        binding.btnDashReconfigure.setOnClickListener {
            promptReconfigure()
        }

        binding.btnDashAccessibility.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(this, "Toggle ON 'PASA Sentinel'", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Open Settings > Accessibility > PASA Sentinel", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnDashOemAutostart.setOnClickListener {
            com.izhaanintellect.pasa.util.OemProtectionHelper.openOemAutostartSettings(this)
        }
    }

    private fun updateDashboardUI() {
        val deviceId = viewModel.getSavedDeviceId()
        binding.tvDashDeviceId.text = "Device ID: $deviceId"
        binding.tvDashBackend.text = "Gateway: ${viewModel.getBackendMode()} (${viewModel.getSavedServerUrl()})"
        binding.tvDashFgs.text = "Service: Persistent FGS (DataSync/Location) | WakeLock: Active"

        val isAdmin = viewModel.isDeviceAdmin()
        if (isAdmin) {
            binding.tvDashAdminStatus.text = "🛡️ Device Admin: Active (Remote Lock/Wipe Ready)"
            binding.btnDashActivateAdmin.visibility = android.view.View.GONE
        } else {
            binding.tvDashAdminStatus.text = "⚠️ Device Admin: Inactive (Remote Lock Disabled)"
            binding.btnDashActivateAdmin.visibility = android.view.View.VISIBLE
        }

        val isOwner = viewModel.isDeviceOwner()
        if (isOwner) {
            val isTokenActive = PasaDeviceAdmin.isResetPasswordTokenActive(this)
            val tokenStatus = if (isTokenActive) "✅ Remote OS PIN Reset: Armed & Ready" else "⚠️ Remote OS PIN Reset: Pending (Tap to Arm)"
            binding.tvDashOwnerStatus.text = "👑 Device Owner: Active (Hardware Lockdown Active)\n$tokenStatus"
            binding.tvDashOwnerStatus.setOnClickListener {
                if (!isTokenActive) {
                    startActivity(EscrowActivationActivity.createIntent(this))
                }
            }
        } else {
            binding.tvDashOwnerStatus.text = "ℹ️ Device Owner: Standard Admin (Elevate via ADB: dpm set-device-owner ...)"
        }

        val isBatteryWhitelisted = viewModel.isBatteryWhitelisted()
        if (isBatteryWhitelisted) {
            binding.tvDashBatteryStatus.text = "⚡ Battery: Unrestricted (Whitelisted against OEM kill)"
            binding.btnDashBatteryWhitelist.text = "✅ Battery Whitelisted (${viewModel.getManufacturer()})"
        } else {
            binding.tvDashBatteryStatus.text = "⚠️ Battery: Optimized (Risk of OEM background termination: ${viewModel.getManufacturer()})"
            binding.btnDashBatteryWhitelist.text = "Whitelist Battery & Auto-Start (${viewModel.getManufacturer()})"
        }

        binding.tvDashKeystore.text = "🔐 Hardware Keystore: ${viewModel.getSecurityLevel()} hardware key"

        val hasA11y = screenshotManager.isAccessibilityServiceEnabled()
        if (hasA11y) {
            binding.tvDashAccessibility.text = "👁️ Screen Capture: ✅ Accessibility Active (Screenshots & Video Ready)"
            binding.btnDashAccessibility.visibility = android.view.View.GONE
        } else {
            binding.tvDashAccessibility.text = "👁️ Screen Capture: ❌ Disabled (Enable to record screen without ADB)"
            binding.btnDashAccessibility.visibility = android.view.View.VISIBLE
        }
    }

    private fun promptReconfigure() {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Enter Master Password"
            setPadding(40, 30, 40, 30)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Administrator Authentication")
            .setMessage("Please enter your Master Password to unlock Sentinel settings:")
            .setView(input)
            .setPositiveButton("Unlock") { _, _ ->
                val entered = input.text.toString()
                if (viewModel.verifyMasterPassword(entered)) {
                    Toast.makeText(this, "✅ Administrator Authenticated", Toast.LENGTH_SHORT).show()
                    showSetupWizard()
                } else {
                    Toast.makeText(this, "❌ Invalid Master Password", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateUI() {
        val isAdminActive = PasaDeviceAdmin.isAdminActive(this)
        val hasPermissions = hasRequiredPermissions()
        val isBatteryIgnored = isBatteryOptimizationIgnored()
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true

        binding.btnActivateAdmin.isEnabled = !isAdminActive
        binding.btnActivateAdmin.text = if (isAdminActive) "✅ Device Admin Active" else getString(R.string.btn_activate_admin)

        binding.btnGrantPermissions.isEnabled = !hasPermissions
        binding.btnGrantPermissions.text = if (hasPermissions) "✅ Permissions Granted" else getString(R.string.btn_grant_permissions)

        binding.btnBatteryOptimization.isEnabled = !isBatteryIgnored
        binding.btnBatteryOptimization.text = if (isBatteryIgnored) getString(R.string.btn_battery_optimization_done) else getString(R.string.btn_battery_optimization)

        binding.btnOverlayPermission.isEnabled = !hasOverlay
        binding.btnOverlayPermission.text = if (hasOverlay) "✅ Overlay & Stealth Capture Allowed" else "Allow Display Over Other Apps"

        val hasA11y = screenshotManager.isAccessibilityServiceEnabled()
        binding.btnAccessibilityPermission.isEnabled = !hasA11y
        binding.btnAccessibilityPermission.text = if (hasA11y) "✅ Accessibility Service Active" else "Enable Accessibility Service (Screenshots/Video)"

        binding.btnActivate.isEnabled = isAdminActive && hasPermissions

        binding.tvStatus.text = when {
            !isAdminActive -> getString(R.string.status_admin_required)
            !hasPermissions -> getString(R.string.status_permissions_required)
            !isBatteryIgnored -> "Recommended: Disable battery optimization for continuous protection"
            !hasA11y -> "Recommended: Enable Accessibility Service for silent screenshots & video"
            else -> getString(R.string.status_ready)
        }
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            return pm.isIgnoringBatteryOptimizations(packageName)
        }
        return true
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
        return permissions.all {
            checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun showTermsDialog() {
        val termsText = """
            🛡️ PASA SENTINEL — TERMS OF SERVICE & EULA
            Effective Date: September 20, 2026

            1. LAWFUL OWNERSHIP & ZERO STALKERWARE
            You represent and warrant that you are the sole lawful owner of this physical device. Deploying PASA Sentinel without the informed consent of the device user is strictly prohibited and violates international cybercrime statutes.

            2. IRREVERSIBLE EMERGENCY ACTIONS
            Remote Wipe (/wipe), Cryptographic Shredding (/shred), and Knox Hardware Lockdown (/lock) are intentionally destructive countermeasures. The developer disclaims all liability for data loss or hardware lockout.

            3. FORENSIC TELEMETRY & PRIVACY
            PASA Sentinel captures camera snapshots, ambient audio, and satellite GPS coordinates for theft recovery. You are solely responsible for compliance with local two-party recording laws.

            4. SOVEREIGN DEFENSE ARCHITECTURE
            Method 1 (Private Bot via @BotFather) provides 100% sovereign, zero-trust defense. Method 2 (Instant Pairing) routes through our managed control plane.

            Full legal terms are published at:
            https://pasa.izhaanintellect.fun/terms
        """.trimIndent()

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("📜 Terms of Service & EULA")
            .setMessage(termsText)
            .setPositiveButton("I Agree & Certify Ownership") { _, _ ->
                binding.cbAcceptTerms.isChecked = true
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showSovereignAdvisoryDialog() {
        val advisoryMsg = "PASA Sentinel provides two Command & Control architectures:\n\n" +
                "🛡️ Method 1: Private Bot via @BotFather (Recommended)\n" +
                "• 100% sovereign, zero-trust defense.\n" +
                "• You hold your private bot token.\n" +
                "• Completely isolated from all third parties.\n\n" +
                "⚡ Method 2: Instant Pairing via @Pas_agent_bot\n" +
                "• Instant setup with a 6-digit one-time code.\n" +
                "• Protected by rate limits and device-binding OTP.\n" +
                "• Commands route through our hardened VPS gateway.\n\n" +
                "For high-threat models and sovereign autonomy, Method 1 is strongly recommended."

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("🛡️ Sovereign Security Advisory")
            .setMessage(advisoryMsg)
            .setPositiveButton("Use Method 1 (Sovereign)") { _, _ ->
                binding.tilBotToken.requestFocus()
                Toast.makeText(this, "Enter your private bot token from @BotFather below", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Proceed with Instant Pair") { _, _ ->
                startInstantPairingWorkflow()
            }
            .show()
    }

    private var pairingJob: kotlinx.coroutines.Job? = null

    private fun startInstantPairingWorkflow() {
        val serverUrl = binding.etServerUrl.text.toString().trim().ifBlank { PreferencesManager.DEFAULT_SERVER_URL }
        val deviceId = preferencesManager.deviceId
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"

        val progressDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("⚡ Initializing Pairing Code")
            .setMessage("Contacting PASA Control Plane...")
            .setCancelable(false)
            .show()

        lifecycleScope.launch {
            try {
                val initRes = viewModel.initPairing(serverUrl, deviceId, deviceName)
                progressDialog.dismiss()

                if (!initRes.ok || initRes.code == null) {
                    Toast.makeText(this@SetupActivity, "❌ Pairing initialization failed: ${initRes.message ?: "Server error"}", Toast.LENGTH_LONG).show()
                    return@launch
                }

                showPairingCodeDialog(
                    serverUrl = serverUrl,
                    code = initRes.code,
                    formattedCode = initRes.formattedCode ?: initRes.code,
                    expiresInSec = initRes.expiresInSeconds ?: 180,
                    botUsername = initRes.botUsername ?: "Pas_agent_bot"
                )
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@SetupActivity, "❌ Connection error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showPairingCodeDialog(
        serverUrl: String,
        code: String,
        formattedCode: String,
        expiresInSec: Int,
        botUsername: String
    ) {
        val dialogView = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        val tvInstruction = android.widget.TextView(this).apply {
            text = "Send this 6-digit one-time code to @$botUsername in Telegram to instantly pair this device:"
            setTextColor(android.graphics.Color.parseColor("#CBD5E1"))
            textSize = 14f
            gravity = android.view.Gravity.CENTER
        }

        val tvCode = android.widget.TextView(this).apply {
            text = formattedCode
            textSize = 36f
            typeface = android.graphics.Typeface.MONOSPACE
            paint.isFakeBoldText = true
            setTextColor(android.graphics.Color.parseColor("#38BDF8"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 30, 0, 20)
            setTextIsSelectable(true)
        }

        val tvTimer = android.widget.TextView(this).apply {
            text = "⏳ Code expires in ${expiresInSec}s"
            setTextColor(android.graphics.Color.parseColor("#94A3B8"))
            textSize = 12f
            gravity = android.view.Gravity.CENTER
        }

        val btnOpenTg = com.google.android.material.button.MaterialButton(this).apply {
            text = "✈️ Open @$botUsername in Telegram"
            setBackgroundColor(android.graphics.Color.parseColor("#0284C7"))
            setOnClickListener {
                try {
                    val tgIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$botUsername?start=pair_$code"))
                    startActivity(tgIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Please open Telegram and message @$botUsername", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val tvWaiting = android.widget.TextView(this).apply {
            text = "📡 Listening for Telegram pairing confirmation..."
            setTextColor(android.graphics.Color.parseColor("#22C55E"))
            textSize = 12f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 10)
        }

        dialogView.addView(tvInstruction)
        dialogView.addView(tvCode)
        dialogView.addView(tvTimer)
        dialogView.addView(btnOpenTg)
        dialogView.addView(tvWaiting)

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("⚡ Instant Device Pairing")
            .setView(dialogView)
            .setNegativeButton("Cancel") { d, _ ->
                pairingJob?.cancel()
                d.dismiss()
            }
            .setCancelable(false)
            .create()

        dialog.show()

        // Polling loop
        pairingJob?.cancel()
        pairingJob = lifecycleScope.launch {
            var remaining = expiresInSec
            while (remaining > 0) {
                kotlinx.coroutines.delay(2500)
                remaining -= 2
                val min = remaining / 60
                val sec = remaining % 60
                tvTimer.text = "⏳ Code expires in %d:%02d".format(min, sec)

                try {
                    val statusRes = viewModel.pollPairingStatus(code)
                    if (statusRes.ok && statusRes.status == "CLAIMED" && !statusRes.ownerChatId.isNullOrBlank()) {
                        tvWaiting.text = "✅ PAIRED! Activating PASA Guardian..."
                        kotlinx.coroutines.delay(1000)
                        dialog.dismiss()

                        // Auto-populate credentials
                        binding.etBotToken.setText(statusRes.botToken ?: "8815969412:AAEN_BqiCldZVza93qApCbGn5hTrcAW9HxA")
                        binding.etChatId.setText(statusRes.ownerChatId)
                        if (binding.etMasterPassword.text.isNullOrBlank()) {
                            binding.etMasterPassword.setText("Pasa@" + (1000..9999).random())
                        }
                        if (binding.etBackupEmail.text.isNullOrBlank()) {
                            binding.etBackupEmail.setText("recovery@pasa.izhaanintellect.fun")
                        }

                        // Activate
                        activatePasa()
                        break
                    } else if (statusRes.status == "EXPIRED") {
                        tvTimer.text = "❌ Code Expired"
                        tvWaiting.text = "Please generate a new code."
                        break
                    }
                } catch (_: Exception) {}
            }
        }
    }
}


