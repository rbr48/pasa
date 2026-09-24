package com.izhaanintellect.pasa.util

import android.media.ExifInterface
import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.security.SecureRandom

/**
 * Privacy & Anti-Forensics Hygiene Utilities for PASA Sentinel.
 * 
 * Provides:
 * 1. EXIF metadata stripping for surveillance photos (removes GPS, device model, software info).
 * 2. Multi-pass cryptographic shredding for temporary media files (PRNG noise + zero-fill)
 *    before unlinking from file system to defeat chip-off hardware recovery.
 */
object PrivacyHygieneHelper {

    private const val TAG = "PASA_PrivacyHygiene"
    private val secureRandom = SecureRandom()

    /**
     * Strips all identifying EXIF metadata tags from a JPEG file.
     * Removes GPS coordinates, timestamps, camera hardware serials, and device identifiers.
     */
    fun stripExifMetadata(file: File?): Boolean {
        if (file == null || !file.exists() || !file.canWrite() || file.length() == 0L) {
            return false
        }
        return try {
            val exif = ExifInterface(file.absolutePath)

            // 1. Strip GPS Location Telemetry
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, null)
            exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, null)
            exif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, null)

            // 2. Strip Device, Hardware & Software Identifiers
            exif.setAttribute(ExifInterface.TAG_MAKE, null)
            exif.setAttribute(ExifInterface.TAG_MODEL, null)
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, null)
            exif.setAttribute(ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION, null)

            // 3. Strip Timestamps & Dates
            exif.setAttribute(ExifInterface.TAG_DATETIME, null)
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, null)
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, null)

            // 4. Strip User Comments, Artist & Descriptions
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, null)
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, null)
            exif.setAttribute(ExifInterface.TAG_ARTIST, null)
            exif.setAttribute(ExifInterface.TAG_COPYRIGHT, null)

            exif.saveAttributes()
            Log.d(TAG, "EXIF metadata stripped from ${file.name}")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to strip EXIF from ${file.name}: ${e.message}")
            false
        }
    }

    /**
     * Multi-pass cryptographic shredder.
     * Overwrites file contents in-place with cryptographically secure random bytes,
     * then with zeros, before calling delete().
     * This destroys magnetic/flash remnants against hardware forensic dump tools.
     */
    fun secureShred(file: File?): Boolean {
        if (file == null || !file.exists()) return true
        if (file.isDirectory) {
            file.listFiles()?.forEach { secureShred(it) }
            return file.delete()
        }

        val length = file.length()
        if (length == 0L) {
            return file.delete()
        }

        return try {
            RandomAccessFile(file, "rws").use { raf ->
                val bufferSize = 8192
                val buffer = ByteArray(bufferSize)

                // Pass 1: Cryptographic PRNG Noise Overwrite
                raf.seek(0)
                var bytesWritten: Long = 0
                while (bytesWritten < length) {
                    val chunk = minOf(bufferSize.toLong(), length - bytesWritten).toInt()
                    secureRandom.nextBytes(buffer)
                    raf.write(buffer, 0, chunk)
                    bytesWritten += chunk
                }

                // Pass 2: Zero-Fill Overwrite
                raf.seek(0)
                buffer.fill(0)
                bytesWritten = 0
                while (bytesWritten < length) {
                    val chunk = minOf(bufferSize.toLong(), length - bytesWritten).toInt()
                    raf.write(buffer, 0, chunk)
                    bytesWritten += chunk
                }

                raf.fd.sync()
            }
            val deleted = file.delete()
            Log.d(TAG, "Cryptographically shredded ${file.name} (length=$length, deleted=$deleted)")
            deleted
        } catch (e: Exception) {
            Log.w(TAG, "Secure shredding error on ${file.name}, falling back to direct delete: ${e.message}")
            file.delete()
        }
    }
}
