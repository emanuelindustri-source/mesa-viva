import type { CardView, DieView, LogEvent, PlayerView, Seat } from '../core/types';
import { uid } from '../core/utils';

export const SPANISH_SUITS = ['oros', 'copas', 'espadas', 'bastos'] as const;
export type SpanishSuit = (typeof SPANISH_SUITS)[number];
export const SPANISH_RANKS = [1, 2, 3, 4, 5, 6, 7, 10, 11, 12] as const;

export function spanishDeck(): CardView[] {
  const deck: CardView[] = [];
  for (const suit of SPANISH_SUITS) {
    for (const rank of SPANISH_RANKS) {
      deck.push({ id: `${suit}-${rank}`, suit, rank });
    }
  }
  return deck;
}

export const FURIA_COLORS = ['sol', 'mar', 'selva', 'magma'] as const;
export type FuriaColor = (typeof FURIA_COLORS)[number];

/** Baraja de 108 naipes inspirada en los juegos de combinar colores. */
export function furiaDeck(): CardView[] {
  const deck: CardView[] = [];
  for (const suit of FURIA_COLORS) {
    deck.push({ id: `${suit}-0`, suit, rank: 0 });
    for (let rank = 1; rank <= 9; rank += 1) {
      deck.push({ id: `${suit}-${rank}a`, suit, rank });
      deck.push({ id: `${suit}-${rank}b`, suit, rank });
    }
    for (const action of ['skip', 'reverse', 'draw2']) {
      deck.push({ id: `${suit}-${action}a`, suit, rank: -1, action });
      deck.push({ id: `${suit}-${action}b`, suit, rank: -1, action });
    }
  }
  for (let i = 0; i < 4; i += 1) {
    deck.push({ id: `wild-${i}`, suit: 'wild', rank: -1, action: 'wild' });
    deck.push({ id: `wild4-${i}`, suit: 'wild', rank: -1, action: 'wild4' });
  }
  return deck;
}

export function cardIds(cards: CardView[]): string {
  return cards.map((c) => c.id).join(',');
}

export function caidaRankPoints(rank: number): number {
  if (rank >= 10 && rank <= 12) return rank - 8;
  return 1;
}

export function event(
  key: string,
  params?: Record<string, unknown>,
  tone: LogEvent['tone'] = 'info',
  actorId?: string,
): LogEvent {
  return {
    id: uid('ev'),
    at: Date.now(),
    key,
    params,
    tone,
    ...(actorId ? { actorId } : {}),
  };
}

export function playerName(seats: Seat[], id: string): string {
  return seats.find((s) => s.id === id)?.name ?? '—';
}

export function playerViews(
  seats: Seat[],
  scores: Record<string, number>,
  turnId: string | null,
  viewerId: string,
  extra: (seat: Seat) => Pick<PlayerView, 'cardsLeft' | 'diceLeft' | 'active'> = () => ({ active: true }),
): PlayerView[] {
  return seats.map((seat) => {
    const e = extra(seat);
    return {
      id: seat.id,
      name: seat.name,
      avatar: seat.avatar,
      isBot: seat.isBot,
      ...(seat.team !== undefined ? { team: seat.team } : {}),
      score: scores[seat.id] ?? 0,
      isTurn: seat.id === turnId,
      isYou: seat.id === viewerId,
      ...e,
    };
  });
}

export function nextIndex(index: number, length: number, direction = 1): number {
  return (index + direction + length) % length;
}

export function teamScore(seats: Seat[], scores: Record<string, number>, team: number): number {
  return seats.filter((s) => s.team === team).reduce((total, s) => total + (scores[s.id] ?? 0), 0);
}

export function teamOf(seat: Seat | undefined): number | undefined {
  return seat?.team;
}

export function diceView(values: number[], hidden = false, held = false): DieView[] {
  return values.map((value, i) => ({ id: `d${i}`, value, hidden, held }));
}

export function shuffleWithRng<T>(items: T[], rng: () => number): T[] {
  const out = items.slice();
  for (let i = out.length - 1; i > 0; i -= 1) {
    const j = Math.floor(rng() * (i + 1));
    const a = out[i];
    const b = out[j];
    if (a !== undefined && b !== undefined) {
      out[i] = b;
      out[j] = a;
    }
  }
  return out;
}

export function sortCards(cards: CardView[]): CardView[] {
  return cards.slice().sort((a, b) => {
    const suitOrder = ['oros', 'copas', 'espadas', 'bastos', 'sol', 'mar', 'selva', 'magma', 'wild'];
    const sa = suitOrder.indexOf(a.suit);
    const sb = suitOrder.indexOf(b.suit);
    if (sa !== sb) return sa - sb;
    return a.rank - b.rank;
  });
}

export function rankOrder(rank: number): number {
  if (rank === 1) return 1;
  if (rank === 12) return 10;
  if (rank === 11) return 11;
  if (rank === 10) return 12;
  return rank;
}

export function rankFromOrder(order: number): number {
  if (order === 10) return 12;
  if (order === 11) return 11;
  if (order === 12) return 10;
  return order;
}
