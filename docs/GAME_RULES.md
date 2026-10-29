# Reglas y variantes

Las reglas completas viven dentro de la app (botón **Reglas** en cada mesa) y en el catálogo. Esta página resume la variante implementada en v1.2.0. Caída y Furia de Colores ya tienen mesa nativa en Jetpack Compose.

## Caída (Venezuela, 2–4 jugadores)

- Baraja española de 40 (1–7, 10–12). Meta configurable: 40, 70 o 100.
- El repartidor “entrega” cuatro cartas: 1·2·3·4 o 4·3·2·1 dan 2 puntos; fallar da 1 punto al jugador a la derecha.
- Se reparten 3 cartas. En tu turno juegas una carta. Si igualas el rango de una carta de la mesa, haces **caída**: te llevas esa carta y las que están encima. Sumas el valor del rango (1–7 = 1; 10 = 2; 11 = 3; 12 = 4). Vaciar la mesa suma 5 (“caída con mesa”).
- Cantos (una vez por mano): ronda, trivilín (5), patrulla (6), vigía (7), registro (8), registrico (10), casa chica (11), casa grande (12). Trivilín de 12 o en la primera mano gana al instante.
- Al agotarse el mazo, cada 10 cartas capturadas valen 1 punto. Gana quien llegue a la meta.
- Variante documentada: las reglas de cantos y caídas varían por barrio; Mesa Viva elige esta versión y la muestra en la mesa. No se reproduce arte tradicional.

## Furia de Colores (2–4 jugadores)

- 108 naipes originales de cuatro colores (Sol, Mar, Selva, Magma), mesa nativa en Compose.
- Se reparten 7. Juega una carta que coincida en color, número o símbolo, o un comodín.
- Salto, Reversa, Roba Dos, Comodín y Comodín Roba Cuatro. Al quedarte con una carta, la mesa grita “¡Última!”.
- Si no puedes jugar, robas una; si sirve, puedes jugarla; si no, pasas.
- Homenaje original a los juegos de combinar colores, sin marcas ni arte de terceros.

## Dominó (2–4 jugadores)

- Doble 6 o doble 12, con parejas opcionales a 4 jugadores.
- 7 fichas por jugador a 4; 10 a 2 (doble 6). Sale el doble más alto; si no, la ficha de más puntos.
- Encadena por cualquiera de los dos extremos. Si no puedes, robas del pozo; si está vacío, pasas.
- Cerrar la mano da los puntos de las manos rivales. Si la mesa se tranca, gana quien tenga menos puntos y suma los demás.
- Partida a 50, 100 o 150.

## Dados Mentirosos (2–6 jugadores)

- 5 dados y un cubilete por jugador; tirada secreta.
- Por turnos se canta cantidad y cara, siempre subiendo. “Dudo” revela todos los dados.
- Si la puja se cumple, el que dudó pierde un dado; si no, lo pierde el que cantó.
- Quien se queda sin dados sale. Gana el último con dados.
- En la mesa puedes tocar los dados o agitar el teléfono.

## Ajedrez (2 jugadores)

- Reglas ortodoxas: enroque corto/largo, captura al paso, promoción automática a dama, jaque, jaque mate, ahogado.
- Tablas por 50 movimientos, triple repetición y material insuficiente.
- IA con material, centro y búsqueda alfa-beta (3 niveles).
- Puedes rendirte desde la mesa.

## Damas (2 jugadores)

- Damas americanas 8×8, 12 fichas por lado.
- Captura obligatoria; cadena de saltos con la misma ficha; coronación al llegar a la última fila.
- Las damas se mueven y capturan en las cuatro diagonales.
- Gana quien deje al rival sin fichas o sin movimientos. Tablas tras 80 jugadas sin captura.

## Serpientes y Escaleras (2–4 jugadores)

- Tablero de 100. Escaleras suben, serpientes bajan. Necesitas el número exacto para llegar al 100; si te pasas, te quedas.
- El primero en llegar gana. Es el juego más rápido de la mesa.

## Mancala / Kalah (2 jugadores)

- 6 hoyos y un almacén por jugador, 4 semillas por hoyo.
- Siembra en sentido contrario a las agujas del reloj, saltando el almacén rival.
- Última semilla en tu almacén → repites. Última semilla en un hoyo vacío tuyo → capturas ese hoyo y el opuesto.
- Cuando un lado se queda vacío, el otro recoge sus semillas. Gana quien tenga más en el almacén.

## Próximos juegos

El catálogo de la app enlaza las reglas de Ludo, Backgammon, Damas Chinas, Go, Xiangqi, Shogi, Janggi, Jungle, Luzhanqi, Ur, Tren Mexicano, Chickenfoot, Aeroplane Chess, Póker, Mahjong, Riichi, Mahjong con naipes, Ajedrez para tres y Soroban. Se implementarán por oleadas, priorizando reglas claras, bots y una mesa digna.

## Derechos

Los nombres de juegos comerciales se usan como referencia cultural. Mesa Viva no incluye arte, logotipos, barajas ni textos propietarios. Toda la ilustración, música y redacción es original o de licencia compatible.
