package com.example.app.data.supabase

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** [status] is the HTTP status; [code] is the server's error code (e.g. invalid_credentials, premium_required). */
class ApiException(val status: Int, val code: String?, message: String) : Exception(message)

/**
 * Holds the signed-in session. TODO before release: move to Android Keystore-backed storage
 * (SharedPreferences is private to the app but not encrypted).
 */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val _signedIn = MutableStateFlow(prefs.getString(REFRESH, null) != null)
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    val accessToken: String? get() = prefs.getString(ACCESS, null)
    val refreshToken: String? get() = prefs.getString(REFRESH, null)
    val expiresAtSec: Long get() = prefs.getLong(EXPIRES, 0)

    /** The signed-in user's id, read from the access token's `sub` claim. */
    val userId: String?
        get() = accessToken?.split(".")?.getOrNull(1)?.let { part ->
            runCatching {
                val json = String(Base64.decode(part, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP), Charsets.UTF_8)
                JSONObject(json).getString("sub")
            }.getOrNull()
        }

    fun save(access: String, refresh: String, expiresAtSec: Long) {
        prefs.edit().putString(ACCESS, access).putString(REFRESH, refresh).putLong(EXPIRES, expiresAtSec).apply()
        _signedIn.value = true
    }

    fun clear() {
        prefs.edit().clear().apply()
        _signedIn.value = false
    }

    private companion object {
        const val ACCESS = "access"
        const val REFRESH = "refresh"
        const val EXPIRES = "expires_at"
    }
}

/**
 * Minimal Supabase REST client (no third-party SDK). [call] blocks, so it can also run on
 * Media3's loading thread; [callAsync] is for coroutines. Never logs request or response bodies.
 */
class SupabaseHttp(
    private val baseUrl: String,
    private val anonKey: String,
    val session: SessionStore,
) {
    private val refreshLock = Any()

    fun call(
        method: String,
        path: String,
        body: JSONObject? = null,
        authed: Boolean = true,
        headers: Map<String, String> = emptyMap(),
    ): String {
        if (authed) ensureFresh()
        return try {
            execute(method, path, body, authed, headers)
        } catch (e: ApiException) {
            // Token rejected: try one refresh, then one retry. Anything else is the caller's problem.
            if (authed && e.status == 401 && refresh()) execute(method, path, body, authed, headers) else throw e
        }
    }

    suspend fun callAsync(
        method: String,
        path: String,
        body: JSONObject? = null,
        authed: Boolean = true,
        headers: Map<String, String> = emptyMap(),
    ): String = withContext(Dispatchers.IO) { call(method, path, body, authed, headers) }

    private fun ensureFresh() {
        val now = System.currentTimeMillis() / 1000
        if (session.accessToken == null || session.expiresAtSec - 60 <= now) refresh()
    }

    /** True if a new session was stored. A refresh token the server rejects signs the person out. */
    private fun refresh(): Boolean = synchronized(refreshLock) {
        val refreshToken = session.refreshToken ?: return false
        try {
            val res = JSONObject(
                execute("POST", "/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", refreshToken), authed = false),
            )
            session.save(
                res.getString("access_token"),
                res.getString("refresh_token"),
                System.currentTimeMillis() / 1000 + res.getLong("expires_in"),
            )
            true
        } catch (e: ApiException) {
            if (e.status == 400 || e.status == 401) session.clear()
            false
        }
        // IOException (no network) is not caught: the person stays signed in and the caller shows a network error.
    }

    private fun execute(
        method: String,
        path: String,
        body: JSONObject?,
        authed: Boolean,
        headers: Map<String, String> = emptyMap(),
    ): String {
        val conn = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer " + ((if (authed) session.accessToken else null) ?: anonKey))
            conn.setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (status !in 200..299) throw errorFrom(status, text)
            return text
        } finally {
            conn.disconnect()
        }
    }

    // Shapes seen: auth {"error_code","msg"}, PostgREST {"code","message"}, our functions {"error"}.
    private fun errorFrom(status: Int, text: String): ApiException = try {
        val j = JSONObject(text)
        val code = j.optString("error_code").ifEmpty { j.optString("error") }.ifEmpty { j.optString("code") }.ifEmpty { null }
        ApiException(status, code, j.optString("msg").ifEmpty { j.optString("message") }.ifEmpty { "HTTP $status" })
    } catch (e: JSONException) {
        ApiException(status, null, "HTTP $status")
    }
}
