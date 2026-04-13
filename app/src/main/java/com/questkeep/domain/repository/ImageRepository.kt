package com.questkeep.domain.repository

import android.net.Uri

interface ImageRepository {
    /** Upload image from [uri], returns the public URL on success. */
    suspend fun uploadImage(uri: Uri): Result<String>
}
