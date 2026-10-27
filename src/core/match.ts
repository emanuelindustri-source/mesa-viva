import { getGame } from './registry';
import { makeRng, randomSeed } from './rng';
import type {
  Difficulty,
  GameAction,
  GameId,
  GameModule,
  GameOptions,
  GameView,
  LogEvent,
  MatchResult,
  Rng,
  Seat,
} from './types';
import { event } from '../games/cards';

export interface MatchConfig {
  gameId: GameId;
  seats: Seat[];
  options: GameOptions;
  difficulty: Difficulty;
  seed?: number;
}

export interface ActOutcome {
  ok: boolean;
  error?: string;
  events: LogEvent[];
}

/**
 * Anfitrión autoritativo de una partida. Mantiene el estado completo, el RNG
 * y el historial público. Los clientes remotos solo reciben `view()`.
 */
export class Match {
  readonly module: GameModule<unknown>;
  readonly config: MatchConfig;
  readonly seed: number;
  readonly startedAt: number;
  readonly rng: Rng;
  state: unknown;
  log: LogEvent[] = [];
  result: MatchResult | null = null;
  private ended = false;

  constructor(config: MatchConfig) {
    const module = getGame(config.gameId);
    if (!module) throw new Error(`Juego desconocido: ${config.gameId}`);
    this.module = module;
    this.config = config;
    this.seed = config.seed ?? randomSeed();
    this.rng = makeRng(this.seed);
    this.startedAt = Date.now();
    this.state = module.create(config.seats, config.options, this.rng);
    this.log.push(
      event('log.matchStart', {
        game: module.meta.nameKey,
        players: config.seats.map((s) => s.name).join(' · '),
      }, 'info'),
    );
    this.syncResult();
  }

  get seats(): Seat[] {
    return this.config.seats;
  }

  currentActor(): Seat | null {
    if (this.result) return null;
    try {
      const view = this.module.view(this.state, this.config.seats[0]?.id ?? '');
      return this.config.seats.find((s) => s.id === view.turnId) ?? null;
    } catch {
      return null;
    }
  }

  view(viewerId: string): GameView {
    const base = this.module.view(this.state, viewerId);
    return {
      ...base,
      log: this.log,
      result: this.result,
      startedAt: this.startedAt,
    };
  }

  act(actorId: string, action: GameAction): ActOutcome {
    if (this.result) return { ok: false, error: 'error.matchOver', events: [] };
    if (!this.config.seats.some((s) => s.id === actorId)) {
      return { ok: false, error: 'error.invalidAction', events: [] };
    }
    try {
      const res = this.module.act(this.state, action, actorId, this.rng);
      if (res.error) return { ok: false, error: res.error, events: [] };
      this.state = res.state;
      if (res.events.length > 0) this.log.push(...res.events);
      this.syncResult();
      return { ok: true, events: res.events };
    } catch (error) {
      return {
        ok: false,
        error: error instanceof Error ? error.message : 'error.generic',
        events: [],
      };
    }
  }

  /** Elige y ejecuta una acción de bot. Devuelve el id del bot o null. */
  botStep(): { actorId: string; events: LogEvent[] } | null {
    if (this.result) return null;
    for (const seat of this.config.seats) {
      if (!seat.isBot) continue;
      let action: GameAction | null = null;
      try {
        action = this.module.bot(this.state, seat.id, this.config.difficulty, this.rng);
      } catch {
        action = null;
      }
      if (!action) continue;
      const outcome = this.act(seat.id, action);
      if (outcome.ok) return { actorId: seat.id, events: outcome.events };
    }
    return null;
  }

  private syncResult(): void {
    if (this.ended) return;
    const result = this.module.result(this.state);
    if (!result) return;
    this.result = result;
    this.ended = true;
    if (!this.log.some((e) => e.key === 'log.matchEnd' && e.at > this.startedAt - 50)) {
      const winner = this.config.seats.find((s) => s.id === result.winnerIds[0]);
      this.log.push(event('log.matchEnd', { p: winner?.name ?? '—' }, result.draw ? 'info' : 'magic'));
    }
  }

  get isOver(): boolean {
    return this.result !== null;
  }

  get durationMs(): number {
    return Date.now() - this.startedAt;
  }
}
