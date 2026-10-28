# Mesa Viva · La fiesta se juega en tu mesa

**Mesa Viva** es una aplicación de juegos de cartas y mesa con alma venezolana. Funciona sin conexión para jugar solo, contra bots o pasando el teléfono; y tiene salas en línea con código y QR para jugar con amigos. La web y el APK Android comparten el mismo motor de juego.

> Estado actual: **v0.6.0**. Trece juegos jugables, Compose fase 0 en Android, reconexión automática de invitados, sala LAN sin internet por QR, APK firmado y Android 16 (API 36), 11 idiomas, temas de festividad, 10 niveles y crónicas PDF.

---

## Lo que ya puedes hacer

- **Caída** venezolana: entrega de mesa, cantos (ronda, trivilín, patrulla, vigía, registro, registrico, casa chica y casa grande), caídas, mesa limpia, parejas y meta de 24 puntos.
- **Furia de Colores**: homenaje original a los juegos de combinar colores, con comodines, rebote, bloqueo y grito de “¡Última!”.
- **Dominó** doble 6 y doble 12, parejas, pozo, tranca y meta configurable.
- **Dados Mentirosos**: cubiletes, pujas, “Dudo”, revelación y eliminación.
- **Ajedrez** completo (enroque, al paso, promoción, jaque mate, ahogado, 50 movimientos, repetición, material insuficiente) con IA alfa-beta.
- **Damas** con captura obligatoria, cadenas y coronación.
- **Serpientes y Escaleras** y **Mancala** con bots.
- **Go** 9×9 y 13×13 con capturas, ko, komi 6.5 y conteo de territorio.
- **Jungle / Dou Shou Qi** con río, trampas, saltos de tigre y león y guaridas.
- **Ludo** 2–4 jugadores con cuatro fichas, capturas, casillas seguras, dado y meta exacta.
- **Backgammon** con barra, capturas, dobles, bear-off y 15 fichas por bando.
- **Xiangqi** con palacio, río, caballo bloqueado, cañón, soldados y generales enfrentados.
- **Modos**: solo contra bots, pasar el teléfono (humanos y bots mezclados), sala en línea con rondas y chat, **sala LAN sin internet** en Android y **reconexión automática** del invitado si se cae la conexión.
- **Progresión**: 10 niveles, XP por partida, victoria, dificultad, puntos y racha; recompensas desbloqueables.
- **Crónica de partida**: últimos 5 movimientos visibles, historial completo, descarga PDF con infografía.
- **Temas**: Halloween criollo (El Silbón, La Sayona, El Hachero), Navidad, Reyes, Carnaval, San Valentín, San Patricio, Semana Santa, Cruz de Mayo, Corpus Christi, independencia por país y mesa neutral. El acento puede venir de la temporada, del sistema (Material You en Android) o del color dominante del fondo de pantalla.
- **Idiomas**: español, inglés, portugués de Brasil, portugués de Portugal, alemán, ruso, italiano, francés, chino tradicional, chino simplificado y japonés.
- **Android**: APK Kotlin nativo con `WebViewAssetLoader`, target Android 16 (API 36), edge-to-edge con insets, back predictivo, APK de release firmado, vibración, sensores para agitar/“lanzar” el teléfono, color Material You, cámara para QR y guardado de PDF en Descargas.
- **Privacidad**: sin cuentas, sin anuncios, sin rastreo. Las partidas locales se quedan en el dispositivo. El diagnóstico se envía solo si tú abres el correo.
- **PWA**: instalable, service worker y funcionamiento offline.

## Catálogo y roadmap

El catálogo dentro de la app muestra la visión completa de **30 juegos** con su estado y enlaces de reglas. La primera ola jugable son los 13 anteriores. Damas Chinas, Shogi, Janggi, Luzhanqi, Ur, Tren Mexicano, Chickenfoot, Aeroplane Chess, Póker, Mahjong, Riichi, Mahjong con naipes, Ajedrez para tres y Soroban están marcados como próximos o en investigación. No se usan marcas, arte ni reglamentos propietarios de terceros: los nombres comerciales del catálogo son referencias culturales.

## Arranque rápido (web)

```bash
npm install
npm run dev
```

Abre `http://localhost:5173`.

Para compilar la web:

```bash
npm run build
npm run preview
```

## Sitio público

- **Portada / presentación:** <https://emanuelindustri-source.github.io/mesa-viva/>
- **Juego / PWA:** <https://emanuelindustri-source.github.io/mesa-viva/play/>
- **Versiones y APK:** <https://github.com/emanuelindustri-source/mesa-viva/releases>

La portada es HTML estático en `landing/`. El juego se compila en `dist/` y el sitio completo de Pages se arma con:

```bash
npm run build:pages
python -m http.server 4180 --directory dist-site
```

El APK de release firmado se genera con `cd android && gradlew.bat assembleRelease`; requiere `android/keystore.properties` y el keystore fuera del repositorio.

## Sala en línea

Las salas usan un **relay WebSocket** incluido en `server/room-server.mjs`. El host mantiene el estado autoritativo de la partida; el relay solo enruta mensajes entre el host y los invitados y no guarda partidas.

Desarrollo local:

```bash
npm run rooms      # relay en ws://localhost:9001
npm run dev        # web en http://localhost:5173
```

`.env.local` apunta a `ws://localhost:9001`. En la app, **Ajustes → Servidor de salas** permite cambiar la URL en tiempo de ejecución (útil para el APK y para una red local). Si usas otro dispositivo, pon la IP de la computadora que ejecuta el relay, por ejemplo `ws://192.168.1.20:9001`, y permite el puerto en el firewall.

Producción: despliega el relay en Render/Fly/Railway/VPS con TLS (`wss://`) y configura `VITE_ROOM_WS_URL` al compilar o la URL desde Ajustes. Incluye `render.yaml` como punto de partida. El servidor respeta `PORT`, `ALLOWED_ORIGINS`, `MAX_ROOMS` y caduca salas inactivas. Para una experiencia pública, añade límites de uso y supervisión.

> La sala en línea necesita el relay. Los modos locales (bots y pasar el teléfono) funcionan completamente sin internet. En **Android**, el anfitrión puede crear una **sala LAN sin internet**: la app levanta un relay WebSocket en su propio teléfono, muestra un QR con `ws://IP-LOCAL:9001` y el invitado se une desde la misma WiFi u hotspot. En la web/PWA la sala LAN requiere desplegar el relay Node o usar la URL de un equipo de la red.

### Sala LAN sin internet (Android)

1. Conecta el teléfono anfitrión a una WiFi o activa un hotspot.
2. **Sala → Sin internet → Crear sala sin internet**.
3. Comparte el QR o el código. El QR incluye `relay=ws://…` para que el invitado configure el servidor automáticamente.
4. El invitado abre **Sala → Sin internet → Escanear QR LAN**, escribe su nombre y entra.
5. El anfitrión debe mantener la app abierta mientras dure la partida. Si la red bloquea puertos, revisa el firewall del hotspot o usa otra WiFi local.

## Android

Requisitos: JDK 17, Android SDK API 36, Gradle wrapper incluido.

**[Descargar el APK de la última versión](https://github.com/emanuelindustri-source/mesa-viva/releases/latest)** · APK de release firmado y APK de depuración instalable (sideload). Para salas en línea, configura el relay en **Ajustes → Servidor de salas**.

```bash
npm run build
npm run android:sync
cd android
gradlew.bat assembleDebug
```

APK de salida: `android/app/build/outputs/apk/debug/app-debug.apk`.

Para una compilación limpia desde la raíz:

```bash
npm run android:build
```

El APK carga los assets locales con `WebViewAssetLoader` sobre un origen HTTPS seguro (`appassets.androidplatform.net`), lo que permite cámara, almacenamiento y audio. Incluye puentes nativos para vibración, sensores, color del sistema, guardado de PDF y el relay LAN (`startLanServer`/`stopLanServer`). La URL del relay se puede cambiar en Ajustes; para el emulador suele ser `ws://10.0.2.2:9001` y para un teléfono en la misma red, `ws://IP-DEL-PC:9001`.

**Firma de release**: el repo no incluye keystore. El APK firmado se genera localmente con un keystore guardado fuera del repositorio y `android/keystore.properties` (ignorado por git); ese archivo y el `.jks` deben respaldarse en un lugar privado. El workflow de CI produce un APK de depuración instalable. El APK no incluye librerías nativas `.so`, así que no tiene el problema de alineación de 16 KB.

## Pruebas y calidad

```bash
npm run typecheck
npm run test
npm run test:relay            # requiere npm run rooms en otra terminal
python tools/check_i18n.py   # claves usadas vs. catálogo español
```

- TypeScript estricto, sin `any` en la capa de motores.
- Motor determinista con semilla reproducible.
- Host autoritativo en red; los clientes solo reciben su vista redactada.
- Validación de acciones en el motor; los bots no pueden saltarse turnos.
- Nombres y chat saneados; límite de tamaño de mensajes; rate limit en el relay.
- CSP en producción, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy` en hosts compatibles.
- Diagnóstico local con bucle de errores y envío por correo a `emanuel.industri@gmail.com`.

## Estructura

```
src/core/        tipos, azar, niveles, registro, Match autoritativo
src/games/       motores de los 13 juegos
src/ui/          pantallas, mesa, cartas, dados, tablero, resultados
landing/         portada estática de presentación, versiones y descargas
src/net/         protocolo de sala y relay WebSocket
src/theme/       festividades, Semana Santa/Carnaval, paletas y Material You
src/i18n/        catálogos y formateo de crónicas
src/audio/       música generativa y SFX sintetizados
src/services/    diagnóstico, sensores, háptica, PDF, QR, PWA
android/         app Android nativa (Kotlin, WebViewAssetLoader)
server/          relay de salas (Node + ws)
docs/            arquitectura, roadmap, privacidad y seguridad
```

## Licencia

Código bajo licencia **MIT** (ver `LICENSE`). El arte, los textos, la música y los personajes originales de Mesa Viva se publican bajo **CC BY-SA 4.0** salvo que se indique lo contrario. Las marcas y juegos comerciales mencionados pertenecen a sus titulares y se usan solo como referencia cultural; no se incluye arte ni reglamento propietario.

## Evidencia por versión

Cada release tiene un archivo inmutable en [`versions/`](./versions/README.md) con alcance, pruebas, enlaces, checksums y problemas conocidos. No se borran las versiones anteriores.

Hecho con cariño para Venezuela y el mundo.
