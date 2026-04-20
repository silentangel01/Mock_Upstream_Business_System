package com.mubs.mobile.domain

import android.content.Context
import android.content.SharedPreferences

actual class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("mubs_session", Context.MODE_PRIVATE)

    actual suspend fun saveToken(token: String) {
        prefs.edit().putString("token", token).apply()
    }

    actual suspend fun getToken(): String? = prefs.getString("token", null)

    actual suspend fun saveUsername(username: String) {
        prefs.edit().putString("username", username).apply()
    }

    actual suspend fun getUsername(): String? = prefs.getString("username", null)

    actual suspend fun saveRole(role: String) {
        prefs.edit().putString("role", role).apply()
    }

    actual suspend fun getRole(): String? = prefs.getString("role", null)

    actual suspend fun clear() {
        prefs.edit().clear().apply()
    }
}
