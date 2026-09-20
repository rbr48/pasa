package com.izhaanintellect.pasa.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.PendingUpload
import com.izhaanintellect.pasa.data.PendingUploadDao
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.network.PasaBackendApi
import com.izhaanintellect.pasa.security.EncryptionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Resilient background evidence uploader managed by Android WorkManager.
 * Guaranteed offline durability: Retries automatically on connectivity restore
 * using exponential backoff, survives device reboots, and respects OEM battery doze.
 */
@HiltWorker
class EvidenceUploadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val pendingUploadDao: PendingUploadDao,
    private val pasaBackendApi: PasaBackendApi,
    private val preferencesManager: PreferencesManager,
    private val encryptionManager: EncryptionManager,
    private val telegramApi: TelegramApi
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "PASA_UploadWorker"
        const val KEY_UPLOAD_ID = "upload_id"

        fun schedule(context: Context, uploadId: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<EvidenceUploadWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_UPLOAD_ID to uploadId))
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )
                .addTag("pasa_evidence_upload")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "upload_$uploadId",
                ExistingWorkPolicy.KEEP,
                workRequest
            )
            Log.i(TAG, "Enqueued WorkManager upload job for uploadId: $uploadId")
        }

        fun schedulePendingBatch(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<EvidenceUploadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .addTag("pasa_batch_upload")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "upload_batch_all",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        val targetUploadId = inputData.getString(KEY_UPLOAD_ID)
        Log.i(TAG, "EvidenceUploadWorker started. Target: ${targetUploadId ?: "ALL_PENDING"}")

        val tasksToProcess = if (!targetUploadId.isNullOrBlank()) {
            val task = pendingUploadDao.getById(targetUploadId)
            if (task != null && task.status != "COMPLETED") listOf(task) else emptyList()
        } else {
            pendingUploadDao.getPending()
        }

        if (tasksToProcess.isEmpty()) {
            Log.d(TAG, "No pending uploads found.")
            return Result.success()
        }

        var anyFailed = false

        for (task in tasksToProcess) {
            val success = processUpload(task)
            if (!success) {
                anyFailed = true
            }
        }

        return if (anyFailed) {
            if (runAttemptCount < 5) {
                Log.w(TAG, "Some uploads failed, scheduling WorkManager exponential backoff retry (attempt $runAttemptCount)")
                Result.retry()
            } else {
                Log.e(TAG, "Upload failed after max retries")
                Result.failure()
            }
        } else {
            // Clean completed uploads older than 7 days
            try {
                val cutoff = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
                pendingUploadDao.purgeCompletedOlderThan(cutoff)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to purge old completed records: ${e.message}")
            }
            Result.success()
        }
    }

    private suspend fun processUpload(task: PendingUpload): Boolean {
        val file = File(task.filePath)
        if (!file.exists() || file.length() == 0L) {
            Log.e(TAG, "File for task ${task.id} not found: ${task.filePath}")
            pendingUploadDao.update(
                task.copy(
                    status = "FAILED",
                    lastError = "File missing or empty",
                    attemptCount = task.attemptCount + 1
                )
            )
            return true // Don't retry missing files
        }

        pendingUploadDao.update(task.copy(status = "UPLOADING"))

        try {
            if (preferencesManager.useBackendServer) {
                val deviceIdBody = preferencesManager.deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
                val cmdIdBody = task.commandId.toRequestBody("text/plain".toMediaTypeOrNull())
                val msgCaption = when (task.fileType) {
                    "PHOTO" -> "📸 Captured photo"
                    "AUDIO" -> "🎙️ Audio recording"
                    "VIDEO" -> "🎥 Captured video"
                    else -> "📁 Captured evidence"
                }
                val msgBody = msgCaption.toRequestBody("text/plain".toMediaTypeOrNull())

                val uploadBytes = if (task.isEncrypted) {
                    encryptionManager.decryptEvidenceVaultToBytes(file)
                } else {
                    file.readBytes()
                }

                val photoPart = if (task.fileType == "PHOTO") {
                    val reqFile = uploadBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("photo", "photo.jpg", reqFile)
                } else null

                val audioPart = if (task.fileType == "AUDIO") {
                    val reqFile = uploadBytes.toRequestBody("audio/m4a".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("audio", "audio.m4a", reqFile)
                } else null

                val videoPart = if (task.fileType == "VIDEO") {
                    val reqFile = uploadBytes.toRequestBody("video/mp4".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("video", "video.mp4", reqFile)
                } else null

                val response = pasaBackendApi.sendDeviceResponse(
                    deviceId = deviceIdBody,
                    commandId = cmdIdBody,
                    message = msgBody,
                    photo = photoPart,
                    audio = audioPart,
                    video = videoPart,
                    evidence = null,
                    latitude = null,
                    longitude = null
                )

                if (response.ok) {
                    Log.i(TAG, "Successfully uploaded ${task.id} via VPS backend")
                    markCompleted(task, file)
                    return true
                } else {
                    throw IllegalStateException("VPS returned not OK")
                }
            } else {
                // Direct Telegram Delivery fallback
                val chatId = preferencesManager.ownerChatId
                val token = preferencesManager.botToken
                if (chatId.isNotBlank() && token.isNotBlank()) {
                    val chatIdBody = chatId.toRequestBody("text/plain".toMediaTypeOrNull())
                    val captionBody = "📸 Resilient capture delivered".toRequestBody("text/plain".toMediaTypeOrNull())
                    val audioCaptionBody = "🎙️ Resilient audio delivered".toRequestBody("text/plain".toMediaTypeOrNull())
                    val videoCaptionBody = "🎥 Resilient video delivered".toRequestBody("text/plain".toMediaTypeOrNull())

                    val uploadBytes = if (task.isEncrypted) {
                        encryptionManager.decryptEvidenceVaultToBytes(file)
                    } else {
                        file.readBytes()
                    }

                    when (task.fileType) {
                        "PHOTO" -> {
                            val part = MultipartBody.Part.createFormData(
                                "photo", "photo.jpg",
                                uploadBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                            )
                            telegramApi.sendPhoto(token, chatIdBody, part, captionBody)
                        }
                        "AUDIO" -> {
                            val part = MultipartBody.Part.createFormData(
                                "audio", "audio.m4a",
                                uploadBytes.toRequestBody("audio/m4a".toMediaTypeOrNull())
                            )
                            telegramApi.sendAudio(token, chatIdBody, part, audioCaptionBody)
                        }
                        "VIDEO" -> {
                            val part = MultipartBody.Part.createFormData(
                                "video", "video.mp4",
                                uploadBytes.toRequestBody("video/mp4".toMediaTypeOrNull())
                            )
                            telegramApi.sendVideo(token, chatIdBody, part, videoCaptionBody)
                        }
                    }

                    Log.i(TAG, "Successfully uploaded ${task.id} directly to Telegram")
                    markCompleted(task, file)
                    return true
                } else {
                    throw IllegalStateException("No ownerChatId or botToken configured for direct delivery")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Upload failed for ${task.id}: ${e.message}")
            pendingUploadDao.update(
                task.copy(
                    status = "PENDING",
                    lastError = e.message,
                    attemptCount = task.attemptCount + 1
                )
            )
            return false
        }
    }

    private suspend fun markCompleted(task: PendingUpload, file: File) {
        pendingUploadDao.update(
            task.copy(
                status = "COMPLETED",
                completedAt = System.currentTimeMillis()
            )
        )
        try {
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Cleaned up temporary evidence file: ${file.name}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete file after upload: ${e.message}")
        }
    }
}
