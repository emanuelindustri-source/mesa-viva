import { useEffect, useMemo, useRef, useState } from 'react';
import type { CSSProperties, ReactNode } from 'react';
import {
  ArrowRight,
  BookOpen,
  Check,
  ChevronRight,
  Copy,
  Dices,
  Flag,
  Hand,
  Layers,
  ListOrdered,
  Megaphone,
  Pause,
  Sparkles,
  Volume2,
  VolumeX,
  WifiOff,
  Bot as BotIcon,
  Crown,
  Home,
} from 'lucide-react';
import type { GameAction, GameView, XpAwardResult } from '../core/types';
import { GAME_BY_ID } from '../core/registry';
import { randomSeed } from '../core/rng';
import { formatDuration } from '../core/utils';
import { formatLog } from '../i18n';
import { useT } from '../i18n/useT';
import { useAppStore } from '../store/appStore';
import { useSession } from '../store/session';
import { toast } from '../store/toasts';
import { useFestivalTheme } from '../theme/useTheme';
import { useWakeLock } from '../services/pwa';
import { haptic } from '../services/haptics';
import { useShakeDetector } from '../services/sensors';
import { diag, trackAction } from '../services/diagnostics';
import {
  initAudio,
  setAudioSettings,
  sfx,
  sfxForLog,
  startMusic,
  stopMusic,
} from '../audio/engine';
import { burst, burstFromElement } from './particles/bus';
import { Avatar, Button, Chip, IconButton, Modal } from './components';
import { GameSurface } from './GameSurface';
import { ResultScreen } from './Result';

const ACTION_ICONS: Record<string, ReactNode> = {
  sparkles: <Sparkles size={16} />,
  megaphone: <Megaphone size={16} />,
  layers: <Layers size={16} />,
  'arrow-right': <ArrowRight size={16} />,
  dices: <Dices size={16} />,
  hand: <Hand size={16} />,
  flag: <Flag size={16} />,
};

function isCaptureEvent(key: string): boolean {
  return ['log.caida', 'log.caidaMesa', 'log.checkerCapture', 'log.mancalaCapture', 'log.checkerPromote', 'log.chessMate', 'log.lastCard', 'log.canto', 'log.cantoWin'].includes(key);
}

function isWinEvent(key: string): boolean {
  return ['log.won', 'log.matchEnd', 'log.chessMate', 'log.cantoWin'].includes(key);
}

function burstKind(key: string): 'dust' | 'spark' | 'confetti' | 'ember' {
  if (isWinEvent(key)) return 'confetti';
  if (isCaptureEvent(key)) return 'spark';
  if (key === 'log.snake' || key === 'log.eliminated' || key === 'log.resign') return 'ember';
  return 'dust';
}

export function TableScreen() {
  const t = useT();
  const match = useSession((s) => s.match);
  const mode = useSession((s) => s.mode);
  const version = useSession((s) => s.version);
  const error = useSession((s) => s.error);
  const actRaw = useSession((s) => s.act);
  const botStep = useSession((s) => s.botStep);
  const start = useSession((s) => s.start);
  const clear = useSession((s) => s.clear);
  const setError = useSession((s) => s.setError);
  const settings = useAppStore((s) => s.settings);
  const setSettings = useAppStore((s) => s.setSettings);
  const recordMatch = useAppStore((s) => s.recordMatch);
  const { festival } = useFestivalTheme();

  const [busy, setBusy] = useState(false);
  const [logOpen, setLogOpen] = useState(false);
  const [rulesOpen, setRulesOpen] = useState(false);
  const [quitOpen, setQuitOpen] = useState(false);
  const [privacy, setPrivacy] = useState<{ id: string; name: string } | null>(null);
  const [award, setAward] = useState<XpAwardResult | null>(null);
  const [resultShown, setResultShown] = useState(false);
  const previousTurn = useRef<string | null>(null);
  const tableRef = useRef<HTMLDivElement>(null);
  const seenLog = useRef<{ seed: number; count: number } | null>(null);
  const awardedSeed = useRef<number | null>(null);

  useWakeLock(settings.keepAwake);

  const view = useMemo<GameView | null>(() => {
    if (!match) return null;
    const base = match.view('spectator');
    let acting = base.turnId;
    if (base.gameId === 'caida' && base.phase === 'entrega') {
      acting = (base.extra?.dealerId as string | undefined) ?? null;
    }
    let viewerId = 'spectator';
    if (mode === 'bots') viewerId = match.seats[0]?.id ?? 'spectator';
    else if (acting) {
      const seat = match.seats.find((s) => s.id === acting);
      if (seat && !seat.isBot) viewerId = seat.id;
    }
    return viewerId === 'spectator' ? base : match.view(viewerId);
  }, [match, version, mode]);

  const meta = view ? GAME_BY_ID[view.gameId] : null;
  const me = match?.seats[0];
  const profileViewerId = me?.id ?? view?.youId ?? '';
  const isMyTurn = Boolean(view && view.turnId && view.youId === view.turnId);
  const activeSeat = match?.seats.find((seat) => seat.id === view?.turnId);
  const pokerBetting = Boolean(view?.gameId === 'poker' && ['preflop', 'flop', 'turn', 'river'].includes(view.phase));
  const botThinking = Boolean(
    match
      && !match.isOver
      && (view?.gameId === 'poker' ? pokerBetting : view?.phase === 'play')
      && activeSeat?.isBot
      && !privacy,
  );
  const cardGame = meta?.category === 'cartas';
  const lastEvents = useMemo(() => (view?.log ?? []).slice(-5).reverse(), [view?.log, version]);
  const round = Number(view?.extra?.round ?? view?.extra?.handNo ?? 0);
  const roundLabel = view && round > 0
    ? view.gameId === 'caida' || view.gameId === 'poker'
      ? t('table.handNo', { n: round })
      : t('table.round', { n: round })
    : t('table.matchCode', { code: match?.seed.toString(36).slice(-4).toUpperCase() ?? '' });

  useEffect(() => {
    if (!error) return;
    toast(t(error), 'danger');
    diag('warn', `Acción rechazada: ${error}`, undefined, match?.config.gameId);
    setError(null);
  }, [error, t, setError, match?.config.gameId]);

  useEffect(() => {
    if (!view || !match) return;
    if (mode !== 'hotseat') {
      setPrivacy(null);
      previousTurn.current = view.turnId;
      return;
    }
    const hiddenPhase = view.gameId === 'poker'
      ? ['preflop', 'flop', 'turn', 'river'].includes(view.phase)
      : view.phase === 'play';
    if (!hiddenPhase || !view.turnId) {
      setPrivacy(null);
      previousTurn.current = view.turnId;
      return;
    }
    if (previousTurn.current !== view.turnId) {
      previousTurn.current = view.turnId;
      const seat = match.seats.find((s) => s.id === view.turnId);
      if (seat && !seat.isBot) setPrivacy({ id: seat.id, name: seat.name });
      else setPrivacy(null);
    }
  }, [view?.turnId, view?.phase, mode, match]);

  useEffect(() => {
    if (!match || match.isOver || privacy) return;
    const lastKey = view?.log?.[view.log.length - 1]?.key ?? '';
    const slowCaida = view?.gameId === 'caida' && [
      'log.caida',
      'log.caidaMesa',
      'log.canto',
      'log.deliveryOk',
      'log.deliveryFail',
      'log.handEnd',
    ].includes(lastKey);
    const delay = view?.gameId === 'caida' ? 1500 + (slowCaida ? 700 : 0) : 720;
    const timer = window.setTimeout(() => {
      botStep();
    }, delay);
    return () => window.clearTimeout(timer);
  }, [match, version, privacy, botStep, view?.gameId, view?.log?.length]);

  useEffect(() => {
    if (!match || match.isOver || view?.phase !== 'handEnd' || privacy) return;
    const timer = window.setTimeout(() => {
      dispatch({ type: 'nextHand' });
    }, 900);
    return () => window.clearTimeout(timer);
  }, [match, view?.phase, view?.extra?.handNo, privacy]);

  useEffect(() => {
    if (!view || !match) return;
    const log = view.log ?? [];
    if (!seenLog.current || seenLog.current.seed !== match.seed) {
      seenLog.current = { seed: match.seed, count: log.length };
      sfx('deal');
      return;
    }
    if (log.length <= seenLog.current.count) {
      seenLog.current = { seed: match.seed, count: log.length };
      return;
    }
    const fresh = log.slice(seenLog.current.count);
    seenLog.current = { seed: match.seed, count: log.length };
    const relevant = fresh.slice(-3);
    for (const event of relevant) {
      const sound = sfxForLog(event.key);
      if (sound) sfx(sound);
      if (settings.haptics) {
        if (isWinEvent(event.key)) haptic('win');
        else if (event.key === 'log.canto' || event.key === 'log.cantoWin') haptic('canto');
        else if (event.key === 'log.roll' || event.key === 'log.bid') haptic('dice');
        else if (isCaptureEvent(event.key)) haptic('capture');
        else haptic('tap');
      }
    }
    if (!settings.reduceMotion) {
      const burstEvent = [...relevant].reverse().find((event) => isCaptureEvent(event.key) || isWinEvent(event.key)) ?? relevant[relevant.length - 1];
      if (burstEvent) {
        const kind = burstKind(burstEvent.key);
        burstFromElement(tableRef.current, kind, {
          colors: [festival.accent, festival.accent2, '#fff4d6'],
          count: kind === 'confetti' ? 28 : kind === 'spark' ? 14 : 8,
          power: kind === 'confetti' ? 1 : 0.9,
        });
      }
    }
  }, [version, match?.seed, view?.log?.length, settings.haptics, settings.reduceMotion, festival.accent, festival.accent2]);

  useEffect(() => {
    if (!match || !view?.result) {
      if (resultShown) setResultShown(false);
      return;
    }
    if (awardedSeed.current === match.seed) {
      setResultShown(true);
      return;
    }
    awardedSeed.current = match.seed;
    setResultShown(true);
    const winner = view.result.winnerIds.includes(profileViewerId);
    const draw = Boolean(view.result.draw);
    const points = view.result.scores[profileViewerId] ?? 0;
    const result = recordMatch({
      gameId: view.gameId,
      won: winner,
      draw,
      points,
      difficulty: match.config.difficulty,
      humanPlayers: match.seats.filter((s) => !s.isBot).length,
      durationMs: match.durationMs,
      players: match.seats.map((s) => s.name),
    });
    setAward(result);
    trackAction('match:finish', `${view.gameId}:${winner ? 'win' : draw ? 'draw' : 'loss'}`);
    if (result.leveledUp) {
      sfx('level');
      haptic('level');
      burst('confetti', window.innerWidth / 2, window.innerHeight * 0.25, { count: 60 });
    }
  }, [match, view?.result, profileViewerId, recordMatch, resultShown]);

  useEffect(() => {
    void initAudio();
    return () => {
      stopMusic();
    };
  }, []);

  useEffect(() => {
    setAudioSettings({ music: settings.music, sfx: settings.sfx, volume: settings.volume });
  }, [settings.music, settings.sfx, settings.volume]);

  useEffect(() => {
    if (!view || !settings.music) {
      stopMusic();
      return;
    }
    let active = true;
    void initAudio().then((ready) => {
      if (ready && active) startMusic(festival, view.gameId);
    });
    return () => {
      active = false;
      stopMusic();
    };
  }, [festival, view?.gameId, settings.music]);

  const shake = useShakeDetector({
    enabled: Boolean(view) && Boolean(settings.haptics),
    onShake: (intensity) => {
      if (!view || !match) return;
      if (view.gameId === 'serpientes' && view.youId === view.turnId && !busy) {
        dispatch({ type: 'roll' }, intensity);
      } else if (view.gameId === 'caida' && view.phase === 'entrega' && view.actions.some((a) => a.action.type === 'deliver')) {
        dispatch({ type: 'deliver' }, intensity);
      } else {
        haptic('dice');
        sfx('dice');
        if (!settings.reduceMotion) burstFromElement(tableRef.current, 'spark', { count: 10 });
      }
    },
  });

  function dispatch(action: GameAction, intensity = 1) {
    if (!match || !view || busy) return;
    const actorId = view.youId && view.youId !== 'spectator' ? view.youId : match.seats[0]?.id ?? '';
    if (action.type === 'play' || action.type === 'canto') {
      const current = match.view(actorId);
      if (current.turnId !== actorId || current.phase !== 'play') {
        toast(t('table.turnChanged'), 'warn');
        return;
      }
    }
    setBusy(true);
    const ok = actRaw(actorId, action);
    if (!ok) {
      haptic('error');
    } else if (!settings.reduceMotion && intensity > 0.6) {
      burstFromElement(tableRef.current, 'dust', { count: 8 });
    }
    window.setTimeout(() => setBusy(false), 220);
  }

  const handleQuit = () => {
    trackAction('match:quit', view?.gameId);
    clear();
    setQuitOpen(false);
    window.location.hash = '#/';
  };

  const handleRematch = () => {
    if (!match) return;
    const config = { ...match.config, seed: randomSeed() };
    setAward(null);
    setPrivacy(null);
    previousTurn.current = null;
    seenLog.current = null;
    awardedSeed.current = null;
    setResultShown(false);
    start(config, mode);
  };

  const handleHome = () => {
    clear();
    window.location.hash = '#/';
  };

  if (!match || !view || !meta) {
    return (
      <div className="screen table-missing">
        <div className="missing-card">
          <span className="big-emoji">🃏</span>
          <h1>{t('table.paused')}</h1>
          <p>{t('notfound.desc')}</p>
          <Button variant="primary" icon={<Home size={18} />} onClick={handleHome}>{t('common.home')}</Button>
        </div>
      </div>
    );
  }

  const gameName = t(meta.nameKey);
  if (resultShown && view.result) {
    return (
      <ResultScreen
        view={view}
        award={award}
        gameName={gameName}
        festival={festival}
        youId={profileViewerId}
        onRematch={handleRematch}
        onHome={handleHome}
      />
    );
  }

  return (
    <div className="table-screen">
      <header className="table-topbar">
        <div className="table-top-left">
          <IconButton label={t('common.back')} onClick={() => setQuitOpen(true)}>
            <ChevronRight size={20} className="flip-x" />
          </IconButton>
          <div className="table-title">
            <span className="eyebrow">{t(`tag.${meta.tagKeys[0]?.replace('tag.', '') ?? 'clasico'}`) || meta.category}</span>
            <h1>{gameName}</h1>
            <span className="table-round">{roundLabel}</span>
          </div>
        </div>
        <div className="table-top-actions">
          <IconButton label={settings.music ? t('table.soundOn') : t('table.soundOff')} onClick={() => setSettings({ music: !settings.music, sfx: !settings.sfx })}>
            {settings.music ? <Volume2 size={19} /> : <VolumeX size={19} />}
          </IconButton>
          <IconButton label={t('table.fullLog')} onClick={() => setLogOpen(true)}>
            <ListOrdered size={19} />
          </IconButton>
          <IconButton label={t('table.rules')} onClick={() => setRulesOpen(true)}>
            <BookOpen size={19} />
          </IconButton>
          <IconButton label={t('table.leave')} onClick={() => setQuitOpen(true)}>
            <Pause size={19} />
          </IconButton>
        </div>
      </header>

      <div className="opponents-strip">
        {view.players.map((player) => (
          <div
            key={player.id}
            className={`opponent ${player.isYou ? 'is-you' : ''} ${player.isTurn ? 'is-turn' : ''} ${!player.active ? 'is-out' : ''} ${player.isTurn && botThinking ? 'is-thinking' : ''}`}
          >
            <div className="opponent-avatar-wrap">
              <Avatar id={player.avatar} size={38} ring={player.isTurn} />
              {player.isYou ? <span className="opponent-you">{t('common.you')}</span> : null}
              {player.isBot ? <span className="opponent-bot" title={t('lobby.botBadge')}><BotIcon size={11} /></span> : null}
            </div>
            <div className="opponent-info">
              <span className="opponent-name">{player.name}</span>
              <span className="opponent-meta">
                {player.team !== undefined ? <span className="team-dot">P{player.team + 1}</span> : null}
                <span className="opponent-points"><strong>{player.score}</strong><small>{t('table.pointsShort')}</small></span>
                {player.cardsLeft !== undefined ? <span className="opponent-cards">🃏 {player.cardsLeft}</span> : null}
                {player.diceLeft !== undefined ? <span className="opponent-cards">🎲 {player.diceLeft}</span> : null}
              </span>
            </div>
            {player.isTurn ? <span className="turn-pulse" aria-label={botThinking ? t('table.thinking') : t('table.yourTurn')} /> : null}
            {player.isTurn && botThinking ? <span className="thinking-dots" aria-hidden="true"><i /><i /><i /></span> : null}
          </div>
        ))}
      </div>

      <div className="table-prompt-row">
        <div className={`prompt ${view.prompt?.tone ?? 'info'}`}>
          {view.prompt ? t(view.prompt.key, view.prompt.params) : isMyTurn ? t('table.yourTurn') : t('table.waiting')}
        </div>
        {mode === 'hotseat' ? <Chip tone="purple">{t('modes.hotseat')}</Chip> : <Chip tone="green">{t('modes.bots')}</Chip>}
      </div>

      <div
        className="table-surface-wrap"
        ref={tableRef}
        style={{ '--season-accent': festival.accent } as CSSProperties}
      >
        <GameSurface view={view} act={dispatch} busy={busy} />
      </div>

      <div className={`action-bar ${view.actions.length === 0 ? 'is-empty' : ''}`} aria-hidden={view.actions.length === 0}>
        {view.actions.map((action) => (
          <Button
            key={action.id}
            variant={action.tone === 'primary' ? 'primary' : action.tone === 'danger' ? 'danger' : action.tone === 'ghost' ? 'ghost' : 'secondary'}
            icon={action.icon ? ACTION_ICONS[action.icon] : undefined}
            disabled={action.disabled || busy}
            onClick={() => dispatch(action.action)}
          >
            {t(action.labelKey, action.labelParams)}
          </Button>
        ))}
      </div>

      <section className="table-log-ticker">
        <div className="log-ticker-head">
          <span><ListOrdered size={15} /> {t('table.lastMoves')}</span>
          <button type="button" onClick={() => setLogOpen(true)}>{t('table.fullLog')} <ChevronRight size={14} /></button>
        </div>
        <div className="log-ticker-list">
          {lastEvents.length === 0 ? <span className="log-empty">{t('table.logEmpty')}</span> : lastEvents.map((event) => (
            <div key={event.id} className={`log-ticker-line tone-${event.tone ?? 'info'}`}>
              <span className="log-dot" />
              <span>{formatLog(event, t)}</span>
            </div>
          ))}
        </div>
      </section>

      {!cardGame ? (
        <div className="table-hints">
          <span>{t('table.playHint')}</span>
          <span>{settings.reduceMotion ? t('table.animationOff') : t('table.animationOn')}</span>
        </div>
      ) : null}

      <Modal open={logOpen} onClose={() => setLogOpen(false)} title={t('table.fullLog')} subtitle={t('table.fullLogHint')} size="lg" variant="bottom">
        <div className="full-log">
          {(view.log ?? []).map((event, i) => (
            <div key={event.id} className={`log-line tone-${event.tone ?? 'info'}`}>
              <span className="log-index">{i + 1}</span>
              <span className="log-time">{new Date(event.at).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}</span>
              <span className="log-text">{formatLog(event, t)}</span>
            </div>
          ))}
        </div>
        <div className="modal-inline-actions">
          <Button
            variant="secondary"
            icon={<Copy size={16} />}
            onClick={async () => {
              const text = (view.log ?? []).map((e) => formatLog(e, t)).join('\n');
              try {
                await navigator.clipboard.writeText(text);
                toast(t('notify.logCopied'), 'success');
              } catch {
                toast(t('notify.error'), 'danger');
              }
            }}
          >
            {t('table.copyLog')}
          </Button>
        </div>
      </Modal>

      <Modal open={rulesOpen} onClose={() => setRulesOpen(false)} title={gameName} subtitle={t(meta.descKey)} size="lg" variant="bottom">
        <div className="rules-content">
          <div className="rules-meta">
            <Chip tone="gold">{meta.minPlayers}–{meta.maxPlayers} {t('common.players')}</Chip>
            <Chip tone="blue">{meta.avgMinutes} {t('common.min')}</Chip>
            <Chip tone="green">{t(`tag.${meta.tagKeys[0]?.replace('tag.', '') ?? 'clasico'}`)}</Chip>
          </div>
          <p>{t(meta.rulesKey)}</p>
          <p className="rules-note">{t('game.caida.variant')}</p>
          <Button variant="primary" onClick={() => setRulesOpen(false)}>{t('rules.close')}</Button>
        </div>
      </Modal>

      <Modal open={quitOpen} onClose={() => setQuitOpen(false)} title={t('table.quitTitle')} subtitle={t('table.quitText')} size="sm">
        <div className="modal-inline-actions">
          <Button variant="ghost" onClick={() => setQuitOpen(false)}>{t('common.cancel')}</Button>
          <Button variant="danger" onClick={handleQuit}>{t('table.quitConfirm')}</Button>
        </div>
      </Modal>

      <Modal
        open={Boolean(privacy)}
        onClose={() => setPrivacy(null)}
        dismissible={false}
        title={t('modes.hotseat')}
        subtitle={t('setup.hotseatHint')}
        size="sm"
      >
        <div className="privacy-card">
          <Avatar id={match.seats.find((s) => s.id === privacy?.id)?.avatar ?? 'jaguar'} size={74} />
          <h2>{privacy?.name}</h2>
          <p>{t('table.yourTurn')}</p>
          <Button variant="primary" full icon={<Check size={18} />} onClick={() => setPrivacy(null)}>
            {t('table.yourHand')}
          </Button>
        </div>
      </Modal>

      <div className="table-footer-meta">
        {typeof view.extra?.target === 'number' ? <span><Crown size={13} /> {t('table.target', { n: view.extra.target })}</span> : null}
        <span><WifiOff size={13} /> {t('common.local')}</span>
        <span>{t('table.matchTime')}: {formatDuration(match.durationMs)}</span>
        {shake.supported && !cardGame ? <span>{t('table.shakeHint')}</span> : null}
      </div>
    </div>
  );
}
