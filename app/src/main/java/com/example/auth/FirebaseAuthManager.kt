package com.example.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data class Success(val uid: String, val email: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class FirebaseAuthManager(private val context: Context) {

    private val auth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    fun isFirebaseConfigured(): Boolean {
        return auth != null
    }

    fun getCurrentUserEmail(): String? {
        return auth?.currentUser?.email
    }

    fun getCurrentUserId(): String? {
        return auth?.currentUser?.uid
    }

    fun isUserLoggedIn(): Boolean {
        return auth?.currentUser != null
    }

    suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        val authInstance = auth
        if (authInstance == null) {
            return AuthResult.Error("Firebase Auth non inizializzato (manca google-services.json o config).")
        }
        return try {
            val result = authInstance.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                AuthResult.Success(user.uid, user.email ?: email)
            } else {
                AuthResult.Error("Creazione utente fallita senza dettagli.")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Errore durante la registrazione Firebase.")
        }
    }

    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        val authInstance = auth
        if (authInstance == null) {
            return AuthResult.Error("Firebase Auth non inizializzato.")
        }
        return try {
            val result = authInstance.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                AuthResult.Success(user.uid, user.email ?: email)
            } else {
                AuthResult.Error("Accesso fallito senza dettagli.")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Credenziali non valide o errore di rete.")
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (_: Exception) {}
    }
}
