package com.questkeep.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

data class ImgurUploadResponse(
    @SerializedName("data") val data: ImgurData?,
    @SerializedName("success") val success: Boolean,
    @SerializedName("status") val status: Int
)

data class ImgurData(
    @SerializedName("id") val id: String?,
    @SerializedName("link") val link: String?,
    @SerializedName("deletehash") val deletehash: String?,
    @SerializedName("error") val error: String?
)

interface ImgurApiService {
    @FormUrlEncoded
    @POST("3/image")
    suspend fun uploadImage(
        @Header("Authorization") authHeader: String,
        @Field("image") base64Image: String,
        @Field("type") type: String = "base64"
    ): ImgurUploadResponse
}
