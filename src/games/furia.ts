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
import { shuffle } from '../core/rng';
import { FURIA_COLORS, event, furiaDeck, playerViews, sortCards, type FuriaColor } from './cards';
import { makeAction } from './helpers';

type Phase = 'play' | 'matchEnd';

interface FuriaState {
  seats: Seat[];
  options: GameOptions;
  scores: Record<string, number>;
  hands: Record<string, CardView[]>;
  deck: CardView[];
  discard: CardView[];
  color: string;
  direction: 1 | -1;
  turn: number;
  phase: Phase;
  hasDrawn: boolean;
  drawnId?: string;
  winnerIds?: string[];
  result?: MatchResult;
  lastPlay?: { playerId: string; card: CardView };
  lastShout?: { playerId: string };
  lastDraw?: { playerId: string; n: number };
  rng: Rng;
}

function cardPoints(card: CardView): number {
  if (card.action === 'wild' || card.action === 'wild4') return 50;
  if (card.action) return 20;
  return card.rank;
}

function isWild(card: CardView): boolean {
  return card.action === 'wild' || card.action === 'wild4';
}

function topCard(state: FuriaState): CardView | undefined {
  return state.discard[state.discard.length - 1];
}

function canPlayOn(card: CardView, state: FuriaState): boolean {
  const top = topCard(state);
  if (!top) return true;
  if (isWild(card)) return true;
  if (card.suit === state.color) return true;
  if (top.rank >= 0 && card.rank === top.rank) return true;
  if (top.action && card.action === top.action) return true;
  return false;
}

function normalizeDeck(state: FuriaState): FuriaState {
  if (state.deck.length > 0) return state;
  const top = topCard(state);
  const rest = state.discard.slice(0, -1);
  if (rest.length === 0) return state;
  return {
    ...state,
    deck: shuffle(rest, state.rng),
    discard: top ? [top] : [],
  };
}

function drawTo(state: FuriaState, playerId: string, count: number): FuriaState {
  let next = normalizeDeck(state);
  const deck = [...next.deck];
  const hand = [...(next.hands[playerId] ?? [])];
  for (let i = 0; i < count; i += 1) {
    if (deck.length === 0) {
      const top = topCard(next);
      const rest = next.discard.slice(0, -1);
      if (rest.length === 0) break;
      deck.push(...shuffle(rest, next.rng));
      next = { ...next, discard: top ? [top] : [] };
    }
    const card = deck.shift();
    if (card) hand.push(card);
  }
  return { ...next, deck, hands: { ...next.hands, [playerId]: hand } };
}

function nextTurnIndex(state: FuriaState, steps = 1): number {
  const n = state.seats.length;
  return (state.turn + state.direction * steps + n * 2) % n;
}

function nextPlayer(state: FuriaState): Seat | undefined {
  return state.seats[nextTurnIndex(state, 1)];
}

function mostCommonColor(hand: CardView[]): FuriaColor {
  const counts = new Map<string, number>();
  for (const card of hand) {
    if (card.suit !== 'wild') counts.set(card.suit, (counts.get(card.suit) ?? 0) + 1);
  }
  let best: string = 'sol';
  let max = -1;
  for (const [suit, count] of counts) {
    if (count > max) {
      best = suit;
      max = count;
    }
  }
  return best as FuriaColor;
}

function makeView(state: FuriaState, viewerId: string): GameView {
  const turnSeat = state.seats[state.turn];
  const isTurn = turnSeat?.id === viewerId;
  const top = topCard(state);
  const hand = sortCards(state.hands[viewerId] ?? []).map((card) => ({
    ...card,
    playable:
      isTurn &&
      state.phase === 'play' &&
      canPlayOn(card, state) &&
      (!state.hasDrawn || state.drawnId === card.id),
  }));
  const actions: UIAction[] = [];
  if (isTurn && state.phase === 'play') {
    if (!state.hasDrawn) {
      actions.push(makeAction('draw', 'action.drawCard', { type: 'draw' }, { tone: 'secondary', icon: 'layers' }));
    } else {
      actions.push(makeAction('pass', 'action.pass', { type: 'pass' }, { tone: 'ghost', icon: 'arrow-right' }));
    }
  }
  let prompt: GameView['prompt'];
  if (state.phase === 'matchEnd') prompt = { key: 'prompt.matchEnd', tone: 'success' };
  else if (isTurn) {
    prompt = state.hasDrawn
      ? { key: 'prompt.furiaDrawn', tone: 'info' }
      : { key: 'prompt.furiaPlay', tone: 'info' };
  } else {
    prompt = { key: 'prompt.waitingPlayer', params: { player: turnSeat?.name ?? '—' }, tone: 'info' };
  }
  return {
    gameId: 'furia',
    youId: viewerId,
    phase: state.phase,
    turnId: turnSeat?.id ?? null,
    players: playerViews(state.seats, state.scores, turnSeat?.id ?? null, viewerId, (seat) => ({
      cardsLeft: state.hands[seat.id]?.length ?? 0,
      active: state.phase !== 'matchEnd',
    })),
    hand,
    center: {
      cards: top ? [{ ...top, playable: false }] : [],
      piles: [{ id: 'deck', labelKey: 'table.deck', count: state.deck.length }],
    },
    actions,
    prompt,
    extra: {
      color: state.color,
      direction: state.direction,
      top: top ?? null,
      lastPlay: state.lastPlay ?? null,
      lastShout: state.lastShout ?? null,
      lastDraw: state.lastDraw ?? null,
      drawnId: state.drawnId ?? null,
      hasDrawn: state.hasDrawn,
      colors: FURIA_COLORS,
    },
  };
}

export const furiaModule: GameModule<FuriaState> = {
  meta: {
    id: 'furia',
    nameKey: 'game.furia.name',
    descKey: 'game.furia.desc',
    rulesKey: 'game.furia.rules',
    icon: '🎨',
    category: 'cartas',
    minPlayers: 2,
    maxPlayers: 4,
    avgMinutes: 12,
    accent: '#e4572e',
    tagKeys: ['tag.colores', 'tag.rapido', 'tag.familiar'],
  },

  create(seats, options, rng) {
    const deck = shuffle(furiaDeck(), rng);
    const hands: Record<string, CardView[]> = Object.fromEntries(seats.map((s) => [s.id, []]));
    for (let i = 0; i < 7; i += 1) {
      for (const seat of seats) {
        const c = deck.shift();
        if (c) hands[seat.id]?.push(c);
      }
    }
    let first = deck.shift();
    while (first && isWild(first)) {
      deck.push(first);
      first = deck.shift();
    }
    const discard = first ? [first] : [];
    const color = first && first.suit !== 'wild' ? first.suit : mostCommonColor(hands[seats[0]?.id ?? ''] ?? []);
    const state: FuriaState = {
      seats: seats.map((s) => ({ ...s })),
      options,
      scores: Object.fromEntries(seats.map((s) => [s.id, 0])),
      hands,
      deck,
      discard,
      color,
      direction: 1,
      turn: 0,
      phase: 'play',
      hasDrawn: false,
      rng,
    };
    return state;
  },

  view(state, viewerId) {
    return makeView(state, viewerId);
  },

  act(state, action, actorId, _rng): ActResult<FuriaState> {
    if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
    const seat = state.seats[state.turn];
    if (!seat || seat.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };

    if (action.type === 'draw') {
      if (state.hasDrawn) return { state, events: [], error: 'error.alreadyDrawn' };
      let next = drawTo(state, actorId, 1);
      const newHand = next.hands[actorId] ?? [];
      const card = newHand[newHand.length - 1];
      const events = [event('log.drew', { p: seat.name, n: 1 }, 'info', actorId)];
      if (!card) {
        next = { ...next, turn: nextTurnIndex(next), hasDrawn: false, drawnId: undefined };
        return { state: next, events };
      }
      if (canPlayOn(card, next)) {
        next = { ...next, hasDrawn: true, drawnId: card.id, lastDraw: { playerId: actorId, n: 1 } };
        return { state: next, events };
      }
      next = { ...next, turn: nextTurnIndex(next), hasDrawn: false, drawnId: undefined, lastDraw: { playerId: actorId, n: 1 } };
      return { state: next, events };
    }

    if (action.type === 'pass') {
      if (!state.hasDrawn) return { state, events: [], error: 'error.invalidAction' };
      return {
        state: {
          ...state,
          turn: nextTurnIndex(state),
          hasDrawn: false,
          drawnId: undefined,
        },
        events: [event('log.passed', { p: seat.name }, 'info', actorId)],
      };
    }

    if (action.type === 'play') {
      const cardId = String(action.cardId ?? '');
      const hand = state.hands[actorId] ?? [];
      const card = hand.find((c) => c.id === cardId);
      if (!card) return { state, events: [], error: 'error.invalidAction' };
      if (state.hasDrawn && state.drawnId !== card.id) return { state, events: [], error: 'error.onlyDrawn' };
      if (!canPlayOn(card, state)) return { state, events: [], error: 'error.cardDoesNotMatch' };
      let chosenColor = isWild(card) ? state.color : (card.suit as FuriaColor);
      if (isWild(card)) {
        const requested = String(action.color ?? '');
        if (!FURIA_COLORS.includes(requested as FuriaColor)) {
          return { state, events: [], error: 'error.chooseColor' };
        }
        chosenColor = requested;
      }
      const hands = { ...state.hands, [actorId]: hand.filter((c) => c.id !== cardId) };
      const discard = [...state.discard, card];
      let next: FuriaState = {
        ...state,
        hands,
        discard,
        color: chosenColor,
        hasDrawn: false,
        drawnId: undefined,
        lastPlay: { playerId: actorId, card },
        lastShout: hands[actorId]?.length === 1 ? { playerId: actorId } : undefined,
      };
      const events = [event('log.played', { p: seat.name, card }, 'info', actorId)];

      if (hands[actorId]?.length === 0) {
        const gained = state.seats
          .filter((s) => s.id !== actorId)
          .reduce((total, s) => total + (hands[s.id] ?? []).reduce((sum, c) => sum + cardPoints(c), 0), 0);
        const result: MatchResult = {
          winnerIds: [actorId],
          scores: Object.fromEntries(state.seats.map((s) => [s.id, s.id === actorId ? gained : 0])),
          reasonKey: 'result.reason.lastCard',
        };
        const finished: FuriaState = {
          ...next,
          phase: 'matchEnd',
          winnerIds: [actorId],
          scores: result.scores,
          result,
        };
        events.push(event('log.won', { p: seat.name }, 'magic', actorId));
        return { state: finished, events };
      }

      if (hands[actorId]?.length === 1) {
        events.push(event('log.lastCard', { p: seat.name }, 'magic', actorId));
      }

      if (card.action === 'reverse') {
        const direction = (state.direction * -1) as 1 | -1;
        next = { ...next, direction };
        events.push(event('log.reversed', { p: seat.name }, 'info', actorId));
      }

      const opponent = nextPlayer(next);
      if (card.action === 'draw2' && opponent) {
        next = drawTo(next, opponent.id, 2);
        events.push(event('log.forcedDraw', { p: opponent.name, n: 2 }, 'warn', actorId));
      }
      if (card.action === 'wild4' && opponent) {
        next = drawTo(next, opponent.id, 4);
        events.push(event('log.forcedDraw', { p: opponent.name, n: 4 }, 'danger', actorId));
      }

      let steps = 1;
      if (card.action === 'skip' || card.action === 'draw2' || card.action === 'wild4') steps = 2;
      if (card.action === 'reverse') {
        if (state.seats.length === 2) steps = 2;
      }
      if (card.action === 'reverse' || card.action === 'skip' || card.action === 'draw2' || card.action === 'wild4') {
        const skipped = next.seats[nextTurnIndex(next, 1)];
        if (skipped && card.action !== 'reverse') {
          events.push(event('log.skipped', { p: skipped.name }, 'warn', actorId));
        }
      }
      next = { ...next, turn: nextTurnIndex(next, steps) };
      // Evita que la pila crezca sin control: recicla cuando haga falta.
      next = normalizeDeck(next);
      return { state: next, events };
    }

    return { state, events: [], error: 'error.invalidAction' };
  },

  bot(state, botId, difficulty: Difficulty, rng: Rng): GameAction | null {
    if (state.phase === 'matchEnd') return null;
    const seat = state.seats[state.turn];
    if (!seat || seat.id !== botId) return null;
    const hand = state.hands[botId] ?? [];
    const playable = hand.filter((c) => canPlayOn(c, state));
    if (state.hasDrawn) {
      const drawn = hand.find((c) => c.id === state.drawnId);
      if (drawn && canPlayOn(drawn, state)) {
        return { type: 'play', cardId: drawn.id, color: mostCommonColor(hand) };
      }
      return { type: 'pass' };
    }
    if (playable.length === 0) return { type: 'draw' };
    const rank = (c: CardView) => (c.action ? 3 : c.rank);
    const sorted = [...playable].sort((a, b) => rank(b) - rank(a));
    const pickCard =
      difficulty === 'easy' && sorted.length > 1
        ? sorted[Math.floor(rng() * sorted.length)] ?? sorted[0]
        : sorted[0];
    if (!pickCard) return { type: 'draw' };
    return { type: 'play', cardId: pickCard.id, color: mostCommonColor(hand) };
  },

  result(state) {
    return state.result ?? null;
  },
};

export type { FuriaState };
