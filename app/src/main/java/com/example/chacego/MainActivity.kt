package com.example.chacego

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.chacego.ui.auth.AuthScreen
import com.example.chacego.ui.auth.AuthViewModel
import com.example.chacego.ui.auth.RC_SIGN_IN_GOOGLE
import com.example.chacego.ui.theme.ChaceGoTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialisation de GoogleSignInClient pour la méthode Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            ChaceGoTheme {
                // Initialisation du ViewModel (il doit être unique pour l'activité)
                authViewModel = remember { AuthViewModel() }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // C'EST LA LIGNE CLÉ : On affiche directement l'écran AuthScreen.
                    // AuthScreen gère lui-même si l'utilisateur est connecté ou doit voir le formulaire.
                    AuthScreen(
                        viewModel = authViewModel,
                        onGoogleSignInClicked = ::signInWithGoogle
                    )
                }
            }
        }
    }

    // Gère le résultat de l'activité Google Sign-In
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN_GOOGLE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("MainActivity", "Google Sign-In successful. Authenticating with Firebase.")
                account?.idToken?.let { idToken ->
                    authViewModel.firebaseAuthWithGoogle(idToken, this)
                }
            } catch (e: ApiException) {
                authViewModel.isLoading = false
                authViewModel.errorMessage = "Échec de Google Sign-In: ${e.localizedMessage}"
                Log.e("MainActivity", "Google Sign-In failed", e)
            }
        }
    }

    // Fonction pour lancer l'activité Google
    private fun signInWithGoogle() {
        authViewModel.isLoading = true
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN_GOOGLE)
    }
}
