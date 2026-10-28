package com.mesaviva.app.shell

import android.content.Context
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Importa la instantánea que la web guarda en localStorage (`mesa-viva-v1`)
 * para que la cáscara Compose muestre el mismo perfil sin duplicar el motor.
 */
object ShellSync {

    fun importFromWeb(context: Context, raw: String?) {
        if (raw.isNullOrBlank() || raw == "null") return
        try {
            val decoded = when (val value = JSONTokener(raw).nextValue()) {
                is String -> value
                is JSONObject -> value.toString()
                else -> return
            }
            val root = JSONObject(decoded)
            val state = root.optJSONObject("state") ?: return
            val profile = state.optJSONObject("profile")
            val settings = state.optJSONObject("settings")
            val prefs = context.getSharedPreferences("mesa_viva_shell", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            if (profile != null) {
                editor.putString("name", profile.optString("name", prefs.getString("name", "") ?: ""))
                editor.putString("avatar", profile.optString("avatar", prefs.getString("avatar", "jaguar") ?: "jaguar"))
                editor.putInt("xp", profile.optInt("xp", prefs.getInt("xp", 0)))
                editor.putInt("level", profile.optInt("level", prefs.getInt("level", 1)))
                editor.putInt("wins", profile.optInt("wins", prefs.getInt("wins", 0)))
                editor.putInt("losses", profile.optInt("losses", prefs.getInt("losses", 0)))
                editor.putInt("matches", profile.optInt("matches", prefs.getInt("matches", 0)))
                editor.putInt("bestStreak", profile.optInt("bestStreak", prefs.getInt("bestStreak", 0)))
            }
            if (settings != null) {
                editor.putString("language", settings.optString("language", prefs.getString("language", "es") ?: "es"))
                editor.putString("country", settings.optString("country", prefs.getString("country", "VE") ?: "VE"))
                editor.putBoolean("music", settings.optBoolean("music", prefs.getBoolean("music", true)))
                editor.putBoolean("sfx", settings.optBoolean("sfx", prefs.getBoolean("sfx", true)))
                editor.putBoolean("haptics", settings.optBoolean("haptics", prefs.getBoolean("haptics", true)))
                editor.putBoolean("reduceMotion", settings.optBoolean("reduceMotion", prefs.getBoolean("reduceMotion", false)))
            }
            editor.apply()
        } catch (_: Exception) {
            // Si la instantánea no es válida, la cáscara conserva sus valores.
        }
    }
}
