import { describe, expect, it } from 'vitest';
import { makeRng } from '../rng';
import { awardXp, levelForXp } from '../levels';
import { detectCanto } from '../../games/caida';
import { caidaModule } from '../../games/caida';
import { furiaModule } from '../../games/furia';
import { dominoModule } from '../../games/domino';
import { dadosModule } from '../../games/dados';
import { ajedrezModule } from '../../games/ajedrez';
import { damasModule } from '../../games/damas';
import { mancalaModule } from '../../games/mancala';
import { serpientesModule } from '../../games/serpientes';
import { goModule } from '../../games/go';
import { jungleModule } from '../../games/jungle';
import { ludoModule } from '../../games/ludo';
import { backgammonModule } from '../../games/backgammon';
import { xiangqiModule } from '../../games/xiangqi';
import { chinoModule } from '../../games/chino';
import { compareHands, evaluateHand, pokerDeck, pokerModule } from '../../games/poker';
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
    expect(detectCanto([card('oros', 7), card('copas', 7), card('espadas', 7)])?.points).toBe(3);
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

  it('no permite cantar después de jugar la primera carta', () => {
    const state = caidaModule.create(seats(2), {}, makeRng(5));
    state.phase = 'play';
    state.turn = 0;
    state.hands.p0 = [
      { id: 'oros-1', suit: 'oros', rank: 1 },
      { id: 'copas-1', suit: 'copas', rank: 1 },
      { id: 'espadas-1', suit: 'espadas', rank: 1 },
    ];
    state.playedThisHand.p0 = true;
    const result = caidaModule.act(state, { type: 'canto' }, 'p0', makeRng(5));
    expect(result.error).toBe('error.cantoAfterPlay');
  });

  it('termina la mano y espera el marcador antes de repartir', () => {
    const state = caidaModule.create(seats(2), {}, makeRng(9));
    state.phase = 'play';
    state.turn = 0;
    state.hands.p0 = [{ id: 'oros-1', suit: 'oros', rank: 1 }];
    state.hands.p1 = [];
    state.deck = [];
    const result = caidaModule.act(state, { type: 'play', cardId: 'oros-1' }, 'p0', makeRng(9));
    expect(result.error).toBeUndefined();
    expect(result.state.phase).toBe('handEnd');
    expect(result.state.lastHandSummary?.length).toBe(2);
    const next = caidaModule.act(result.state, { type: 'nextHand' }, 'p0', makeRng(9));
    expect(next.state.phase).toBe('entrega');
    expect(next.state.handNo).toBe(2);
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

describe('Go', () => {
  it('abre 81 intersecciones en 9×9 y termina con dos pases', () => {
    const match = new Match({ gameId: 'go', seats: seats(2), options: { boardSize: 9 }, difficulty: 'normal', seed: 37 });
    const view = match.view('p0');
    expect(view.center.board?.cols).toBe(9);
    expect(view.center.board?.rows).toBe(9);
    expect((view.extra?.legalMoves as number[]).length).toBe(81);
    expect(match.act('p0', { type: 'play', index: 40 }).ok).toBe(true);
    expect(match.view('p1').turnId).toBe('p1');
    expect(match.act('p1', { type: 'pass' }).ok).toBe(true);
    expect(match.act('p0', { type: 'pass' }).ok).toBe(true);
    expect(match.view('p0').phase).toBe('matchEnd');
    expect(match.view('p0').result?.scores).toBeTruthy();
    expect(goModule.meta.maxPlayers).toBe(2);
  });

  it('respeta el tablero de 13×13', () => {
    const match = new Match({ gameId: 'go', seats: seats(2), options: { boardSize: 13 }, difficulty: 'normal', seed: 41 });
    expect(match.view('p0').center.board?.cols).toBe(13);
    expect((match.view('p0').extra?.legalMoves as number[]).length).toBe(169);
  });
});

describe('Jungle', () => {
  it('inicia con 16 animales y acepta un movimiento legal', () => {
    const match = new Match({ gameId: 'jungle', seats: seats(2), options: {}, difficulty: 'normal', seed: 43 });
    const view = match.view('p0');
    const pieces = view.extra?.pieces as { r: number; b: number };
    expect(pieces.r).toBe(8);
    expect(pieces.b).toBe(8);
    const moves = (view.extra?.legalMoves ?? []) as Array<{ from: number; to: number }>;
    expect(moves.length).toBeGreaterThan(0);
    expect(match.act('p0', { type: 'move', from: moves[0]!.from, to: moves[0]!.to }).ok).toBe(true);
    expect(match.view('p1').turnId).toBe('p1');
    expect(jungleModule.meta.maxPlayers).toBe(2);
  });
});

describe('Ludo', () => {
  it('reparte 16 fichas y saca una ficha con un 6', () => {
    const six = () => 0.99;
    const state = ludoModule.create(seats(4), {}, six);
    const view = ludoModule.view(state, 'p0');
    expect((view.extra?.tokens as unknown[]).length).toBe(16);
    expect(view.extra?.colors).toEqual(['red', 'green', 'yellow', 'blue']);
    const rolled = ludoModule.act(state, { type: 'roll' }, 'p0', six);
    expect(rolled.error).toBeUndefined();
    expect(rolled.state.phase).toBe('move');
    expect(rolled.state.dice).toBe(6);
    const moved = ludoModule.act(rolled.state, { type: 'move', tokenId: 'red-0' }, 'p0', six);
    expect(moved.error).toBeUndefined();
    expect(moved.state.tokens.find((token) => token.id === 'red-0')?.progress).toBe(0);
    expect(moved.state.phase).toBe('roll');
    expect(moved.state.turn).toBe(0);
    expect(ludoModule.meta.maxPlayers).toBe(4);
  });

  it('en dos jugadores usa rojo y amarillo', () => {
    const state = ludoModule.create(seats(2), {}, () => 0.5);
    const view = ludoModule.view(state, 'p0');
    expect(view.extra?.colors).toEqual(['red', 'yellow']);
    expect((view.extra?.tokens as unknown[]).length).toBe(8);
  });

  it('captura una ficha rival fuera de las casillas seguras', () => {
    const state = ludoModule.create(seats(2), {}, () => 0.5);
    state.tokens = state.tokens.map((token) => {
      if (token.id === 'red-0') return { ...token, progress: 7 };
      if (token.id === 'yellow-0') return { ...token, progress: 36 };
      return token;
    });
    state.turn = 0;
    state.phase = 'move';
    state.dice = 3;
    const result = ludoModule.act(state, { type: 'move', tokenId: 'red-0' }, 'p0', () => 0.5);
    expect(result.error).toBeUndefined();
    expect(result.state.tokens.find((token) => token.id === 'yellow-0')?.progress).toBe(-1);
    expect(result.state.captures.p0).toBe(1);
    expect(result.state.turn).toBe(0);
    expect(result.state.phase).toBe('roll');
  });
});

describe('Xiangqi', () => {
  it('inicia con 32 piezas y acepta un avance de soldado', () => {
    const match = new Match({ gameId: 'xiangqi', seats: seats(2), options: {}, difficulty: 'normal', seed: 47 });
    const view = match.view('p0');
    expect(view.center.board?.cols).toBe(9);
    expect(view.center.board?.rows).toBe(10);
    expect((view.extra?.pieces as { r: number; b: number }).r).toBe(16);
    expect((view.extra?.pieces as { r: number; b: number }).b).toBe(16);
    const moves = (view.extra?.legalMoves ?? []) as Array<{ from: number; to: number }>;
    expect(moves.length).toBeGreaterThan(0);
    expect(match.act('p0', { type: 'move', from: 54, to: 45 }).ok).toBe(true);
    expect(match.view('p1').turnId).toBe('p1');
    expect(xiangqiModule.meta.maxPlayers).toBe(2);
  });
});

describe('Damas Chinas', () => {
  it('genera la estrella de 121 hoyos y mueve una ficha', () => {
    const state = chinoModule.create(seats(2), {}, () => 0.5);
    const view = chinoModule.view(state, 'p0');
    expect((view.extra?.cells as unknown[]).length).toBe(121);
    expect((view.extra?.pieces as Array<number | null>).filter((piece) => piece === 0).length).toBe(10);
    expect((view.extra?.pieces as Array<number | null>).filter((piece) => piece === 1).length).toBe(10);
    const moves = (view.extra?.legalMoves ?? []) as Array<{ from: number; to: number }>;
    expect(moves.length).toBeGreaterThan(0);
    const moved = chinoModule.act(state, { type: 'move', from: moves[0]!.from, to: moves[0]!.to }, 'p0', () => 0.5);
    expect(moved.error).toBeUndefined();
    expect(moved.state.turn).toBe(1);
    expect(chinoModule.meta.maxPlayers).toBe(6);
  });
});

describe('Backgammon', () => {
  it('inicia con 15 fichas por bando y mueve con dobles', () => {
    const four = () => 0.5;
    const state = backgammonModule.create(seats(2), {}, four);
    const view = backgammonModule.view(state, 'p0');
    const points = view.extra?.points as number[];
    expect(points[23]).toBe(2);
    expect(points[12]).toBe(5);
    expect(points[7]).toBe(3);
    expect(points[5]).toBe(5);
    expect(points.reduce((sum, value) => sum + Math.max(0, value), 0)).toBe(15);
    const rolled = backgammonModule.act(state, { type: 'roll' }, 'p0', four);
    expect(rolled.error).toBeUndefined();
    expect(rolled.state.movesLeft).toEqual([4, 4, 4, 4]);
    const after = backgammonModule.view(rolled.state, 'p0');
    const moves = (after.extra?.legalMoves ?? []) as Array<{ from: number; to: number }>;
    expect(moves.length).toBeGreaterThan(0);
    const moved = backgammonModule.act(rolled.state, { type: 'move', from: moves[0]!.from, to: moves[0]!.to }, 'p0', four);
    expect(moved.error).toBeUndefined();
    expect(moved.state.movesLeft.length).toBe(3);
    expect(backgammonModule.meta.maxPlayers).toBe(2);
  });

  it('manda una ficha sola a la barra', () => {
    const state = backgammonModule.create(seats(2), {}, () => 0.5);
    state.points = Array.from({ length: 24 }, () => 0);
    state.points[23] = 1;
    state.points[19] = -1;
    state.phase = 'move';
    state.turn = 'w';
    state.dice = [4, 4];
    state.movesLeft = [4];
    const result = backgammonModule.act(state, { type: 'move', from: 23, to: 19 }, 'p0', () => 0.5);
    expect(result.error).toBeUndefined();
    expect(result.state.bar.b).toBe(1);
    expect(result.state.points[19]).toBe(1);
  });

  it('saca la última ficha y cierra la partida', () => {
    const state = backgammonModule.create(seats(2), {}, () => 0.5);
    state.points = Array.from({ length: 24 }, () => 0);
    state.points[0] = 1;
    state.off = { w: 14, b: 0 };
    state.bar = { w: 0, b: 0 };
    state.phase = 'move';
    state.turn = 'w';
    state.dice = [1, 1];
    state.movesLeft = [1];
    const result = backgammonModule.act(state, { type: 'move', from: 0, to: 24 }, 'p0', () => 0.5);
    expect(result.error).toBeUndefined();
    expect(result.state.off.w).toBe(15);
    expect(result.state.phase).toBe('matchEnd');
    expect(result.state.result?.winnerIds).toEqual(['p0']);
  });
});

describe('Póker', () => {
  const card = (suit: string, rank: number) => ({ id: `${suit}-${rank}`, suit, rank });

  it('usa una baraja francesa de 52 cartas sin duplicados', () => {
    const deck = pokerDeck();
    expect(deck.length).toBe(52);
    expect(new Set(deck.map((c) => c.id)).size).toBe(52);
  });

  it('evalúa las categorías de mano y la escalera baja', () => {
    const royal = evaluateHand([
      card('hearts', 10), card('hearts', 11), card('hearts', 12), card('hearts', 13), card('hearts', 14),
    ]);
    expect(royal.category).toBe(8);
    expect(royal.ranks[0]).toBe(14);

    const wheel = evaluateHand([
      card('hearts', 14), card('spades', 2), card('clubs', 3), card('diamonds', 4), card('hearts', 5),
    ]);
    expect(wheel.category).toBe(4);
    expect(wheel.ranks).toEqual([5, 4, 3, 2, 1]);

    const quads = evaluateHand([
      card('hearts', 9), card('spades', 9), card('clubs', 9), card('diamonds', 9), card('hearts', 2),
    ]);
    expect(quads.category).toBe(7);

    const full = evaluateHand([
      card('hearts', 7), card('spades', 7), card('clubs', 7), card('diamonds', 3), card('hearts', 3),
    ]);
    expect(full.category).toBe(6);

    const twoPair = evaluateHand([
      card('hearts', 7), card('spades', 7), card('clubs', 3), card('diamonds', 3), card('hearts', 14),
    ]);
    expect(twoPair.category).toBe(2);
    expect(twoPair.ranks[0]).toBe(7);
    expect(compareHands(twoPair, full)).toBeLessThan(0);
  });

  it('reparte, iguala la ciega y avanza al flop', () => {
    const match = new Match({ gameId: 'poker', seats: seats(2), options: { target: 800 }, difficulty: 'normal', seed: 11 });
    const preflop = match.view('p0');
    expect(preflop.hand.length).toBe(2);
    expect(preflop.players.every((p) => p.cardsLeft === 2)).toBe(true);
    expect(preflop.players[0]?.score).toBe(790);
    expect(preflop.turnId).toBe('p0');
    const call = match.act('p0', { type: 'call' });
    expect(call.ok).toBe(true);
    const afterCall = match.view('p1');
    expect(afterCall.turnId).toBe('p1');
    const check = match.act('p1', { type: 'check' });
    expect(check.ok).toBe(true);
    const flop = match.view('p0');
    expect(flop.phase).toBe('flop');
    expect(flop.center.cards?.length).toBe(3);
    expect(flop.extra?.pot as number).toBe(40);
    expect(flop.turnId).toBe('p1');
  });

  it('paga botes laterales y conserva todas las fichas', () => {
    const match = new Match({ gameId: 'poker', seats: seats(3), options: { target: 200 }, difficulty: 'normal', seed: 19 });
    const state = match.state as ReturnType<typeof pokerModule.create>;
    state.players[1]!.chips = 90;
    expect(match.act('p0', { type: 'allin' }).ok).toBe(true);
    expect(match.act('p1', { type: 'call' }).ok).toBe(true);
    expect(match.act('p2', { type: 'fold' }).ok).toBe(true);
    const end = match.view('p0');
    const chips = end.players.reduce((total, player) => total + player.score, 0);
    expect(chips + Number(end.center.piles?.[0]?.count ?? 0)).toBe(500);
    expect(end.phase === 'handEnd' || end.phase === 'matchEnd').toBe(true);
    const showdown = end.extra?.showdown as { rows: unknown[]; pots: unknown[] } | null;
    expect(showdown?.rows.length).toBe(3);
    expect(showdown?.pots.length).toBe(2);
  });

  it('los bots juegan manos completas y la partida termina', () => {
    const bots: Seat[] = Array.from({ length: 4 }, (_, i) => ({
      id: `b${i}`,
      name: `Bot ${i + 1}`,
      avatar: 'jaguar',
      isBot: true,
    }));
    const match = new Match({ gameId: 'poker', seats: bots, options: { target: 200 }, difficulty: 'normal', seed: 23 });
    for (let step = 0; step < 4000 && !match.isOver; step += 1) {
      const view = match.view('b0');
      if (view.phase === 'handEnd') {
        match.act('b0', { type: 'nextHand' });
        continue;
      }
      if (!match.botStep()) {
        expect.fail(`La mesa se quedó sin acción en la fase ${view.phase}`);
      }
    }
    expect(match.isOver).toBe(true);
    const result = match.result;
    expect(result?.winnerIds.length).toBeGreaterThan(0);
    const total = Object.values(result?.scores ?? {}).reduce((sum, value) => sum + value, 0);
    expect(total).toBe(800);
  });
});
