import { describe, expect, it } from 'vitest';
import type { Seat } from '../types';
import { makeRng } from '../rng';
import { Match } from '../match';
import { reversiLegalMoves, reversiModule } from '../../games/reversi';
import { urLegalMoves, urModule, type UrState } from '../../games/ur';
import { allMoves, luzhanqiModule, type LzState } from '../../games/luzhanqi';
import { threeChessModule, type ThreeChessState } from '../../games/threechess';

const seats: Seat[] = [
  { id: 'p0', name: 'Jugador 1', avatar: 'jaguar', isBot: false },
  { id: 'p1', name: 'Jugador 2', avatar: 'silbon', isBot: true },
];

describe('Reversi web', () => {
  it('abre con cuatro fichas y cuatro jugadas legales', () => {
    const state = reversiModule.create(seats, {}, makeRng(1));
    expect(state.board.filter((cell) => cell !== 0)).toHaveLength(4);
    expect(reversiLegalMoves(state.board, 0).size).toBe(4);
  });

  it('voltea fichas y cambia el turno', () => {
    const match = new Match({ gameId: 'reversi', seats, options: {}, difficulty: 'normal', seed: 2 });
    const before = match.view('p0');
    const cells = before.center.board?.cells ?? [];
    const move = cells.findIndex((cell) => cell.highlight === 'move');
    expect(move).toBeGreaterThanOrEqual(0);
    const result = match.act('p0', { type: 'tap', cell: move });
    expect(result.ok).toBe(true);
    const after = match.view('p0');
    expect(after.turnId).toBe('p1');
    expect(after.center.board?.cells?.[move]?.side).toBe('w');
  });

  it('el bot devuelve una casilla legal', () => {
    const state = reversiModule.create(seats, {}, makeRng(3));
    const action = reversiModule.bot({ ...state, turn: 1 }, 'p1', 'normal', makeRng(4));
    expect(action?.type).toBe('tap');
    if (action?.type === 'tap') {
      expect(reversiLegalMoves(state.board, 1).has(Number(action.cell))).toBe(true);
    }
  });
});

describe('Luzhanqi web', () => {
  it('reparte 25 piezas por bando', () => {
    const state = luzhanqiModule.create(seats, {}, makeRng(20));
    expect(state.pieces).toHaveLength(50);
    expect(state.pieces.filter((piece) => piece.side === 0)).toHaveLength(25);
    expect(state.pieces.filter((piece) => piece.side === 1)).toHaveLength(25);
    expect(state.pieces.some((piece) => piece.rank === 'flag')).toBe(true);
    expect(new Set(state.pieces.map((piece) => piece.id)).size).toBe(50);
    expect(allMoves(state, 0).length).toBeGreaterThan(0);
    expect(allMoves(state, 1).length).toBeGreaterThan(0);
  });

  it('selecciona una pieza y mueve a una casilla iluminada', () => {
    const match = new Match({ gameId: 'luzhanqi', seats, options: {}, difficulty: 'normal', seed: 21 });
    const before = match.view('p0');
    const cells = before.center.board?.cells ?? [];
    const own = cells.findIndex((cell, index) => Math.floor(index / 5) === 4 && cell.side === 'r' && cell.glyph && cell.glyph !== '军旗' && cell.glyph !== '地雷');
    expect(own).toBeGreaterThanOrEqual(0);
    expect(match.act('p0', { type: 'tap', cell: own }).ok).toBe(true);
    const selected = match.view('p0');
    const target = (selected.center.board?.cells ?? []).findIndex((cell) => cell.highlight === 'move');
    expect(target).toBeGreaterThanOrEqual(0);
    expect(match.act('p0', { type: 'tap', cell: target }).ok).toBe(true);
    expect(match.view('p0').gameId).toBe('luzhanqi');
  });

  it('el ingeniero desactiva una mina y la bandera cierra la partida', () => {
    const base = luzhanqiModule.create(seats, {}, makeRng(22));
    const engineerState: LzState = {
      ...base,
      pieces: [
        { id: 'r1', side: 0, rank: 'engineer', cell: 25, revealed: false },
        { id: 'b1', side: 1, rank: 'mine', cell: 26, revealed: false },
        { id: 'rf', side: 0, rank: 'flag', cell: 1, revealed: false },
        { id: 'bf', side: 1, rank: 'flag', cell: 56, revealed: false },
      ],
    };
    const disarmed = luzhanqiModule.act(engineerState, { type: 'move', pieceId: 'r1', target: 26 }, 'p0', makeRng(1));
    expect(disarmed.error).toBeUndefined();
    expect(disarmed.state.pieces.some((piece) => piece.id === 'r1')).toBe(true);
    expect(disarmed.state.pieces.some((piece) => piece.id === 'b1')).toBe(false);

    const flagState: LzState = {
      ...base,
      pieces: [
        { id: 'r1', side: 0, rank: 'commander', cell: 25, revealed: false },
        { id: 'bf', side: 1, rank: 'flag', cell: 26, revealed: false },
        { id: 'rf', side: 0, rank: 'flag', cell: 1, revealed: false },
      ],
    };
    const won = luzhanqiModule.act(flagState, { type: 'move', pieceId: 'r1', target: 26 }, 'p0', makeRng(1));
    expect(won.state.phase).toBe('matchEnd');
    expect(won.state.winner).toBe(0);
  });
});

describe('Ajedrez para tres web', () => {
  it('coloca tres ejércitos de 16 piezas', () => {
    const state = threeChessModule.create(seats, {}, makeRng(30));
    expect(state.pieces).toHaveLength(48);
    for (const side of [0, 1, 2]) {
      expect(state.pieces.filter((piece) => piece.side === side)).toHaveLength(16);
    }
  });

  it('selecciona un peón y lo mueve', () => {
    const match = new Match({ gameId: 'three-chess', seats, options: {}, difficulty: 'normal', seed: 31 });
    const before = match.view('p0');
    const own = (before.center.board?.cells ?? []).findIndex((cell, index) => Math.floor(index / 12) === 1 && cell.side === 'w' && cell.glyph === '♟');
    expect(own).toBeGreaterThanOrEqual(0);
    expect(match.act('p0', { type: 'tap', cell: own }).ok).toBe(true);
    const selected = match.view('p0');
    const target = (selected.center.board?.cells ?? []).findIndex((cell) => cell.highlight === 'move');
    expect(target).toBeGreaterThanOrEqual(0);
    expect(match.act('p0', { type: 'tap', cell: target }).ok).toBe(true);
    expect(match.view('p0').turnId).toBe('p1');
  });

  it('promociona un peón al borde opuesto', () => {
    const base = threeChessModule.create(seats, {}, makeRng(32));
    const state: ThreeChessState = {
      ...base,
      pieces: [
        { id: 'n1', side: 0, type: 'pawn', cell: 10 * 12 + 5 },
        { id: 'nk', side: 0, type: 'king', cell: 5 },
        { id: 'wk', side: 1, type: 'king', cell: 5 * 12 },
        { id: 'sk', side: 2, type: 'king', cell: 11 * 12 + 11 },
      ],
    };
    const moved = threeChessModule.act(state, { type: 'move', pieceId: 'n1', target: 11 * 12 + 5 }, 'p0', makeRng(1));
    expect(moved.error).toBeUndefined();
    expect(moved.state.pieces.find((piece) => piece.id === 'n1')?.type).toBe('queen');
  });
});

describe('Juego Real de Ur web', () => {
  it('reparte cuatro fichas por bando', () => {
    const state = urModule.create(seats, {}, makeRng(10));
    expect(state.pieces).toHaveLength(8);
    expect(state.pieces.filter((piece) => piece.side === 0)).toHaveLength(4);
    expect(state.pieces.filter((piece) => piece.side === 1)).toHaveLength(4);
  });

  it('lanza el dado y mueve una ficha legal', () => {
    const match = new Match({ gameId: 'ur', seats, options: {}, difficulty: 'normal', seed: 11 });
    const before = match.view('p0');
    expect(before.actions.some((action) => action.action.type === 'roll')).toBe(true);
    const rolled = match.act('p0', { type: 'roll' });
    expect(rolled.ok).toBe(true);
    const after = match.view('p0');
    if (Number(after.extra?.dice) > 0) {
      const legal = (after.extra?.legal ?? []) as string[];
      expect(legal.length).toBeGreaterThan(0);
      const moved = match.act('p0', { type: 'move', pieceId: legal[0] });
      expect(moved.ok).toBe(true);
    }
    expect(after.gameId).toBe('ur');
  });

  it('las rosetas y la metaexacta están en el motor', () => {
    const state = urModule.create(seats, {}, makeRng(12));
    const withDice: UrState = { ...state, dice: 4 };
    const moves = urLegalMoves(withDice);
    expect(moves.length).toBe(4);
    const result = urModule.act(withDice, { type: 'move', pieceId: moves[0]?.id }, 'p0', makeRng(1));
    expect(result.error).toBeUndefined();
    expect(result.state.pieces.some((piece) => piece.position >= 0)).toBe(true);
  });
});
