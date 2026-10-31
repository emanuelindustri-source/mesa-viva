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
    Aeroplane,
    Mahjong,
    Riichi,
    MahjongCards,
    Luzhanqi,
    MexicanTrain,
    Chickenfoot,
    ThreeChess,
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

private data class FestivalWindow(
    val festival: Festival,
    val start: LocalDate,
    val end: LocalDate,
    val priority: Int,
)

private val MESA = Festival("mesa", "🪅", "Mesa criolla", "Creole table", 0xFFE0A63A, 0xFF4FB286)
private val HALLOWEEN = Festival("halloween", "🎃", "Halloween criollo", "Creole Halloween", 0xFFF0A23A, 0xFFA855F7)
private val NAVIDAD = Festival("navidad", "🎄", "Navidad", "Christmas", 0xFFD94F4F, 0xFFE7C66A)
private val REYES = Festival("reyes", "👑", "Día de Reyes", "Three Kings' Day", 0xFFD4A62A, 0xFF6A4FB3)
private val CARNAVAL = Festival("carnaval", "🎭", "Carnaval", "Carnival", 0xFFFF5D8F, 0xFF22C1C3)
private val SAN_VALENTIN = Festival("san-valentin", "💘", "San Valentín", "Valentine's Day", 0xFFFF6B8A, 0xFFFFB3C1)
private val SAN_PATRICIO = Festival("san-patricio", "☘️", "San Patricio", "Saint Patrick's Day", 0xFF4CAF7D, 0xFFA7D129)
private val SEMANA_SANTA = Festival("semana-santa", "🕯️", "Semana Santa", "Holy Week", 0xFFA78BFA, 0xFFD6C7A1)
private val CORPUS = Festival("corpus", "🥁", "Corpus Christi", "Corpus Christi", 0xFFE25822, 0xFFF2C14E)
private val CRUZ_DE_MAYO = Festival("cruz-de-mayo", "🌺", "Cruz de Mayo", "May Cross", 0xFFF0A6C8, 0xFF8BC34A)
private val DIA_DE_MUERTOS = Festival("dia-de-muertos", "💀", "Día de Muertos", "Day of the Dead", 0xFFF28C28, 0xFFB33FA1)
private val SAN_JUAN = Festival("san-juan", "🔥", "San Juan Bautista", "Saint John's Day", 0xFFF2994A, 0xFFEB5757)

private val INDEPENDENCE_DATES = mapOf(
    "VE" to LocalDate.of(1, 7, 5),
    "CO" to LocalDate.of(1, 7, 20),
    "AR" to LocalDate.of(1, 7, 9),
    "MX" to LocalDate.of(1, 9, 16),
    "CL" to LocalDate.of(1, 9, 18),
    "PE" to LocalDate.of(1, 7, 28),
    "EC" to LocalDate.of(1, 8, 10),
    "BO" to LocalDate.of(1, 8, 6),
    "UY" to LocalDate.of(1, 8, 25),
    "PY" to LocalDate.of(1, 5, 14),
    "PA" to LocalDate.of(1, 11, 3),
    "CR" to LocalDate.of(1, 9, 15),
    "DO" to LocalDate.of(1, 2, 27),
    "US" to LocalDate.of(1, 7, 4),
    "BR" to LocalDate.of(1, 9, 7),
    "PT" to LocalDate.of(1, 6, 10),
    "DE" to LocalDate.of(1, 10, 3),
    "FR" to LocalDate.of(1, 7, 14),
    "IT" to LocalDate.of(1, 6, 2),
    "RU" to LocalDate.of(1, 6, 12),
    "CN" to LocalDate.of(1, 10, 1),
    "TW" to LocalDate.of(1, 10, 10),
    "JP" to LocalDate.of(1, 2, 11),
)

private val COUNTRY_FLAGS = mapOf(
    "VE" to "🇻🇪", "CO" to "🇨🇴", "AR" to "🇦🇷", "MX" to "🇲🇽", "CL" to "🇨🇱",
    "PE" to "🇵🇪", "EC" to "🇪🇨", "BO" to "🇧🇴", "UY" to "🇺🇾", "PY" to "🇵🇾",
    "PA" to "🇵🇦", "CR" to "🇨🇷", "DO" to "🇩🇴", "US" to "🇺🇸", "BR" to "🇧🇷",
    "PT" to "🇵🇹", "DE" to "🇩🇪", "FR" to "🇫🇷", "IT" to "🇮🇹", "RU" to "🇷🇺",
    "CN" to "🇨🇳", "TW" to "🇹🇼", "JP" to "🇯🇵",
)

/** Pascua gregoriana (Meeus/Jones/Butcher), usada por Carnaval, Semana Santa y Corpus. */
fun easterSunday(year: Int): LocalDate {
    val a = year % 19
    val b = year / 100
    val c = year % 100
    val d = b / 4
    val e = b % 4
    val f = (b + 8) / 25
    val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30
    val i = c / 4
    val k = c % 4
    val l = (32 + 2 * e + 2 * i - h - k) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val month = (h + l - 7 * m + 114) / 31
    val day = ((h + l - 7 * m + 114) % 31) + 1
    return LocalDate.of(year, month, day)
}

/**
 * Festividad vigente para una fecha y un país. Las ventanas se solapan a
 * propósito; gana la de mayor prioridad. Sin coincidencia, la mesa neutral.
 */
fun festivalFor(date: LocalDate = LocalDate.now(), country: String = "VE"): Festival {
    val code = country.uppercase()
    val year = date.year
    val easter = easterSunday(year)
    val windows = mutableListOf<FestivalWindow>()

    fun add(festival: Festival, start: LocalDate, end: LocalDate, priority: Int) {
        windows += FestivalWindow(festival, start, end, priority)
    }

    add(SEMANA_SANTA, easter.minusDays(7), easter.plusDays(7), 100)
    add(CARNAVAL, easter.minusDays(50), easter.minusDays(44), 95)
    add(REYES, LocalDate.of(year, 1, 5), LocalDate.of(year, 1, 6), 90)
    add(NAVIDAD, LocalDate.of(year, 12, 20), LocalDate.of(year, 12, 31), 85)
    add(NAVIDAD, LocalDate.of(year, 1, 1), LocalDate.of(year, 1, 6), 84)
    add(HALLOWEEN, LocalDate.of(year, 10, 25), LocalDate.of(year, 11, 2), 75)
    if (code == "MX") add(DIA_DE_MUERTOS, LocalDate.of(year, 11, 1), LocalDate.of(year, 11, 2), 78)
    add(SAN_VALENTIN, LocalDate.of(year, 2, 7), LocalDate.of(year, 2, 14), 60)
    add(SAN_PATRICIO, LocalDate.of(year, 3, 10), LocalDate.of(year, 3, 17), 55)
    if (code == "VE") {
        add(CORPUS, easter.plusDays(59), easter.plusDays(61), 70)
        add(CRUZ_DE_MAYO, LocalDate.of(year, 5, 1), LocalDate.of(year, 5, 5), 50)
        add(SAN_JUAN, LocalDate.of(year, 6, 23), LocalDate.of(year, 6, 24), 45)
    }
    val independence = INDEPENDENCE_DATES[code]
    if (independence != null) {
        val day = independence.withYear(year)
        add(
            Festival(
                "independencia",
                COUNTRY_FLAGS[code] ?: "🎉",
                "Independencia",
                "Independence Day",
                0xFFF4C542,
                0xFF2F6FDB,
            ),
            day.minusDays(2),
            day.plusDays(2),
            10,
        )
    }
    return windows
        .filter { !date.isBefore(it.start) && !date.isAfter(it.end) }
        .maxByOrNull { it.priority }
        ?.festival
        ?: MESA
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

    val festival: Festival get() = festivalFor(LocalDate.now(), country)

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

