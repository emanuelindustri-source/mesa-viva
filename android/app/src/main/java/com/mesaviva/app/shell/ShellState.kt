package com.mesaviva.app.shell

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.util.Locale

enum class ShellScreen {
    Home,
    Catalog,
    Profile,
    Settings,
    Snakes,
    Domino,
    Chess,
    Ludo,
    Mancala,
    Backgammon,
    Damas,
    Go,
    Jungle,
    Xiangqi,
    Chino,
    Dados,
    Caida,
    Furia,
    Poker,
    Reversi,
    Ur,
    Brisca,
    Truco,
    Soroban,
    Shogi,
    Janggi,
    Room,
}

data class Festival(
    val id: String,
    val emoji: String,
    val nameEs: String,
    val nameEn: String,
    val accent: Long,
    val accent2: Long,
) {
    fun name(language: String): String = if (language == "en") nameEn else nameEs
}

fun festivalFor(date: LocalDate = LocalDate.now()): Festival {
    val month = date.monthValue
    val day = date.dayOfMonth
    return when {
        month == 10 -> Festival("halloween", "🎃", "Halloween criollo", "Creole Halloween", 0xFFE8842E, 0xFF9B5DE5)
        month == 12 -> Festival("navidad", "🎄", "Navidad", "Christmas", 0xFFD64545, 0xFF2E9E6B)
        month == 1 && day <= 6 -> Festival("reyes", "👑", "Día de Reyes", "Three Kings' Day", 0xFFE0A63A, 0xFF6C63C7)
        month == 2 && day <= 14 -> Festival("san-valentin", "💘", "San Valentín", "Valentine's Day", 0xFFE35D8A, 0xFFFFA8C5)
        month == 3 && day in 10..17 -> Festival("san-patricio", "🍀", "San Patricio", "Saint Patrick's Day", 0xFF3FA66A, 0xFFE0A63A)
        month == 7 && day == 5 -> Festival("independencia", "🇻🇪", "Independencia de Venezuela", "Venezuelan Independence", 0xFFF2C94C, 0xFF2F6FED)
        month == 6 && day == 24 -> Festival("san-juan", "🔥", "San Juan Bautista", "Saint John's Day", 0xFFF2994A, 0xFFEB5757)
        else -> Festival("mesa", "🪅", "Mesa criolla", "Creole table", 0xFFE0A63A, 0xFF4FB286)
    }
}

private val XP_THRESHOLDS = intArrayOf(0, 120, 300, 600, 1000, 1600, 2400, 3500, 5000, 7000)

fun levelForXp(xp: Int): Int {
    var level = 1
    for (index in XP_THRESHOLDS.indices) {
        if (xp >= XP_THRESHOLDS[index]) level = index + 1
    }
    return level.coerceIn(1, 10)
}

fun xpProgress(xp: Int): Float {
    val level = levelForXp(xp)
    val current = XP_THRESHOLDS[level - 1]
    val next = XP_THRESHOLDS.getOrNull(level) ?: return 1f
    if (next <= current) return 1f
    return ((xp - current).toFloat() / (next - current).toFloat()).coerceIn(0f, 1f)
}

/**
 * Estado de la cáscara nativa. La web sigue siendo la dueña del motor de la
 * partida; aquí guardamos preferencias de la cáscara y una instantánea del
 * perfil que GameWebActivity importa al volver.
 */
class ShellState(context: Context) {

    private val prefs = context.getSharedPreferences("mesa_viva_shell", Context.MODE_PRIVATE)
    private val defaultLanguage = if (Locale.getDefault().language == "en") "en" else "es"

    var screen by mutableStateOf(ShellScreen.Home)
    var roomLink by mutableStateOf<String?>(null)
    var pendingReplay by mutableStateOf<ReplayLink?>(null)
    var language by mutableStateOf(prefs.getString("language", defaultLanguage) ?: defaultLanguage)
    var dynamicColor by mutableStateOf(prefs.getBoolean("dynamicColor", true))
    var haptics by mutableStateOf(prefs.getBoolean("haptics", true))
    var music by mutableStateOf(prefs.getBoolean("music", true))
    var sfx by mutableStateOf(prefs.getBoolean("sfx", true))
    var reduceMotion by mutableStateOf(prefs.getBoolean("reduceMotion", false))
    var name by mutableStateOf(prefs.getString("name", "").orEmpty())
    var avatar by mutableStateOf(prefs.getString("avatar", "jaguar") ?: "jaguar")
    var country by mutableStateOf(prefs.getString("country", "VE") ?: "VE")
    var xp by mutableStateOf(prefs.getInt("xp", 0))
    var level by mutableStateOf(prefs.getInt("level", 1))
    var wins by mutableStateOf(prefs.getInt("wins", 0))
    var losses by mutableStateOf(prefs.getInt("losses", 0))
    var matches by mutableStateOf(prefs.getInt("matches", 0))
    var streak by mutableStateOf(prefs.getInt("streak", 0))
    var bestStreak by mutableStateOf(prefs.getInt("bestStreak", 0))

    val festival: Festival = festivalFor()

    fun t(key: String, params: Map<String, Any?> = emptyMap()): String = ShellStrings.t(language, key, params)

    fun refresh() {
        language = prefs.getString("language", language) ?: language
        dynamicColor = prefs.getBoolean("dynamicColor", dynamicColor)
        haptics = prefs.getBoolean("haptics", haptics)
        music = prefs.getBoolean("music", music)
        sfx = prefs.getBoolean("sfx", sfx)
        reduceMotion = prefs.getBoolean("reduceMotion", reduceMotion)
        name = prefs.getString("name", name).orEmpty()
        avatar = prefs.getString("avatar", avatar) ?: avatar
        country = prefs.getString("country", country) ?: country
        xp = prefs.getInt("xp", xp)
        level = prefs.getInt("level", level)
        wins = prefs.getInt("wins", wins)
        losses = prefs.getInt("losses", losses)
        matches = prefs.getInt("matches", matches)
        streak = prefs.getInt("streak", streak)
        bestStreak = prefs.getInt("bestStreak", bestStreak)
    }

    fun save() {
        prefs.edit()
            .putString("language", language)
            .putBoolean("dynamicColor", dynamicColor)
            .putBoolean("haptics", haptics)
            .putBoolean("music", music)
            .putBoolean("sfx", sfx)
            .putBoolean("reduceMotion", reduceMotion)
            .apply()
    }

    fun updateLanguage(value: String) {
        language = if (value == "en") "en" else "es"
        save()
    }

    fun updateDynamicColor(value: Boolean) {
        dynamicColor = value
        save()
    }

    fun updateHaptics(value: Boolean) {
        haptics = value
        save()
    }

    fun updateMusic(value: Boolean) {
        music = value
        save()
    }

    fun updateSfx(value: Boolean) {
        sfx = value
        save()
    }

    fun updateReduceMotion(value: Boolean) {
        reduceMotion = value
        save()
    }

    /**
     * Registra una partida terminada y reparte XP. Las victorias suman 90 XP,
     * las derrotas 45 y los empates 30. El nivel se recalcula con la misma
     * tabla que muestra la pantalla de perfil.
     */
    fun awardMatch(win: Boolean, draw: Boolean = false) {
        matches += 1
        when {
            draw -> {
                xp += 30
                streak = 0
            }
            win -> {
                wins += 1
                xp += 90
                streak += 1
                if (streak > bestStreak) bestStreak = streak
            }
            else -> {
                losses += 1
                xp += 45
                streak = 0
            }
        }
        level = levelForXp(xp)
        prefs.edit()
            .putInt("xp", xp)
            .putInt("level", level)
            .putInt("wins", wins)
            .putInt("losses", losses)
            .putInt("matches", matches)
            .putInt("streak", streak)
            .putInt("bestStreak", bestStreak)
            .apply()
    }

    fun vibrate(context: Context, millis: Long = 28L) {
        if (!haptics) return
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(millis)
            }
        } catch (_: Exception) {
            // La vibración es opcional.
        }
    }
}

