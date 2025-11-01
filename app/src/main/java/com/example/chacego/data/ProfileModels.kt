package com.example.chacego.data

import com.google.gson.annotations.SerializedName

// 1. Model for a single game history entry
data class GameHistory(
    val opponentId: String,
    val result: String, // 'win', 'loss', or 'draw'
    val scoreChange: Int,
    val timestamp: String // ISO date string
)

// 2. Model for the complete player profile stored in MongoDB
data class PlayerProfile(
    @SerializedName("userId")
    val userId: String, // Firebase UID
    val nickname: String,
    @SerializedName("profile_picture_url")
    val profilePictureUrl: String,
    val score: Int,
    val wins: Int,
    val losses: Int,
    val winrate: Double,
    @SerializedName("history_of_games")
    val historyOfGames: List<GameHistory>
) {
    val isSuccessful: Boolean = false
    val body: PlayerProfile? = null
}

// 3. Model for sending profile customization requests (PUT)
data class CustomizationRequest(
    val nickname: String? = null,
    @SerializedName("profile_picture_url")
    val profilePictureUrl: String? = null
)

// 4. Model for sending game result requests (POST)
data class GameResultRequest(
    val didWin: Boolean,
    val scoreChange: Int,
    val opponentId: String
)