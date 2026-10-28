import type {
  ActResult,
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

export type LudoColor = 'red' | 'green' | 'yellow' | 'blue';

export interface LudoToken {
  id: string;
  color: LudoColor;
  slot: number;
  /** -1 en base, 0..51 en el recorrido, 52..56 en la columna, 57 en la meta. */
  progress: number;
}

interface LudoState {
  seats: Seat[];
  options: GameOptions;
  colors: LudoColor[];
  tokens: LudoToken[];
  turn: number;
  phase: 'roll' | 'move' | 'matchEnd';
  dice: number | null;
  lastRoll: number | null;
  sixStreak: number;
  captures: Record<string, number>;
  lastMove?: { tokenId: string; from: number; to: number };
  result?: MatchResult;
  rng: Rng;
}

interface LudoMove {
  tokenId: string;
  from: number;
  to: number;
}

const COLOR_ORDER: LudoColor[] = ['red', 'green', 'yellow', 'blue'];
const COLOR_LABEL: Record<LudoColor, string> = {
  red: 'ludo.red',
  green: 'ludo.green',
  yellow: 'ludo.yellow',
  blue: 'ludo.blue',
};

/** Recorrido de 52 casillas, en sentido horario. */
export const LUDO_TRACK: Array<[number, number]> = [
  [6, 14], [6, 13], [6, 12], [6, 11], [6, 10], [6, 9],
  [5, 8], [4, 8], [3, 8], [2, 8], [1, 8], [0, 8],
  [0, 7],
  [0, 6], [1, 6], [2, 6], [3, 6], [4, 6], [5, 6],
  [6, 5], [6, 4], [6, 3], [6, 2], [6, 1], [6, 0],
  [7, 0],
  [8, 0], [8, 1], [8, 2], [8, 3], [8, 4], [8, 5],
  [9, 6], [10, 6], [11, 6], [12, 6], [13, 6], [14, 6],
  [14, 7],
  [14, 8], [13, 8], [12, 8], [11, 8], [10, 8], [9, 8],
  [8, 9], [8, 10], [8, 11], [8, 12], [8, 13], [8, 14],
  [7, 14],
];

export const LUDO_START: Record<LudoColor, number> = {
  red: 0,
  green: 13,
  yellow: 26,
  blue: 39,
};

export const LUDO_HOME: Record<LudoColor, Array<[number, number]>> = {
  red: [[7, 13], [7, 12], [7, 11], [7, 10], [7, 9]],
  green: [[1, 7], [2, 7], [3, 7], [4, 7], [5, 7]],
  yellow: [[7, 1], [7, 2], [7, 3], [7, 4], [7, 5]],
  blue: [[13, 7], [12, 7], [11, 7], [10, 7], [9, 7]],
};

export const LUDO_BASE: Record<LudoColor, Array<[number, number]>> = {
  red: [[1, 10], [4, 10], [1, 13], [4, 13]],
  green: [[1, 1], [4, 1], [1, 4], [4, 4]],
  yellow: [[10, 1], [13, 1], [10, 4], [13, 4]],
  blue: [[10, 10], [13, 10], [10, 13], [13, 13]],
};

const SAFE_TRACK = new Set<number>([0, 8, 13, 21, 26, 34, 39, 47]);
const FINISH = 57;

export function ludoColorsForPlayers(count: number): LudoColor[] {
  if (count <= 2) return ['red', 'yellow'];
  if (count === 3) return ['red', 'green', 'yellow'];
  return [...COLOR_ORDER];
}

export function ludoTokenCoord(token: LudoToken): [number, number] {
  if (token.progress === -1) return LUDO_BASE[token.color][token.slot] ?? [0, 0];
  if (token.progress >= 52 && token.progress < FINISH) return LUDO_HOME[token.color][token.progress - 52] ?? [7, 7];
  if (token.progress >= FINISH) return [7, 7];
  return LUDO_TRACK[(LUDO_START[token.color] + token.progress) % LUDO_TRACK.length] ?? [0, 0];
}

function absolute(color: LudoColor, progress: number): number {
  return (LUDO_START[color] + progress) % LUDO_TRACK.length;
}

function tokensOf(state: LudoState, color: LudoColor): LudoToken[] {
  return state.tokens.filter((token) => token.color === color);
}

function seatColor(state: LudoState, seatIndex: number): LudoColor {
  return state.colors[seatIndex] ?? 'red';
}

function legalMoves(state: LudoState, seatIndex: number, die: number): LudoMove[] {
  if (die < 1 || die > 6) return [];
  const color = seatColor(state, seatIndex);
  const moves: LudoMove[] = [];
  for (const token of tokensOf(state, color)) {
    if (token.progress === FINISH) continue;
    if (token.progress === -1) {
      if (die === 6) moves.push({ tokenId: token.id, from: -1, to: 0 });
      continue;
    }
    const to = token.progress + die;
    if (to <= FINISH) moves.push({ tokenId: token.id, from: token.progress, to });
  }
  return moves;
}

function finishedCount(state: LudoState, seatIndex: number): number {
  return tokensOf(state, seatColor(state, seatIndex)).filter((token) => token.progress >= FINISH).length;
}

function scores(state: LudoState): Record<string, number> {
  return Object.fromEntries(state.seats.map((seat, index) => [seat.id, finishedCount(state, index)]));
}

function resultFor(state: LudoState, winnerIndex: number): MatchResult {
  const winner = state.seats[winnerIndex];
  return {
    winnerIds: winner ? [winner.id] : [],
    scores: scores(state),
    reasonKey: 'result.reason.ludoHome',
  };
}

function nextSeat(state: LudoState): number {
  return (state.turn + 1) % Math.max(1, state.seats.length);
}

function promptFor(state: LudoState, isTurn: boolean): PromptView {
  if (state.phase === 'matchEnd') return { key: 'prompt.matchEnd', tone: 'success' };
  if (!isTurn) {
    const seat = state.seats[state.turn];
    return { key: 'prompt.waitingPlayer', params: { player: seat?.name ?? '—' }, tone: 'info' };
  }
  if (state.phase === 'roll') return { key: 'prompt.ludoRoll', tone: 'info' };
  return { key: 'prompt.ludoMove', params: { n: state.dice ?? 0 }, tone: 'info' };
}

function applyMove(state: LudoState, move: LudoMove): { state: LudoState; captured: LudoToken[]; finished: boolean } {
  const die = state.dice ?? 0;
  const moving = state.tokens.find((token) => token.id === move.tokenId);
  if (!moving) return { state, captured: [], finished: false };
  const captured: LudoToken[] = [];
  if (move.to <= 51 && !SAFE_TRACK.has(absolute(moving.color, move.to))) {
    const destination = absolute(moving.color, move.to);
    for (const token of state.tokens) {
      if (token.color === moving.color) continue;
      if (token.progress < 0 || token.progress > 51) continue;
      if (absolute(token.color, token.progress) === destination) captured.push(token);
    }
  }
  const capturedIds = new Set(captured.map((token) => token.id));
  const tokens = state.tokens.map((token) => {
    if (token.id === moving.id) return { ...token, progress: move.to };
    if (capturedIds.has(token.id)) return { ...token, progress: -1 };
    return token;
  });
  const finished = move.to >= FINISH;
  const seat = state.seats[state.turn];
  const captures = { ...state.captures };
  if (seat) captures[seat.id] = (captures[seat.id] ?? 0) + captured.length;
  const next: LudoState = {
    ...state,
    tokens,
    captures,
    dice: null,
    lastMove: { tokenId: move.tokenId, from: move.from, to: move.to },
    lastRoll: die,
    sixStreak: die === 6 ? state.sixStreak : 0,
    phase: 'roll',
    turn: state.turn,
  };
  const won = tokensOf(next, moving.color).every((token) => token.progress >= FINISH);
  if (won) {
    return {
      state: { ...next, phase: 'matchEnd', result: resultFor(next, state.turn) },
      captured,
      finished,
    };
  }
  const extra = die === 6 || captured.length > 0 || finished;
  return {
    state: { ...next, turn: extra ? state.turn : nextSeat(state) },
    captured,
    finished,
  };
}

export const ludoModule: GameModule<LudoState> = {
  meta: {
    id: 'ludo',
    nameKey: 'game.ludo.name',
    descKey: 'game.ludo.desc',
    rulesKey: 'game.ludo.rules',
    icon: '🎲',
    category: 'tablero',
    minPlayers: 2,
    maxPlayers: 4,
    avgMinutes: 25,
    accent: '#d9534f',
    tagKeys: ['tag.clasico', 'tag.familia', 'tag.dados'],
  },

  create(seats, options, rng) {
    const colors = ludoColorsForPlayers(seats.length);
    const active = seats.slice(0, colors.length);
    const tokens: LudoToken[] = [];
    colors.forEach((color) => {
      for (let slot = 0; slot < 4; slot += 1) {
        tokens.push({ id: `${color}-${slot}`, color, slot, progress: -1 });
      }
    });
    return {
      seats: active.map((seat) => ({ ...seat })),
      options,
      colors,
      tokens,
      turn: 0,
      phase: 'roll',
      dice: null,
      lastRoll: null,
      sixStreak: 0,
      captures: Object.fromEntries(active.map((seat) => [seat.id, 0])),
      rng,
    };
  },

  view(state, viewerId): GameView {
    const turnSeat = state.seats[state.turn];
    const isTurn = turnSeat?.id === viewerId;
    const moves = isTurn && state.phase === 'move' && state.dice ? legalMoves(state, state.turn, state.dice) : [];
    const actions: UIAction[] = [];
    if (state.phase === 'roll') {
      if (isTurn) actions.push(makeAction('roll', 'action.rollDice', { type: 'roll' }, { tone: 'primary', icon: 'dice' }));
    }
    if (state.phase !== 'matchEnd') {
      actions.push(makeAction('resign', 'action.resign', { type: 'resign' }, { tone: 'danger', icon: 'flag' }));
    }
    return {
      gameId: 'ludo',
      youId: viewerId,
      phase: state.phase,
      turnId: turnSeat?.id ?? null,
      players: playerViews(state.seats, scores(state), turnSeat?.id ?? null, viewerId, () => ({ active: true })),
      hand: [],
      center: {},
      actions,
      prompt: promptFor(state, isTurn),
      extra: {
        colors: state.colors,
        colorLabels: state.colors.map((color) => COLOR_LABEL[color]),
        tokens: state.tokens.map((token) => ({
          ...token,
          coord: ludoTokenCoord(token),
          finished: token.progress >= FINISH,
        })),
        legalMoves: moves,
        dice: state.dice,
        lastRoll: state.lastRoll,
        sixStreak: state.sixStreak,
        captures: state.captures,
        homeCount: Object.fromEntries(state.seats.map((seat, index) => [seat.id, finishedCount(state, index)])),
      },
    };
  },

  act(state, action, actorId, _rng): ActResult<LudoState> {
    if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
    const turnSeat = state.seats[state.turn];
    if (!turnSeat || turnSeat.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };
    if (action.type === 'resign') {
      const winnerIndex = state.seats.findIndex((seat) => seat.id !== actorId);
      const winner = state.seats[winnerIndex];
      const finished: LudoState = {
        ...state,
        phase: 'matchEnd',
        result: {
          winnerIds: winner ? [winner.id] : [],
          scores: scores(state),
          reasonKey: 'result.reason.resign',
        },
      };
      return { state: finished, events: [event('log.resign', { p: turnSeat.name }, 'danger', actorId)] };
    }
    if (action.type === 'roll') {
      if (state.phase !== 'roll') return { state, events: [], error: 'error.invalidAction' };
      const die = Math.floor(state.rng() * 6) + 1;
      const sixStreak = die === 6 ? state.sixStreak + 1 : 0;
      const events = [event('log.ludoRoll', { p: turnSeat.name, n: die }, 'info', actorId)];
      if (sixStreak >= 3) {
        events.push(event('log.ludoThreeSixes', { p: turnSeat.name }, 'warn', actorId));
        return {
          state: {
            ...state,
            phase: 'roll',
            turn: nextSeat(state),
            dice: null,
            lastRoll: die,
            sixStreak: 0,
          },
          events,
        };
      }
      const moves = legalMoves(state, state.turn, die);
      if (moves.length === 0) {
        events.push(event('log.ludoNoMoves', { p: turnSeat.name }, 'warn', actorId));
        return {
          state: {
            ...state,
            phase: 'roll',
            turn: nextSeat(state),
            dice: null,
            lastRoll: die,
            sixStreak: 0,
          },
          events,
        };
      }
      return {
        state: { ...state, phase: 'move', dice: die, lastRoll: die, sixStreak },
        events,
      };
    }
    if (action.type !== 'move' || state.phase !== 'move' || !state.dice) {
      return { state, events: [], error: 'error.invalidAction' };
    }
    const tokenId = String(action.tokenId ?? '');
    const move = legalMoves(state, state.turn, state.dice).find((candidate) => candidate.tokenId === tokenId);
    if (!move) return { state, events: [], error: 'error.invalidMove' };
    const { state: next, captured, finished } = applyMove(state, move);
    const events = [
      event('log.ludoMove', { p: turnSeat.name, n: move.to }, 'info', actorId),
    ];
    if (captured.length > 0) events.push(event('log.ludoCapture', { p: turnSeat.name, n: captured.length }, 'success', actorId));
    if (finished) events.push(event('log.ludoFinish', { p: turnSeat.name }, 'magic', actorId));
    if (next.phase === 'matchEnd') events.push(event('log.won', { p: turnSeat.name }, 'magic', actorId));
    return { state: next, events };
  },

  bot(state, botId, difficulty: Difficulty, rng: Rng): GameAction | null {
    if (state.phase === 'matchEnd') return null;
    const turnSeat = state.seats[state.turn];
    if (!turnSeat || turnSeat.id !== botId) return null;
    if (state.phase === 'roll') return { type: 'roll' };
    const moves = state.dice ? legalMoves(state, state.turn, state.dice) : [];
    if (moves.length === 0) return null;
    const color = seatColor(state, state.turn);
    let best = -Infinity;
    let pool: LudoMove[] = [];
    for (const move of moves) {
      const captured = move.to <= 51 && !SAFE_TRACK.has(absolute(color, move.to))
        ? state.tokens.filter((token) => token.color !== color && token.progress >= 0 && token.progress <= 51 && absolute(token.color, token.progress) === absolute(color, move.to)).length
        : 0;
      const value = captured * 120 + (move.to >= FINISH ? 100 : 0) + (move.from === -1 ? 35 : 0) + move.to * 0.25 + (difficulty === 'easy' ? rng() * 15 : 0);
      if (value > best + 1) {
        best = value;
        pool = [move];
      } else if (Math.abs(value - best) <= 1) {
        pool.push(move);
      }
    }
    const chosen = pool[Math.floor(rng() * pool.length)] ?? moves[0];
    return chosen ? { type: 'move', tokenId: chosen.tokenId } : null;
  },

  result(state) {
    return state.result ?? null;
  },
};

export type { LudoState, LudoMove };
