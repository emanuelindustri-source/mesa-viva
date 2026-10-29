# Mesa Viva · La fiesta se juega en tu mesa

**Mesa Viva** es una aplicación de juegos de cartas y mesa con alma venezolana. Funciona sin conexión para jugar solo, contra bots o pasando el teléfono; y tiene salas en línea con código y QR para jugar con amigos. La web y el APK Android comparten el mismo motor de juego.

> Estado actual: **v1.2.0**. Quince juegos jugables, Póker Texas Hold'em sin dinero real y **catorce juegos 100 % Jetpack Compose**: Serpientes y Escaleras, Dominó, Ajedrez, Ludo, Mancala, Backgammon, Damas, Dados Mentirosos, Go, Jungle, Xiangqi, Damas Chinas, **Caída** y **Furia de Colores**. La cáscara nativa suma **Sala de juegos con QR** y sincroniza idioma y preferencias con la mesa web. Android 16 (API 36), APK/AAB firmados y PWA offline.

---

## Lo que ya puedes hacer

- **Póker de la casa**: Texas Hold'em 2–6 jugadores, ciegas 10/20, cuatro calles, subidas con mínimo, all-in, botes laterales, devolución de apuestas no igualadas, evaluación de la mejor mano de 5 entre 7 y bots con tres dificultades. Solo fichas de práctica: sin dinero real.
- **Dominó nativo (Compose)**: doble 6, 2–4 jugadores, parejas, pozo, tranca, cierre de mano, meta configurable y bots. Fichas dibujadas con pips, mesa de fieltro, selección de extremo y vibración.
- **Ajedrez nativo (Compose)**: reglas completas (enroque, al paso, promoción elegible, jaque, mate, ahogado, 50 movimientos, triple repetición y material insuficiente), historial SAN, tres dificultades y bot alfa-beta.
- **Ludo nativo (Compose)**: 2–4 jugadores, cuatro fichas, salida con 6, capturas, casillas seguras, tres seises, turno extra y meta exacta. Tablero 15×15, dados, bots y modo pasar el teléfono.
- **Mancala nativo (Compose)**: siembra, captura, turno extra, almacenes, bots y modo local.
- **Backgammon nativo (Compose)**: 15 fichas, barra, capturas, dobles, bear-off, pips y bots.
- **Sala nativa (Compose)**: crear sala, unirse por código, escanear QR, QR local con ZXing y selector de ronda de juegos. La mesa y el relay siguen en la capa web/Android.
- **Damas nativa (Compose)**: 8×8, 12 fichas por lado, captura obligatoria, cadenas, coronación, empate por 80 jugadas y bot alfa-beta.
- **Dados Mentirosos nativo (Compose)**: 2–6 jugadores, cinco dados, pujas, “Dudo”, revelación, eliminación y privacidad al pasar el teléfono.
- **Go nativo (Compose)**: 9×9 y 13×13, capturas, ko, komi 6.5, conteo de territorio y bot heurístico.
- **Jungle nativo (Compose)**: río, trampas, guaridas, rata/elefante y saltos de tigre y león.
- **Xiangqi nativo (Compose)**: palacio, río, caballo bloqueado, cañón, soldados, generales enfrentados y bot alfa-beta.
- **Damas Chinas nativo (Compose)**: estrella de 121 hoyos para 2–6 jugadores, saltos encadenados y meta opuesta.
- **Caída nativa (Compose)**: baraja española de 40 cartas dibujada, entrega de mesa, cantos (ronda, trivilín, patrulla, vigía, registro, registrico, casa chica y casa grande), caídas, mesa limpia, parejas, meta 40/70/100 y bots.
- **Furia de Colores nativa (Compose)**: homenaje original a los juegos de combinar colores, con 108 naipes, comodines, rebote, bloqueo, roba dos/cuatro y grito de “¡Última!”.
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
- **Damas Chinas** con estrella de 121 hoyos, saltos encadenados y meta en el triángulo opuesto.
- **Modos**: solo contra bots, pasar el teléfono (humanos y bots mezclados), sala en línea con rondas y chat, **sala LAN sin internet** en Android y **reconexión automática** del invitado si se cae la conexión.
- **Progresión**: 10 niveles, XP por partida, victoria, dificultad, puntos y racha; recompensas desbloqueables.
- **Crónica de partida**: últimos 5 movimientos visibles e historial completo en las mesas nativas; PDF con infografía en la mesa web.
- **Temas**: Halloween criollo (El Silbón, La Sayona, El Hachero), Navidad, Reyes, Carnaval, San Valentín, San Patricio, Semana Santa, Cruz de Mayo, Corpus Christi, independencia por país y mesa neutral. El acento puede venir de la temporada, del sistema (Material You en Android) o del color dominante del fondo de pantalla.
- **Idiomas**: español, inglés, portugués de Brasil, portugués de Portugal, alemán, ruso, italiano, francés, chino tradicional, chino simplificado y japonés.
- **Android**: cáscara nativa en **Jetpack Compose** (Material 3, color dinámico/festividad, inicio, catálogo, niveles, ajustes, sala y navegación), **catorce juegos nativos** (Serpientes, Dominó, Ajedrez, Ludo, Mancala, Backgammon, Damas, Dados, Go, Jungle, Xiangqi, Damas Chinas, Caída y Furia), y mesa compartida en `GameWebActivity` con `WebViewAssetLoader`. Solo Póker sigue en la capa web. El idioma y las preferencias de la cáscara viajan a la web; el perfil vuelve con `ShellSync`. Target Android 16 (API 36), edge-to-edge con insets, back predictivo, APK de release firmado, vibración, sensores para agitar/“lanzar” el teléfono, cámara para QR y guardado de PDF en Descargas.
- **Privacidad**: sin cuentas, sin anuncios, sin rastreo. Las partidas locales se quedan en el dispositivo. El diagnóstico se envía solo si tú abres el correo.
- **PWA**: instalable, service worker y funcionamiento offline.

## Catálogo y roadmap

El catálogo dentro de la app muestra la visión completa de **30 juegos** con su estado y enlaces de reglas. La primera ola jugable son los **15** anteriores, con Póker ya integrado. Shogi, Janggi, Luzhanqi, Ur, Tren Mexicano, Chickenfoot, Aeroplane Chess, Mahjong, Riichi, Mahjong con naipes, Ajedrez para tres y Soroban están marcados como próximos o en investigación. No se usan marcas, arte ni reglamentos propietarios de terceros: los nombres comerciales del catálogo son referencias culturales.

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

- 30 pruebas de motores web + 48 pruebas de motores nativos Kotlin (Dominó, Ajedrez, Ludo, Mancala, Backgammon, Damas, Dados, Go, Jungle, Xiangqi, Damas Chinas, Caída y Furia). **78 en total**.
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
src/games/       motores de los 15 juegos
src/ui/          pantallas, mesa, cartas, dados, tablero, resultados
landing/         portada estática de presentación, versiones y descargas
src/net/         protocolo de sala y relay WebSocket
src/theme/       festividades, Semana Santa/Carnaval, paletas y Material You
src/i18n/        catálogos y formateo de crónicas
src/audio/       música generativa y SFX sintetizados
src/services/    diagnóstico, sensores, háptica, PDF, QR, PWA
android/         app Android (Compose nativo + 14 juegos Kotlin + GameWebActivity + relay LAN)
server/          relay de salas (Node + ws)
docs/            arquitectura, roadmap, privacidad y seguridad
```

## Licencia

Código bajo licencia **MIT** (ver `LICENSE`). El arte, los textos, la música y los personajes originales de Mesa Viva se publican bajo **CC BY-SA 4.0** salvo que se indique lo contrario. Las marcas y juegos comerciales mencionados pertenecen a sus titulares y se usan solo como referencia cultural; no se incluye arte ni reglamento propietario.

## Evidencia por versión

Cada release tiene un archivo inmutable en [`versions/`](./versions/README.md) con alcance, pruebas, enlaces, checksums y problemas conocidos. No se borran las versiones anteriores.

Hecho con cariño para Venezuela y el mundo.
