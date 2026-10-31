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
import { prompt } from './helpers';

type Color = 0 | 1;
type Phase = 'play' | 'matchEnd';

export interface ReversiState {
  seats: Seat[];
  board: number[];
  turn: Color;
  phase: Phase;
  lastMove?: number;
  lastFlips: number[];
  winner?: Color;
  draw: boolean;
  passed: boolean;
  history: LogEvent[];
  result?: MatchResult;
  rng: Rng;
}

const SIZE = 8;
const DIRECTIONS: Array<[number, number]> = [
  [-1, -1], [-1, 0], [-1, 1],
  [0, -1], [0, 1],
  [1, -1], [1, 0], [1, 1],
];

function other(color: Color): Color {
  return color === 0 ? 1 : 0;
}

function stone(color: Color): number {
  return color === 0 ? 1 : 2;
}

function file(index: number): string {
  return String.fromCharCode(97 + (index % SIZE));
}

function square(index: number): string {
  return `${file(index)}${SIZE - Math.floor(index / SIZE)}`;
}

function initialBoard(): number[] {
  const board = Array.from({ length: SIZE * SIZE }, () => 0);
  board[27] = 1;
  board[36] = 1;
  board[28] = 2;
  board[35] = 2;
  return board;
}

export function reversiLegalMoves(board: number[], color: Color): Map<number, number[]> {
  const mine = stone(color);
  const theirs = stone(other(color));
  const result = new Map<number, number[]>();
  for (let index = 0; index < board.length; index += 1) {
    if (board[index] !== 0) continue;
    const col = index % SIZE;
    const row = Math.floor(index / SIZE);
    const flips: number[] = [];
    for (const [dc, dr] of DIRECTIONS) {
      let c = col + dc;
      let r = row + dr;
      const line: number[] = [];
      while (c >= 0 && c < SIZE && r >= 0 && r < SIZE) {
        const cell = r * SIZE + c;
        const value = board[cell];
        if (value === theirs) line.push(cell);
        else if (value === mine) {
          if (line.length > 0) flips.push(...line);
          break;
        } else break;
        c += dc;
        r += dr;
      }
    }
    if (flips.length > 0) result.set(index, [...new Set(flips)]);
  }
  return result;
}

function count(board: number[], value: number): number {
  return board.filter((cell) => cell === value).length;
}

function normalizeTurn(state: ReversiState): ReversiState {
  if (reversiLegalMoves(state.board, state.turn).size > 0) return state;
  const rival = other(state.turn);
  if (reversiLegalMoves(state.board, rival).size > 0) {
    return { ...state, turn: rival, passed: true };
  }
  const white = count(state.board, 1);
  const black = count(state.board, 2);
  const winner = white === black ? undefined : white > black ? 0 : 1;
  const result: MatchResult = {
    winnerIds: winner === undefined ? [] : [state.seats[winner]?.id ?? ''],
    draw: white === black,
    scores: {
      [state.seats[0]?.id ?? 'p0']: white,
      [state.seats[1]?.id ?? 'p1']: black,
    },
    reasonKey: 'result.reason.reversiScore',
  };
  return {
    ...state,
    phase: 'matchEnd',
    winner,
    draw: white === black,
    result,
    history: [
      ...state.history,
      event('log.reversiEnd', { white, black }, 'success'),
    ],
  };
}

function applyMove(state: ReversiState, index: number): ReversiState {
  const flips = reversiLegalMoves(state.board, state.turn).get(index);
  if (!flips) return state;
  const board = state.board.slice();
  board[index] = stone(state.turn);
  for (const cell of flips) board[cell] = board[index] ?? 0;
  const entry = event('log.reversiMove', {
    p: state.seats[state.turn]?.name ?? '—',
    square: square(index),
    flips: flips.length,
  }, 'info', state.seats[state.turn]?.id);
  const next: ReversiState = {
    ...state,
    board,
    turn: other(state.turn),
    lastMove: index,
    lastFlips: flips,
    passed: false,
    history: [...state.history, entry],
  };
  return normalizeTurn(next);
}

function chooseBotMove(state: ReversiState, difficulty: Difficulty, rng: Rng): number | null {
  const moves = reversiLegalMoves(state.board, state.turn);
  if (moves.size === 0) return null;
  if (difficulty === 'easy') {
    const keys = [...moves.keys()];
    return keys[Math.floor(rng() * keys.length)] ?? null;
  }
  const corners = new Set([0, 7, 56, 63]);
  let best = -Infinity;
  let pool: number[] = [];
  for (const [index, flips] of moves.entries()) {
    const value = (corners.has(index) ? 50 : 0) + flips.length * 2 + (difficulty === 'hard' ? 0 : rng());
    if (value > best) {
      best = value;
      pool = [index];
    } else if (value === best) pool.push(index);
  }
  return pool[Math.floor(rng() * pool.length)] ?? null;
}

export const reversiModule: GameModule<ReversiState> = {
  meta: {
    id: 'reversi',
    nameKey: 'game.reversi.name',
    descKey: 'game.reversi.desc',
    rulesKey: 'game.reversi.rules',
    icon: '⚫',
    category: 'tablero',
    minPlayers: 2,
    maxPlayers: 2,
    avgMinutes: 15,
    accent: '#2f6fdb',
    tagKeys: ['tag.estrategia', 'tag.clasico'],
  },

  create(seats, _options, rng): ReversiState {
    const base: ReversiState = {
      seats: seats.slice(0, 2),
      board: initialBoard(),
      turn: 0,
      phase: 'play',
      lastFlips: [],
      draw: false,
      passed: false,
      history: [event('log.reversiStart', {}, 'info')],
      rng,
    };
    return base;
  },

  view(state, viewerId): GameView {
    const legal = state.phase === 'play' && state.seats[state.turn]?.id === viewerId
      ? reversiLegalMoves(state.board, state.turn)
      : new Map<number, number[]>();
    const white = count(state.board, 1);
    const black = count(state.board, 2);
    const cells = state.board.map((value, index) => ({
      glyph: value === 1 ? '●' : value === 2 ? '○' : undefined,
      side: value === 1 ? 'w' as const : value === 2 ? 'b' as const : undefined,
      label: square(index),
      highlight: legal.has(index)
        ? 'move' as const
        : state.lastMove === index
          ? 'last' as const
          : state.lastFlips.includes(index)
            ? 'capture' as const
            : undefined,
    }));
    return {
      gameId: 'reversi',
      youId: viewerId,
      phase: state.phase,
      turnId: state.seats[state.turn]?.id ?? null,
      players: playerViews(state.seats, { [state.seats[0]?.id ?? 'p0']: white, [state.seats[1]?.id ?? 'p1']: black }, state.seats[state.turn]?.id ?? null, viewerId, (seat) => ({
        cardsLeft: seat.id === state.seats[0]?.id ? white : black,
        active: state.phase !== 'matchEnd',
      })),
      hand: [],
      center: { board: { cols: 8, rows: 8, cells } },
      actions: [],
      prompt: state.phase === 'matchEnd'
        ? prompt('prompt.matchEnd', {}, 'success')
        : state.seats[state.turn]?.id === viewerId
          ? prompt('prompt.reversi', { n: legal.size }, 'info')
          : prompt('prompt.waitingPlayer', { player: state.seats[state.turn]?.name ?? '—' }, 'info'),
      extra: {
        white,
        black,
        passed: state.passed,
        lastFlips: state.lastFlips,
      },
    };
  },

  act(state, action, actorId): ActResult<ReversiState> {
    if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
    if (state.seats[state.turn]?.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };
    if (action.type !== 'tap' && action.type !== 'move') return { state, events: [], error: 'error.invalidAction' };
    const index = Number(action.cell);
    if (!Number.isInteger(index) || index < 0 || index >= SIZE * SIZE) {
      return { state, events: [], error: 'error.invalidAction' };
    }
    const flips = reversiLegalMoves(state.board, state.turn).get(index);
    if (!flips) return { state, events: [], error: 'error.invalidAction' };
    const before = state.history.length;
    const next = applyMove(state, index);
    return { state: next, events: next.history.slice(before) };
  },

  bot(state, botId, difficulty, rng): GameAction | null {
    if (state.phase === 'matchEnd') return null;
    if (state.seats[state.turn]?.id !== botId) return null;
    const index = chooseBotMove(state, difficulty, rng);
    return index === null ? null : { type: 'tap', cell: index };
  },

  result(state): MatchResult | null {
    return state.result ?? null;
  },
};
