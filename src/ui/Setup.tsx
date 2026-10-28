import { useEffect, useMemo, useState } from 'react';
import type { CSSProperties } from 'react';
import { motion } from 'framer-motion';
import {
  ArrowLeft,
  ArrowRight,
  Bot,
  Check,
  ChevronRight,
  Clock,
  Globe2,
  Minus,
  Plus,
  Shield,
  Sparkles,
  UserRound,
  Users,
  BookOpen,
  Shuffle,
} from 'lucide-react';
import { GAME_BY_ID } from '../core/registry';
import type { Difficulty, GameId, GameOptions, Seat } from '../core/types';
import { BOT_AVATARS, BOT_NAMES, AVATARS } from '../core/constants';
import { randomSeed } from '../core/rng';
import { useAppStore } from '../store/appStore';
import { useSession, type SessionMode } from '../store/session';
import { toast } from '../store/toasts';
import { useT } from '../i18n/useT';
import { Avatar, Button, Chip, Modal } from './components';
import { navigate } from '../app/router';
import { initAudio } from '../audio/engine';
import { trackAction } from '../services/diagnostics';

type Mode = SessionMode;

function makeSeats(count: number, mode: Mode, profileName: string, profileAvatar: string, profileId: string, labels: { bot: string; player: string }): Seat[] {
  return Array.from({ length: count }, (_, i) => {
    if (i === 0) return { id: profileId, name: profileName, avatar: profileAvatar, isBot: false };
    if (mode === 'bots') {
      return {
        id: `bot-${i}`,
        name: BOT_NAMES[(i - 1) % BOT_NAMES.length] ?? labels.bot,
        avatar: BOT_AVATARS[(i - 1) % BOT_AVATARS.length] ?? 'silbon',
        isBot: true,
      };
    }
    return {
      id: `local-${i}`,
      name: labels.player.replace('{n}', String(i + 1)),
      avatar: AVATARS[i % AVATARS.length]?.id ?? 'jaguar',
      isBot: false,
    };
  });
}

export function SetupScreen({ gameId, initialMode }: { gameId: string; initialMode: string | null }) {
  const t = useT();
  const meta = GAME_BY_ID[gameId as GameId];
  const profile = useAppStore((s) => s.profile);
  const startMatch = useSession((s) => s.start);
  const [mode, setMode] = useState<Mode>(initialMode === 'hotseat' ? 'hotseat' : 'bots');
  const defaultCount = meta ? (meta.maxPlayers <= 2 ? 2 : Math.min(4, meta.maxPlayers)) : 2;
  const [count, setCount] = useState(defaultCount);
  const [seats, setSeats] = useState<Seat[]>(() =>
    meta
      ? makeSeats(defaultCount, initialMode === 'hotseat' ? 'hotseat' : 'bots', profile.name, profile.avatar, profile.id, {
          bot: t('setup.botNames'),
          player: t('setup.playerName'),
        })
      : [],
  );
  const [difficulty, setDifficulty] = useState<Difficulty>('normal');
  const [teams, setTeams] = useState(meta?.id === 'domino');
  const [target, setTarget] = useState(meta?.id === 'caida' ? 24 : 100);
  const [doubleTwelve, setDoubleTwelve] = useState(false);
  const [boardSize, setBoardSize] = useState(9);
  const [rulesOpen, setRulesOpen] = useState(false);

  const anyBot = seats.some((s) => s.isBot);
  const minPlayers = meta?.minPlayers ?? 2;
  const maxPlayers = meta?.maxPlayers ?? 4;
  const categoryLabel = meta?.category === 'cartas' ? t('catalog.cards') : meta?.category === 'tablero' ? t('catalog.board') : t('catalog.dice');
  const canTeams = Boolean(meta && (meta.id === 'caida' || meta.id === 'domino') && count === 4);
  const targetOptions = meta?.id === 'caida' ? [24, 30, 50] : meta?.id === 'domino' ? [50, 100, 150] : [];

  useEffect(() => {
    setSeats((previous) => {
      const base = makeSeats(count, mode, profile.name, profile.avatar, profile.id, {
        bot: t('setup.botNames'),
        player: t('setup.playerName'),
      });
      return base.map((seat, i) => {
        const old = previous[i];
        if (!old) return seat;
        return {
          ...seat,
          name: old.name || seat.name,
          avatar: old.avatar || seat.avatar,
          ...(i === 0 ? { name: profile.name, avatar: profile.avatar, isBot: false } : {}),
        };
      });
    });
  }, [count, mode, profile.name, profile.avatar, profile.id, t]);

  const options = useMemo<GameOptions>(() => ({
    ...(meta?.id === 'caida' || meta?.id === 'domino' ? { target } : {}),
    ...(canTeams ? { teams } : {}),
    ...(meta?.id === 'domino' ? { doubleTwelve } : {}),
    ...(meta?.id === 'go' ? { boardSize } : {}),
  }), [meta?.id, target, canTeams, teams, doubleTwelve, boardSize]);

  if (!meta) {
    return (
      <div className="screen">
        <div className="empty-state">
          <span className="big-emoji">🃏</span>
          <h1>{t('notfound.title')}</h1>
          <Button variant="primary" onClick={() => navigate('/')}>{t('common.home')}</Button>
        </div>
      </div>
    );
  }

  const updateSeat = (index: number, patch: Partial<Seat>) => {
    setSeats((current) => current.map((seat, i) => (i === index ? { ...seat, ...patch } : seat)));
  };

  const cycleAvatar = (index: number) => {
    const seat = seats[index];
    if (!seat) return;
    const current = AVATARS.findIndex((a) => a.id === seat.avatar);
    const next = AVATARS[(current + 1) % AVATARS.length];
    if (next) updateSeat(index, { avatar: next.id });
  };

  const toggleBot = (index: number) => {
    const seat = seats[index];
    if (!seat || index === 0) return;
    updateSeat(index, {
      isBot: !seat.isBot,
      ...(seat.isBot
        ? { name: t('setup.playerName', { n: index + 1 }), avatar: AVATARS[index % AVATARS.length]?.id ?? 'jaguar' }
        : { name: BOT_NAMES[(index - 1) % BOT_NAMES.length] ?? t('setup.botNames'), avatar: BOT_AVATARS[(index - 1) % BOT_AVATARS.length] ?? 'silbon' }),
    });
  };

  const addSeat = () => {
    if (count >= maxPlayers) return;
    setCount((c) => c + 1);
  };

  const removeSeat = () => {
    if (count <= minPlayers) return;
    setCount((c) => c - 1);
  };

  const start = () => {
    if (seats.length < minPlayers) {
      toast(t('setup.needPlayers', { n: minPlayers }), 'warn');
      return;
    }
    void initAudio();
    trackAction('setup:start', `${meta.id}:${mode}:${seats.length}`);
    startMatch({ gameId: meta.id, seats, options, difficulty, seed: randomSeed() }, mode);
    if (!useSession.getState().match) {
      toast(t('error.generic'), 'danger');
      return;
    }
    navigate('/game');
  };

  return (
    <div className="screen setup-screen">
      <div className="setup-top">
        <Button variant="ghost" size="sm" icon={<ArrowLeft size={17} />} onClick={() => navigate('/')}>{t('common.back')}</Button>
        <div className="setup-game-badge" style={{ '--setup-accent': meta.accent } as CSSProperties}>
          <span>{meta.icon}</span>
          <div>
            <strong>{t(meta.nameKey)}</strong>
            <span>{meta.minPlayers}–{meta.maxPlayers} {t('common.players')} · {meta.avgMinutes} {t('common.min')}</span>
          </div>
        </div>
        <Button variant="ghost" size="sm" icon={<BookOpen size={16} />} onClick={() => setRulesOpen(true)}>{t('setup.rules')}</Button>
      </div>

      <header className="setup-heading">
        <span className="eyebrow">{t('setup.game')}</span>
        <h1>{t('setup.title')}</h1>
        <p>{t('setup.subtitle')}</p>
      </header>

      <div className="setup-grid">
        <section className="setup-panel setup-players">
          <div className="setup-panel-head">
            <div>
              <span className="eyebrow">{t('setup.mode')}</span>
              <h2>{t('setup.players')}</h2>
            </div>
            <div className="count-control">
              <button type="button" onClick={removeSeat} disabled={count <= minPlayers} aria-label="Menos jugadores"><Minus size={15} /></button>
              <span>{count}</span>
              <button type="button" onClick={addSeat} disabled={count >= maxPlayers} aria-label="Más jugadores"><Plus size={15} /></button>
            </div>
          </div>

          <div className="mode-tabs">
            <button type="button" className={mode === 'bots' ? 'is-active' : ''} onClick={() => setMode('bots')}>
              <Bot size={18} />
              <span>{t('modes.bots')}</span>
              <small>{t('modes.botsDesc')}</small>
            </button>
            <button type="button" className={mode === 'hotseat' ? 'is-active' : ''} onClick={() => setMode('hotseat')}>
              <Users size={18} />
              <span>{t('modes.hotseat')}</span>
              <small>{t('modes.hotseatDesc')}</small>
            </button>
          </div>
          <p className="setup-hint"><Shield size={14} /> {mode === 'hotseat' ? t('setup.hotseatHint') : t('setup.localOnly')}</p>

          <div className="seat-list">
            {seats.map((seat, index) => (
              <motion.div key={seat.id} className="seat-row" layout initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}>
                <button type="button" className="seat-avatar" onClick={() => cycleAvatar(index)} aria-label={t('setup.avatar')}>
                  <Avatar id={seat.avatar} size={44} ring={index === 0} />
                  <span className="seat-avatar-edit"><Shuffle size={10} /></span>
                </button>
                <div className="seat-main">
                  <input
                    type="text"
                    value={seat.name}
                    maxLength={16}
                    disabled={index === 0}
                    onChange={(event) => updateSeat(index, { name: event.target.value })}
                    aria-label={t('setup.yourName')}
                  />
                  <span className="seat-tag">
                    {index === 0 ? t('setup.you') : seat.isBot ? t('lobby.botBadge') : t('setup.seat', { n: index + 1 })}
                    {seat.isBot ? <Bot size={12} /> : <UserRound size={12} />}
                  </span>
                </div>
                {index > 0 && mode === 'hotseat' ? (
                  <button type="button" className={`seat-toggle ${seat.isBot ? 'is-bot' : ''}`} onClick={() => toggleBot(index)}>
                    {seat.isBot ? <Bot size={15} /> : <UserRound size={15} />}
                    <span>{seat.isBot ? t('lobby.botBadge') : t('common.player')}</span>
                  </button>
                ) : null}
                {index === 0 ? <Chip tone="gold">{t('common.you')}</Chip> : null}
              </motion.div>
            ))}
          </div>
          <div className="seat-add">
            <Button variant="ghost" size="sm" icon={<Plus size={15} />} onClick={addSeat} disabled={count >= maxPlayers}>{mode === 'bots' ? t('setup.addBot') : t('setup.addHuman')}</Button>
            <span>{count}/{maxPlayers}</span>
          </div>
        </section>

        <div className="setup-side">
          {anyBot ? (
            <section className="setup-panel">
              <div className="setup-panel-head">
                <div>
                  <span className="eyebrow">{t('setup.difficulty')}</span>
                  <h2>{t('setup.bots')}</h2>
                </div>
                <Bot size={20} />
              </div>
              <div className="difficulty-list">
                {([
                  ['easy', 'setup.easy', 'setup.easyDesc', '🌱'],
                  ['normal', 'setup.normal', 'setup.normalDesc', '⚖️'],
                  ['hard', 'setup.hard', 'setup.hardDesc', '🔥'],
                ] as const).map(([value, label, desc, emoji]) => (
                  <button key={value} type="button" className={`difficulty-option ${difficulty === value ? 'is-active' : ''}`} onClick={() => setDifficulty(value)}>
                    <span className="difficulty-emoji">{emoji}</span>
                    <span>
                      <strong>{t(label)}</strong>
                      <small>{t(desc)}</small>
                    </span>
                    {difficulty === value ? <Check size={16} /> : null}
                  </button>
                ))}
              </div>
            </section>
          ) : null}

          <section className="setup-panel">
            <div className="setup-panel-head">
              <div>
                <span className="eyebrow">{t('setup.options')}</span>
                <h2>{t('setup.houseRules')}</h2>
              </div>
              <Sparkles size={19} />
            </div>
            {canTeams ? (
              <label className="check-row">
                <input type="checkbox" checked={teams} onChange={(event) => setTeams(event.target.checked)} />
                <span>{t('setup.teams')}</span>
              </label>
            ) : null}
            {targetOptions.length > 0 ? (
              <div className="option-row">
                <span>{t('setup.target')}</span>
                <div className="segmented">
                  {targetOptions.map((value) => (
                    <button key={value} type="button" className={target === value ? 'is-active' : ''} onClick={() => setTarget(value)}>{value}</button>
                  ))}
                </div>
              </div>
            ) : null}
            {meta.id === 'domino' ? (
              <label className="check-row">
                <input type="checkbox" checked={doubleTwelve} onChange={(event) => setDoubleTwelve(event.target.checked)} />
                <span>{t('setup.doubleTwelve')}</span>
              </label>
            ) : null}
            {meta.id === 'go' ? (
              <div className="option-row">
                <span>{t('setup.boardSize')}</span>
                <div className="segmented">
                  {[9, 13].map((value) => (
                    <button key={value} type="button" className={boardSize === value ? 'is-active' : ''} onClick={() => setBoardSize(value)}>
                      {value}×{value}
                    </button>
                  ))}
                </div>
              </div>
            ) : null}
            <div className="setup-info-line">
              <Clock size={14} /> {meta.avgMinutes} {t('common.min')} · {t(`tag.${meta.tagKeys[0]?.replace('tag.', '') ?? 'clasico'}`)}
            </div>
          </section>

          <section className="setup-room-cta">
            <Globe2 size={22} />
            <div>
              <strong>{t('sala.onlineTitle')}</strong>
              <p>{t('sala.onlineHint')}</p>
            </div>
            <Button variant="ghost" size="sm" trailing={<ChevronRight size={15} />} onClick={() => navigate('/room')}>{t('sala.create')}</Button>
          </section>
        </div>
      </div>

      <div className="setup-start-bar">
        <div className="setup-start-info">
          <Avatar id={profile.avatar} size={36} />
          <span>{t('setup.gameInfo', { players: seats.length, time: meta.avgMinutes, category: categoryLabel })}</span>
        </div>
        <Button variant="primary" size="lg" trailing={<ArrowRight size={18} />} onClick={start} disabled={seats.length < minPlayers}>
          {t('setup.start')}
        </Button>
      </div>

      <Modal open={rulesOpen} onClose={() => setRulesOpen(false)} title={t(meta.nameKey)} subtitle={t(meta.descKey)} size="lg" variant="bottom">
        <div className="rules-content">
          <div className="rules-meta">
            <Chip tone="gold">{meta.minPlayers}–{meta.maxPlayers} {t('common.players')}</Chip>
            <Chip tone="blue">{meta.avgMinutes} {t('common.min')}</Chip>
            {meta.tagKeys.slice(0, 2).map((tag) => <Chip key={tag} tone="green">{t(tag)}</Chip>)}
          </div>
          <p>{t(meta.rulesKey)}</p>
          <Button variant="primary" onClick={() => setRulesOpen(false)}>{t('rules.close')}</Button>
        </div>
      </Modal>
    </div>
  );
}
