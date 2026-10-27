import { useEffect, useRef, useState } from 'react';
import type { CSSProperties } from 'react';
import { motion } from 'framer-motion';
import { ArrowRight, Check, ChevronLeft, Globe2, Heart, Shield, Sparkles } from 'lucide-react';
import { AVATARS, GAMES_COUNT } from '../core/constants';
import { useAppStore } from '../store/appStore';
import { useT } from '../i18n/useT';
import { COUNTRY_NAMES, LOCALES, detectCountry, localeMeta, setLocale, type Locale } from '../i18n';
import { useFestivalTheme } from '../theme/useTheme';
import { Button } from './components';
import { initAudio } from '../audio/engine';
import { burst } from './particles/bus';
import { trackAction } from '../services/diagnostics';

export function OnboardingScreen({ onDone }: { onDone: () => void }) {
  const t = useT();
  const profile = useAppStore((s) => s.profile);
  const settings = useAppStore((s) => s.settings);
  const onboard = useAppStore((s) => s.onboard);
  const setSettings = useAppStore((s) => s.setSettings);
  const { festival } = useFestivalTheme();
  const [step, setStep] = useState(0);
  const [name, setName] = useState(profile.name);
  const [avatar, setAvatar] = useState(profile.avatar || 'jaguar');
  const [accepted, setAccepted] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    panelRef.current?.scrollTo({ top: 0, behavior: 'smooth' });
    window.scrollTo({ top: 0, behavior: 'auto' });
  }, [step]);

  const chooseLanguage = (code: Locale) => {
    setLocale(code);
    setSettings({ language: code, country: detectCountry(code) });
  };

  const next = () => {
    if (step === 0 && name.trim().length < 2) return;
    if (step < 2) {
      setStep((s) => s + 1);
      return;
    }
    if (!accepted) return;
    onboard({ name, avatar, language: settings.language, country: settings.country });
    void initAudio().then((ready) => {
      if (ready) burst('confetti', window.innerWidth / 2, window.innerHeight * 0.35, { count: 60 });
    });
    trackAction('onboarding:complete', settings.language);
    onDone();
  };

  return (
    <div className="onboarding">
      <div className="onboard-visual">
        <div className="onboard-glow" style={{ background: `radial-gradient(circle at 30% 20%, ${festival.accent}55, transparent 60%)` }} />
        <div className="onboard-brand">
          <span className="brand-mark">🃏</span>
          <div>
            <span className="brand-name">Mesa Viva</span>
            <span className="brand-sub">{t('app.tagline')}</span>
          </div>
        </div>
        <div className="onboard-art" aria-hidden="true">
          <span className="art-card art-1">A</span>
          <span className="art-card art-2">7</span>
          <span className="art-card art-3">R</span>
          <span className="art-die">⚄</span>
          <span className="art-emoji">{festival.emoji}</span>
        </div>
        <div className="onboard-quote">
          <p>{t('onboarding.lead')}</p>
          <span>{t('app.subtitle')}</span>
        </div>
        <div className="onboard-badges">
          <span><Shield size={14} /> {t('onboarding.privacyTitle')}</span>
          <span><Globe2 size={14} /> {LOCALES.length} {t('common.language')}</span>
          <span><Sparkles size={14} /> {GAMES_COUNT} {t('common.playable')}</span>
        </div>
      </div>

      <div className="onboard-panel" ref={panelRef}>
        <div className="step-dots" aria-hidden="true">
          {[0, 1, 2].map((i) => <span key={i} className={i === step ? 'is-active' : i < step ? 'is-done' : ''} />)}
        </div>
        <div className="onboard-step-label">{t('onboarding.step', { n: step + 1, total: 3 })}</div>

        <motion.div key={step} initial={{ opacity: 0, x: 12 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.25 }} className="onboard-step">
          {step === 0 ? (
            <>
              <span className="eyebrow">01 · {t('app.name')}</span>
              <h1>{t('onboarding.welcome')}</h1>
              <p className="lead">{t('onboarding.lead')}</p>
              <label className="field">
                <span>{t('onboarding.name')}</span>
                <input
                  type="text"
                  value={name}
                  maxLength={16}
                  autoFocus
                  onChange={(event) => setName(event.target.value)}
                  placeholder={t('onboarding.namePlaceholder')}
                />
              </label>
              <div className="field-label">{t('onboarding.avatar')}</div>
              <div className="avatar-grid">
                {AVATARS.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    className={`avatar-choice ${avatar === item.id ? 'is-active' : ''}`}
                    onClick={() => setAvatar(item.id)}
                    aria-label={item.label}
                    aria-pressed={avatar === item.id}
                  >
                    <span>{item.emoji}</span>
                    {avatar === item.id ? <i><Check size={10} /></i> : null}
                  </button>
                ))}
              </div>
            </>
          ) : null}

          {step === 1 ? (
            <>
              <span className="eyebrow">02 · {t('onboarding.language')}</span>
              <h1>{t('onboarding.language')}</h1>
              <p className="lead">{t('settings.languageDesc')}</p>
              <div className="lang-grid">
                {LOCALES.map((locale) => (
                  <button
                    key={locale.code}
                    type="button"
                    className={`lang-choice ${settings.language === locale.code ? 'is-active' : ''}`}
                    onClick={() => chooseLanguage(locale.code)}
                  >
                    <span className="lang-flag">{locale.flag}</span>
                    <span>{locale.label}</span>
                    {settings.language === locale.code ? <Check size={14} /> : null}
                  </button>
                ))}
              </div>
              <label className="field">
                <span>{t('onboarding.country')}</span>
                <select value={settings.country} onChange={(event) => setSettings({ country: event.target.value })}>
                  {Object.entries(COUNTRY_NAMES).map(([code, label]) => (
                    <option key={code} value={code}>{label}</option>
                  ))}
                </select>
                <small>{t('onboarding.countryHint')}</small>
              </label>
              <div className="detected-note"><Sparkles size={14} /> {t('onboarding.detected', { value: `${localeMeta(settings.language).label} · ${COUNTRY_NAMES[settings.country] ?? settings.country}` })}</div>
            </>
          ) : null}

          {step === 2 ? (
            <>
              <span className="eyebrow">03 · {t('onboarding.theme')}</span>
              <h1>{t('onboarding.theme')}</h1>
              <p className="lead">{t('onboarding.themeHint')}</p>
              <div className="theme-preview" style={{ '--preview-accent': festival.accent, '--preview-accent2': festival.accent2 } as CSSProperties}>
                <div className="theme-preview-top">
                  <span className="festival-pill">{festival.emoji} {t(festival.nameKey)}</span>
                  <span className="theme-swatch" />
                </div>
                <div className="theme-preview-cards">
                  <span className="preview-card">A</span>
                  <span className="preview-card">7</span>
                  <span className="preview-card">R</span>
                </div>
                <div className="theme-preview-bar"><span /></div>
                <div className="theme-preview-footer">
                  <span>{t('theme.dynamic')}</span>
                  <span>{t('theme.festival')}</span>
                </div>
              </div>
              <div className="privacy-blurb">
                <Shield size={20} />
                <div>
                  <strong>{t('onboarding.privacyTitle')}</strong>
                  <p>{t('onboarding.privacyText')}</p>
                </div>
              </div>
              <label className="check-row">
                <input type="checkbox" checked={accepted} onChange={(event) => setAccepted(event.target.checked)} />
                <span>{t('onboarding.terms')}</span>
              </label>
              <div className="heart-note"><Heart size={14} /> {t('settings.madeWith')}</div>
            </>
          ) : null}
        </motion.div>

        <footer className="onboard-footer">
          {step > 0 ? (
            <Button variant="ghost" icon={<ChevronLeft size={17} />} onClick={() => setStep((s) => s - 1)}>
              {t('common.back')}
            </Button>
          ) : <span />}
          <Button
            variant="primary"
            size="lg"
            trailing={step === 2 ? <Sparkles size={18} /> : <ArrowRight size={18} />}
            disabled={(step === 0 && name.trim().length < 2) || (step === 2 && !accepted)}
            onClick={next}
          >
            {step === 2 ? t('onboarding.start') : t('common.continue')}
          </Button>
        </footer>
      </div>
    </div>
  );
}
