package com.izhaanintellect.pasa.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.ExifInterface
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import com.izhaanintellect.pasa.util.PrivacyHygieneHelper
import java.io.File
import java.io.FileOutputStream

/**
 * Normalizes camera photo orientation, applies physical bitmap rotation,
 * and strips sensitive EXIF tracking metadata for covert captures.
 *
 * Solves the issue where front-camera sensor hardware produces unrotated landscape images
 * by physically rotating the pixel matrix to the actual device orientation (e.g. portrait).
 */
object CameraImageHelper {

    private const val TAG = "PASA_CameraImageHelper"

    /**
     * Resolves the current display rotation constant (Surface.ROTATION_0, 90, 180, 270).
     */
    fun getTargetRotation(context: Context): Int {
        return try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    context.display?.rotation ?: Surface.ROTATION_0
                } catch (_: Exception) {
                    Surface.ROTATION_0
                }
            } else {
                @Suppress("DEPRECATION")
                wm?.defaultDisplay?.rotation ?: Surface.ROTATION_0
            }
        } catch (_: Exception) {
            Surface.ROTATION_0
        }
    }

    /**
     * Translates the surface rotation constant into degrees (0, 90, 180, 270).
     */
    fun getDisplayRotationDegrees(context: Context): Int {
        return when (getTargetRotation(context)) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    /**
     * Determines the physical sensor orientation in degrees from Camera2 CameraManager.
     * Front cameras are typically 270°; back cameras are typically 90°.
     */
    fun getSensorOrientation(context: Context, useFrontCamera: Boolean): Int {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            if (cameraManager != null) {
                val targetFacing = if (useFrontCamera) {
                    CameraCharacteristics.LENS_FACING_FRONT
                } else {
                    CameraCharacteristics.LENS_FACING_BACK
                }

                for (cameraId in cameraManager.cameraIdList) {
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (facing == targetFacing) {
                        return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: if (useFrontCamera) 270 else 90
                    }
                }
            }
            if (useFrontCamera) 270 else 90
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query sensor orientation: ${e.message}")
            if (useFrontCamera) 270 else 90
        }
    }

    /**
     * Normalizes a captured photo file to ensure its physical pixel orientation
     * matches the actual device orientation when the photo was taken.
     *
     * 1. Inspects EXIF orientation and raw image dimensions.
     * 2. If the front camera took a landscape photo while device was in portrait,
     *    or if EXIF specifies rotation, rotates the physical bitmap matrix.
     * 3. Re-compresses to clean JPEG, strips tracking EXIF, and sets orientation to NORMAL.
     */
    fun normalizeAndOptimizePhoto(context: Context, file: File?, useFrontCamera: Boolean): File? {
        if (file == null || !file.exists() || file.length() == 0L) {
            return file
        }

        try {
            // 1. Read EXIF orientation
            val exif = ExifInterface(file.absolutePath)
            val exifOrientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            var exifDegrees = when (exifOrientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            // 2. Read raw image bounds without loading full pixels into RAM
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val rawWidth = bounds.outWidth
            val rawHeight = bounds.outHeight

            val displayDegrees = getDisplayRotationDegrees(context)
            val sensorOrientation = getSensorOrientation(context, useFrontCamera)

            // Determine if the photo is unrotated raw sensor output.
            // When taking photos in portrait (display 0° or 180°), raw sensor output has rawWidth > rawHeight (landscape).
            // If EXIF didn't record rotation (0°), but the image is landscape on a portrait device:
            var rotationNeeded = exifDegrees
            if (rotationNeeded == 0) {
                val isDevicePortrait = (displayDegrees == 0 || displayDegrees == 180)
                val isImageLandscape = (rawWidth > rawHeight)

                if (isDevicePortrait && isImageLandscape) {
                    // Raw uncompensated landscape sensor output captured in portrait mode.
                    // Front camera sensor is mounted at 270° -> needs (270 + displayDegrees) % 360 = 270° clockwise rotation to be upright portrait.
                    // Back camera sensor is mounted at 90° -> needs (90 - displayDegrees + 360) % 360 = 90° rotation.
                    rotationNeeded = if (useFrontCamera) {
                        (sensorOrientation + displayDegrees) % 360
                    } else {
                        (sensorOrientation - displayDegrees + 360) % 360
                    }
                    if (rotationNeeded == 0) rotationNeeded = if (useFrontCamera) 270 else 90
                    Log.i(TAG, "Uncompensated landscape photo detected in portrait mode (useFront=$useFrontCamera). Rotating $rotationNeeded° to actual portrait.")
                } else if (!isDevicePortrait && !isImageLandscape) {
                    // Image is portrait while device is landscape
                    rotationNeeded = if (useFrontCamera) {
                        (sensorOrientation + displayDegrees) % 360
                    } else {
                        (sensorOrientation - displayDegrees + 360) % 360
                    }
                    Log.i(TAG, "Uncompensated portrait photo detected in landscape mode (useFront=$useFrontCamera). Rotating $rotationNeeded° to actual landscape.")
                }
            }

            val needsDownscale = (rawWidth > 1920 || rawHeight > 1920 || file.length() > 600 * 1024)

            // 3. If rotation is required or image is too large, decode, rotate matrix, and re-encode
            if (rotationNeeded != 0 || needsDownscale) {
                var sampleSize = 1
                while ((rawWidth / sampleSize) > 1920 || (rawHeight / sampleSize) > 1920) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val decodedBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                if (decodedBitmap != null) {
                    val finalBitmap: Bitmap = if (rotationNeeded != 0) {
                        val matrix = Matrix().apply {
                            postRotate(rotationNeeded.toFloat())
                        }
                        val rotated = Bitmap.createBitmap(
                            decodedBitmap,
                            0,
                            0,
                            decodedBitmap.width,
                            decodedBitmap.height,
                            matrix,
                            true
                        )
                        if (rotated != decodedBitmap) {
                            decodedBitmap.recycle()
                        }
                        rotated
                    } else {
                        decodedBitmap
                    }

                    FileOutputStream(file).use { out ->
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                        out.flush()
                    }
                    finalBitmap.recycle()
                    Log.i(TAG, "Photo physically normalized: rotation=$rotationNeeded°, size=${file.length() / 1024} KB")
                }
            }

            // 4. Strip sensitive tracking EXIF and set orientation tag to NORMAL (1)
            PrivacyHygieneHelper.stripExifMetadata(file)
            try {
                val cleanExif = ExifInterface(file.absolutePath)
                cleanExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                cleanExif.saveAttributes()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to write ORIENTATION_NORMAL EXIF tag: ${e.message}")
            }

        } catch (e: Throwable) {
            Log.e(TAG, "Error normalizing photo orientation: ${e.message}", e)
        }

        return file
    }
}
