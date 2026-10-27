import type { Rng } from './types';

/**
 * Generador determinista (mulberry32). Permite repetir una partida con la
 * misma semilla para depuración, torneos y auditoría de resultados.
 */
export function makeRng(seed: number): Rng {
  let a = seed >>> 0;
  return function next(): number {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export function randomSeed(): number {
  const c = globalThis.crypto;
  if (c && typeof c.getRandomValues === 'function') {
    const buf = new Uint32Array(1);
    c.getRandomValues(buf);
    return buf[0] ?? Date.now();
  }
  return (Date.now() ^ Math.floor(Math.random() * 0xffffffff)) >>> 0;
}

export function shuffle<T>(items: T[], rng: Rng): T[] {
  const out = items.slice();
  for (let i = out.length - 1; i > 0; i -= 1) {
    const j = Math.floor(rng() * (i + 1));
    const a = out[i];
    const b = out[j];
    if (a !== undefined && b !== undefined) {
      out[i] = b;
      out[j] = a;
    }
  }
  return out;
}

export function pick<T>(items: T[], rng: Rng): T {
  if (items.length === 0) throw new Error('pick() sobre una lista vacía');
  return items[Math.floor(rng() * items.length)] as T;
}

export function randInt(rng: Rng, maxExclusive: number): number {
  return Math.floor(rng() * maxExclusive);
}

export function rolld6(rng: Rng): number {
  return 1 + Math.floor(rng() * 6);
}

export function rollDice(rng: Rng, count: number, faces = 6): number[] {
  return Array.from({ length: count }, () => 1 + Math.floor(rng() * faces));
}
