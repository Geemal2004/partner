package com.studypartner.planner.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.studypartner.planner.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "AuthRepository"
        private const val PLAY_SERVICES_RESOLUTION_REQUEST_CODE = 9000
    }

    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    fun getCurrentUserSync(): FirebaseUser? = firebaseAuth.currentUser

    private fun checkPlayServicesAvailability(activity: Activity): Result<Unit>? {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val playServicesStatus = googleApiAvailability.isGooglePlayServicesAvailable(activity)
        if (playServicesStatus == ConnectionResult.SUCCESS) return null

        return if (googleApiAvailability.isUserResolvableError(playServicesStatus)) {
            try {
                googleApiAvailability.getErrorDialog(
                    activity,
                    playServicesStatus,
                    PLAY_SERVICES_RESOLUTION_REQUEST_CODE
                )?.show()
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying Google Play Services resolution dialog", e)
            }
            val errorString = googleApiAvailability.getErrorString(playServicesStatus)
            val msg = "Google Play Services update or initialization required ($errorString). " +
                "Please update or initialize Google Play Services on your device."
            Log.e(TAG, "Sign in failed: $msg")
            Result.failure(Exception(msg))
        } else {
            val msg = "Google Play Services is not supported or unavailable on this device."
            Log.e(TAG, "Sign in failed: $msg")
            Result.failure(Exception(msg))
        }
    }

    suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> {
        // 1. Check Google Play Services availability and offer resolution if needed
        val playServicesError = checkPlayServicesAvailability(activity)
        if (playServicesError != null) {
            @Suppress("UNCHECKED_CAST")
            return playServicesError as Result<FirebaseUser>
        }

        // 2. Retrieve webClientId safely
        val webClientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.w(TAG, "Resource default_web_client_id not found", e)
            ""
        }
        if (webClientId.isBlank()) {
            val msg = "Web Client ID is empty or not configured in strings.xml."
            Log.e(TAG, "Sign in failed: $msg")
            return Result.failure(Exception(msg))
        }

        // 3. Modern GetGoogleIdOption setup
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(activity)

        return try {
            val result = credentialManager.getCredential(request = request, context = activity)
            handleSignInResult(result)
        } catch (e: GetCredentialCancellationException) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(Exception("Sign in was canceled.", e))
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.e(TAG, "Sign in failed", e)
            val userMsg = "Google Play Services or Credential Provider configuration error. " +
                "Please update Google Play Services or check app setup."
            Result.failure(Exception(userMsg, e))
        } catch (e: NoCredentialException) {
            Log.e(TAG, "Sign in failed", e)
            val userMsg = "No Google accounts found on this device or available for sign-in. " +
                "Please add a Google account in device Settings and try again."
            Result.failure(Exception(userMsg, e))
        } catch (e: GetCredentialCustomException) {
            Log.e(TAG, "Sign in failed", e)
            val userFriendlyMessage = formatAuthException(e)
            Result.failure(Exception(userFriendlyMessage, e))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Sign in failed", e)
            val userFriendlyMessage = formatAuthException(e)
            Result.failure(Exception(userFriendlyMessage, e))
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            val userFriendlyMessage = formatAuthException(e)
            Result.failure(Exception(userFriendlyMessage, e))
        }
    }

    private suspend fun handleSignInResult(result: GetCredentialResponse): Result<FirebaseUser> {
        val credential = result.credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val msg = "Received unexpected credential type from Credential Manager: ${credential.type}"
            Log.e(TAG, "Sign in failed: $msg")
            return Result.failure(Exception(msg))
        }

        return try {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                val msg = "Firebase user is null after sign in."
                Log.e(TAG, "Sign in failed: $msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            val userFriendlyMessage = formatAuthException(e)
            Result.failure(Exception(userFriendlyMessage, e))
        }
    }

    private fun formatAuthException(e: Exception): String {
        val message = e.message ?: ""
        val causeMessage = e.cause?.message ?: ""
        val combined = "$message $causeMessage"

        return when {
            e is GetCredentialCancellationException ||
            combined.contains("canceled", ignoreCase = true) ||
            combined.contains("cancelled", ignoreCase = true) -> {
                "Sign in was canceled."
            }
            e is NoCredentialException ||
            combined.contains("NoCredentialException", ignoreCase = true) ||
            combined.contains("no credentials available", ignoreCase = true) -> {
                "No Google accounts found on this device or available for sign-in. " +
                "Please add a Google account in device Settings and try again."
            }
            e is GetCredentialProviderConfigurationException ||
            combined.contains("GetCredentialProviderConfigurationException", ignoreCase = true) -> {
                "Google Play Services or Credential Provider configuration error. " +
                "Please update Google Play Services or check app setup."
            }
            combined.contains("10") ||
            combined.contains("DEVELOPER_ERROR", ignoreCase = true) -> {
                "Google Sign-In configuration error (DEVELOPER_ERROR / Code 10). " +
                "Please verify that your SHA-1 fingerprint is registered in Firebase Console " +
                "for package 'com.studypartner.planner' and default_web_client_id is correct."
            }
            combined.contains("7") ||
            combined.contains("NETWORK_ERROR", ignoreCase = true) -> {
                "Network error during Google Sign-In. Please check your internet connection."
            }
            combined.contains("12500") ||
            combined.contains("SIGN_IN_FAILED", ignoreCase = true) ||
            combined.contains("SERVICE_VERSION_UPDATE_REQUIRED", ignoreCase = true) -> {
                "Google Sign-In failed. Please update or enable Google Play Services on your test device."
            }
            message.isNotBlank() -> message
            else -> "Google Sign-In failed due to an unknown error."
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    suspend fun saveFcmToken(token: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save FCM token to Firestore", e)
        }
    }
}
