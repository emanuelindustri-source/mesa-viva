import { ArrowLeft, ExternalLink, Heart, Mail, Shield, Sparkles } from 'lucide-react';
import { useT } from '../i18n/useT';
import { APP_VERSION, CONTACT_EMAIL, GITHUB_URL, SITE_URL } from '../core/constants';
import { Button, Chip, PageHeader } from './components';
import { navigate } from '../app/router';

export function PrivacyScreen() {
  const t = useT();
  const sections = [
    ['privacy.data', 'privacy.dataDesc'],
    ['privacy.camera', 'privacy.cameraDesc'],
    ['privacy.network', 'privacy.networkDesc'],
    ['privacy.diagnostics', 'privacy.diagnosticsDesc'],
  ] as const;
  return (
    <div className="screen simple-screen">
      <PageHeader title={t('privacy.title')} onBack={() => navigate('/')} />
      <section className="prose-card">
        <div className="prose-intro"><Shield size={24} /><p>{t('privacy.intro')}</p></div>
        {sections.map(([title, body]) => (
          <article key={title}>
            <h2>{t(title)}</h2>
            <p>{t(body)}</p>
          </article>
        ))}
        <article>
          <h2>{t('privacy.contact')}</h2>
          <p>{t('privacy.contactDesc')}</p>
        </article>
        <footer>
          <span>{t('privacy.license')}</span>
          <span>{t('privacy.lastUpdate')}</span>
        </footer>
      </section>
      <div className="simple-actions">
        <Button variant="ghost" icon={<ArrowLeft size={16} />} onClick={() => navigate('/')}>{t('common.home')}</Button>
        <Button variant="ghost" icon={<Mail size={16} />} onClick={() => { window.location.href = `mailto:${CONTACT_EMAIL}`; }}>{t('common.contact')}</Button>
      </div>
    </div>
  );
}

export function AboutScreen() {
  const t = useT();
  return (
    <div className="screen simple-screen">
      <PageHeader title={t('about.title')} onBack={() => navigate('/')} actions={<Chip tone="gold">v{APP_VERSION}</Chip>} />
      <section className="about-hero">
        <span className="about-logo">🃏</span>
        <div>
          <span className="eyebrow">Mesa Viva</span>
          <h2>{t('about.lead')}</h2>
          <p>{t('about.story')}</p>
        </div>
      </section>
      <div className="about-grid">
        <div className="about-card">
          <Sparkles size={20} />
          <h3>{t('about.tech')}</h3>
          <p>{t('about.credits')}</p>
        </div>
        <div className="about-card">
          <Heart size={20} />
          <h3>{t('settings.madeWith')}</h3>
          <p>{t('privacy.license')}</p>
        </div>
        <a className="about-card" href={GITHUB_URL} target="_blank" rel="noreferrer">
          <ExternalLink size={20} />
          <h3>{t('settings.openSource')}</h3>
          <p>{GITHUB_URL}</p>
        </a>
        <a className="about-card" href={SITE_URL} target="_blank" rel="noreferrer">
          <ExternalLink size={20} />
          <h3>{t('app.name')} Web</h3>
          <p>{SITE_URL}</p>
        </a>
      </div>
      <div className="simple-actions">
        <Button variant="ghost" icon={<ArrowLeft size={16} />} onClick={() => navigate('/')}>{t('common.home')}</Button>
        <Button variant="ghost" icon={<Mail size={16} />} onClick={() => { window.location.href = `mailto:${CONTACT_EMAIL}`; }}>{CONTACT_EMAIL}</Button>
      </div>
    </div>
  );
}

export function NotFoundScreen() {
  const t = useT();
  return (
    <div className="screen notfound-screen">
      <div className="missing-card">
        <span className="big-emoji">🃏</span>
        <h1>{t('notfound.title')}</h1>
        <p>{t('notfound.desc')}</p>
        <Button variant="primary" onClick={() => navigate('/')}>{t('notfound.home')}</Button>
      </div>
    </div>
  );
}
