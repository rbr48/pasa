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

        return OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.telegram.org/")
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
        val client = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                var request = chain.request()
                val currentBase = preferencesManager.serverUrl.trim()
                try {
                    val uri = java.net.URI.create(currentBase)
                    val host = uri.host
                    if (!host.isNullOrBlank()) {
                        val scheme = uri.scheme ?: "https"
                        val port = if (uri.port != -1) uri.port else (if (scheme == "http") 80 else 443)
                        val basePath = (uri.path ?: "").trim('/')
                        val endpointPath = request.url.encodedPath.trim('/')

                        val finalEndpoint = if (basePath.isNotEmpty() && endpointPath.startsWith(basePath)) {
                            endpointPath
                        } else if (basePath.isNotEmpty()) {
                            "$basePath/$endpointPath"
                        } else {
                            endpointPath
                        }

                        val newUrl = request.url.newBuilder()
                            .scheme(scheme)
                            .host(host)
                            .port(port)
                            .encodedPath("/$finalEndpoint")
                            .build()
                        val requestBuilder = request.newBuilder().url(newUrl)
                        val apiKey = preferencesManager.apiKey
                        if (apiKey.isNotBlank()) {
                            requestBuilder.header("Authorization", "Bearer $apiKey")
                        }

                        // Hardware-backed Proof-of-Possession Header (ASTRA Hardened Layer)
                        if (com.izhaanintellect.pasa.crypto.DeviceIdentity.exists()) {
                            val method = request.method
                            val proofPath = "/$finalEndpoint"
                            try {
                                val proof = com.izhaanintellect.pasa.crypto.DeviceAuth.proof(
                                    deviceId = preferencesManager.deviceId,
                                    method = method,
                                    path = proofPath
                                )
                                requestBuilder.header(com.izhaanintellect.pasa.crypto.DeviceAuth.headerName(), proof)
                            } catch (e: Exception) {
                                android.util.Log.w("AppModule", "Failed to generate device proof: ${e.message}")
                            }
                        }

                        request = requestBuilder.build()
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AppModule", "Failed to parse custom server URL: $currentBase", e)
                }
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(client)
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
        return com.izhaanintellect.pasa.crypto.CommandVerifier(
            deviceId = preferencesManager.deviceId,
            replayStore = replayStore,
            trustedKeys = preferencesManager.trustedCommandKeys
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
