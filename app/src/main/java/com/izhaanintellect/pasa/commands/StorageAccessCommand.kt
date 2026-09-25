package com.izhaanintellect.pasa.commands

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.AuthManager
import com.izhaanintellect.pasa.security.Totp
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
 *   /gallery_latest <master_password> [count]   (extract 1-10 most recent photos)
 *   /getfile <master_password> <path>           (download any storage file up to 50MB)
 *   /list_files [directory]                     (browse files in directory)
 */
@Singleton
class StorageAccessCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telegramApi: TelegramApi,
    private val preferencesManager: PreferencesManager,
    private val authManager: AuthManager
) : Command {

    override val name = "/gallery_latest"
    override val description = "Extract recent gallery photos or download files from storage (Requires Master Password)"
    override val usage = "/gallery_latest <master_password> [count] | /getfile <master_password> <#|name|path> | /list_files [dir|shortcut]"

    companion object {
        private const val TAG = "PASA_StorageAccess"
        private const val MAX_UPLOAD_BYTES = 50 * 1024 * 1024L // 50MB Telegram Bot API limit

        // Stateful tracking of last listed directory and files for effortless 1-tap extraction
        @Volatile private var lastListedDirectory: File? = null
        @Volatile private var lastListedFiles: List<File> = emptyList()

        fun getLastListedFiles(): List<File> = lastListedFiles
        fun getLastListedDirectory(): File? = lastListedDirectory
    }

    private fun verifyCredentials(candidate: String?): Boolean {
        if (candidate.isNullOrBlank()) return false
        val isPass = authManager.verifyMasterPassword(candidate)
        val totpSecret = preferencesManager.smsTotpSecret
        val isTotp = totpSecret.isNotBlank() && Totp.verify(totpSecret, candidate, window = 3)
        return isPass || isTotp
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        return executeGalleryLatest(args, chatId)
    }

    suspend fun executeGalleryLatest(args: List<String>, chatId: Long): CommandResult = withContext(Dispatchers.IO) {
        if (authManager.hasMasterPassword()) {
            val candidate = args.firstOrNull()?.trim()
            if (candidate.isNullOrBlank()) {
                return@withContext CommandResult(
                    success = false,
                    message = """
                        🔑 <b>Gallery Extraction (Zero-Trust Guard)</b>
                        ━━━━━━━━━━━━━━━━━━━━
                        To extract camera roll photos, Master Password verification is required.

                        <b>Syntax:</b> <code>/gallery_latest &lt;master_password&gt; [count 1-10]</code>
                        <b>Example:</b> <code>/gallery_latest MySecretPass123 3</code>
                    """.trimIndent()
                )
            }
            if (!verifyCredentials(candidate)) {
                return@withContext CommandResult(
                    success = false,
                    message = "⛔ <b>Authentication Failed!</b> Incorrect Master Password. Gallery extraction rejected."
                )
            }
        }

        val requestedCount = (if (authManager.hasMasterPassword()) args.getOrNull(1) else args.firstOrNull())
            ?.toIntOrNull()?.coerceIn(1, 10) ?: 3

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
                val displayName = cursor.getString(nameColumn) ?: ""
                // Skip OEM and Google Photos trashed or hidden media
                if (displayName.startsWith(".trashed-") || displayName.startsWith(".")) {
                    continue
                }

                val id = cursor.getLong(idColumn)
                val safeName = if (displayName.isNotBlank()) displayName else "photo_${id}.jpg"
                val dateAdded = cursor.getLong(dateColumn) * 1000L
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(dateAdded))

                val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                // Copy stream to temp cache file for transmission
                val tempFile = File(cacheDir, "pasa_gal_${id}_$safeName")
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
        val remainingArgs = if (authManager.hasMasterPassword()) {
            if (args.isEmpty()) {
                val lastDirNote = lastListedDirectory?.let {
                    "\n📂 <b>Active Directory:</b> <code>${it.absolutePath}</code>\n" +
                    if (lastListedFiles.isNotEmpty()) "🔢 <b>Available Numbers:</b> <code>1</code> to <code>${lastListedFiles.size}</code>\n" else ""
                } ?: ""

                return@withContext CommandResult(
                    success = false,
                    message = """
                        🔑 <b>File Download (Zero-Trust Guard)</b>
                        ━━━━━━━━━━━━━━━━━━━━
                        To download files from device storage, Master Password verification is required.$lastDirNote
                        <b>Syntax:</b> <code>/getfile &lt;master_password&gt; &lt;#|name|path&gt;</code>
                        <b>Example:</b> <code>/getfile MySecretPass123 1</code>
                        <b>Example:</b> <code>/getfile MySecretPass123 /sdcard/Download/document.pdf</code>
                    """.trimIndent()
                )
            }

            val candidate = args[0].trim()
            if (!verifyCredentials(candidate)) {
                return@withContext CommandResult(
                    success = false,
                    message = "⛔ <b>Authentication Failed!</b> Incorrect Master Password. File extraction rejected."
                )
            }
            args.drop(1)
        } else {
            args
        }

        if (remainingArgs.isEmpty()) {
            return@withContext CommandResult(
                success = false,
                message = "❌ Missing file target. Usage: <code>/getfile &lt;master_password&gt; &lt;#|name|path&gt;</code>"
            )
        }

        val rawInput = remainingArgs.joinToString(" ").trim()
        val index = rawInput.toIntOrNull()

        val targetFile: File = when {
            // Case 1: Numeric index referencing last /list_files result
            index != null -> {
                val files = lastListedFiles
                if (files.isEmpty()) {
                    return@withContext CommandResult(
                        success = false,
                        message = "❌ <b>No Active File List:</b> Run <code>/list_files</code> first to populate file numbers, or provide an absolute path."
                    )
                }
                if (index !in 1..files.size) {
                    return@withContext CommandResult(
                        success = false,
                        message = "❌ <b>Invalid File Number:</b> <code>$index</code> is out of range. Choose between <code>1</code> and <code>${files.size}</code>."
                    )
                }
                files[index - 1]
            }

            // Case 2: Relative filename or path
            !rawInput.startsWith("/") -> {
                val candidateInLastDir = lastListedDirectory?.let { File(it, rawInput) }
                val sdcard = Environment.getExternalStorageDirectory()
                val candidateInSdcard = File(sdcard, rawInput)
                val candidateInCamera = File(File(sdcard, "DCIM/Camera"), rawInput)
                val candidateInDownload = File(File(sdcard, "Download"), rawInput)

                when {
                    candidateInLastDir != null && candidateInLastDir.exists() -> candidateInLastDir
                    candidateInCamera.exists() -> candidateInCamera
                    candidateInDownload.exists() -> candidateInDownload
                    candidateInSdcard.exists() -> candidateInSdcard
                    else -> candidateInLastDir ?: File(sdcard, rawInput)
                }
            }

            // Case 3: Absolute path
            else -> File(rawInput)
        }

        if (!targetFile.exists()) {
            return@withContext CommandResult(
                success = false,
                message = "❌ <b>File Not Found:</b> <code>${targetFile.absolutePath}</code> does not exist on device."
            )
        }

        if (targetFile.isDirectory) {
            return@withContext CommandResult(
                success = false,
                message = "❌ <b>Target is a Directory:</b> Use <code>/list_files ${targetFile.absolutePath}</code> to browse its contents."
            )
        }

        if (!targetFile.canRead()) {
            return@withContext CommandResult(
                success = false,
                message = "❌ <b>Permission Denied:</b> Cannot read <code>${targetFile.absolutePath}</code>. Check storage permissions."
            )
        }

        val sizeBytes = targetFile.length()
        if (sizeBytes > MAX_UPLOAD_BYTES) {
            val sizeMb = String.format(Locale.US, "%.1f", sizeBytes / (1024.0 * 1024.0))
            return@withContext CommandResult(
                success = false,
                message = "❌ <b>File Too Large:</b> File size ($sizeMb MB) exceeds Telegram's 50MB upload limit."
            )
        }

        val sizeFormatted = formatFileSize(sizeBytes)
        val ext = targetFile.extension.lowercase(Locale.ROOT)
        val isImage = ext in listOf("jpg", "jpeg", "png", "webp", "gif")
        val isVideo = ext in listOf("mp4", "mkv", "webm", "3gp", "avi")
        val isAudio = ext in listOf("mp3", "m4a", "wav", "aac", "ogg", "flac")
        val modDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(targetFile.lastModified()))

        when {
            isImage -> CommandResult(
                success = true,
                message = "🖼️ <b>Photo Extracted:</b> <code>${targetFile.name}</code>\n📊 <b>Size:</b> $sizeFormatted | <b>Date:</b> $modDate\n📁 <code>${targetFile.absolutePath}</code>",
                photoFile = targetFile
            )
            isVideo -> CommandResult(
                success = true,
                message = "🎥 <b>Video Extracted:</b> <code>${targetFile.name}</code>\n📊 <b>Size:</b> $sizeFormatted | <b>Date:</b> $modDate\n📁 <code>${targetFile.absolutePath}</code>",
                videoFile = targetFile
            )
            isAudio -> CommandResult(
                success = true,
                message = "🎙️ <b>Audio Extracted:</b> <code>${targetFile.name}</code>\n📊 <b>Size:</b> $sizeFormatted | <b>Date:</b> $modDate\n📁 <code>${targetFile.absolutePath}</code>",
                audioFile = targetFile
            )
            else -> CommandResult(
                success = true,
                message = "📄 <b>File Extracted:</b> <code>${targetFile.name}</code>\n📊 <b>Size:</b> $sizeFormatted | <b>Date:</b> $modDate\n📁 <code>${targetFile.absolutePath}</code>",
                documentFile = targetFile
            )
        }
    }

    suspend fun executeListFiles(args: List<String>): CommandResult = withContext(Dispatchers.IO) {
        val showAll = args.any {
            it.equals("--all", ignoreCase = true) ||
            it.equals("-a", ignoreCase = true) ||
            it.equals("all", ignoreCase = true) ||
            it.equals("trash", ignoreCase = true)
        }
        val cleanArgs = args.filterNot {
            it.equals("--all", ignoreCase = true) ||
            it.equals("-a", ignoreCase = true) ||
            it.equals("all", ignoreCase = true) ||
            it.equals("trash", ignoreCase = true)
        }

        // Separate page number from directory args (e.g. /list_files camera 2)
        val pageArg = cleanArgs.lastOrNull()?.toIntOrNull()
        val dirArgs = if (pageArg != null) cleanArgs.dropLast(1) else cleanArgs
        val page    = pageArg ?: 1

        val sdcard = Environment.getExternalStorageDirectory()
        val defaultDir = File(sdcard, "DCIM/Camera")

        val targetDir: File = if (dirArgs.isEmpty()) {
            if (defaultDir.exists() && defaultDir.isDirectory) defaultDir else sdcard
        } else {
            val query = dirArgs.joinToString(" ").trim()
            resolveDirectory(query, sdcard)
        }

        if (!targetDir.exists()) {
            return@withContext CommandResult(
                false,
                "❌ <b>Directory Not Found:</b> <code>${targetDir.absolutePath}</code>\n\n💡 <i>Try shortcuts: <code>/list_files camera</code>, <code>downloads</code>, <code>pictures</code>, <code>sdcard</code></i>"
            )
        }

        if (!targetDir.isDirectory) {
            return@withContext CommandResult(
                false,
                "❌ <code>${targetDir.absolutePath}</code> is a file, not a directory. Use <code>/getfile ${targetDir.absolutePath}</code> to download."
            )
        }

        return@withContext listDirectory(targetDir, showAll, page)
    }


    private fun resolveDirectory(query: String, sdcard: File): File {
        val qLower = query.lowercase(Locale.ROOT)
        return when (qLower) {
            "camera" -> File(sdcard, "DCIM/Camera")
            "dcim" -> File(sdcard, "DCIM")
            "download", "downloads" -> File(sdcard, "Download")
            "pictures", "photos" -> File(sdcard, "Pictures")
            "documents", "docs" -> File(sdcard, "Documents")
            "screenshots" -> {
                val picScreenshots = File(sdcard, "Pictures/Screenshots")
                val dcimScreenshots = File(sdcard, "DCIM/Screenshots")
                if (picScreenshots.exists()) picScreenshots else dcimScreenshots
            }
            "whatsapp" -> {
                val waMedia = File(sdcard, "Android/media/com.whatsapp/WhatsApp/Media")
                val waOld = File(sdcard, "WhatsApp/Media")
                if (waMedia.exists()) waMedia else waOld
            }
            "root", "sdcard", "internal", "home" -> sdcard
            "..", "up", "back" -> lastListedDirectory?.parentFile ?: sdcard
            else -> {
                // 1. Direct absolute path
                if (query.startsWith("/")) {
                    File(query)
                } else {
                    // 2. Relative to last listed directory
                    val relativeToLast = lastListedDirectory?.let { File(it, query) }
                    if (relativeToLast != null && relativeToLast.exists() && relativeToLast.isDirectory) {
                        relativeToLast
                    } else {
                        // 3. Relative to sdcard root
                        val relativeToSdcard = File(sdcard, query)
                        if (relativeToSdcard.exists() && relativeToSdcard.isDirectory) {
                            relativeToSdcard
                        } else {
                            relativeToLast ?: relativeToSdcard
                        }
                    }
                }
            }
        }
    }

    private fun listDirectory(dir: File, showAll: Boolean, page: Int = 1): CommandResult {
        val allEntries = dir.listFiles()
        if (allEntries == null) {
            return CommandResult(false, "❌ Unable to read directory <code>${dir.absolutePath}</code>. Check permissions.")
        }

        // Subdirectories
        val subdirs = allEntries
            .filter { it.isDirectory && (showAll || !it.name.startsWith(".")) }
            .sortedBy { it.name.lowercase(Locale.ROOT) }

        // Files
        val allFiles = allEntries.filter { !it.isDirectory }
        val trashedOrHiddenCount = allFiles.count { it.name.startsWith(".trashed-") || it.name.startsWith(".") }

        val activeFiles = allFiles
            .filter { showAll || (!it.name.startsWith(".trashed-") && !it.name.startsWith(".")) }
            .sortedByDescending { it.lastModified() } // NEWEST FIRST

        // Cache FULL list for 1-tap /getfile <number> (global across all pages)
        lastListedDirectory = dir
        lastListedFiles = activeFiles

        // Pagination config — only files are paged; subdirs always shown in full (capped at 8)
        val filesPerPage = 15
        val totalFilePages = maxOf(1, (activeFiles.size + filesPerPage - 1) / filesPerPage)
        val validPage = page.coerceIn(1, totalFilePages)
        val fileStartIdx = (validPage - 1) * filesPerPage
        val pageFiles = activeFiles.drop(fileStartIdx).take(filesPerPage)

        val sb = StringBuilder()
        sb.append("📁 <b>Folder:</b> <code>${dir.absolutePath}</code>\n")
        sb.append("📊 <b>Items:</b> ${subdirs.size} folder(s), ${activeFiles.size} active file(s)")
        if (!showAll && trashedOrHiddenCount > 0) {
            sb.append(" <i>($trashedOrHiddenCount trash/hidden filtered)</i>")
        }
        if (totalFilePages > 1) {
            sb.append(" — Files page $validPage of $totalFilePages")
        }
        sb.append("\n━━━━━━━━━━━━━━━━━━━━\n")

        // 1. Subfolders section (shown only on page 1 to avoid repetition)
        if (subdirs.isNotEmpty() && validPage == 1) {
            sb.append("📂 <b>Subfolders:</b>\n")
            val shownDirs = subdirs.take(8)
            for (sub in shownDirs) {
                val childCount = sub.list()?.size ?: 0
                sb.append("📁 <b>${sub.name}/</b>  [<i>$childCount</i>] — <code>/list_files ${sub.name}</code>\n")
            }
            if (subdirs.size > 8) {
                sb.append("   <i>...and ${subdirs.size - 8} more folders</i>\n")
            }
            sb.append("\n")
        }

        // 2. Active Files section (Newest first, numbered by global index for 1-tap /getfile)
        if (activeFiles.isEmpty()) {
            sb.append("<i>(No active files in this folder)</i>\n")
        } else {
            sb.append("📄 <b>Files (Newest First):</b>\n")
            for ((localIdx, f) in pageFiles.withIndex()) {
                val globalNum = fileStartIdx + localIdx + 1 // 1-based global number for /getfile
                val size = formatFileSize(f.length())
                val modDate = SimpleDateFormat("MM-dd HH:mm", Locale.US).format(Date(f.lastModified()))
                val ext = f.extension.lowercase(Locale.ROOT)
                val icon = when {
                    ext in listOf("jpg", "jpeg", "png", "webp", "gif") -> "🖼️"
                    ext in listOf("mp4", "mkv", "webm", "3gp") -> "🎥"
                    ext in listOf("mp3", "m4a", "wav", "aac") -> "🎙️"
                    ext in listOf("pdf", "doc", "docx", "txt") -> "📑"
                    ext in listOf("zip", "rar", "tar", "gz") -> "📦"
                    ext == "apk" -> "📱"
                    else -> "📄"
                }
                sb.append("$icon <b>[$globalNum]</b> <code>${f.name}</code>\n")
                sb.append("   └ $size • $modDate • 📥 <code>/getfile $globalNum</code>\n")
            }
        }

        // 3. Footer with navigation and tips
        sb.append("\n━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("⚡ <b>1-Tap Download:</b> Tap any <code>/getfile &lt;num&gt;</code> above.\n")

        // File page navigation
        if (totalFilePages > 1) {
            val dirPath = dir.absolutePath
            val navItems = mutableListOf<String>()
            if (validPage > 1) navItems.add("👈 <code>/list_files $dirPath ${validPage - 1}</code>")
            if (validPage < totalFilePages) navItems.add("👉 <code>/list_files $dirPath ${validPage + 1}</code>")
            if (navItems.isNotEmpty()) {
                sb.append("📄 <b>File Pages:</b> ${navItems.joinToString(" • ")}\n")
            }
        }

        if (!showAll && trashedOrHiddenCount > 0) {
            sb.append("🗑️ <b>Show Trash:</b> <code>/list_files --all</code>\n")
        }
        if (dir.parentFile != null && dir.absolutePath != Environment.getExternalStorageDirectory().absolutePath) {
            sb.append("⬆️ <b>Parent Folder:</b> <code>/list_files ..</code>\n")
        }
        sb.append("🧭 <b>Shortcuts:</b> <code>/list_files camera</code> | <code>downloads</code> | <code>pictures</code> | <code>docs</code>")

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
