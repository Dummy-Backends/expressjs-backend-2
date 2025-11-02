package com.example.chacego.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Random

/**
 * Service to handle email verification code generation and validation using Firestore
 * uyghkghgtu
 */
class EmailVerificationService {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val verificationCollection = "email_verifications"

    /**
     * Generates a 6-digit verification code
     */
    private fun generateVerificationCode(): String {
        val random = Random()
        return String.format("%06d", random.nextInt(1000000))
    }

    /**
     * Sends verification code and stores it in Firestore
     * Note: You need to implement email sending via:
     * - Firebase Cloud Functions
     * - Your backend API
     * - Or a service like EmailJS
     */
    fun sendVerificationCode(
        email: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Check if there's an existing unexpired code
        db.collection(verificationCollection)
            .whereEqualTo("email", email)
            .whereEqualTo("isUsed", false)
            .whereGreaterThan("expiresAt", System.currentTimeMillis())
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Existing valid code found, reuse it
                    val existingCode = documents.documents[0].getString("code") ?: ""
                    Log.d("EmailVerification", "Reusing existing code for $email")
                    onSuccess(existingCode)
                } else {
                    // Generate new code
                    val code = generateVerificationCode()
                    val verification = EmailVerification(
                        email = email,
                        code = code
                    )

                    // Store in Firestore
                    db.collection(verificationCollection)
                        .document(email) // Use email as document ID for easy lookup
                        .set(verification)
                        .addOnSuccessListener {
                            Log.d("EmailVerification", "Code generated and stored: $code")
                            
                            // TODO: Send email with verification code
                            // You need to implement this using:
                            // 1. Firebase Cloud Functions (recommended)
                            // 2. Your backend API
                            // 3. Email service like SendGrid, Mailgun, etc.
                            
                            // For now, we'll log it (in production, send via email)
                            Log.d("EmailVerification", "Verification code for $email: $code")
                            Log.w("EmailVerification", "⚠️ Implement email sending service!")
                            
                            // Call your email sending function here
                            // Example: sendEmailViaCloudFunction(email, code)
                            
                            onSuccess(code)
                        }
                        .addOnFailureListener { e ->
                            Log.e("EmailVerification", "Failed to store verification code", e)
                            onFailure("Erreur lors de la génération du code: ${e.localizedMessage}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("EmailVerification", "Failed to check existing codes", e)
                onFailure("Erreur lors de la vérification: ${e.localizedMessage}")
            }
    }

    /**
     * Verifies the entered code for the given email
     */
    fun verifyCode(
        email: String,
        code: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(verificationCollection)
            .document(email)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val verification = document.toObject(EmailVerification::class.java)
                    if (verification != null) {
                        // Check if code matches
                        if (verification.code == code) {
                            // Check if code is expired
                            if (System.currentTimeMillis() > verification.expiresAt) {
                                onFailure("Le code de vérification a expiré. Veuillez en demander un nouveau.")
                            } else if (verification.isUsed) {
                                onFailure("Ce code a déjà été utilisé. Veuillez en demander un nouveau.")
                            } else {
                                // Mark code as used
                                document.reference.update("isUsed", true)
                                    .addOnSuccessListener {
                                        Log.d("EmailVerification", "Code verified successfully for $email")
                                        onSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("EmailVerification", "Failed to mark code as used", e)
                                        onFailure("Erreur lors de la vérification: ${e.localizedMessage}")
                                    }
                            }
                        } else {
                            onFailure("Code de vérification incorrect.")
                        }
                    } else {
                        onFailure("Erreur lors de la vérification du code.")
                    }
                } else {
                    onFailure("Aucun code de vérification trouvé pour cet email.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("EmailVerification", "Failed to verify code", e)
                onFailure("Erreur lors de la vérification: ${e.localizedMessage}")
            }
    }

    /**
     * Deletes expired verification codes (optional cleanup function)
     */
    fun cleanupExpiredCodes() {
        db.collection(verificationCollection)
            .whereLessThan("expiresAt", System.currentTimeMillis())
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("EmailVerification", "Cleaned up ${documents.size()} expired codes")
                    }
            }
    }
}

