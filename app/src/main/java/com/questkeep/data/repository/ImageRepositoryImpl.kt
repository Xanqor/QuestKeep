package com.questkeep.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.questkeep.BuildConfig
import com.questkeep.data.remote.ImgbbApiService
import com.questkeep.data.remote.ImgurApiService
import com.questkeep.domain.repository.ImageRepository
import com.questkeep.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imgurApi: ImgurApiService,
    private val imgbbApi: ImgbbApiService
) : ImageRepository {

    override suspend fun uploadImage(uri: Uri): Result<String> {
        return try {
            val base64 = compressAndEncode(uri)

            // 1. Primary provider: Imgur anonymous upload (Client-ID)
            try {
                val clientId = Constants.DEFAULT_IMGUR_CLIENT_ID
                val response = imgurApi.uploadImage("Client-ID $clientId", base64)
                val link = response.data?.link
                if (response.success && !link.isNullOrBlank()) {
                    return Result.success(link)
                }
            } catch (_: Exception) {
                // Fallback to secondary provider below
            }

            // 2. Secondary provider: Imgbb
            try {
                val apiKey = BuildConfig.IMGBB_API_KEY
                if (apiKey.isNotBlank()) {
                    val response = imgbbApi.uploadImage(apiKey, base64)
                    if (response.success && response.data != null) {
                        return Result.success(response.data.url)
                    }
                }
            } catch (_: Exception) {
                // Both external providers failed
            }

            // 3. Resilient Fallback: Compact Data URI
            // Guarantees avatar or image thumbnail saves even without external cloud/network services
            val compactDataUri = createCompactDataUri(uri)
            if (!compactDataUri.isNullOrBlank()) {
                return Result.success(compactDataUri)
            }

            Result.failure(Exception("Не удалось загрузить изображение. Проверьте интернет-соединение."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun readUriBytesSafely(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        // Attempt 1: Direct ContentResolver stream
        try {
            context.contentResolver.openInputStream(uri)?.use { return@withContext it.readBytes() }
        } catch (_: Exception) {}

        // Attempt 2: ParcelFileDescriptor
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { return@withContext it.readBytes() }
            }
        } catch (_: Exception) {}

        // Attempt 3: File copy to cache
        try {
            val tempFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.bin")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                val bytes = tempFile.readBytes()
                tempFile.delete()
                return@withContext bytes
            }
        } catch (_: Exception) {}

        // Attempt 4: Local file scheme
        if (uri.scheme == "file") {
            try {
                val file = File(uri.path ?: "")
                if (file.exists()) return@withContext file.readBytes()
            } catch (_: Exception) {}
        }

        null
    }

    private suspend fun compressAndEncode(uri: Uri): String = withContext(Dispatchers.IO) {
        val bytes = readUriBytesSafely(uri)
            ?: throw Exception("Не удалось открыть изображение. Выберите другое фото.")

        // First pass: decode bounds only to prevent OOM
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

        val maxSize = 1600
        var inSampleSize = 1
        while (boundsOptions.outWidth / inSampleSize > maxSize ||
            boundsOptions.outHeight / inSampleSize > maxSize
        ) {
            inSampleSize *= 2
        }

        // Second pass: decode scaled bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            ?: throw Exception("Не удалось обработать изображение.")

        val output = ByteArrayOutputStream()
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, output)
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun createCompactDataUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = readUriBytesSafely(uri) ?: return@withContext null
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

            val targetSize = 256
            var inSampleSize = 1
            while (boundsOptions.outWidth / inSampleSize > targetSize ||
                boundsOptions.outHeight / inSampleSize > targetSize
            ) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                ?: return@withContext null

            val scaledBitmap = Bitmap.createScaledBitmap(
                originalBitmap,
                targetSize.coerceAtMost(originalBitmap.width),
                targetSize.coerceAtMost(originalBitmap.height),
                true
            )

            val output = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, output)
            if (scaledBitmap != originalBitmap) scaledBitmap.recycle()
            originalBitmap.recycle()

            val base64Data = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64Data"
        } catch (_: Exception) {
            null
        }
    }
}
