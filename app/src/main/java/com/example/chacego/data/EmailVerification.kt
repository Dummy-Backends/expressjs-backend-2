package com.example.chacego.data

/**
 * Data class for email verification codes stored in Firestore
 */
data class EmailVerification(
    val email: String,
    val code: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (10 * 60 * 1000), // 10 minutes
    val isUsed: Boolean = false
)
