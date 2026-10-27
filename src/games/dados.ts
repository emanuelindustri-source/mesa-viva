import type {
  ActResult,
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
import { rollDice } from '../core/rng';
import { event, playerViews } from './cards';
import { makeAction } from './helpers';

type Phase = 'bid' | 'reveal' | 'matchEnd';

interface Bid {
  qty: number;
  face: number;
  bidderId: string;
}

interface Reveal {
  dice: Record<string, number[]>;
  bid: Bid;
  challengerId: string;
  actual: number;
  bidTrue: boolean;
  loserId: string;
}

interface DadosState {
  seats: Seat[];
  options: GameOptions;
  scores: Record<string, number>;
  diceCount: Record<string, number>;
  dice: Record<string, number[]>;
  turn: number;
  bid?: Bid;
  phase: Phase;
  round: number;
  starter: number;
  eliminated: string[];
  reveal?: Reveal;
  winnerIds?: string[];
  result?: MatchResult;
  rng: Rng;
}

function aliveSeats(state: DadosState): Seat[] {
  return state.seats.filter((s) => (state.diceCount[s.id] ?? 0) > 0);
}

function totalDice(state: DadosState): number {
  return aliveSeats(state).reduce((total, s) => total + (state.diceCount[s.id] ?? 0), 0);
}

function nextAlive(state: DadosState, from: number): number {
  for (let step = 1; step <= state.seats.length; step += 1) {
    const idx = (from + step) % state.seats.length;
    const seat = state.seats[idx];
    if (seat && (state.diceCount[seat.id] ?? 0) > 0) return idx;
  }
  return from;
}

function rollAll(state: DadosState): DadosState {
  const dice: Record<string, number[]> = {};
  for (const seat of aliveSeats(state)) {
    dice[seat.id] = rollDice(state.rng, state.diceCount[seat.id] ?? 0);
  }
  return { ...state, dice };
}

function startRound(state: DadosState, starter: number): DadosState {
  const rolled = rollAll(state);
  return {
    ...rolled,
    round: state.round + 1,
    starter,
    turn: starter,
    bid: undefined,
    reveal: undefined,
    phase: 'bid',
  };
}

function matchEnd(state: DadosState, winnerId: string): DadosState {
  const result: MatchResult = {
    winnerIds: [winnerId],
    scores: Object.fromEntries(state.seats.map((s) => [s.id, state.diceCount[s.id] ?? 0])),
    reasonKey: 'result.reason.lastDie',
  };
  return { ...state, phase: 'matchEnd', winnerIds: [winnerId], result };
}

function resolveDoubt(state: DadosState, challengerId: string): { state: DadosState; events: ReturnType<typeof event>[]; error?: string } {
  if (!state.bid) return { state, events: [], error: 'error.noBid' };
  const allDice = aliveSeats(state).flatMap((s) => state.dice[s.id] ?? []);
  const actual = allDice.filter((v) => v === state.bid?.face).length;
  const bidTrue = actual >= state.bid.qty;
  const loserId = bidTrue ? challengerId : state.bid.bidderId;
  const loserSeat = state.seats.find((s) => s.id === loserId);
  const diceCount = {
    ...state.diceCount,
    [loserId]: Math.max(0, (state.diceCount[loserId] ?? 0) - 1),
  };
  const eliminated = (diceCount[loserId] ?? 0) === 0
    ? [...new Set([...state.eliminated, loserId])]
    : state.eliminated;
  const reveal: Reveal = {
    dice: state.dice,
    bid: state.bid,
    challengerId,
    actual,
    bidTrue,
    loserId,
  };
  const events = [
    event('log.doubt', { p: state.seats.find((s) => s.id === challengerId)?.name ?? '—', qty: state.bid.qty, face: state.bid.face }, 'danger', challengerId),
    event('log.reveal', { actual, qty: state.bid.qty, face: state.bid.face }, bidTrue ? 'warn' : 'success'),
    event('log.lostDie', { p: loserSeat?.name ?? '—' }, 'danger', loserId),
  ];
  if ((diceCount[loserId] ?? 0) === 0) {
    events.push(event('log.eliminated', { p: loserSeat?.name ?? '—' }, 'warn', loserId));
  }
  const alive = state.seats.filter((s) => (diceCount[s.id] ?? 0) > 0);
  if (alive.length <= 1) {
    const winner = alive[0];
    if (winner) {
      events.push(event('log.won', { p: winner.name }, 'magic', winner.id));
      return {
        state: matchEnd({ ...state, diceCount, eliminated, reveal, phase: 'reveal' }, winner.id),
        events,
      };
    }
  }
  return {
    state: {
      ...state,
      diceCount,
      eliminated,
      reveal,
      phase: 'reveal',
      turn: state.seats.findIndex((s) => s.id === loserId),
    },
    events,
  };
}

export const dadosModule: GameModule<DadosState> = {
  meta: {
    id: 'dados',
    nameKey: 'game.dados.name',
    descKey: 'game.dados.desc',
    rulesKey: 'game.dados.rules',
    icon: '🎲',
    category: 'dados',
    minPlayers: 2,
    maxPlayers: 6,
    avgMinutes: 10,
    accent: '#c97b2b',
    tagKeys: ['tag.mentira', 'tag.dados', 'tag.fiesta'],
  },

  create(seats, options, rng) {
    const base: DadosState = {
      seats: seats.map((s) => ({ ...s })),
      options,
      scores: Object.fromEntries(seats.map((s) => [s.id, 0])),
      diceCount: Object.fromEntries(seats.map((s) => [s.id, 5])),
      dice: {},
      turn: 0,
      phase: 'bid',
      round: 1,
      starter: 0,
      eliminated: [],
      rng,
    };
    return rollAll(base);
  },

  view(state, viewerId): GameView {
    const turnSeat = state.seats[state.turn];
    const isTurn = turnSeat?.id === viewerId;
    const total = totalDice(state);
    const actions: UIAction[] = [];
    if (state.phase === 'bid' && isTurn) {
      if (state.bid) {
        actions.push(
          makeAction('doubt', 'action.doubt', { type: 'doubt' }, { tone: 'danger', icon: 'hand' }),
        );
      }
    }
    if (state.phase === 'reveal' && isTurn) {
      actions.push(
        makeAction('continue', 'action.continue', { type: 'continue' }, { tone: 'primary', icon: 'arrow-right' }),
      );
    }
    let prompt: GameView['prompt'];
    if (state.phase === 'matchEnd') prompt = { key: 'prompt.matchEnd', tone: 'success' };
    else if (state.phase === 'reveal') {
      prompt = {
        key: 'prompt.dadosReveal',
        params: { actual: state.reveal?.actual ?? 0, qty: state.reveal?.bid.qty ?? 0, face: state.reveal?.bid.face ?? 0 },
        tone: 'warn',
      };
    } else if (isTurn) {
      prompt = state.bid
        ? { key: 'prompt.dadosRaise', params: { qty: state.bid.qty, face: state.bid.face }, tone: 'info' }
        : { key: 'prompt.dadosOpen', tone: 'info' };
    } else {
      prompt = { key: 'prompt.waitingPlayer', params: { player: turnSeat?.name ?? '—' }, tone: 'info' };
    }
    return {
      gameId: 'dados',
      youId: viewerId,
      phase: state.phase,
      turnId: state.phase === 'bid' ? (turnSeat?.id ?? null) : (state.seats[state.turn]?.id ?? null),
      players: playerViews(state.seats, state.scores, state.phase === 'bid' ? (turnSeat?.id ?? null) : null, viewerId, (seat) => ({
        diceLeft: state.diceCount[seat.id] ?? 0,
        active: (state.diceCount[seat.id] ?? 0) > 0,
      })),
      hand: [],
      handDice: (state.dice[viewerId] ?? []).slice(0, state.diceCount[viewerId] ?? 0).map((value, i) => ({ id: `mine-${i}`, value })),
      center: {
        dice: aliveSeats(state).flatMap((s) => {
          const values = state.dice[s.id] ?? [];
          const visible = state.phase === 'reveal' || s.id === viewerId;
          return values.map((value, i) => ({
            id: `${s.id}-${i}`,
            value: visible ? value : 0,
            hidden: !visible,
          }));
        }),
      },
      actions,
      prompt,
      extra: {
        bid: state.bid ?? null,
        totalDice: total,
        maxQty: total,
        round: state.round,
        reveal: state.reveal ?? null,
        eliminated: state.eliminated,
        isTurn,
      },
    };
  },

  act(state, action, actorId, _rng): ActResult<DadosState> {
    if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
    const turnSeat = state.seats[state.turn];
    if (!turnSeat || turnSeat.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };

    if (state.phase === 'reveal' && action.type === 'continue') {
      const alive = state.seats.filter((s) => (state.diceCount[s.id] ?? 0) > 0);
      if (alive.length <= 1) {
        const winner = alive[0];
        return winner
          ? { state: matchEnd(state, winner.id), events: [event('log.won', { p: winner.name }, 'magic', winner.id)] }
          : { state, events: [] };
      }
      const loserIndex = state.seats.findIndex((s) => s.id === actorId);
      const starter = (state.diceCount[actorId] ?? 0) > 0 ? loserIndex : nextAlive(state, loserIndex);
      const next = startRound(state, starter);
      return {
        state: next,
        events: [event('log.roundStart', { n: next.round }, 'info')],
      };
    }

    if (state.phase !== 'bid') return { state, events: [], error: 'error.notNow' };

    if (action.type === 'doubt') {
      if (!state.bid) return { state, events: [], error: 'error.noBid' };
      const res = resolveDoubt(state, actorId);
      return { state: res.state, events: res.events, ...(res.error ? { error: res.error } : {}) };
    }

    if (action.type === 'bid') {
      const qty = Math.floor(Number(action.qty ?? 0));
      const face = Math.floor(Number(action.face ?? 0));
      const total = totalDice(state);
      if (face < 1 || face > 6) return { state, events: [], error: 'error.invalidBid' };
      if (qty < 1 || qty > total) return { state, events: [], error: 'error.invalidBid' };
      if (state.bid) {
        const higher = qty > state.bid.qty || (qty === state.bid.qty && face > state.bid.face);
        if (!higher) return { state, events: [], error: 'error.bidTooLow' };
      }
      const bid: Bid = { qty, face, bidderId: actorId };
      const next = {
        ...state,
        bid,
        turn: nextAlive(state, state.turn),
      };
      return {
        state: next,
        events: [event('log.bid', { p: turnSeat.name, qty, face }, 'info', actorId)],
      };
    }

    return { state, events: [], error: 'error.invalidAction' };
  },

  bot(state, botId, difficulty: Difficulty, _rng: Rng): GameAction | null {
    if (state.phase === 'matchEnd') return null;
    const seat = state.seats[state.turn];
    if (!seat || seat.id !== botId) return null;
    if (state.phase === 'reveal') return { type: 'continue' };
    const mine = state.dice[botId] ?? [];
    const total = totalDice(state);
    const counts = [1, 2, 3, 4, 5, 6].map((face) => ({
      face,
      mine: mine.filter((v) => v === face).length,
    }));
    counts.sort((a, b) => b.mine - a.mine || b.face - a.face);
    const best = counts[0] ?? { face: 1, mine: 1 };
    const others = Math.max(0, total - mine.length);
    const expected = best.mine + others / 6;
    const sd = Math.sqrt(others * (1 / 6) * (5 / 6));

    const bid = state.bid;
    if (bid) {
      const risk = difficulty === 'hard' ? 1.05 : difficulty === 'normal' ? 1.3 : 1.6;
      const threshold = expected + sd * risk;
      if (bid.qty > threshold || bid.qty >= total) {
        return { type: 'doubt' };
      }
      let qty = bid.qty;
      let face = best.face;
      if (face <= bid.face) {
        // Busca otra cara con presencia propia.
        const alt = counts.find((c) => c.face > bid.face && c.mine > 0);
        if (alt) face = alt.face;
        else {
          qty = bid.qty + 1;
          face = best.face;
        }
      }
      if (qty < bid.qty) qty = bid.qty;
      if (qty === bid.qty && face <= bid.face) qty = bid.qty + 1;
      if (qty > total) return { type: 'doubt' };
      if (qty < 1) qty = 1;
      return { type: 'bid', qty, face };
    }

    const aggressive = difficulty === 'hard' ? 1 : difficulty === 'normal' ? 0 : -1;
    let qty = Math.max(1, Math.round(expected + aggressive * 0.3));
    if (qty > total) qty = total;
    const face = best.face;
    return { type: 'bid', qty, face };
  },

  result(state) {
    return state.result ?? null;
  },
};

export type { DadosState, Bid, Reveal };
