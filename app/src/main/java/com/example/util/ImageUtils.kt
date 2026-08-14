package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {
    /**
     * Creates a temporary File in external/internal cache and returns its FileProvider Uri for camera capture.
     */
    fun createCameraImageUri(context: Context): Uri? {
        return try {
            val imageFolder = File(context.filesDir, "property_photos").apply { if (!exists()) mkdirs() }
            val photoFile = File(imageFolder, "photo_${System.currentTimeMillis()}.jpg")
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves a Bitmap (from TakePicturePreview) into internal app storage and returns its Uri string.
     */
    fun saveBitmapToStorage(context: Context, bitmap: Bitmap): String? {
        return try {
            val imageFolder = File(context.filesDir, "property_photos").apply { if (!exists()) mkdirs() }
            val photoFile = File(imageFolder, "photo_${System.currentTimeMillis()}.jpg")
            FileOutputStream(photoFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Uri.fromFile(photoFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Copies an external gallery Uri content to local app internal storage to ensure persistent access.
     */
    fun copyGalleryUriToStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val imageFolder = File(context.filesDir, "property_photos").apply { if (!exists()) mkdirs() }
            val photoFile = File(imageFolder, "photo_${System.currentTimeMillis()}.jpg")
            
            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            val outputStream = FileOutputStream(photoFile)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(photoFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            sourceUri.toString()
        }
    }
}
