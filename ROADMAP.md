# Roadmap

## v0.6.0 · Compose, primera piedra (actual)

- [x] `ComponentActivity` + `setContent` + `AndroidView`.
- [x] Compose BOM, activity-compose, ui, foundation y material3.
- [x] APK firmado probado en teléfono real.
- [ ] Compose fase 1: inicio, ajustes, niveles y lobby nativos (v0.7.0).
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

## v0.4.0 · La fiesta completa

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
