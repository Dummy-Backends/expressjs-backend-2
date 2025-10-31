package com.example.chacego.ui.auth

// Ligne ajoutée pour résoudre l'erreur
import android.app.Activity
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.chacego.data.EmailVerificationService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth


// Constante déplacée ici
const val RC_SIGN_IN_GOOGLE = 9001

// Enum pour les étapes de l'inscription avec vérification email
enum class SignUpStep {
    ENTER_EMAIL,      // Étape 1: Saisie de l'email
    VERIFY_CODE,      // Étape 2: Vérification du code
    CREATE_PASSWORD   // Étape 3: Création du mot de passe
}

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val emailVerificationService = EmailVerificationService()

    // --- États de l'UI et de l'Authentification ---
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var verificationCode by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // États de vérification email (pour l'inscription)
    var currentSignUpStep by mutableStateOf(SignUpStep.ENTER_EMAIL)
    var emailVerified by mutableStateOf(false)
    var verificationCodeSent by mutableStateOf(false)

    // État de l'utilisateur
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // États de navigation
    var isSigningUp by mutableStateOf(true)
    var isAuthenticated by mutableStateOf(auth.currentUser != null)

    init {
        auth.addAuthStateListener { firebaseAuth ->
            isAuthenticated = firebaseAuth.currentUser != null
        }
    }

    private fun handleAuthResult(task: Task<AuthResult>, onSuccessMessage: String) {
        isLoading = false
        if (task.isSuccessful) {
            errorMessage = null
            Log.d("AuthViewModel", "$onSuccessMessage: ${auth.currentUser?.uid}")
            isAuthenticated = true
        } else {
            errorMessage = task.exception?.localizedMessage ?: "Erreur d'authentification inconnue."
            Log.e("AuthViewModel", "Authentication failed", task.exception)
        }
    }

    // 1. Email/Password
    
    // Étape 1: Envoi du code de vérification (pour l'inscription)
    fun sendEmailVerificationCode(activity: Activity) {
        if (email.isBlank()) {
            errorMessage = "Veuillez entrer une adresse email valide."
            return
        }
        
        isLoading = true
        errorMessage = null
        
        emailVerificationService.sendVerificationCode(
            email = email,
            onSuccess = { code ->
                isLoading = false
                verificationCodeSent = true
                currentSignUpStep = SignUpStep.VERIFY_CODE
                Log.d("AuthViewModel", "Verification code sent to $email")
                // Note: In production, the code should be sent via email
                // For development/testing, you can log it or show it in a debug message
            },
            onFailure = { error ->
                isLoading = false
                errorMessage = error
                Log.e("AuthViewModel", "Failed to send verification code", Exception(error))
            }
        )
    }
    
    // Étape 2: Vérification du code
    fun verifyEmailCode(activity: Activity) {
        if (verificationCode.isBlank() || verificationCode.length != 6) {
            errorMessage = "Veuillez entrer un code de vérification valide (6 chiffres)."
            return
        }
        
        isLoading = true
        errorMessage = null
        
        emailVerificationService.verifyCode(
            email = email,
            code = verificationCode,
            onSuccess = {
                isLoading = false
                emailVerified = true
                currentSignUpStep = SignUpStep.CREATE_PASSWORD
                Log.d("AuthViewModel", "Email verified successfully: $email")
            },
            onFailure = { error ->
                isLoading = false
                errorMessage = error
                Log.e("AuthViewModel", "Verification failed", Exception(error))
            }
        )
    }
    
    // Étape 3: Création du compte avec mot de passe (après vérification email)
    fun createAccountWithVerifiedEmail(activity: Activity) {
        if (!emailVerified) {
            errorMessage = "Veuillez d'abord vérifier votre email."
            return
        }
        
        if (password.isBlank() || password.length < 6) {
            errorMessage = "Le mot de passe doit contenir au moins 6 caractères."
            return
        }
        
        isLoading = true
        errorMessage = null
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    // Reset verification states
                    resetVerificationStates()
                    handleAuthResult(task, "Inscription Email réussie")
                } else {
                    handleAuthResult(task, "Inscription Email réussie")
                }
            }
    }
    
    // Connexion (pas de vérification email nécessaire)
    fun authenticateWithEmail(activity: Activity) {
        if (isSigningUp) {
            // Pour l'inscription, on doit suivre le flux de vérification
            if (currentSignUpStep == SignUpStep.ENTER_EMAIL) {
                sendEmailVerificationCode(activity)
            } else if (currentSignUpStep == SignUpStep.VERIFY_CODE) {
                verifyEmailCode(activity)
            } else if (currentSignUpStep == SignUpStep.CREATE_PASSWORD) {
                createAccountWithVerifiedEmail(activity)
            }
        } else {
            // Connexion: pas de vérification email nécessaire
            isLoading = true
            errorMessage = null
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity) { task ->
                    handleAuthResult(task, "Connexion Email réussie")
                }
        }
    }
    
    // Réinitialiser les états de vérification
    private fun resetVerificationStates() {
        currentSignUpStep = SignUpStep.ENTER_EMAIL
        emailVerified = false
        verificationCodeSent = false
        verificationCode = ""
    }
    
    // Réinitialiser le formulaire
    fun resetAuthForm() {
        email = ""
        password = ""
        verificationCode = ""
        errorMessage = null
        resetVerificationStates()
    }

    // 2. Google Sign-In (Authentification finale Firebase)
    fun firebaseAuthWithGoogle(idToken: String, activity: Activity) {
        isLoading = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                handleAuthResult(task, "Connexion Google réussie")
            }
    }

    // Déconnexion
    fun signOut(activity: Activity) {
        auth.signOut()
        // Utilisation de l'importation explicite pour GoogleSignInOptions
        GoogleSignIn.getClient(activity, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        isAuthenticated = false
        resetAuthForm()
    }
    
    // Changer entre inscription et connexion
    fun toggleSignUpMode() {
        isSigningUp = !isSigningUp
        resetAuthForm()
    }
}