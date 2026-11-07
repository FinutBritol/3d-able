package com.example.dbtest

import android.content.Context
import androidx.core.content.edit

object SessionManager {
    const val PREFS_NAME = "app_session"
    const val KEY_USERNAME = "logged_in_username"

    fun saveSession(context: Context, username: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_USERNAME, username)
            }
    }

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                remove(KEY_USERNAME)
            }
    }

    fun getLoggedInUsername(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USERNAME, null)
    }
}