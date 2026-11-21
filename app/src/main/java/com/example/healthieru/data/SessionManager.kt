package com.example.healthieru.data

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    var userId: Long
        get() = prefs.getLong("userId", -1L)
        set(value) { prefs.edit().putLong("userId", value).apply() }

    fun isSignedIn() = userId != -1L
    fun clear() { prefs.edit().remove("userId").apply() }
}