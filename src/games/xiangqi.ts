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
type Kind = 'K' | 'A' | 'E' | 'H' | 'R' | 'C' | 'S';

interface Piece {
  side: Side;
  kind: Kind;
}

interface XqState {
  seats: Seat[];
  options: GameOptions;
  board: (Piece | null)[];
  turn: Side;
  phase: 'play' | 'matchEnd';
  lastMove?: { from: number; to: number };
  check?: Side;
  result?: MatchResult;
  rng: Rng;
}

interface Move {
  from: number;
  to: number;
  captured?: Kind;
}

const COLS = 9;
const ROWS = 10;
const idx = (col: number, row: number): number => row * COLS + col;
const colOf = (i: number): number => i % COLS;
const rowOf = (i: number): number => Math.floor(i / COLS);
const inBoard = (col: number, row: number): boolean => col >= 0 && col < COLS && row >= 0 && row < ROWS;
const other = (side: Side): Side => (side === 'r' ? 'b' : 'r');

const GLYPHS: Record<Side, Record<Kind, string>> = {
  r: { K: '帅', A: '仕', E: '相', H: '马', R: '车', C: '炮', S: '兵' },
  b: { K: '将', A: '士', E: '象', H: '马', R: '车', C: '炮', S: '卒' },
};

const VALUES: Record<Kind, number> = { K: 10000, R: 900, C: 450, H: 400, E: 200, A: 200, S: 100 };

function initialBoard(): (Piece | null)[] {
  const board: (Piece | null)[] = Array.from({ length: 90 }, () => null);
  const back: Kind[] = ['R', 'H', 'E', 'A', 'K', 'A', 'E', 'H', 'R'];
  back.forEach((kind, col) => {
    board[idx(col, 0)] = { side: 'b', kind };
    board[idx(col, 9)] = { side: 'r', kind };
  });
  board[idx(1, 2)] = { side: 'b', kind: 'C' };
  board[idx(7, 2)] = { side: 'b', kind: 'C' };
  board[idx(1, 7)] = { side: 'r', kind: 'C' };
  board[idx(7, 7)] = { side: 'r', kind: 'C' };
  for (const col of [0, 2, 4, 6, 8]) {
    board[idx(col, 3)] = { side: 'b', kind: 'S' };
    board[idx(col, 6)] = { side: 'r', kind: 'S' };
  }
  return board;
}

function inPalace(index: number, side: Side): boolean {
  const col = colOf(index);
  const row = rowOf(index);
  if (col < 3 || col > 5) return false;
  return side === 'r' ? row >= 7 && row <= 9 : row >= 0 && row <= 2;
}

function crossedRiver(index: number, side: Side): boolean {
  return side === 'r' ? rowOf(index) <= 4 : rowOf(index) >= 5;
}

function ownSide(index: number, side: Side): boolean {
  return side === 'r' ? rowOf(index) >= 5 : rowOf(index) <= 4;
}

function addTarget(moves: Move[], board: (Piece | null)[], from: number, to: number, side: Side): boolean {
  const target = board[to];
  if (target && target.side === side) return false;
  moves.push({ from, to, ...(target ? { captured: target.kind } : {}) });
  return !target;
}

function pseudoMoves(state: XqState, from: number): Move[] {
  const piece = state.board[from];
  if (!piece) return [];
  const moves: Move[] = [];
  const col = colOf(from);
  const row = rowOf(from);
  const side = piece.side;

  const step = (dc: number, dr: number, limit = 1): void => {
    for (let distance = 1; distance <= limit; distance += 1) {
      const nc = col + dc * distance;
      const nr = row + dr * distance;
      if (!inBoard(nc, nr)) return;
      const to = idx(nc, nr);
      const target = state.board[to];
      if (!target) {
        moves.push({ from, to });
        continue;
      }
      if (target.side !== side) moves.push({ from, to, captured: target.kind });
      return;
    }
  };

  switch (piece.kind) {
    case 'K': {
      for (const [dc, dr] of [[1, 0], [-1, 0], [0, 1], [0, -1]] as const) {
        const nc = col + dc;
        const nr = row + dr;
        if (!inBoard(nc, nr)) continue;
        const to = idx(nc, nr);
        if (!inPalace(to, side)) continue;
        addTarget(moves, state.board, from, to, side);
      }
      // Regla del general volador: puede capturar al general rival si no hay piezas entre ambos.
      const rival = state.board.findIndex((p) => p?.kind === 'K' && p.side !== side);
      if (rival >= 0 && colOf(rival) === col) {
        const min = Math.min(row, rowOf(rival));
        const max = Math.max(row, rowOf(rival));
        let clear = true;
        for (let r = min + 1; r < max; r += 1) {
          if (state.board[idx(col, r)]) {
            clear = false;
            break;
          }
        }
        if (clear) moves.push({ from, to: rival, captured: 'K' });
      }
      break;
    }
    case 'A': {
      for (const [dc, dr] of [[1, 1], [1, -1], [-1, 1], [-1, -1]] as const) {
        const nc = col + dc;
        const nr = row + dr;
        if (!inBoard(nc, nr)) continue;
        const to = idx(nc, nr);
        if (inPalace(to, side)) addTarget(moves, state.board, from, to, side);
      }
      break;
    }
    case 'E': {
      for (const [dc, dr] of [[2, 2], [2, -2], [-2, 2], [-2, -2]] as const) {
        const nc = col + dc;
        const nr = row + dr;
        if (!inBoard(nc, nr)) continue;
        const to = idx(nc, nr);
        if (!ownSide(to, side)) continue;
        const eye = idx(col + dc / 2, row + dr / 2);
        if (state.board[eye]) continue;
        addTarget(moves, state.board, from, to, side);
      }
      break;
    }
    case 'H': {
      const legs: Array<[number, number, number, number]> = [
        [1, 0, 2, 1], [1, 0, 2, -1],
        [-1, 0, -2, 1], [-1, 0, -2, -1],
        [0, 1, 1, 2], [0, 1, -1, 2],
        [0, -1, 1, -2], [0, -1, -1, -2],
      ];
      for (const [ldc, ldr, dc, dr] of legs) {
        const leg = idx(col + ldc, row + ldr);
        if (!inBoard(col + ldc, row + ldr) || state.board[leg]) continue;
        const nc = col + dc;
        const nr = row + dr;
        if (!inBoard(nc, nr)) continue;
        addTarget(moves, state.board, from, idx(nc, nr), side);
      }
      break;
    }
    case 'R':
      step(1, 0, 8);
      step(-1, 0, 8);
      step(0, 1, 9);
      step(0, -1, 9);
      break;
    case 'C': {
      for (const [dc, dr] of [[1, 0], [-1, 0], [0, 1], [0, -1]] as const) {
        let jumped = false;
        for (let distance = 1; distance < 10; distance += 1) {
          const nc = col + dc * distance;
          const nr = row + dr * distance;
          if (!inBoard(nc, nr)) break;
          const to = idx(nc, nr);
          const target = state.board[to];
          if (!jumped) {
            if (!target) moves.push({ from, to });
            else jumped = true;
          } else if (target) {
            if (target.side !== side) moves.push({ from, to, captured: target.kind });
            break;
          }
        }
      }
      break;
    }
    case 'S': {
      const forward = side === 'r' ? -1 : 1;
      const nr = row + forward;
      if (inBoard(col, nr)) addTarget(moves, state.board, from, idx(col, nr), side);
      if (crossedRiver(from, side)) {
        for (const dc of [-1, 1]) {
          if (inBoard(col + dc, row)) addTarget(moves, state.board, from, idx(col + dc, row), side);
        }
      }
      break;
    }
    default:
      break;
  }
  return moves;
}

function generalIndex(board: (Piece | null)[], side: Side): number {
  return board.findIndex((piece) => piece?.kind === 'K' && piece.side === side);
}

function inCheck(board: (Piece | null)[], side: Side): boolean {
  const general = generalIndex(board, side);
  if (general < 0) return false;
  const rival = other(side);
  const rivalGeneral = generalIndex(board, rival);
  if (rivalGeneral >= 0 && colOf(rivalGeneral) === colOf(general)) {
    const min = Math.min(rowOf(general), rowOf(rivalGeneral));
    const max = Math.max(rowOf(general), rowOf(rivalGeneral));
    let clear = true;
    for (let r = min + 1; r < max; r += 1) {
      if (board[idx(colOf(general), r)]) {
        clear = false;
        break;
      }
    }
    if (clear) return true;
  }
  const pretend: XqState = {
    seats: [],
    options: {},
    board,
    turn: rival,
    phase: 'play',
    rng: () => 0,
  };
  for (let i = 0; i < board.length; i += 1) {
    const piece = board[i];
    if (!piece || piece.side !== rival) continue;
    if (pseudoMoves(pretend, i).some((move) => move.to === general)) return true;
  }
  return false;
}

function applyMove(state: XqState, move: Move): XqState {
  const board = state.board.slice();
  const piece = board[move.from];
  if (!piece) return state;
  const captured = board[move.to];
  board[move.to] = piece;
  board[move.from] = null;
  const next: XqState = {
    ...state,
    board,
    turn: other(piece.side),
    lastMove: { from: move.from, to: move.to },
  };
  if (captured?.kind === 'K') {
    return { ...next, phase: 'matchEnd', result: resultFor(next, piece.side, 'result.reason.xiangqiGeneral') };
  }
  return next;
}

function legalMoves(state: XqState, side: Side = state.turn): Move[] {
  const out: Move[] = [];
  for (let from = 0; from < state.board.length; from += 1) {
    const piece = state.board[from];
    if (!piece || piece.side !== side) continue;
    for (const move of pseudoMoves(state, from)) {
      const next = applyMove(state, move);
      if (!inCheck(next.board, side)) out.push(move);
    }
  }
  return out;
}

function material(board: (Piece | null)[], side: Side): number {
  return board.reduce((total, piece) => (piece?.side === side ? total + VALUES[piece.kind] : total), 0);
}

function resultFor(state: XqState, winner: Side, reasonKey: string): MatchResult {
  const seat = state.seats[winner === 'r' ? 0 : 1];
  return {
    winnerIds: seat ? [seat.id] : [],
    scores: {
      [state.seats[0]?.id ?? 'r']: material(state.board, 'r'),
      [state.seats[1]?.id ?? 'b']: material(state.board, 'b'),
    },
    reasonKey,
  };
}

function finished(state: XqState, winner: Side, reasonKey: string): XqState {
  return { ...state, phase: 'matchEnd', result: resultFor(state, winner, reasonKey) };
}

function promptFor(state: XqState, isTurn: boolean): PromptView {
  if (state.phase === 'matchEnd') return { key: 'prompt.matchEnd', tone: 'success' };
  if (!isTurn) {
    const seat = state.seats[state.turn === 'r' ? 0 : 1];
    return { key: 'prompt.waitingPlayer', params: { player: seat?.name ?? '—' }, tone: 'info' };
  }
  if (inCheck(state.board, state.turn)) return { key: 'prompt.xiangqiCheck', tone: 'warn' };
  return { key: 'prompt.xiangqiYourTurn', tone: 'info' };
}

function coord(i: number): string {
  return `${String.fromCharCode(65 + colOf(i))}${10 - rowOf(i)}`;
}

function evaluate(state: XqState, side: Side): number {
  let score = material(state.board, side) - material(state.board, other(side));
  for (let i = 0; i < state.board.length; i += 1) {
    const piece = state.board[i];
    if (!piece) continue;
    const center = 4 - Math.abs(colOf(i) - 4);
    const advance = side === 'r' ? 9 - rowOf(i) : rowOf(i);
    score += (piece.side === side ? 1 : -1) * (center * 2 + (piece.kind === 'S' ? advance * 3 : 0));
  }
  return score;
}

function search(state: XqState, depth: number, alpha: number, beta: number, root: Side): number {
  if (state.phase === 'matchEnd') {
    return state.result?.winnerIds[0] === state.seats[0]?.id ? (root === 'r' ? 100000 : -100000) : (root === 'b' ? 100000 : -100000);
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

export const xiangqiModule: GameModule<XqState> = {
  meta: {
    id: 'xiangqi',
    nameKey: 'game.xiangqi.name',
    descKey: 'game.xiangqi.desc',
    rulesKey: 'game.xiangqi.rules',
    icon: '🀄',
    category: 'tablero',
    minPlayers: 2,
    maxPlayers: 2,
    avgMinutes: 25,
    accent: '#b0392e',
    tagKeys: ['tag.estrategia', 'tag.ancestral', 'tag.duelo'],
  },

  create(seats, options, rng) {
    return {
      seats: seats.slice(0, 2).map((seat) => ({ ...seat })),
      options,
      board: initialBoard(),
      turn: 'r',
      phase: 'play',
      rng,
    };
  },

  view(state, viewerId): GameView {
    const turnSeat = state.seats[state.turn === 'r' ? 0 : 1];
    const isTurn = turnSeat?.id === viewerId;
    const moves = isTurn && state.phase === 'play' ? legalMoves(state, state.turn) : [];
    const destinations = new Map<number, boolean>();
    for (const move of moves) destinations.set(move.to, Boolean(state.board[move.to]));
    const sources = new Set(moves.map((move) => move.from));
    const cells: BoardCell[] = state.board.map((piece, i) => {
      let highlight: BoardCell['highlight'];
      if (destinations.has(i)) highlight = destinations.get(i) ? 'capture' : 'move';
      if (sources.has(i)) highlight = 'selected';
      if (state.lastMove?.to === i) highlight = 'last';
      return {
        glyph: piece ? GLYPHS[piece.side][piece.kind] : undefined,
        tone: 'xiangqi',
        ...(piece ? { side: piece.side } : {}),
        ...(highlight ? { highlight } : {}),
        label: coord(i),
        pieceId: piece ? `${piece.side}${piece.kind}` : undefined,
      };
    });
    const actions: UIAction[] = [];
    if (state.phase === 'play') actions.push(makeAction('resign', 'action.resign', { type: 'resign' }, { tone: 'danger', icon: 'flag' }));
    const pieces = {
      r: state.board.filter((piece) => piece?.side === 'r').length,
      b: state.board.filter((piece) => piece?.side === 'b').length,
    };
    return {
      gameId: 'xiangqi',
      youId: viewerId,
      phase: state.phase,
      turnId: turnSeat?.id ?? null,
      players: playerViews(
        state.seats,
        { [state.seats[0]?.id ?? 'r']: pieces.r, [state.seats[1]?.id ?? 'b']: pieces.b },
        turnSeat?.id ?? null,
        viewerId,
        () => ({ active: true }),
      ),
      hand: [],
      center: { board: { cols: COLS, rows: ROWS, cells, orientation: 'white' } },
      actions,
      prompt: promptFor(state, isTurn),
      extra: {
        legalMoves: moves.map((move) => ({ from: move.from, to: move.to, capture: Boolean(move.captured) })),
        lastMove: state.lastMove ?? null,
        check: inCheck(state.board, state.turn),
        pieces,
        turn: state.turn,
      },
    };
  },

  act(state, action, actorId, _rng): ActResult<XqState> {
    if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
    const turnSeat = state.seats[state.turn === 'r' ? 0 : 1];
    if (!turnSeat || turnSeat.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };
    if (action.type === 'resign') {
      const winner = other(state.turn);
      return {
        state: finished(state, winner, 'result.reason.resign'),
        events: [event('log.resign', { p: turnSeat.name }, 'danger', actorId)],
      };
    }
    if (action.type !== 'move') return { state, events: [], error: 'error.invalidAction' };
    const from = Number(action.from);
    const to = Number(action.to);
    const move = legalMoves(state, state.turn).find((candidate) => candidate.from === from && candidate.to === to);
    if (!move) return { state, events: [], error: 'error.invalidMove' };
    const next = applyMove(state, move);
    const events = [event('log.xiangqiMove', { p: turnSeat.name, from: coord(from), to: coord(to) }, 'info', actorId)];
    if (move.captured) events.push(event('log.xiangqiCapture', { p: turnSeat.name, piece: GLYPHS[other(state.turn)][move.captured] }, 'success', actorId));
    if (next.phase === 'matchEnd') {
      events.push(event('log.won', { p: turnSeat.name }, 'magic', actorId));
      return { state: next, events };
    }
    const rival = next.turn;
    const rivalMoves = legalMoves(next, rival);
    if (rivalMoves.length === 0) {
      const checked = inCheck(next.board, rival);
      const end = finished(next, state.turn, checked ? 'result.reason.checkmate' : 'result.reason.xiangqiNoMoves');
      events.push(event('log.won', { p: turnSeat.name }, 'magic', actorId));
      return { state: end, events };
    }
    if (inCheck(next.board, rival)) events.push(event('log.xiangqiCheck', { p: state.seats[rival === 'r' ? 0 : 1]?.name ?? '—' }, 'warn', actorId));
    return { state: next, events };
  },

  bot(state, botId, difficulty: Difficulty, rng: Rng): GameAction | null {
    if (state.phase === 'matchEnd') return null;
    const turnSeat = state.seats[state.turn === 'r' ? 0 : 1];
    if (!turnSeat || turnSeat.id !== botId) return null;
    const moves = legalMoves(state, state.turn);
    if (moves.length === 0) return null;
    if (difficulty === 'easy' && moves.length > 1) {
      const captures = moves.filter((move) => move.captured);
      const pool = captures.length > 0 ? captures : moves;
      const pick = pool[Math.floor(rng() * pool.length)] ?? moves[0];
      return pick ? { type: 'move', from: pick.from, to: pick.to } : null;
    }
    const depth = difficulty === 'hard' ? 3 : 2;
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

export type { XqState, Move as XiangqiMove };
