package com.mesaviva.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.window.OnBackInvokedDispatcher
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import com.mesaviva.app.shell.ShellSync
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

class GameWebActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var loading: LinearLayout? = null
    private var errorPanel: LinearLayout? = null
    private var pendingPermission: PermissionRequest? = null
    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null
    private var lanRelay: LanRelayServer? = null

    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private var accelerometer: Sensor? = null
    private var lastShakeAt = 0L
    private var lastMagnitude = 0f

    private val baseUrl: String
        get() {
            val route = intent.getStringExtra("route").orEmpty()
            val suffix = if (route.isBlank()) "" else "#${if (route.startsWith("/")) route else "/$route"}"
            return "https://appassets.androidplatform.net/assets/www/index.html$suffix"
        }

    private val assetLoader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#061713"))
            fitsSystemWindows = false
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
                    or WindowInsetsCompat.Type.ime(),
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val web = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#061713"))
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        webView = web

        root.addView(
            web,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        val splash = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#061713"))
            addView(ProgressBar(this@GameWebActivity).apply { isIndeterminate = true })
            addView(TextView(this@GameWebActivity).apply {
                text = "Mesa Viva"
                setTextColor(Color.parseColor("#F5F1E6"))
                textSize = 22f
                gravity = Gravity.CENTER
                setPadding(0, 28, 0, 0)
            })
        }
        loading = splash
        root.addView(
            splash,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        val error = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setBackgroundColor(Color.parseColor("#061713"))
            setPadding(48, 48, 48, 48)
            addView(TextView(this@GameWebActivity).apply {
                text = "No pudimos abrir la mesa"
                setTextColor(Color.parseColor("#F5F1E6"))
                textSize = 20f
                gravity = Gravity.CENTER
            })
            addView(TextView(this@GameWebActivity).apply {
                text = "Comprueba el espacio del dispositivo y vuelve a intentarlo."
                setTextColor(Color.parseColor("#9FB8AE"))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 12, 0, 24)
            })
            addView(Button(this@GameWebActivity).apply {
                text = "Reintentar"
                setOnClickListener {
                    visibility = View.GONE
                    loading?.visibility = View.VISIBLE
                    webView?.loadUrl(baseUrl)
                }
            })
        }
        errorPanel = error
        root.addView(
            error,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        setContent {
            AndroidView(factory = { root }, modifier = Modifier.fillMaxSize())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            ) { handleBack() }
        }
        configureWebView(web)
        web.loadUrl(baseUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(web: WebView) {
        val settings = web.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = false
        settings.userAgentString = settings.userAgentString + " MesaVivaAndroid/0.8.0"

        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        web.addJavascriptInterface(Bridge(), "MesaViva")

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if (url.host == "appassets.androidplatform.net") return false
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, url))
                    true
                } catch (_: Exception) {
                    true
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                loading?.visibility = View.GONE
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    loading?.visibility = View.GONE
                    errorPanel?.visibility = View.VISIBLE
                }
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (newProgress >= 100) loading?.visibility = View.GONE
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                if (!request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                    request.deny()
                    return
                }
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                } else {
                    pendingPermission?.deny()
                    pendingPermission = request
                    requestPermissions(arrayOf(Manifest.permission.CAMERA), REQ_CAMERA)
                }
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams,
            ): Boolean {
                pendingFileCallback?.onReceiveValue(null)
                pendingFileCallback = filePathCallback
                return try {
                    startActivityForResult(fileChooserParams.createIntent(), REQ_FILE)
                    true
                } catch (_: Exception) {
                    pendingFileCallback = null
                    false
                }
            }
        }
    }

    inner class Bridge {
        @JavascriptInterface
        fun vibrate(pattern: String) {
            val values = pattern.split(',').mapNotNull { it.trim().toLongOrNull() }.toLongArray()
            if (values.isEmpty()) return
            try {
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (values.size == 1) {
                        vibrator.vibrate(VibrationEffect.createOneShot(values[0], VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        vibrator.vibrate(VibrationEffect.createWaveform(values, -1))
                    }
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(values, -1)
                }
            } catch (_: Exception) {
                // La vibración es opcional.
            }
        }

        @JavascriptInterface
        fun systemAccent(): String {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val id = resources.getIdentifier("system_accent1_300", "color", "android")
                    if (id != 0) String.format("#%06X", 0xFFFFFF and getColor(id)) else "#E0A63A"
                } else {
                    "#E0A63A"
                }
            } catch (_: Exception) {
                "#E0A63A"
            }
        }

        @JavascriptInterface
        fun startLanServer(): String {
            if (lanRelay == null) lanRelay = LanRelayServer()
            return lanRelay?.start() ?: ""
        }

        @JavascriptInterface
        fun stopLanServer() {
            lanRelay?.stop()
            lanRelay = null
        }

        @JavascriptInterface
        fun lanServerUrl(): String = lanRelay?.url ?: ""

        @JavascriptInterface
        fun savePdf(filename: String, base64: String): Boolean {
            return try {
                val safeName = filename.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120).ifBlank { "mesa-viva.pdf" }
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        values.clear()
                        values.put(MediaStore.Downloads.IS_PENDING, 0)
                        contentResolver.update(uri, values, null, null)
                        runOnUiThread {
                            Toast.makeText(this@GameWebActivity, "PDF guardado en Descargas", Toast.LENGTH_LONG).show()
                        }
                        true
                    } else false
                } else {
                    val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
                    File(dir, safeName).writeBytes(bytes)
                    runOnUiThread {
                        Toast.makeText(this@GameWebActivity, "PDF guardado en ${dir.path}", Toast.LENGTH_LONG).show()
                    }
                    true
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CAMERA) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            val request = pendingPermission
            pendingPermission = null
            if (granted) {
                request?.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
            } else {
                request?.deny()
                Toast.makeText(this, "La cámara es necesaria para escanear el QR.", Toast.LENGTH_LONG).show()
            }
        }
    }

    @Deprecated("Activity result para el selector de archivos")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_FILE) {
            val callback = pendingFileCallback
            pendingFileCallback = null
            if (callback != null) {
                val result = if (resultCode == RESULT_OK && data != null) {
                    WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                } else {
                    null
                }
                callback.onReceiveValue(result)
            }
        }
    }

    private val shakeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt(x * x + y * y + z * z)
            val jerk = abs(magnitude - lastMagnitude)
            lastMagnitude = magnitude
            val now = System.currentTimeMillis()
            if (now - lastShakeAt < 1100) return
            if (magnitude > 19f || jerk > 17f) {
                lastShakeAt = now
                val intensity = (magnitude / 45f).coerceIn(0.35f, 1f)
                webView?.evaluateJavascript(
                    "window.dispatchEvent(new CustomEvent('mesaviva:shake',{detail:{intensity:$intensity}}));",
                    null,
                )
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let { sensorManager.registerListener(shakeListener, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(shakeListener)
        syncProfileSnapshot()
        webView?.onPause()
    }

    /** Guarda el perfil y los ajustes de la web para que la cáscara Compose los muestre. */
    private fun syncProfileSnapshot() {
        val web = webView ?: return
        web.evaluateJavascript("localStorage.getItem('mesa-viva-v1')") { value ->
            ShellSync.importFromWeb(this, value)
        }
    }

    private fun handleBack() {
        val web = webView
        if (web != null && web.canGoBack()) web.goBack() else finish()
    }

    @Deprecated("Compatibilidad con Android anterior a 13")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        handleBack()
    }

    override fun onDestroy() {
        lanRelay?.stop()
        lanRelay = null
        webView?.removeJavascriptInterface("MesaViva")
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    companion object {
        private const val REQ_CAMERA = 77
        private const val REQ_FILE = 88
    }
}

