# Relevo del proyecto · Mesa Viva

> **No borrar.** Este archivo existe para que cualquier chat, persona o IA sepa dónde quedó el proyecto. Se actualiza al cerrar cada versión, pero no se elimina.

## Estado actual

- **Versión publicada:** 0.8.0.
- **Juegos jugables:** 15.
- **Repositorio:** <https://github.com/emanuelindustri-source/mesa-viva>
- **Portada:** <https://emanuelindustri-source.github.io/mesa-viva/>
- **Juego web:** <https://emanuelindustri-source.github.io/mesa-viva/play/>
- **Release:** <https://github.com/emanuelindustri-source/mesa-viva/releases/tag/v0.8.0>
- **VERSION:** `0.8.0`
- **Android:** `versionCode=10`, `versionName=0.8.0`, compileSdk/targetSdk 36.

### Calidad

- 30/30 pruebas de motores (6 nuevas de Póker).
- TypeScript sin errores.
- Web compilada con Vite 6.4.5.
- APK debug y APK release firmado compilados.
- Emulador Android 14 (API 34): cáscara Compose, catálogo, Serpientes, WebView y PWA verificados con capturas.
- Teléfono real `23117RA68G`: **instalación bloqueada por MIUI** (`INSTALL_FAILED_USER_RESTRICTED`). Hay que activar “Instalar vía USB” en Opciones de desarrollador; el APK anterior se desinstaló durante la prueba.

### Checksums v0.8.0

- APK release firmado: `A46B1DAEBCEED2742882FC146E2398976F72679B4A4CEE9AD7E3C43BD4B7E955` · 9.544.193 bytes.
- APK debug: `E924C5AEA5CC3A9C0A21009970D4CB652ECE58AE18675282CB8B8CDBE36B3F8C` · 12.784.580 bytes.
- Artefactos locales: `artifacts/mesa-viva-0.8.0-release.apk` y `artifacts/mesa-viva-0.8.0-debug.apk`.

## Compose

- **Cáscara nativa completa:** inicio, catálogo, niveles, ajustes, navegación y back handling.
- **Serpientes y Escaleras 100 % Compose** con tablero 10×10, escaleras/serpientes dibujadas, dado animado, bots y vibración.
- **Mesa compartida:** `GameWebActivity` hospeda la PWA; conserva cámara, PDF, sensores, Material You y relay LAN.
- **Tema:** Material 3, acento de festividad y superficies dinámicas; `ShellSync` importa el perfil de `localStorage` al volver.
- **Pendiente:** Domó y Ajedrez nativos, lobby/QR nativo, sincronización bidireccional y retirada del WebView juego por juego.

## Siguiente versión: 0.9.0

Ver [`v0.9.0.md`](./v0.9.0.md). Incluye:

1. Domó y Ajedrez nativos en Compose.
2. Lobby/QR nativo.
3. Perfil y ajustes bidireccionales.
4. Prueba en teléfono real y sala LAN.
5. Accesibilidad, AAB y checklist de publicación.

## Dónde está cada cosa

| Ruta | Contenido |
|---|---|
| `src/games/` | 15 motores, incluido `poker.ts` |
| `src/ui/GameSurface.tsx` | Superficies de juego web, incluida `PokerSurface` |
| `src/core/` | Tipos, registro, Match autoritativo, niveles |
| `src/net/room.ts` | Salas, host/invitado y reconexión |
| `server/room-server.mjs` | Relay Node |
| `android/.../MainActivity.kt` | Cáscara Compose |
| `android/.../GameWebActivity.kt` | Mesa compartida WebView + puentes nativos |
| `android/.../shell/` | Estado, textos, tema, pantallas y Serpientes nativo |
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
cd android && gradlew.bat assembleRelease
```

## Seguridad

- El token de GitHub compartido en el chat debe revocarse.
- El keystore y la contraseña viven en `C:\Users\Sr.Hidalgo\.mesa-viva\`; no se copian al repositorio.
- `android/keystore.properties` está ignorado por git.
- El diagnóstico se envía solo si el usuario abre el correo a `emanuel.industri@gmail.com`.

## Reglas de continuidad

1. No borrar archivos de `versions/`.
2. Cada versión nueva crea su propio `vX.Y.Z.md`.
3. Mantener este `NEXT.md` actualizado, sin eliminarlo.
4. No guardar tokens, contraseñas ni keystores en el repositorio.
5. Antes de publicar: typecheck, tests, build web, APK, CI/Pages/Android.
6. No afirmar “100 % Compose” hasta retirar el WebView de los 15 juegos.
