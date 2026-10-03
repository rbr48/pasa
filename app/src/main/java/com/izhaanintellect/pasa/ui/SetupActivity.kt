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
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast

import android.graphics.Color
import android.text.Html
import android.text.Spanned
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.izhaanintellect.pasa.R
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.databinding.ActivitySetupBinding
import com.izhaanintellect.pasa.security.AuthManager
import com.izhaanintellect.pasa.service.PasaService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
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
    @Inject lateinit var authManager: AuthManager

    private var isSessionAuthenticated = false
    private var isAuthenticating = false

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
        // Enforce FLAG_SECURE: Blocks OS recents screenshots, malicious screen recorders, and screen casts
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Programmatic IME Sanitization: Prevent third-party & Gboard learning dictionaries from caching credentials
        val noLearningFlag = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        binding.etBotToken.imeOptions = binding.etBotToken.imeOptions or noLearningFlag
        binding.etMasterPassword.imeOptions = binding.etMasterPassword.imeOptions or noLearningFlag
        binding.etChatId.imeOptions = binding.etChatId.imeOptions or noLearningFlag

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
        setupDashboardObservers()

        if (preferencesManager.botToken.isNotBlank()) {
            binding.etBotToken.setText(preferencesManager.botToken)
        } else {
            binding.etBotToken.hint = "Enter your Telegram Bot Token"
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (preferencesManager.isSetupComplete && binding.layoutSetupWizard.visibility == android.view.View.VISIBLE) {
                    showDashboard()
                } else {
                    finish()
                }
            }
        })

        if (preferencesManager.isSetupComplete && authManager.hasMasterPassword()) {
            PasaService.start(this)
            binding.layoutDashboard.visibility = android.view.View.GONE
            binding.layoutSetupWizard.visibility = android.view.View.GONE
            authenticateOwner(
                title = "PASA Sentinel Verification",
                subtitle = "Confirm identity to access Guardian Console",
                isStartup = true
            ) {
                showDashboard()
            }
        } else {
            showSetupWizard()
        }
    }

    private fun setupClickListeners() {
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

        // Mandatory Terms & Conditions Acceptance UX
        binding.cbAcceptTerms.isChecked = preferencesManager.hasAcceptedTerms
        binding.cbAcceptTerms.setOnClickListener {
            if (!preferencesManager.hasAcceptedTerms) {
                binding.cbAcceptTerms.isChecked = false
                showFullTermsDialog()
            } else {
                preferencesManager.hasAcceptedTerms = binding.cbAcceptTerms.isChecked
            }
        }

        binding.tvTermsAgreement.setOnClickListener {
            showFullTermsDialog()
        }

        binding.llTermsSection.setOnClickListener {
            if (!preferencesManager.hasAcceptedTerms) {
                showFullTermsDialog()
            }
        }

        binding.tvReadTermsLink.setOnClickListener {
            showFullTermsDialog()
        }

        // Activate Button
        binding.btnActivate.setOnClickListener {
            activatePasa()
        }
    }


    private fun activatePasa() {
        if (!preferencesManager.hasAcceptedTerms || !binding.cbAcceptTerms.isChecked) {
            Toast.makeText(this, "⚠️ You must review and accept the full Terms of Service & EULA before activation.", Toast.LENGTH_LONG).show()
            showFullTermsDialog()
            binding.cbAcceptTerms.requestFocus()
            return
        }

        val botToken = binding.etBotToken.text.toString().trim()
        val chatId = binding.etChatId.text.toString().trim()
        val password = binding.etMasterPassword.text.toString()
        val email = binding.etBackupEmail.text.toString().trim()
        val stealthMode = binding.switchStealth.isChecked

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

        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilBackupEmail.error = "Please enter a valid email address (or leave empty)"
            return
        }
        binding.tilBackupEmail.error = null

        val saved = viewModel.validateAndSave(botToken, chatId, password, email, stealthMode)
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

            // Restart guardian background service fresh with new credentials
            viewModel.restartGuardianService()

            if (stealthMode) {
                val componentName = ComponentName(this@SetupActivity, SetupActivity::class.java)
                packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            Toast.makeText(
                this@SetupActivity,
                "🛡️ PASA Guardian Active (Direct Telegram Sovereign Mode)",
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
        if (preferencesManager.botToken.isNotBlank()) {
            binding.etBotToken.setText(preferencesManager.botToken)
        }
        if (preferencesManager.ownerChatId.isNotBlank()) {
            binding.etChatId.setText(preferencesManager.ownerChatId)
        }
        if (preferencesManager.backupEmail.isNotBlank()) {
            binding.etBackupEmail.setText(preferencesManager.backupEmail)
        }
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
        binding.tvDashBackend.text = "Gateway: ${viewModel.getBackendMode()}"
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
        authenticateOwner(
            title = "Administrator Authentication",
            subtitle = "Verify identity to reconfigure Sentinel settings",
            isStartup = false
        ) {
            Toast.makeText(this, "✅ Administrator Authenticated", Toast.LENGTH_SHORT).show()
            showSetupWizard()
        }
    }

    private fun authenticateOwner(
        title: String,
        subtitle: String,
        isStartup: Boolean = false,
        onSuccess: () -> Unit
    ) {
        if (isAuthenticating) return
        isAuthenticating = true

        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )

        val cipher = getOrGenerateBiometricCipher()

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS && cipher != null) {
            val executor = ContextCompat.getMainExecutor(this)
            val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    try {
                        result.cryptoObject?.cipher?.doFinal("pasa_session".toByteArray(Charsets.UTF_8))
                    } catch (_: Exception) {}
                    isAuthenticating = false
                    isSessionAuthenticated = true
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    isAuthenticating = false
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        showMasterPasswordDialog(title, subtitle, isStartup, onSuccess)
                    } else if (errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        if (isStartup) {
                            finish()
                        }
                    } else {
                        showMasterPasswordDialog(title, subtitle, isStartup, onSuccess)
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText("Use Master Password")
                .build()

            prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        } else {
            isAuthenticating = false
            showMasterPasswordDialog(title, subtitle, isStartup, onSuccess)
        }
    }

    private fun getOrGenerateBiometricCipher(): Cipher? {
        return try {
            val keyAlias = "pasa_biometric_auth_key"
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!keyStore.containsAlias(keyAlias)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore"
                )
                val specBuilder = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    specBuilder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                }
                keyGenerator.init(specBuilder.build())
                keyGenerator.generateKey()
            }
            val key = keyStore.getKey(keyAlias, null) as? SecretKey ?: return null
            Cipher.getInstance("AES/CBC/PKCS7Padding").apply {
                init(Cipher.ENCRYPT_MODE, key)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun showMasterPasswordDialog(
        title: String,
        subtitle: String,
        isStartup: Boolean,
        onSuccess: () -> Unit
    ) {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Enter Master Password"
            setPadding(48, 36, 48, 36)
            imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(if (subtitle.isNotBlank()) "$subtitle\n\nEnter Master Password to proceed:" else "Enter your Master Password to proceed:")
            .setView(input)
            .setCancelable(!isStartup)
            .setPositiveButton("Verify") { _, _ ->
                val entered = input.text.toString()
                if (viewModel.verifyMasterPassword(entered)) {
                    isSessionAuthenticated = true
                    Toast.makeText(this, "✅ Owner Identity Verified", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(this, "❌ Invalid Master Password", Toast.LENGTH_LONG).show()
                    if (isStartup) {
                        finish()
                    }
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                if (isStartup) {
                    finish()
                }
            }
            .setOnCancelListener {
                if (isStartup) {
                    finish()
                }
            }
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

    private fun showFullTermsDialog() {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val dialogBinding = com.izhaanintellect.pasa.databinding.DialogFullTermsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.tvFullTermsBody.text = getFullTermsHtml()

        var hasReachedBottom = preferencesManager.hasAcceptedTerms
        if (hasReachedBottom) {
            dialogBinding.pbReadingProgress.progress = 100
            dialogBinding.tvReadingProgressBadge.text = "100%"
            dialogBinding.tvReadingProgressBadge.setTextColor(Color.parseColor("#22E07A"))
            dialogBinding.tvScrollNotice.text = "✅ Complete terms reviewed. You may accept or decline."
            dialogBinding.tvScrollNotice.setTextColor(Color.parseColor("#22E07A"))
            dialogBinding.btnAcceptFullTerms.isEnabled = true
            dialogBinding.btnAcceptFullTerms.alpha = 1.0f
        }

        dialogBinding.nsvTerms.setOnScrollChangeListener { v: androidx.core.widget.NestedScrollView, _, scrollY, _, _ ->
            val child = v.getChildAt(0)
            if (child != null) {
                val totalScroll = child.measuredHeight - v.measuredHeight
                if (totalScroll > 0) {
                    val progress = ((scrollY.toFloat() / totalScroll.toFloat()) * 100).toInt().coerceIn(0, 100)
                    dialogBinding.pbReadingProgress.progress = progress
                    dialogBinding.tvReadingProgressBadge.text = "$progress%"

                    val diff = child.bottom - (v.height + scrollY)
                    if (diff <= 64 || progress >= 98) {
                        if (!hasReachedBottom) {
                            hasReachedBottom = true
                            dialogBinding.pbReadingProgress.progress = 100
                            dialogBinding.tvReadingProgressBadge.text = "100%"
                            dialogBinding.tvReadingProgressBadge.setTextColor(Color.parseColor("#22E07A"))
                            dialogBinding.tvScrollNotice.text = "✅ Complete terms reviewed. You may now accept."
                            dialogBinding.tvScrollNotice.setTextColor(Color.parseColor("#22E07A"))
                            dialogBinding.btnAcceptFullTerms.isEnabled = true
                            dialogBinding.btnAcceptFullTerms.alpha = 1.0f
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        }
                    }
                }
            }
        }

        dialogBinding.btnAcceptFullTerms.setOnClickListener {
            preferencesManager.hasAcceptedTerms = true
            binding.cbAcceptTerms.isChecked = true
            dialog.dismiss()
            Toast.makeText(this, "✅ Terms of Service & EULA Accepted", Toast.LENGTH_SHORT).show()
        }

        dialogBinding.btnDeclineTerms.setOnClickListener {
            preferencesManager.hasAcceptedTerms = false
            binding.cbAcceptTerms.isChecked = false
            dialog.dismiss()
            Toast.makeText(this, "❌ Terms Declined. Acceptance is required to activate PASA.", Toast.LENGTH_LONG).show()
        }

        dialog.show()
    }

    private fun getFullTermsHtml(): Spanned {
        val html = """
            <p><font color="#00E5FF"><b>PASA SENTINEL — END-USER LICENSE AGREEMENT &amp; TERMS OF SERVICE</b></font><br/>
            <font color="#94A3B8"><b>Effective Date:</b> September 20, 2026 | <b>Version:</b> 3.5.9 (Build 55)<br/>
            <b>Provider:</b> Izhaan Intellect &amp; The PASA Sentinel Security Engineering Team</font></p>

            <p><font color="#F59E0B"><b>⚠️ MANDATORY LEGAL NOTICE — READ CAREFULLY BEFORE ACTIVATION:</b></font><br/>
            <font color="#E2E8F0">By installing, launching, configuring, or activating PASA Sentinel on any hardware device, or by linking it to any Telegram Command &amp; Control (C2) channel or web gateway, you ("User", "Administrator", "Licensee", "You") unconditionally agree to be bound by all the terms, conditions, representations, and warranties set forth in this Agreement. If you do not agree to these terms, you must immediately decline and permanently remove this application.</font></p>

            <p><font color="#38BDF8"><b>1. LAWFUL OWNERSHIP &amp; ZERO STALKERWARE CERTIFICATION</b></font><br/>
            <font color="#E2E8F0"><b>1.1 Sole Ownership Warranty:</b> You expressly represent, warrant, and certify under penalty of perjury that you are the sole legal owner of this physical Android device, or a designated enterprise IT administrator acting with explicit corporate authority and written end-user consent.<br/>
            <b>1.2 Anti-Stalkerware Prohibition:</b> PASA Sentinel is engineered, distributed, and licensed <b>exclusively as a sovereign defensive countermeasure</b> against physical theft, violent snatching, unauthorized device tampering, and extortion. Under no circumstances may PASA Sentinel be deployed covertly onto a spouse's, partner's, child's, employee's, or third party's personal device without continuous, informed written consent. Unauthorized surveillance is a serious criminal offense under international cybercrime statutes (including U.S. CFAA 18 U.S.C. § 1030, ECPA 18 U.S.C. § 2510, EU GDPR/ePrivacy, and Bangladesh Cyber Security Act).</font></p>

            <p><font color="#38BDF8"><b>2. EMERGENCY COUNTERMEASURES &amp; IRREVERSIBLE DATA LOSS</b></font><br/>
            <font color="#E2E8F0"><b>2.1 Hardware Countermeasures:</b> PASA Sentinel equips the administrator with hardware-grade countermeasures intended to protect corporate or personal confidentiality in extreme compromise scenarios:
            <br/>• <b>Remote Factory Reset (<code>/wipe</code>, <code>/wipe_confirm</code>):</b> Triggers irreversible hardware sanitization destroying all data, cryptographic keys, and system states.
            <br/>• <b>Cryptographic File Shredding (<code>/shred</code>):</b> Overwrites target directory trees with zero-fill entropy buffers before file deletion.
            <br/>• <b>Hardware OS PIN Reset (<code>/set_os_pin</code>):</b> Overwrites the Android lockscreen credential via Device Owner cryptographic Escrow Tokens.
            <br/>• <b>Knox Kiosk Lockdown (<code>/lock</code>):</b> Completely suppresses Android UI navigation, status bars, and hardware key handlers.<br/>
            <b>2.2 Zero Liability Disclaimer:</b> YOU ACKNOWLEDGE THAT EMERGENCY ACTIONS ARE DESTRUCTIVE AND IRREVERSIBLE BY DESIGN. The developer disclaims all liability for accidental wipe execution, loss of personal data or cryptocurrency wallets, or hardware lockouts resulting from forgotten passwords.</font></p>

            <p><font color="#38BDF8"><b>3. FORENSIC TELEMETRY &amp; WIRETAP LAW COMPLIANCE</b></font><br/>
            <font color="#E2E8F0"><b>3.1 Forensic Evidence Collection:</b> When armed, in Lost Mode, or triggered by intrusion traps (snatch detection, power disconnect, SIM ejection, screen touch in Lost Mode, or failed unlock attempts), PASA Sentinel autonomously captures satellite GNSS coordinates, silent front/rear camera mugshots, ambient microphone recordings, and screen captures.<br/>
            <b>3.2 Compliance with Recording Laws:</b> Certain jurisdictions enforce strict two-party (all-party) audio recording consent. You alone are responsible for verifying compliance with local wiretapping laws. Forensic evidence may only be used for legitimate crime reporting and device recovery.</font></p>

            <p><font color="#38BDF8"><b>4. TELEGRAM COMMAND &amp; CONTROL (C2) ARCHITECTURE</b></font><br/>
            <font color="#E2E8F0"><b>4.1 Sovereign Direct Bot vs. VPS Gateway:</b> PASA supports both sovereign private bot tokens (via @BotFather) and multi-tenant VPS control plane gateways. In private bot mode, no telemetry passes through third-party servers.<br/>
            <b>4.2 Telegram Account Security:</b> You are solely responsible for securing your personal Telegram account with Two-Step Verification (2FA) and biometric passcodes. Anyone with access to your Telegram client can dispatch administrative commands to your device.</font></p>

            <p><font color="#38BDF8"><b>5. DUAL-USE EXPORT CONTROLS &amp; TRADE SANCTIONS</b></font><br/>
            <font color="#E2E8F0">PASA Sentinel implements kernel-level cryptography (AES-256-GCM, StrongBox Keymaster, TEE) and administrative hardware lockout mechanisms classified as dual-use technologies under international trade frameworks (including the Wassenaar Arrangement and U.S. Export Administration Regulations - EAR). You certify that you are not located in, nor a resident or national of, any embargoed jurisdiction and are not listed on any denied persons or SDN lists.</font></p>

            <p><font color="#38BDF8"><b>6. DISCLAIMER OF WARRANTIES ("AS-IS")</b></font><br/>
            <font color="#E2E8F0">PASA SENTINEL IS PROVIDED ON AN "AS-IS" AND "AS-AVAILABLE" BASIS WITHOUT WARRANTIES OF ANY KIND, EXPRESS OR IMPLIED. PROVIDER DOES NOT WARRANT UNINTERRUPTED AVAILABILITY, FREEDOM FROM BUGS, OR THAT TELEMETRY TRANSMISSION CAN OVERCOME PHYSICAL HARDWARE DESTRUCTION OR RF FARADAY SHIELDING.</font></p>

            <p><font color="#38BDF8"><b>7. LIMITATION OF LIABILITY &amp; INDEMNIFICATION</b></font><br/>
            <font color="#E2E8F0"><b>7.1 Liability Cap:</b> Under no circumstances shall Provider's total aggregate liability exceed the purchase price paid for your PASA Sentinel license.<br/>
            <b>7.2 Hold Harmless:</b> You agree to defend, indemnify, and hold harmless Izhaan Intellect and its developers against any legal claims, damages, or fines arising from unlawful deployment, privacy violations, or emergency wipe data loss.</font></p>

            <p><font color="#38BDF8"><b>8. SOVEREIGN ZERO-TELEMETRY &amp; ZERO-STORAGE OATH</b></font><br/>
            <font color="#E2E8F0">Provider adheres to a strict Zero-Storage Architecture. No surveillance photos, audio wiretaps, or GPS coordinates are ever written to server disk or cloud databases. Evidence streams directly to your Telegram bot and is instantly shredded from device RAM.</font></p>

            <p><font color="#38BDF8"><b>9. COMMERCIAL LICENSING &amp; 7-DAY MONEY-BACK GUARANTEE</b></font><br/>
            <font color="#E2E8F0">Every commercial license (Pro Lifetime $25 USD / ৳3,000 BDT or Enterprise Fleet $99 USD / ৳12,000 BDT) is backed by an unconditional 7-day money-back guarantee via Binance Pay (0% fees) or bKash personal transfer.</font></p>

            <p><font color="#38BDF8"><b>10. TERMINATION &amp; REVOCATION</b></font><br/>
            <font color="#E2E8F0">Provider reserves the right to terminate license keys or revoke gateway access without notice upon detection of abusive activity, stalkerware deployment, or reverse engineering attempts.</font></p>

            <p><font color="#38BDF8"><b>11. GOVERNING LAW &amp; EXCLUSIVE JURISDICTION</b></font><br/>
            <font color="#E2E8F0">This Agreement is governed by the substantive laws of Bangladesh. Any dispute arising out of or in connection with this software shall be submitted to the exclusive jurisdiction of the competent courts in <b>Dhaka, Bangladesh</b>.</font></p>

            <p><font color="#38BDF8"><b>12. OFFICIAL CONTACT &amp; DISCLOSURE</b></font><br/>
            <font color="#E2E8F0">Support &amp; Inquiries: support@izhaanintellect.fun<br/>
            WhatsApp Security Desk: +880 1762-033445<br/>
            Official Legal Portal: https://pasa.izhaanintellect.fun/terms.html</font></p>
        """.trimIndent()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
    }
}


