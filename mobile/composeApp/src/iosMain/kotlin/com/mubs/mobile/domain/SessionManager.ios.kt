package com.mubs.mobile.domain

import platform.Foundation.NSUserDefaults

actual class SessionManager {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual suspend fun saveToken(token: String) {
        defaults.setObject(token, forKey = "token")
    }

    actual suspend fun getToken(): String? =
        defaults.stringForKey("token")

    actual suspend fun saveUsername(username: String) {
        defaults.setObject(username, forKey = "username")
    }

    actual suspend fun getUsername(): String? =
        defaults.stringForKey("username")

    actual suspend fun saveRole(role: String) {
        defaults.setObject(role, forKey = "role")
    }

    actual suspend fun getRole(): String? =
        defaults.stringForKey("role")

    actual suspend fun clear() {
        listOf("token", "username", "role").forEach {
            defaults.removeObjectForKey(it)
        }
    }
}
