package com.mubs.mobile.domain

import java.util.prefs.Preferences

actual class SessionManager {
    private val prefs = Preferences.userNodeForPackage(SessionManager::class.java)

    actual suspend fun saveToken(token: String) { prefs.put("token", token) }
    actual suspend fun getToken(): String? = prefs.get("token", null)
    actual suspend fun saveUsername(username: String) { prefs.put("username", username) }
    actual suspend fun getUsername(): String? = prefs.get("username", null)
    actual suspend fun saveRole(role: String) { prefs.put("role", role) }
    actual suspend fun getRole(): String? = prefs.get("role", null)
    actual suspend fun clear() { prefs.clear() }
}
