package com.questkeep.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Query

data class ImgbbUploadResponse(
    @SerializedName("data") val data: ImgbbData?,
    @SerializedName("success") val success: Boolean
)

data class ImgbbData(
    @SerializedName("url") val url: String,
    @SerializedName("delete_url") val deleteUrl: String
)

interface ImgbbApiService {
    @FormUrlEncoded
    @POST("1/upload")
    suspend fun uploadImage(
        @Query("key") apiKey: String,
        @Field("image") base64Image: String
    ): ImgbbUploadResponse
}
