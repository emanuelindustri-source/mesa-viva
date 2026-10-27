# Despliegue

## Web en GitHub Pages

El workflow `.github/workflows/pages.yml` publica `dist/` en cada push a `main`.

1. Sube el repositorio a GitHub.
2. En **Settings → Pages**, elige **GitHub Actions** como fuente.
3. Opcional: en **Settings → Secrets and variables → Actions → Variables**, crea `VITE_ROOM_WS_URL` con la URL `wss://` del relay. Sin ella, la web compila igual, pero las salas en línea piden configurar el servidor en Ajustes.
4. Haz push a `main` o lanza el workflow manualmente.

La URL esperada es `https://TU-USUARIO.github.io/mesa-viva/`.

## Relay de salas

El relay es un servicio Node con `ws`; no guarda partidas. El host mantiene el estado y el relay solo enruta.

### Render (rápido)

1. Crea un **Blueprint** desde el repo; `render.yaml` ya define `mesa-viva-room`.
2. Ajusta `ALLOWED_ORIGINS` a tu URL de Pages (coma-separada si hay varias).
3. Cuando esté desplegado, copia la URL pública y cámbiala a `wss://`.
4. Configura esa URL como variable del repo para el build web y/o escríbela en **Ajustes → Servidor de salas** dentro de la app.

### VPS / Docker / Fly / Railway

```bash
npm install
PORT=9001 ALLOWED_ORIGINS=https://tu-sitio.github.io node server/room-server.mjs
```

Pon el proceso detrás de un proxy TLS (Caddy, nginx, Cloudflare) para obtener `wss://`. Abre el puerto o usa el proxy.

Variables:

- `PORT`: puerto HTTP/WS (por defecto 9001).
- `ALLOWED_ORIGINS`: orígenes permitidos separados por coma. Vacío = todos (solo desarrollo).
- `MAX_ROOMS`: máximo de salas simultáneas (500 por defecto).

### Red local / APK

El relay puede correr en una computadora de la misma red:

```bash
npm run rooms
```

En **Ajustes → Servidor de salas** del APK, escribe `ws://IP-DE-LA-COMPUTADORA:9001`. Todos los jugadores deben usar el mismo relay. Permite el puerto en el firewall. Esto es una sala LAN con servidor, no la sala LAN sin infraestructura que está en el roadmap.

## Android

### Depuración

```bash
npm run build
npm run android:sync
cd android
gradlew.bat assembleDebug
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

El workflow `android.yml` compila en tags `v*` y publica el APK como artefacto/release. También se puede lanzar manualmente.

### Release firmado

1. Genera un keystore **fuera del repositorio**:
   ```bash
   keytool -genkeypair -v -keystore mesa-viva.jks -alias mesa-viva -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Crea `android/keystore.properties` (ignorado por git):
   ```properties
   storeFile=C:\\ruta\\segura\\mesa-viva.jks
   storePassword=...
   keyAlias=mesa-viva
   keyPassword=...
   ```
3. Configura `signingConfigs` en `android/app/build.gradle.kts` y ejecuta `assembleRelease`.
4. Guarda el keystore y contraseñas en un gestor de secretos. Perder la clave impide actualizar la app en tiendas.
5. Sube el `.aab` a Play Console si vas a publicar; el APK debug es solo para pruebas.

## Diagnóstico

- Si la web no carga, revisa que `base: './'` siga funcionando bajo el subpath de Pages.
- Si las salas no conectan, revisa la URL `wss://`, `ALLOWED_ORIGINS`, el firewall y la consola del relay.
- Si el APK no carga, revisa que `android/app/src/main/assets/www/index.html` exista después de `android:sync`.
- Si el PDF no se guarda en Android, revisa el permiso de almacenamiento/MediaStore y el puente `MesaViva.savePdf`.

## Checklist previo a publicar

- [ ] `npm run typecheck`, `npm test`, `npm run build`.
- [ ] `python tools/check_i18n.py` sin claves faltantes.
- [ ] Relay con TLS y `ALLOWED_ORIGINS`.
- [ ] APK firmado y probado en un dispositivo real.
- [ ] Política de privacidad y términos accesibles.
- [ ] Token de GitHub rotado y sin secretos en el repo.
