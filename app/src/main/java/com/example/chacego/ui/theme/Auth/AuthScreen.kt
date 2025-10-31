package com.example.chacego.ui.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // Cette importation est maintenant correcte
import com.example.chacego.ui.auth.AuthViewModel
import com.example.chacego.ui.auth.SignUpStep

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = viewModel(),
    onGoogleSignInClicked: () -> Unit
) {
    if (viewModel.isAuthenticated) {
        MainScreen(viewModel)
    } else {
        AuthenticationForm(viewModel, onGoogleSignInClicked)
    }
}


@SuppressLint("ContextCastToActivity")
@Composable
fun MainScreen(viewModel: AuthViewModel) {
    val context = LocalContext.current as Activity
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bienvenue !", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "UID: ${viewModel.currentUser?.uid ?: "N/A"}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Email: ${viewModel.currentUser?.email ?: "N/A"}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            viewModel.signOut(context)
            Toast.makeText(context, "Déconnexion réussie", Toast.LENGTH_SHORT).show()
        }) {
            Text("Déconnexion")
        }
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

        // --- Formulaire avec vérification email pour l'inscription ---
        when {
            // Inscription: Flux multi-étapes avec vérification email
            viewModel.isSigningUp -> {
                when (viewModel.currentSignUpStep) {
                    SignUpStep.ENTER_EMAIL -> {
                        // Étape 1: Saisie de l'email
                        OutlinedTextField(
                            value = viewModel.email,
                            onValueChange = { viewModel.email = it },
                            label = { Text("Email") },
                            enabled = !viewModel.isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.authenticateWithEmail(context) },
                            enabled = !viewModel.isLoading && viewModel.email.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (viewModel.isLoading) {
                                CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Envoyer le code de vérification")
                            }
                        }
                    }
                    
                    SignUpStep.VERIFY_CODE -> {
                        // Étape 2: Vérification du code
                        Text(
                            "Code envoyé à ${viewModel.email}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = viewModel.verificationCode,
                            onValueChange = { if (it.length <= 6) viewModel.verificationCode = it },
                            label = { Text("Code de vérification (6 chiffres)") },
                            enabled = !viewModel.isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("000000") }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.authenticateWithEmail(context) },
                            enabled = !viewModel.isLoading && viewModel.verificationCode.length == 6,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (viewModel.isLoading) {
                                CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Vérifier le code")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Renvoyer le code",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                if (!viewModel.isLoading) {
                                    viewModel.currentSignUpStep = SignUpStep.ENTER_EMAIL
                                    viewModel.verificationCode = ""
                                    viewModel.errorMessage = null
                                }
                            }
                        )
                    }
                    
                    SignUpStep.CREATE_PASSWORD -> {
                        // Étape 3: Création du mot de passe
                        Text(
                            "Email vérifié: ${viewModel.email}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = viewModel.password,
                            onValueChange = { viewModel.password = it },
                            label = { Text("Mot de passe") },
                            enabled = !viewModel.isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            // TODO: Ajouter visualTransformation pour masquer le mot de passe
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Le mot de passe doit contenir au moins 6 caractères",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.authenticateWithEmail(context) },
                            enabled = !viewModel.isLoading && viewModel.password.length >= 6,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (viewModel.isLoading) {
                                CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Créer le compte")
                            }
                        }
                    }
                }
                
                // Bascule Inscription/Connexion (seulement visible à l'étape 1)
                if (viewModel.currentSignUpStep == SignUpStep.ENTER_EMAIL) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Déjà un compte? Connectez-vous",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { viewModel.toggleSignUpMode() }
                    )
                }
            }
            
            // Connexion: Formulaire simple email/password
            else -> {
                OutlinedTextField(
                    value = viewModel.email,
                    onValueChange = { viewModel.email = it },
                    label = { Text("Email") },
                    enabled = !viewModel.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = { viewModel.password = it },
                    label = { Text("Mot de passe") },
                    enabled = !viewModel.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    // TODO: Ajouter visualTransformation pour masquer le mot de passe
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.authenticateWithEmail(context) },
                    enabled = !viewModel.isLoading && viewModel.email.isNotBlank() && viewModel.password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Se connecter")
                    }
                }
                
                // Bascule Inscription/Connexion
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Pas encore inscrit? Créez un compte",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { viewModel.toggleSignUpMode() }
                )
            }
        }

        // Afficher Google Sign-In seulement à l'étape initiale
        if (!viewModel.isSigningUp || viewModel.currentSignUpStep == SignUpStep.ENTER_EMAIL) {
            Spacer(modifier = Modifier.height(32.dp))

            Divider(Modifier.padding(horizontal = 32.dp))
            Text("OU", modifier = Modifier.padding(vertical = 16.dp))

            // Google
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
}
