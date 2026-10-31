import type {
  ActResult,
  Difficulty,
  GameAction,
  GameModule,
  GameView,
  MatchResult,
  Seat,
  UIAction,
} from '../core/types';
import type { Rng } from '../core/types';
import { event, playerViews } from './cards';
import { makeAction, prompt } from './helpers';

type Side = 0 | 1;
type Phase = 'play' | 'matchEnd';

export interface UrPiece {
  id: string;
  side: Side;
  position: number;
}

export interface UrState {
  seats: Seat[];
  pieces: UrPiece[];
  turn: Side;
  dice: number;
  phase: Phase;
  winner?: Side;
  lastPiece?: string;
  lastFrom: number;
  lastTo: number;
  captured: boolean;
  extraTurn: boolean;
  history: Array<ReturnType<typeof event>>;
  result?: MatchResult;
  rng: Rng;
}

const TRACK = 20;
const ROSETTES = new Set([0, 4, 8, 13, 16]);
const PIECES = 4;

function other(side: Side): Side {
  return side === 0 ? 1 : 0;
}

function label(position: number): string {
  return position < 0 ? 'inicio' : String(position);
}

export function urLegalMoves(state: UrState): UrPiece[] {
  if (state.phase === 'matchEnd' || state.dice < 1 || state.dice > 4) return [];
  return state.pieces.filter((piece) => piece.side === state.turn
    && piece.position < TRACK
    && (piece.position < 0 || piece.position + state.dice <= TRACK));
}

function winnerIfAny(pieces: UrPiece[], side: Side): Side | undefined {
  return pieces.filter((piece) => piece.side === side).every((piece) => piece.position === TRACK) ? side : undefined;
}

function finishResult(state: UrState, winner: Side | undefined, finished: Record<string, number>): MatchResult {
  return {
    winnerIds: winner === undefined ? [] : [state.seats[winner]?.id ?? ''],
    draw: winner === undefined,
    scores: finished,
    reasonKey: winner === undefined ? 'result.reason.draw' : 'result.reason.urFinish',
  };
}

export const urModule: GameModule<UrState> = {
  meta: {
    id: 'ur',
    nameKey: 'game.ur.name',
    descKey: 'game.ur.desc',
    rulesKey: 'game.ur.rules',
    icon: '🏺',
    category: 'tablero',
    minPlayers: 2,
    maxPlayers: 2,
    avgMinutes: 25,
    accent: '#c98a3a',
    tagKeys: ['tag.clasico', 'tag.cultural'],
  },

  create(seats, _options, rng): UrState {
    const pieces: UrPiece[] = [];
    for (let i = 0; i < PIECES; i += 1) pieces.push({ id: `w${i}`, side: 0, position: -1 });
    for (let i = 0; i < PIECES; i += 1) pieces.push({ id: `b${i}`, side: 1, position: -1 });
    return {
      seats: seats.slice(0, 2),
      pieces,
      turn: 0,
      dice: 0,
      phase: 'play',
      lastFrom: -1,
      lastTo: -1,
      captured: false,
      extraTurn: false,
      history: [event('log.urStart', {}, 'info')],
      rng,
    };
  },

  view(state, viewerId): GameView {
    const isTurn = state.seats[state.turn]?.id === viewerId && state.phase === 'play';
    const finished = {
      [state.seats[0]?.id ?? 'p0']: state.pieces.filter((piece) => piece.side === 0 && piece.position === TRACK).length,
      [state.seats[1]?.id ?? 'p1']: state.pieces.filter((piece) => piece.side === 1 && piece.position === TRACK).length,
    };
    const actions: UIAction[] = [];
    if (isTurn && state.dice === 0) {
      actions.push(makeAction('roll', 'action.rollDice', { type: 'roll' }, { tone: 'primary', icon: 'dices' }));
    }
    if (state.phase === 'play') {
      actions.push(makeAction('resign', 'action.resign', { type: 'resign' }, { tone: 'ghost' }));
    }
    return {
      gameId: 'ur',
      youId: viewerId,
      phase: state.phase,
      turnId: state.seats[state.turn]?.id ?? null,
      players: playerViews(state.seats, finished, state.seats[state.turn]?.id ?? null, viewerId, (seat) => ({
        cardsLeft: finished[seat.id] ?? 0,
        active: state.phase !== 'matchEnd',
      })),
      hand: [],
      center: {},
      actions,
      prompt: state.phase === 'matchEnd'
        ? prompt('prompt.matchEnd', {}, 'success')
        : isTurn
          ? state.dice === 0
            ? prompt('prompt.urRoll', {}, 'info')
            : prompt('prompt.urMove', { n: state.dice }, 'info')
          : prompt('prompt.waitingPlayer', { player: state.seats[state.turn]?.name ?? '—' }, 'info'),
      extra: {
        pieces: state.pieces,
        dice: state.dice,
        rosettes: [...ROSETTES],
        track: TRACK,
        finished,
        legal: urLegalMoves(state).map((piece) => piece.id),
        lastPiece: state.lastPiece ?? null,
        lastFrom: state.lastFrom,
        lastTo: state.lastTo,
        captured: state.captured,
        extraTurn: state.extraTurn,
      },
    };
  },

  act(state, action, actorId): ActResult<UrState> {
    if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
    if (state.seats[state.turn]?.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };
    const before = state.history.length;
    if (action.type === 'resign') {
      const winner = other(state.turn);
      const finished = {
        [state.seats[0]?.id ?? 'p0']: state.pieces.filter((piece) => piece.side === 0 && piece.position === TRACK).length,
        [state.seats[1]?.id ?? 'p1']: state.pieces.filter((piece) => piece.side === 1 && piece.position === TRACK).length,
      };
      const next: UrState = {
        ...state,
        phase: 'matchEnd',
        winner,
        result: finishResult(state, winner, finished),
        history: [...state.history, event('log.urResign', { p: state.seats[state.turn]?.name ?? '—' }, 'warn', actorId)],
      };
      return { state: next, events: next.history.slice(before) };
    }
    if (action.type === 'roll') {
      if (state.dice !== 0) return { state, events: [], error: 'error.invalidAction' };
      const value = 1 + Math.floor(state.rng() * 4);
      const rolled: UrState = {
        ...state,
        dice: value,
        lastPiece: undefined,
        captured: false,
        extraTurn: false,
        history: [...state.history, event('log.urRoll', { p: state.seats[state.turn]?.name ?? '—', value }, 'info', actorId)],
      };
      if (urLegalMoves(rolled).length === 0) {
        const passed: UrState = {
          ...rolled,
          dice: 0,
          turn: other(state.turn),
          history: [...rolled.history, event('log.urNoMove', { p: state.seats[state.turn]?.name ?? '—' }, 'warn', actorId)],
        };
        return { state: passed, events: passed.history.slice(before) };
      }
      return { state: rolled, events: rolled.history.slice(before) };
    }
    if (action.type !== 'move') return { state, events: [], error: 'error.invalidAction' };
    if (state.dice < 1 || state.dice > 4) return { state, events: [], error: 'error.invalidAction' };
    const pieceId = String(action.pieceId ?? '');
    const piece = state.pieces.find((value) => value.id === pieceId);
    if (!piece || piece.side !== state.turn) return { state, events: [], error: 'error.invalidAction' };
    const target = piece.position < 0 ? state.dice - 1 : piece.position + state.dice;
    if (target > TRACK) return { state, events: [], error: 'error.invalidAction' };
    let captured = false;
    const pieces = state.pieces.map((value) => (value.id === pieceId ? { ...value, position: target } : value));
    if (target < TRACK && !ROSETTES.has(target)) {
      for (const value of pieces) {
        if (value.side !== state.turn && value.position === target) {
          value.position = -1;
          captured = true;
        }
      }
    }
    const winner = winnerIfAny(pieces, state.turn);
    const extra = ROSETTES.has(target) || captured || target === TRACK;
    const finished = {
      [state.seats[0]?.id ?? 'p0']: pieces.filter((value) => value.side === 0 && value.position === TRACK).length,
      [state.seats[1]?.id ?? 'p1']: pieces.filter((value) => value.side === 1 && value.position === TRACK).length,
    };
    const next: UrState = {
      ...state,
      pieces,
      dice: 0,
      turn: winner === undefined && !extra ? other(state.turn) : state.turn,
      phase: winner === undefined ? 'play' : 'matchEnd',
      winner,
      lastPiece: pieceId,
      lastFrom: piece.position,
      lastTo: target,
      captured,
      extraTurn: extra,
      result: winner === undefined ? undefined : finishResult(state, winner, finished),
      history: [...state.history, event('log.urMove', {
        p: state.seats[state.turn]?.name ?? '—',
        from: label(piece.position),
        to: target === TRACK ? 'meta' : String(target),
        extra: extra ? ' +' : '',
      }, extra ? 'success' : 'info', actorId)],
    };
    return { state: next, events: next.history.slice(before) };
  },

  bot(state, botId, _difficulty: Difficulty, rng): GameAction | null {
    if (state.phase === 'matchEnd') return null;
    if (state.seats[state.turn]?.id !== botId) return null;
    if (state.dice === 0) return { type: 'roll' };
    const moves = urLegalMoves(state);
    if (moves.length === 0) return { type: 'roll' };
    const first = moves[0];
    if (!first) return { type: 'roll' };
    let best = first;
    let bestValue = -Infinity;
    for (const piece of moves) {
      const target = piece.position < 0 ? state.dice - 1 : piece.position + state.dice;
      let value = target;
      if (target === TRACK) value += 1000;
      if (ROSETTES.has(target) && target < TRACK) value += 80;
      if (target < TRACK && !ROSETTES.has(target)) {
        value += state.pieces.filter((rival) => rival.side !== state.turn && rival.position === target).length * 200;
      }
      value += rng();
      if (value > bestValue) {
        bestValue = value;
        best = piece;
      }
    }
    return { type: 'move', pieceId: best.id };
  },

  result(state): MatchResult | null {
    return state.result ?? null;
  },
};
