package com.izhaanintellect.pasa.network

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * Retrofit interface for communication with the PASA VPS Backend Control Plane.
 */
interface PasaBackendApi {

    @GET("health")
    suspend fun getHealth(): BackendHealthResponse

    @POST("api/verify-bot")
    suspend fun verifyBot(
        @Body request: VerifyBotRequest
    ): VerifyBotResponse

    @POST("api/device/register")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest
    ): RegisterDeviceResponse

    @GET("api/device/poll")
    suspend fun pollCommands(
        @Query("deviceId") deviceId: String,
        @Query("timeout") timeout: Int? = 25
    ): PollCommandsResponse

    @Multipart
    @POST("api/device/response")
    suspend fun sendDeviceResponse(
        @Part("deviceId") deviceId: RequestBody,
        @Part("commandId") commandId: RequestBody?,
        @Part("message") message: RequestBody?,
        @Part photo: MultipartBody.Part?,
        @Part audio: MultipartBody.Part?,
        @Part video: MultipartBody.Part?,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?
    ): SimpleBackendResponse

    @Multipart
    @POST("api/device/alert")
    suspend fun sendDeviceAlert(
        @Part("deviceId") deviceId: RequestBody,
        @Part("alertType") alertType: RequestBody,
        @Part("message") message: RequestBody,
        @Part photo: MultipartBody.Part?,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?
    ): SimpleBackendResponse
}

// --- Data Models ---

data class BackendHealthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("service") val service: String?,
    @SerializedName("version") val version: String?
)

data class VerifyBotRequest(
    @SerializedName("token") val token: String
)

data class VerifyBotResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("bot") val bot: BotInfo?,
    @SerializedName("description") val description: String? = null
)

data class BotInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String?,
    @SerializedName("firstName") val firstName: String?
)

data class RegisterDeviceRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("deviceName") val deviceName: String,
    @SerializedName("botToken") val botToken: String,
    @SerializedName("ownerChatId") val ownerChatId: String,
    @SerializedName("masterPasswordHash") val masterPasswordHash: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("publicKeyJwk") val publicKeyJwk: String? = null,
    @SerializedName("attestationChain") val attestationChain: List<String>? = null
)

data class RegisterDeviceResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("apiKey") val apiKey: String?,
    @SerializedName("signingKeyId") val signingKeyId: String? = null,
    @SerializedName("commandSigningPublicJwk") val commandSigningPublicJwk: String? = null
)

data class PollCommandsResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("commands") val commands: List<RemoteCommand>?
)

data class RemoteCommand(
    @SerializedName("id") val id: String,
    @SerializedName("command") val command: String,
    @SerializedName("args") val args: List<String>?,
    @SerializedName("chatId") val chatId: Long,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("envelope") val envelope: String? = null
)

data class SimpleBackendResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("message") val message: String?
)
