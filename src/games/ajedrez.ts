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
type PieceType = 'p' | 'n' | 'b' | 'r' | 'q' | 'k';

interface Piece {
  type: PieceType;
  color: Color;
}

interface Move {
  from: number;
  to: number;
  piece: Piece;
  captured?: Piece;
  promo?: PieceType;
  castle?: 'K' | 'Q';
  ep?: boolean;
}

interface ChessState {
  seats: Seat[];
  options: GameOptions;
  board: (Piece | null)[];
  turn: Color;
  castling: { wK: boolean; wQ: boolean; bK: boolean; bQ: boolean };
  ep: number | null;
  halfmove: number;
  fullmove: number;
  positions: string[];
  lastMove?: { from: number; to: number };
  phase: 'play' | 'matchEnd';
  winnerIds?: string[];
  result?: MatchResult;
  rng: Rng;
  scores: Record<string, number>;
}

const GLYPHS: Record<Color, Record<PieceType, string>> = {
  w: { k: '♔', q: '♕', r: '♖', b: '♗', n: '♘', p: '♙' },
  b: { k: '♚', q: '♛', r: '♜', b: '♝', n: '♞', p: '♟' },
};

const MATE = 100000;
const VAL: Record<PieceType, number> = { p: 100, n: 320, b: 330, r: 500, q: 900, k: 0 };

const KNIGHT_OFFSETS: Array<[number, number]> = [
  [1, 2], [2, 1], [2, -1], [1, -2], [-1, -2], [-2, -1], [-2, 1], [-1, 2],
];
const KING_OFFSETS: Array<[number, number]> = [
  [0, 1], [1, 1], [1, 0], [1, -1], [0, -1], [-1, -1], [-1, 0], [-1, 1],
];
const BISHOP_DIRS: Array<[number, number]> = [[1, 1], [1, -1], [-1, -1], [-1, 1]];
const ROOK_DIRS: Array<[number, number]> = [[0, 1], [1, 0], [0, -1], [-1, 0]];

const fileOf = (i: number) => i % 8;
const rankOf = (i: number) => Math.floor(i / 8);
const onBoard = (f: number, r: number) => f >= 0 && f < 8 && r >= 0 && r < 8;
const idx = (f: number, r: number) => r * 8 + f;
const opponent = (c: Color): Color => (c === 'w' ? 'b' : 'w');

function initialBoard(): (Piece | null)[] {
  const board: (Piece | null)[] = Array.from({ length: 64 }, () => null);
  const back: PieceType[] = ['r', 'n', 'b', 'q', 'k', 'b', 'n', 'r'];
  back.forEach((type, f) => {
    board[idx(f, 0)] = { type, color: 'b' };
    board[idx(f, 7)] = { type, color: 'w' };
  });
  for (let f = 0; f < 8; f += 1) {
    board[idx(f, 1)] = { type: 'p', color: 'b' };
    board[idx(f, 6)] = { type: 'p', color: 'w' };
  }
  return board;
}

function findKing(board: (Piece | null)[], color: Color): number {
  return board.findIndex((p) => p?.type === 'k' && p.color === color);
}

function isAttacked(board: (Piece | null)[], square: number, by: Color): boolean {
  const f = fileOf(square);
  const r = rankOf(square);
  // Peones
  const pawnRow = by === 'w' ? r + 1 : r - 1;
  if (onBoard(f - 1, pawnRow)) {
    const p = board[idx(f - 1, pawnRow)];
    if (p?.type === 'p' && p.color === by) return true;
  }
  if (onBoard(f + 1, pawnRow)) {
    const p = board[idx(f + 1, pawnRow)];
    if (p?.type === 'p' && p.color === by) return true;
  }
  // Caballos
  for (const [df, dr] of KNIGHT_OFFSETS) {
    const nf = f + df;
    const nr = r + dr;
    if (!onBoard(nf, nr)) continue;
    const p = board[idx(nf, nr)];
    if (p?.type === 'n' && p.color === by) return true;
  }
  // Rey
  for (const [df, dr] of KING_OFFSETS) {
    const nf = f + df;
    const nr = r + dr;
    if (!onBoard(nf, nr)) continue;
    const p = board[idx(nf, nr)];
    if (p?.type === 'k' && p.color === by) return true;
  }
  // Deslizantes
  for (const [df, dr] of BISHOP_DIRS) {
    let nf = f + df;
    let nr = r + dr;
    while (onBoard(nf, nr)) {
      const p = board[idx(nf, nr)];
      if (p) {
        if (p.color === by && (p.type === 'b' || p.type === 'q')) return true;
        break;
      }
      nf += df;
      nr += dr;
    }
  }
  for (const [df, dr] of ROOK_DIRS) {
    let nf = f + df;
    let nr = r + dr;
    while (onBoard(nf, nr)) {
      const p = board[idx(nf, nr)];
      if (p) {
        if (p.color === by && (p.type === 'r' || p.type === 'q')) return true;
        break;
      }
      nf += df;
      nr += dr;
    }
  }
  return false;
}

function inCheck(board: (Piece | null)[], color: Color): boolean {
  const king = findKing(board, color);
  if (king < 0) return false;
  return isAttacked(board, king, opponent(color));
}

function pseudoMoves(state: ChessState, color: Color): Move[] {
  const moves: Move[] = [];
  const board = state.board;
  for (let from = 0; from < 64; from += 1) {
    const piece = board[from];
    if (!piece || piece.color !== color) continue;
    const f = fileOf(from);
    const r = rankOf(from);
    const add = (to: number, extra: Partial<Move> = {}) => {
      const target = board[to];
      if (target?.color === color) return;
      moves.push({ from, to, piece, ...(target ? { captured: target } : {}), ...extra });
    };
    if (piece.type === 'p') {
      const dir = color === 'w' ? -1 : 1;
      const startRow = color === 'w' ? 6 : 1;
      const promoRow = color === 'w' ? 0 : 7;
      const one = idx(f, r + dir);
      if (onBoard(f, r + dir) && !board[one]) {
        if (r + dir === promoRow) moves.push({ from, to: one, piece, promo: 'q' });
        else moves.push({ from, to: one, piece });
        if (r === startRow) {
          const two = idx(f, r + dir * 2);
          if (!board[two]) moves.push({ from, to: two, piece });
        }
      }
      for (const df of [-1, 1]) {
        const nf = f + df;
        const nr = r + dir;
        if (!onBoard(nf, nr)) continue;
        const to = idx(nf, nr);
        const target = board[to];
        if (target && target.color !== color) {
          moves.push({ from, to, piece, captured: target, ...(nr === promoRow ? { promo: 'q' } : {}) });
        } else if (state.ep === to && !target) {
          moves.push({ from, to, piece, ep: true });
        }
      }
    } else if (piece.type === 'n') {
      for (const [df, dr] of KNIGHT_OFFSETS) {
        if (onBoard(f + df, r + dr)) add(idx(f + df, r + dr));
      }
    } else if (piece.type === 'k') {
      for (const [df, dr] of KING_OFFSETS) {
        if (onBoard(f + df, r + dr)) add(idx(f + df, r + dr));
      }
      const rights = state.castling;
      if (color === 'w' && from === 60) {
        if (rights.wK && !board[61] && !board[62] && board[63]?.type === 'r' && board[63]?.color === 'w') {
          if (!isAttacked(board, 60, 'b') && !isAttacked(board, 61, 'b') && !isAttacked(board, 62, 'b')) {
            moves.push({ from, to: 62, piece, castle: 'K' });
          }
        }
        if (rights.wQ && !board[59] && !board[58] && !board[57] && board[56]?.type === 'r' && board[56]?.color === 'w') {
          if (!isAttacked(board, 60, 'b') && !isAttacked(board, 59, 'b') && !isAttacked(board, 58, 'b')) {
            moves.push({ from, to: 58, piece, castle: 'Q' });
          }
        }
      }
      if (color === 'b' && from === 4) {
        if (rights.bK && !board[5] && !board[6] && board[7]?.type === 'r' && board[7]?.color === 'b') {
          if (!isAttacked(board, 4, 'w') && !isAttacked(board, 5, 'w') && !isAttacked(board, 6, 'w')) {
            moves.push({ from, to: 6, piece, castle: 'K' });
          }
        }
        if (rights.bQ && !board[3] && !board[2] && !board[1] && board[0]?.type === 'r' && board[0]?.color === 'b') {
          if (!isAttacked(board, 4, 'w') && !isAttacked(board, 3, 'w') && !isAttacked(board, 2, 'w')) {
            moves.push({ from, to: 2, piece, castle: 'Q' });
          }
        }
      }
    } else {
      const dirs = piece.type === 'b' ? BISHOP_DIRS : piece.type === 'r' ? ROOK_DIRS : [...BISHOP_DIRS, ...ROOK_DIRS];
      for (const [df, dr] of dirs) {
        let nf = f + df;
        let nr = r + dr;
        while (onBoard(nf, nr)) {
          const to = idx(nf, nr);
          const target = board[to];
          if (!target) moves.push({ from, to, piece });
          else {
            if (target.color !== color) moves.push({ from, to, piece, captured: target });
            break;
          }
          nf += df;
          nr += dr;
        }
      }
    }
  }
  return moves;
}

function applyMove(state: ChessState, move: Move): ChessState {
  const board = state.board.slice();
  const piece = board[move.from];
  if (!piece) return state;
  board[move.from] = null;
  if (move.ep) {
    const dir = piece.color === 'w' ? -1 : 1;
    const capturedSquare = idx(fileOf(move.to), rankOf(move.to) - dir);
    board[capturedSquare] = null;
  }
  board[move.to] = move.promo ? { type: move.promo, color: piece.color } : piece;
  if (move.castle === 'K') {
    const rookFrom = piece.color === 'w' ? 63 : 7;
    const rookTo = piece.color === 'w' ? 61 : 5;
    board[rookTo] = board[rookFrom] ?? null;
    board[rookFrom] = null;
  }
  if (move.castle === 'Q') {
    const rookFrom = piece.color === 'w' ? 56 : 0;
    const rookTo = piece.color === 'w' ? 59 : 3;
    board[rookTo] = board[rookFrom] ?? null;
    board[rookFrom] = null;
  }
  let { wK, wQ, bK, bQ } = state.castling;
  if (piece.type === 'k') {
    if (piece.color === 'w') {
      wK = false;
      wQ = false;
    } else {
      bK = false;
      bQ = false;
    }
  }
  if (move.from === 63 || move.to === 63) wK = false;
  if (move.from === 56 || move.to === 56) wQ = false;
  if (move.from === 7 || move.to === 7) bK = false;
  if (move.from === 0 || move.to === 0) bQ = false;

  let ep: number | null = null;
  if (piece.type === 'p' && Math.abs(move.to - move.from) === 16) {
    ep = (move.from + move.to) / 2;
  }
  const isPawn = piece.type === 'p';
  const isCapture = Boolean(move.captured) || Boolean(move.ep);
  const next: ChessState = {
    ...state,
    board,
    turn: opponent(state.turn),
    castling: { wK, wQ, bK, bQ },
    ep,
    halfmove: isPawn || isCapture ? 0 : state.halfmove + 1,
    fullmove: state.turn === 'b' ? state.fullmove + 1 : state.fullmove,
    lastMove: { from: move.from, to: move.to },
    positions: [
      ...state.positions,
      keyOf(board, opponent(state.turn), { wK, wQ, bK, bQ }, ep),
    ],
  };
  return next;
}

function legalMoves(state: ChessState, color: Color): Move[] {
  const out: Move[] = [];
  for (const move of pseudoMoves(state, color)) {
    const next = applyMove(state, move);
    if (!inCheck(next.board, color)) out.push(move);
  }
  return out;
}

function positionKey(state: ChessState): string {
  return keyOf(state.board, state.turn, state.castling, state.ep);
}

function keyOf(
  board: (Piece | null)[],
  turn: Color,
  castling: ChessState['castling'],
  ep: number | null,
): string {
  const cells = board.map((p) => (p ? `${p.color}${p.type}` : '.')).join('');
  const c = castling;
  return `${cells}|${turn}|${c.wK}${c.wQ}${c.bK}${c.bQ}|${ep ?? '-'}`;
}

function evaluateFor(state: ChessState, color: Color): number {
  let score = 0;
  for (let i = 0; i < 64; i += 1) {
    const p = state.board[i];
    if (!p) continue;
    const f = fileOf(i);
    const r = rankOf(i);
    const center = 4 - (Math.abs(3.5 - f) + Math.abs(3.5 - r));
    let value = VAL[p.type];
    if (p.type === 'p') value += (p.color === 'w' ? 6 - r : r - 1) * 10 + center * 2;
    if (p.type === 'n') value += center * 8;
    if (p.type === 'b') value += center * 4;
    if (p.type === 'r') value += (p.color === 'w' ? r === 1 : r === 6) ? 10 : 0;
    if (p.type === 'q') value += center * 2;
    if (p.type === 'k') value -= center * 3;
    score += p.color === color ? value : -value;
  }
  return score;
}

function insufficientMaterial(board: (Piece | null)[]): boolean {
  const pieces = board.filter((p): p is Piece => Boolean(p));
  if (pieces.length === 2) return true;
  if (pieces.length === 3) {
    return pieces.some((p) => p.type === 'b' || p.type === 'n');
  }
  if (pieces.length === 4) {
    const minors = pieces.filter((p) => p.type === 'b' || p.type === 'n');
    if (minors.length === 2 && minors.every((p) => p.type === 'b')) {
      const squares = board.map((p, i) => (p?.type === 'b' ? i : -1)).filter((i) => i >= 0);
      if (squares.length === 2) {
        const [a, b] = squares;
        if (a !== undefined && b !== undefined) {
          return (fileOf(a) + rankOf(a)) % 2 === (fileOf(b) + rankOf(b)) % 2;
        }
      }
    }
  }
  return false;
}

function scoreState(state: ChessState): Record<string, number> {
  return Object.fromEntries(state.seats.map((s) => [s.id, 0]));
}

function finish(
  state: ChessState,
  winnerColor: Color | null,
  reason: string,
  draw = false,
): ChessState {
  const winnerSeat = winnerColor ? state.seats[winnerColor === 'w' ? 0 : 1] : undefined;
  const winnerIds = winnerSeat ? [winnerSeat.id] : [];
  const result: MatchResult = {
    winnerIds,
    draw,
    scores: scoreState(state),
    reasonKey: reason,
  };
  return { ...state, phase: 'matchEnd', winnerIds, result };
}

function checkGameEnd(state: ChessState): ChessState {
  const moves = legalMoves(state, state.turn);
  if (moves.length === 0) {
    if (inCheck(state.board, state.turn)) {
      return finish(state, opponent(state.turn), 'result.reason.checkmate');
    }
    return finish(state, null, 'result.reason.stalemate', true);
  }
  if (state.halfmove >= 100) return finish(state, null, 'result.reason.fiftyMoves', true);
  if (insufficientMaterial(state.board)) return finish(state, null, 'result.reason.material', true);
  const key = positionKey(state);
  if (state.positions.filter((k) => k === key).length >= 3) {
    return finish(state, null, 'result.reason.repetition', true);
  }
  return state;
}

function squareName(i: number): string {
  return `${'abcdefgh'[fileOf(i)] ?? '?'}${8 - rankOf(i)}`;
}

function sanFor(state: ChessState, move: Move): string {
  if (move.castle === 'K') return 'O-O';
  if (move.castle === 'Q') return 'O-O-O';
  const piece = move.piece;
  const letter = piece.type === 'p' ? '' : piece.type.toUpperCase();
  let san = letter;
  if (piece.type !== 'p') {
    const others = legalMoves(state, piece.color).filter(
      (m) => m.piece.type === piece.type && m.to === move.to && m.from !== move.from,
    );
    if (others.length > 0) {
      const sameFile = others.some((m) => fileOf(m.from) === fileOf(move.from));
      const sameRank = others.some((m) => rankOf(m.from) === rankOf(move.from));
      if (!sameFile) san += squareName(move.from)[0];
      else if (!sameRank) san += squareName(move.from)[1];
      else san += squareName(move.from);
    }
  } else if (move.captured || move.ep) {
    san += squareName(move.from)[0] ?? '';
  }
  if (move.captured || move.ep) san += 'x';
  san += squareName(move.to);
  if (move.promo) san += `=${move.promo.toUpperCase()}`;
  const next = applyMove(state, move);
  if (inCheck(next.board, next.turn)) {
    san += legalMoves(next, next.turn).length === 0 ? '#' : '+';
  }
  return san;
}

function search(state: ChessState, depth: number, alpha: number, beta: number, color: Color): number {
  const moves = legalMoves(state, color);
  if (moves.length === 0) return inCheck(state.board, color) ? -MATE + (10 - depth) : 0;
  if (depth <= 0) return evaluateFor(state, color);
  const ordered = moves.slice().sort((a, b) => {
    const av = (a.captured ? VAL[a.captured.type] : 0) + (a.promo ? 800 : 0);
    const bv = (b.captured ? VAL[b.captured.type] : 0) + (b.promo ? 800 : 0);
    return bv - av;
  });
  let best = -Infinity;
  for (const move of ordered) {
    const next = applyMove(state, move);
    const score = -search(next, depth - 1, -beta, -alpha, opponent(color));
    if (score > best) best = score;
    if (score > alpha) alpha = score;
    if (alpha >= beta) break;
  }
  return best;
}

export const ajedrezModule: GameModule<ChessState> = {
  meta: {
    id: 'ajedrez',
    nameKey: 'game.ajedrez.name',
    descKey: 'game.ajedrez.desc',
    rulesKey: 'game.ajedrez.rules',
    icon: '♞',
    category: 'tablero',
    minPlayers: 2,
    maxPlayers: 2,
    avgMinutes: 15,
    accent: '#b08d57',
    tagKeys: ['tag.estrategia', 'tag.clasico', 'tag.duelo'],
  },

  create(seats, options, rng) {
    const board = initialBoard();
    const state: ChessState = {
      seats: seats.slice(0, 2).map((s) => ({ ...s })),
      options,
      board,
      turn: 'w',
      castling: { wK: true, wQ: true, bK: true, bQ: true },
      ep: null,
      halfmove: 0,
      fullmove: 1,
      positions: [keyOf(board, 'w', { wK: true, wQ: true, bK: true, bQ: true }, null)],
      phase: 'play',
      rng,
      scores: Object.fromEntries(seats.slice(0, 2).map((s) => [s.id, 0])),
    };
    return state;
  },

  view(state, viewerId): GameView {
    const turnSeat = state.seats[state.turn === 'w' ? 0 : 1];
    const isTurn = turnSeat?.id === viewerId;
    const kingSquare = findKing(state.board, state.turn);
    const checked = inCheck(state.board, state.turn);
    const cells: BoardCell[] = state.board.map((piece, i) => {
      const light = (fileOf(i) + rankOf(i)) % 2 === 0;
      const highlight = state.lastMove && (state.lastMove.from === i || state.lastMove.to === i)
        ? 'last'
        : undefined;
      return {
        glyph: piece ? GLYPHS[piece.color][piece.type] : undefined,
        tone: light ? 'light' : 'dark',
        ...(highlight ? { highlight } : {}),
        ...(checked && i === kingSquare ? { highlight: 'check' } : {}),
        label: squareName(i),
        pieceId: piece ? `${piece.color}${piece.type}` : undefined,
      };
    });
    const actions: UIAction[] = [];
    if (state.phase === 'play') {
      actions.push(makeAction('resign', 'action.resign', { type: 'resign' }, { tone: 'danger', icon: 'flag' }));
    }
    const moves = isTurn && state.phase === 'play'
      ? legalMoves(state, state.turn).map((m) => ({
          from: m.from,
          to: m.to,
          promo: m.promo ?? null,
          castle: m.castle ?? null,
          ep: Boolean(m.ep),
        }))
      : [];
    const prompt = state.phase === 'matchEnd'
      ? { key: 'prompt.matchEnd', tone: 'success' as const }
      : checked
        ? { key: isTurn ? 'prompt.checkYou' : 'prompt.checkOther', params: { player: turnSeat?.name ?? '—' }, tone: 'warn' as const }
        : isTurn
          ? { key: 'prompt.chessYourTurn', tone: 'info' as const }
          : { key: 'prompt.waitingPlayer', params: { player: turnSeat?.name ?? '—' }, tone: 'info' as const };
    return {
      gameId: 'ajedrez',
      youId: viewerId,
      phase: state.phase,
      turnId: turnSeat?.id ?? null,
      players: playerViews(state.seats, state.scores, turnSeat?.id ?? null, viewerId, () => ({ active: true })),
      hand: [],
      center: {
        board: { cols: 8, rows: 8, cells, orientation: 'white' },
      },
      actions,
      prompt,
      extra: {
        turn: state.turn,
        check: checked,
        kingSquare,
        legalMoves: moves,
        lastMove: state.lastMove ?? null,
        fullmove: state.fullmove,
        halfmove: state.halfmove,
        captured: {
          w: state.board.filter((p) => p?.color === 'b').length,
          b: state.board.filter((p) => p?.color === 'w').length,
        },
      },
    };
  },

  act(state, action, actorId, _rng): ActResult<ChessState> {
    if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
    const turnSeat = state.seats[state.turn === 'w' ? 0 : 1];
    if (!turnSeat || turnSeat.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };

    if (action.type === 'resign') {
      const winner = opponent(state.turn);
      const finished = finish(state, winner, 'result.reason.resign');
      return {
        state: finished,
        events: [event('log.resign', { p: turnSeat.name }, 'danger', actorId)],
      };
    }

    if (action.type !== 'move') return { state, events: [], error: 'error.invalidAction' };
    const from = Number(action.from);
    const to = Number(action.to);
    if (!Number.isInteger(from) || !Number.isInteger(to)) return { state, events: [], error: 'error.invalidMove' };
    const legal = legalMoves(state, state.turn);
    const move = legal.find((m) => m.from === from && m.to === to);
    if (!move) return { state, events: [], error: 'error.invalidMove' };
    const san = sanFor(state, move);
    const next = applyMove(state, move);
    const events = [event('log.chessMove', { p: turnSeat.name, san }, 'info', actorId)];
    const checked = checkGameEnd(next);
    if (checked.phase === 'matchEnd') {
      events.push(
        event(
          checked.result?.draw ? 'log.draw' : 'log.chessMate',
          { p: turnSeat.name },
          checked.result?.draw ? 'info' : 'magic',
        ),
      );
      return { state: checked, events };
    }
    if (inCheck(next.board, next.turn)) {
      events.push(event('log.chessCheck', { p: state.seats[next.turn === 'w' ? 0 : 1]?.name ?? '—' }, 'warn'));
    }
    return { state: checked, events };
  },

  bot(state, botId, difficulty: Difficulty, rng: Rng): GameAction | null {
    if (state.phase === 'matchEnd') return null;
    const color = state.turn;
    const turnSeat = state.seats[color === 'w' ? 0 : 1];
    if (!turnSeat || turnSeat.id !== botId) return null;
    const moves = legalMoves(state, color);
    if (moves.length === 0) return null;
    if (difficulty === 'easy') {
      const captures = moves.filter((m) => m.captured);
      const pool = captures.length > 0 && rng() < 0.55 ? captures : moves;
      const move = pool[Math.floor(rng() * pool.length)];
      return move ? { type: 'move', from: move.from, to: move.to } : null;
    }
    const depth = difficulty === 'hard' ? 3 : 2;
    let bestScore = -Infinity;
    let bestMoves: Move[] = [];
    for (const move of moves) {
      const next = applyMove(state, move);
      const score = -search(next, depth - 1, -Infinity, Infinity, opponent(color));
      if (score > bestScore + 5) {
        bestScore = score;
        bestMoves = [move];
      } else if (Math.abs(score - bestScore) <= 5) {
        bestMoves.push(move);
      }
    }
    const chosen = bestMoves[Math.floor(rng() * bestMoves.length)] ?? moves[0];
    return chosen ? { type: 'move', from: chosen.from, to: chosen.to } : null;
  },

  result(state) {
    return state.result ?? null;
  },
};

export type { ChessState, Piece, Move };
