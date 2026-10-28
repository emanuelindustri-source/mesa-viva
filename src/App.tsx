import { useEffect } from 'react';
import type { ReactNode } from 'react';
import {
  Bug,
  Gamepad2,
  Home,
  LayoutGrid,
  Settings,
  Trophy,
  Users,
} from 'lucide-react';
import { useRoute, navigate } from './app/router';
import { useAppStore, type Settings as AppSettings } from './store/appStore';
import { isLocale } from './i18n';
import { useFestivalTheme } from './theme/useTheme';
import { useT } from './i18n/useT';
import { usePwa } from './services/pwa';
import { initAudio, setAudioSettings, startMusic, stopMusic } from './audio/engine';
import { Avatar, ToastHost } from './ui/components';
import { ParticleCanvas } from './ui/particles/ParticleCanvas';
import { OnboardingScreen } from './ui/Onboarding';
import { HomeScreen } from './ui/Home';
import { SetupScreen } from './ui/Setup';
import { TableScreen } from './ui/Table';
import { CatalogScreen } from './ui/Catalog';
import { LevelsScreen } from './ui/Levels';
import { SettingsScreen } from './ui/Settings';
import { DiagnosticsScreen } from './ui/Diagnostics';
import { AboutScreen, NotFoundScreen, PrivacyScreen } from './ui/SimplePages';
import { RoomScreen } from './ui/Room';
import { useScrollTop } from './ErrorBoundary';
import { levelForXp } from './core/levels';
import { APP_VERSION } from './core/constants';

function OfflineNotice() {
  const { online } = usePwa();
  const t = useT();
  if (online) return null;
  return <div className="global-offline"><span className="offline-dot" /> {t('common.offline')} · {t('home.offlineDesc')}</div>;
}

export default function App() {
  const route = useRoute();
  const onboarded = useAppStore((s) => s.onboarded);
  const profile = useAppStore((s) => s.profile);
  const settings = useAppStore((s) => s.settings);
  const { festival, accent } = useFestivalTheme();
  const t = useT();
  const level = levelForXp(profile.xp);
  useScrollTop(route.path);

  // La cáscara nativa pasa idioma y preferencias al abrir la mesa. La web
  // devuelve el perfil al volver mediante ShellSync.
  useEffect(() => {
    const hash = window.location.hash;
    const queryIndex = hash.indexOf('?');
    if (queryIndex < 0) return;
    const params = new URLSearchParams(hash.slice(queryIndex + 1));
    if (params.get('native') !== '1') return;
    const patch: Partial<AppSettings> = {};
    const lang = params.get('lang');
    if (lang && isLocale(lang)) patch.language = lang;
    const music = params.get('music');
    if (music === '0' || music === '1') patch.music = music === '1';
    const sfx = params.get('sfx');
    if (sfx === '0' || sfx === '1') patch.sfx = sfx === '1';
    const haptics = params.get('haptics');
    if (haptics === '0' || haptics === '1') patch.haptics = haptics === '1';
    const motion = params.get('motion');
    if (motion === '0' || motion === '1') patch.reduceMotion = motion === '0';
    if (Object.keys(patch).length > 0) useAppStore.getState().setSettings(patch);
  }, []);

  useEffect(() => {
    setAudioSettings({ music: settings.music, sfx: settings.sfx, volume: settings.volume });
  }, [settings.music, settings.sfx, settings.volume]);

  useEffect(() => {
    if (!settings.music || route.path === '/game') {
      if (!settings.music) stopMusic();
      return;
    }
    let alive = true;
    void initAudio().then((ready) => {
      if (ready && alive) startMusic(festival, null);
    });
    return () => {
      alive = false;
    };
  }, [route.path, festival, settings.music]);

  useEffect(() => {
    const resume = () => {
      void initAudio().then((ready) => {
        if (ready && settings.music && route.path !== '/game') startMusic(festival, null);
      });
      window.removeEventListener('pointerdown', resume);
    };
    window.addEventListener('pointerdown', resume, { once: true });
    return () => window.removeEventListener('pointerdown', resume);
  }, [festival, route.path, settings.music]);

  if (!onboarded) {
    return (
      <>
        <OnboardingScreen onDone={() => navigate('/')} />
        <ParticleCanvas ambient={undefined} colors={[accent, festival.accent2, '#fff4d6']} />
        <ToastHost />
      </>
    );
  }

  const path = route.path;
  const isTable = path === '/game';
  const setupGame = route.parts[0] === 'setup' ? route.parts[1] : null;

  let content: ReactNode;
  if (isTable) content = <TableScreen />;
  else if (path === '/' || path === '/home') content = <HomeScreen />;
  else if (path === '/catalog') content = <CatalogScreen />;
  else if (setupGame) content = <SetupScreen key={route.path} gameId={setupGame} initialMode={route.params.get('mode')} />;
  else if (path === '/levels') content = <LevelsScreen />;
  else if (path === '/settings') content = <SettingsScreen />;
  else if (path === '/diagnostics') content = <DiagnosticsScreen />;
  else if (path === '/room') content = <RoomScreen />;
  else if (path === '/privacy') content = <PrivacyScreen />;
  else if (path === '/about') content = <AboutScreen />;
  else if (path === '/onboarding') content = <OnboardingScreen onDone={() => navigate('/')} />;
  else content = <NotFoundScreen />;

  return (
    <>
      {!isTable ? (
        <div className="app-shell">
          <header className="app-header">
            <button type="button" className="app-brand" onClick={() => navigate('/')} aria-label="Mesa Viva">
              <span className="brand-mark">🃏</span>
              <span className="brand-text">
                <strong>Mesa Viva</strong>
                <small>{t('app.tagline')}</small>
              </span>
            </button>
            <nav className="desktop-nav" aria-label="Navegación principal">
              <button type="button" className={path === '/' ? 'is-active' : ''} onClick={() => navigate('/')}><Home size={16} /> {t('nav.home')}</button>
              <button type="button" className={path === '/catalog' ? 'is-active' : ''} onClick={() => navigate('/catalog')}><LayoutGrid size={16} /> {t('nav.play')}</button>
              <button type="button" className={path === '/room' ? 'is-active' : ''} onClick={() => navigate('/room')}><Users size={16} /> {t('nav.room')}</button>
              <button type="button" className={path === '/levels' ? 'is-active' : ''} onClick={() => navigate('/levels')}><Trophy size={16} /> {t('nav.levels')}</button>
              <button type="button" className={path === '/settings' ? 'is-active' : ''} onClick={() => navigate('/settings')}><Settings size={16} /> {t('nav.settings')}</button>
            </nav>
            <div className="app-header-actions">
              <button type="button" className="season-chip" onClick={() => navigate('/settings')} title={t(festival.nameKey)}>
                <span>{festival.emoji}</span>
                <span className="season-chip-name">{t(festival.nameKey)}</span>
              </button>
              <button type="button" className="level-chip" onClick={() => navigate('/levels')} title={t(level.titleKey)}>
                <Avatar id={profile.avatar} size={28} />
                <span>{level.icon} {level.level}</span>
              </button>
              <button type="button" className="header-icon" onClick={() => navigate('/diagnostics')} aria-label={t('diag.title')}><Bug size={18} /></button>
              <button type="button" className="header-icon" onClick={() => navigate('/settings')} aria-label={t('nav.settings')}><Settings size={18} /></button>
            </div>
          </header>
          <OfflineNotice />
          <main className="app-main">{content}</main>
          <nav className="bottom-nav" aria-label={t('nav.home')}>
            <button type="button" className={path === '/' ? 'is-active' : ''} onClick={() => navigate('/')}><Home size={20} /><span>{t('nav.home')}</span></button>
            <button type="button" className={path === '/catalog' || setupGame ? 'is-active' : ''} onClick={() => navigate('/catalog')}><Gamepad2 size={20} /><span>{t('nav.play')}</span></button>
            <button type="button" className={path === '/room' ? 'is-active' : ''} onClick={() => navigate('/room')}><Users size={20} /><span>{t('nav.room')}</span></button>
            <button type="button" className={path === '/levels' ? 'is-active' : ''} onClick={() => navigate('/levels')}><Trophy size={20} /><span>{t('nav.levels')}</span></button>
            <button type="button" className={path === '/settings' ? 'is-active' : ''} onClick={() => navigate('/settings')}><Settings size={20} /><span>{t('nav.settings')}</span></button>
          </nav>
          <footer className="app-footer">
            <span>Mesa Viva v{APP_VERSION} · {t('settings.madeWith')}</span>
            <div>
              <button type="button" onClick={() => navigate('/privacy')}>{t('privacy.title')}</button>
              <button type="button" onClick={() => navigate('/about')}>{t('common.about')}</button>
              <button type="button" onClick={() => navigate('/diagnostics')}>{t('diag.title')}</button>
            </div>
          </footer>
        </div>
      ) : content}
      <ParticleCanvas
        ambient={settings.reduceMotion ? undefined : isTable ? 'dust' : festival.particle}
        colors={[accent, festival.accent2, '#fff4d6']}
      />
      <ToastHost />
    </>
  );
}
