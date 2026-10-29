package com.mesaviva.app

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.mesaviva.app.shell.MesaVivaApp
import com.mesaviva.app.shell.MesaVivaTheme
import com.mesaviva.app.shell.NativeAudio
import com.mesaviva.app.shell.ShellScreen
import com.mesaviva.app.shell.ShellState

/**
 * Cáscara nativa en Jetpack Compose.
 *
 * Las pantallas de inicio, catálogo, niveles y ajustes son nativas. La mesa de
 * los juegos se abre en GameWebActivity, que hospeda el motor compartido con la
 * web; Serpientes y Escaleras ya es 100 % Compose.
 */
class MainActivity : ComponentActivity() {

    private var shell: ShellState? = null
    private var roomLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomLink = intent?.dataString?.takeIf { it.startsWith("mesaviva://") }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }

        setContent {
            val state = remember { ShellState(this) }
            shell = state
            LaunchedEffect(roomLink) {
                val link = roomLink ?: return@LaunchedEffect
                roomLink = null
                state.roomLink = link
                state.screen = ShellScreen.Room
            }
            MesaVivaTheme(state) {
                MesaVivaApp(
                    state = state,
                    onOpenWeb = { route ->
                        startActivity(
                            Intent(this, GameWebActivity::class.java)
                                .putExtra("route", route),
                        )
                    },
                    onOpenUrl = { url ->
                        runCatching { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.takeIf { it.startsWith("mesaviva://") }?.let { roomLink = it }
    }

    override fun onResume() {
        super.onResume()
        shell?.refresh()
        shell?.let { NativeAudio.startMusic(this, it.festival.id, it.music) }
    }

    override fun onPause() {
        NativeAudio.stopMusic()
        super.onPause()
    }
}
