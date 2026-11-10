package com.example.app.data

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AuthFailure { InvalidCredentials, EmailTaken, WeakPassword, Network, Unknown }

sealed interface AuthResult {
    data object Success : AuthResult
    /** Sign-up worked but the account is inactive until the emailed link is opened. */
    data object ConfirmEmail : AuthResult
    data class Failure(val reason: AuthFailure) : AuthResult
}

/**
 * What the screens need from authentication. The Supabase version (replica-backend) must keep these
 * signatures so AuthScreen and the rest of the app do not change.
 */
interface AuthRepository {
    val signedIn: StateFlow<Boolean>
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun signOut()

    /** Deletes the account and all its data. The store requires this for apps that offer sign-up. */
    suspend fun deleteAccount(): Boolean
}

/**
 * In-memory stand-in with dev hooks so every screen state can be reached by hand:
 *  - any email containing "offline"          -> network error
 *  - sign in with password "wrongpass"       -> invalid credentials
 *  - sign up with an email starting "taken@" -> email already registered
 *  - sign up with password "password"        -> weak password
 *  - sign up with an email containing "confirm" -> check-your-email state
 * These hooks must not exist in the real repository.
 */
class FakeAuthRepository(context: Context) : AuthRepository {
    private val prefs = context.getSharedPreferences("fake_auth", Context.MODE_PRIVATE)
    private val _signedIn = MutableStateFlow(prefs.getBoolean(KEY, false))
    override val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    private fun set(value: Boolean) {
        prefs.edit().putBoolean(KEY, value).apply()
        _signedIn.value = value
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        delay(600)
        return when {
            email.contains("offline") -> AuthResult.Failure(AuthFailure.Network)
            password == "wrongpass" -> AuthResult.Failure(AuthFailure.InvalidCredentials)
            else -> { set(true); AuthResult.Success }
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult {
        delay(600)
        return when {
            email.contains("offline") -> AuthResult.Failure(AuthFailure.Network)
            email.startsWith("taken@") -> AuthResult.Failure(AuthFailure.EmailTaken)
            password.equals("password", ignoreCase = true) -> AuthResult.Failure(AuthFailure.WeakPassword)
            email.contains("confirm") -> AuthResult.ConfirmEmail
            else -> { set(true); AuthResult.Success }
        }
    }

    override suspend fun signOut() = set(false)

    override suspend fun deleteAccount(): Boolean {
        set(false)
        return true
    }

    private companion object { const val KEY = "signed_in" }
}
