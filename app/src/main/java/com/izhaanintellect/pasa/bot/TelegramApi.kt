package com.izhaanintellect.pasa.bot

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * Retrofit interface for the Telegram Bot API.
 * Handles polling for updates, sending messages, photos, audio, and locations.
 */
interface TelegramApi {

    /** Long-polls for new updates (messages) from Telegram. */
    @GET("bot{token}/getUpdates")
    suspend fun getUpdates(
        @Path(value = "token", encoded = true) token: String,
        @Query("offset") offset: Long? = null,
        @Query("timeout") timeout: Int = 30
    ): TelegramResponse<List<Update>>

    /** Gets the bot's own profile (useful for testing connection). */
    @GET("bot{token}/getMe")
    suspend fun getMe(
        @Path(value = "token", encoded = true) token: String
    ): TelegramResponse<From>

    /** Gets bot profile using a direct full URL. */
    @GET
    suspend fun getMeDirect(
        @Url fullUrl: String
    ): TelegramResponse<From>

    /** Sends a text message to a chat. */
    @POST("bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path(value = "token", encoded = true) token: String,
        @Body request: SendMessageRequest
    ): TelegramResponse<Message>

    /** Sends a photo to a chat. */
    @Multipart
    @POST("bot{token}/sendPhoto")
    suspend fun sendPhoto(
        @Path(value = "token", encoded = true) token: String,
        @Part("chat_id") chatId: RequestBody,
        @Part photo: MultipartBody.Part,
        @Part("caption") caption: RequestBody? = null
    ): TelegramResponse<Message>

    /** Sends an audio file to a chat. */
    @Multipart
    @POST("bot{token}/sendAudio")
    suspend fun sendAudio(
        @Path(value = "token", encoded = true) token: String,
        @Part("chat_id") chatId: RequestBody,
        @Part audio: MultipartBody.Part,
        @Part("caption") caption: RequestBody? = null
    ): TelegramResponse<Message>

    /** Sends a video file to a chat. */
    @Multipart
    @POST("bot{token}/sendVideo")
    suspend fun sendVideo(
        @Path(value = "token", encoded = true) token: String,
        @Part("chat_id") chatId: RequestBody,
        @Part video: MultipartBody.Part,
        @Part("caption") caption: RequestBody? = null
    ): TelegramResponse<Message>

    /** Sends a location pin or starts live location stream. */
    @POST("bot{token}/sendLocation")
    suspend fun sendLocation(
        @Path(value = "token", encoded = true) token: String,
        @Body request: SendLocationRequest
    ): TelegramResponse<Message>

    /** Updates a live location stream. */
    @POST("bot{token}/editMessageLiveLocation")
    suspend fun editMessageLiveLocation(
        @Path(value = "token", encoded = true) token: String,
        @Body request: EditMessageLiveLocationRequest
    ): TelegramResponse<Message>

    /** Stops a live location stream. */
    @POST("bot{token}/stopMessageLiveLocation")
    suspend fun stopMessageLiveLocation(
        @Path(value = "token", encoded = true) token: String,
        @Body request: StopMessageLiveLocationRequest
    ): TelegramResponse<Message>
}

// --- Data Models ---

/** Generic Telegram API response wrapper. */
data class TelegramResponse<T>(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("result") val result: T?,
    @SerializedName("description") val description: String? = null
)

/** Represents an incoming update from Telegram. */
data class Update(
    @SerializedName("update_id") val updateId: Long,
    @SerializedName("message") val message: TelegramMessage? = null
)

/** Represents a Telegram message. */
data class TelegramMessage(
    @SerializedName("message_id") val messageId: Long,
    @SerializedName("from") val from: From? = null,
    @SerializedName("chat") val chat: Chat,
    @SerializedName("date") val date: Long,
    @SerializedName("text") val text: String? = null
)

/** Represents the sender of a message. */
data class From(
    @SerializedName("id") val id: Long,
    @SerializedName("is_bot") val isBot: Boolean = false,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("username") val username: String? = null
)

/** Represents a Telegram chat. */
data class Chat(
    @SerializedName("id") val id: Long,
    @SerializedName("type") val type: String,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("username") val username: String? = null
)

/** Represents a sent message (response from Telegram). */
data class Message(
    @SerializedName("message_id") val messageId: Long,
    @SerializedName("chat") val chat: Chat,
    @SerializedName("date") val date: Long,
    @SerializedName("text") val text: String? = null
)

/** Request body for sendMessage. */
data class SendMessageRequest(
    @SerializedName("chat_id") val chatId: Long,
    @SerializedName("text") val text: String,
    @SerializedName("parse_mode") val parseMode: String = "HTML",
    @SerializedName("disable_web_page_preview") val disableWebPagePreview: Boolean = false
)

/** Request body for sendLocation. */
data class SendLocationRequest(
    @SerializedName("chat_id") val chatId: Long,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("live_period") val livePeriod: Int? = null
)

/** Request body for editMessageLiveLocation. */
data class EditMessageLiveLocationRequest(
    @SerializedName("chat_id") val chatId: Long,
    @SerializedName("message_id") val messageId: Long,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

/** Request body for stopMessageLiveLocation. */
data class StopMessageLiveLocationRequest(
    @SerializedName("chat_id") val chatId: Long,
    @SerializedName("message_id") val messageId: Long
)
