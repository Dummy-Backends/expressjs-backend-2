package com.example.chacego.data

import retrofit2.http.GET
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val BASE_URL = "http://192.168.1.15:3000/api/"  // ton LAN OK

// DTO pour un match venant du backend Node.js
data class MatchDto(
    val _id: String,
    val date: String,
    val role: String,
    val duree: Int,
    val distance: Double,
    val resultat: String
)

interface HistoryApiService {

    @GET("matches")
    suspend fun getMatches(): List<MatchDto>
}

object HistoryRetrofitClient {
    val api: HistoryApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HistoryApiService::class.java)
    }
}
