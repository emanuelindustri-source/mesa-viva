# Changelog

## 2.7.1 · 2026-10-30

Ajustes de mesa.

### Corregido

- Mahjong conserva las reclamaciones de los bots cuando el humano pasa.
- Los resultados de mano y ronda se pueden reabrir si se cierra el diálogo.
- Se corrigieron los textos de las manos de Chickenfoot y Tren Mexicano.
- Se añadió la ruta de QA `--es screen <id>` para abrir mesas directamente.

### Pruebas

- **146 pruebas Kotlin + 30 web = 176 verdes.**
- APK debug, APK release firmado y AAB release firmado.
- `versionCode=30`, `versionName=2.7.1`.

## 2.7.0 · 2026-10-30

La mesa completa.

### Añadido

- **Mahjong americano, Riichi y Mahjong con naipes** nativos en Compose, con cuatro manos, muro, flores, comodines, descartes, pon, chi, kan, ron, tsumo, riichi, furiten, yaku y puntuación por mano.
- **Luzhanqi** nativo: tablero 5×12, 25 piezas por bando, piezas ocultas, vías, campamentos, ingenieros, minas, bombas y captura de bandera.
- **Tren Mexicano** nativo: dominó doble 12, 2–8 jugadores, tres rondas, tren propio, tren público, trenes abiertos y bloqueos.
- **Chickenfoot** nativo: dominó doble 9, 2–8 jugadores, tres rondas y la pata de tres ramas.
- **Ajedrez para tres** nativo: tablero 12×12, tres ejércitos de 16 piezas, promociones, jaque, eliminación y bots.
- Historial, PDF, audio, vibración y XP en las mesas nuevas.
- Textos en español e inglés para las 7 mesas nuevas.

### Cambiado

- El catálogo pasa de **23 a 30 juegos jugables de 30**.
- Android: `versionCode=29`, `versionName=2.7.0`.
- El inicio, el catálogo, los ajustes y la portada anuncian la 2.7.0.
- Mahjong cierra correctamente la partida al terminar la cuarta mano.
- La mesa de Luzhanqi permite entrar en campamentos vacíos y protege a su ocupante.

### Pruebas

- **146 pruebas Kotlin** verdes y **30 web** verdes: **176 en total**.
- TypeScript sin errores.
- Web compilada con Vite 6.4.5.
- APK debug, APK release firmado y AAB release firmado.
- Verificación visual en emulador Android 14.

### Pendiente

- Paridad web de los 7 juegos nuevos (la web conserva 15).
- Relay público con TLS, replays web de todos los juegos y accesibilidad completa.
- Baseline Profile y prueba en teléfono físico.
- Los 11 idiomas y las festividades completas en la cáscara nativa.

## 2.6.0 · 2026-10-30

Cielo de colores.

### Añadido

- **Aeroplane Chess nativo** de 2–4 jugadores con cuatro aviones por color.
- Saltos de cuatro casillas, capturas, turnos extra y meta exacta.
- Bots, modo pasar el teléfono, historial, PDF y XP.
- 1 prueba nueva del salto de Aeroplane.
- El catálogo llega a **23 juegos jugables de 30**.

### Cambiado

- Android: `versionCode=28`, `versionName=2.6.0`.
- El inicio, catálogo y ajustes muestran 23 juegos y la versión 2.6.0.
- Botonera de Ludo/Aeroplane con cuatro botones de igual ancho.

### Pendiente

- Mahjong, Luzhanqi y los 5 juegos restantes.
- Relay público, replays web y accesibilidad completa.
- Teléfono físico.

## 2.5.0 · 2026-10-30

Palacio y río.

### Añadido

- **Janggi nativo**: ajedrez coreano 9×10 con palacio, río y generales.
- Cañón con pantalla, caballo y elefante bloqueados, soldados laterales y generales enfrentados.
- Jaque, jugadas legales, mate y bots easy/normal/hard.
- Modo pasar el teléfono, historial, PDF y XP.
- 2 pruebas nuevas de Janggi.
- El catálogo llega a **22 juegos jugables de 30**.

### Cambiado

- Android: `versionCode=27`, `versionName=2.5.0`.
- El inicio, catálogo y ajustes muestran 22 juegos y la versión 2.5.0.

### Pendiente

- Mahjong, Luzhanqi y los 6 juegos restantes.
- Relay público, replays web y accesibilidad completa.
- Teléfono físico.

## 2.4.0 · 2026-10-30

Juegos del mundo.

### Añadido

- **Shogi nativo**: tablero 9×9, ocho piezas japonesas y promociones.
- Capturas que pasan a la mano, drops y restricciones de peón/lanza/caballo/nifu.
- Jaque, jugadas legales y mate comprobados con el rey a salvo.
- Bots easy, normal y hard, modo pasar el teléfono, historial, PDF y XP.
- Mesa con kanji, selección, destinos legales y diálogo de promoción.
- 3 pruebas nuevas de Shogi.
- El catálogo llega a **21 juegos jugables de 30**.

### Cambiado

- Android: `versionCode=26`, `versionName=2.4.0`.
- El inicio, catálogo y ajustes muestran 21 juegos y la versión 2.4.0.

### Pendiente

- Janggi, Mahjong y los 7 juegos restantes.
- Relay público, replays web y accesibilidad completa.
- Teléfono físico.

## 2.3.0 · 2026-10-30

El ábaco.

### Añadido

- **Soroban jugable**: ábaco japonés de 13 varillas, cuentas celestiales y terrestres.
- Retos de 1 a 6 cifras, diez aciertos por partida, puntuación, racha y mejor racha.
- Controles por varilla, comprobación, siguiente nivel y limpieza total.
- Historial, PDF y XP al completar.
- 2 pruebas nuevas de Soroban.
- El catálogo llega a **20 juegos jugables de 30**.

### Cambiado

- Android: `versionCode=25`, `versionName=2.3.0`.
- El inicio, catálogo y ajustes muestran 20 juegos y la versión 2.3.0.
- La categoría `herramienta` se muestra como Herramienta.

### Pendiente

- Shogi, Janggi, Mahjong y los 8 juegos restantes.
- Relay público, replays web y accesibilidad completa.
- Teléfono físico.

## 2.2.0 · 2026-10-30

La estrella conectada.

### Añadido

- **Damas Chinas en sala** de 2–6 jugadores con estrella de 121 hoyos, saltos encadenados, bots y meta opuesta.
- Validación autoritativa de turno, origen, destino y salto en el anfitrión.
- Bot de reemplazo para asientos vacíos o invitados desconectados.
- 8 juegos con sala nativa: Damas, Mancala, Dominó, Serpientes, Brisca, Truco, Ludo y Damas Chinas.
- 1 prueba nueva del códec de Damas Chinas.

### Cambiado

- Android: `versionCode=24`, `versionName=2.2.0`.
- El lobby nativo incluye Damas Chinas con selector de 2–6 jugadores.
- Los asientos de invitado se amplían hasta el 5.
- El inicio y el catálogo muestran la 2.2.0.

### Pendiente

- Relay público con TLS, replays web adicionales y accesibilidad completa.
- Los 11 juegos restantes y el teléfono físico.

## 2.1.0 · 2026-10-29

Salas grandes.

### Añadido

- **Ludo en sala** de 2–4 jugadores con tablero 15×15, dado, capturas, turnos extra y meta exacta.
- Validación autoritativa de tirada y movimiento en el anfitrión.
- Bot de reemplazo para asientos vacíos o invitados desconectados.
- 7 juegos con sala nativa: Damas, Mancala, Dominó, Serpientes, Brisca, Truco y Ludo.
- 1 prueba nueva del códec de Ludo.
- Prueba real de sala con anfitrión Android e invitado WebSocket.

### Cambiado

- Android: `versionCode=23`, `versionName=2.1.0`.
- El lobby nativo incluye Ludo con selector de 2, 3 o 4 jugadores.
- El inicio y el catálogo muestran la 2.1.0.

### Pendiente

- Damas Chinas en sala.
- Relay público con TLS, replays web adicionales y accesibilidad completa.
- Los 11 juegos restantes y el teléfono físico.

## 2.0.0 · 2026-10-29

La mesa conectada.

### Añadido

- **Brisca en sala** de 2–4 jugadores con manos privadas, parejas, bots, historial, audio y PDF.
- **Truco en sala** de 2 jugadores con envido, real envido, truco, retruco, vale cuatro y respuestas sincronizadas.
- **Códec de cartas** que oculta la mano rival y la baraja restante; el anfitrión valida cada jugada y canto.
- **Bot de reemplazo** si un invitado se desconecta a mitad de partida.
- 3 pruebas nuevas del códec de cartas.
- 6 juegos con sala nativa: Damas, Mancala, Dominó, Serpientes, Brisca y Truco.

### Cambiado

- Android: `versionCode=22`, `versionName=2.0.0`.
- El inicio, catálogo y ajustes muestran la 2.0.0.
- El lobby nativo permite elegir Brisca (2/4) y Truco (2).

### Pendiente

- Ludo y Damas Chinas en salas.
- Relay público con TLS, replays web adicionales y accesibilidad completa.
- Los 11 juegos restantes del catálogo y el teléfono físico.

## 1.9.0 · 2026-10-29

La baraja completa.

### Añadido

- **Brisca nativa** de 2–4 jugadores con triunfo, bazas, parejas, meta configurable, bots, historial, PDF y XP.
- **Truco nativo** de 2 jugadores con envido, real envido, truco, retruco, vale cuatro, mazo, bots, historial, PDF y XP.
- 8 pruebas nuevas de Brisca y Truco.
- El catálogo llega a **19 juegos jugables** de 30.

### Cambiado

- Android: `versionCode=21`, `versionName=1.9.0`.
- El inicio, catálogo, ajustes y portada muestran la 1.9.0 y 19 juegos.
- Las novedades describen Brisca y Truco.

### Pendiente

- Ludo y Damas Chinas en salas.
- Brisca y Truco online.
- Relay público, replays web adicionales y accesibilidad completa.
- Teléfono físico bloqueado por MIUI.

## 1.8.0 · 2026-10-29

Treinta juegos.

### Añadido

- **Catálogo de 30 juegos** sin duplicados: 17 jugables y 13 próximos.
- **Reversi nativo** 8×8 con volteo, passing, esquinas, movilidad y bots de tres dificultades.
- **Juego Real de Ur nativo** de 20 casillas, rosetas, capturas, turno extra y llegada exacta.
- **Brisca** y **Truco** en el catálogo como próximos juegos.
- 11 pruebas nuevas de Reversi y Ur.
- Portada y catálogo muestran 17 de 30.

### Cambiado

- Android: `versionCode=20`, `versionName=1.8.0`.
- El inicio y los ajustes muestran 1.8.0.
- Se eliminó la ficha duplicada de Ur.

### Pendiente

- Brisca y Truco nativos.
- Ludo y Damas Chinas en salas.
- Dominó por parejas, relay público y accesibilidad completa.
- Teléfono físico bloqueado por MIUI.

## 1.7.0 · 2026-10-29

La mesa llena.

### Añadido

- **Dominó nativo de 2–4 jugadores** en salas LAN/online, con manos privadas, pozo oculto, asientos, bots, puntos y siguiente mano.
- Configuración de 2, 3 o 4 jugadores de Dominó desde el lobby.
- **Reproducción automática** de replays en Damas y Mancala.
- **Visor web de replays** con tableros de Damas y Mancala, lista de jugadas y auto-play.
- El enlace compartido incluye versión nativa y versión web.
- Accesibilidad de fichas de Ludo, puntos de Backgammon y tablero de Go.
- 4 pruebas nuevas del códec de Dominó.

### Cambiado

- Android: `versionCode=19`, `versionName=1.7.0`.
- El catálogo y los ajustes muestran 1.7.0.
- El lobby diferencia Dominó y Serpientes de 2–4 jugadores.

### Corregido

- Las manos ajenas y las fichas del pozo no viajan a los invitados.
- El anfitrión solo acepta jugadas del asiento y turno correctos.

### Pendiente

- Ludo y Damas Chinas de 3–6 jugadores.
- Relay público y enlaces universales.
- Replay web de Póker, Caída y Furia.
- Accesibilidad completa y teléfono físico.

## 1.6.0 · 2026-10-29

La mesa grande.

### Añadido

- **Serpientes y Escaleras nativo para 2–4 jugadores** en salas LAN/online, con asientos, turnos, bots de relleno, escaleras, serpientes, número exacto y XP.
- **Relay online configurable** al crear la sala; si se deja vacío se usa el relay LAN del teléfono.
- **Replays exportables**: enlace `mesaviva://replay`, archivo JSON en `Descargas/Mesa Viva`, compartición e importación desde Ajustes o enlace profundo.
- **Audio en las quince mesas**: Ajedrez, Ludo, Go, Jungle, Xiangqi y Damas Chinas se suman a los efectos procedurales.
- Asientos visibles en el lobby y espectadores en Damas/Mancala.
- Casillas de Serpientes descritas para lectores de pantalla.
- 13 pruebas nuevas de reglas, códec y replays.
- Medición de arranque en frío en emulador.

### Cambiado

- Android: `versionCode=18`, `versionName=1.6.0`.
- La cabecera distingue LAN, online y sala conectada.
- El catálogo y los ajustes muestran 1.6.0.

### Corregido

- El anfitrión solo acepta jugadas de los asientos que corresponden.
- Un invitado nuevo puede ocupar un asiento y los bots cubren los huecos.
- El replay valida estrictamente el formato y los rangos.

### Pendiente

- Dominó, Ludo y Damas Chinas de 3–8 jugadores.
- Relay público desplegado y enlaces universales.
- Reproductor de replay automático y visor web.
- Accesibilidad completa y teléfono físico.

## 1.5.0 · 2026-10-29

La mesa conectada.

### Añadido

- **Salas nativas LAN** para Damas y Mancala: anfitrión, invitado, QR, escáner integrado, código, chat, reconexión y bot de relleno.
- Enlace profundo `mesaviva://join?ws=…&code=…` y entrada manual por `ws://`.
- El anfitrión valida cada jugada con el motor Kotlin y difunde el estado; el invitado nunca es autoridad.
- **Replays con semilla** en Damas y Mancala, con paso anterior/siguiente y contador de jugada.
- **PDF con tarjeta de estadísticas** para Damas y Mancala.
- **Audio nativo procedural**: música original por festividad y efectos de carta, captura, coronación, dados, victoria, derrota, empate, chat y toque.
- XP real al terminar partidas nativas y varias mesas locales, con racha y mejor racha.
- Fichas de dominó y casillas de Damas descritas para lectores de pantalla.
- Códec `RoomCodec` y 9 pruebas nuevas de protocolo y semilla.
- Gradle **8.14.4**.

### Cambiado

- Android: `versionCode=17`, `versionName=1.5.0`.
- La sala separa **Sala nativa** y **Sala web**.
- El catálogo y los ajustes muestran 1.5.0.
- La pestaña web sigue siendo el respaldo para los quince juegos y las salas remotas.

### Corregido

- El invitado ve el tablero de Damas con sus piezas abajo.
- Un invitado nuevo puede ocupar el asiento si el anterior se desconectó.
- El historial de Damas y Mancala ya no se limita a cinco líneas para el PDF y el replay.

### Pendiente

- Salas nativas de 3–8 jugadores.
- Relay online nativo y replay exportable.
- Accesibilidad completa y audio en las quince mesas.
- Instalación en el teléfono físico bloqueado por MIUI.

## 1.4.0 · 2026-10-29

Crónica y accesibilidad.

### Añadido

- **Crónica PDF nativa en las 15 mesas**: título, subtítulo, fecha, historial completo numerado, paginación y pie de página.
- Guardado en `Descargas/Mesa Viva` en Android 10+; en Android 8–9 se usa almacenamiento privado y `FileProvider`.
- Compartición del PDF con `ACTION_SEND` desde Mesa Viva.
- `FileProvider` y `res/xml/file_paths.xml`.
- Descripciones semánticas en cartas españolas, de Furia y de Póker.
- Banner de turno como región viva para lectores de pantalla.
- Historial completo en Ludo, Mancala, Backgammon, Dominó y Serpientes.
- Evidencia visual y PDF real en `docs/evidence/v1.4.0/`.

### Cambiado

- Android: `versionCode=16`, `versionName=1.4.0`.
- El inicio, el catálogo y los ajustes pasan a 1.4.0.
- La web se compila con Vite 6.4.5 y se sincroniza dentro del APK.

### Corregido

- El PDF se genera sin permisos de almacenamiento en Android 10+.
- El historial de las mesas clásicas ya no se limita a cinco líneas.
- La crónica incluye nombre del juego y contexto, no solo “History”.
- La compartición del PDF respeta el selector del sistema.

### Pendiente

- Salas nativas y replays con semilla.
- Accesibilidad completa y audio nativo.
- Teléfono físico bloqueado por MIUI.
- Gradle 8.14.4+ y perfil de rendimiento.

## 1.3.0 · 2026-10-29

La mesa completa.

### Añadido

- **Póker Texas Hold'em nativo en Compose**: baraja francesa de 52 cartas, 2–6 jugadores, ciegas 10/20, dealer rotativo, preflop/flop/turn/river, subidas con mínimo, all-in, botes laterales, devolución de apuestas no igualadas y showdown.
- **Evaluador de la mejor mano de 5 entre 7**: carta alta, pareja, doble pareja, trío, escalera, color, full, póker y escalera de color.
- **Showdown nativo**: filas por jugador, mano, cartas ganadoras, bote repartido y siguiente mano.
- Bots con fuerza de mano, pot odds, agresividad y tres dificultades.
- `PokerCardFace` con baraja francesa, reverso y estados de carta.
- 8 pruebas Kotlin nuevas; **86 pruebas en total** (30 web + 55 nativas).
- Evidencia visual en `docs/evidence/v1.3.0/`.

### Cambiado

- Android: `versionCode=15`, `versionName=1.3.0`.
- El inicio y el catálogo pasan a **15 juegos 100 % Compose**.
- Póker se marca como nativo; ya no queda ningún juego jugable en WebView.
- El WebView queda como respaldo de la PWA y de la sala online.
- La web se compila con Vite 6.4.5 y se sincroniza dentro del APK.

### Corregido

- Póker mantiene el bote, las apuestas y el turno después de cada calle.
- El color activo de Furia ya cambia al jugar una carta de otro color, en Kotlin y en la web.
- Las manos ajenas permanecen ocultas y la mano del jugador activo se muestra en modo pasar el teléfono.
- El showdown devuelve apuestas no igualadas antes de repartir botes laterales.
- La mesa de Póker muestra ciegas, bote, apuesta actual y cartas comunitarias.

### Pendiente

- Salas nativas y reconexión.
- Crónica nativa con PDF y replays con semilla.
- Accesibilidad completa, audio nativo y prueba en teléfono físico.
- Gradle 8.14.4+ y perfil de rendimiento.

## 1.2.0 · 2026-10-29

La mesa completa.

### Añadido

- **Caída nativa en Compose**: baraja española de 40 cartas dibujada, entrega de 4, cantos, caídas, mesa limpia, parejas, meta 40/70/100, conteo de cartas y bots.
- **Furia de Colores nativa en Compose**: 108 naipes originales, comodines, salto, reversa, roba dos, roba cuatro, ¡Última!, elección de color y bots.
- `CardUi.kt`: baraja española, baraja Furia, reversos, etiquetas y partículas de polvo.
- Modo “pasar el teléfono” con mano oculta y botón **Ver mi mano**.
- 12 pruebas Kotlin nuevas; **78 pruebas en total** (30 web + 48 nativas).
- Evidencia visual en `docs/evidence/v1.2.0/`.

### Cambiado

- Android: `versionCode=14`, `versionName=1.2.0`.
- El inicio y el catálogo pasan a **14 juegos 100 % Compose**; solo Póker sigue en WebView.
- Se corrigió el color activo de Furia al jugar una carta de otro color que coincide en número o acción, en Kotlin y en la mesa web.
- La web se compila con Vite 6.4.5 y se sincroniza dentro del APK.

### Corregido

- Caída permite entregar, cantar antes de jugar, encadenar capturas y contar cartas al final de la mano.
- Furia cambia el color activo al jugar una carta de otro color y exige color en los comodines.
- Las manos de los bots no se muestran en Furia; la mano del jugador permanece visible.
- La baraja española distingue oros, copas, espadas y bastos con sota, caballo y rey.
- El tablero de Caída muestra el mazo, la mesa, la entrega, las capturas y el historial de cinco jugadas.

### Pendiente

- Salas nativas para los juegos Compose y reconexión.
- Póker 100 % Compose.
- Crónica nativa con PDF y replays con semilla.
- Accesibilidad completa, audio nativo y prueba en teléfono físico.
- Gradle 8.14.4+ y perfil de rendimiento.

## 1.1.0 · 2026-10-28

La mesa crece.

### Añadido

- **Damas nativo en Compose**: 8×8, 12 fichas por lado, captura obligatoria, cadenas, coronación, empate por 80 jugadas, bot alfa-beta y modo pasar el teléfono.
- **Dados Mentirosos nativo en Compose**: 2–6 jugadores, cinco dados, pujas, “Dudo”, revelación, eliminación, privacidad al pasar el teléfono y bot con umbral de riesgo.
- **Go nativo en Compose**: 9×9 y 13×13, grupos y libertades, ko simple, komi 6.5, conteo de territorio, capturas y bot heurístico.
- **Jungle nativo en Compose**: 7×9, río, trampas, guaridas, rata/elefante, saltos de tigre y león, y bot alfa-beta.
- **Xiangqi nativo en Compose**: palacio, río, caballo bloqueado, cañón, soldados, generales enfrentados, jaque, mate, ahogamiento y bot alfa-beta.
- **Damas Chinas nativo en Compose**: estrella de 121 hoyos, 2–6 jugadores, saltos encadenados, progreso por triángulo y bot con distancia BFS.
- Interfaz nativa compartida para las nuevas mesas: cabecera, banner de turno, últimas cinco jugadas e historial completo.
- 19 pruebas Kotlin nuevas; **66 pruebas en total** (30 web + 36 nativas).
- AAB y APK de release firmados, instalados y verificados en emulador Android 14.
- Evidencia visual en `docs/evidence/v1.1.0/`.

### Cambiado

- Android: `versionCode=13`, `versionName=1.1.0`.
- El catálogo nativo marca doce juegos como “Nativo Compose”.
- El inicio, el catálogo y las novedades pasan a “12 juegos 100 % Compose”.
- `VERSION`, `package.json`, README, ROADMAP y portada pasan a 1.1.0.
- La migración Compose se documenta como fase 3 parcial: 12 de 15 juegos fuera del WebView.

### Corregido

- El tablero de Damas usa la diagonal correcta para las blancas y permite la coronación.
- Las nuevas mesas muestran siempre las últimas cinco jugadas y el historial completo.
- Dados oculta los cubiletes de los bots y pide confirmación visual al pasar el teléfono.
- Go ya no llena el tablero de puntos de jugadas legales; solo marca la última piedra.
- Los diálogos de preparación y resultado no dejan la mesa bloqueada.

### Pendiente

- Salas nativas para los doce juegos Compose y reconexión.
- Caída, Furia de Colores y Póker 100 % Compose.
- Crónica nativa con PDF y replays con semilla.
- Accesibilidad completa, prueba en teléfono físico y checklist de tienda.
- Actualización a Gradle 8.14.4+ y perfil de rendimiento.

## 1.0.0 · 2026-10-28

La mesa completa.

### Añadido

- **Ludo nativo en Compose**: 2–4 jugadores, cuatro fichas, salida con 6, capturas, casillas seguras, tres seises, turno extra y meta exacta. Tablero 15×15, bots y modo pasar el teléfono.
- **Mancala nativo en Compose**: siembra, captura, turno extra, almacenes, bots y modo local.
- **Backgammon nativo en Compose**: barra, capturas, dobles, bear-off, pips y bots.
- **Seis juegos 100 % Compose**: Serpientes, Dominó, Ajedrez, Ludo, Mancala y Backgammon.
- 8 pruebas Kotlin nuevas; **47 pruebas en total** (30 web + 17 nativas).
- **AAB de release firmado**, además del APK.

### Cambiado

- Android: `versionCode=12`, `versionName=1.0.0`.
- El catálogo nativo marca Ludo, Mancala y Backgammon como “Nativo Compose”.
- El inicio muestra los seis juegos nativos y las novedades de 1.0.0.
- La portada y el README pasan a 1.0.0 y a seis juegos nativos.

### Corregido

- El tablero de Mancala usa hoyos ovalados y no columnas estiradas.
- El dado de Backgammon conserva la última tirada después de mover.
- La barra inferior se oculta en Ludo, Mancala y Backgammon para dejar espacio a la mesa.
- Los juegos nativos conservan el historial de las últimas jugadas en pantalla.

### Pendiente

- Salas online nativas y reconexión.
- Retirar el WebView de los 9 juegos restantes.
- Probar APK/AAB y LAN en el teléfono físico.
- Accesibilidad avanzada y checklist de tienda.

## 0.9.0 · 2026-10-28

La mesa nativa.

### Añadido

- **Dominó nativo en Compose**: doble 6, 2–4 jugadores, parejas, pozo, tranca, cierre de mano, meta 100/150/200, bots y fichas con pips dibujadas.
- **Ajedrez nativo en Compose**: enroque, captura al paso, promoción elegible, jaque, mate, ahogado, 50 movimientos, triple repetición, material insuficiente, historial SAN y bot alfa-beta.
- **Sala nativa**: crear sala, unirse por código, escanear QR, QR local con ZXing y selector de ronda de juegos.
- **Sincronización de ajustes**: la cáscara pasa idioma, música, efectos, vibración y movimiento reducido a la web; `ShellSync` devuelve el perfil.
- 9 pruebas Kotlin de Dominó y Ajedrez.
- Evidencia visual en `docs/evidence/v0.9.0/`.

### Cambiado

- Android: `versionCode=11`, `versionName=0.9.0`, dependencia ZXing y JUnit.
- El catálogo nativo marca Serpientes, Dominó y Ajedrez como “Nativo Compose”.
- La navegación inferior añade Sala y el inicio muestra la sección “100 % Compose”.
- Póker, Caída y los demás juegos siguen compartiendo el motor web.

### Corregido

- Las piezas blancas de ajedrez se ven con contraste alto sobre cualquier casilla.
- El tablero de dominó ocupa el espacio disponible con una mesa de fieltro.
- La tirada de dados de Serpientes ya no muestra un punto cuando el dado está en cero.
- El QR de sala se genera localmente y no depende de un servicio externo.

### Pendiente

- Salas online nativas para Dominó y Ajedrez.
- Retirar el WebView de los 12 juegos restantes.
- Probar la APK y la sala LAN en el teléfono físico (el dispositivo no estaba conectado en esta sesión).

## 0.8.0 · 2026-10-28

Póker y cáscara Compose.

### Añadido

- **Póker de la casa**: Texas Hold'em 2–6 jugadores sin dinero real, ciegas 10/20, preflop/flop/turn/river, subidas con mínimo, all-in, botes laterales, devolución de apuestas no igualadas, showdown y eliminación por stack.
- Evaluador de la mejor mano de 5 entre 7 cartas y bots con tres dificultades.
- Interfaz de póker con fichas, bote, cartas comunitarias, ciegas, botón de reparto, ciegas por jugador, modal de subida, showdown y últimos movimientos.
- **Cáscara nativa Jetpack Compose**: inicio, catálogo de 15 juegos, niveles, ajustes, navegación inferior y tema Material 3 con acento de festividad + color dinámico.
- **Serpientes y Escaleras 100 % Compose**: tablero 10×10, escaleras/serpientes dibujadas, dado animado, bots, vibración y diálogo de victoria.
- `GameWebActivity` separada para la mesa compartida; el perfil de la web se importa a la cáscara al volver.
- 5 pruebas nuevas de póker (30 en total).

### Cambiado

- Android: Kotlin 2.4.21, AGP 8.13.2, Gradle 8.13, Compose BOM 2025.10.00 y `activity-compose` 1.10.1.
- `versionCode=10` y `versionName=0.8.0`.
- Catálogo: 15 juegos jugables; la portada, el onboarding y el README pasan de 14 a 15.
- El póker reemplaza su estado “próximamente” por jugable en la web y la APK.
- La página principal muestra la versión 0.8.0 y la nueva entrada del historial.

### Corregido

- El tablero de póker ya no se recorta: la mesa se organiza en modo compacto y la subida se abre en modal.
- Las cartas comunitarias, el bote y la mano propia caben en la superficie de juego.
- El perfil de la cáscara se refresca al volver de la mesa.

### Pendiente

- Prueba en teléfono físico y sala LAN; el equipo MIUI bloqueó la instalación por USB durante esta sesión.
- Migración de los 14 juegos restantes a Compose (v0.9.0+).

## 0.7.1 · 2026-10-28

Caída, mesa legible.

### Corregido

- Bots de Caída a 1500 ms + 700 ms tras caída, canto, entrega o fin de mano.
- Entrega visible con 4 cartas, resultado y puntos.
- Carta recién jugada resaltada; capturas con fantasma, flecha y flotante +N.
- Mano centrada sin cartas cortadas ni frames con cartas de más.
- Cabecera compacta para 4 jugadores.
- Píldora de captura con limpieza por mano; ya no queda congelada.
- Barra de acciones con altura reservada y toast desplazado.
- Canto solo antes de la primera carta; sin victoria instantánea.
- Reglas completas y meta por defecto 100.
- Pluralización y pistas de cartas corregidas.
- Partículas capadas y `reduceMotion` automático en equipos de baja potencia.
- 25 pruebas.

## 0.7.0 · 2026-10-28

La estrella.

### Añadido

- **Damas Chinas** con tablero estrella de 121 hoyos y coordenadas axiales.
- 2–6 jugadores, 10 fichas, saltos encadenados y meta en el triángulo opuesto.
- Bot por cercanía a la meta y saltos.
- 23 pruebas de motores.

### Cambiado

- Catálogo: 14 juegos jugables.
- Versión de app, service worker y Android a 0.7.0; `versionCode=8`.

### Pendiente

- Póker y Compose fase 1 pasan a v0.8.0.

## 0.6.0 · 2026-10-28

Compose, primera piedra.

### Añadido

- `MainActivity` migrada a `ComponentActivity` y `setContent` con `AndroidView`.
- Compose BOM 2024.12.01, activity-compose, ui, foundation y material3.
- Plugin de Compose para Kotlin 2.0.21.
- APK v0.6.0 probado e instalado en el teléfono real `23117RA68G`; inicio y Caída renderizan sin cierres.

### Notas

- La mesa sigue en WebView; **no es una migración completa a Compose**.
- Damas Chinas y Póker pasan a v0.7.0.
- El APK sube a 8.19 MB por el runtime de Compose.

## 0.5.0 · 2026-10-28

El general y el río.

### Añadido

- **Xiangqi**: tablero 9×10, palacio, río, elefante con ojo, caballo con pata, cañón con pantalla, soldados y generales enfrentados.
- Jaque, jaque mate, ahogado, rendición y bot alfa-beta.
- 22 pruebas de motores.
- Portada y versión 0.5.0.

### Pendiente

- Damas Chinas y Póker pasan a v0.6.0.
- Prueba de sala LAN en teléfono físico.

## 0.4.0 · 2026-10-28

Mesa resistente y Android 16.

### Añadido

- **Reconexión de invitados**: el invitado conserva su asiento y vuelve a la sala tras perder la conexión.
- Relay Node y relay LAN de Android aceptan `rejoin`, mantienen una ventana de gracia de 60 s y distinguen salida voluntaria de caída.
- Avisos de “reconectando” y “volviste a la mesa”.
- **Android 16**: edge-to-edge con insets de barras/teclado y back predictivo integrado con el WebView.
- **APK de release firmado** (RSA 4096) además del APK de depuración.
- **Portada web** de presentación en la raíz de Pages, con historial de versiones y descargas; el juego queda en `/play/`.
- Prueba de protocolo de reconexión con relay real.

### Cambiado

- Versión de app y service worker a 0.4.0; `versionCode=5`.
- Android se mantiene en `targetSdk=36` y sin librerías nativas `.so`.

### Pendiente

- Los juegos Damas Chinas, Xiangqi y Póker pasan a v0.5.0 para no sacrificar calidad.
- La prueba de la sala LAN sigue pendiente en teléfono físico.

## 0.3.0 · 2026-10-28

La mesa clásica.

### Añadido

- **Ludo** 2–4 jugadores: cuatro fichas, salida con 6, capturas, casillas seguras, tres seises, turno extra y meta exacta.
- **Backgammon**: 15 fichas, barra, capturas, dobles, bear-off y conteo de pips.
- Tableros interactivos propios para Ludo y Backgammon, con selección de fichas y destinos.
- 21 pruebas de motores (6 nuevas para Ludo y Backgammon).

### Cambiado

- Catálogo: Ludo y Backgammon pasan a jugables.
- Portada, README y contadores: de 10 a 12 juegos.
- Versión de app y service worker a 0.3.0.
- Android: `compileSdk`/`targetSdk` 36 y workflow con SDK 36.

## 0.2.0 · 2026-10-28

La mesa se amplía y se vuelve local.

### Añadido

- **Go** 9×9 y 13×13: capturas de grupos, ko, pases, rendición, komi 6.5 y conteo de territorio.
- **Jungle / Dou Shou Qi**: río, trampas, jerarquía animal, saltos de tigre y león, guaridas y bots con búsqueda alfa-beta.
- **Sala LAN sin internet en Android**: relay WebSocket nativo en el teléfono anfitrión, QR con la IP local y código de sala, unión automática del invitado y limpieza del servidor al salir.
- Preparación de Go con tamaño de tablero 9×9/13×13.
- 3 pruebas nuevas de motores (15 en total), incluidas las reglas básicas de Go y Jungle.

### Cambiado

- Catálogo: Go y Jungle pasan de “próximos” a jugables.
- Portada, README y contadores: de 8 a 10 juegos.
- Salas LAN: el invitado configura el relay desde el QR sin escribir direcciones.

### Corregido

- El cierre de una sala LAN detiene el relay nativo y limpia las claves locales.
- La app limpia un relay LAN huérfano al arrancar.

## 0.1.1 · 2026-10-28

Pulido de mesa y CI.

### Corregido

- Furia y Dominó muestran “7 cartas” / “6 fichas” en vez de “puntos” en la mano.
- Los tableros de ajedrez y damas ya no muestran coordenadas en cada casilla; las coordenadas quedan como etiquetas accesibles.
- Mancala identifica los almacenes con el nombre del jugador o “Tú”, no con B/W.
- El workflow Android usa el SDK preinstalado del runner (la acción antigua intentaba instalar un paquete `tools` eliminado).
- Onboarding: rejilla de avatares en 5 columnas para no recortarse.

## 0.1.0 · 2026-10-27

Primera mesa funcional.

### Añadido

- 8 juegos con bots: Caída, Furia de Colores, Dominó, Dados Mentirosos, Ajedrez, Damas, Serpientes y Escaleras, Mancala.
- Modo pasar el teléfono con pantalla de privacidad; mezcla de humanos y bots.
- Salas en línea con relay WebSocket, código de 6 caracteres, QR, chat, playlist y puntuación acumulada.
- Host autoritativo y vistas redactadas por jugador.
- Perfil local con 10 niveles, XP, rachas, victorias y recompensas.
- 11 idiomas de interfaz y temas por festividad.
- Semana Santa/Carnaval/Corpus calculados con Pascua; independencia según país.
- Color de acento por temporada, Material You o color dominante del fondo.
- Música generativa y SFX sintetizados; partículas; háptica; sensores para agitar el teléfono.
- Crónica de partida con últimos 5 movimientos, historial completo y PDF.
- Diagnóstico local con envío por correo y exportación.
- PWA instalable/offline y APK Android con `WebViewAssetLoader`.
- CI, Pages, build Android, Dependabot, SECURITY, privacidad, roadmap y arquitectura.

### Notas

- La sala LAN sin servidor por doble QR queda para v0.2; los modos locales sí funcionan offline.
- El catálogo muestra 30 juegos con su estado; los jugables son los 8 anteriores.
- El APK de CI es de depuración; la firma de release se documenta en `docs/DEPLOY.md`.
- Reglas de juegos tradicionales adaptadas y documentadas como variante Mesa Viva.


