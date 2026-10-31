import { describe, expect, it } from 'vitest';
import type { CardView, Seat } from '../types';
import { makeRng } from '../rng';
import { Match } from '../match';
import { canWinHand, mahjongCardsModule, mahjongModule, riichiModule } from '../../games/mahjong';

const seats: Seat[] = Array.from({ length: 4 }, (_, index) => ({
  id: `p${index}`,
  name: `Jugador ${index + 1}`,
  avatar: 'jaguar',
  isBot: index > 0,
}));

function tile(suit: string, rank: number, id = `${suit}-${rank}-${Math.random()}`): CardView {
  return { id, suit, rank };
}

function hand(suit: string, ranks: number[]): CardView[] {
  return ranks.map((rank, index) => tile(suit, rank, `${suit}-${rank}-${index}`));
}

function totalTiles(state: ReturnType<typeof mahjongModule.create>): number {
  const handTiles = Object.values(state.players).reduce((total, player) => total + player.hand.length + player.flowers.length, 0);
  const meldTiles = Object.values(state.players).reduce((total, player) => total + player.melds.reduce((sum, meld) => sum + meld.tiles.length, 0), 0);
  return state.wall.length + handTiles + meldTiles;
}

describe('Mahjong web', () => {
  it('reparte 152 piezas en americano y 136 en Riichi sin perder ninguna', () => {
    const american = mahjongModule.create(seats, {}, makeRng(11));
    expect(totalTiles(american)).toBe(152);
    expect(american.players.p0?.hand.length).toBe(14);
    expect(american.players.p1?.hand.length).toBe(13);
    const riichi = riichiModule.create(seats, {}, makeRng(11));
    expect(totalTiles(riichi)).toBe(136);
    expect(Object.values(riichi.players).some((player) => player.flowers.length > 0)).toBe(false);
  });

  it('reconoce cuatro tríos y un par en americano', () => {
    const winning = [
      ...hand('dots', [1, 1, 1]),
      ...hand('dots', [2, 2, 2]),
      ...hand('bams', [3, 3, 3]),
      ...hand('cracks', [4, 4, 4]),
      ...hand('wind', [1, 1]),
    ];
    expect(winning).toHaveLength(14);
    expect(canWinHand(winning, [], 'american')).toBe(true);
    expect(canWinHand(winning, [], 'riichi')).toBe(true);
  });

  it('americano no acepta secuencias, Riichi sí', () => {
    const sequence = [
      ...hand('dots', [1, 2, 3]),
      ...hand('dots', [4, 5, 6]),
      ...hand('bams', [1, 2, 3]),
      ...hand('bams', [7, 7, 7]),
      ...hand('cracks', [9, 9]),
    ];
    expect(canWinHand(sequence, [], 'american')).toBe(false);
    expect(canWinHand(sequence, [], 'riichi')).toBe(true);
  });

  it('Riichi acepta siete pares', () => {
    const pairs = [
      ...hand('dots', [1, 1, 2, 2]),
      ...hand('bams', [3, 3, 4, 4]),
      ...hand('cracks', [5, 5, 6, 6]),
      ...hand('wind', [1, 1]),
    ];
    expect(canWinHand(pairs, [], 'riichi')).toBe(true);
  });

  it('el comodín completa un trío americano', () => {
    const wild = [
      ...hand('dots', [1, 1]),
      ...hand('dots', [2, 2, 2]),
      ...hand('bams', [3, 3, 3]),
      ...hand('cracks', [4, 4, 4]),
      ...hand('wind', [1, 1]),
      tile('joker', 1, 'joker-1'),
    ];
    expect(wild).toHaveLength(14);
    expect(canWinHand(wild, [], 'american')).toBe(true);
    expect(canWinHand(wild, [], 'riichi')).toBe(false);
  });

  it('descarta, avanza el turno y roba una pieza', () => {
    const match = new Match({ gameId: 'mahjong', seats, options: {}, difficulty: 'normal', seed: 21 });
    const before = match.view('p0');
    expect(before.gameId).toBe('mahjong');
    expect(before.turnId).toBe('p0');
    const first = before.hand[0];
    expect(first).toBeDefined();
    const result = match.act('p0', { type: 'discard', tileId: first?.id });
    expect(result.ok).toBe(true);
    const after = match.view('p0');
    expect(after.turnId === 'p0' || after.players.find((player) => player.id === 'p0')?.cardsLeft === 13).toBe(true);
    expect(Number(after.extra?.wallCount ?? 0)).toBeLessThan(Number(before.extra?.wallCount ?? 0));
  });

  it('el bot elige una pieza legal de su propia mano', () => {
    const state = riichiModule.create(seats, {}, makeRng(33));
    const action = riichiModule.bot({ ...state, turn: 1 }, 'p1', 'normal', makeRng(1));
    expect(action?.type === 'discard' || action?.type === 'tsumo' || action === null).toBe(true);
    if (action?.type === 'discard') {
      expect(state.players.p1?.hand.some((value) => value.id === action.tileId)).toBe(true);
    }
  });

  it('Mahjong con naipes usa el mismo motor y su propia identidad', () => {
    const match = new Match({ gameId: 'mahjong-cards', seats, options: {}, difficulty: 'normal', seed: 44 });
    expect(match.view('p0').gameId).toBe('mahjong-cards');
    expect(mahjongCardsModule.meta.category).toBe('cartas');
  });
});
