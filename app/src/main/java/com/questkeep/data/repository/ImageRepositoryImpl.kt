package com.questkeep.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.questkeep.data.remote.ImgbbApiService
import com.questkeep.domain.repository.ImageRepository
import com.questkeep.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imgbbApi: ImgbbApiService
) : ImageRepository {

    override suspend fun uploadImage(uri: Uri): Result<String> {
        return try {
            val base64 = compressAndEncode(uri)
            val response = imgbbApi.uploadImage(Constants.IMGBB_API_KEY, base64)
            if (response.success && response.data != null) {
                Result.success(response.data.url)
            } else {
                Result.failure(Exception("Imgbb upload failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun compressAndEncode(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open image")
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Scale down to max 800px on the longer side
        val maxSize = 800
        val scale = maxSize.toFloat() / maxOf(original.width, original.height)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(),
                true
            )
        } else original

        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
}
