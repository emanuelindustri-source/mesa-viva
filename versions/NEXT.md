# Relevo del proyecto · Mesa Viva

> **No borrar.** Este archivo existe para que cualquier chat, persona o IA sepa dónde quedó el proyecto. Se actualiza al cerrar cada versión, pero no se elimina.

## Estado actual

- **Versión publicada:** 1.0.0.
- **Juegos jugables:** 15.
- **Juegos 100 % Compose:** 6 (Serpientes y Escaleras, Dominó, Ajedrez, Ludo, Mancala y Backgammon).
- **Repositorio:** <https://github.com/emanuelindustri-source/mesa-viva>
- **Portada:** <https://emanuelindustri-source.github.io/mesa-viva/>
- **Juego web:** <https://emanuelindustri-source.github.io/mesa-viva/play/>
- **Release:** <https://github.com/emanuelindustri-source/mesa-viva/releases/tag/v1.0.0>
- **VERSION:** `1.0.0`
- **Android:** `versionCode=12`, `versionName=1.0.0`, compileSdk/targetSdk 36.

### Calidad

- 30/30 pruebas de motores web.
- 17/17 pruebas Kotlin (Dominó, Ajedrez, Ludo, Mancala y Backgammon).
- **47 pruebas en total.**
- TypeScript sin errores.
- Web compilada con Vite 6.4.5.
- APK debug, APK release firmado y AAB release firmado.
- Emulador Android 14: Home, Ludo, Mancala, Backgammon, Sala, QR y WebView verificados.
- Teléfono real `23117RA68G`: no estaba conectado. Sigue pendiente “Instalar vía USB” de MIUI.

### Checksums v1.0.0

- APK release firmado: `33E57917714D7B9EEBA3D480ECA3DC00C25CEED9E618162554C58D0477EF8B5D` · 9.970.453 bytes.
- APK debug (CI): `10CF5856C62933504DAE1B5A3E67EF4301CBC13CCFC07CD0F56D49EAD007DE1D` · 13.157.858 bytes.
- AAB release firmado: `9EF6ADEA05D066EC2BDCE2164C77F5D44E3610B09510A65EC9170616B8D17E49` · 9.609.022 bytes.
- Artefactos locales en `artifacts/`: `mesa-viva-1.0.0-release.apk`, `mesa-viva-1.0.0-debug.apk`, `mesa-viva-1.0.0-release.aab`.

## Compose

- **Cáscara nativa completa:** inicio, catálogo, niveles, ajustes, sala y navegación.
- **Seis juegos 100 % Compose:** Serpientes, Dominó, Ajedrez, Ludo, Mancala y Backgammon.
- **Sala nativa:** crear/unirse, QR local ZXing, escaneo y selector de ronda.
- **Sincronización:** la cáscara pasa idioma/música/efectos/vibración/movimiento a la web; `ShellSync` devuelve el perfil.
- **Pendiente:** salas nativas, 9 juegos restantes en WebView y retirada del WebView.

## Siguiente versión: 1.1.0

Ver [`v1.1.0.md`](./v1.1.0.md). Incluye:

1. Salas nativas para los seis juegos Compose.
2. Caída, Furia, Dados, Damas, Go, Jungle, Xiangqi, Damas Chinas y Póker nativos.
3. Prueba en teléfono físico, LAN y AAB.
4. Accesibilidad, AAB y checklist de tienda.
5. Retirada progresiva del WebView.

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
| `android/.../games/` | Motores Kotlin: Dominó, Ajedrez, Ludo, Mancala y Backgammon |
| `android/.../shell/` | Estado, textos, tema, pantallas, sala, QR y los seis juegos |
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
