import { useRef, useState } from 'react';
import {
  Bug,
  Check,
  ChevronRight,
  Download,
  ExternalLink,
  Globe2,
  Heart,
  Image as ImageIcon,
  Languages,
  Moon,
  Music2,
  Palette,
  Shield,
  Smartphone,
  Sparkles,
  Sun,
  Trash2,
  Upload,
  Volume2,
  VolumeX,
  Wifi,
} from 'lucide-react';
import { useAppStore, type AccentSource } from '../store/appStore';
import { useT } from '../i18n/useT';
import { COUNTRY_NAMES, LOCALES, detectCountry, localeMeta, type Locale } from '../i18n';
import { FESTIVALS, fetchSystemAccent } from '../theme/festivals';
import { useFestivalTheme } from '../theme/useTheme';
import { extractWallpaperAccent } from '../services/wallpaper';
import { initAudio, setAudioSettings, sfx } from '../audio/engine';
import { usePwa } from '../services/pwa';
import { toast } from '../store/toasts';
import { trackAction } from '../services/diagnostics';
import { APP_VERSION, CONTACT_EMAIL, GITHUB_URL, AVATARS } from '../core/constants';
import { Avatar, Button, Chip, Modal, PageHeader, Segmented, SettingRow, Toggle } from './components';
import { navigate } from '../app/router';

export function SettingsScreen() {
  const t = useT();
  const profile = useAppStore((s) => s.profile);
  const settings = useAppStore((s) => s.settings);
  const setSettings = useAppStore((s) => s.setSettings);
  const setProfile = useAppStore((s) => s.setProfile);
  const clearHistory = useAppStore((s) => s.clearHistory);
  const resetAll = useAppStore((s) => s.resetAll);
  const { festival, accent } = useFestivalTheme();
  const { canInstall, installed, install } = usePwa();
  const [name, setName] = useState(profile.name);
  const [resetOpen, setResetOpen] = useState(false);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [roomServer, setRoomServer] = useState(() => (typeof localStorage === 'undefined' ? '' : localStorage.getItem('mesa-viva-room-server') ?? ''));
  const [wallpaperBusy, setWallpaperBusy] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);
  const systemAccent = fetchSystemAccent();

  const changeLanguage = (code: Locale) => {
    setSettings({ language: code, country: detectCountry(code) });
    toast(t('settings.languageChanged'), 'success');
    trackAction('settings:language', code);
  };

  const changeTheme = (value: 'auto' | 'light' | 'dark') => {
    setSettings({ theme: value });
    toast(t('notify.themeChanged'), 'success');
  };

  const handleWallpaper = async (file: File | undefined) => {
    if (!file) return;
    setWallpaperBusy(true);
    try {
      const color = await extractWallpaperAccent(file);
      setSettings({ wallpaperColor: color, accentSource: 'wallpaper' });
      toast(t('notify.themeChanged'), 'success');
      trackAction('settings:wallpaper', color);
    } catch {
      toast(t('settings.wallpaperError'), 'danger');
    } finally {
      setWallpaperBusy(false);
    }
  };

  const testSound = async () => {
    const ready = await initAudio();
    if (!ready) {
      toast(t('error.audio'), 'warn');
      return;
    }
    setAudioSettings({ music: settings.music, sfx: settings.sfx, volume: settings.volume });
    sfx('canto');
    toast(t('settings.audioTest'), 'success');
  };

  const saveRoomServer = () => {
    const clean = roomServer.trim();
    if (clean && !/^wss?:\/\//i.test(clean)) {
      toast(t('sala.serverInvalid'), 'warn');
      return;
    }
    if (clean) localStorage.setItem('mesa-viva-room-server', clean);
    else localStorage.removeItem('mesa-viva-room-server');
    toast(t('settings.roomServerSaved'), 'success');
    trackAction('settings:roomServer', clean || 'default');
  };

  const accentSources: Array<{ value: AccentSource; label: string }> = [
    { value: 'season', label: t('theme.festival') },
    { value: 'system', label: t('theme.system') },
    { value: 'custom', label: t('settings.manualColor') },
    { value: 'wallpaper', label: t('settings.wallpaper') },
  ];

  return (
    <div className="screen settings-screen">
      <PageHeader title={t('settings.title')} subtitle={t('settings.aboutDesc')} onBack={() => navigate('/')} />

      <section className="settings-section">
        <div className="settings-section-head"><Languages size={19} /><h2>{t('settings.language')}</h2></div>
        <p className="settings-desc">{t('settings.languageDesc')}</p>
        <div className="lang-grid lang-grid-settings">
          {LOCALES.map((locale) => (
            <button key={locale.code} type="button" className={`lang-choice ${settings.language === locale.code ? 'is-active' : ''}`} onClick={() => changeLanguage(locale.code)}>
              <span className="lang-flag">{locale.flag}</span>
              <span>{locale.label}</span>
              {settings.language === locale.code ? <Check size={14} /> : null}
            </button>
          ))}
        </div>
        <SettingRow icon={<Globe2 size={18} />} title={t('settings.country')} hint={t('settings.countryDesc')}>
          <select className="select" value={settings.country} onChange={(event) => setSettings({ country: event.target.value })}>
            {Object.entries(COUNTRY_NAMES).map(([code, label]) => <option key={code} value={code}>{label}</option>)}
          </select>
        </SettingRow>
      </section>

      <section className="settings-section">
        <div className="settings-section-head"><Palette size={19} /><h2>{t('settings.appearance')}</h2></div>
        <SettingRow icon={settings.theme === 'light' ? <Sun size={18} /> : <Moon size={18} />} title={t('settings.theme')}>
          <Segmented
            value={settings.theme}
            onChange={changeTheme}
            options={[
              { value: 'auto', label: t('settings.themeAuto') },
              { value: 'light', label: t('settings.themeLight') },
              { value: 'dark', label: t('settings.themeDark') },
            ]}
          />
        </SettingRow>
        <SettingRow icon={<Sparkles size={18} />} title={t('settings.accent')} hint={t('settings.accentHint')}>
          <div className="accent-preview" style={{ background: accent }} aria-label={accent} />
        </SettingRow>
        <div className="accent-sources">
          {accentSources.map((source) => (
            <button key={source.value} type="button" className={settings.accentSource === source.value ? 'is-active' : ''} onClick={() => setSettings({ accentSource: source.value })}>
              {source.label}
            </button>
          ))}
        </div>
        {settings.accentSource === 'custom' ? (
          <div className="accent-custom">
            <input type="color" value={settings.accent ?? festival.accent} onChange={(event) => setSettings({ accent: event.target.value })} aria-label={t('settings.accent')} />
            <span>{settings.accent ?? festival.accent}</span>
            <Button variant="ghost" size="sm" onClick={() => setSettings({ accent: null })}>{t('settings.accentAuto')}</Button>
          </div>
        ) : null}
        {settings.accentSource === 'system' ? (
          <div className="system-color-note">
            <span className="system-swatch" style={{ background: systemAccent ?? '#888' }} />
            {systemAccent ? <span>{t('settings.systemColor')} · {systemAccent}</span> : <span>{t('settings.dynamicColorDesc')}</span>}
          </div>
        ) : null}
        {settings.accentSource === 'wallpaper' ? (
          <div className="wallpaper-row">
            <input ref={fileRef} type="file" accept="image/*" hidden onChange={(event) => void handleWallpaper(event.target.files?.[0])} />
            <Button variant="secondary" size="sm" icon={<Upload size={16} />} loading={wallpaperBusy} onClick={() => fileRef.current?.click()}>{t('settings.wallpaper')}</Button>
            {settings.wallpaperColor ? <span className="wallpaper-swatch" style={{ background: settings.wallpaperColor }} /> : null}
            <span className="settings-hint">{t('settings.wallpaperDesc')}</span>
          </div>
        ) : null}
        <SettingRow icon={<ImageIcon size={18} />} title={t('settings.festival')} hint={t('settings.festivalDesc')}>
          <select className="select" value={settings.festival} onChange={(event) => setSettings({ festival: event.target.value })}>
            <option value="auto">{t('settings.festivalAuto')}</option>
            {Object.values(FESTIVALS).map((item) => (
              <option key={item.id} value={item.id}>{item.emoji} {t(item.nameKey)}</option>
            ))}
          </select>
        </SettingRow>
        <Toggle checked={settings.dynamicColor} onChange={(value) => setSettings({ dynamicColor: value })} label={t('settings.dynamicColor')} hint={t('settings.dynamicColorDesc')} />
        <Toggle checked={settings.highContrast} onChange={(value) => setSettings({ highContrast: value })} label={t('settings.contrast')} hint={t('settings.contrastDesc')} />
      </section>

      <section className="settings-section">
        <div className="settings-section-head"><Music2 size={19} /><h2>{t('common.sound')}</h2></div>
        <Toggle checked={settings.music} onChange={(value) => setSettings({ music: value })} label={t('settings.music')} hint={t('settings.musicDesc')} />
        <Toggle checked={settings.sfx} onChange={(value) => setSettings({ sfx: value })} label={t('settings.sfx')} hint={t('settings.sfxDesc')} />
        <SettingRow icon={settings.music || settings.sfx ? <Volume2 size={18} /> : <VolumeX size={18} />} title={t('settings.volume') ?? t('common.sound')}>
          <div className="volume-control">
            <input type="range" min={0} max={100} value={Math.round(settings.volume * 100)} onChange={(event) => setSettings({ volume: Number(event.target.value) / 100 })} />
            <span>{Math.round(settings.volume * 100)}%</span>
          </div>
        </SettingRow>
        <div className="settings-inline-actions">
          <Button variant="ghost" size="sm" icon={<Volume2 size={15} />} onClick={() => void testSound()}>{t('settings.audioTest')}</Button>
        </div>
        <Toggle checked={settings.haptics} onChange={(value) => setSettings({ haptics: value })} label={t('settings.haptics')} hint={t('settings.hapticsDesc')} />
        <Toggle checked={settings.reduceMotion} onChange={(value) => setSettings({ reduceMotion: value })} label={t('settings.reduceMotion')} hint={t('settings.reduceMotionDesc')} />
      </section>

      <section className="settings-section">
        <div className="settings-section-head"><Avatar id={profile.avatar} size={24} /><h2>{t('settings.profile')}</h2></div>
        <label className="field">
          <span>{t('settings.profile')}</span>
          <div className="inline-field">
            <input value={name} maxLength={16} onChange={(event) => setName(event.target.value)} />
            <Button variant="secondary" size="sm" onClick={() => { setProfile({ name }); toast(t('settings.saved'), 'success'); }}>{t('common.save')}</Button>
          </div>
        </label>
        <div className="field-label">{t('onboarding.avatar')}</div>
        <div className="avatar-grid avatar-grid-settings">
          {AVATARS.map((item) => (
            <button key={item.id} type="button" className={`avatar-choice ${profile.avatar === item.id ? 'is-active' : ''}`} onClick={() => setProfile({ avatar: item.id })} aria-label={item.label}>
              <span>{item.emoji}</span>
              {profile.avatar === item.id ? <i><Check size={10} /></i> : null}
            </button>
          ))}
        </div>
        <div className="settings-inline-actions">
          <span className="settings-hint">{t('settings.profile')} · {profile.matches} {t('levels.matches')}</span>
        </div>
      </section>

      <section className="settings-section">
        <div className="settings-section-head"><Wifi size={19} /><h2>{t('settings.roomServer')}</h2></div>
        <p className="settings-desc">{t('settings.roomServerDesc')}</p>
        <div className="inline-field">
          <input
            value={roomServer}
            onChange={(event) => setRoomServer(event.target.value)}
            placeholder={t('settings.roomServerPlaceholder')}
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck={false}
          />
          <Button variant="secondary" size="sm" onClick={saveRoomServer}>{t('common.save')}</Button>
        </div>
      </section>

      <section className="settings-section">
        <div className="settings-section-head"><Smartphone size={19} /><h2>{t('install.title')}</h2></div>
        <p className="settings-desc">{t('install.desc')}</p>
        <div className="settings-inline-actions">
          {installed ? <Chip tone="green"><Check size={12} /> {t('app.installed')}</Chip> : canInstall ? <Button variant="secondary" icon={<Download size={16} />} onClick={() => void install()}>{t('install.button')}</Button> : <span className="settings-hint">{t('install.android')}</span>}
        </div>
      </section>

      <section className="settings-section">
        <div className="settings-section-head"><Shield size={19} /><h2>{t('settings.privacy')}</h2></div>
        <p className="settings-desc">{t('settings.privacyDesc')}</p>
        <div className="settings-links">
          <button type="button" onClick={() => navigate('/privacy')}><Shield size={16} /> {t('privacy.title')} <ChevronRight size={15} /></button>
          <button type="button" onClick={() => navigate('/diagnostics')}><Bug size={16} /> {t('diag.title')} <ChevronRight size={15} /></button>
          <a href={GITHUB_URL} target="_blank" rel="noreferrer"><ExternalLink size={16} /> {t('settings.openSource')} <ChevronRight size={15} /></a>
          <a href={`mailto:${CONTACT_EMAIL}`}><Heart size={16} /> {t('common.contact')} <ChevronRight size={15} /></a>
        </div>
        <Toggle checked={settings.tips} onChange={(value) => setSettings({ tips: value })} label={t('settings.tips')} hint={t('settings.tipsDesc')} />
        <Toggle checked={settings.keepAwake} onChange={(value) => setSettings({ keepAwake: value })} label={t('settings.keepAwake')} hint={t('settings.keepAwakeDesc')} />
      </section>

      <section className="settings-section settings-danger">
        <div className="settings-section-head"><Trash2 size={19} /><h2>{t('settings.data')}</h2></div>
        <div className="settings-data-row">
          <div>
            <strong>{t('settings.clearHistory')}</strong>
            <p>{t('settings.clearHistoryConfirm')}</p>
          </div>
          <Button variant="ghost" size="sm" onClick={() => setHistoryOpen(true)}>{t('settings.clearHistory')}</Button>
        </div>
        <div className="settings-data-row">
          <div>
            <strong>{t('settings.reset')}</strong>
            <p>{t('settings.resetConfirm')}</p>
          </div>
          <Button variant="danger" size="sm" onClick={() => setResetOpen(true)}>{t('settings.reset')}</Button>
        </div>
      </section>

      <section className="settings-about">
        <div className="brand-mark small">🃏</div>
        <div>
          <strong>Mesa Viva {APP_VERSION}</strong>
          <p>{t('about.tech')}</p>
          <button type="button" onClick={() => navigate('/about')}>{t('common.about')} <ChevronRight size={14} /></button>
        </div>
      </section>

      <Modal open={historyOpen} onClose={() => setHistoryOpen(false)} title={t('settings.clearHistory')} subtitle={t('settings.clearHistoryConfirm')} size="sm">
        <div className="modal-inline-actions">
          <Button variant="ghost" onClick={() => setHistoryOpen(false)}>{t('common.cancel')}</Button>
          <Button variant="danger" onClick={() => { clearHistory(); setHistoryOpen(false); toast(t('notify.saved'), 'success'); }}>{t('common.delete')}</Button>
        </div>
      </Modal>

      <Modal open={resetOpen} onClose={() => setResetOpen(false)} title={t('settings.reset')} subtitle={t('settings.resetConfirm')} size="sm">
        <div className="modal-inline-actions">
          <Button variant="ghost" onClick={() => setResetOpen(false)}>{t('common.cancel')}</Button>
          <Button variant="danger" onClick={() => { resetAll(); setResetOpen(false); navigate('/onboarding'); }}>{t('common.confirm')}</Button>
        </div>
      </Modal>

      <div className="settings-footer">
        <span>{t('settings.madeWith')} · v{APP_VERSION}</span>
        <span>{localeMeta(settings.language).flag} {localeMeta(settings.language).label}</span>
      </div>
    </div>
  );
}
