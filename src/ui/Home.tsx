import { useMemo } from 'react';
import type { CSSProperties } from 'react';
import { motion } from 'framer-motion';
import {
  ArrowRight,
  Bot,
  ChevronRight,
  Clock,
  Download,
  Gamepad2,
  LayoutGrid,
  Play,
  Plus,
  Smartphone,
  Sparkles,
  Trophy,
  Users,
  WifiOff,
} from 'lucide-react';
import { GAMES, GAME_BY_ID } from '../core/registry';
import { levelForXp, nextLevel, xpProgress } from '../core/levels';
import { formatDate } from '../core/utils';
import { APP_VERSION, GITHUB_URL, GAMES_COUNT } from '../core/constants';
import { useAppStore } from '../store/appStore';
import { useT, useLocale } from '../i18n/useT';
import { localeMeta } from '../i18n';
import { useFestivalTheme } from '../theme/useTheme';
import { usePwa } from '../services/pwa';
import { Avatar, Button, Chip, ProgressBar, SectionTitle } from './components';
import { navigate } from '../app/router';

export function HomeScreen() {
  const t = useT();
  const locale = useLocale();
  const profile = useAppStore((s) => s.profile);
  const history = useAppStore((s) => s.history);
  const { festival } = useFestivalTheme();
  const { canInstall, installed, install, online } = usePwa();
  const level = levelForXp(profile.xp);
  const next = nextLevel(level.level);
  const progress = xpProgress(profile.xp);
  const hour = new Date().getHours();
  const greeting = hour < 12 ? t('home.greetingMorning', { name: profile.name }) : hour < 19 ? t('home.greetingAfternoon', { name: profile.name }) : t('home.greetingEvening', { name: profile.name });
  const daily = useMemo(() => GAMES[new Date().getDate() % GAMES.length] ?? GAMES[0], []);
  const levelsLeft = next ? next.minXp - profile.xp : 0;

  if (!profile.name) return null;

  return (
    <div className="home-screen">
      <section className="home-hero">
        <div className="hero-ambient" aria-hidden="true">
          <span className="hero-orbit" />
          <span className="hero-orbit orbit-2" />
          <span className="hero-fest-emoji">{festival.emoji}</span>
          <div className="hero-cards" aria-hidden="true">
            <span className="hero-card hero-card-1">A<span>✦</span></span>
            <span className="hero-card hero-card-2">7<span>●</span></span>
            <span className="hero-card hero-card-3">R<span>♛</span></span>
          </div>
        </div>
        <div className="hero-copy">
          <span className="season-pill"><span className="season-dot" /> {festival.emoji} {t(festival.nameKey)} · {t('festival.badge')}</span>
          <h1>{greeting}</h1>
          <p className="hero-sub">{t('home.heroSub')}</p>
          <div className="hero-actions">
            <Button variant="primary" size="lg" icon={<Play size={18} />} onClick={() => navigate(`/setup/${daily?.id ?? 'caida'}?mode=bots`)}>
              {t('home.quickPlay')}
            </Button>
            <Button variant="soft" size="lg" icon={<Bot size={18} />} onClick={() => navigate('/setup/caida?mode=bots')}>
              {t('home.quickBots')}
            </Button>
            <Button variant="ghost" size="lg" icon={<Users size={18} />} onClick={() => navigate('/room')}>
              {t('home.quickRoom')}
            </Button>
          </div>
          <div className="hero-season-line">
            <span>{t(festival.greetingKey)}</span>
          </div>
        </div>

        <div className="hero-side">
          <div className="level-mini-card">
            <div className="level-mini-top">
              <Avatar id={profile.avatar} size={48} ring />
              <div>
                <span className="eyebrow">{t('home.levelProgress')}</span>
                <strong>{t(level.titleKey)}</strong>
                <span className="level-mini-num">{t('common.level')} {level.level} {level.icon}</span>
              </div>
            </div>
            <ProgressBar value={progress.ratio} />
            <div className="level-mini-foot">
              <span>{profile.xp} XP</span>
              <span>{next ? t('home.xpToNext', { xp: levelsLeft, level: next.level }) : t('home.maxLevel')}</span>
            </div>
          </div>
          <button type="button" className="festival-mini-card" onClick={() => navigate('/settings')}>
            <div className="festival-mini-head">
              <span className="festival-mini-emoji">{festival.emoji}</span>
              <div>
                <span className="eyebrow">{t('home.festivalNow')}</span>
                <strong>{t(festival.nameKey)}</strong>
              </div>
              <ChevronRight size={18} />
            </div>
            <p>{t(festival.descKey)}</p>
            <div className="festival-mini-chars">
              {festival.characters.slice(0, 4).map((character) => <span key={character}>{t(character)}</span>)}
            </div>
          </button>
        </div>
      </section>

      {!online ? (
        <div className="offline-banner"><WifiOff size={16} /> {t('home.offline')} · {t('home.offlineDesc')}</div>
      ) : null}

      <section className="home-stats">
        <div className="stat-card"><Trophy size={19} /><strong>{profile.wins}</strong><span>{t('levels.wins')}</span></div>
        <div className="stat-card"><Gamepad2 size={19} /><strong>{profile.matches}</strong><span>{t('levels.matches')}</span></div>
        <div className="stat-card"><Sparkles size={19} /><strong>{profile.bestStreak}</strong><span>{t('levels.bestStreak')}</span></div>
        <div className="stat-card"><Clock size={19} /><strong>{level.level}/10</strong><span>{t('common.level')}</span></div>
      </section>

      <section className="home-games">
        <SectionTitle
          eyebrow={t('home.featured')}
          title={t('home.quickPlay')}
          action={<Button variant="ghost" size="sm" trailing={<ArrowRight size={15} />} onClick={() => navigate('/catalog')}>{t('common.all')}</Button>}
        />
        <div className="game-grid">
          {GAMES.map((game, index) => (
            <motion.button
              type="button"
              key={game.id}
              className="game-tile"
              style={{ '--tile-accent': game.accent } as CSSProperties}
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.04 }}
              onClick={() => navigate(`/setup/${game.id}`)}
            >
              <div className="game-tile-art">
                <span>{game.icon}</span>
                <div className="game-tile-glow" />
              </div>
              <div className="game-tile-body">
                <div className="game-tile-head">
                  <h3>{t(game.nameKey)}</h3>
                  <span className="game-tile-arrow"><ChevronRight size={16} /></span>
                </div>
                <p>{t(game.descKey)}</p>
                <div className="game-tile-meta">
                  <span><Users size={12} /> {game.minPlayers}–{game.maxPlayers}</span>
                  <span><Clock size={12} /> {game.avgMinutes} {t('common.min')}</span>
                  {game.tagKeys.slice(0, 1).map((tag) => <Chip key={tag} tone="default">{t(tag)}</Chip>)}
                </div>
              </div>
            </motion.button>
          ))}
        </div>
      </section>

      <section className="home-room-banner">
        <div className="room-banner-copy">
          <span className="eyebrow">{t('home.quickRoom')}</span>
          <h2>{t('sala.title')}</h2>
          <p>{t('sala.subtitle')}</p>
          <div className="room-banner-actions">
            <Button variant="gold" icon={<Plus size={17} />} onClick={() => navigate('/room')}>{t('sala.create')}</Button>
            <Button variant="ghost" icon={<LayoutGrid size={17} />} onClick={() => navigate('/catalog')}>{t('home.seeCatalog')}</Button>
          </div>
        </div>
        <div className="room-banner-art" aria-hidden="true">
          <span className="room-qr">▦</span>
          <span className="room-card room-card-a">♠</span>
          <span className="room-card room-card-b">♦</span>
          <span className="room-chip">QR</span>
        </div>
      </section>

      <section className="home-recent">
        <SectionTitle
          eyebrow={t('home.recent')}
          title={t('levels.matches')}
          action={<Button variant="ghost" size="sm" onClick={() => navigate('/levels')}>{t('levels.title')}</Button>}
        />
        {history.length === 0 ? (
          <div className="recent-empty">
            <span className="big-emoji">🃏</span>
            <p>{t('home.noRecent')}</p>
            <Button variant="secondary" size="sm" onClick={() => navigate('/catalog')}>{t('home.seeCatalog')}</Button>
          </div>
        ) : (
          <div className="recent-list">
            {history.slice(0, 5).map((entry) => {
              const game = GAME_BY_ID[entry.gameId];
              return (
                <button key={entry.id} type="button" className="recent-row" onClick={() => navigate(`/setup/${entry.gameId}`)}>
                  <span className={`recent-result ${entry.result}`}>{entry.result === 'win' ? '🏆' : entry.result === 'draw' ? '🤝' : '🃏'}</span>
                  <span className="recent-main">
                    <strong>{game ? t(game.nameKey) : entry.gameId}</strong>
                    <span>{formatDate(entry.date, localeMeta(locale).intl)} · {entry.players.slice(0, 2).join(' · ')}</span>
                  </span>
                  <span className="recent-xp">+{entry.xp} XP</span>
                  <ChevronRight size={16} />
                </button>
              );
            })}
          </div>
        )}
      </section>

      <section className="home-install">
        <div className="install-icon"><Smartphone size={22} /></div>
        <div>
          <strong>{t('install.title')}</strong>
          <p>{t('install.desc')}</p>
        </div>
        {installed ? <Chip tone="green">{t('app.installed')}</Chip> : canInstall ? (
          <Button variant="secondary" icon={<Download size={17} />} onClick={() => void install()}>{t('install.button')}</Button>
        ) : (
          <span className="install-hint">{t('install.android')}</span>
        )}
      </section>

      <footer className="home-footer">
        <div className="footer-brand"><span className="brand-mark small">🃏</span><span>Mesa Viva · v{APP_VERSION}</span></div>
        <div className="footer-links">
          <button type="button" onClick={() => navigate('/privacy')}>{t('privacy.title')}</button>
          <button type="button" onClick={() => navigate('/diagnostics')}>{t('diag.title')}</button>
          <button type="button" onClick={() => navigate('/catalog')}>{t('nav.catalog')}</button>
          <a href={GITHUB_URL} target="_blank" rel="noreferrer">{t('settings.openSource')}</a>
        </div>
        <p>{t('settings.madeWith')} · {GAMES_COUNT} {t('common.playable')}</p>
      </footer>
    </div>
  );
}
