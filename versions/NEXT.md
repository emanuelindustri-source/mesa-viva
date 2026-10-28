# Relevo del proyecto · Mesa Viva

> **No borrar.** Este archivo existe para que cualquier chat, persona o IA sepa dónde quedó el proyecto. Se actualiza al cerrar cada versión, pero no se elimina.

## Estado actual

- **Versión publicada:** 0.9.0.
- **Juegos jugables:** 15.
- **Juegos 100 % Compose:** 3 (Serpientes y Escaleras, Dominó, Ajedrez).
- **Repositorio:** <https://github.com/emanuelindustri-source/mesa-viva>
- **Portada:** <https://emanuelindustri-source.github.io/mesa-viva/>
- **Juego web:** <https://emanuelindustri-source.github.io/mesa-viva/play/>
- **Release:** <https://github.com/emanuelindustri-source/mesa-viva/releases/tag/v0.9.0>
- **VERSION:** `0.9.0`
- **Android:** `versionCode=11`, `versionName=0.9.0`, compileSdk/targetSdk 36.

### Calidad

- 30/30 pruebas de motores web.
- 9/9 pruebas Kotlin de Dominó y Ajedrez.
- **39 pruebas en total.**
- TypeScript sin errores.
- Web compilada con Vite 6.4.5.
- APK debug y APK release firmado compilados.
- Emulador Android 14: Home, Dominó, Ajedrez, Sala, QR y WebView en español verificados con capturas.
- Teléfono real `23117RA68G`: no estaba conectado en esta sesión. Sigue pendiente “Instalar vía USB” de MIUI.

### Checksums v0.9.0

- APK release firmado: `338A0AD2F53A43E8447E832A7EDD44A0B68EE9E651338A0D424591E0313348DE` · 9.872.145 bytes.
- APK debug: lo publica el workflow de CI; consultar el release. El build local previo fue `F31890B69BE3BC203CFC5D9D592F73F061C7438C0722D1C3150EF0BF83B12DAC`.
- Artefactos locales: `artifacts/mesa-viva-0.9.0-release.apk` y `artifacts/mesa-viva-0.9.0-debug.apk`.

## Compose

- **Cáscara nativa completa:** inicio, catálogo, niveles, ajustes, sala y navegación.
- **Serpientes y Escaleras 100 % Compose.**
- **Dominó 100 % Compose:** doble 6, parejas, pozo, tranca, bots y meta.
- **Ajedrez 100 % Compose:** reglas completas, promoción elegible, historial SAN y bot alfa-beta.
- **Sala nativa:** crear/unirse, QR local ZXing, escaneo y selector de ronda.
- **Sincronización:** la cáscara pasa idioma/música/efectos/vibración/movimiento a la web; `ShellSync` devuelve el perfil.
- **Pendiente:** salas nativas, 12 juegos restantes en WebView y retirada del WebView.

## Siguiente versión: 1.0.0

Ver [`v1.0.0.md`](./v1.0.0.md). Incluye:

1. Salas nativas para Dominó y Ajedrez.
2. Ludo, Mancala y Backgammon nativos.
3. Prueba en teléfono físico y LAN.
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
| `android/.../games/` | Motores Kotlin de Dominó y Ajedrez |
| `android/.../shell/` | Estado, textos, tema, pantallas, sala y QR |
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
5. Antes de publicar: typecheck, tests web, tests Kotlin, build web, APK, CI/Pages/Android.
6. No afirmar “100 % Compose” hasta retirar el WebView de los 15 juegos.
