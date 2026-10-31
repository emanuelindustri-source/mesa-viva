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
import com.mesaviva.app.shell.ReplayCodec
import com.mesaviva.app.shell.ShellScreen
import com.mesaviva.app.shell.ShellState
import com.mesaviva.app.shell.screenForGame

/**
 * Cáscara nativa en Jetpack Compose.
 *
 * Las pantallas de inicio, catálogo, niveles y ajustes son nativas. La mesa de
 * los juegos se abre en GameWebActivity, que hospeda el motor compartido con la
 * web; Serpientes y Escaleras ya es 100 % Compose.
 */
class MainActivity : ComponentActivity() {

    private var shell: ShellState? = null
    private var deepLink by mutableStateOf<String?>(null)
    private var requestedScreen by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLink = intent?.dataString?.takeIf { it.startsWith("mesaviva://") }
        requestedScreen = intent?.getStringExtra("screen")
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
            LaunchedEffect(requestedScreen) {
                val id = requestedScreen ?: return@LaunchedEffect
                requestedScreen = null
                screenForGame(id)?.let { state.screen = it }
            }
            LaunchedEffect(deepLink) {
                val link = deepLink ?: return@LaunchedEffect
                deepLink = null
                when {
                    link.startsWith("mesaviva://join") -> {
                        state.roomLink = link
                        state.screen = ShellScreen.Room
                    }
                    link.startsWith("mesaviva://replay") -> {
                        val replay = ReplayCodec.parse(link) ?: return@LaunchedEffect
                        state.pendingReplay = replay
                        state.screen = if (replay.game == ReplayCodec.DAMAS) {
                            ShellScreen.Damas
                        } else {
                            ShellScreen.Mancala
                        }
                    }
                }
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
        intent.dataString?.takeIf { it.startsWith("mesaviva://") }?.let { deepLink = it }
        intent.getStringExtra("screen")?.let { requestedScreen = it }
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
