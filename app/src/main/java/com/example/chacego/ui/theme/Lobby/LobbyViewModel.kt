package com.example.chacego.ui.theme.Lobby

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chacego.data.PlayerProfile
import com.example.chacego.data.ProfileApiService
import com.example.chacego.data.RetrofitClient
import com.google.android.gms.auth.api.signin.GoogleSignIn // <-- ADDED IMPORT
import com.google.android.gms.auth.api.signin.GoogleSignInOptions // <-- ADDED IMPORT
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.io.IOException

class LobbyViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val profileApiService: ProfileApiService = RetrofitClient.api

    // State Management
    var profile by mutableStateOf<PlayerProfile?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    init {
        // Load profile when ViewModel is created
        loadProfile()
    }

    /**
     * Loads the user's profile from the backend
     */
    fun loadProfile() {
        viewModelScope.launch {
            val user = currentUser ?: return@launch
            if (!user.isEmailVerified) {
                errorMessage = "Email not verified"
                return@launch
            }

            isLoading = true
            errorMessage = null

            try {
                val idToken: String = user.getIdToken(true).await().token
                    ?: throw IOException("ID Token is empty.")

                Log.d("LobbyViewModel", "Loading profile from backend...")
                profile = profileApiService.getProfile("Bearer $idToken")
                Log.d("LobbyViewModel", "Profile loaded: nickname=${profile?.nickname}")

            } catch (e: HttpException) {
                Log.e("LobbyViewModel", "HTTP error: ${e.code()}, message: ${e.message()}")
                errorMessage = when (e.code()) {
                    404 -> "Profile not found"
                    else -> "Server error (${e.code()})"
                }
            } catch (e: IOException) {
                Log.e("LobbyViewModel", "Network error", e)
                errorMessage = "Network error. Please check your connection."
            } catch (e: Exception) {
                Log.e("LobbyViewModel", "Unknown error", e)
                errorMessage = "Unknown error: ${e.localizedMessage ?: e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Refreshes the profile data
     */
    fun refreshProfile() {
        loadProfile()
    }

    // --- UPDATED SIGN OUT FUNCTION ---
    /**
     * Signs the user out from Firebase AND the Google Sign-In client.
     */
    fun signOut(activity: Activity) { // <-- NOW ACCEPTS ACTIVITY
        auth.signOut()
        // This is the missing line that clears the Google cache
        GoogleSignIn.getClient(activity, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        Log.d("LobbyViewModel", "User signed out from Firebase and Google.")
    }
    // --- END OF UPDATED FUNCTION ---
}