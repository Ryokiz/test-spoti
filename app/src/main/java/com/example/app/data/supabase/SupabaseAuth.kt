package com.example.app.data.supabase

import com.example.app.data.AuthFailure
import com.example.app.data.AuthRepository
import com.example.app.data.AuthResult
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class SupabaseAuth(private val http: SupabaseHttp) : AuthRepository {
    override val signedIn: StateFlow<Boolean> = http.session.signedIn

    override suspend fun signIn(email: String, password: String): AuthResult = guard {
        val res = JSONObject(
            http.callAsync("POST", "/auth/v1/token?grant_type=password", credentials(email, password), authed = false),
        )
        store(res)
        AuthResult.Success
    }

    override suspend fun signUp(email: String, password: String): AuthResult = guard {
        val res = JSONObject(http.callAsync("POST", "/auth/v1/signup", credentials(email, password), authed = false))
        // With email confirmation on there is no session yet. For an address that already exists Supabase answers
        // the same way (to avoid revealing who has an account), so "email taken" cannot be told apart here.
        if (res.has("access_token")) {
            store(res)
            AuthResult.Success
        } else {
            AuthResult.ConfirmEmail
        }
    }

    override suspend fun signOut() {
        try {
            http.callAsync("POST", "/auth/v1/logout?scope=local")
        } catch (e: Exception) {
            // Even if the server cannot be reached, this device must end up signed out.
        }
        http.session.clear()
    }

    override suspend fun deleteAccount(): Boolean = try {
        http.callAsync("POST", "/functions/v1/delete-account", JSONObject().put("confirm", true))
        http.session.clear()
        true
    } catch (e: Exception) {
        false
    }

    private fun credentials(email: String, password: String) = JSONObject().put("email", email).put("password", password)

    private fun store(res: JSONObject) = http.session.save(
        res.getString("access_token"),
        res.getString("refresh_token"),
        System.currentTimeMillis() / 1000 + res.getLong("expires_in"),
    )

    private suspend fun guard(block: suspend () -> AuthResult): AuthResult = try {
        block()
    } catch (e: ApiException) {
        when (e.code) {
            "email_not_confirmed" -> AuthResult.ConfirmEmail
            "invalid_credentials" -> AuthResult.Failure(AuthFailure.InvalidCredentials)
            "user_already_exists", "email_exists" -> AuthResult.Failure(AuthFailure.EmailTaken)
            "weak_password" -> AuthResult.Failure(AuthFailure.WeakPassword)
            else -> AuthResult.Failure(AuthFailure.Unknown)
        }
    } catch (e: IOException) {
        AuthResult.Failure(AuthFailure.Network)
    } catch (e: JSONException) {
        AuthResult.Failure(AuthFailure.Unknown)
    }
}
