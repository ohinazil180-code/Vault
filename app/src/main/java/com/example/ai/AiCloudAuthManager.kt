package com.example.ai

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class CloudUserSession(
    val sessionId: String,
    val email: String,
    val displayName: String,
    val deviceName: String = "Pixel 8 (Private Vault Ω)",
    val createdAt: Long = System.currentTimeMillis()
)

class AiCloudAuthManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("aegis_ai_cloud_auth", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(prefs.getString("access_token", null) != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow(prefs.getString("user_email", ""))
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _backendUrl = MutableStateFlow(prefs.getString("backend_url", "https://api.aegis.internal") ?: "https://api.aegis.internal")
    val backendUrl: StateFlow<String> = _backendUrl.asStateFlow()

    private val _sessions = MutableStateFlow<List<CloudUserSession>>(
        listOf(
            CloudUserSession(
                sessionId = "sess_current_" + UUID.randomUUID().toString().take(8),
                email = prefs.getString("user_email", "agent@aegis.internal") ?: "agent@aegis.internal",
                displayName = "Security Officer",
                deviceName = "Pixel 8 Pro (Active)"
            )
        )
    )
    val sessions: StateFlow<List<CloudUserSession>> = _sessions.asStateFlow()

    fun login(email: String, password: String):Result<String> {
        if (email.isBlank() || !email.contains("@") || password.length < 6) {
            return Result.failure(Exception("Valid email and password (minimum 6 chars) required."))
        }
        val token = "jwt_aegis_" + UUID.randomUUID().toString()
        val refresh = "rft_aegis_" + UUID.randomUUID().toString()

        prefs.edit()
            .putString("access_token", token)
            .putString("refresh_token", refresh)
            .putString("user_email", email)
            .apply()

        _isLoggedIn.value = true
        _userEmail.value = email

        _sessions.value = listOf(
            CloudUserSession(
                sessionId = "sess_" + UUID.randomUUID().toString().take(8),
                email = email,
                displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                deviceName = "Current Device"
            )
        )
        return Result.success("Logged in successfully.")
    }

    fun register(email: String, password: String, displayName: String): Result<String> {
        if (email.isBlank() || !email.contains("@") || password.length < 8) {
            return Result.failure(Exception("Valid email and password (minimum 8 chars) required."))
        }
        return login(email, password)
    }

    fun logout() {
        prefs.edit()
            .remove("access_token")
            .remove("refresh_token")
            .remove("user_email")
            .apply()

        _isLoggedIn.value = false
        _userEmail.value = null
        _sessions.value = emptyList()
    }

    fun setBackendUrl(url: String) {
        prefs.edit().putString("backend_url", url).apply()
        _backendUrl.value = url
    }

    fun revokeSession(sessionId: String) {
        _sessions.value = _sessions.value.filter { it.sessionId != sessionId }
    }
}
