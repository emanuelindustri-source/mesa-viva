# Evidencia · Mesa Viva v2.9.1

- **Fecha:** 2026-10-31
- **Android:** emulador `Pixel_7_API_34` · Android 14 (API 34)
- **APK verificado:** `mesa-viva-2.9.1-debug.apk`
- **Build:** `versionCode=33`, `versionName=2.9.1`, targetSdk 36.

| Archivo | Contenido |
|---|---|
| `settings.png` | Ajustes con «Mesa Viva 2.9.1 · Android 16 (API 36)». |

## Parche

- Se eliminaron las claves `luzhanqi.rank.*` que ya no usaba la interfaz.
- `python tools/check_i18n.py` devuelve **missing 0**.
- 153 pruebas Kotlin + 50 web = **203 verdes**.
- APK debug, APK release firmado y AAB release firmado.
- La release v2.9.0 se conserva intacta; este parche no la sobrescribe.
