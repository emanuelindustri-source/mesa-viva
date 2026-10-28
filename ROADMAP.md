# Roadmap

## v0.9.0 · La mesa nativa (actual)

- [x] Dominó nativo en Compose: doble 6, 2–4 jugadores, parejas, pozo, tranca, meta y bots.
- [x] Ajedrez nativo en Compose: reglas completas, promoción elegible, historial SAN y bot alfa-beta.
- [x] Sala nativa: crear/unirse, QR local con ZXing, escaneo y selector de ronda.
- [x] Sincronización nativa → web de idioma, música, efectos, vibración y movimiento reducido.
- [x] 9 pruebas Kotlin + 30 pruebas web = 39.
- [x] Fichas de dominó y piezas de ajedrez dibujadas en Compose.
- [x] APK release firmado y evidencia visual en `docs/evidence/v0.9.0/`.
- [ ] Salas online nativas para Dominó y Ajedrez (hoy la sala nativa abre la mesa web).
- [ ] Retirar el WebView de los 12 juegos restantes.
- [ ] Probar la APK y la sala LAN en el teléfono físico.

## v0.8.0 · Póker y cáscara Compose

- [x] Póker Texas Hold'em 2–6 jugadores sin dinero real: ciegas, calles, subidas, all-in, botes laterales, showdown y bots.
- [x] Evaluador de la mejor mano de 5 entre 7 y 5 pruebas nuevas (30 en total).
- [x] Cáscara nativa Jetpack Compose: inicio, catálogo, niveles, ajustes y navegación.
- [x] Serpientes y Escaleras 100 % Compose con dado, escaleras, serpientes, bots y vibración.
- [x] Compose BOM 2025.10.00, Kotlin 2.4.21, AGP 8.13.2 y Gradle 8.13.
- [x] Vuelta nativa del perfil desde la web con `localStorage` (`mesa-viva-v1`).
- [x] APK firmado y verificación en emulador Android 14.
- [ ] Prueba de sala LAN en teléfono físico (bloqueada por la restricción de instalación MIUI del equipo).
- [ ] Retirada completa del WebView y migración de los 14 juegos restantes (v0.9.0+).

## v0.7.1 · Caída, mesa legible

- [x] Ritmo de bots, entrega visible, capturas animadas y mano estable.
- [x] Canto antes de la primera carta; reglas y meta documentadas.
- [x] 25 pruebas.
- [ ] Póker (v0.8.0) y lobby nativo (v0.9.0).
- [ ] Prueba de sala LAN en teléfono físico.

## v0.7.0 · La estrella

- [x] Damas Chinas con 121 hoyos, 2–6 jugadores, saltos y bots.
- [x] 23 pruebas de motores.
- [x] 14 juegos jugables.
- [ ] Póker (v0.8.0).
- [ ] Compose fase 1: inicio, ajustes y niveles (v0.8.0); lobby nativo (v0.9.0).
- [ ] Partida completa y LAN en teléfono real.

## v0.6.0 · Compose, primera piedra

- [x] `ComponentActivity` + `setContent` + `AndroidView`.
- [x] Compose BOM, activity-compose, ui, foundation y material3.
- [x] APK firmado probado en teléfono real.
- [ ] Compose fase 1: inicio, ajustes y niveles nativos (v0.8.0); lobby nativo (v0.9.0).
- [ ] Damas Chinas y Póker (v0.7.0).
- [ ] Retirada completa del WebView (v0.8.0+).

## v0.5.0 · El general y el río

Xiangqi y consolidación de la mesa.

- [x] Xiangqi 9×10 con palacio, río, cañón, caballo bloqueado y generales enfrentados.
- [x] Jaque, jaque mate, ahogado y bot alfa-beta.
- [x] 22 pruebas de motores.
- [ ] Damas Chinas y Póker (v0.6.0).
- [ ] Prueba de sala LAN en teléfono físico.
- [ ] Torneos y ligas de sala.

## v0.4.0 · Mesa resistente

Reconexión de invitados, Android 16 y APK firmado.

- [x] Reconexión automática del invitado con identidad y ventana de gracia.
- [x] `rejoin`/`leave` en relay Node y relay LAN Kotlin.
- [x] Edge-to-edge y back predictivo en Android 16.
- [x] APK de release firmado con keystore fuera del repositorio.
- [x] 21 pruebas de motores + prueba de reconexión con relay real.
- [ ] Damas Chinas, Xiangqi y Póker (v0.5.0).
- [ ] Prueba de sala LAN en teléfono físico.
- [ ] Torneos y ligas de sala.

## v0.3.0 · La mesa clásica

Ludo, Backgammon y Android moderno.

- [x] 12 juegos jugables con bots: Caída, Furia de Colores, Dominó, Dados Mentirosos, Ajedrez, Damas, Serpientes y Escaleras, Mancala, Go, Jungle, Ludo y Backgammon.
- [x] Ludo 2–4 jugadores con capturas, casillas seguras y meta exacta.
- [x] Backgammon con barra, capturas, dobles y bear-off.
- [x] 21 pruebas de motores.
- [x] APK con target Android 16 (API 36).
- [ ] Reconexión de invitados a mitad de partida y bots que cubran asientos desconectados.
- [ ] Torneos y ligas de sala.
- [ ] Validación de la sala LAN en teléfono físico.

## v0.2.0 · La mesa se amplía

Go, Jungle y la sala LAN sin internet en Android.

- [x] 10 juegos jugables: Caída, Furia de Colores, Dominó, Dados Mentirosos, Ajedrez, Damas, Serpientes y Escaleras, Mancala, Go y Jungle.
- [x] Go 9×9/13×13 con capturas, ko y conteo.
- [x] Jungle con río, trampas, saltos y guaridas.
- [x] Sala LAN sin internet con relay nativo en el teléfono anfitrión y QR.
- [x] Limpieza del relay LAN al salir o al arrancar la app.
- [x] 15 pruebas de motores.

## v0.1.1 · La primera mesa

Pulido de onboarding, cartas/fichas, coordenadas de tablero y CI Android. La base de v0.1.0 se mantiene.

- [x] 8 juegos jugables con bots: Caída, Furia de Colores, Dominó, Dados Mentirosos, Ajedrez, Damas, Serpientes y Escaleras, Mancala.
- [x] Modo pasar el teléfono con pantalla de privacidad.
- [x] Sala en línea con relay WebSocket, código y QR, chat, rondas y host autoritativo.
- [x] 10 niveles de perfil y recompensas.
- [x] 11 idiomas de interfaz.
- [x] Temas de festividad, Semana Santa/Carnaval, Material You y color del fondo.
- [x] Crónica completa y PDF.
- [x] APK Android con puentes nativos.
- [x] Diagnóstico y envío por correo.
- [x] PWA instalable y offline.

## v1.0.0 · La mesa completa

- [ ] Salas nativas para los tres juegos Compose y reconexión visible.
- [ ] Reproducción de partidas con semilla.
- [ ] Accesibilidad: lector de pantalla completo, control por teclado y tamaños de texto.
- [ ] Prueba en teléfono físico y publicación del APK/AAB.
- [ ] Mahjong americano, Riichi y Mahjong con naipes.
- [ ] Retirada progresiva del WebView hasta la paridad total.

## v0.4.0 · La fiesta completa (histórico)

- [ ] Mahjong americano, Riichi y Mahjong con naipes.
- [ ] Aeroplane Chess, Ajedrez para tres y Soroban interactivo.
- [ ] Editor de reglas de la casa por sala.
- [ ] Replays reproducibles con semilla y exportación.
- [ ] Accesibilidad avanzada: lector de pantalla completo, control por teclado y tamaños de texto.
- [ ] Firma de release, AAB y proceso de publicación en tiendas.
- [ ] Relay autohospedado de referencia con métricas y moderación.

## Principios

1. **Offline primero.** Los bots y el modo local no dependen de la red.
2. **Originalidad.** Sin copiar arte, marcas ni reglamentos propietarios.
3. **Mesa segura.** El host valida; el invitado nunca ve lo que no debe.
4. **Cultura venezolana con respeto.** Caída, dominó, leyendas y festividades, sin caricaturizar.
5. **Accesible y hospitalaria.** 11 idiomas, movimiento reducido, alto contraste y un tono cálido.
