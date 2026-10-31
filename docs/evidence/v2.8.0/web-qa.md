# QA web · Mahjong · v2.8.0

- **Fecha:** 2026-10-31
- **URL:** `http://localhost:5173/#/setup/mahjong`
- **Navegador:** Chromium integrado de OpenCode
- **Modo:** contra bots, dificultad normal
- **Semilla:** aleatoria de la sesión

## Pasos y resultado

1. Se abrió el setup de Mahjong. La mesa muestra `4–4 jugadores · 35 mín.` y la opción de bots.
2. Se pulsó **Repartir y jugar**. La mesa renderizó el muro de 152 piezas, la mano de 14, los tres rivales con 13 y el río de descartes.
3. Se seleccionó **círculos 7** y se pulsó **Descartar**. La mano bajó a 13, el muro pasó de 95 a 94 y el turno pasó a **El Silbón**.
4. Los tres bots descartaron (caracteres 9, círculos 9 y bambú 1). El turno volvió al humano con el muro en 91.
5. No hubo errores de consola durante el ciclo.

## Texto observado

```text
Tu turno: elige una pieza y descarta.
Muro: 95 → 94 → 91
El Silbón descarta caracteres 9.
La Sayona descarta círculos 9.
El Hachero descarta bambú 1.
Prueba descarta círculos 7.
```

## Cobertura automática

- 8 pruebas del motor web: reparto 152/136, cuatro tríos y par, secuencias, siete pares, comodín, descarte/robo, bot legal y Mahjong con naipes.
- 7 pruebas Kotlin de festividades: Pascua, Semana Santa, Carnaval, Corpus, independencia por país y prioridades.

La mesa web está publicada en <https://emanuelindustri-source.github.io/mesa-viva/play/> y el código vive en `src/games/mahjong.ts` y `src/ui/MahjongSurface.tsx`.
