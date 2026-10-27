import { describe, expect, it } from 'vitest';
import { makeRng } from '../rng';
import { awardXp, levelForXp } from '../levels';
import { detectCanto } from '../../games/caida';
import { furiaModule } from '../../games/furia';
import { dominoModule } from '../../games/domino';
import { dadosModule } from '../../games/dados';
import { ajedrezModule } from '../../games/ajedrez';
import { damasModule } from '../../games/damas';
import { mancalaModule } from '../../games/mancala';
import { serpientesModule } from '../../games/serpientes';
import { Match } from '../match';
import type { Seat } from '../types';
import { easterSunday, resolveFestival } from '../../theme/festivals';

const seats = (n: number): Seat[] => Array.from({ length: n }, (_, i) => ({
  id: `p${i}`,
  name: `Jugador ${i + 1}`,
  avatar: 'jaguar',
  isBot: i > 0,
}));

describe('núcleo', () => {
  it('el azar con semilla es reproducible', () => {
    const a = makeRng(42);
    const b = makeRng(42);
    expect([a(), a(), a()]).toEqual([b(), b(), b()]);
  });

  it('calcula los 10 niveles y limita la XP por partida', () => {
    expect(levelForXp(0).level).toBe(1);
    expect(levelForXp(7000).level).toBe(10);
    expect(levelForXp(999999).level).toBe(10);
    const xp = awardXp({ won: true, points: 24, difficulty: 'hard', humanPlayers: 4, streak: 9 });
    expect(xp).toBeGreaterThan(80);
    expect(xp).toBeLessThanOrEqual(170);
  });

  it('calcula Pascua y resuelve Halloween criollo', () => {
    expect(easterSunday(2026).getMonth()).toBe(3);
    expect(easterSunday(2026).getDate()).toBe(5);
    const festival = resolveFestival(new Date(2026, 9, 27), 'VE');
    expect(festival.id).toBe('halloween');
    expect(festival.characters.length).toBeGreaterThan(0);
  });
});

describe('Caída', () => {
  it('detecta cantos venezolanos', () => {
    const card = (suit: string, rank: number) => ({ id: `${suit}-${rank}`, suit, rank });
    expect(detectCanto([card('oros', 1), card('copas', 11), card('espadas', 12)])?.key).toBe('registro');
    expect(detectCanto([card('oros', 1), card('copas', 11), card('espadas', 11)])?.key).toBe('casaChica');
    expect(detectCanto([card('oros', 10), card('copas', 11), card('espadas', 12)])?.key).toBe('patrulla');
    expect(detectCanto([card('oros', 7), card('copas', 7), card('espadas', 7)])?.points).toBe(5);
  });

  it('reparte, entrega la mesa y juega sin filtrar la mano rival', () => {
    const match = new Match({ gameId: 'caida', seats: seats(2), options: {}, difficulty: 'normal', seed: 7 });
    const before = match.view('p0');
    expect(before.gameId).toBe('caida');
    expect(before.phase).toBe('entrega');
    const deliver = match.act('p0', { type: 'deliver' });
    expect(deliver.ok).toBe(true);
    const mine = match.view('p0');
    const rival = match.view('p1');
    expect(mine.hand.length).toBe(3);
    expect(rival.hand.length).toBe(3);
    expect(mine.hand.every((c) => c.id !== rival.hand[0]?.id)).toBe(true);
    expect(mine.players.every((p) => p.cardsLeft === 3)).toBe(true);
    expect(mine.phase).toBe('play');
    expect(mine.turnId).toBe('p1');
  });
});

describe('Furia', () => {
  it('solo muestra la mano propia y conserva 7 cartas iniciales', () => {
    const match = new Match({ gameId: 'furia', seats: seats(3), options: {}, difficulty: 'normal', seed: 11 });
    const view = match.view('p0');
    expect(view.hand.length).toBe(7);
    expect(view.players.find((p) => p.id === 'p1')?.cardsLeft).toBe(7);
    expect(view.center.cards?.length).toBe(1);
    expect(view.hand.every((c) => typeof c.id === 'string')).toBe(true);
    expect(furiaModule.meta.maxPlayers).toBe(4);
  });
});

describe('Dominó', () => {
  it('inicia con una ficha en la mesa y reparte las 28 fichas', () => {
    const match = new Match({ gameId: 'domino', seats: seats(4), options: {}, difficulty: 'normal', seed: 13 });
    const view = match.view('p0');
    expect(view.handTiles?.length).toBeGreaterThanOrEqual(6);
    expect(view.handTiles?.length).toBeLessThanOrEqual(7);
    expect(view.center.tiles?.length).toBe(1);
    const inHands = view.players.reduce((total, player) => total + (player.cardsLeft ?? 0), 0);
    expect(inHands + 1).toBe(28);
    expect(view.extra?.left).not.toBeNull();
    expect(dominoModule.meta.maxPlayers).toBe(4);
  });
});

describe('Dados Mentirosos', () => {
  it('oculta los dados rivales y muestra solo los propios', () => {
    const match = new Match({ gameId: 'dados', seats: seats(3), options: {}, difficulty: 'normal', seed: 17 });
    const view = match.view('p0');
    expect(view.handDice?.length).toBe(5);
    const hidden = (view.center.dice ?? []).filter((d) => d.hidden);
    expect(hidden.length).toBe(10);
    expect(hidden.every((d) => d.value === 0)).toBe(true);
    expect(view.extra?.maxQty).toBe(15);
    expect(dadosModule.meta.maxPlayers).toBe(6);
  });
});

describe('Ajedrez', () => {
  it('genera 20 movimientos legales y acepta e2-e4', () => {
    const match = new Match({ gameId: 'ajedrez', seats: seats(2), options: {}, difficulty: 'normal', seed: 19 });
    const view = match.view('p0');
    const moves = (view.extra?.legalMoves ?? []) as Array<{ from: number; to: number }>;
    expect(moves.length).toBe(20);
    expect(moves.some((m) => m.from === 52 && m.to === 36)).toBe(true);
    expect(match.act('p0', { type: 'move', from: 52, to: 36 }).ok).toBe(true);
    const reply = match.view('p1');
    expect(reply.turnId).toBe('p1');
    expect(reply.extra?.fullmove).toBe(1);
    expect(ajedrezModule.meta.maxPlayers).toBe(2);
  });
});

describe('Damas', () => {
  it('inicia con movimientos legales y cambia de turno', () => {
    const match = new Match({ gameId: 'damas', seats: seats(2), options: {}, difficulty: 'normal', seed: 23 });
    const view = match.view('p0');
    const moves = (view.extra?.legalMoves ?? []) as Array<{ from: number; to: number }>;
    expect(moves.length).toBeGreaterThan(0);
    expect(match.act('p0', { type: 'move', from: moves[0]!.from, to: moves[0]!.to }).ok).toBe(true);
    expect(match.view('p1').turnId).toBe('p1');
    expect(damasModule.meta.maxPlayers).toBe(2);
  });
});

describe('Mancala y Serpientes', () => {
  it('siembra semillas y avanza el turno o repite', () => {
    const match = new Match({ gameId: 'mancala', seats: seats(2), options: {}, difficulty: 'normal', seed: 29 });
    const view = match.view('p0');
    const movable = (view.extra?.movable ?? []) as number[];
    expect(movable.length).toBe(6);
    expect(match.act('p0', { type: 'sow', pit: movable[0]! }).ok).toBe(true);
    expect(match.view('p1').players.length).toBe(2);
    expect(mancalaModule.meta.maxPlayers).toBe(2);
  });

  it('lanza el dado y avanza la ficha', () => {
    const match = new Match({ gameId: 'serpientes', seats: seats(3), options: {}, difficulty: 'normal', seed: 31 });
    expect(match.act('p0', { type: 'roll' }).ok).toBe(true);
    const view = match.view('p0');
    expect(view.players[0]?.score).toBeGreaterThanOrEqual(1);
    expect(serpientesModule.meta.maxPlayers).toBe(4);
  });
});
