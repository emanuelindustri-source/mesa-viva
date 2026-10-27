import type {
  ActResult,
  BoardCell,
  Difficulty,
  GameAction,
  GameModule,
  GameOptions,
  GameView,
  MatchResult,
  Seat,
  UIAction,
} from '../core/types';
import type { Rng } from '../core/types';
import { event, playerViews } from './cards';
import { makeAction } from './helpers';

type Color = 'w' | 'b';

interface Checker {
  color: Color;
  king: boolean;
}

interface Move {
  from: number;
  to: number;
  captured?: number;
  promotion?: boolean;
}

interface DamasState {
  seats: Seat[];
  options: GameOptions;
  board: (Checker | null)[];
  turn: Color;
  mustContinueFrom: number | null;
  phase: 'play' | 'matchEnd';
  sinceCapture: number;
  lastMove?: { from: number; to: number };
  lastCapture?: number;
  winnerIds?: string[];
  result?: MatchResult;
  scores: Record<string, number>;
  rng: Rng;
}

const fileOf = (i: number) => i % 8;
const rankOf = (i: number) => Math.floor(i / 8);
const onBoard = (f: number, r: number) => f >= 0 && f < 8 && r >= 0 && r < 8;
const idx = (f: number, r: number) => r * 8 + f;
const other = (c: Color): Color => (c === 'w' ? 'b' : 'w');
const isDark = (i: number) => (fileOf(i) + rankOf(i)) % 2 === 1;

function initialBoard(): (Checker | null)[] {
  const board: (Checker | null)[] = Array.from({ length: 64 }, () => null);
  for (let i = 0; i < 64; i += 1) {
    if (!isDark(i)) continue;
    const r = rankOf(i);
    if (r <= 2) board[i] = { color: 'b', king: false };
    if (r >= 5) board[i] = { color: 'w', king: false };
  }
  return board;
}

function directions(piece: Checker): Array<[number, number]> {
  if (piece.king) return [[1, 1], [1, -1], [-1, 1], [-1, -1]];
  return piece.color === 'w' ? [[-1, -1], [-1, 1]] : [[1, -1], [1, 1]];
}

function jumpsFrom(board: (Checker | null)[], from: number, piece: Checker): Move[] {
  const moves: Move[] = [];
  const f = fileOf(from);
  const r = rankOf(from);
  for (const [df, dr] of directions(piece)) {
    const mf = f + df;
    const mr = r + dr;
    const tf = f + df * 2;
    const tr = r + dr * 2;
    if (!onBoard(tf, tr) || !onBoard(mf, mr)) continue;
    const mid = idx(mf, mr);
    const to = idx(tf, tr);
    const victim = board[mid];
    if (victim && victim.color !== piece.color && !board[to]) {
      const promo = piece.king ? false : piece.color === 'w' ? tr === 0 : tr === 7;
      moves.push({ from, to, captured: mid, ...(promo ? { promotion: true } : {}) });
    }
  }
  return moves;
}

function quietMovesFrom(board: (Checker | null)[], from: number, piece: Checker): Move[] {
  const moves: Move[] = [];
  const f = fileOf(from);
  const r = rankOf(from);
  for (const [df, dr] of directions(piece)) {
    const nf = f + df;
    const nr = r + dr;
    if (!onBoard(nf, nr)) continue;
    const to = idx(nf, nr);
    if (board[to]) continue;
    const promo = piece.king ? false : piece.color === 'w' ? nr === 0 : nr === 7;
    moves.push({ from, to, ...(promo ? { promotion: true } : {}) });
  }
  return moves;
}

function allMoves(state: DamasState): Move[] {
  const color = state.turn;
  const board = state.board;
  const captures: Move[] = [];
  const quiets: Move[] = [];
  if (state.mustContinueFrom !== null) {
    const from = state.mustContinueFrom;
    const piece = board[from];
    if (piece && piece.color === color) {
      return jumpsFrom(board, from, piece);
    }
  }
  for (let i = 0; i < 64; i += 1) {
    const piece = board[i];
    if (!piece || piece.color !== color) continue;
    captures.push(...jumpsFrom(board, i, piece));
    quiets.push(...quietMovesFrom(board, i, piece));
  }
  return captures.length > 0 ? captures : quiets;
}

function applyMove(state: DamasState, move: Move): DamasState {
  const board = state.board.slice();
  const piece = board[move.from];
  if (!piece) return state;
  board[move.from] = null;
  if (move.captured !== undefined) board[move.captured] = null;
  const promoted = Boolean(move.promotion);
  board[move.to] = promoted ? { color: piece.color, king: true } : piece;
  const nextColor = other(state.turn);
  const hasMore =
    !promoted &&
    jumpsFrom(board, move.to, board[move.to] as Checker).length > 0;
  const mustContinueFrom = hasMore ? move.to : null;
  return {
    ...state,
    board,
    mustContinueFrom,
    turn: hasMore ? state.turn : nextColor,
    sinceCapture: move.captured !== undefined || promoted ? 0 : state.sinceCapture + 1,
    lastMove: { from: move.from, to: move.to },
    ...(move.captured !== undefined ? { lastCapture: move.captured } : {}),
  };
}

function hasPieces(board: (Checker | null)[], color: Color): boolean {
  return board.some((p) => p?.color === color);
}

function legalFor(state: DamasState): Move[] {
  if (!hasPieces(state.board, state.turn)) return [];
  return allMoves(state);
}

function materialFor(state: DamasState, color: Color): number {
  let score = 0;
  for (let i = 0; i < 64; i += 1) {
    const p = state.board[i];
    if (!p || p.color !== color) continue;
    const r = rankOf(i);
    const advancement = p.color === 'w' ? 7 - r : r;
    score += (p.king ? 190 : 100) + advancement * 3;
    if (fileOf(i) >= 2 && fileOf(i) <= 5) score += 4;
  }
  return score;
}

function evaluate(state: DamasState, root: Color): number {
  return materialFor(state, root) - materialFor(state, other(root));
}

function search(state: DamasState, depth: number, alpha: number, beta: number, root: Color): number {
  const moves = legalFor(state);
  if (moves.length === 0) {
    return state.turn === root ? -100000 + (10 - depth) : 100000 - (10 - depth);
  }
  if (depth <= 0) return evaluate(state, root);
  const ordered = moves.slice().sort((a, b) => Number(Boolean(b.captured)) - Number(Boolean(a.captured)));
  if (state.turn === root) {
    let best = -Infinity;
    for (const move of ordered) {
      const value = search(applyMove(state, move), depth - 1, alpha, beta, root);
      if (value > best) best = value;
      if (value > alpha) alpha = value;
      if (alpha >= beta) break;
    }
    return best;
  }
  let best = Infinity;
  for (const move of ordered) {
    const value = search(applyMove(state, move), depth - 1, alpha, beta, root);
    if (value < best) best = value;
    if (value < beta) beta = value;
    if (alpha >= beta) break;
  }
  return best;
}

function finish(state: DamasState, winner: Color | null, reason: string, draw = false): DamasState {
  const winnerSeat = winner ? state.seats[winner === 'w' ? 0 : 1] : undefined;
  const winnerIds = winnerSeat ? [winnerSeat.id] : [];
  const result: MatchResult = {
    winnerIds,
    draw,
    scores: Object.fromEntries(state.seats.map((s) => [s.id, 0])),
    reasonKey: reason,
  };
  return { ...state, phase: 'matchEnd', winnerIds, result };
}

function checkEnd(state: DamasState): DamasState {
  if (state.sinceCapture >= 80) return finish(state, null, 'result.reason.draw', true);
  const nextMoves = legalFor(state);
  if (nextMoves.length === 0) {
    return finish(state, other(state.turn), 'result.reason.noMoves');
  }
  return state;
}

export const damasModule: GameModule<DamasState> = {
  meta: {
    id: 'damas',
    nameKey: 'game.damas.name',
    descKey: 'game.damas.desc',
    rulesKey: 'game.damas.rules',
    icon: '⚫',
    category: 'tablero',
    minPlayers: 2,
    maxPlayers: 2,
    avgMinutes: 15,
    accent: '#7d5a3c',
    tagKeys: ['tag.estrategia', 'tag.clasico', 'tag.duelo'],
  },

  create(seats, options, rng) {
    const board = initialBoard();
    return {
      seats: seats.slice(0, 2).map((s) => ({ ...s })),
      options,
      board,
      turn: 'w',
      mustContinueFrom: null,
      phase: 'play',
      sinceCapture: 0,
      scores: Object.fromEntries(seats.slice(0, 2).map((s) => [s.id, 0])),
      rng,
    };
  },

  view(state, viewerId): GameView {
    const turnSeat = state.seats[state.turn === 'w' ? 0 : 1];
    const isTurn = turnSeat?.id === viewerId;
    const moves = isTurn && state.phase === 'play' ? legalFor(state) : [];
    const destinations = new Set(moves.map((m) => m.to));
    const sources = new Set(moves.map((m) => m.from));
    const cells: BoardCell[] = state.board.map((piece, i) => {
      const dark = isDark(i);
      let highlight: BoardCell['highlight'];
      if (destinations.has(i)) highlight = 'move';
      if (sources.has(i)) highlight = 'selected';
      if (state.lastMove?.to === i) highlight = 'last';
      return {
        glyph: piece ? (piece.king ? '♛' : '●') : undefined,
        tone: piece
          ? piece.color === 'w'
            ? 'piece-w'
            : 'piece-b'
          : dark
            ? 'dark'
            : 'light',
        label: dark ? `${fileOf(i)}${rankOf(i)}` : undefined,
        ...(highlight ? { highlight } : {}),
        pieceId: piece ? `${piece.color}${piece.king ? 'K' : 'M'}` : undefined,
      };
    });
    const actions: UIAction[] = [];
    if (state.phase === 'play') {
      actions.push(makeAction('resign', 'action.resign', { type: 'resign' }, { tone: 'danger', icon: 'flag' }));
    }
    const prompt = state.phase === 'matchEnd'
      ? { key: 'prompt.matchEnd', tone: 'success' as const }
      : state.mustContinueFrom !== null
        ? { key: 'prompt.damasChain', tone: 'success' as const }
        : isTurn
          ? { key: 'prompt.damasYourTurn', tone: 'info' as const }
          : { key: 'prompt.waitingPlayer', params: { player: turnSeat?.name ?? '—' }, tone: 'info' as const };
    return {
      gameId: 'damas',
      youId: viewerId,
      phase: state.phase,
      turnId: turnSeat?.id ?? null,
      players: playerViews(state.seats, state.scores, turnSeat?.id ?? null, viewerId, () => ({ active: true })),
      hand: [],
      center: { board: { cols: 8, rows: 8, cells, orientation: 'white' } },
      actions,
      prompt,
      extra: {
        turn: state.turn,
        mustContinueFrom: state.mustContinueFrom,
        legalMoves: moves.map((m) => ({ from: m.from, to: m.to, capture: m.captured !== undefined })),
        lastMove: state.lastMove ?? null,
        pieces: {
          w: state.board.filter((p) => p?.color === 'w').length,
          b: state.board.filter((p) => p?.color === 'b').length,
        },
        sinceCapture: state.sinceCapture,
      },
    };
  },

  act(state, action, actorId, _rng): ActResult<DamasState> {
    if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
    const turnSeat = state.seats[state.turn === 'w' ? 0 : 1];
    if (!turnSeat || turnSeat.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };
    if (action.type === 'resign') {
      return {
        state: finish(state, other(state.turn), 'result.reason.resign'),
        events: [event('log.resign', { p: turnSeat.name }, 'danger', actorId)],
      };
    }
    if (action.type !== 'move') return { state, events: [], error: 'error.invalidAction' };
    const from = Number(action.from);
    const to = Number(action.to);
    const move = legalFor(state).find((m) => m.from === from && m.to === to);
    if (!move) return { state, events: [], error: 'error.invalidMove' };
    const events = [event('log.checkerMove', { p: turnSeat.name, from: square(from), to: square(to) }, 'info', actorId)];
    if (move.captured !== undefined) {
      events.push(event('log.checkerCapture', { p: turnSeat.name }, 'success', actorId));
    }
    if (move.promotion) {
      events.push(event('log.checkerPromote', { p: turnSeat.name }, 'magic', actorId));
    }
    let next = applyMove(state, move);
    if (next.mustContinueFrom === null) {
      next = checkEnd(next);
      if (next.phase === 'matchEnd') {
        events.push(
          event(
            next.result?.draw ? 'log.draw' : 'log.won',
            { p: next.result?.draw ? '—' : turnSeat.name },
            next.result?.draw ? 'info' : 'magic',
          ),
        );
      }
    }
    return { state: next, events };
  },

  bot(state, botId, difficulty: Difficulty, rng: Rng): GameAction | null {
    if (state.phase === 'matchEnd') return null;
    const color = state.turn;
    const seat = state.seats[color === 'w' ? 0 : 1];
    if (!seat || seat.id !== botId) return null;
    const moves = legalFor(state);
    if (moves.length === 0) return null;
    if (difficulty === 'easy' && moves.length > 1) {
      const captures = moves.filter((m) => m.captured !== undefined);
      const pool = captures.length > 0 ? captures : moves;
      const pick = pool[Math.floor(rng() * pool.length)] ?? moves[0];
      return pick ? { type: 'move', from: pick.from, to: pick.to } : null;
    }
    const depth = difficulty === 'hard' ? 4 : difficulty === 'normal' ? 3 : 2;
    let best = -Infinity;
    let pool: Move[] = [];
    for (const move of moves) {
      const value = search(applyMove(state, move), depth - 1, -Infinity, Infinity, color);
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

function square(i: number): string {
  return `${'abcdefgh'[fileOf(i)] ?? '?'}${8 - rankOf(i)}`;
}

export type { DamasState, Checker };
