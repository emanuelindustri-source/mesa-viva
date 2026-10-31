# Evidencia · Mesa Viva v2.8.0

- **Fecha:** 2026-10-31
- **Android:** emulador `Pixel_7_API_34` · Android 14 (API 34)
- **APK verificado:** `mesa-viva-2.8.0-debug.apk`
- **Build:** `versionCode=31`, `versionName=2.8.0`, targetSdk 36.

| Archivo | Contenido |
|---|---|
| `home.png` | Inicio 2.8.0 con 30 juegos Android, Halloween criollo y 11 idiomas. |
| `settings.png` | Ajustes con «Mesa Viva 2.8.0 · Android 16 (API 36)». |
| `web-qa.md` | Registro de la prueba funcional de Mahjong en el navegador. |

La QA web se hizo con el servidor de desarrollo en `http://localhost:5173/#/setup/mahjong`: se inició una mesa contra tres bots, se descartó una pieza, los bots jugaron sus turnos y la mesa volvió al humano sin errores de consola. La captura visual del navegador no está disponible porque la herramienta de captura exige una ventana visible; el registro textual y el código de la mesa quedan como evidencia.
