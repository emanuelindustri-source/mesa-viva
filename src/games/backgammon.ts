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

type BgSide = 'w' | 'b';

interface BgMove {
  /** -1 = barra; 24 = fuera del tablero. */
  from: number;
  to: number;
  die: number;
  hit?: boolean;
}

interface BackgammonState {
  seats: Seat[];
  options: GameOptions;
  points: number[];
  bar: Record<BgSide, number>;
  off: Record<BgSide, number>;
  turn: BgSide;
  phase: 'roll' | 'move' | 'matchEnd';
  dice: [number, number] | null;
  lastDice: [number, number] | null;
  movesLeft: number[];
  lastMove?: BgMove;
  result?: MatchResult;
  rng: Rng;
}

const HOME_LIMIT = 15;
const sideSeat = (state: BackgammonState, side: BgSide): Seat | undefined =>
  state.seats[side === 'w' ? 0 : 1];

const other = (side: BgSide): BgSide => (side === 'w' ? 'b' : 'w');

function initialPoints(): number[] {
  const points = Array.from({ length: 24 }, () => 0);
  // Blancas: 2 en 24, 5 en 13, 3 en 8, 5 en 6.
  points[23] = 2;
  points[12] = 5;
  points[7] = 3;
  points[5] = 5;
  // Negras: 2 en 1, 5 en 12, 3 en 17, 5 en 19.
  points[0] = -2;
  points[11] = -5;
  points[16] = -3;
  points[18] = -5;
  return points;
}

function canLand(points: number[], side: BgSide, to: number): boolean {
  if (to < 0 || to > 23) return false;
  const count = points[to] ?? 0;
  return side === 'w' ? count >= -1 : count <= 1;
}

function allHome(state: BackgammonState, side: BgSide): boolean {
  if (state.bar[side] > 0) return false;
  if (side === 'w') return state.points.slice(6).every((value) => value <= 0);
  return state.points.slice(0, 18).every((value) => value >= 0);
}

function canBearOff(state: BackgammonState, side: BgSide, from: number, die: number): boolean {
  if (!allHome(state, side)) return false;
  if (side === 'w') {
    if (from > 5 || from < 0) return false;
    const point = from + 1;
    if (die < point) return false;
    if (die === point) return true;
    return state.points.slice(from + 1, 6).every((value) => value <= 0);
  }
  if (from < 18 || from > 23) return false;
  const point = 24 - from;
  if (die < point) return false;
  if (die === point) return true;
  return state.points.slice(from + 1, 24).every((value) => value >= 0);
}

function legalMoves(state: BackgammonState, side: BgSide = state.turn): BgMove[] {
  if (state.phase !== 'move' || state.movesLeft.length === 0) return [];
  const moves: BgMove[] = [];
  const seen = new Set<number>();
  for (const die of state.movesLeft) {
    if (seen.has(die)) continue;
    seen.add(die);
    if (state.bar[side] > 0) {
      const to = side === 'w' ? 24 - die : die - 1;
      if (canLand(state.points, side, to)) {
        moves.push({
          from: -1,
          to,
          die,
          ...(state.points[to] === (side === 'w' ? -1 : 1) ? { hit: true } : {}),
        });
      }
      continue;
    }
    for (let from = 0; from < 24; from += 1) {
      const count = state.points[from] ?? 0;
      if (side === 'w' ? count <= 0 : count >= 0) continue;
      const to = side === 'w' ? from - die : from + die;
      if (to >= 0 && to <= 23) {
        if (!canLand(state.points, side, to)) continue;
        moves.push({
          from,
          to,
          die,
          ...(state.points[to] === (side === 'w' ? -1 : 1) ? { hit: true } : {}),
        });
      } else if (canBearOff(state, side, from, die)) {
        moves.push({ from, to: 24, die });
      }
    }
  }
  return moves;
}

function applyMove(state: BackgammonState, move: BgMove): BackgammonState {
  const side = state.turn;
  const sign = side === 'w' ? 1 : -1;
  const points = state.points.slice();
  const bar = { ...state.bar };
  const off = { ...state.off };
  if (move.from === -1) bar[side] = Math.max(0, bar[side] - 1);
  else points[move.from] = (points[move.from] ?? 0) - sign;
  if (move.to === 24) {
    off[side] += 1;
  } else {
    if (points[move.to] === -sign) {
      bar[side === 'w' ? 'b' : 'w'] += 1;
      points[move.to] = 0;
    }
    points[move.to] = (points[move.to] ?? 0) + sign;
  }
  const movesLeft = state.movesLeft.slice();
  const index = movesLeft.indexOf(move.die);
  if (index >= 0) movesLeft.splice(index, 1);
  const next: BackgammonState = {
    ...state,
    points,
    bar,
    off,
    movesLeft,
    dice: null,
    lastMove: move,
  };
  if (off[side] >= HOME_LIMIT) {
    return { ...next, phase: 'matchEnd', result: resultFor(next, side) };
  }
  if (movesLeft.length > 0 && legalMoves({ ...next, phase: 'move' }, side).length > 0) {
    return { ...next, phase: 'move', turn: side };
  }
  return {
    ...next,
    phase: 'roll',
    turn: other(side),
    movesLeft: [],
    dice: null,
  };
}

function resultFor(state: BackgammonState, winner: BgSide): MatchResult {
  const seat = sideSeat(state, winner);
  return {
    winnerIds: seat ? [seat.id] : [],
    scores: {
      [state.seats[0]?.id ?? 'w']: state.off.w,
      [state.seats[1]?.id ?? 'b']: state.off.b,
    },
    reasonKey: 'result.reason.backgammonOff',
    stats: {
      whiteOff: state.off.w,
      blackOff: state.off.b,
      whiteBar: state.bar.w,
      blackBar: state.bar.b,
    },
  };
}

function pipCount(points: number[], bar: number, side: BgSide): number {
  let result = bar * 25;
  for (let i = 0; i < 24; i += 1) {
    const count = points[i] ?? 0;
    if (side === 'w' && count > 0) result += count * (i + 1);
    if (side === 'b' && count < 0) result += -count * (24 - i);
  }
  return result;
}

function promptFor(state: BackgammonState, isTurn: boolean): PromptView {
  if (state.phase === 'matchEnd') return { key: 'prompt.matchEnd', tone: 'success' };
  if (!isTurn) {
    const seat = sideSeat(state, state.turn);
    return { key: 'prompt.waitingPlayer', params: { player: seat?.name ?? '—' }, tone: 'info' };
  }
  if (state.phase === 'roll') return { key: 'prompt.bgRoll', tone: 'info' };
  if (legalMoves(state).length === 0) return { key: 'prompt.bgNoMoves', tone: 'warn' };
  return { key: 'prompt.bgMove', params: { n: state.movesLeft.join(' · ') }, tone: 'info' };
}

export const backgammonModule: GameModule<BackgammonState> = {
  meta: {
    id: 'backgammon',
    nameKey: 'game.backgammon.name',
    descKey: 'game.backgammon.desc',
    rulesKey: 'game.backgammon.rules',
    icon: '🎯',
    category: 'tablero',
    minPlayers: 2,
    maxPlayers: 2,
    avgMinutes: 20,
    accent: '#8a6a3d',
    tagKeys: ['tag.clasico', 'tag.dados', 'tag.duelo'],
  },

  create(seats, options, rng) {
    return {
      seats: seats.slice(0, 2).map((seat) => ({ ...seat })),
      options,
      points: initialPoints(),
      bar: { w: 0, b: 0 },
      off: { w: 0, b: 0 },
      turn: 'w',
      phase: 'roll',
      dice: null,
      lastDice: null,
      movesLeft: [],
      rng,
    };
  },

  view(state, viewerId): GameView {
    const turnSeat = sideSeat(state, state.turn);
    const isTurn = turnSeat?.id === viewerId;
    const moves = isTurn ? legalMoves(state) : [];
    const actions: UIAction[] = [];
    if (state.phase === 'roll' && isTurn) {
      actions.push(makeAction('roll', 'action.rollDice', { type: 'roll' }, { tone: 'primary', icon: 'dice' }));
    }
    if (state.phase === 'move' && isTurn && moves.length === 0) {
      actions.push(makeAction('pass', 'action.pass', { type: 'pass' }, { tone: 'secondary', icon: 'arrow-right' }));
    }
    if (state.phase !== 'matchEnd') {
      actions.push(makeAction('resign', 'action.resign', { type: 'resign' }, { tone: 'danger', icon: 'flag' }));
    }
    return {
      gameId: 'backgammon',
      youId: viewerId,
      phase: state.phase,
      turnId: turnSeat?.id ?? null,
      players: playerViews(
        state.seats,
        { [state.seats[0]?.id ?? 'w']: state.off.w, [state.seats[1]?.id ?? 'b']: state.off.b },
        turnSeat?.id ?? null,
        viewerId,
        () => ({ active: true }),
      ),
      hand: [],
      center: {},
      actions,
      prompt: promptFor(state, isTurn),
      extra: {
        points: state.points,
        bar: state.bar,
        off: state.off,
        dice: state.dice,
        lastDice: state.lastDice,
        movesLeft: state.movesLeft,
        legalMoves: moves,
        turn: state.turn,
        home: { w: allHome(state, 'w'), b: allHome(state, 'b') },
        pip: { w: pipCount(state.points, state.bar.w, 'w'), b: pipCount(state.points, state.bar.b, 'b') },
      },
    };
  },

  act(state, action, actorId, _rng): ActResult<BackgammonState> {
    if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
    const turnSeat = sideSeat(state, state.turn);
    if (!turnSeat || turnSeat.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };
    if (action.type === 'resign') {
      const winner = sideSeat(state, other(state.turn));
      return {
        state: {
          ...state,
          phase: 'matchEnd',
          result: {
            winnerIds: winner ? [winner.id] : [],
            scores: {
              [state.seats[0]?.id ?? 'w']: state.off.w,
              [state.seats[1]?.id ?? 'b']: state.off.b,
            },
            reasonKey: 'result.reason.resign',
          },
        },
        events: [event('log.resign', { p: turnSeat.name }, 'danger', actorId)],
      };
    }
    if (action.type === 'roll') {
      if (state.phase !== 'roll') return { state, events: [], error: 'error.invalidAction' };
      const d1 = Math.floor(state.rng() * 6) + 1;
      const d2 = Math.floor(state.rng() * 6) + 1;
      const dice: [number, number] = [d1, d2];
      const movesLeft = d1 === d2 ? [d1, d1, d1, d1] : [d1, d2];
      const rolled: BackgammonState = {
        ...state,
        dice,
        lastDice: dice,
        movesLeft,
        phase: 'move',
      };
      const events = [event('log.bgRoll', { p: turnSeat.name, a: d1, b: d2 }, 'info', actorId)];
      if (legalMoves(rolled).length === 0) {
        events.push(event('log.bgNoMoves', { p: turnSeat.name }, 'warn', actorId));
        return {
          state: {
            ...rolled,
            phase: 'roll',
            turn: other(state.turn),
            dice: null,
            movesLeft: [],
          },
          events,
        };
      }
      return { state: rolled, events };
    }
    if (action.type === 'pass') {
      if (state.phase !== 'move' || legalMoves(state).length > 0) {
        return { state, events: [], error: 'error.invalidAction' };
      }
      return {
        state: { ...state, phase: 'roll', turn: other(state.turn), dice: null, movesLeft: [] },
        events: [event('log.bgNoMoves', { p: turnSeat.name }, 'warn', actorId)],
      };
    }
    if (action.type !== 'move' || state.phase !== 'move') {
      return { state, events: [], error: 'error.invalidAction' };
    }
    const from = Number(action.from);
    const to = Number(action.to);
    const move = legalMoves(state).find((candidate) => candidate.from === from && candidate.to === to);
    if (!move) return { state, events: [], error: 'error.invalidMove' };
    const next = applyMove(state, move);
    const events = [event('log.bgMove', {
      p: turnSeat.name,
      from: from === -1 ? 'bar' : String(from + 1),
      to: to === 24 ? 'off' : String(to + 1),
    }, 'info', actorId)];
    if (move.hit) events.push(event('log.bgHit', { p: turnSeat.name }, 'success', actorId));
    if (to === 24) events.push(event('log.bgBearOff', { p: turnSeat.name }, 'magic', actorId));
    if (next.phase === 'matchEnd') events.push(event('log.won', { p: turnSeat.name }, 'magic', actorId));
    return { state: next, events };
  },

  bot(state, botId, difficulty: Difficulty, rng: Rng): GameAction | null {
    if (state.phase === 'matchEnd') return null;
    const turnSeat = sideSeat(state, state.turn);
    if (!turnSeat || turnSeat.id !== botId) return null;
    if (state.phase === 'roll') return { type: 'roll' };
    const moves = legalMoves(state);
    if (moves.length === 0) return { type: 'pass' };
    let best = -Infinity;
    let pool: BgMove[] = [];
    for (const move of moves) {
      const value = (move.hit ? 120 : 0)
        + (move.to === 24 ? 110 : 0)
        + (move.from === -1 ? 70 : 0)
        + (move.to >= 18 ? 20 : 0)
        + move.die
        + (difficulty === 'easy' ? rng() * 20 : 0);
      if (value > best + 1) {
        best = value;
        pool = [move];
      } else if (Math.abs(value - best) <= 1) {
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

export type { BackgammonState, BgMove, BgSide };
