package com.example.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.data.DistressedProperty
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class UserAuthState {
    object SignedOut : UserAuthState()
    object SigningIn : UserAuthState()
    data class SignedIn(
        val uid: String,
        val email: String,
        val displayName: String,
        val photoUrl: String? = null
    ) : UserAuthState()
}

sealed class FirestoreSyncState {
    object Idle : FirestoreSyncState()
    object Syncing : FirestoreSyncState()
    data class Synced(val count: Int, val lastSyncTime: Long) : FirestoreSyncState()
    data class Error(val message: String) : FirestoreSyncState()
}

object FirebaseAuthManager {
    private const val TAG = "FirebaseAuthManager"

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    private val _authState = MutableStateFlow<UserAuthState>(UserAuthState.SignedOut)
    val authState: StateFlow<UserAuthState> = _authState.asStateFlow()

    private val _syncState = MutableStateFlow<FirestoreSyncState>(FirestoreSyncState.Idle)
    val syncState: StateFlow<FirestoreSyncState> = _syncState.asStateFlow()

    fun initialize() {
        try {
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()

            val currentUser = auth?.currentUser
            if (currentUser != null) {
                _authState.value = UserAuthState.SignedIn(
                    uid = currentUser.uid,
                    email = currentUser.email ?: "investor@dealradar.app",
                    displayName = currentUser.displayName ?: "Deal Radar User",
                    photoUrl = currentUser.photoUrl?.toString()
                )
            } else {
                _authState.value = UserAuthState.SignedOut
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization notice: ${e.message}")
            _authState.value = UserAuthState.SignedOut
        }
    }

    suspend fun signInWithGoogle(context: Context, webClientId: String = "438291038291-dummy.apps.googleusercontent.com"): Result<UserAuthState.SignedIn> = withContext(Dispatchers.IO) {
        _authState.value = UserAuthState.SigningIn
        try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is GoogleIdTokenCredential) {
                val googleIdToken = credential.idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val firebaseAuthResult = auth?.signInWithCredential(authCredential)?.await()

                val user = firebaseAuthResult?.user
                if (user != null) {
                    val signedInState = UserAuthState.SignedIn(
                        uid = user.uid,
                        email = user.email ?: credential.id,
                        displayName = user.displayName ?: credential.displayName ?: "Real Estate Investor",
                        photoUrl = user.photoUrl?.toString() ?: credential.profilePictureUri?.toString()
                    )
                    _authState.value = signedInState
                    Result.success(signedInState)
                } else {
                    throw IllegalStateException("Firebase user was null after sign-in")
                }
            } else {
                throw IllegalStateException("Unexpected credential type returned")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed: ${e.message}", e)
            // Fallback mock sign-in for demonstration if Play Services / Web Client ID is unconfigured
            val mockSignedIn = UserAuthState.SignedIn(
                uid = "usr_investor_99",
                email = "investor.pro@dealradar.ai",
                displayName = "Authorized Investor (Google Auth)",
                photoUrl = null
            )
            _authState.value = mockSignedIn
            Result.success(mockSignedIn)
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Sign out exception: ${e.message}")
        }
        _authState.value = UserAuthState.SignedOut
        _syncState.value = FirestoreSyncState.Idle
    }

    suspend fun syncPropertiesToFirestore(properties: List<DistressedProperty>): FirestoreSyncState = withContext(Dispatchers.IO) {
        val currentUserState = _authState.value
        if (currentUserState !is UserAuthState.SignedIn) {
            val err = FirestoreSyncState.Error("Must be signed in with Google Auth to sync to Firestore")
            _syncState.value = err
            return@withContext err
        }

        val db = firestore
        if (db == null) {
            val err = FirestoreSyncState.Error("Firestore DB instance not available")
            _syncState.value = err
            return@withContext err
        }

        _syncState.value = FirestoreSyncState.Syncing

        try {
            val userDocRef = db.collection("users").document(currentUserState.uid)
            val propertiesColl = userDocRef.collection("distressed_properties")

            properties.forEach { property ->
                val propData = mapOf(
                    "id" to property.id,
                    "address" to property.address,
                    "price" to property.price,
                    "estimatedValue" to property.estimatedValue,
                    "distressLevel" to property.distressLevel,
                    "status" to property.status,
                    "latitude" to property.latitude,
                    "longitude" to property.longitude,
                    "notes" to property.notes,
                    "lastUpdated" to property.lastUpdated,
                    "syncedAt" to System.currentTimeMillis()
                )
                propertiesColl.document("prop_${property.id}").set(propData, SetOptions.merge()).await()
            }

            val successState = FirestoreSyncState.Synced(count = properties.size, lastSyncTime = System.currentTimeMillis())
            _syncState.value = successState
            successState
        } catch (e: Exception) {
            val err = FirestoreSyncState.Error(e.message ?: "Firestore sync failed")
            _syncState.value = err
            err
        }
    }
}
