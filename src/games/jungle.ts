import type {
  ActResult,
  BoardCell,
  Difficulty,
  GameAction,
  GameModule,
  GameOptions,
  GameView,
  MatchResult,
  PromptView,
  Rng,
  Seat,
  UIAction,
} from '../core/types';
import { event, playerViews } from './cards';
import { makeAction } from './helpers';

type Side = 'r' | 'b';

interface Piece {
  side: Side;
  /** 1 rata, 2 gato, 3 perro, 4 lobo, 5 leopardo, 6 tigre, 7 león, 8 elefante. */
  rank: number;
}

interface Move {
  from: number;
  to: number;
  jump?: boolean;
}

interface JungleState {
  seats: Seat[];
  options: GameOptions;
  board: (Piece | null)[];
  turn: Side;
  phase: 'play' | 'matchEnd';
  lastMove?: { from: number; to: number; jump?: boolean };
  captures: Record<string, number>;
  moveNo: number;
  result?: MatchResult;
  rng: Rng;
}

const COLS = 7;
const ROWS = 9;
const idx = (col: number, row: number) => row * COLS + col;
const colOf = (i: number) => i % COLS;
const rowOf = (i: number) => Math.floor(i / COLS);
const inBounds = (col: number, row: number) => col >= 0 && col < COLS && row >= 0 && row < ROWS;
const other = (side: Side): Side => (side === 'r' ? 'b' : 'r');

const RED_DEN = idx(3, 8);
const BLACK_DEN = idx(3, 0);

/** La trampa pertenece al jugador cuya guarida está al lado. */
const TRAP_OWNER: Record<number, Side> = {
  [idx(2, 0)]: 'b',
  [idx(4, 0)]: 'b',
  [idx(3, 1)]: 'b',
  [idx(2, 8)]: 'r',
  [idx(4, 8)]: 'r',
  [idx(3, 7)]: 'r',
};

const WATER = new Set<number>();
for (let row = 3; row <= 5; row += 1) {
  for (const col of [1, 2, 4, 5]) WATER.add(idx(col, row));
}

const ANIMALS = ['', '🐭', '🐱', '🐶', '🐺', '🐆', '🐯', '🦁', '🐘'];
const RANKS = ['', 'rata', 'gato', 'perro', 'lobo', 'leopardo', 'tigre', 'león', 'elefante'];

function denFor(side: Side): number {
  return side === 'r' ? RED_DEN : BLACK_DEN;
}

function enemyDen(side: Side): number {
  return side === 'r' ? BLACK_DEN : RED_DEN;
}

function isWater(i: number): boolean {
  return WATER.has(i);
}

function isTrap(i: number): boolean {
  return TRAP_OWNER[i] !== undefined;
}

function inEnemyTrap(i: number, side: Side): boolean {
  const owner = TRAP_OWNER[i];
  return owner !== undefined && owner !== side;
}

function initialBoard(): (Piece | null)[] {
  const board: (Piece | null)[] = Array.from({ length: COLS * ROWS }, () => null);
  const layout: Array<[number, Side, number, number]> = [
    // Negro (arriba)
    [6, 'b', 0, 0], [7, 'b', 6, 0],
    [2, 'b', 1, 1], [3, 'b', 5, 1],
    [8, 'b', 0, 2], [4, 'b', 2, 2], [5, 'b', 4, 2], [1, 'b', 6, 2],
    // Rojo (abajo)
    [1, 'r', 0, 6], [5, 'r', 2, 6], [4, 'r', 4, 6], [8, 'r', 6, 6],
    [3, 'r', 1, 7], [2, 'r', 5, 7],
    [7, 'r', 0, 8], [6, 'r', 6, 8],
  ];
  for (const [rank, side, col, row] of layout) {
    board[idx(col, row)] = { side, rank };
  }
  return board;
}

function canCapture(attacker: Piece, defender: Piece, from: number, to: number): boolean {
  // Una rata en el agua no puede capturar en tierra.
  if (isWater(from) && !isWater(to)) return false;
  // En una trampa del atacante, cualquier ficha defensora cae.
  if (TRAP_OWNER[to] === attacker.side) return true;
  // La rata come al elefante; el elefante no puede comer a la rata.
  if (attacker.rank === 1 && defender.rank === 8) return true;
  if (attacker.rank === 8 && defender.rank === 1) return false;
  return attacker.rank >= defender.rank;
}

function legalMoves(state: JungleState, side: Side): Move[] {
  const moves: Move[] = [];
  const board = state.board;
  const dirs: Array<[number, number]> = [[0, 1], [0, -1], [1, 0], [-1, 0]];
  for (let from = 0; from < board.length; from += 1) {
    const piece = board[from];
    if (!piece || piece.side !== side) continue;
    const col = colOf(from);
    const row = rowOf(from);
    for (const [dc, dr] of dirs) {
      const nc = col + dc;
      const nr = row + dr;
      if (!inBounds(nc, nr)) continue;
      const to = idx(nc, nr);
      if (to === denFor(side)) continue;
      if (isWater(to) && piece.rank !== 1) continue;
      const target = board[to];
      if (target && target.side === side) continue;
      if (target && !canCapture(piece, target, from, to)) continue;
      moves.push({ from, to });
    }

    // Tigre y león saltan el río si no hay ratas en el camino.
    if (piece.rank === 6 || piece.rank === 7) {
      for (const [dc, dr] of dirs) {
        let nc = col + dc;
        let nr = row + dr;
        if (!inBounds(nc, nr) || !isWater(idx(nc, nr))) continue;
        const path: number[] = [];
        while (inBounds(nc, nr) && isWater(idx(nc, nr))) {
          path.push(idx(nc, nr));
          nc += dc;
          nr += dr;
        }
        if (!inBounds(nc, nr) || path.length === 0) continue;
        const to = idx(nc, nr);
        if (to === denFor(side)) continue;
        const target = board[to];
        if (target && target.side === side) continue;
        if (path.some((i) => board[i]?.rank === 1)) continue;
        if (target && !canCapture(piece, target, from, to)) continue;
        moves.push({ from, to, jump: true });
      }
    }
  }
  return moves;
}

function pieceCount(board: (Piece | null)[], side: Side): number {
  return board.reduce((total, piece) => (piece?.side === side ? total + 1 : total), 0);
}

function material(board: (Piece | null)[], side: Side): number {
  return board.reduce((total, piece) => (piece?.side === side ? total + piece.rank : total), 0);
}

function resultFor(state: JungleState, winner: Side, reasonKey: string): MatchResult {
  const winnerSeat = state.seats[winner === 'r' ? 0 : 1];
  return {
    winnerIds: winnerSeat ? [winnerSeat.id] : [],
    scores: {
      [state.seats[0]?.id ?? 'r']: material(state.board, 'r'),
      [state.seats[1]?.id ?? 'b']: material(state.board, 'b'),
    },
    reasonKey,
    stats: {
      redPieces: pieceCount(state.board, 'r'),
      blackPieces: pieceCount(state.board, 'b'),
    },
  };
}

function applyMove(state: JungleState, move: Move): JungleState {
  const piece = state.board[move.from];
  if (!piece) return state;
  const board = state.board.slice();
  const captured = board[move.to];
  board[move.to] = piece;
  board[move.from] = null;
  const mover = piece.side;
  const rival = other(mover);
  const seat = state.seats[mover === 'r' ? 0 : 1];
  const captures = {
    ...state.captures,
    ...(captured && seat ? { [seat.id]: (state.captures[seat.id] ?? 0) + 1 } : {}),
  };
  const next: JungleState = {
    ...state,
    board,
    captures,
    turn: rival,
    lastMove: { from: move.from, to: move.to, ...(move.jump ? { jump: true } : {}) },
    moveNo: state.moveNo + 1,
  };
  if (move.to === enemyDen(mover)) {
    return { ...next, phase: 'matchEnd', result: resultFor(next, mover, 'result.reason.jungleDen') };
  }
  if (pieceCount(board, rival) === 0) {
    return { ...next, phase: 'matchEnd', result: resultFor(next, mover, 'result.reason.junglePieces') };
  }
  if (legalMoves(next, rival).length === 0) {
    return { ...next, phase: 'matchEnd', result: resultFor(next, mover, 'result.reason.jungleBlocked') };
  }
  return next;
}

function seatFor(state: JungleState, side: Side): Seat | undefined {
  return state.seats[side === 'r' ? 0 : 1];
}

function coord(i: number): string {
  return `${String.fromCharCode(65 + colOf(i))}${ROWS - rowOf(i)}`;
}

const RANK_VALUE = [0, 2, 3, 4, 6, 8, 10, 12, 16];

function evaluate(state: JungleState, side: Side): number {
  const rival = other(side);
  let score = (material(state.board, side) - material(state.board, rival)) * 10;
  const target = enemyDen(side);
  const tc = colOf(target);
  const tr = rowOf(target);
  for (let i = 0; i < state.board.length; i += 1) {
    const piece = state.board[i];
    if (!piece) continue;
    const dist = Math.abs(colOf(i) - tc) + Math.abs(rowOf(i) - tr);
    const advance = (14 - dist) * (piece.rank === 1 ? 0.6 : 0.35);
    score += piece.side === side ? advance : -advance;
    if (inEnemyTrap(i, piece.side)) {
      const value = RANK_VALUE[piece.rank] ?? 0;
      score += piece.side === side ? -value * 2 : value * 2;
    }
  }
  return score;
}

function search(state: JungleState, depth: number, alpha: number, beta: number, root: Side): number {
  if (state.phase === 'matchEnd') {
    const winner = state.result?.winnerIds[0];
    const winnerSide = winner && winner === state.seats[0]?.id ? 'r' : 'b';
    return winnerSide === root ? 100000 + depth : -100000 - depth;
  }
  if (depth <= 0) return evaluate(state, root);
  const side = state.turn;
  const moves = legalMoves(state, side);
  if (moves.length === 0) return side === root ? -100000 : 100000;
  const maximizing = side === root;
  let best = maximizing ? -Infinity : Infinity;
  for (const move of moves) {
    const value = search(applyMove(state, move), depth - 1, alpha, beta, root);
    if (maximizing) {
      if (value > best) best = value;
      if (best > alpha) alpha = best;
    } else {
      if (value < best) best = value;
      if (best < beta) beta = best;
    }
    if (alpha >= beta) break;
  }
  return best;
}

export const jungleModule: GameModule<JungleState> = {
  meta: {
    id: 'jungle',
    nameKey: 'game.jungle.name',
    descKey: 'game.jungle.desc',
    rulesKey: 'game.jungle.rules',
    icon: '🐯',
    category: 'tablero',
    minPlayers: 2,
    maxPlayers: 2,
    avgMinutes: 15,
    accent: '#2f7d5b',
    tagKeys: ['tag.estrategia', 'tag.ancestral', 'tag.duelo'],
  },

  create(seats, options, rng) {
    return {
      seats: seats.slice(0, 2).map((seat) => ({ ...seat })),
      options,
      board: initialBoard(),
      turn: 'r',
      phase: 'play',
      captures: Object.fromEntries(seats.slice(0, 2).map((seat) => [seat.id, 0])),
      moveNo: 0,
      rng,
    };
  },

  view(state, viewerId): GameView {
    const turnSeat = seatFor(state, state.turn);
    const isTurn = turnSeat?.id === viewerId;
    const moves = isTurn && state.phase === 'play' ? legalMoves(state, state.turn) : [];
    const destinations = new Map<number, boolean>();
    for (const move of moves) destinations.set(move.to, Boolean(state.board[move.to]));
    const sources = new Set(moves.map((move) => move.from));
    const cells: BoardCell[] = state.board.map((piece, i) => {
      const row = rowOf(i);
      const col = colOf(i);
      const den = i === RED_DEN ? 'r' : i === BLACK_DEN ? 'b' : null;
      const trap = isTrap(i);
      const water = isWater(i);
      const dark = (row + col) % 2 === 0;
      const terrain = den === 'r' ? 'den-r' : den === 'b' ? 'den-b' : water ? 'water' : trap ? 'trap' : dark ? 'jungle' : 'jungle-alt';
      let highlight: BoardCell['highlight'];
      if (destinations.has(i)) highlight = destinations.get(i) ? 'capture' : 'move';
      if (sources.has(i)) highlight = 'selected';
      if (state.lastMove?.to === i) highlight = 'last';
      return {
        glyph: piece ? ANIMALS[piece.rank] : den ? '★' : water ? '≈' : undefined,
        tone: terrain,
        ...(piece ? { side: piece.side } : {}),
        ...(highlight ? { highlight } : {}),
        ...(trap && !piece ? { sub: '▲' } : {}),
        label: `${String.fromCharCode(65 + col)}${ROWS - row}`,
        pieceId: piece ? `${piece.side}${piece.rank}` : undefined,
      };
    });
    const actions: UIAction[] = [];
    if (state.phase === 'play') {
      actions.push(makeAction('resign', 'action.resign', { type: 'resign' }, { tone: 'danger', icon: 'flag' }));
    }
    const prompt: PromptView = state.phase === 'matchEnd'
      ? { key: 'prompt.matchEnd', tone: 'success' }
      : isTurn
        ? { key: 'prompt.jungleYourTurn', tone: 'info' }
        : { key: 'prompt.waitingPlayer', params: { player: turnSeat?.name ?? '—' }, tone: 'info' };
    return {
      gameId: 'jungle',
      youId: viewerId,
      phase: state.phase,
      turnId: turnSeat?.id ?? null,
      players: playerViews(
        state.seats,
        state.captures,
        turnSeat?.id ?? null,
        viewerId,
        () => ({ active: true }),
      ),
      hand: [],
      center: { board: { cols: COLS, rows: ROWS, cells, orientation: 'white' } },
      actions,
      prompt,
      extra: {
        legalMoves: moves.map((move) => ({ from: move.from, to: move.to, capture: Boolean(state.board[move.to]) })),
        lastMove: state.lastMove ?? null,
        pieces: { r: pieceCount(state.board, 'r'), b: pieceCount(state.board, 'b') },
        captures: state.captures,
        moveNo: state.moveNo,
        side: state.turn,
      },
    };
  },

  act(state, action, actorId, _rng): ActResult<JungleState> {
    if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
    const turnSeat = seatFor(state, state.turn);
    if (!turnSeat || turnSeat.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };
    if (action.type === 'resign') {
      const rival = other(state.turn);
      const winner = seatFor(state, rival);
      if (!winner) return { state, events: [], error: 'error.invalidAction' };
      const finished: JungleState = {
        ...state,
        phase: 'matchEnd',
        result: resultFor(state, rival, 'result.reason.resign'),
      };
      return {
        state: finished,
        events: [event('log.resign', { p: turnSeat.name }, 'danger', actorId)],
      };
    }
    if (action.type !== 'move') return { state, events: [], error: 'error.invalidAction' };
    const from = Number(action.from);
    const to = Number(action.to);
    const move = legalMoves(state, state.turn).find((candidate) => candidate.from === from && candidate.to === to);
    if (!move) return { state, events: [], error: 'error.invalidMove' };
    const next = applyMove(state, move);
    const events = [
      event('log.jungleMove', { p: turnSeat.name, from: coord(from), to: coord(to) }, 'info', actorId),
    ];
    if (state.board[to]) events.push(event('log.jungleCapture', { p: turnSeat.name, n: RANKS[state.board[to]?.rank ?? 0] }, 'success', actorId));
    if (move.jump) events.push(event('log.jungleJump', { p: turnSeat.name }, 'magic', actorId));
    if (next.phase === 'matchEnd') {
      const won = next.result?.winnerIds[0] === turnSeat.id;
      events.push(event(won ? 'log.won' : 'log.draw', { p: turnSeat.name }, won ? 'magic' : 'info'));
    }
    return { state: next, events };
  },

  bot(state, botId, difficulty: Difficulty, rng: Rng): GameAction | null {
    if (state.phase === 'matchEnd') return null;
    const turnSeat = seatFor(state, state.turn);
    if (!turnSeat || turnSeat.id !== botId) return null;
    const moves = legalMoves(state, state.turn);
    if (moves.length === 0) return null;
    if (difficulty === 'easy' && moves.length > 1) {
      const captures = moves.filter((move) => state.board[move.to]);
      const pool = captures.length > 0 ? captures : moves;
      const pick = pool[Math.floor(rng() * pool.length)] ?? moves[0];
      return pick ? { type: 'move', from: pick.from, to: pick.to } : null;
    }
    const depth = difficulty === 'hard' ? 4 : 2;
    let best = -Infinity;
    let pool: Move[] = [];
    for (const move of moves) {
      const value = search(applyMove(state, move), depth - 1, -Infinity, Infinity, state.turn);
      if (value > best + 2) {
        best = value;
        pool = [move];
      } else if (Math.abs(value - best) <= 2) {
        pool.push(move);
      }
    }
    const chosen = pool[Math.floor(rng() * pool.length)] ?? moves[0];
    return chosen ? { type: 'move', from: chosen.from, to: chosen.to } : null;
  },

  result(state) {
    return state.result ?? null;
  },
};

export type { JungleState, Move as JungleMove, Side as JungleSide };
