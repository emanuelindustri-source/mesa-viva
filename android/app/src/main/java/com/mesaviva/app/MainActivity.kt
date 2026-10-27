package com.mesaviva.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.webkit.WebViewAssetLoader
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : Activity() {

    private var webView: WebView? = null
    private var loading: LinearLayout? = null
    private var errorPanel: LinearLayout? = null
    private var pendingPermission: PermissionRequest? = null
    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null

    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private var accelerometer: Sensor? = null
    private var lastShakeAt = 0L
    private var lastMagnitude = 0f

    private val baseUrl = "https://appassets.androidplatform.net/assets/www/index.html"

    private val assetLoader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#061713")
        window.navigationBarColor = Color.parseColor("#061713")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#061713"))
            fitsSystemWindows = true
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
            addView(ProgressBar(this@MainActivity).apply { isIndeterminate = true })
            addView(TextView(this@MainActivity).apply {
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
            addView(TextView(this@MainActivity).apply {
                text = "No pudimos abrir la mesa"
                setTextColor(Color.parseColor("#F5F1E6"))
                textSize = 20f
                gravity = Gravity.CENTER
            })
            addView(TextView(this@MainActivity).apply {
                text = "Comprueba el espacio del dispositivo y vuelve a intentarlo."
                setTextColor(Color.parseColor("#9FB8AE"))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 12, 0, 24)
            })
            addView(Button(this@MainActivity).apply {
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

        setContentView(root)
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
        settings.userAgentString = settings.userAgentString + " MesaVivaAndroid/0.1.0"

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
                            Toast.makeText(this@MainActivity, "PDF guardado en Descargas", Toast.LENGTH_LONG).show()
                        }
                        true
                    } else false
                } else {
                    val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
                    File(dir, safeName).writeBytes(bytes)
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "PDF guardado en ${dir.path}", Toast.LENGTH_LONG).show()
                    }
                    true
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
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
        webView?.onPause()
    }

    @Deprecated("Back tradicional del WebView")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val web = webView
        if (web != null && web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
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
