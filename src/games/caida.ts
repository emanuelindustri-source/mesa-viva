import type {
  ActResult,
  CardView,
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
import { shuffle, pick } from '../core/rng';
import {
  caidaRankPoints,
  event,
  nextIndex,
  playerName,
  playerViews,
  spanishDeck,
  sortCards,
} from './cards';
import { makeAction } from './helpers';

type Phase = 'entrega' | 'play' | 'handEnd' | 'matchEnd';

interface CantoResult {
  key: string;
  points: number;
  instant: boolean;
  rank?: number;
}

interface DeliveryResult {
  cards: CardView[];
  ok: boolean;
  order: number[];
  points: number;
  toPlayer: string;
}

interface CaptureResult {
  playerId: string;
  cards: CardView[];
  points: number;
  mesa: boolean;
  rank: number;
}

interface CaidaState {
  seats: Seat[];
  options: GameOptions;
  scores: Record<string, number>;
  hands: Record<string, CardView[]>;
  deck: CardView[];
  table: CardView[];
  captured: Record<string, CardView[]>;
  claimedHands: Record<string, number[]>;
  dealer: number;
  turn: number;
  handNo: number;
  target: number;
  teams: boolean;
  phase: Phase;
  delivery?: DeliveryResult;
  lastCapture?: CaptureResult;
  lastCanto?: { playerId: string; key: string; points: number; instant: boolean };
  lastPlayed?: { playerId: string; card: CardView };
  lastCapturer?: string;
  handStartScores: Record<string, number>;
  playedThisHand: Record<string, boolean>;
  lastHandSummary?: Array<{ playerId: string; name: string; cards: number; points: number; score: number }>;
  winnerIds?: string[];
  result?: MatchResult;
  rng: Rng;
}

const CANTO_NAMES: Record<string, string> = {
  ronda: 'canto.ronda',
  trivilin: 'canto.trivilin',
  patrulla: 'canto.patrulla',
  vigia: 'canto.vigia',
  registro: 'canto.registro',
  registrico: 'canto.registrico',
  casaChica: 'canto.casaChica',
  casaGrande: 'canto.casaGrande',
};

function cantoLabel(key: string): string {
  return CANTO_NAMES[key] ?? 'canto.ronda';
}

function rankPoints(rank: number): number {
  return caidaRankPoints(rank);
}

/** Valora la mano y devuelve el mejor canto disponible. */
export function detectCanto(hand: CardView[]): CantoResult | null {
  if (hand.length < 3) return null;
  const ranks = hand.map((c) => c.rank).sort((a, b) => a - b);
  const counts = new Map<number, number>();
  for (const r of ranks) counts.set(r, (counts.get(r) ?? 0) + 1);
  const triple = [...counts.entries()].find(([, n]) => n === 3);
  const pair = [...counts.entries()].find(([, n]) => n === 2);
  const has = (r: number) => counts.has(r);

  if (triple) {
    return {
      key: 'trivilin',
      points: 3,
      instant: false,
      rank: triple[0],
    };
  }
  if (has(1) && has(11) && has(11) && counts.get(11) === 2) {
    return { key: 'casaChica', points: 5, instant: false };
  }
  if (has(1) && has(12) && counts.get(12) === 2) {
    return { key: 'casaGrande', points: 6, instant: false };
  }
  if (has(1) && has(10) && has(11)) return { key: 'registrico', points: 5, instant: false };
  if (has(1) && has(11) && has(12)) return { key: 'registro', points: 4, instant: false };

  if (pair) {
    const pairRank = pair[0];
    const third = ranks.find((r) => r !== pairRank) ?? 0;
    const adjacent = third === pairRank - 1 || third === pairRank + 1;
    if (adjacent) return { key: 'vigia', points: 4, instant: false, rank: pairRank };
    return { key: 'ronda', points: Math.min(2, rankPoints(pairRank)), instant: false, rank: pairRank };
  }

  const unique = [...counts.keys()].sort((a, b) => a - b);
  if (unique.length === 3) {
    const [a, b, c] = unique;
    if (a !== undefined && b !== undefined && c !== undefined) {
      const consecutive =
        (b === a + 1 && c === b + 1) ||
        (a === 1 && b === 2 && c === 3) ||
        (a === 5 && b === 6 && c === 7) ||
        (a === 10 && b === 11 && c === 12);
      if (consecutive) return { key: 'patrulla', points: 3, instant: false };
    }
  }
  return null;
}

function scoreFor(state: CaidaState, seatId: string): number {
  return state.scores[seatId] ?? 0;
}

function teamTotal(state: CaidaState, team: number): number {
  return state.seats.filter((s) => s.team === team).reduce((total, s) => total + scoreFor(state, s.id), 0);
}

function maxScore(state: CaidaState): number {
  if (state.teams) {
    const teams = [...new Set(state.seats.map((s) => s.team ?? 0))];
    return Math.max(0, ...teams.map((t) => teamTotal(state, t)));
  }
  return Math.max(0, ...state.seats.map((s) => scoreFor(state, s.id)));
}

function finishMatch(state: CaidaState, reason = 'result.reason.caidaPoints', forceWinnerId?: string): CaidaState {
  const forced = forceWinnerId ? state.seats.find((s) => s.id === forceWinnerId) : undefined;
  if (forced) {
    return {
      ...state,
      phase: 'matchEnd',
      winnerIds: [forced.id],
      result: {
        winnerIds: [forced.id],
        draw: false,
        scores: Object.fromEntries(
          state.seats.map((s) => [
            s.id,
            state.teams ? teamTotal(state, s.team ?? 0) : scoreFor(state, s.id),
          ]),
        ),
        reasonKey: reason,
      },
    };
  }
  const winners: Seat[] = [];
  if (state.teams) {
    const teams = [...new Set(state.seats.map((s) => s.team ?? 0))];
    let best = -1;
    for (const t of teams) best = Math.max(best, teamTotal(state, t));
    for (const t of teams) {
      if (teamTotal(state, t) === best) winners.push(...state.seats.filter((s) => (s.team ?? 0) === t));
    }
  } else {
    const best = maxScore(state);
    winners.push(...state.seats.filter((s) => scoreFor(state, s.id) === best));
  }
  const winnerIds = winners.map((s) => s.id);
  return {
    ...state,
    phase: 'matchEnd',
    winnerIds,
    result: {
      winnerIds,
      draw: winnerIds.length > 1,
      scores: Object.fromEntries(state.seats.map((s) => [s.id, state.teams ? teamTotal(state, s.team ?? 0) : scoreFor(state, s.id)])),
      reasonKey: reason,
    },
  };
}

function checkTarget(state: CaidaState): CaidaState {
  return maxScore(state) >= state.target ? finishMatch(state) : state;
}

function newHand(state: CaidaState, dealer: number): CaidaState {
  const deck = shuffle(spanishDeck(), state.rng);
  return {
    ...state,
    deck,
    table: [],
    hands: Object.fromEntries(state.seats.map((s) => [s.id, []])),
    captured: Object.fromEntries(state.seats.map((s) => [s.id, []])),
    claimedHands: Object.fromEntries(state.seats.map((s) => [s.id, []])),
    dealer,
    turn: nextIndex(dealer, state.seats.length),
    handNo: state.handNo + 1,
    phase: 'entrega',
    delivery: undefined,
    lastCapture: undefined,
    lastCanto: undefined,
    lastPlayed: undefined,
    lastCapturer: undefined,
    handStartScores: { ...state.scores },
    playedThisHand: Object.fromEntries(state.seats.map((s) => [s.id, false])),
    lastHandSummary: undefined,
  };
}

interface InternalState extends CaidaState {
  rng: Rng;
}

function startDelivery(state: InternalState): { state: InternalState; events: ReturnType<typeof event>[] } {
  const dealer = state.seats[state.dealer];
  if (!dealer) return { state, events: [] };
  const deck = state.deck.slice();
  const cards = deck.splice(0, 4);
  const ranks = cards.map((c) => c.rank);
  const asc = [1, 2, 3, 4];
  const desc = [4, 3, 2, 1];
  const ok = ranks.every((r, i) => r === asc[i]) || ranks.every((r, i) => r === desc[i]);
  const points = ok ? 2 : 1;
  const recipient = ok ? dealer : (state.seats[nextIndex(state.dealer, state.seats.length)] as Seat);
  const scores = { ...state.scores, [recipient.id]: (state.scores[recipient.id] ?? 0) + points };
  const delivery: DeliveryResult = {
    cards,
    ok,
    order: ranks,
    points,
    toPlayer: recipient.id,
  };

  const hands: Record<string, CardView[]> = Object.fromEntries(state.seats.map((s) => [s.id, []]));
  const cursor = nextIndex(state.dealer, state.seats.length);
  for (let round = 0; round < 3; round += 1) {
    for (let i = 0; i < state.seats.length; i += 1) {
      const seat = state.seats[(cursor + i) % state.seats.length];
      if (!seat) continue;
      const card = deck.shift();
      if (card) hands[seat.id]?.push(card);
    }
  }

  const events = [
    event(
      ok ? 'log.deliveryOk' : 'log.deliveryFail',
      { p: dealer.name, cards: cards.map((c) => c.rank).join(' · '), points, to: recipient.name },
      ok ? 'success' : 'warn',
      dealer.id,
    ),
  ];

  const nextState: InternalState = {
    ...state,
    deck,
    scores,
    hands,
    delivery,
    phase: 'play',
    turn: nextIndex(state.dealer, state.seats.length),
    lastCapture: undefined,
    lastCanto: undefined,
    lastPlayed: undefined,
    lastCapturer: undefined,
  };
  return { state: checkTarget(nextState), events };
}

function endHand(state: InternalState): { state: InternalState; events: ReturnType<typeof event>[] } {
  const events = [event('log.handEnd', {}, 'info')];
  let captured = { ...state.captured };
  let table = state.table;
  if (table.length > 0 && state.lastCapturer) {
    const owner = state.seats.find((seat) => seat.id === state.lastCapturer);
    if (owner) {
      captured = {
        ...captured,
        [owner.id]: [...(captured[owner.id] ?? []), ...table],
      };
      events.push(event('log.tableSweep', { p: owner.name, n: table.length }, 'info', owner.id));
    }
  }
  table = [];
  const scores = { ...state.scores };
  for (const seat of state.seats) {
    const count = captured[seat.id]?.length ?? 0;
    const pts = Math.floor(count / 10);
    if (pts > 0) {
      scores[seat.id] = (scores[seat.id] ?? 0) + pts;
      events.push(event('log.cardsCounted', { p: seat.name, n: count, points: pts }, 'success', seat.id));
    }
  }
  const summary = state.seats.map((seat) => ({
    playerId: seat.id,
    name: seat.name,
    cards: captured[seat.id]?.length ?? 0,
    points: (scores[seat.id] ?? 0) - (state.handStartScores[seat.id] ?? 0),
    score: scores[seat.id] ?? 0,
  }));
  const nextState: InternalState = {
    ...state,
    captured,
    table,
    scores,
    phase: 'handEnd',
    lastHandSummary: summary,
    lastCapture: undefined,
    lastPlayed: undefined,
    delivery: undefined,
    lastCanto: undefined,
    lastCapturer: undefined,
  };
  const checked = checkTarget(nextState);
  if (checked.phase === 'matchEnd') {
    events.push(event('log.matchEnd', { p: playerName(state.seats, checked.winnerIds?.[0] ?? '') }, 'magic'));
    return { state: checked, events };
  }
  return { state: nextState, events };
}

function playCard(
  state: InternalState,
  actorId: string,
  cardId: string,
): { state: InternalState; events: ReturnType<typeof event>[]; error?: string } {
  if (state.phase !== 'play') return { state, events: [], error: 'error.notNow' };
  const seat = state.seats[state.turn];
  if (!seat || seat.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };
  const hand = state.hands[actorId] ?? [];
  const card = hand.find((c) => c.id === cardId);
  if (!card) return { state, events: [], error: 'error.invalidAction' };

  const hands = { ...state.hands, [actorId]: hand.filter((c) => c.id !== cardId) };
  const tableBefore = state.table;
  const matchIndex = [...tableBefore].reverse().findIndex((c) => c.rank === card.rank);
  const realIndex = matchIndex >= 0 ? tableBefore.length - 1 - matchIndex : -1;
  const tableAfterPush = [...tableBefore, card];
  let table = tableAfterPush;
  let captured = state.captured;
  let points = 0;
  let mesa = false;
  let captureCards: CardView[] = [];
  const events: ReturnType<typeof event>[] = [event('log.played', { p: seat.name, card }, 'info', actorId)];

  if (realIndex >= 0) {
    captureCards = tableAfterPush.slice(realIndex);
    table = tableAfterPush.slice(0, realIndex);
    points = rankPoints(card.rank);
    mesa = realIndex === 0;
    captured = {
      ...state.captured,
      [actorId]: [...(state.captured[actorId] ?? []), ...captureCards],
    };
    if (mesa) points += 5;
    events.push(
      event(
        mesa ? 'log.caidaMesa' : 'log.caida',
        { p: seat.name, n: captureCards.length, points, card },
        mesa ? 'magic' : 'success',
        actorId,
      ),
    );
  }

  let scores = { ...state.scores };
  if (points > 0) scores = { ...scores, [actorId]: (scores[actorId] ?? 0) + points };

  let deck = state.deck;
  if (deck.length > 0 && (hands[actorId]?.length ?? 0) < 3) {
    const draw = deck[0];
    if (draw) {
      deck = deck.slice(1);
      hands[actorId] = sortCards([...(hands[actorId] ?? []), draw]);
      events.push(event('log.drew', { p: seat.name, n: 1 }, 'info', actorId));
    }
  }

  const nextState: InternalState = {
    ...state,
    hands,
    deck,
    table,
    captured,
    scores,
    turn: nextIndex(state.turn, state.seats.length),
    playedThisHand: { ...state.playedThisHand, [actorId]: true },
    lastPlayed: { playerId: actorId, card },
    lastCapturer: captureCards.length > 0 ? actorId : state.lastCapturer,
    lastCapture:
      captureCards.length > 0
        ? { playerId: actorId, cards: captureCards, points, mesa, rank: card.rank }
        : undefined,
  };

  const everyoneEmpty = state.seats.every((s) => (hands[s.id]?.length ?? 0) === 0);
  if (everyoneEmpty) {
    const ended = endHand(nextState);
    return { state: ended.state, events: [...events, ...ended.events] };
  }
  const checked = checkTarget(nextState);
  if (checked.phase === 'matchEnd') {
    events.push(event('log.matchEnd', { p: playerName(state.seats, checked.winnerIds?.[0] ?? '') }, 'magic'));
  }
  return { state: checked, events };
}

function claimCanto(state: InternalState, actorId: string): { state: InternalState; events: ReturnType<typeof event>[]; error?: string } {
  if (state.phase !== 'play') return { state, events: [], error: 'error.notNow' };
  const seat = state.seats[state.turn];
  if (!seat || seat.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };
  const claimed = state.claimedHands[actorId] ?? [];
  if (claimed.includes(state.handNo)) return { state, events: [], error: 'error.alreadyClaimed' };
  if (state.playedThisHand[actorId]) return { state, events: [], error: 'error.cantoAfterPlay' };
  const canto = detectCanto(state.hands[actorId] ?? []);
  if (!canto) return { state, events: [], error: 'error.noCanto' };
  const scores = { ...state.scores, [actorId]: (state.scores[actorId] ?? 0) + canto.points };
  const next: InternalState = {
    ...state,
    scores,
    claimedHands: { ...state.claimedHands, [actorId]: [...claimed, state.handNo] },
    lastCanto: { playerId: actorId, key: canto.key, points: canto.points, instant: false },
  };
  const events = [
    event('log.canto', { p: seat.name, canto: cantoLabel(canto.key), points: canto.points }, 'success', actorId),
  ];
  return { state: checkTarget(next), events };
}

function chooseBotCard(state: InternalState, botId: string): string {
  const hand = state.hands[botId] ?? [];
  if (hand.length === 0) return '';
  const table = state.table;
  const scored = hand.map((card) => {
    const idx = [...table].reverse().findIndex((c) => c.rank === card.rank);
    const capture = idx >= 0;
    const points = capture ? rankPoints(card.rank) + (table.length - 1 - idx === 0 ? 5 : 0) : 0;
    const risk = capture ? 0 : table.some((c) => c.rank === card.rank) ? 12 : rankPoints(card.rank);
    return { card, value: points * 10 - risk + (card.rank <= 7 ? 2 : 0) };
  });
  scored.sort((a, b) => b.value - a.value);
  return scored[0]?.card.id ?? (hand[0]?.id ?? '');
}

function createGame(seats: Seat[], options: GameOptions, rng: Rng): InternalState {
  const teams = Boolean(options.teams) && seats.length === 4;
  const withTeams = seats.map((s, i) => (teams ? { ...s, team: i % 2 } : { ...s, team: undefined }));
  const deck = shuffle(spanishDeck(), rng);
  return {
    seats: withTeams,
    options,
    scores: Object.fromEntries(withTeams.map((s) => [s.id, 0])),
    hands: Object.fromEntries(withTeams.map((s) => [s.id, []])),
    deck,
    table: [],
    captured: Object.fromEntries(withTeams.map((s) => [s.id, []])),
    claimedHands: Object.fromEntries(withTeams.map((s) => [s.id, []])),
    dealer: 0,
    turn: 0,
    handNo: 1,
    target: options.target ?? 100,
    teams,
    phase: 'entrega',
    handStartScores: Object.fromEntries(withTeams.map((s) => [s.id, 0])),
    playedThisHand: Object.fromEntries(withTeams.map((s) => [s.id, false])),
    rng,
  };
}

export const caidaModule: GameModule<CaidaState> = {
  meta: {
    id: 'caida',
    nameKey: 'game.caida.name',
    descKey: 'game.caida.desc',
    rulesKey: 'game.caida.rules',
    icon: '🃏',
    category: 'cartas',
    minPlayers: 2,
    maxPlayers: 4,
    avgMinutes: 18,
    accent: '#e0a63a',
    tagKeys: ['tag.venezolano', 'tag.cantos', 'tag.equipos'],
  },

  create(seats, options, rng) {
    return createGame(seats, options, rng);
  },

  view(state, viewerId) {
    const turnSeat = state.seats[state.turn];
    const isTurn = turnSeat?.id === viewerId;
    const canto = isTurn && state.phase === 'play' ? detectCanto(state.hands[viewerId] ?? []) : null;
    const canClaim =
      Boolean(canto) &&
      !(state.claimedHands[viewerId] ?? []).includes(state.handNo) &&
      !state.playedThisHand[viewerId] &&
      state.phase === 'play';
    const isDealer = state.seats[state.dealer]?.id === viewerId;
    const actions: UIAction[] = [];
    if (state.phase === 'entrega' && isDealer) {
      actions.push(makeAction('deliver', 'action.deliver', { type: 'deliver' }, { tone: 'primary', icon: 'sparkles' }));
    }
    if (state.phase === 'handEnd') {
      actions.push(makeAction('nextHand', 'action.nextHand', { type: 'nextHand' }, { tone: 'primary', icon: 'arrow-right' }));
    }
    if (canClaim && canto) {
      actions.push(
        makeAction('canto', 'action.canto', { type: 'canto' }, {
          tone: 'primary',
          icon: 'megaphone',
          labelParams: { canto: canto.key, points: canto.points },
        }),
      );
    }
    if (state.phase === 'play') {
      actions.push(makeAction('resign', 'action.resign', { type: 'resign' }, { tone: 'danger', icon: 'flag' }));
    }
    const hand = sortCards(state.hands[viewerId] ?? []).map((card) => ({
      ...card,
      playable: isTurn && state.phase === 'play',
    }));

    let prompt: GameView['prompt'];
    if (state.phase === 'entrega') {
      prompt = isDealer
        ? { key: 'prompt.caidaDeliver', tone: 'info' }
        : { key: 'prompt.caidaWaitDeliver', params: { player: state.seats[state.dealer]?.name ?? '—' }, tone: 'info' };
    } else if (state.phase === 'handEnd') {
      prompt = { key: 'prompt.caidaHandEnd', tone: 'success' };
    } else if (state.phase === 'matchEnd') {
      prompt = { key: 'prompt.matchEnd', tone: 'success' };
    } else if (isTurn) {
      prompt = canClaim
        ? { key: 'prompt.caidaCanto', params: { canto: canto?.key ?? '', points: canto?.points ?? 0 }, tone: 'success' }
        : { key: 'prompt.yourTurn', tone: 'info' };
    } else {
      prompt = {
        key: 'prompt.waitingPlayer',
        params: { player: turnSeat?.name ?? '—' },
        tone: 'info',
      };
    }

    const view: GameView = {
      gameId: 'caida',
      youId: viewerId,
      phase: state.phase,
      turnId: turnSeat?.id ?? null,
      players: playerViews(state.seats, state.scores, turnSeat?.id ?? null, viewerId, (seat) => ({
        cardsLeft: state.hands[seat.id]?.length ?? 0,
        active: state.phase !== 'matchEnd',
      })),
      hand,
      center: {
        cards: state.table,
        piles: [{ id: 'deck', labelKey: 'table.deck', count: state.deck.length }],
      },
      actions,
      prompt,
      extra: {
        handNo: state.handNo,
        target: state.target,
        teams: state.teams,
        dealerId: state.seats[state.dealer]?.id ?? null,
        canto: canto ? { key: canto.key, points: canto.points, instant: canto.instant } : null,
        claimed: (state.claimedHands[viewerId] ?? []).includes(state.handNo),
        captured: Object.fromEntries(state.seats.map((s) => [s.id, state.captured[s.id]?.length ?? 0])),
        teamScores: state.teams
          ? Object.fromEntries([...new Set(state.seats.map((s) => s.team ?? 0))].map((t) => [t, teamTotal(state, t)]))
          : null,
        delivery: state.delivery ?? null,
        lastCapture: state.lastCapture ?? null,
        lastCanto: state.lastCanto ?? null,
        lastPlayed: state.lastPlayed ?? null,
        lastCapturer: state.lastCapturer ?? null,
        handSummary: state.lastHandSummary ?? null,
        canClaim,
        playedThisHand: state.playedThisHand[viewerId] ?? false,
      },
    };
    return view;
  },

  act(state, action, actorId, _rng): ActResult<CaidaState> {
    const internal = state as InternalState;
    if (action.type === 'nextHand') {
      if (internal.phase !== 'handEnd') return { state, events: [], error: 'error.notNow' };
      if (!internal.seats.some((seat) => seat.id === actorId)) return { state, events: [], error: 'error.invalidAction' };
      const dealer = nextIndex(internal.dealer, internal.seats.length);
      return { state: newHand(internal, dealer), events: [] };
    }
    if (action.type === 'deliver') {
      if (internal.phase !== 'entrega') return { state, events: [], error: 'error.notNow' };
      if (internal.seats[internal.dealer]?.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };
      const res = startDelivery(internal);
      return { state: res.state, events: res.events };
    }
    if (action.type === 'canto') {
      const res = claimCanto(internal, actorId);
      return { state: res.state, events: res.events, ...(res.error ? { error: res.error } : {}) };
    }
    if (action.type === 'play') {
      const cardId = String(action.cardId ?? '');
      const res = playCard(internal, actorId, cardId);
      return { state: res.state, events: res.events, ...(res.error ? { error: res.error } : {}) };
    }
    return { state, events: [], error: 'error.invalidAction' };
  },

  bot(state, botId, _difficulty: Difficulty, rng: Rng): GameAction | null {
    const internal = state as InternalState;
    if (internal.phase === 'matchEnd') return null;
    const seat = internal.seats[internal.turn];
    if (internal.phase === 'entrega') {
      return internal.seats[internal.dealer]?.id === botId ? { type: 'deliver' } : null;
    }
    if (internal.phase !== 'play' || seat?.id !== botId) return null;
    const canto = detectCanto(internal.hands[botId] ?? []);
    const canClaim = Boolean(canto) && !(internal.claimedHands[botId] ?? []).includes(internal.handNo) && !internal.playedThisHand[botId];
    if (canClaim) return { type: 'canto' };
    const cardId = chooseBotCard(internal, botId);
    if (cardId) return { type: 'play', cardId };
    const fallback = pick(internal.hands[botId] ?? [], rng);
    return fallback ? { type: 'play', cardId: fallback.id } : null;
  },

  result(state) {
    return (state as CaidaState).result ?? null;
  },
};

export type { CaidaState };
