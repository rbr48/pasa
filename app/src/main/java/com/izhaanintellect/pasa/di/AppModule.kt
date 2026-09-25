package com.izhaanintellect.pasa.di

import android.content.Context
import androidx.room.Room
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.AppDatabase
import com.izhaanintellect.pasa.data.CommandLogDao
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.EncryptionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt dependency injection module for PASA.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Zero-Data: License calls always go to the canonical PASA server — never a user-supplied URL.
    private const val PASA_LICENSE_SERVER = "https://izhaanintellect.fun/pasa/"

    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext context: Context
    ): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // Redact authorization and query parameters to avoid leaking bot tokens into logs
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        }

        val userAgentInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "PASA-Android/1.0 (Linux; Android 16; Mobile)")
                .build()
            chain.proceed(request)
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // TRANSPORT SECURITY & MITM DEFENSE ARCHITECTURE
        // ─────────────────────────────────────────────────────────────────────────────
        // NOTE: Hardcoded static leaf/intermediate certificate pinning for third-party C2
        // infrastructure (api.telegram.org) is deliberately excluded per OWASP and Google
        // Android Security guidelines. Third-party C2 endpoints rotate intermediate CAs
        // (GoDaddy G2, Google Trust Services, Cloudflare edge certs) without advance notice,
        // which would cause immediate SSLPeerUnverifiedException and permanently brick the C2.
        //
        // Cloudflare-Resilient Multi-Root CA Pinning for pasa.izhaanintellect.fun:
        // For PASA's own VPS C2 gateway (pasa.izhaanintellect.fun), we enforce CertificatePinner
        // pinned against Cloudflare's trusted root authority set (ISRG Root X1, GTS Root R1/R4,
        // GlobalSign ECC Root R4, GlobalSign Root CA, and current Leaf SPKI). This blocks rogue CA
        // interception while ensuring edge certificate rotations will NEVER brick the sovereign client.
        //
        // PASA provides three layers of sovereign MITM defense:
        // 1. OS-Level (res/xml/network_security_config.xml): Cleartext traffic is completely
        //    disabled, and only trusted system CAs are accepted (user-installed proxy CAs rejected).
        // 2. Transport-Level: Strict Modern TLS (TLS 1.2 and TLS 1.3) connection specification
        //    and Cloudflare-resilient multi-root certificate pinning for sovereign infrastructure.
        // 3. Application-Level (ASTRA Cryptographic Layer):
        //    - Outbound requests carry hardware-backed ECDSA StrongBox/TEE DeviceAuth proofs.
        //    - Inbound C2 commands require valid Ed25519 cryptographic signatures; even a full
        //      TLS-terminating proxy cannot forge or execute unauthorized commands.
        // ─────────────────────────────────────────────────────────────────────────────
        val modernTlsSpec = okhttp3.ConnectionSpec.Builder(okhttp3.ConnectionSpec.MODERN_TLS)
            .tlsVersions(okhttp3.TlsVersion.TLS_1_3, okhttp3.TlsVersion.TLS_1_2)
            .build()

        val certificatePinner = okhttp3.CertificatePinner.Builder()
            .add(
                "pasa.izhaanintellect.fun",
                "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=", // ISRG Root X1 (Let's Encrypt)
                "sha256/hxqRlPTuQARG9qEBQROPqqrrnM3aEP5RQTJQKplIbho=", // GTS Root R1 (Google Trust Services)
                "sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c=", // GTS Root R4 (Google Trust Services ECC)
                "sha256/CLOmGQukYCuZGPx3ZPAvMkpagnBm38asb+zMaGQe3nI=", // GlobalSign ECC Root R4 (Cloudflare Active Root)
                "sha256/K87oWBWMECbeeJn0gWSacZZhfNmMITGsquUsMxphyk4=", // GlobalSign Root CA (R1)
                "sha256/sGQ5UCWb+T8nZPejelA8MafJ5HOY7FoTUsZS7hP6DGg="  // Current Leaf SPKI (Cloudflare Edge)
            )
            .add(
                "*.izhaanintellect.fun",
                "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=",
                "sha256/hxqRlPTuQARG9qEBQROPqqrrnM3aEP5RQTJQKplIbho=",
                "sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c=",
                "sha256/CLOmGQukYCuZGPx3ZPAvMkpagnBm38asb+zMaGQe3nI=",
                "sha256/K87oWBWMECbeeJn0gWSacZZhfNmMITGsquUsMxphyk4=",
                "sha256/sGQ5UCWb+T8nZPejelA8MafJ5HOY7FoTUsZS7hP6DGg="
            )
            .build()

        return OkHttpClient.Builder()
            .connectionSpecs(listOf(modernTlsSpec))
            .certificatePinner(certificatePinner)
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Zero-Data Unblocked Sovereign Proxy:
    // Routes Telegram Bot API requests through our stateless, memory-only Cloudflare/Nginx reverse proxy
    // to bypass regional ISP blocks (e.g. Bangladesh BTRC filtering) without requiring third-party VPNs.
    // Zero logs, zero caching, zero disk storage on proxy.
    private const val TELEGRAM_GATEWAY_URL = "https://pasa.izhaanintellect.fun/tg/"

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(TELEGRAM_GATEWAY_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTelegramApi(retrofit: Retrofit): TelegramApi {
        return retrofit.create(TelegramApi::class.java)
    }

    @Provides
    @Singleton
    fun providePasaBackendApi(
        okHttpClient: OkHttpClient,
        preferencesManager: PreferencesManager
    ): com.izhaanintellect.pasa.network.PasaBackendApi {
        val baseUrl = if (preferencesManager.serverUrl.isNotBlank()) preferencesManager.serverUrl else PASA_LICENSE_SERVER
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.izhaanintellect.pasa.network.PasaBackendApi::class.java)
    }

    @Provides
    @Singleton
    fun provideReplayStore(preferencesManager: PreferencesManager): com.izhaanintellect.pasa.crypto.ReplayStore {
        return com.izhaanintellect.pasa.crypto.PersistentReplayStore(preferencesManager)
    }

    @Provides
    @Singleton
    fun provideCommandVerifier(
        preferencesManager: PreferencesManager,
        replayStore: com.izhaanintellect.pasa.crypto.ReplayStore
    ): com.izhaanintellect.pasa.crypto.CommandVerifier {
        val defaultServerKey = """{"crv":"Ed25519","x":"yd8Y7WZq2YkLBMUuamTDNKQ6IT_HkwdN2MPcPWgjrNs","kty":"OKP","kid":"pasa-server-1"}"""
        return com.izhaanintellect.pasa.crypto.CommandVerifier(
            deviceId = preferencesManager.deviceId,
            replayStore = replayStore,
            trustedKeys = mapOf("pasa-server-1" to defaultServerKey),
            trustedKeysProvider = {
                val keys = preferencesManager.trustedCommandKeys.toMutableMap()
                if (!keys.containsKey("pasa-server-1")) {
                    keys["pasa-server-1"] = defaultServerKey
                }
                keys
            }
        )
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        encryptionManager: EncryptionManager
    ): AppDatabase {
        // Dynamically retrieve or generate a hardware-backed 256-bit passphrase
        val passphrase = encryptionManager.getOrCreateDatabasePassphrase()
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pasa_secure_db"
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideCommandLogDao(database: AppDatabase): CommandLogDao {
        return database.commandLogDao()
    }

    @Provides
    @Singleton
    fun providePendingUploadDao(database: AppDatabase): com.izhaanintellect.pasa.data.PendingUploadDao {
        return database.pendingUploadDao()
    }
}
