package com.example.chacego.data

// Cette classe sera utilisée pour sauvegarder et lire les données de l'utilisateur dans Firestore.
data class User(
    // UID Firebase de l'utilisateur, utilisé comme clé de document Firestore
    val uid: String? = null,
    // Email, sera rempli par les méthodes Email/Google
    val email: String? = null,
    // Numéro de téléphone (optionnel, pour le profil utilisateur)
    val phoneNumber: String? = null,
    // Champ de démonstration que vous pouvez étendre (Nom, Prénom, etc.)
    val createdAt: Long = System.currentTimeMillis()
)
