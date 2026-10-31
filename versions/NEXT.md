# Relevo del proyecto · Mesa Viva

> **No borrar.** Este archivo existe para que cualquier chat, persona o IA sepa dónde quedó el proyecto. Se actualiza al cerrar cada versión, pero no se elimina.

## Estado actual

- **Versión publicada:** 2.7.0.
- **Catálogo:** 30 juegos.
- **Juegos jugables:** 30, todos 100 % Jetpack Compose.
- **Juegos en WebView:** 0 (el WebView queda como respaldo de la PWA y de la sala remota).
- **Salas nativas:** Damas y Mancala (2), Dominó (2–4), Serpientes (2–4), Brisca (2–4), Truco (2), Ludo (2–4) y Damas Chinas (2–6), por LAN o relay online configurable.
- **Manos privadas:** Dominó, Brisca y Truco.
- **Replays:** semilla, paso a paso, automático, enlace, archivo JSON, importación y visor web de Damas/Mancala.
- **Crónica PDF:** 30 mesas, con tarjeta de estadísticas.
- **Audio nativo:** música procedural por festividad y efectos en las 30 mesas.
- **Repositorio:** <https://github.com/emanuelindustri-source/mesa-viva>
- **Portada:** <https://emanuelindustri-source.github.io/mesa-viva/>
- **Juego web:** <https://emanuelindustri-source.github.io/mesa-viva/play/>
- **Replay web:** <https://emanuelindustri-source.github.io/mesa-viva/replay/>
- **Release:** <https://github.com/emanuelindustri-source/mesa-viva/releases/tag/v2.7.0>
- **VERSION:** `2.7.0`
- **Android:** `versionCode=29`, `versionName=2.7.0`, compileSdk/targetSdk 36.
- **Gradle:** 8.14.4.

### Calidad

- 30/30 pruebas de motores web.
- 146/146 pruebas Kotlin (juegos, salas, replays, Mahjong, Luzhanqi, trenes, ajedrez para tres, Reversi, Ur, Brisca, Truco, Ludo, Damas Chinas, Soroban, Shogi, Janggi, Aeroplane y códecs).
- **176 pruebas en total.**
- TypeScript sin errores.
- Web compilada con Vite 6.4.5 y sincronizada dentro del APK.
- APK debug, APK release firmado y AAB release firmado.
- Mesas nuevas verificadas en emulador Android 14.
- 30 juegos nativos en el catálogo; 0 pendientes.
- La web conserva 15 juegos; la paridad web queda para v2.8.0.
- Teléfono físico `22101316G`: detectado por `adb`, pero MIUI rechaza la instalación USB.

### Checksums v2.7.0

- APK release firmado: `8414012DE3103E33454C779A44258F354478FD5BF542833018ED5B417B2678A7` · 11.046.041 bytes.
- AAB release firmado: `8FD7EB1722C0682091FF30FE3B2F4FE9950C4CF4BD0706087E97EB8FC3CFD741` · 10.675.504 bytes.
- APK debug: `F8C5C33FABD054D4073019AB131B1D3EAF8BC2B2969211AAD909433E5479C567` · 16.671.605 bytes.
- Artefactos locales en `artifacts/`: `mesa-viva-2.7.0-release.apk`, `mesa-viva-2.7.0-release.aab` y `mesa-viva-2.7.0-debug.apk`.

## Compose

- **Cáscara nativa completa:** inicio, catálogo, niveles, ajustes, sala y navegación.
- **30 juegos 100 % Compose:** Póker, Caída, Furia, Dominó, Dados, Ajedrez, Damas, Serpientes, Mancala, Go, Jungle, Ludo, Backgammon, Xiangqi, Damas Chinas, Reversi, Ur, Brisca, Truco, Soroban, Shogi, Janggi, Aeroplane Chess, Mahjong americano, Riichi, Mahjong con naipes, Luzhanqi, Tren Mexicano, Chickenfoot y Ajedrez para tres.
- **Salas nativas:** Damas, Mancala, Dominó, Serpientes, Brisca, Truco, Ludo y Damas Chinas.
- **Replay automático** y **visor web** de Damas/Mancala.
- **Crónica PDF** en cada mesa.
- **Audio nativo** en las 30 mesas.
- **XP real** al terminar.
- **Accesibilidad:** cartas, fichas, casillas, puntos y tableros descritos.
- **Pendiente:** paridad web, accesibilidad completa, Baseline Profile, relay público y teléfono físico.

## Siguiente versión: 2.8.0

Ver [`v2.8.0.md`](./v2.8.0.md). Incluye:

1. Paridad web de los 7 juegos nuevos y catálogo web de 30.
2. Relay público con TLS, métricas y moderación.
3. Replays web de todos los juegos.
4. Accesibilidad completa, Baseline Profile y pruebas instrumentadas.
5. 11 idiomas y festividades completas en la cáscara nativa.
6. Teléfono físico y checklist de tienda.

## Dónde está cada cosa

| Ruta | Contenido |
|---|---|
| `src/games/` | 15 motores web |
| `src/ui/GameSurface.tsx` | Superficies web |
| `src/core/` | Tipos, registro, Match autoritativo, niveles |
| `src/net/room.ts` | Salas web, host/invitado y reconexión |
| `server/room-server.mjs` | Relay Node |
| `landing/replay/index.html` | Visor web de replays |
| `android/.../games/` | Motores Kotlin de los 30 juegos |
| `android/.../games/MahjongGame.kt` | Mahjong americano, Riichi y naipes |
| `android/.../games/LuzhanqiGame.kt` | Luzhanqi con niebla, vías y campamentos |
| `android/.../games/TrainGames.kt` | Tren Mexicano y Chickenfoot |
| `android/.../games/ThreeChessGame.kt` | Ajedrez para tres |
| `android/.../shell/MahjongScreen.kt` | Mesa de Mahjong |
| `android/.../shell/LuzhanqiScreen.kt` | Mesa de Luzhanqi |
| `android/.../shell/MexicanTrainScreen.kt` | Mesa de Tren Mexicano |
| `android/.../shell/ChickenfootScreen.kt` | Mesa de Chickenfoot |
| `android/.../shell/ThreeChessScreen.kt` | Mesa de Ajedrez para tres |
| `android/.../shell/ShellGames.kt` | Catálogo y mapa de pantallas/deep links |
| `android/.../shell/NativeRoom.kt` | Sesión host/invitado, asientos, protocolo y bots |
| `android/.../shell/RoomCodec.kt` | Códec JSON con redacción de manos |
| `android/.../shell/ReplayExport.kt` | Enlaces nativos/web, archivo e importación |
| `android/.../shell/NativeAudio.kt` | Música y efectos procedurales |
| `android/.../shell/NativeChronicle.kt` | Crónica PDF |
| `landing/` | Portada, versiones, descargas y replay |
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
cd android && gradlew.bat assembleDebug
cd android && gradlew.bat assembleRelease
cd android && gradlew.bat bundleRelease
```

## Seguridad

- El token de GitHub compartido en el chat debe revocarse.
- El keystore y la contraseña viven en `C:\Users\Sr.Hidalgo\.mesa-viva\`.
- `android/keystore.properties` está ignorado por git.
- El QR se genera localmente con ZXing.
- La sala nativa valida cada jugada en el anfitrión.
- Las manos privadas se redactan por asiento; el pozo solo viaja como conteo.
- El replay es autocontenido y no incluye datos personales.
- El relay online es opcional y debe ser de confianza.
- El diagnóstico se envía solo si el usuario abre el correo a `emanuel.industri@gmail.com`.
- El PDF se guarda en el dispositivo y solo se comparte si el usuario abre el selector.

## Reglas de continuidad

1. No borrar archivos de `versions/`.
2. Cada versión nueva crea su propio `vX.Y.Z.md`.
3. Mantener este `NEXT.md` actualizado, sin eliminarlo.
4. No guardar tokens, contraseñas ni keystores en el repositorio.
5. Antes de publicar: typecheck, tests web, tests Kotlin, build web, APK, AAB, CI/Pages/Android.
6. No afirmar “100 % Compose” global hasta retirar el WebView. **Estado v2.7.0: los 30 juegos jugables son Compose; el WebView solo queda como respaldo de la PWA y la sala remota.**
