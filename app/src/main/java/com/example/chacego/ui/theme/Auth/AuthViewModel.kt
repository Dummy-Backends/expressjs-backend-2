package com.example.chacego.ui.auth

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chacego.data.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import android.net.Uri
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.io.IOException

// Constants
const val RC_SIGN_IN_GOOGLE = 9001

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val profileApiService: ProfileApiService = RetrofitClient.api

    // --- UI State ---
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var nicknameInput by mutableStateOf("")
    var pictureUrlInput by mutableStateOf("")
    var selectedImageUri by mutableStateOf<Uri?>(null)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var infoMessage by mutableStateOf<String?>(null)

    // --- User Data ---
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    var profile by mutableStateOf<PlayerProfile?>(null)

    // --- Navigation States ---
    var isSigningUp by mutableStateOf(true)
    var isAuthenticated by mutableStateOf(auth.currentUser?.isEmailVerified == true)

    init {
        // Observe auth state changes.
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            isAuthenticated = user != null && user.isEmailVerified

            if (isAuthenticated && profile == null) {
                fetchOrCreateProfile(null)
            } else if (!isAuthenticated) {
                profile = null
            }
        }
    }

    // --- Handlers ---

    private fun handleAuthResult(task: Task<AuthResult>, onSuccessMessage: String, activity: Activity) {
        isLoading = false
        if (task.isSuccessful) {
            errorMessage = null
            Log.d("AuthViewModel", "$onSuccessMessage: ${auth.currentUser?.uid}")
            fetchOrCreateProfile(activity)
        } else {
            errorMessage = task.exception?.localizedMessage ?: "Erreur d'authentification inconnue."
            Log.e("AuthViewModel", "Authentication failed", task.exception)
            isAuthenticated = false
        }
    }

    private fun resetAuthForm() {
        email = ""
        password = ""
        errorMessage = null
        infoMessage = null
    }

    // --- Auth Methods ---

    fun signUpWithEmail(activity: Activity) {
        if (email.isBlank() || password.length < 6) return

        isLoading = true
        errorMessage = null
        infoMessage = null

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                isLoading = false
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()?.addOnCompleteListener { sendTask ->
                        if (sendTask.isSuccessful) {
                            infoMessage = "Un email de vérification a été envoyé. Veuillez vérifier votre boîte mail et vous reconnecter."
                            auth.signOut()
                            isAuthenticated = false
                        } else {
                            errorMessage = sendTask.exception?.localizedMessage ?: "Échec de l'envoi de l'email de vérification."
                            auth.signOut()
                        }
                    }
                } else {
                    errorMessage = task.exception?.localizedMessage ?: "Échec de l'inscription."
                }
            }
    }

    fun signInWithEmail(activity: Activity) {
        if (email.isBlank() || password.isBlank()) return

        isLoading = true
        errorMessage = null
        infoMessage = null

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        handleAuthResult(task, "Connexion Email réussie", activity)
                    } else {
                        isLoading = false
                        auth.signOut()
                        isAuthenticated = false
                        infoMessage = "Votre email n'est pas vérifié. Veuillez vérifier votre boîte mail."
                        user?.sendEmailVerification()
                    }
                } else {
                    isLoading = false
                    errorMessage = task.exception?.localizedMessage ?: "Échec de la connexion."
                }
            }
    }

    fun firebaseAuthWithGoogle(idToken: String, activity: Activity) {
        isLoading = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                handleAuthResult(task, "Connexion Google réussie", activity)
            }
    }

    fun signOut(activity: Activity) {
        auth.signOut()
        GoogleSignIn.getClient(activity, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        isAuthenticated = false
        resetAuthForm()
        profile = null
    }

    fun toggleSignUpMode() {
        isSigningUp = !isSigningUp
        resetAuthForm()
    }

    // --- Profile API Integration ---

    fun fetchOrCreateProfile(activity: Activity?) {
        viewModelScope.launch {
            val user = currentUser ?: return@launch
            if (!user.isEmailVerified) return@launch

            isLoading = true
            errorMessage = null

            try {
                // Get Firebase ID token
                val idToken: String = try {
                    val tokenResult = user.getIdToken(true).await()
                    val token = tokenResult.token
                    if (token == null || token.isEmpty() || token.trim().isEmpty()) {
                        throw IOException("ID Token is empty.")
                    }
                    token
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error getting ID token", e)
                    throw IOException("Failed to retrieve ID Token: ${e.message}")
                }

                // Fetch profile from backend (returns PlayerProfile directly, not Response)
                Log.d("AuthViewModel", "Fetching profile from backend...")
                profile = profileApiService.getProfile("Bearer $idToken")
                Log.d("AuthViewModel", "Profile fetched: nickname=${profile?.nickname}")

                // If profile has default nickname, user needs to customize it
                if (profile?.nickname == "NewPlayer") {
                    Log.d("AuthViewModel", "Profile needs customization - showing CustomizationScreen")
                }

            } catch (e: HttpException) {
                Log.e("AuthViewModel", "HTTP error: ${e.code()}, message: ${e.message()}")
                // Handle 404 - profile doesn't exist yet, create default
                if (e.code() == 404) {
                    Log.i("AuthViewModel", "Profile not found (404). Creating default profile.")
                    profile = PlayerProfile(
                        userId = user.uid,
                        nickname = "NewPlayer",
                        profilePictureUrl = "",
                        score = 100,
                        wins = 0,
                        losses = 0,
                        winrate = 0.0,
                        historyOfGames = emptyList()
                    )
                } else {
                    errorMessage = "Erreur serveur (${e.code()}): ${e.message()}"
                }
            } catch (e: IOException) {
                Log.e("AuthViewModel", "Network error", e)
                errorMessage = "Erreur réseau. Vérifiez que le serveur backend est démarré."
                // Create default profile to allow offline customization attempt
                profile = PlayerProfile(
                    userId = user.uid,
                    nickname = "NewPlayer",
                    profilePictureUrl = "",
                    score = 100,
                    wins = 0,
                    losses = 0,
                    winrate = 0.0,
                    historyOfGames = emptyList()
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Unknown error", e)
                errorMessage = "Erreur inconnue: ${e.localizedMessage ?: e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Upload image to Firebase Storage and get download URL
    private suspend fun uploadImageToFirebaseStorage(imageUri: Uri, userId: String): String {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val imageRef = storageRef.child("profile_pictures/${userId}_${System.currentTimeMillis()}.jpg")
        
        return try {
            val uploadTask = imageRef.putFile(imageUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error uploading image", e)
            throw IOException("Failed to upload image: ${e.message}")
        }
    }

    fun createOrUpdateProfile(activity: Activity, nickname: String, imageUri: Uri?) {
        viewModelScope.launch {
            val user = currentUser ?: return@launch
            if (!user.isEmailVerified) return@launch

            isLoading = true
            errorMessage = null

            try {
                // Upload image if provided
                var pictureUrl = ""
                if (imageUri != null) {
                    Log.d("AuthViewModel", "Uploading image to Firebase Storage...")
                    pictureUrl = uploadImageToFirebaseStorage(imageUri, user.uid)
                    Log.d("AuthViewModel", "Image uploaded successfully: $pictureUrl")
                }

                // Get Firebase ID token
                val idToken: String = try {
                    val tokenResult = user.getIdToken(true).await()
                    val token = tokenResult.token
                    if (token == null || token.isEmpty() || token.trim().isEmpty()) {
                        throw IOException("ID Token is empty.")
                    }
                    token
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error getting ID token for update", e)
                    throw IOException("Failed to retrieve ID Token: ${e.message}")
                }

                // Prepare customization request
                val customizationRequest = CustomizationRequest(
                    nickname = nickname.trim(),
                    profilePictureUrl = pictureUrl.takeIf { it.isNotBlank() }
                )

                // Update profile (returns PlayerProfile directly)
                Log.d("AuthViewModel", "Updating profile with nickname: ${nickname.trim()}")
                profile = profileApiService.updateProfile("Bearer $idToken", customizationRequest)
                
                Log.d("AuthViewModel", "Profile updated successfully: ${profile?.nickname}")
                selectedImageUri = null // Clear selected image after successful upload
                Toast.makeText(activity, "Profil mis à jour avec succès!", Toast.LENGTH_SHORT).show()

            } catch (e: HttpException) {
                Log.e("AuthViewModel", "HTTP error updating profile", e)
                errorMessage = "Erreur serveur (${e.code()}). Impossible de mettre à jour le profil."
            } catch (e: IOException) {
                Log.e("AuthViewModel", "Network error updating profile", e)
                errorMessage = "Erreur réseau. Vérifiez votre connexion et que le serveur est démarré."
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Unknown error updating profile", e)
                errorMessage = "Erreur inconnue: ${e.localizedMessage ?: e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}
