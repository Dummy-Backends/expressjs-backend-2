package com.example.chacego.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Base URL for the local Express server
// IMPORTANT: Use 10.0.2.2 for Android Emulator to access localhost (127.0.0.1)
private const val BASE_URL = "http://192.168.1.15:3000/api/"

// Response model for image upload
data class ImageUploadResponse(
    val success: Boolean,
    val imageUrl: String,
    val filename: String
)

interface ProfileApiService {

    // GET /api/profile
    @GET("profile")
    suspend fun getProfile(
        // Sends the Firebase ID Token (JWT) in the Authorization header
        @Header("Authorization") token: String
    ): PlayerProfile

    // PUT /api/profile/customize
    @PUT("profile/customize")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: CustomizationRequest
    ): PlayerProfile

    // POST /api/profile/upload-image
    @Multipart
    @POST("profile/upload-image")
    suspend fun uploadImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    ): ImageUploadResponse

    // POST /api/profile/game_result
    @POST("profile/game_result")
    suspend fun sendGameResult(
        @Header("Authorization") token: String,
        @Body result: GameResultRequest
    ): PlayerProfile
}

// Retrofit Builder for API access
object RetrofitClient {
    val api: ProfileApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProfileApiService::class.java)
    }
}
