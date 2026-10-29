# Relevo del proyecto · Mesa Viva

> **No borrar.** Este archivo existe para que cualquier chat, persona o IA sepa dónde quedó el proyecto. Se actualiza al cerrar cada versión, pero no se elimina.

## Estado actual

- **Versión publicada:** 1.1.0.
- **Juegos jugables:** 15.
- **Juegos 100 % Compose:** 12 (Serpientes, Dominó, Ajedrez, Ludo, Mancala, Backgammon, Damas, Dados, Go, Jungle, Xiangqi y Damas Chinas).
- **Juegos en WebView:** 3 (Caída, Furia de Colores y Póker).
- **Repositorio:** <https://github.com/emanuelindustri-source/mesa-viva>
- **Portada:** <https://emanuelindustri-source.github.io/mesa-viva/>
- **Juego web:** <https://emanuelindustri-source.github.io/mesa-viva/play/>
- **Release:** <https://github.com/emanuelindustri-source/mesa-viva/releases/tag/v1.1.0>
- **VERSION:** `1.1.0`
- **Android:** `versionCode=13`, `versionName=1.1.0`, compileSdk/targetSdk 36.

### Calidad

- 30/30 pruebas de motores web.
- 36/36 pruebas Kotlin (Dominó, Ajedrez, Ludo, Mancala, Backgammon, Damas, Dados, Go, Jungle, Xiangqi y Damas Chinas).
- **66 pruebas en total.**
- TypeScript sin errores.
- Web compilada con Vite 6.4.5.
- APK debug, APK release firmado y AAB release firmado.
- Emulador Android 14: inicio, Damas, Go, Jungle, Xiangqi, Damas Chinas, Dados, movimientos, revelación y diálogos verificados.
- Teléfono real `23117RA68G`: no detectado por `adb` en esta sesión. Sigue pendiente “Instalar vía USB” de MIUI.

### Checksums v1.1.0

- APK release firmado: `7B66158DDA81F0F71BD63F7ABD54CCFD66045681083F7BE8B4435D502834AE94` · 10.167.061 bytes.
- AAB release firmado: `C159BE20DBA2045B44FB76C3E87CE0A89CB112FA62FD35A13667A0BCAF5CFC24` · 9.805.620 bytes.
- APK debug de CI: `98CB8E8C1054005D77BA7C787FC474F779CF17B13AB67E31C2EDDC3EA144FD24` · 13.469.150 bytes.
- Artefactos locales en `artifacts/`: `mesa-viva-1.1.0-release.apk`, `mesa-viva-1.1.0-release.aab`.

## Compose

- **Cáscara nativa completa:** inicio, catálogo, niveles, ajustes, sala y navegación.
- **Doce juegos 100 % Compose:** los seis de v1.0.0 más Damas, Dados, Go, Jungle, Xiangqi y Damas Chinas.
- **Sala nativa:** crear/unirse, QR local ZXing, escaneo y selector de ronda.
- **Sincronización:** la cáscara pasa idioma/música/efectos/vibración/movimiento a la web; `ShellSync` devuelve el perfil.
- **Pendiente:** salas nativas, Caída/Furia/Póker nativos, PDF/replays nativos y retirada del WebView.

## Siguiente versión: 1.2.0

Ver [`v1.2.0.md`](./v1.2.0.md). Incluye:

1. Salas nativas host/invitado y reconexión.
2. Caída, Furia y Póker nativos hasta la paridad total.
3. Crónica nativa con PDF y replays con semilla.
4. Accesibilidad completa y prueba en teléfono físico.
5. Gradle 8.14.4+, perfil de rendimiento y checklist de tienda.
6. Retirada progresiva del WebView.

## Dónde está cada cosa

| Ruta | Contenido |
|---|---|
| `src/games/` | 15 motores web, incluido `poker.ts` |
| `src/ui/GameSurface.tsx` | Superficies web, incluida `PokerSurface` |
| `src/core/` | Tipos, registro, Match autoritativo, niveles |
| `src/net/room.ts` | Salas, host/invitado y reconexión |
| `server/room-server.mjs` | Relay Node |
| `android/.../MainActivity.kt` | Cáscara Compose |
| `android/.../GameWebActivity.kt` | Mesa compartida WebView + puentes + params nativos |
| `android/.../games/` | Motores Kotlin: Dominó, Ajedrez, Ludo, Mancala, Backgammon, Damas, Dados, Go, Jungle, Xiangqi y Damas Chinas |
| `android/.../shell/` | Estado, textos, tema, pantallas, sala, QR y las doce mesas |
| `landing/` | Portada estática con historial de versiones |
| `tools/build-pages.mjs` | Portada + juego en `dist-site/` |
| `versions/` | Evidencia inmutable por versión |

## Comandos

```bash
npm install
npm run dev                 # web en 5173
npm run typecheck
npm test
npm run test:relay          # requiere npm run rooms
npm run build
npm run build:pages         # portada + juego en dist-site/
npm run android:sync
cd android && gradlew.bat testDebugUnitTest
cd android && gradlew.bat assembleRelease
cd android && gradlew.bat bundleRelease
```

## Seguridad

- El token de GitHub compartido en el chat debe revocarse.
- El keystore y la contraseña viven en `C:\Users\Sr.Hidalgo\.mesa-viva\`; no se copian al repositorio.
- `android/keystore.properties` está ignorado por git.
- El QR se genera localmente con ZXing; no se envía a un servicio.
- El diagnóstico se envía solo si el usuario abre el correo a `emanuel.industri@gmail.com`.

## Reglas de continuidad

1. No borrar archivos de `versions/`.
2. Cada versión nueva crea su propio `vX.Y.Z.md`.
3. Mantener este `NEXT.md` actualizado, sin eliminarlo.
4. No guardar tokens, contraseñas ni keystores en el repositorio.
5. Antes de publicar: typecheck, tests web, tests Kotlin, build web, APK, AAB, CI/Pages/Android.
6. No afirmar “100 % Compose” global hasta retirar el WebView de los 15 juegos.
