import { useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import {
  ArrowRight,
  BookOpen,
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
  Users,
} from 'lucide-react';
import type { GameAction, XpAwardResult } from '../core/types';
import { GAME_BY_ID } from '../core/registry';
import { formatLog } from '../i18n';
import { useT } from '../i18n/useT';
import { useRoom } from '../store/room';
import { useAppStore } from '../store/appStore';
import { useFestivalTheme } from '../theme/useTheme';
import { toast } from '../store/toasts';
import { Button, Chip, IconButton, Modal, Avatar } from './components';
import { GameSurface } from './GameSurface';
import { ResultScreen } from './Result';
import { navigate } from '../app/router';
import { sfx } from '../audio/engine';
import { haptic } from '../services/haptics';
import { trackAction } from '../services/diagnostics';

const ICONS: Record<string, ReactNode> = {
  sparkles: <Sparkles size={16} />,
  megaphone: <Megaphone size={16} />,
  layers: <Layers size={16} />,
  'arrow-right': <ArrowRight size={16} />,
  dices: <Dices size={16} />,
  hand: <Hand size={16} />,
  flag: <Flag size={16} />,
};

export function RemoteTable() {
  const t = useT();
  const roomState = useRoom();
  const view = roomState.view;
  const room = roomState.room;
  const role = roomState.role;
  const roundId = roomState.roundId;
  const settings = useAppStore((s) => s.settings);
  const setSettings = useAppStore((s) => s.setSettings);
  const recordMatch = useAppStore((s) => s.recordMatch);
  const { festival } = useFestivalTheme();
  const [busy, setBusy] = useState(false);
  const [logOpen, setLogOpen] = useState(false);
  const [leaveOpen, setLeaveOpen] = useState(false);
  const [award, setAward] = useState<XpAwardResult | null>(null);
  const awarded = useRef<string | null>(null);
  const lastLog = useRef(0);

  const meta = view ? GAME_BY_ID[view.gameId] : null;
  const gameName = meta ? t(meta.nameKey) : 'Mesa Viva';
  const lastEvents = useMemo(() => (view?.log ?? []).slice(-5).reverse(), [view?.log, view?.log?.length]);

  useEffect(() => {
    if (roomState.error) {
      toast(t(roomState.error), 'danger');
      roomState.clearError();
    }
  }, [roomState.error, roomState, t]);

  useEffect(() => {
    if (!view || !view.result || !room || !roundId) return;
    if (view.youId === 'spectator') return;
    if (awarded.current === roundId) return;
    awarded.current = roundId;
    const won = view.result.winnerIds.includes(view.youId);
    const draw = Boolean(view.result.draw);
    const points = view.result.scores[view.youId] ?? 0;
    const result = recordMatch({
      gameId: view.gameId,
      won,
      draw,
      points,
      difficulty: 'normal',
      humanPlayers: room.players.length,
      durationMs: 0,
      players: room.players.map((p) => p.name),
    });
    setAward(result);
    trackAction('room:finish', `${room.code}:${won ? 'win' : draw ? 'draw' : 'loss'}`);
  }, [view, room, roundId, recordMatch]);

  useEffect(() => {
    if (!view?.log) return;
    const fresh = view.log.slice(lastLog.current);
    lastLog.current = view.log.length;
    if (fresh.length === 0) return;
    const last = fresh[fresh.length - 1];
    if (!last) return;
    if (last.key === 'log.played' || last.key === 'log.dominoPlay' || last.key === 'log.chessMove' || last.key === 'log.checkerMove') {
      sfx('card');
      haptic('card', settings.haptics);
    } else if (last.key === 'log.won' || last.key === 'log.matchEnd' || last.key === 'log.cantoWin') {
      sfx('win');
      haptic('win', settings.haptics);
    } else if (last.key === 'log.canto') {
      sfx('canto');
      haptic('canto', settings.haptics);
    } else if (last.key === 'log.roll') {
      sfx('dice');
      haptic('dice', settings.haptics);
    }
  }, [view?.log?.length, settings.haptics]);

  const act = (action: GameAction) => {
    if (busy) return;
    setBusy(true);
    roomState.act(action);
    window.setTimeout(() => setBusy(false), 240);
  };

  const leave = () => {
    roomState.leave();
    setLeaveOpen(false);
    navigate('/room');
  };

  const backToRoom = () => {
    roomState.backToLobby();
    setAward(null);
  };

  if (!view || !room || !meta) {
    return (
      <div className="table-screen remote-loading">
        <div className="missing-card">
          <span className="big-emoji">🃏</span>
          <h1>{t('sala.connecting')}</h1>
          <p>{t('sala.waitingHost')}</p>
          <Button variant="danger" onClick={leave}>{t('sala.leaveRoom')}</Button>
        </div>
      </div>
    );
  }

  if (view.result) {
    return (
      <div className="table-screen remote-result">
        <ResultScreen
          view={view}
          award={award}
          gameName={gameName}
          festival={festival}
          youId={view.youId}
          onRematch={backToRoom}
          onHome={leave}
          rematchLabel={t('sala.backToRoom')}
          homeLabel={t('sala.leaveRoom')}
        />
      </div>
    );
  }

  return (
    <div className="table-screen remote-table">
      <header className="table-topbar">
        <div className="table-top-left">
          <IconButton label={t('sala.leaveRoom')} onClick={() => setLeaveOpen(true)}>
            <ChevronRight size={20} className="flip-x" />
          </IconButton>
          <div className="table-title">
            <span className="eyebrow">{role === 'host' ? t('sala.youAreHost') : t('sala.youAreGuest')} · {room.code}</span>
            <h1>{gameName}</h1>
            <span className="table-round">{t('sala.roundOf', { n: room.round, total: room.playlist.length })}</span>
          </div>
        </div>
        <div className="table-top-actions">
          <IconButton label={settings.music ? t('table.soundOn') : t('table.soundOff')} onClick={() => setSettings({ music: !settings.music, sfx: !settings.sfx })}>
            {settings.music ? <Volume2 size={19} /> : <VolumeX size={19} />}
          </IconButton>
          <IconButton label={t('table.fullLog')} onClick={() => setLogOpen(true)}><ListOrdered size={19} /></IconButton>
          <IconButton label={t('sala.leaveRoom')} onClick={() => setLeaveOpen(true)}><Pause size={19} /></IconButton>
        </div>
      </header>

      <div className="opponents-strip">
        {view.players.map((player) => (
          <div key={player.id} className={`opponent ${player.isYou ? 'is-you' : ''} ${player.isTurn ? 'is-turn' : ''} ${!player.active ? 'is-out' : ''}`}>
            <div className="opponent-avatar-wrap">
              <Avatar id={player.avatar} size={38} ring={player.isTurn} />
              {player.isYou ? <span className="opponent-you">{t('common.you')}</span> : null}
            </div>
            <div className="opponent-info">
              <span className="opponent-name">{player.name}</span>
              <span className="opponent-meta">
                {player.cardsLeft !== undefined ? `${player.cardsLeft} 🃏` : null}
                {player.diceLeft !== undefined ? `${player.diceLeft} 🎲` : null}
                <strong>{player.score}</strong>
              </span>
            </div>
            {player.isTurn ? <span className="turn-pulse" /> : null}
          </div>
        ))}
      </div>

      <div className="table-prompt-row">
        <div className={`prompt ${view.prompt?.tone ?? 'info'}`}>
          {view.youId === 'spectator'
            ? t('sala.spectator')
            : view.prompt
              ? t(view.prompt.key, view.prompt.params)
              : t('table.waiting')}
        </div>
        <Chip tone={role === 'host' ? 'gold' : 'purple'}>{role === 'host' ? t('sala.host') : t('sala.guest')}</Chip>
      </div>

      <div className="table-surface-wrap">
        <GameSurface view={view} act={act} busy={busy} />
      </div>

      {view.youId !== 'spectator' && view.actions.length > 0 ? (
        <div className="action-bar">
          {view.actions.map((action) => (
            <Button
              key={action.id}
              variant={action.tone === 'primary' ? 'primary' : action.tone === 'danger' ? 'danger' : action.tone === 'ghost' ? 'ghost' : 'secondary'}
              icon={action.icon ? ICONS[action.icon] : undefined}
              disabled={action.disabled || busy}
              onClick={() => act(action.action)}
            >
              {t(action.labelKey, action.labelParams)}
            </Button>
          ))}
        </div>
      ) : null}

      <section className="table-log-ticker">
        <div className="log-ticker-head">
          <span><ListOrdered size={15} /> {t('table.lastMoves')}</span>
          <button type="button" onClick={() => setLogOpen(true)}>{t('table.fullLog')} <ChevronRight size={14} /></button>
        </div>
        <div className="log-ticker-list">
          {lastEvents.map((event) => (
            <div key={event.id} className={`log-ticker-line tone-${event.tone ?? 'info'}`}>
              <span className="log-dot" />
              <span>{formatLog(event, t)}</span>
            </div>
          ))}
        </div>
      </section>

      <div className="table-footer-meta">
        <span><Users size={13} /> {t('sala.playersCount', { n: room.players.length, max: 6 })}</span>
        <span>{t('sala.chat')}: {room.chat.length}</span>
        <span>{role === 'host' ? t('sala.youAreHost') : t('sala.youAreGuest')}</span>
      </div>

      <Modal open={logOpen} onClose={() => setLogOpen(false)} title={t('table.fullLog')} subtitle={t('table.fullLogHint')} size="lg" variant="bottom">
        <div className="full-log">
          {(view.log ?? []).map((event, i) => (
            <div key={event.id} className={`log-line tone-${event.tone ?? 'info'}`}>
              <span className="log-index">{i + 1}</span>
              <span className="log-text">{formatLog(event, t)}</span>
            </div>
          ))}
        </div>
        <div className="modal-inline-actions">
          <Button variant="secondary" icon={<Copy size={16} />} onClick={async () => {
            try {
              await navigator.clipboard.writeText((view.log ?? []).map((e) => formatLog(e, t)).join('\n'));
              toast(t('notify.logCopied'), 'success');
            } catch {
              toast(t('notify.error'), 'danger');
            }
          }}>{t('table.copyLog')}</Button>
        </div>
      </Modal>

      <Modal open={leaveOpen} onClose={() => setLeaveOpen(false)} title={t('sala.leaveRoom')} subtitle={t('sala.leaveConfirm')} size="sm">
        <div className="modal-inline-actions">
          <Button variant="ghost" onClick={() => setLeaveOpen(false)}>{t('common.cancel')}</Button>
          <Button variant="danger" onClick={leave}>{t('sala.leave')}</Button>
        </div>
      </Modal>

      <div className="room-table-hint">
        <BookOpen size={14} /> {role === 'host' ? t('sala.hostHint') : t('sala.guestHint')}
      </div>
    </div>
  );
}
