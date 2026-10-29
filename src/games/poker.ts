import type {
  ActResult,
  CardView,
  Difficulty,
  GameAction,
  GameModule,
  GameOptions,
  GameView,
  LogEvent,
  MatchResult,
  Seat,
  UIAction,
} from '../core/types';
import type { Rng } from '../core/types';
import { shuffle } from '../core/rng';
import { event, playerName, playerViews } from './cards';
import { makeAction, prompt } from './helpers';

/**
 * Póker Texas Hold'em sin dinero real.
 *
 * Reglas implementadas: ciegas pequeña/grande, botón de reparto rotativo,
 * cuatro calles (preflop, flop, turn, river), subidas con mínimo, all-in,
 * botes laterales, retirada de apuestas no igualadas, showdown con evaluación
 * de la mejor mano de 5 entre 7 cartas y eliminación por stack.
 *
 * No hay dinero, compras ni apuestas reales: solo fichas de práctica.
 */

export const POKER_SUITS = ['clubs', 'diamonds', 'hearts', 'spades'] as const;
export type PokerSuit = (typeof POKER_SUITS)[number];

/** Baraja francesa de 52 cartas: 2..10, J=11, Q=12, K=13, A=14. */
export function pokerDeck(): CardView[] {
  const deck: CardView[] = [];
  for (const suit of POKER_SUITS) {
    for (let rank = 2; rank <= 14; rank += 1) {
      deck.push({ id: `${suit}-${rank}`, suit, rank });
    }
  }
  return deck;
}

const CATEGORY_KEYS = [
  'poker.hand.high',
  'poker.hand.pair',
  'poker.hand.twoPair',
  'poker.hand.trips',
  'poker.hand.straight',
  'poker.hand.flush',
  'poker.hand.fullHouse',
  'poker.hand.quads',
  'poker.hand.straightFlush',
] as const;

export interface HandEval {
  category: number;
  ranks: number[];
  nameKey: string;
  cards: CardView[];
}

function byRankDesc(a: CardView, b: CardView): number {
  return b.rank - a.rank;
}

function straightHigh(ranks: number[]): number | null {
  const unique = [...new Set(ranks)].sort((a, b) => b - a);
  if (unique.includes(14)) unique.push(1);
  for (let i = 0; i + 4 < unique.length; i += 1) {
    const a = unique[i];
    const b = unique[i + 1];
    const c = unique[i + 2];
    const d = unique[i + 3];
    const e = unique[i + 4];
    if (a === undefined || b === undefined || c === undefined || d === undefined || e === undefined) continue;
    if (a - 1 === b && b - 1 === c && c - 1 === d && d - 1 === e) return a;
  }
  return null;
}

function cardsForRanks(cards: CardView[], ranks: number[]): CardView[] {
  const pool = cards.slice().sort(byRankDesc);
  const out: CardView[] = [];
  for (const rank of ranks) {
    const index = pool.findIndex((card) => card.rank === rank);
    if (index >= 0) {
      const [picked] = pool.splice(index, 1);
      if (picked) out.push(picked);
    }
  }
  return out;
}

/** Evalúa la mejor mano de 5 cartas dentro de 5..7 cartas. */
export function evaluateHand(cards: CardView[]): HandEval {
  const sorted = cards.slice().sort(byRankDesc);
  if (sorted.length < 5) {
    const top = sorted.slice(0, 5);
    return {
      category: 0,
      ranks: top.map((card) => card.rank),
      nameKey: CATEGORY_KEYS[0],
      cards: top,
    };
  }

  const count = new Map<number, number>();
  for (const card of sorted) count.set(card.rank, (count.get(card.rank) ?? 0) + 1);
  const groups = [...count.entries()].sort((a, b) => (b[1] - a[1]) || (b[0] - a[0]));
  const quad = groups.find(([, n]) => n === 4);
  const trips = groups.filter(([, n]) => n === 3).map(([rank]) => rank).sort((a, b) => b - a);
  const pairs = groups.filter(([, n]) => n === 2).map(([rank]) => rank).sort((a, b) => b - a);

  const bySuit = new Map<string, CardView[]>();
  for (const card of sorted) bySuit.set(card.suit, [...(bySuit.get(card.suit) ?? []), card]);
  const flushCards = [...bySuit.values()].find((list) => list.length >= 5)?.slice(0, 5) ?? null;

  if (flushCards) {
    const sfHigh = straightHigh(flushCards.map((card) => card.rank));
    if (sfHigh !== null) {
      const ranks = sfHigh === 1 ? [5, 4, 3, 2, 1] : [sfHigh, sfHigh - 1, sfHigh - 2, sfHigh - 3, sfHigh - 4];
      return {
        category: 8,
        ranks,
        nameKey: CATEGORY_KEYS[8],
        cards: cardsForRanks(flushCards, ranks),
      };
    }
  }

  if (quad) {
    const quadRank = quad[0];
    const kicker = sorted.find((card) => card.rank !== quadRank)?.rank ?? 0;
    return {
      category: 7,
      ranks: [quadRank, kicker],
      nameKey: CATEGORY_KEYS[7],
      cards: cardsForRanks(sorted, [quadRank, quadRank, quadRank, quadRank, kicker]),
    };
  }

  if (trips[0] !== undefined) {
    const [firstTrips, secondTrips] = trips;
    const pairRank = secondTrips ?? pairs.find((rank) => rank !== firstTrips);
    if (pairRank !== undefined) {
      return {
        category: 6,
        ranks: [firstTrips, pairRank],
        nameKey: CATEGORY_KEYS[6],
        cards: cardsForRanks(sorted, [firstTrips, firstTrips, firstTrips, pairRank, pairRank]),
      };
    }
  }

  if (flushCards) {
    return {
      category: 5,
      ranks: flushCards.map((card) => card.rank),
      nameKey: CATEGORY_KEYS[5],
      cards: flushCards,
    };
  }

  const straight = straightHigh(sorted.map((card) => card.rank));
  if (straight !== null) {
    const ranks = straight === 1 ? [5, 4, 3, 2, 1] : [straight, straight - 1, straight - 2, straight - 3, straight - 4];
    return {
      category: 4,
      ranks,
      nameKey: CATEGORY_KEYS[4],
      cards: cardsForRanks(sorted, ranks),
    };
  }

  if (trips[0] !== undefined) {
    const tripRank = trips[0];
    const kickers = groups
      .filter(([rank, n]) => rank !== tripRank && n < 3)
      .map(([rank]) => rank)
      .sort((a, b) => b - a)
      .slice(0, 2);
    return {
      category: 3,
      ranks: [tripRank, ...kickers],
      nameKey: CATEGORY_KEYS[3],
      cards: cardsForRanks(sorted, [tripRank, tripRank, tripRank, ...kickers]),
    };
  }

  if (pairs.length >= 2) {
    const highPair = pairs[0] as number;
    const lowPair = pairs[1] as number;
    const kicker = groups
      .filter(([rank]) => rank !== highPair && rank !== lowPair)
      .map(([rank]) => rank)
      .sort((a, b) => b - a)[0] ?? 0;
    return {
      category: 2,
      ranks: [highPair, lowPair, kicker],
      nameKey: CATEGORY_KEYS[2],
      cards: cardsForRanks(sorted, [highPair, highPair, lowPair, lowPair, kicker]),
    };
  }

  if (pairs[0] !== undefined) {
    const pairRank = pairs[0];
    const kickers = groups
      .filter(([rank]) => rank !== pairRank)
      .map(([rank]) => rank)
      .sort((a, b) => b - a)
      .slice(0, 3);
    return {
      category: 1,
      ranks: [pairRank, ...kickers],
      nameKey: CATEGORY_KEYS[1],
      cards: cardsForRanks(sorted, [pairRank, pairRank, ...kickers]),
    };
  }

  const high = sorted.slice(0, 5);
  return {
    category: 0,
    ranks: high.map((card) => card.rank),
    nameKey: CATEGORY_KEYS[0],
    cards: high,
  };
}

export function compareHands(a: HandEval, b: HandEval): number {
  if (a.category !== b.category) return a.category - b.category;
  const length = Math.max(a.ranks.length, b.ranks.length);
  for (let i = 0; i < length; i += 1) {
    const ra = a.ranks[i] ?? 0;
    const rb = b.ranks[i] ?? 0;
    if (ra !== rb) return ra - rb;
  }
  return 0;
}

export type PokerStreet = 'preflop' | 'flop' | 'turn' | 'river' | 'showdown' | 'handEnd' | 'matchEnd';

interface PlayerMove {
  kind: 'fold' | 'check' | 'call' | 'raise' | 'allin' | 'blind';
  amount?: number;
  allIn?: boolean;
}

interface PokerPlayer {
  id: string;
  chips: number;
  hole: CardView[];
  folded: boolean;
  allIn: boolean;
  bet: number;
  totalBet: number;
  acted: boolean;
  out: boolean;
  lastMove?: PlayerMove;
}

interface ShowdownRow {
  playerId: string;
  name: string;
  hole: CardView[];
  handKey: string;
  best: CardView[];
  folded: boolean;
  won: number;
}

interface ShowdownInfo {
  rows: ShowdownRow[];
  pots: Array<{ amount: number; winners: string[] }>;
  totalPot: number;
}

interface HandWinner {
  playerId: string;
  amount: number;
  folded: boolean;
}

export interface PokerState {
  seats: Seat[];
  options: GameOptions;
  players: PokerPlayer[];
  deck: CardView[];
  community: CardView[];
  dealer: number;
  turn: number;
  street: PokerStreet;
  currentBet: number;
  minRaise: number;
  smallBlind: number;
  bigBlind: number;
  handNo: number;
  actionLog: Array<{ playerId: string; move: PlayerMove }>;
  showdown?: ShowdownInfo;
  handWinner?: HandWinner;
  result?: MatchResult;
  rng: Rng;
}

function clonePlayers(players: PokerPlayer[]): PokerPlayer[] {
  return players.map((player) => ({ ...player, hole: player.hole.slice() }));
}

function potTotal(state: PokerState): number {
  return state.players.reduce((total, player) => total + player.totalBet, 0);
}

function livePlayers(state: PokerState): PokerPlayer[] {
  return state.players.filter((player) => !player.folded && !player.out);
}

function isBettingStreet(street: PokerStreet): boolean {
  return street === 'preflop' || street === 'flop' || street === 'turn' || street === 'river';
}

function nextAlive(state: PokerState, from: number): number {
  const n = state.players.length;
  for (let step = 1; step <= n; step += 1) {
    const index = (from + step) % n;
    const player = state.players[index];
    if (player && player.chips > 0 && !player.out) return index;
  }
  return -1;
}

function nextToAct(state: PokerState, from: number): number {
  const n = state.players.length;
  for (let step = 1; step <= n; step += 1) {
    const index = (from + step) % n;
    const player = state.players[index];
    if (!player || player.out || player.folded || player.allIn || player.chips <= 0) continue;
    if (!player.acted || player.bet < state.currentBet) return index;
  }
  return -1;
}

function logPoker(
  key: string,
  params?: Record<string, unknown>,
  tone: LogEvent['tone'] = 'info',
  actorId?: string,
): LogEvent {
  return event(key, params, tone, actorId);
}

function postBlind(players: PokerPlayer[], index: number, amount: number): number {
  const player = players[index];
  if (!player) return 0;
  const pay = Math.min(amount, player.chips);
  player.chips -= pay;
  player.bet += pay;
  player.totalBet += pay;
  player.lastMove = { kind: 'blind', amount: pay };
  if (player.chips === 0) player.allIn = true;
  return pay;
}

function startHand(state: PokerState): { state: PokerState; events: LogEvent[] } {
  const alive = state.players.filter((player) => player.chips > 0 && !player.out);
  if (alive.length < 2) return { state: finishMatch(state), events: [] };

  const players = state.players.map((player) => ({
    ...player,
    hole: [] as CardView[],
    folded: player.chips <= 0,
    allIn: false,
    bet: 0,
    totalBet: 0,
    acted: false,
    out: player.chips <= 0,
    lastMove: undefined as PlayerMove | undefined,
  }));
  const deck = shuffle(pokerDeck(), state.rng);
  let dealer = state.dealer;
  if (!players[dealer] || players[dealer]?.chips === 0) {
    const found = nextAlive({ ...state, players }, dealer);
    dealer = found >= 0 ? found : 0;
  }

  const n = players.length;
  let cursor = 0;
  for (let round = 0; round < 2; round += 1) {
    for (let step = 1; step <= n; step += 1) {
      const index = (dealer + step) % n;
      const player = players[index];
      if (!player || player.chips <= 0) continue;
      const card = deck[cursor];
      cursor += 1;
      if (card) player.hole.push(card);
    }
  }

  const aliveIndices = players
    .map((player, index) => ({ player, index }))
    .filter(({ player }) => player.chips > 0)
    .map(({ index }) => index);

  const dealerAlive = aliveIndices.includes(dealer) ? dealer : (aliveIndices[0] ?? 0);
  const twoHanded = aliveIndices.length === 2;
  const sbIndex = twoHanded ? dealerAlive : (nextAlive({ ...state, players, dealer: dealerAlive }, dealerAlive));
  const bbIndex = sbIndex >= 0 ? nextAlive({ ...state, players, dealer: sbIndex }, sbIndex) : -1;
  const safeSb = sbIndex >= 0 ? sbIndex : dealerAlive;
  const safeBb = bbIndex >= 0 ? bbIndex : nextAlive({ ...state, players, dealer: safeSb }, safeSb);

  const events: LogEvent[] = [logPoker('log.pokerHand', { n: state.handNo + 1 }, 'info')];
  const sbPaid = postBlind(players, safeSb, state.smallBlind);
  const bbPaid = postBlind(players, safeBb >= 0 ? safeBb : safeSb, state.bigBlind);
  if (sbPaid > 0) {
    events.push(logPoker('log.pokerBlind', {
      p: playerName(state.seats, players[safeSb]?.id ?? ''),
      amount: sbPaid,
      blind: 'SB',
    }, 'info'));
  }
  if (bbPaid > 0 && safeBb >= 0) {
    events.push(logPoker('log.pokerBlind', {
      p: playerName(state.seats, players[safeBb]?.id ?? ''),
      amount: bbPaid,
      blind: 'BB',
    }, 'info'));
  }

  const currentBet = Math.max(0, ...players.map((player) => player.bet));
  const firstIndex = twoHanded
    ? safeSb
    : (nextToAct({ ...state, players, currentBet, turn: safeBb }, safeBb) >= 0
      ? nextToAct({ ...state, players, currentBet, turn: safeBb }, safeBb)
      : safeSb);

  const nextState: PokerState = {
    ...state,
    players,
    deck: deck.slice(cursor),
    community: [],
    dealer: dealerAlive,
    turn: firstIndex >= 0 ? firstIndex : safeSb,
    street: 'preflop',
    currentBet,
    minRaise: state.bigBlind,
    handNo: state.handNo + 1,
    actionLog: [],
    showdown: undefined,
    handWinner: undefined,
    result: undefined,
  };

  const withTurn = { ...nextState, turn: nextState.turn };
  const next = nextToAct(withTurn, withTurn.turn - 1);
  return {
    state: next >= 0 ? { ...nextState, turn: next } : nextState,
    events,
  };
}

function finishMatch(state: PokerState): PokerState {
  const alive = state.players.filter((player) => player.chips > 0 && !player.out);
  const seatsById = new Map(state.seats.map((seat) => [seat.id, seat]));
  const humansAlive = alive.filter((player) => !seatsById.get(player.id)?.isBot);
  let winners: PokerPlayer[] = [];
  if (alive.length === 1) {
    winners = alive;
  } else if (humansAlive.length === 0) {
    const best = Math.max(0, ...alive.map((player) => player.chips));
    winners = alive.filter((player) => player.chips === best);
  } else {
    return state;
  }
  const winnerIds = winners.map((player) => player.id);
  const result: MatchResult = {
    winnerIds,
    draw: winnerIds.length > 1,
    scores: Object.fromEntries(state.players.map((player) => [player.id, player.chips])),
    reasonKey: 'result.reason.pokerChips',
    stats: {
      hands: state.handNo,
      pot: potTotal(state),
    },
  };
  return { ...state, street: 'matchEnd', result };
}

function prepareNextHand(state: PokerState): PokerState {
  const players = state.players.map((player) => ({
    ...player,
    out: player.chips <= 0,
    folded: player.chips <= 0,
    bet: 0,
    totalBet: 0,
    allIn: false,
    acted: false,
    lastMove: undefined as PlayerMove | undefined,
  }));
  const withPlayers = { ...state, players };
  const dealer = nextAlive(withPlayers, state.dealer);
  return startHand({ ...withPlayers, dealer: dealer >= 0 ? dealer : state.dealer }).state;
}

function resetStreet(state: PokerState): PokerState {
  return {
    ...state,
    players: state.players.map((player) => ({
      ...player,
      bet: 0,
      acted: false,
    })),
    currentBet: 0,
    minRaise: state.bigBlind,
    actionLog: [],
  };
}

function dealCards(state: PokerState, count: number): { state: PokerState; cards: CardView[] } {
  const deck = state.deck.slice();
  deck.shift(); // carta quemada
  const cards: CardView[] = [];
  for (let i = 0; i < count; i += 1) {
    const card = deck.shift();
    if (card) cards.push(card);
  }
  return {
    state: {
      ...state,
      deck,
      community: [...state.community, ...cards],
    },
    cards,
  };
}

function awardUncontested(state: PokerState, winner: PokerPlayer): { state: PokerState; events: LogEvent[] } {
  const amount = potTotal(state);
  const players = state.players.map((player) => ({
    ...player,
    chips: player.chips + (player.id === winner.id ? amount : 0),
    bet: 0,
    totalBet: 0,
  }));
  const events = [
    logPoker('log.pokerWin', {
      p: playerName(state.seats, winner.id),
      amount,
    }, 'success', winner.id),
  ];
  const next: PokerState = {
    ...state,
    players,
    street: 'handEnd',
    turn: -1,
    handWinner: { playerId: winner.id, amount, folded: true },
    showdown: undefined,
  };
  return { state: checkEliminations(next), events };
}

function checkEliminations(state: PokerState): PokerState {
  const events: LogEvent[] = [];
  const players = state.players.map((player) => {
    if (player.chips <= 0 && !player.out) {
      events.push(logPoker('log.pokerEliminated', {
        p: playerName(state.seats, player.id),
      }, 'warn', player.id));
      return { ...player, out: true, folded: true, allIn: false };
    }
    return player;
  });
  void events;
  return finishMatch({ ...state, players });
}

function sidePots(state: PokerState): Array<{ amount: number; eligible: string[] }> {
  const levels = [...new Set(state.players.map((player) => player.totalBet).filter((value) => value > 0))]
    .sort((a, b) => a - b);
  const pots: Array<{ amount: number; eligible: string[] }> = [];
  let previous = 0;
  for (const level of levels) {
    const contributors = state.players.filter((player) => player.totalBet >= level).length;
    const amount = (level - previous) * contributors;
    const eligible = state.players
      .filter((player) => !player.folded && !player.out && player.totalBet >= level)
      .map((player) => player.id);
    if (amount > 0) {
      if (eligible.length === 0 && pots.length > 0) {
        const last = pots[pots.length - 1];
        if (last) last.amount += amount;
      } else if (eligible.length === 0) {
        pots.push({ amount, eligible: state.players.filter((player) => !player.folded && !player.out).map((player) => player.id) });
      } else {
        pots.push({ amount, eligible });
      }
    }
    previous = level;
  }
  return pots;
}

function refundUncalled(state: PokerState): PokerState {
  const contributions = [...state.players.map((player) => player.totalBet)].sort((a, b) => b - a);
  const top = contributions[0] ?? 0;
  const second = contributions[1] ?? 0;
  if (top <= second) return state;
  const topPlayer = state.players.find((player) => player.totalBet === top);
  if (!topPlayer) return state;
  const refund = top - second;
  const players = state.players.map((player) => (player.id === topPlayer.id
    ? { ...player, chips: player.chips + refund, totalBet: player.totalBet - refund }
    : player));
  return { ...state, players };
}

function showdown(state: PokerState): { state: PokerState; events: LogEvent[] } {
  const refunded = refundUncalled(state);
  const evaluations = new Map<string, HandEval>();
  for (const player of refunded.players) {
    if (player.folded || player.out) continue;
    evaluations.set(player.id, evaluateHand([...player.hole, ...refunded.community]));
  }
  const pots = sidePots(refunded);
  const events: LogEvent[] = [];
  const wonBy = new Map<string, number>();
  const potResults: Array<{ amount: number; winners: string[] }> = [];

  for (const pot of pots) {
    const contenders = pot.eligible
      .map((id) => ({ id, hand: evaluations.get(id) }))
      .filter((entry): entry is { id: string; hand: HandEval } => Boolean(entry.hand));
    if (contenders.length === 0) continue;
    let best = contenders[0] as { id: string; hand: HandEval };
    for (const contender of contenders) {
      if (compareHands(contender.hand, best.hand) > 0) best = contender;
    }
    const winners = contenders.filter((entry) => compareHands(entry.hand, best.hand) === 0).map((entry) => entry.id);
    const share = Math.floor(pot.amount / winners.length);
    let remainder = pot.amount - share * winners.length;
    for (const winnerId of winners) {
      const extra = remainder > 0 ? 1 : 0;
      remainder -= extra;
      const amount = share + extra;
      wonBy.set(winnerId, (wonBy.get(winnerId) ?? 0) + amount);
      const player = refunded.players.find((entry) => entry.id === winnerId);
      if (player) {
        events.push(logPoker('log.pokerTake', {
          p: playerName(state.seats, winnerId),
          amount,
          hand: best.hand.nameKey,
        }, 'success', winnerId));
      }
    }
    potResults.push({ amount: pot.amount, winners });
  }

  if (wonBy.size === 0) {
    const live = livePlayers(refunded);
    const fallback = live[0];
    if (fallback) wonBy.set(fallback.id, potTotal(refunded));
  }

  const totalPot = potTotal(refunded);
  const players = refunded.players.map((player) => {
    const won = wonBy.get(player.id) ?? 0;
    return {
      ...player,
      chips: player.chips + won,
      bet: 0,
      totalBet: 0,
    };
  });

  const rows: ShowdownRow[] = refunded.players
    .filter((player) => player.hole.length > 0)
    .map((player) => {
      const hand = evaluations.get(player.id);
      return {
        playerId: player.id,
        name: playerName(state.seats, player.id),
        hole: player.hole,
        handKey: hand?.nameKey ?? (player.folded ? 'poker.hand.folded' : 'poker.hand.high'),
        best: hand?.cards ?? [],
        folded: player.folded,
        won: wonBy.get(player.id) ?? 0,
      };
    });

  const winnerIds = [...wonBy.keys()];
  events.push(logPoker('log.pokerShowdown', {
    p: winnerIds.map((id) => playerName(state.seats, id)).join(' · '),
    pot: totalPot,
  }, 'magic'));

  const next: PokerState = {
    ...refunded,
    players,
    street: 'handEnd',
    turn: -1,
    showdown: { rows, pots: potResults, totalPot },
    handWinner: winnerIds[0]
      ? { playerId: winnerIds[0], amount: wonBy.get(winnerIds[0]) ?? 0, folded: false }
      : undefined,
  };
  return { state: checkEliminations(next), events };
}

function completeStreet(state: PokerState): { state: PokerState; events: LogEvent[] } {
  const canAct = state.players.filter((player) => !player.out && !player.folded && !player.allIn && player.chips > 0);
  if (state.street === 'river' || canAct.length <= 1) {
    return runOutAndShowdown(state);
  }
  const nextStreet: PokerStreet = state.street === 'preflop' ? 'flop' : state.street === 'flop' ? 'turn' : 'river';
  const reset = resetStreet(state);
  const count = nextStreet === 'flop' ? 3 : 1;
  const dealt = dealCards(reset, count);
  const events = [
    logPoker('log.pokerStreet', { street: `street.${nextStreet}`, n: dealt.cards.length }, 'info'),
  ];
  const first = nextToAct({ ...dealt.state, street: nextStreet, turn: -1 }, state.dealer);
  if (first < 0) {
    return runOutAndShowdown({ ...dealt.state, street: nextStreet });
  }
  return {
    state: { ...dealt.state, street: nextStreet, turn: first },
    events,
  };
}

function runOutAndShowdown(state: PokerState): { state: PokerState; events: LogEvent[] } {
  let current: PokerState = { ...state, players: state.players.map((player) => ({ ...player, bet: 0, acted: false })), currentBet: 0 };
  const events: LogEvent[] = [];
  while (current.community.length < 5) {
    const need = current.community.length === 0 ? 3 : 1;
    const street: PokerStreet = current.community.length === 0 ? 'flop' : current.community.length === 3 ? 'turn' : 'river';
    const dealt = dealCards(current, need);
    current = { ...dealt.state, street };
    events.push(logPoker('log.pokerStreet', { street: `street.${street}`, n: need }, 'info'));
  }
  const result = showdown({ ...current, street: 'showdown' });
  return { state: result.state, events: [...events, ...result.events] };
}

function afterAction(state: PokerState): { state: PokerState; events: LogEvent[] } {
  const live = livePlayers(state);
  if (live.length === 1) return awardUncontested(state, live[0] as PokerPlayer);
  const next = nextToAct(state, state.turn);
  if (next >= 0) return { state: { ...state, turn: next }, events: [] };
  return completeStreet(state);
}

function bettingActions(state: PokerState, viewerId: string): UIAction[] {
  if (!isBettingStreet(state.street)) return [];
  const player = state.players.find((entry) => entry.id === viewerId);
  if (!player || player.folded || player.out || player.allIn) return [];
  if (state.seats[state.turn]?.id !== viewerId) return [];
  const toCall = Math.max(0, state.currentBet - player.bet);
  const actions: UIAction[] = [
    makeAction('fold', 'action.fold', { type: 'fold' }, { tone: 'danger', icon: 'flag' }),
  ];
  if (toCall === 0) {
    actions.push(makeAction('check', 'action.check', { type: 'check' }, { tone: 'secondary', icon: 'hand' }));
  } else {
    actions.push(makeAction('call', 'action.call', { type: 'call' }, {
      tone: 'primary',
      icon: 'hand',
      labelParams: { amount: Math.min(toCall, player.chips) },
    }));
  }
  if (player.chips > 0) {
    actions.push(makeAction('allin', 'action.allIn', { type: 'allin' }, {
      tone: 'danger',
      icon: 'sparkles',
      labelParams: { amount: player.chips },
    }));
  }
  return actions;
}

function playerInfo(state: PokerState) {
  return state.players.map((player) => ({
    id: player.id,
    chips: player.chips,
    bet: player.bet,
    totalBet: player.totalBet,
    folded: player.folded,
    allIn: player.allIn,
    out: player.out,
    lastMove: player.lastMove ?? null,
    holeCount: player.hole.length,
  }));
}

function botStrength(state: PokerState, player: PokerPlayer): number {
  if (state.community.length >= 3) {
    const hand = evaluateHand([...player.hole, ...state.community]);
    const high = hand.ranks[0] ?? 0;
    return Math.max(0, Math.min(1, hand.category / 8 * 0.86 + (high / 14) * 0.14));
  }
  const [a = 0, b = 0] = player.hole.map((card) => card.rank).sort((x, y) => y - x);
  let score = ((a + b) / 28) * 0.58;
  if (a === b) score += 0.28;
  if (player.hole[0]?.suit === player.hole[1]?.suit) score += 0.045;
  if (Math.abs(a - b) <= 1) score += 0.05;
  if (a >= 12 && b >= 10) score += 0.07;
  if (a === 14) score += 0.02;
  return Math.max(0, Math.min(1, score));
}

function botAction(state: PokerState, botId: string, difficulty: Difficulty, rng: Rng): GameAction | null {
  if (!isBettingStreet(state.street)) return null;
  const index = state.players.findIndex((player) => player.id === botId);
  const player = state.players[index];
  if (!player || player.folded || player.out || player.allIn) return null;
  if (state.seats[state.turn]?.id !== botId) return null;

  const toCall = Math.max(0, state.currentBet - player.bet);
  const pot = potTotal(state);
  const maxTarget = player.bet + player.chips;
  const noise = (rng() - 0.5) * (difficulty === 'hard' ? 0.12 : difficulty === 'easy' ? 0.3 : 0.2);
  const strength = Math.max(0, Math.min(1, botStrength(state, player) + noise));
  const raiseTarget = (fraction: number): number => {
    const base = Math.max(state.currentBet + state.minRaise, Math.round(((pot + toCall) * fraction) / state.bigBlind) * state.bigBlind);
    return Math.min(maxTarget, Math.max(state.currentBet + 1, base));
  };

  if (toCall === 0) {
    if (strength > 0.72) {
      const target = raiseTarget(0.55);
      if (target > state.currentBet && target <= maxTarget) return { type: 'raise', amount: target };
    }
    if (strength < 0.3 && rng() < (difficulty === 'hard' ? 0.08 : 0.14)) {
      const target = raiseTarget(0.35);
      if (target > state.currentBet && target <= maxTarget) return { type: 'raise', amount: target };
    }
    return { type: 'check' };
  }

  const allInCall = toCall >= player.chips;
  if (allInCall) {
    if (strength > 0.56 || rng() < 0.12) return { type: 'call' };
    return { type: 'fold' };
  }
  const odds = toCall / (pot + toCall);
  if (strength > 0.8 && maxTarget > state.currentBet) {
    return { type: 'raise', amount: raiseTarget(0.7) };
  }
  if (strength > odds + (difficulty === 'hard' ? 0.24 : 0.15)) return { type: 'call' };
  if (toCall <= state.bigBlind / 2 && strength > 0.35) return { type: 'call' };
  return { type: 'fold' };
}

function applyAction(
  state: PokerState,
  actorId: string,
  action: GameAction,
): { state: PokerState; events: LogEvent[]; error?: string } {
  const index = state.players.findIndex((player) => player.id === actorId);
  const current = state.players[index];
  if (!current || index < 0) return { state, events: [], error: 'error.invalidAction' };
  if (state.seats[state.turn]?.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };
  if (!isBettingStreet(state.street)) return { state, events: [], error: 'error.notNow' };

  const players = clonePlayers(state.players);
  const player = players[index] as PokerPlayer;
  const toCall = Math.max(0, state.currentBet - player.bet);
  const name = playerName(state.seats, actorId);
  const events: LogEvent[] = [];
  const type = String(action.type ?? '');
  let nextMinRaise = state.minRaise;

  if (type === 'fold') {
    player.folded = true;
    player.acted = true;
    player.lastMove = { kind: 'fold' };
    events.push(logPoker('log.pokerFold', { p: name }, 'warn', actorId));
  } else if (type === 'check') {
    if (toCall > 0) return { state, events: [], error: 'error.pokerCallFirst' };
    player.acted = true;
    player.lastMove = { kind: 'check' };
    events.push(logPoker('log.pokerCheck', { p: name }, 'info', actorId));
  } else if (type === 'call') {
    if (toCall <= 0) return { state, events: [], error: 'error.pokerNothingToCall' };
    const pay = Math.min(toCall, player.chips);
    player.chips -= pay;
    player.bet += pay;
    player.totalBet += pay;
    player.acted = true;
    if (player.chips === 0) player.allIn = true;
    player.lastMove = { kind: 'call', amount: pay, allIn: player.allIn };
    events.push(logPoker('log.pokerCall', { p: name, amount: pay }, 'info', actorId));
  } else if (type === 'raise' || type === 'bet' || type === 'allin') {
    const maxTarget = player.bet + player.chips;
    let target = type === 'allin' ? maxTarget : Math.floor(Number(action.amount));
    if (!Number.isFinite(target)) return { state, events: [], error: 'error.invalidAction' };
    if (target > maxTarget) target = maxTarget;
    if (target <= state.currentBet) {
      if (maxTarget > state.currentBet && type === 'allin') {
        target = maxTarget;
      } else if (toCall > 0) {
        const pay = Math.min(toCall, player.chips);
        player.chips -= pay;
        player.bet += pay;
        player.totalBet += pay;
        player.acted = true;
        if (player.chips === 0) player.allIn = true;
        player.lastMove = { kind: 'call', amount: pay, allIn: player.allIn };
        events.push(logPoker('log.pokerCall', { p: name, amount: pay }, 'info', actorId));
        const after = afterAction({
          ...state,
          players,
          currentBet: Math.max(0, ...players.map((entry) => entry.bet)),
          actionLog: [...state.actionLog, { playerId: actorId, move: player.lastMove }],
        });
        return { state: after.state, events: [...events, ...after.events] };
      } else {
        return { state, events: [], error: 'error.pokerMinRaise' };
      }
    } else {
      const previousBet = state.currentBet;
      const raiseSize = target - previousBet;
      const allIn = target >= maxTarget;
      if (raiseSize < state.minRaise && !allIn) return { state, events: [], error: 'error.pokerMinRaise' };
      if (raiseSize >= state.minRaise) nextMinRaise = raiseSize;
      const pay = target - player.bet;
      player.chips -= pay;
      player.bet = target;
      player.totalBet += pay;
      player.acted = true;
      if (player.chips === 0) player.allIn = true;
      player.lastMove = { kind: 'raise', amount: target, allIn: player.allIn };
      for (const other of players) {
        if (other.id !== actorId && !other.folded && !other.out && !other.allIn) other.acted = false;
      }
      events.push(logPoker(player.allIn ? 'log.pokerAllIn' : 'log.pokerRaise', {
        p: name,
        amount: target,
      }, player.allIn ? 'warn' : 'success', actorId));
    }
  } else if (type === 'nextHand') {
    return { state, events: [], error: 'error.notNow' };
  } else {
    return { state, events: [], error: 'error.invalidAction' };
  }

  const currentBet = Math.max(0, ...players.map((entry) => entry.bet));
  const withBets: PokerState = { ...state, players, currentBet, minRaise: nextMinRaise };
  const move: PlayerMove = player.lastMove ?? { kind: 'check' };
  const logged: PokerState = {
    ...withBets,
    actionLog: [...state.actionLog, { playerId: actorId, move }].slice(-12),
  };
  const after = afterAction(logged);
  return { state: after.state, events: [...events, ...after.events] };
}

export const pokerModule: GameModule<PokerState> = {
  meta: {
    id: 'poker',
    nameKey: 'game.poker.name',
    descKey: 'game.poker.desc',
    rulesKey: 'game.poker.rules',
    icon: '♠️',
    category: 'cartas',
    minPlayers: 2,
    maxPlayers: 6,
    avgMinutes: 22,
    accent: '#1f7a5c',
    tagKeys: ['tag.cartas', 'tag.estrategia', 'tag.apuestasFicticias'],
  },

  create(seats, options, rng) {
    const startingChips = Math.max(200, Math.floor(options.target ?? 800));
    const state: PokerState = {
      seats,
      options,
      players: seats.map((seat) => ({
        id: seat.id,
        chips: startingChips,
        hole: [],
        folded: false,
        allIn: false,
        bet: 0,
        totalBet: 0,
        acted: false,
        out: false,
      })),
      deck: [],
      community: [],
      dealer: 0,
      turn: 0,
      street: 'preflop',
      currentBet: 0,
      minRaise: 20,
      smallBlind: 10,
      bigBlind: 20,
      handNo: 0,
      actionLog: [],
      rng,
    };
    return startHand(state).state;
  },

  view(state, viewerId) {
    const viewer = state.players.find((player) => player.id === viewerId);
    const turnSeat = isBettingStreet(state.street) ? state.seats[state.turn] : null;
    const toCall = viewer ? Math.max(0, state.currentBet - viewer.bet) : 0;
    const callAmount = viewer ? Math.min(toCall, viewer.chips) : 0;
    const maxTarget = viewer ? viewer.bet + viewer.chips : 0;
    const minTarget = Math.min(maxTarget, state.currentBet + state.minRaise);
    const pot = potTotal(state);
    const isTurn = Boolean(turnSeat && turnSeat.id === viewerId);
    const hero = viewer ? evaluateHand([...viewer.hole, ...state.community]) : null;
    const canRaise = Boolean(viewer && isTurn && maxTarget > state.currentBet && viewer.chips > 0);
    const canCheck = Boolean(viewer && isTurn && toCall === 0);
    const canCall = Boolean(viewer && isTurn && toCall > 0);
    const actions = bettingActions(state, viewerId);

    let promptView: GameView['prompt'];
    if (state.street === 'handEnd') {
      promptView = prompt('prompt.pokerHandEnd', undefined, 'success');
    } else if (state.street === 'matchEnd') {
      promptView = prompt('prompt.matchEnd', undefined, 'success');
    } else if (state.street === 'showdown') {
      promptView = prompt('prompt.pokerShowdown', undefined, 'success');
    } else if (isTurn) {
      promptView = prompt(toCall > 0 ? 'prompt.pokerYourTurnCall' : 'prompt.pokerYourTurn', {
        amount: callAmount,
        pot,
      }, 'info');
    } else {
      promptView = prompt('prompt.waitingPlayer', { player: turnSeat?.name ?? '—' }, 'info');
    }

    return {
      gameId: 'poker',
      youId: viewerId,
      phase: state.street,
      turnId: turnSeat?.id ?? null,
      players: playerViews(state.seats, Object.fromEntries(state.players.map((player) => [player.id, player.chips])), turnSeat?.id ?? null, viewerId, (seat) => {
        const entry = state.players.find((player) => player.id === seat.id);
        return {
          cardsLeft: entry?.hole.length ?? 0,
          active: Boolean(entry && !entry.folded && !entry.out),
        };
      }),
      hand: viewer ? viewer.hole.map((card) => ({ ...card })) : [],
      center: {
        cards: state.community,
        piles: [{ id: 'pot', labelKey: 'poker.pot', count: pot }],
      },
      actions,
      prompt: promptView,
      extra: {
        handNo: state.handNo,
        street: state.street,
        pot,
        currentBet: state.currentBet,
        smallBlind: state.smallBlind,
        bigBlind: state.bigBlind,
        dealerId: state.seats[state.dealer]?.id ?? null,
        toCall,
        callAmount,
        minRaise: state.minRaise,
        minTarget,
        maxTarget,
        chips: viewer?.chips ?? 0,
        isTurn,
        canCheck,
        canCall,
        canRaise,
        heroHandKey: hero?.nameKey ?? 'poker.hand.high',
        heroBest: hero?.cards ?? [],
        playersInfo: playerInfo(state),
        actionLog: state.actionLog.slice(-6),
        showdown: state.showdown ?? null,
        handWinner: state.handWinner ?? null,
      },
    };
  },

  act(state, action, actorId, _rng): ActResult<PokerState> {
    const type = String(action.type ?? '');
    if (type === 'nextHand') {
      if (state.street !== 'handEnd') return { state, events: [], error: 'error.notNow' };
      const next = prepareNextHand(state);
      if (next.street === 'matchEnd') return { state: next, events: [] };
      return { state: next, events: [logPoker('log.pokerNextHand', { n: next.handNo }, 'info')] };
    }
    const applied = applyAction(state, actorId, action);
    return {
      state: applied.state,
      events: applied.events,
      ...(applied.error ? { error: applied.error } : {}),
    };
  },

  bot(state, botId, difficulty, rng) {
    return botAction(state, botId, difficulty, rng);
  },

  result(state) {
    return state.result ?? null;
  },
};

export type { PokerPlayer, ShowdownRow, ShowdownInfo };
