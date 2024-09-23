package com.diaz1401.chat.utilities

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(LocalConstants.KEY_PREFERENCE_NAME, Context.MODE_PRIVATE)

    private fun editPreferences(action: (SharedPreferences.Editor) -> Unit) {
        val editor = sharedPreferences.edit()
        action(editor)
        editor.apply()
    }

    fun putString(key: String?, value: String?) {
        editPreferences { it.putString(key, value) }
    }

    fun getString(key: String?): String? {
        return sharedPreferences.getString(key, null)
    }

    fun putBoolean(key: String?, value: Boolean) {
        editPreferences { it.putBoolean(key, value) }
    }

    fun getBoolean(key: String?): Boolean {
        return sharedPreferences.getBoolean(key, false)
    }

    fun clear() {
        editPreferences { it.clear() }
    }
}