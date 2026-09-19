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

    private lateinit var binding: ActivitySetupBinding
    private val viewModel: SetupViewModel by viewModels()

    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateUI()
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
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        updateUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // If already initialized and running, directly ensure service is active and finish
        if (preferencesManager.isSetupComplete) {
            PasaService.start(this)
            finish()
            return
        }

        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
        binding.etServerUrl.setText(viewModel.getSavedServerUrl())
        if (preferencesManager.botToken.isNotBlank()) {
            binding.etBotToken.setText(preferencesManager.botToken)
        } else {
            binding.etBotToken.hint = "Enter your Telegram Bot Token"
        }
        updateUI()
    }

    private fun setupClickListeners() {
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

        // Activate Button
        binding.btnActivate.setOnClickListener {
            activatePasa()
        }
    }


    private fun activatePasa() {
        val serverUrl = binding.etServerUrl.text.toString().trim()
        val botToken = binding.etBotToken.text.toString().trim()
        val chatId = binding.etChatId.text.toString().trim()
        val password = binding.etMasterPassword.text.toString()
        val email = binding.etBackupEmail.text.toString().trim()
        val stealthMode = binding.switchStealth.isChecked

        if (serverUrl.isEmpty()) {
            binding.tilServerUrl.error = "Server URL cannot be empty"
            return
        }
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

            Toast.makeText(
                this@SetupActivity,
                "🛡️ PASA Guardian Active & Linked to VPS",
                Toast.LENGTH_LONG
            ).show()

            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
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

        binding.btnActivate.isEnabled = isAdminActive && hasPermissions

        binding.tvStatus.text = when {
            !isAdminActive -> getString(R.string.status_admin_required)
            !hasPermissions -> getString(R.string.status_permissions_required)
            !isBatteryIgnored -> "Recommended: Disable battery optimization for continuous protection"
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
}

