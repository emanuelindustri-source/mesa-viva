import type {
  ActResult,
  Difficulty,
  GameAction,
  GameModule,
  GameView,
  LogEvent,
  MatchResult,
  Seat,
} from '../core/types';
import type { Rng } from '../core/types';
import { event, playerViews } from './cards';
import { makeAction, prompt } from './helpers';

type Side = 0 | 1 | 2;
type Phase = 'play' | 'matchEnd';
type PieceType = 'king' | 'queen' | 'rook' | 'bishop' | 'knight' | 'pawn';

interface Piece {
  id: string;
  side: Side;
  type: PieceType;
  cell: number;
}

interface Move {
  from: number;
  to: number;
  side: Side;
  piece: PieceType;
  captured: PieceType | null;
  promoted: boolean;
  check: boolean;
  eliminated: Side[];
}

export interface ThreeChessState {
  seats: Seat[];
  pieces: Piece[];
  turn: Side;
  phase: Phase;
  winner?: Side;
  eliminated: Side[];
  selected?: string;
  lastMove?: Move;
  history: LogEvent[];
  result?: MatchResult;
  rng: Rng;
}

const SIZE = 12;
const CELLS = SIZE * SIZE;
const BACK: PieceType[] = ['rook', 'knight', 'bishop', 'queen', 'king', 'bishop', 'knight', 'rook'];
const VALUES: Record<PieceType, number> = { king: 0, queen: 9, rook: 5, bishop: 3, knight: 3, pawn: 1 };
const GLYPHS: Record<PieceType, string> = { king: '♚', queen: '♛', rook: '♜', bishop: '♝', knight: '♞', pawn: '♟' };

function cell(row: number, col: number): number {
  return row * SIZE + col;
}

function row(index: number): number {
  return Math.floor(index / SIZE);
}

function col(index: number): number {
  return index % SIZE;
}

function inBoard(r: number, c: number): boolean {
  return r >= 0 && r < SIZE && c >= 0 && c < SIZE;
}

function nextActive(state: ThreeChessState, from: Side): Side {
  for (let step = 1; step <= 3; step += 1) {
    const candidate = ((from + step) % 3) as Side;
    if (!state.eliminated.includes(candidate)) return candidate;
  }
  return from;
}

function pieceAt(state: ThreeChessState, target: number): Piece | undefined {
  return state.pieces.find((piece) => piece.cell === target);
}

function kingOf(state: ThreeChessState, side: Side): Piece | undefined {
  return state.pieces.find((piece) => piece.side === side && piece.type === 'king');
}

function forward(side: Side): [number, number] {
  if (side === 0) return [1, 0];
  if (side === 1) return [0, 1];
  return [-1, 0];
}

function pseudoMoves(state: ThreeChessState, piece: Piece): number[] {
  const r = row(piece.cell);
  const c = col(piece.cell);
  const result = new Set<number>();
  const add = (tr: number, tc: number) => {
    if (!inBoard(tr, tc)) return;
    const occupant = pieceAt(state, cell(tr, tc));
    if (!occupant || occupant.side !== piece.side) result.add(cell(tr, tc));
  };
  const slide = (dr: number, dc: number) => {
    let tr = r + dr;
    let tc = c + dc;
    while (inBoard(tr, tc)) {
      const occupant = pieceAt(state, cell(tr, tc));
      if (!occupant) result.add(cell(tr, tc));
      else {
        if (occupant.side !== piece.side) result.add(cell(tr, tc));
        break;
      }
      tr += dr;
      tc += dc;
    }
  };
  if (piece.type === 'pawn') {
    const [dr, dc] = forward(piece.side);
    const oneRow = r + dr;
    const oneCol = c + dc;
    if (inBoard(oneRow, oneCol) && !pieceAt(state, cell(oneRow, oneCol))) {
      result.add(cell(oneRow, oneCol));
      const start = piece.side === 0 ? r === 1 : piece.side === 1 ? c === 1 : r === 10;
      const twoRow = r + dr * 2;
      const twoCol = c + dc * 2;
      if (start && inBoard(twoRow, twoCol) && !pieceAt(state, cell(twoRow, twoCol))) result.add(cell(twoRow, twoCol));
    }
    if (dr !== 0) {
      add(oneRow, oneCol - 1);
      add(oneRow, oneCol + 1);
    } else {
      add(oneRow - 1, oneCol);
      add(oneRow + 1, oneCol);
    }
  } else if (piece.type === 'knight') {
    const jumps: Array<[number, number]> = [[-2, -1], [-2, 1], [-1, -2], [-1, 2], [1, -2], [1, 2], [2, -1], [2, 1]];
    for (const [dr, dc] of jumps) add(r + dr, c + dc);
  } else if (piece.type === 'bishop') {
    slide(1, 1); slide(1, -1); slide(-1, 1); slide(-1, -1);
  } else if (piece.type === 'rook') {
    slide(1, 0); slide(-1, 0); slide(0, 1); slide(0, -1);
  } else if (piece.type === 'queen') {
    slide(1, 1); slide(1, -1); slide(-1, 1); slide(-1, -1);
    slide(1, 0); slide(-1, 0); slide(0, 1); slide(0, -1);
  } else {
    for (let dr = -1; dr <= 1; dr += 1) for (let dc = -1; dc <= 1; dc += 1) if (dr !== 0 || dc !== 0) add(r + dr, c + dc);
  }
  return [...result];
}

function attacks(state: ThreeChessState, piece: Piece, target: number): boolean {
  if (piece.type === 'pawn') {
    const [dr, dc] = forward(piece.side);
    const r = row(piece.cell) + dr;
    if (dr !== 0) return (inBoard(r, col(piece.cell) - 1) && cell(r, col(piece.cell) - 1) === target)
      || (inBoard(r, col(piece.cell) + 1) && cell(r, col(piece.cell) + 1) === target);
    return (inBoard(row(piece.cell) - 1, col(piece.cell) + dc) && cell(row(piece.cell) - 1, col(piece.cell) + dc) === target)
      || (inBoard(row(piece.cell) + 1, col(piece.cell) + dc) && cell(row(piece.cell) + 1, col(piece.cell) + dc) === target);
  }
  return pseudoMoves(state, piece).includes(target);
}

function isAttacked(state: ThreeChessState, target: number, by: Side): boolean {
  return state.pieces.some((piece) => piece.side === by && !state.eliminated.includes(by) && attacks(state, piece, target));
}

function inCheck(state: ThreeChessState, side: Side): boolean {
  const king = kingOf(state, side);
  if (!king) return false;
  return [0, 1, 2].some((other) => other !== side && !state.eliminated.includes(other as Side) && isAttacked(state, king.cell, other as Side));
}

function withMove(state: ThreeChessState, piece: Piece, target: number): ThreeChessState {
  const pieces = state.pieces
    .filter((value) => value.cell !== target || value.side === piece.side)
    .map((value) => (value.id === piece.id ? { ...value, cell: target } : value));
  const promoted = piece.type === 'pawn' && (
    (piece.side === 0 && row(target) === SIZE - 1)
    || (piece.side === 1 && col(target) === SIZE - 1)
    || (piece.side === 2 && row(target) === 0)
  );
  return {
    ...state,
    pieces: pieces.map((value) => (value.id === piece.id && promoted ? { ...value, type: 'queen' as PieceType } : value)),
  };
}

function legalMovesForSide(state: ThreeChessState, side: Side): Array<{ pieceId: string; target: number }> {
  return state.pieces
    .filter((piece) => piece.side === side && !state.eliminated.includes(side))
    .flatMap((piece) => pseudoMoves(state, piece)
      .filter((target) => {
        const next = withMove(state, piece, target);
        const king = kingOf(next, side);
        return Boolean(king && ![0, 1, 2].some((other) => other !== side && !next.eliminated.includes(other as Side) && isAttacked(next, king.cell, other as Side)));
      })
      .map((target) => ({ pieceId: piece.id, target })));
}

function applyMove(state: ThreeChessState, piece: Piece, target: number): ThreeChessState {
  const captured = pieceAt(state, target);
  const moved = withMove(state, piece, target);
  const promoted = piece.type === 'pawn' && moved.pieces.find((value) => value.id === piece.id)?.type === 'queen';
  const nextTurn = nextActive(moved, piece.side);
  let next: ThreeChessState = {
    ...moved,
    turn: nextTurn,
    selected: undefined,
    lastMove: {
      from: piece.cell,
      to: target,
      side: piece.side,
      piece: piece.type,
      captured: captured?.type ?? null,
      promoted,
      check: false,
      eliminated: [],
    },
    history: [...state.history, event('log.threeChessMove', {
      p: state.seats[piece.side]?.name ?? '—',
      from: `${String.fromCharCode(65 + col(piece.cell))}${SIZE - row(piece.cell)}`,
      to: `${String.fromCharCode(65 + col(target))}${SIZE - row(target)}`,
      captured: captured?.type ?? '',
    }, captured ? 'success' : 'info', state.seats[piece.side]?.id)],
  };
  const eliminated: Side[] = [];
  for (const side of [0, 1, 2] as Side[]) {
    if (next.eliminated.includes(side)) continue;
    const king = kingOf(next, side);
    if (!king || legalMovesForSide(next, side).length === 0) eliminated.push(side);
  }
  if (eliminated.length > 0) {
    next = {
      ...next,
      pieces: next.pieces.filter((value) => !eliminated.includes(value.side)),
      eliminated: [...next.eliminated, ...eliminated],
      history: [...next.history, event('log.threeChessOut', { sides: eliminated.map((side) => state.seats[side]?.name ?? '—').join(' · ') }, 'warn')],
      lastMove: next.lastMove ? { ...next.lastMove, eliminated } : undefined,
    };
  }
  const alive = [0, 1, 2].filter((side) => !next.eliminated.includes(side as Side)) as Side[];
  if (alive.length <= 1) {
    const winner = alive[0];
    const scores: Record<string, number> = {};
    for (const seat of state.seats) scores[seat.id] = material(next, state.seats.indexOf(seat) as Side);
    next = {
      ...next,
      phase: 'matchEnd',
      winner,
      turn: winner ?? next.turn,
      result: {
        winnerIds: winner === undefined ? [] : [state.seats[winner]?.id ?? ''],
        draw: winner === undefined,
        scores,
        reasonKey: 'result.reason.threeChess',
      },
      history: [...next.history, event('log.threeChessEnd', { p: winner === undefined ? '—' : state.seats[winner]?.name ?? '—' }, 'success')],
    };
  } else if (next.eliminated.includes(next.turn)) {
    next = { ...next, turn: nextActive(next, next.turn) };
  }
  if (next.phase === 'play' && [0, 1, 2].some((side) => inCheck(next, side as Side))) {
    next = next.lastMove ? { ...next, lastMove: { ...next.lastMove, check: true } } : next;
  }
  return next;
}

function material(state: ThreeChessState, side: Side): number {
  return state.pieces.filter((piece) => piece.side === side).reduce((total, piece) => total + VALUES[piece.type], 0);
}

export const threeChessModule: GameModule<ThreeChessState> = {
  meta: {
    id: 'three-chess',
    nameKey: 'game.threeChess.name',
    descKey: 'game.threeChess.desc',
    rulesKey: 'game.threeChess.rules',
    icon: '♞',
    category: 'tablero',
    minPlayers: 3,
    maxPlayers: 3,
    avgMinutes: 40,
    accent: '#7c6bd6',
    tagKeys: ['tag.estrategia', 'tag.cultural'],
  },

  create(seats, _options, rng): ThreeChessState {
    const pieces: Piece[] = [];
    let id = 0;
    for (let file = 0; file < 8; file += 1) {
      const type = BACK[file] ?? 'pawn';
      pieces.push({ id: `n${id++}`, side: 0, type, cell: cell(0, file + 2) });
      pieces.push({ id: `n${id++}`, side: 0, type: 'pawn', cell: cell(1, file + 2) });
    }
    for (let file = 0; file < 8; file += 1) {
      const type = BACK[file] ?? 'pawn';
      pieces.push({ id: `w${id++}`, side: 1, type, cell: cell(file + 2, 0) });
      pieces.push({ id: `w${id++}`, side: 1, type: 'pawn', cell: cell(file + 2, 1) });
    }
    for (let file = 0; file < 8; file += 1) {
      const type = BACK[file] ?? 'pawn';
      pieces.push({ id: `s${id++}`, side: 2, type, cell: cell(11, file + 2) });
      pieces.push({ id: `s${id++}`, side: 2, type: 'pawn', cell: cell(10, file + 2) });
    }
    return {
      seats: seats.slice(0, 3),
      pieces,
      turn: 0,
      phase: 'play',
      eliminated: [],
      history: [event('log.threeChessStart', {}, 'info')],
      rng,
    };
  },

  view(state, viewerId): GameView {
    const isTurn = state.seats[state.turn]?.id === viewerId && state.phase === 'play';
    const selected = state.selected;
    const legal = selected
      ? new Set(legalMovesForSide(state, state.turn).filter((move) => move.pieceId === selected).map((move) => move.target))
      : new Set<number>();
    const cells = Array.from({ length: CELLS }, (_, index) => {
      const piece = pieceAt(state, index);
      return {
        glyph: piece ? GLYPHS[piece.type] : undefined,
        side: piece ? piece.side === 0 ? 'w' as const : piece.side === 1 ? 'b' as const : 'r' as const : undefined,
        highlight: legal.has(index) ? 'move' as const : selected === piece?.id ? 'selected' as const : state.lastMove?.to === index ? 'last' as const : undefined,
        label: `${String.fromCharCode(65 + col(index))}${SIZE - row(index)}`,
      };
    });
    const scores = {
      [state.seats[0]?.id ?? 'p0']: material(state, 0),
      [state.seats[1]?.id ?? 'p1']: material(state, 1),
      [state.seats[2]?.id ?? 'p2']: material(state, 2),
    };
    return {
      gameId: 'three-chess',
      youId: viewerId,
      phase: state.phase,
      turnId: state.seats[state.turn]?.id ?? null,
      players: playerViews(state.seats, scores, state.seats[state.turn]?.id ?? null, viewerId, (seat) => {
        const side = state.seats.indexOf(seat) as Side;
        return { cardsLeft: state.pieces.filter((piece) => piece.side === side).length, active: !state.eliminated.includes(side) };
      }),
      hand: [],
      center: { board: { cols: SIZE, rows: SIZE, cells } },
      actions: state.phase === 'play'
        ? [makeAction('resign', 'action.resign', { type: 'resign' }, { tone: 'ghost' })]
        : [],
      prompt: state.phase === 'matchEnd'
        ? prompt('prompt.matchEnd', {}, 'success')
        : isTurn
          ? selected
            ? prompt('prompt.threeChessMove', {}, 'info')
            : prompt('prompt.threeChessSelect', {}, 'info')
          : prompt('prompt.waitingPlayer', { player: state.seats[state.turn]?.name ?? '—' }, 'info'),
      extra: {
        selected,
        legal: [...legal],
        eliminated: state.eliminated,
        scores,
      },
    };
  },

  act(state, action, actorId): ActResult<ThreeChessState> {
    if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
    if (state.seats[state.turn]?.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };
    const before = state.history.length;
    if (action.type === 'resign') {
      const side = state.turn;
      const eliminated = [...state.eliminated, side];
      const alive = ([0, 1, 2] as Side[]).filter((value) => !eliminated.includes(value));
      const winner = alive.length === 1 ? alive[0] : undefined;
      const scores: Record<string, number> = {};
      for (const seat of state.seats) scores[seat.id] = material(state, state.seats.indexOf(seat) as Side);
      const next: ThreeChessState = {
        ...state,
        pieces: state.pieces.filter((piece) => piece.side !== side),
        eliminated,
        phase: alive.length <= 1 ? 'matchEnd' : 'play',
        winner,
        turn: nextActive({ ...state, eliminated }, side),
        result: winner === undefined ? undefined : { winnerIds: [state.seats[winner]?.id ?? ''], draw: false, scores, reasonKey: 'result.reason.resign' },
        history: [...state.history, event('log.threeChessResign', { p: state.seats[side]?.name ?? '—' }, 'warn', actorId)],
      };
      return { state: next, events: next.history.slice(before) };
    }
    if (action.type === 'move') {
      const piece = state.pieces.find((value) => value.id === String(action.pieceId ?? ''));
      const target = Number(action.target);
      if (!piece || !Number.isInteger(target)) return { state, events: [], error: 'error.invalidAction' };
      if (!legalMovesForSide(state, state.turn).some((move) => move.pieceId === piece.id && move.target === target)) {
        return { state, events: [], error: 'error.invalidAction' };
      }
      const next = applyMove(state, piece, target);
      return { state: next, events: next.history.slice(before) };
    }
    if (action.type !== 'tap') return { state, events: [], error: 'error.invalidAction' };
    const target = Number(action.cell);
    if (!Number.isInteger(target) || target < 0 || target >= CELLS) return { state, events: [], error: 'error.invalidAction' };
    const piece = pieceAt(state, target);
    if (piece && piece.side === state.turn) return { state: { ...state, selected: piece.id }, events: [] };
    if (state.selected) {
      const selected = state.pieces.find((value) => value.id === state.selected);
      if (selected && legalMovesForSide(state, state.turn).some((move) => move.pieceId === selected.id && move.target === target)) {
        const next = applyMove(state, selected, target);
        return { state: next, events: next.history.slice(before) };
      }
    }
    return { state, events: [], error: 'error.invalidAction' };
  },

  bot(state, botId, _difficulty: Difficulty, rng): GameAction | null {
    if (state.phase === 'matchEnd') return null;
    if (state.seats[state.turn]?.id !== botId) return null;
    const moves = legalMovesForSide(state, state.turn);
    if (moves.length === 0) return null;
    let best = moves[0];
    let bestScore = -Infinity;
    for (const move of moves) {
      const piece = state.pieces.find((value) => value.id === move.pieceId);
      const captured = pieceAt(state, move.target);
      let score = captured ? VALUES[captured.type] * 10 : 0;
      if (piece?.type === 'pawn' && ((piece.side === 0 && row(move.target) === SIZE - 1) || (piece.side === 1 && col(move.target) === SIZE - 1) || (piece.side === 2 && row(move.target) === 0))) score += 80;
      score += rng() * 3;
      if (score > bestScore) {
        bestScore = score;
        best = move;
      }
    }
    return best ? { type: 'move', pieceId: best.pieceId, target: best.target } : null;
  },

  result(state): MatchResult | null {
    return state.result ?? null;
  },
};
