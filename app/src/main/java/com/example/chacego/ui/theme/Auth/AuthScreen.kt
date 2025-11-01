package com.example.chacego.ui.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chacego.data.PlayerProfile

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = viewModel(),
    onGoogleSignInClicked: () -> Unit
) {
    if (viewModel.isAuthenticated) {
        // --- AUTHENTICATED USER NAVIGATION ---
        when {
            // 1. If profile is NOT loaded yet, show a loading screen
            viewModel.profile == null -> LoadingScreen()

            // 2. If profile is loaded but needs customization (new user check)
            viewModel.profile!!.nickname == "NewPlayer" -> CustomizationScreen(viewModel)

            // 3. Profile is complete, show completion screen
            else -> ProfileCompleteScreen(viewModel)
        }
    } else {
        // --- UN-AUTHENTICATED USER NAVIGATION ---
        AuthenticationForm(viewModel, onGoogleSignInClicked)
    }
}

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Chargement du profil...")
    }
}


@SuppressLint("ContextCastToActivity")
@Composable
fun AuthenticationForm(viewModel: AuthViewModel, onGoogleSignInClicked: () -> Unit) {
    val context = LocalContext.current as Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (viewModel.isSigningUp) "Inscription" else "Connexion",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        // --- Message d'erreur ---
        if (viewModel.errorMessage != null) {
            Text(
                text = "Erreur: ${viewModel.errorMessage}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // --- Message d'information (Email verification status) ---
        if (viewModel.infoMessage != null) {
            Text(
                text = viewModel.infoMessage!!,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // --- Formulaire classique: Sign Up ---
        if (viewModel.isSigningUp) {
            OutlinedTextField(
                value = viewModel.email,
                onValueChange = { viewModel.email = it },
                label = { Text("Email") },
                enabled = !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.password,
                onValueChange = { viewModel.password = it },
                label = { Text("Mot de passe") },
                enabled = !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Le mot de passe doit contenir au moins 6 caractères",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.signUpWithEmail(context) },
                enabled = !viewModel.isLoading && viewModel.email.isNotBlank() && viewModel.password.length >= 6,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Créer le compte")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Déjà un compte? Connectez-vous",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { viewModel.toggleSignUpMode() }
            )
        }
        // --- Formulaire classique: Sign In ---
        else {
            OutlinedTextField(
                value = viewModel.email,
                onValueChange = { viewModel.email = it },
                label = { Text("Email") },
                enabled = !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.password,
                onValueChange = { viewModel.password = it },
                label = { Text("Mot de passe") },
                enabled = !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.signInWithEmail(context) },
                enabled = !viewModel.isLoading && viewModel.email.isNotBlank() && viewModel.password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Se connecter")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Pas encore inscrit? Créez un compte",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { viewModel.toggleSignUpMode() }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Divider(Modifier.padding(horizontal = 32.dp))
        Text("OU", modifier = Modifier.padding(vertical = 16.dp))

        // Google Button
        Button(
            onClick = onGoogleSignInClicked,
            enabled = !viewModel.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("S'inscrire/Se connecter avec Google")
        }
    }
}

@Composable
fun CustomizationScreen(viewModel: AuthViewModel) {
    val context = LocalContext.current
    var nicknameInput by remember { mutableStateOf(viewModel.profile?.nickname?.takeIf { it != "NewPlayer" } ?: "") }
    var pictureUrlInput by remember { mutableStateOf(viewModel.profile?.profilePictureUrl ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Finalisez votre profil",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Ceci n'est demandé qu'une seule fois.")
        Spacer(modifier = Modifier.height(32.dp))

        // --- Nickname Input ---
        OutlinedTextField(
            value = nicknameInput,
            onValueChange = { nicknameInput = it },
            label = { Text("Nom d'utilisateur (Nickname)") },
            enabled = !viewModel.isLoading,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- Picture URL Input ---
        OutlinedTextField(
            value = pictureUrlInput,
            onValueChange = { pictureUrlInput = it },
            label = { Text("URL de la photo de profil (Optionnel)") },
            enabled = !viewModel.isLoading,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(32.dp))

        // --- Error Message ---
        if (viewModel.errorMessage != null) {
            Text(
                text = "Erreur: ${viewModel.errorMessage}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // --- Save Button ---
        Button(
            onClick = {
                if (nicknameInput.isBlank()) {
                    Toast.makeText(context, "Le Nickname est requis.", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.createOrUpdateProfile(context as Activity, nicknameInput, pictureUrlInput)
                }
            },
            enabled = !viewModel.isLoading && nicknameInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Enregistrer et Continuer")
            }
        }
    }
}

@Composable
fun ProfileCompleteScreen(viewModel: AuthViewModel) {
    val profile = viewModel.profile ?: return
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "✅ Profil configuré!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Bienvenue, ${profile.nickname}!",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Votre profil est prêt. Vous pouvez maintenant jouer!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(48.dp))

        // --- Log Out Button ---
        OutlinedButton(
            onClick = {
                viewModel.signOut(context as Activity)
                Toast.makeText(context, "Déconnexion réussie", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Déconnexion")
        }
    }
}
