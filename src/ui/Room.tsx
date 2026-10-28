import { useEffect, useMemo, useState } from 'react';
import {
  ArrowRight,
  Check,
  Copy,
  Crown,
  Gamepad2,
  Link2,
  LogIn,
  MessageCircle,
  Plus,
  QrCode,
  Send,
  Share2,
  Sparkles,
  Trash2,
  Users,
  Wifi,
  WifiOff,
  X,
} from 'lucide-react';
import { GAMES, GAME_BY_ID } from '../core/registry';
import type { GameId } from '../core/types';
import { normalizeCode } from '../net/room';
import { useRoom } from '../store/room';
import { useAppStore } from '../store/appStore';
import { useT } from '../i18n/useT';
import { toast } from '../store/toasts';
import { copyText, shareText } from '../services/pwa';
import { trackAction } from '../services/diagnostics';
import { AVATARS } from '../core/constants';
import { Avatar, Button, Chip, IconButton, Modal, PageHeader } from './components';
import { QRCodeView, QRScanner } from './QR';
import { RemoteTable } from './RemoteTable';
import { useRoute, navigate } from '../app/router';

export function RoomScreen() {
  const t = useT();
  const room = useRoom();
  const profile = useAppStore((s) => s.profile);
  const route = useRoute();
  const [tab, setTab] = useState<'online' | 'offline'>('online');
  const [name, setName] = useState(profile.name);
  const [avatar, setAvatar] = useState(profile.avatar);
  const [code, setCode] = useState('');
  const [scanOpen, setScanOpen] = useState(false);
  const [qrOpen, setQrOpen] = useState(false);
  const [chatText, setChatText] = useState('');
  const [selectedGame, setSelectedGame] = useState<GameId | ''>('');
  const [joining, setJoining] = useState(false);
  const [lanHost, setLanHost] = useState(() => typeof localStorage !== 'undefined' && localStorage.getItem('mesa-viva-lan-host') === '1');
  const nativeLan = typeof window !== 'undefined' && typeof (window as unknown as { MesaViva?: { startLanServer?: unknown } }).MesaViva?.startLanServer === 'function';

  useEffect(() => {
    const relay = route.params.get('relay');
    if (relay && /^wss?:\/\//i.test(relay)) {
      localStorage.setItem('mesa-viva-room-server', relay);
      if (nativeLan) setTab('offline');
    }
    const fromUrl = route.params.get('code');
    if (fromUrl) setCode(normalizeCode(fromUrl));
    if (route.params.get('scan') === '1') setScanOpen(true);
  }, [route.params, nativeLan]);

  useEffect(() => {
    if (room.error) {
      toast(t(room.error), 'danger');
      room.clearError();
    }
  }, [room.error, room, t]);

  const inviteUrl = useMemo(() => {
    if (!room.room) return '';
    const params = new URLSearchParams({ code: room.room.code });
    const relay = (typeof localStorage !== 'undefined'
      ? localStorage.getItem('mesa-viva-lan-url') ?? localStorage.getItem('mesa-viva-room-server')
      : '')?.trim();
    if (relay) params.set('relay', relay);
    return `${window.location.origin}${window.location.pathname}#/room?${params.toString()}`;
  }, [room.room]);

  if (room.view && (room.phase === 'playing' || room.phase === 'result')) {
    return <RemoteTable />;
  }

  const create = () => {
    if (name.trim().length < 2) {
      toast(t('error.nameRequired'), 'warn');
      return;
    }
    trackAction('room:create', name);
    room.create(name.trim(), avatar);
  };

  const createLanRoom = () => {
    if (name.trim().length < 2) {
      toast(t('error.nameRequired'), 'warn');
      return;
    }
    const bridge = (window as unknown as { MesaViva?: { startLanServer?: () => string } }).MesaViva;
    const lanUrl = bridge?.startLanServer?.();
    if (!lanUrl) {
      toast(t('sala.lanError'), 'danger');
      return;
    }
    try {
      const parsed = new URL(lanUrl);
      const localUrl = `ws://127.0.0.1:${parsed.port || '9001'}`;
      localStorage.setItem('mesa-viva-lan-url', lanUrl);
      localStorage.setItem('mesa-viva-room-server', localUrl);
      localStorage.setItem('mesa-viva-lan-host', '1');
      setLanHost(true);
      trackAction('room:lanHost', lanUrl);
      room.create(name.trim(), avatar);
    } catch {
      toast(t('sala.lanError'), 'danger');
    }
  };

  const join = () => {
    const clean = normalizeCode(code);
    if (clean.length < 4) {
      toast(t('sala.invalid'), 'warn');
      return;
    }
    if (name.trim().length < 2) {
      toast(t('error.nameRequired'), 'warn');
      return;
    }
    setJoining(true);
    trackAction('room:join', clean);
    room.join(clean, name.trim(), avatar);
    window.setTimeout(() => setJoining(false), 1200);
  };

  const copyCode = async () => {
    if (!room.room) return;
    const ok = await copyText(inviteUrl);
    toast(ok ? t('sala.codeCopied') : t('notify.error'), ok ? 'success' : 'danger');
  };

  const share = async () => {
    if (!room.room) return;
    const ok = await shareText('Mesa Viva', t('lobby.shareText', { code: room.room.code }), inviteUrl);
    toast(ok ? t('notify.copied') : t('notify.error'), ok ? 'success' : 'danger');
  };

  const addSelectedGame = () => {
    if (!selectedGame) return;
    room.addGame(selectedGame);
    setSelectedGame('');
    toast(t('sala.roundAdded'), 'success');
  };

  const sendChat = (text: string) => {
    const clean = text.trim();
    if (!clean) return;
    room.sendChat(clean);
    setChatText('');
  };

  if (!room.room) {
    return (
      <div className="screen room-screen room-lobby screen-enter">
        <PageHeader
          title={t('sala.title')}
          subtitle={t('sala.subtitle')}
          onBack={() => navigate('/')}
          actions={<Chip tone={tab === 'online' ? 'green' : 'gold'}>{tab === 'online' ? t('sala.modeOnline') : t('sala.modeOffline')}</Chip>}
        />

        <div className="room-hero">
          <div className="room-hero-art" aria-hidden="true"><span>▦</span><span>🃏</span><span>♠</span></div>
          <div>
            <span className="eyebrow">{t('sala.connection')}</span>
            <h2>{tab === 'online' ? t('sala.onlineTitle') : t('sala.offlineTitle')}</h2>
            <p>{tab === 'online' ? t('sala.onlineHint') : t('sala.offlineHint')}</p>
          </div>
        </div>

        <div className="segmented room-tabs">
          <button type="button" className={tab === 'online' ? 'is-active' : ''} onClick={() => setTab('online')}><Wifi size={16} /> {t('sala.modeOnline')}</button>
          <button type="button" className={tab === 'offline' ? 'is-active' : ''} onClick={() => setTab('offline')}><WifiOff size={16} /> {t('sala.modeOffline')}</button>
        </div>

        {tab === 'online' ? (
          <div className="room-entry-grid">
            <section className="room-entry-card">
              <div className="room-entry-icon"><Sparkles size={22} /></div>
              <h3>{t('sala.createTitle')}</h3>
              <p>{t('sala.hostHint')}</p>
              <label className="field">
                <span>{t('sala.name')}</span>
                <input value={name} onChange={(event) => setName(event.target.value)} maxLength={16} placeholder={t('onboarding.namePlaceholder')} />
              </label>
              <div className="field-label">{t('onboarding.avatar')}</div>
              <div className="avatar-row">
                {AVATARS.slice(0, 7).map((item) => (
                  <button key={item.id} type="button" className={`avatar-choice mini ${avatar === item.id ? 'is-active' : ''}`} onClick={() => setAvatar(item.id)} aria-label={item.label}>
                    <span>{item.emoji}</span>
                  </button>
                ))}
              </div>
              <Button variant="primary" full icon={room.connecting ? undefined : <Plus size={17} />} loading={room.connecting} onClick={create}>{t('sala.createBtn')}</Button>
            </section>
            <section className="room-entry-card room-join-card">
              <div className="room-entry-icon"><LogIn size={22} /></div>
              <h3>{t('sala.joinTitle')}</h3>
              <p>{t('sala.guestHint')}</p>
              <label className="field">
                <span>{t('sala.code')}</span>
                <div className="inline-field">
                  <input
                    value={code}
                    onChange={(event) => setCode(normalizeCode(event.target.value))}
                    maxLength={6}
                    placeholder={t('lobby.codePlaceholder')}
                    className="code-input"
                    autoCapitalize="characters"
                  />
                  <Button variant="secondary" size="sm" icon={<QrCode size={16} />} onClick={() => setScanOpen(true)}>{t('sala.scan')}</Button>
                </div>
              </label>
              <label className="field">
                <span>{t('sala.name')}</span>
                <input value={name} onChange={(event) => setName(event.target.value)} maxLength={16} placeholder={t('onboarding.namePlaceholder')} />
              </label>
              <Button variant="gold" full icon={joining ? undefined : <ArrowRight size={17} />} loading={joining || room.connecting} onClick={join}>{t('sala.joinBtn')}</Button>
              {room.connecting ? <p className="room-connecting">{t('sala.connecting')}</p> : null}
            </section>
          </div>
        ) : nativeLan ? (
          <div className="room-entry-grid">
            <section className="room-entry-card">
              <div className="room-entry-icon"><WifiOff size={22} /></div>
              <h3>{t('sala.lanHostTitle')}</h3>
              <p>{t('sala.lanHostDesc')}</p>
              <label className="field">
                <span>{t('sala.name')}</span>
                <input value={name} onChange={(event) => setName(event.target.value)} maxLength={16} placeholder={t('onboarding.namePlaceholder')} />
              </label>
              <div className="field-label">{t('onboarding.avatar')}</div>
              <div className="avatar-row">
                {AVATARS.slice(0, 7).map((item) => (
                  <button key={item.id} type="button" className={`avatar-choice mini ${avatar === item.id ? 'is-active' : ''}`} onClick={() => setAvatar(item.id)} aria-label={item.label}>
                    <span>{item.emoji}</span>
                  </button>
                ))}
              </div>
              <Button variant="primary" full icon={<Wifi size={17} />} loading={room.connecting} onClick={createLanRoom}>{t('sala.lanCreate')}</Button>
              <p className="room-hint"><WifiOff size={13} /> {t('sala.lanHint')}</p>
            </section>
            <section className="room-entry-card room-join-card">
              <div className="room-entry-icon"><QrCode size={22} /></div>
              <h3>{t('sala.lanJoinTitle')}</h3>
              <p>{t('sala.lanJoinDesc')}</p>
              <Button variant="secondary" full icon={<QrCode size={17} />} onClick={() => setScanOpen(true)}>{t('sala.lanScan')}</Button>
              <label className="field">
                <span>{t('sala.code')}</span>
                <input value={code} onChange={(event) => setCode(normalizeCode(event.target.value))} maxLength={6} placeholder={t('lobby.codePlaceholder')} className="code-input" autoCapitalize="characters" />
              </label>
              <label className="field">
                <span>{t('sala.name')}</span>
                <input value={name} onChange={(event) => setName(event.target.value)} maxLength={16} placeholder={t('onboarding.namePlaceholder')} />
              </label>
              <Button variant="gold" full icon={joining ? undefined : <ArrowRight size={17} />} loading={joining || room.connecting} onClick={join}>{t('sala.joinBtn')}</Button>
              <p className="room-hint"><Users size={13} /> {t('sala.lanGuestHint')}</p>
            </section>
          </div>
        ) : (
          <div className="room-offline-card">
            <div className="room-entry-icon"><WifiOff size={22} /></div>
            <div>
              <h3>{t('sala.offlineTitle')}</h3>
              <p>{t('sala.offlineHint')}</p>
              <div className="room-offline-actions">
                <Button variant="primary" icon={<Gamepad2 size={17} />} onClick={() => navigate('/catalog')}>{t('modes.hotseat')}</Button>
                <Button variant="ghost" icon={<Users size={17} />} onClick={() => navigate('/setup/caida?mode=hotseat')}>{t('modes.friends')}</Button>
              </div>
              <p className="room-offline-note">{t('sala.offlineLocalDesc')}</p>
            </div>
          </div>
        )}

        <div className="room-feature-row">
          <span><ShieldIcon /> {t('sala.step1')}</span>
          <span><QrCode size={15} /> {t('sala.step2')}</span>
          <span><Check size={15} /> {t('sala.step3')}</span>
        </div>

        <Modal open={scanOpen} onClose={() => setScanOpen(false)} title={t('sala.scanTitle')} subtitle={t('sala.scanGuest')} size="md" variant="bottom">
          <QRScanner
            onResult={(value) => {
              const codeMatch = value.match(/[?&]code=([A-Z0-9]{4,6})/i) ?? value.match(/\b([A-Z0-9]{6})\b/);
              const relayMatch = value.match(/[?&]relay=([^&]+)/i);
              const relay = relayMatch?.[1] ? decodeURIComponent(relayMatch[1]) : null;
              if (relay && /^wss?:\/\//i.test(relay)) {
                localStorage.setItem('mesa-viva-room-server', relay);
                if (nativeLan) setTab('offline');
              }
              const found = codeMatch?.[1];
              if (found) {
                setCode(normalizeCode(found));
                setScanOpen(false);
                toast(t('sala.codeCopied'), 'success');
              } else {
                toast(t('sala.invalid'), 'warn');
              }
            }}
            onClose={() => setScanOpen(false)}
          />
          <p className="room-scan-hint">{t('sala.scanHint')}</p>
        </Modal>
      </div>
    );
  }

  const roomGameIds = room.room.playlist;
  const updatable = GAMES.filter((g) => !roomGameIds.includes(g.id));
  const isHost = room.role === 'host';
  const scoreEntries = Object.entries(room.room.scores)
    .map(([id, score]) => ({ id, score, name: room.room?.players.find((p) => p.id === id)?.name ?? '—' }))
    .sort((a, b) => b.score - a.score);

  return (
    <div className="screen room-screen room-lobby">
      <PageHeader
        title={t('sala.title')}
        subtitle={`${t('sala.room')} ${room.room.code} · ${t('sala.playersIn', { n: room.room.players.length })}`}
        onBack={() => setTab('online')}
        actions={
        <>
          {lanHost ? <Chip tone="gold"><WifiOff size={12} /> {t('sala.lanBadge')}</Chip> : null}
          <Chip tone={isHost ? 'gold' : 'purple'}>{isHost ? t('sala.host') : t('sala.guest')}</Chip>
        </>
      }
      />

      <section className="room-code-card">
        <div className="room-code-main">
          <span className="eyebrow">{t('sala.code')}</span>
          <strong>{room.room.code}</strong>
          <span className="room-code-hint">{t('sala.codeHint')}</span>
        </div>
        <div className="room-code-actions">
          <Button variant="secondary" size="sm" icon={<QrCode size={16} />} onClick={() => setQrOpen(true)}>{t('sala.qr')}</Button>
          <Button variant="ghost" size="sm" icon={<Copy size={16} />} onClick={() => void copyCode()}>{t('sala.copy')}</Button>
          <Button variant="ghost" size="sm" icon={<Share2 size={16} />} onClick={() => void share()}>{t('common.share')}</Button>
          <Button variant="danger" size="sm" icon={<X size={16} />} onClick={() => { room.leave(); navigate('/room'); }}>{t('sala.leave')}</Button>
        </div>
      </section>

      <div className="room-lobby-grid">
        <div className="room-lobby-main">
          <section className="room-panel">
            <div className="room-panel-head">
              <div><span className="eyebrow">{t('sala.players')}</span><h2>{t('sala.playersCount', { n: room.room.players.length, max: 6 })}</h2></div>
              <Chip tone="green"><Wifi size={12} /> {t('sala.connected')}</Chip>
            </div>
            <div className="room-player-list">
              {room.room.players.map((player) => (
                <div key={player.id} className={`room-player ${player.id === room.youId ? 'is-you' : ''}`}>
                  <Avatar id={player.avatar} size={40} ring={player.isHost} />
                  <div className="room-player-info">
                    <strong>{player.name}</strong>
                    <span>
                      {player.isHost ? <Chip tone="gold"><Crown size={10} /> {t('sala.host')}</Chip> : <span>{t('sala.guest')}</span>}
                      {player.id === room.youId ? <Chip tone="green">{t('sala.you')}</Chip> : null}
                    </span>
                  </div>
                  <span className={`room-player-status ${player.connected ? 'is-online' : 'is-offline'}`} title={player.connected ? t('sala.connected') : t('sala.disconnected')} />
                </div>
              ))}
              {room.room.players.length < 2 ? (
                <div className="room-waiting">
                  <Users size={17} />
                  <span>{t('sala.waiting')}</span>
                </div>
              ) : null}
            </div>
          </section>

          <section className="room-panel">
            <div className="room-panel-head">
              <div><span className="eyebrow">{t('sala.rounds')}</span><h2>{t('sala.playlist')}</h2></div>
              <Chip tone="default">{roomGameIds.length}/8</Chip>
            </div>
            <div className="room-playlist">
              {roomGameIds.length === 0 ? (
                <div className="room-empty"><Sparkles size={18} /><p>{t('sala.empty')}</p></div>
              ) : roomGameIds.map((gameId, index) => {
                const game = GAME_BY_ID[gameId];
                if (!game) return null;
                return (
                  <div key={`${gameId}-${index}`} className="room-play-row">
                    <span className="room-play-index">{index + 1}</span>
                    <span className="room-play-icon">{game.icon}</span>
                    <div className="room-play-info">
                      <strong>{t(game.nameKey)}</strong>
                      <span>{game.minPlayers}–{game.maxPlayers} {t('common.players')} · {game.avgMinutes} {t('common.min')}</span>
                    </div>
                    {isHost ? (
                      <>
                        <Button variant="primary" size="sm" icon={<ArrowRight size={14} />} onClick={() => { room.start(gameId); toast(t('sala.starting'), 'success'); }}>{t('sala.startRound')}</Button>
                        <IconButton label={t('sala.removeRound')} onClick={() => room.removeGame(gameId)}><Trash2 size={15} /></IconButton>
                      </>
                    ) : (
                      <Chip tone="default">{t('sala.pending')}</Chip>
                    )}
                  </div>
                );
              })}
            </div>
            <div className="room-add-row">
              <select value={selectedGame} onChange={(event) => setSelectedGame(event.target.value as GameId | '')} aria-label={t('sala.selectGame')}>
                <option value="">{t('sala.selectGame')}</option>
                {updatable.map((game) => <option key={game.id} value={game.id}>{game.icon} {t(game.nameKey)}</option>)}
              </select>
              <Button variant="secondary" size="sm" icon={<Plus size={16} />} onClick={addSelectedGame} disabled={!selectedGame}>{t('sala.addRound')}</Button>
            </div>
            {isHost ? <p className="room-hint"><Crown size={13} /> {t('sala.hostHint')} · {t('sala.startHint')}</p> : <p className="room-hint"><Users size={13} /> {t('sala.waitingHost')}</p>}
          </section>

          {room.room.round > 0 ? (
            <section className="room-panel room-scores-panel">
              <div className="room-panel-head"><div><span className="eyebrow">{t('sala.score')}</span><h2>{t('sala.scores')}</h2></div></div>
              <div className="room-score-list">
                {scoreEntries.map((entry, index) => (
                  <div key={entry.id} className="room-score-row">
                    <span>{index + 1}</span>
                    <strong>{entry.name}</strong>
                    <span>{entry.score}</span>
                  </div>
                ))}
              </div>
            </section>
          ) : null}
        </div>

        <aside className="room-sidebar">
          <section className="room-panel chat-panel">
            <div className="room-panel-head">
              <div><span className="eyebrow">{t('sala.chat')}</span><h2><MessageCircle size={17} /> {t('sala.chat')}</h2></div>
              <Chip tone="default">{room.room.chat.length}</Chip>
            </div>
            <div className="chat-list">
              {room.room.chat.length === 0 ? <p className="chat-empty">{t('sala.chatEmpty')}</p> : room.room.chat.map((message) => (
                <div key={message.id} className={`chat-message ${message.from === room.youId ? 'is-you' : ''}`}>
                  <span className="chat-name">{message.name}</span>
                  <span className="chat-text">{message.text}</span>
                </div>
              ))}
            </div>
            <div className="chat-quick">
              {[t('sala.chatQuick1'), t('sala.chatQuick2'), t('sala.chatQuick3'), t('sala.chatQuick4')].map((phrase) => (
                <button key={phrase} type="button" onClick={() => sendChat(phrase)}>{phrase}</button>
              ))}
            </div>
            <form className="chat-form" onSubmit={(event) => { event.preventDefault(); sendChat(chatText); }}>
              <input value={chatText} onChange={(event) => setChatText(event.target.value)} maxLength={180} placeholder={t('sala.chatPlaceholder')} />
              <IconButton label={t('sala.send')} onClick={() => sendChat(chatText)}><Send size={16} /></IconButton>
            </form>
          </section>

          <section className="room-panel room-info-panel">
            <div className="room-info-row"><QrCode size={17} /><div><strong>{t('sala.qr')}</strong><p>{t('sala.qrHint', { code: room.room.code })}</p></div><Button variant="ghost" size="sm" onClick={() => setQrOpen(true)}>{t('common.open')}</Button></div>
            <div className="room-info-row"><Link2 size={17} /><div><strong>{t('sala.copyLink')}</strong><p>{inviteUrl.slice(0, 44)}…</p></div><Button variant="ghost" size="sm" onClick={() => void copyCode()}>{t('sala.copy')}</Button></div>
            {lanHost ? <div className="room-info-row"><WifiOff size={17} /><div><strong>{t('sala.lanHostTitle')}</strong><p>{t('sala.lanHostNote')}</p></div></div> : null}
            <div className="room-info-row"><ShieldIcon /><div><strong>{t('sala.guestHint')}</strong><p>{t('sala.onlineHint')}</p></div></div>
          </section>
        </aside>
      </div>

      <Modal open={qrOpen} onClose={() => setQrOpen(false)} title={t('sala.qr')} subtitle={t('sala.qrHint', { code: room.room.code })} size="sm">
        <div className="qr-modal">
          <QRCodeView value={inviteUrl} size={230} label={`QR ${room.room.code}`} />
          <strong className="qr-code">{room.room.code}</strong>
          <div className="qr-actions">
            <Button variant="primary" icon={<Copy size={16} />} onClick={() => void copyCode()}>{t('sala.copy')}</Button>
            <Button variant="secondary" icon={<Share2 size={16} />} onClick={() => void share()}>{t('common.share')}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

function ShieldIcon() {
  return <Check size={15} />;
}
