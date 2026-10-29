# Relevo del proyecto · Mesa Viva

> **No borrar.** Este archivo existe para que cualquier chat, persona o IA sepa dónde quedó el proyecto. Se actualiza al cerrar cada versión, pero no se elimina.

## Estado actual

- **Versión publicada:** 1.4.0.
- **Juegos jugables:** 15.
- **Juegos 100 % Compose:** 15 (todos).
- **Juegos en WebView:** 0 (el WebView queda como respaldo de la PWA y de la sala online).
- **Crónica PDF nativa:** 15 mesas, con guardado en `Descargas/Mesa Viva` y compartición.
- **Repositorio:** <https://github.com/emanuelindustri-source/mesa-viva>
- **Portada:** <https://emanuelindustri-source.github.io/mesa-viva/>
- **Juego web:** <https://emanuelindustri-source.github.io/mesa-viva/play/>
- **Release:** <https://github.com/emanuelindustri-source/mesa-viva/releases/tag/v1.4.0>
- **VERSION:** `1.4.0`
- **Android:** `versionCode=16`, `versionName=1.4.0`, compileSdk/targetSdk 36.

### Calidad

- 30/30 pruebas de motores web.
- 56/56 pruebas Kotlin (los quince juegos).
- **86 pruebas en total.**
- TypeScript sin errores.
- Web compilada con Vite 6.4.5 y sincronizada dentro del APK.
- APK debug, APK release firmado y AAB release firmado.
- Emulador Android 14: inicio, Póker, Caída, Furia, Damas, Go, Jungle, Xiangqi, Damas Chinas, Dados y crónica PDF verificados.
- PDF generado en `Descargas/Mesa Viva`, abierto y compartido con Android.
- Teléfono real `23117RA68G`: detectado por `adb`, pero MIUI bloqueó la instalación por USB (“Instalación bloqueada de Mesa Viva vía USB”). Pendiente habilitar **Instalar vía USB** en Opciones de desarrollador.

### Checksums v1.4.0

- APK release firmado: `03B9877EC621011EB349CA8343A61F1CC1CEF243E196F583BE7AB8C4D8ED0A34` · 10.347.777 bytes.
- AAB release firmado: `82BF157B48A970C6A7401A6988443DEB70E7D57DBDB368CBE69A0E75526E5036` · 9.982.625 bytes.
- Artefactos locales en `artifacts/`: `mesa-viva-1.4.0-release.apk`, `mesa-viva-1.4.0-release.aab`. La release de GitHub añade `mesa-viva-1.4.0-debug.apk`.

## Compose

- **Cáscara nativa completa:** inicio, catálogo, niveles, ajustes, sala y navegación.
- **Quince juegos 100 % Compose.**
- **Crónica PDF** en cada mesa: título, fecha, movimientos, paginación y compartición.
- **Accesibilidad básica:** cartas descritas, cartas boca abajo y banner de turno como región viva.
- **Sala nativa:** crear/unirse, QR local ZXing, escaneo y selector de ronda.
- **Sincronización:** la cáscara pasa idioma/música/efectos/vibración/movimiento a la web; `ShellSync` devuelve el perfil.
- **Pendiente:** salas nativas, replays, audio nativo y accesibilidad completa.

## Siguiente versión: 1.5.0

Ver [`v1.5.0.md`](./v1.5.0.md). Incluye:

1. Salas nativas host/invitado y reconexión.
2. Replays con semilla y exportación.
3. Accesibilidad completa y audio nativo por juego.
4. Teléfono físico, LAN real y checklist de tienda.
5. Gradle 8.14.4+ y perfil de rendimiento.
6. Retirada del WebView de respaldo al desacoplar la sala online y la PWA.

## Dónde está cada cosa

| Ruta | Contenido |
|---|---|
| `src/games/` | 15 motores web |
| `src/ui/GameSurface.tsx` | Superficies web |
| `src/core/` | Tipos, registro, Match autoritativo, niveles |
| `src/net/room.ts` | Salas, host/invitado y reconexión |
| `server/room-server.mjs` | Relay Node |
| `android/.../MainActivity.kt` | Cáscara Compose |
| `android/.../GameWebActivity.kt` | WebView de respaldo + puentes + params nativos |
| `android/.../games/` | Motores Kotlin de los quince juegos |
| `android/.../shell/` | Estado, textos, tema, pantallas, sala, QR, quince mesas |
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
- El QR se genera localmente con ZXing; no se envía a un servicio.
- El diagnóstico se envía solo si el usuario abre el correo a `emanuel.industri@gmail.com`.
- El PDF se guarda en el dispositivo y solo se comparte si el usuario abre el selector de Android.

## Reglas de continuidad

1. No borrar archivos de `versions/`.
2. Cada versión nueva crea su propio `vX.Y.Z.md`.
3. Mantener este `NEXT.md` actualizado, sin eliminarlo.
4. No guardar tokens, contraseñas ni keystores en el repositorio.
5. Antes de publicar: typecheck, tests web, tests Kotlin, build web, APK, AAB, CI/Pages/Android.
6. No afirmar “100 % Compose” global hasta retirar el WebView de los 15 juegos. **Estado v1.4.0: los quince juegos jugables ya son Compose; el WebView solo queda como respaldo.**
