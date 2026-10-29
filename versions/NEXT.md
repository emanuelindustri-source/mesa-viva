# Relevo del proyecto · Mesa Viva

> **No borrar.** Este archivo existe para que cualquier chat, persona o IA sepa dónde quedó el proyecto. Se actualiza al cerrar cada versión, pero no se elimina.

## Estado actual

- **Versión publicada:** 1.7.0.
- **Juegos jugables:** 15.
- **Juegos 100 % Compose:** 15 (todos).
- **Juegos en WebView:** 0 (el WebView queda como respaldo de la PWA y de la sala remota).
- **Salas nativas:** Damas y Mancala (2), Dominó (2–4) y Serpientes (2–4), por LAN o relay online configurable.
- **Replays:** semilla, paso a paso, automático, enlace, archivo JSON, importación y visor web.
- **Crónica PDF:** 15 mesas, con tarjeta de estadísticas.
- **Audio nativo:** música procedural por festividad y efectos en las 15 mesas.
- **Repositorio:** <https://github.com/emanuelindustri-source/mesa-viva>
- **Portada:** <https://emanuelindustri-source.github.io/mesa-viva/>
- **Juego web:** <https://emanuelindustri-source.github.io/mesa-viva/play/>
- **Replay web:** <https://emanuelindustri-source.github.io/mesa-viva/replay/>
- **Release:** <https://github.com/emanuelindustri-source/mesa-viva/releases/tag/v1.7.0>
- **VERSION:** `1.7.0`
- **Android:** `versionCode=19`, `versionName=1.7.0`, compileSdk/targetSdk 36.
- **Gradle:** 8.14.4.

### Calidad

- 30/30 pruebas de motores web.
- 82/82 pruebas Kotlin (juegos, códec de salas y replays).
- **112 pruebas en total.**
- TypeScript sin errores.
- Web compilada con Vite 6.4.5 y sincronizada dentro del APK.
- APK debug, APK release firmado y AAB release firmado.
- Dominó probado con anfitrión real + 3 invitados WebSocket; cada invitado recibió solo su mano.
- Visor web probado en navegador real, sin errores de consola.
- Replay automático probado en Android.
- Arranque en frío medido en emulador Android 14: 3.278 / 2.616 / 2.737 ms (v1.6.0).
- Teléfono real `23117RA68G`: detectado por `adb`, pero MIUI bloquea la instalación por USB (“Instalación bloqueada de Mesa Viva vía USB”). Pendiente habilitar **Instalar vía USB** en Opciones de desarrollador.

### Checksums v1.7.0

- APK release firmado: `C6DBBD6F9F265C12D98D2E3632F9E98D88BA51FB7B7AB53F99EEFEA0B9942844` · 10.587.289 bytes.
- AAB release firmado: `D9B3522F3FB23C2F36F9D8511510174AEBF06F81724DFEE6A907B47945A6C383` · 10.208.630 bytes.
- APK debug de CI: `A9FC685C1EF1B484DDAA09B114F18CB1810F1ACAD42542A6356D214739FFB71E` · 14.118.978 bytes.
- Artefactos locales en `artifacts/`: `mesa-viva-1.7.0-release.apk`, `mesa-viva-1.7.0-release.aab`. La release de GitHub añade `mesa-viva-1.7.0-debug.apk`.

## Compose

- **Cáscara nativa completa:** inicio, catálogo, niveles, ajustes, sala y navegación.
- **Quince juegos 100 % Compose.**
- **Salas nativas:** Damas, Mancala, Dominó y Serpientes con asientos, QR, escáner, chat, reconexión y bots.
- **Dominó con manos privadas:** el códec redacta las manos ajenas y el pozo.
- **Replay automático** en Damas y Mancala y **visor web** con tablero.
- **Crónica PDF** en cada mesa: título, fecha, movimientos, estadísticas, paginación y compartición.
- **Audio nativo:** síntesis propia, sin samples; música por festividad y efectos en 15 mesas.
- **XP real:** victorias, derrotas y empates actualizan nivel y racha.
- **Accesibilidad:** cartas, fichas, casillas, puntos de Backgammon, tablero de Go y turnos descritos.
- **Pendiente:** Ludo/Damas Chinas, relay público, replay web de Póker/Caída/Furia y accesibilidad completa.

## Siguiente versión: 1.8.0

Ver [`v1.8.0.md`](./v1.8.0.md). Incluye:

1. Ludo y Damas Chinas nativos de 3–6 jugadores.
2. Relay público con TLS, límites y enlaces universales.
3. Replay web de Póker, Caída y Furia.
4. Accesibilidad completa en las quince mesas.
5. Teléfono físico y checklist de Play Store.
6. Baseline Profile, macrobenchmark y optimización de arranque.

## Dónde está cada cosa

| Ruta | Contenido |
|---|---|
| `src/games/` | 15 motores web |
| `src/ui/GameSurface.tsx` | Superficies web |
| `src/core/` | Tipos, registro, Match autoritativo, niveles |
| `src/net/room.ts` | Salas web, host/invitado y reconexión |
| `server/room-server.mjs` | Relay Node |
| `landing/replay/index.html` | Visor web de replays |
| `android/.../MainActivity.kt` | Cáscara Compose y enlaces `mesaviva://join` y `mesaviva://replay` |
| `android/.../GameWebActivity.kt` | WebView de respaldo + puentes + params nativos |
| `android/.../LanRelayServer.kt` | Relay LAN del anfitrión |
| `android/.../games/` | Motores Kotlin de los quince juegos |
| `android/.../shell/NativeRoom.kt` | Sesión host/invitado, asientos, protocolo y bots |
| `android/.../shell/RoomCodec.kt` | Códec JSON con redacción de manos de Dominó |
| `android/.../shell/SnakesRoom.kt` | Reglas puras de Serpientes para 2–4 |
| `android/.../shell/ReplayExport.kt` | Enlaces nativos/web, archivo JSON e importación |
| `android/.../shell/RoomScreen.kt` | Lobby, sala web y mesas LAN/online |
| `android/.../shell/NativeAudio.kt` | Música y efectos procedurales |
| `android/.../shell/NativeChronicle.kt` | Crónica PDF, MediaStore, FileProvider y compartición |
| `landing/` | Portada estática con historial de versiones |
| `tools/build-pages.mjs` | Portada + juego + replay en `dist-site/` |
| `versions/` | Evidencia inmutable por versión |

## Comandos

```bash
npm install
npm run dev                 # web en 5173
npm run typecheck
npm test
npm run test:relay          # requiere npm run rooms
npm run build
npm run build:pages         # portada + juego + replay en dist-site/
npm run android:sync
cd android && gradlew.bat testDebugUnitTest
cd android && gradlew.bat assembleRelease
cd android && gradlew.bat bundleRelease
```

## Seguridad

- El token de GitHub compartido en el chat debe revocarse.
- El keystore y la contraseña viven en `C:\Users\Sr.Hidalgo\.mesa-viva\`; no se copian al repositorio.
- `android/keystore.properties` está ignorado por git.
- El QR nativo se genera localmente con ZXing; no se envía a un servicio.
- La sala nativa valida cada jugada en el anfitrión; el invitado nunca es autoridad.
- Las manos de Dominó se redactan por asiento; el pozo solo viaja como conteo.
- El replay es autocontenido y no incluye datos personales.
- El relay online es opcional y debe ser de confianza; se admite `ws://` y `wss://`.
- El diagnóstico se envía solo si el usuario abre el correo a `emanuel.industri@gmail.com`.
- El PDF se guarda en el dispositivo y solo se comparte si el usuario abre el selector de Android.
- El escáner QR usa la cámara solo con permiso explícito y no guarda imágenes.

## Reglas de continuidad

1. No borrar archivos de `versions/`.
2. Cada versión nueva crea su propio `vX.Y.Z.md`.
3. Mantener este `NEXT.md` actualizado, sin eliminarlo.
4. No guardar tokens, contraseñas ni keystores en el repositorio.
5. Antes de publicar: typecheck, tests web, tests Kotlin, build web, APK, AAB, CI/Pages/Android.
6. No afirmar “100 % Compose” global hasta retirar el WebView de la PWA y la sala remota. **Estado v1.7.0: los quince juegos jugables son Compose; el WebView solo queda como respaldo de la PWA y la sala remota.**
