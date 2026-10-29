# Relevo del proyecto · Mesa Viva

> **No borrar.** Este archivo existe para que cualquier chat, persona o IA sepa dónde quedó el proyecto. Se actualiza al cerrar cada versión, pero no se elimina.

## Estado actual

- **Versión publicada:** 1.5.0.
- **Juegos jugables:** 15.
- **Juegos 100 % Compose:** 15 (todos).
- **Juegos en WebView:** 0 (el WebView queda como respaldo de la PWA y de la sala remota).
- **Salas nativas LAN:** Damas y Mancala, con QR, chat, reconexión y bot de relleno.
- **Replays con semilla:** Damas y Mancala.
- **Crónica PDF:** 15 mesas, ahora con tarjeta de estadísticas cuando el juego la aporta.
- **Audio nativo:** música procedural por festividad y efectos en 10 mesas.
- **Repositorio:** <https://github.com/emanuelindustri-source/mesa-viva>
- **Portada:** <https://emanuelindustri-source.github.io/mesa-viva/>
- **Juego web:** <https://emanuelindustri-source.github.io/mesa-viva/play/>
- **Release:** <https://github.com/emanuelindustri-source/mesa-viva/releases/tag/v1.5.0>
- **VERSION:** `1.5.0`
- **Android:** `versionCode=17`, `versionName=1.5.0`, compileSdk/targetSdk 36.
- **Gradle:** 8.14.4.

### Calidad

- 30/30 pruebas de motores web.
- 65/65 pruebas Kotlin (15 juegos + 9 del códec de sala y semillas).
- **95 pruebas en total.**
- TypeScript sin errores.
- Web compilada con Vite 6.4.5 y sincronizada dentro del APK.
- APK debug, APK release firmado y AAB release firmado.
- Emulador Android 14: inicio, sala nativa, Damas, Mancala, chat, QR, replay y PDF verificados.
- Host real con invitado WebSocket y invitado real con host WebSocket.
- Escáner QR integrado verificado (permiso de cámara y `CaptureActivity`).
- Teléfono real `23117RA68G`: detectado por `adb`, pero MIUI bloquea la instalación por USB (“Instalación bloqueada de Mesa Viva vía USB”). Pendiente habilitar **Instalar vía USB** en Opciones de desarrollador.

### Checksums v1.5.0

- APK release firmado: `A7176DD549E5B361B3ED392FF13ADF51117E57E7A018A2E94B5AFC06FB1406A8` · 10.521.733 bytes.
- AAB release firmado: `DC333CEB0A201812AE1EEC8614A69F8334C03BE447594A90EA28131606807770` · 10.153.209 bytes.
- APK debug de CI: `22C03825AA4F29EAA906A9267B50E2D0F19E4A449B6D7A0CE9674D953FFB5E25` · 14.037.038 bytes.
- Artefactos locales en `artifacts/`: `mesa-viva-1.5.0-release.apk`, `mesa-viva-1.5.0-release.aab`. La release de GitHub añade `mesa-viva-1.5.0-debug.apk`.

## Compose

- **Cáscara nativa completa:** inicio, catálogo, niveles, ajustes, sala y navegación.
- **Quince juegos 100 % Compose.**
- **Salas nativas LAN:** relay Kotlin, protocolo JSON, QR, escáner, chat, reconexión y bot.
- **Replay con semilla:** Damas y Mancala, con paso anterior/siguiente.
- **Crónica PDF** en cada mesa: título, fecha, movimientos, estadísticas, paginación y compartición.
- **Audio nativo:** síntesis propia, sin samples; música por festividad y efectos.
- **XP real:** victorias, derrotas y empates actualizan nivel y racha.
- **Accesibilidad:** cartas y fichas descritas, turnos como región viva.
- **Pendiente:** salas de 3–8 jugadores, relay online nativo, replay exportable y accesibilidad completa.

## Siguiente versión: 1.6.0

Ver [`v1.6.0.md`](./v1.6.0.md). Incluye:

1. Salas nativas de 3–8 jugadores (Dominó, Ludo, Serpientes y Damas Chinas).
2. Relay online nativo y reconexión entre redes.
3. Exportación de replay y PDF con línea de tiempo.
4. Accesibilidad completa y audio en las quince mesas.
5. Teléfono físico, LAN real y checklist de Play Store.
6. Perfil de rendimiento y arranque en frío.

## Dónde está cada cosa

| Ruta | Contenido |
|---|---|
| `src/games/` | 15 motores web |
| `src/ui/GameSurface.tsx` | Superficies web |
| `src/core/` | Tipos, registro, Match autoritativo, niveles |
| `src/net/room.ts` | Salas web, host/invitado y reconexión |
| `server/room-server.mjs` | Relay Node |
| `android/.../MainActivity.kt` | Cáscara Compose y enlace `mesaviva://` |
| `android/.../GameWebActivity.kt` | WebView de respaldo + puentes + params nativos |
| `android/.../LanRelayServer.kt` | Relay LAN del anfitrión |
| `android/.../games/` | Motores Kotlin de los quince juegos |
| `android/.../shell/NativeRoom.kt` | Sesión host/invitado, protocolo y bots |
| `android/.../shell/RoomCodec.kt` | Códec JSON del estado de sala |
| `android/.../shell/RoomScreen.kt` | Lobby nativo, sala web y mesas LAN |
| `android/.../shell/NativeAudio.kt` | Música y efectos procedurales |
| `android/.../shell/NativeChronicle.kt` | Crónica PDF, MediaStore, FileProvider y compartición |
| `android/.../shell/CardUi.kt` | Barajas española, Furia y francesa, reversos y partículas |
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
- El QR nativo se genera localmente con ZXing; no se envía a un servicio.
- La sala nativa valida cada jugada en el anfitrión; el invitado nunca es autoridad.
- El diagnóstico se envía solo si el usuario abre el correo a `emanuel.industri@gmail.com`.
- El PDF se guarda en el dispositivo y solo se comparte si el usuario abre el selector de Android.
- El escáner QR usa la cámara solo con permiso explícito y no guarda imágenes.

## Reglas de continuidad

1. No borrar archivos de `versions/`.
2. Cada versión nueva crea su propio `vX.Y.Z.md`.
3. Mantener este `NEXT.md` actualizado, sin eliminarlo.
4. No guardar tokens, contraseñas ni keystores en el repositorio.
5. Antes de publicar: typecheck, tests web, tests Kotlin, build web, APK, AAB, CI/Pages/Android.
6. No afirmar “100 % Compose” global hasta retirar el WebView de la PWA y la sala remota. **Estado v1.5.0: los quince juegos jugables son Compose; el WebView solo queda como respaldo de la PWA y la sala remota.**
