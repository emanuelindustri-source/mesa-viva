import { useEffect, useState } from 'react';
import {
  AlertTriangle,
  Bug,
  CheckCircle2,
  Copy,
  Download,
  Mail,
  RefreshCw,
  Send,
  Shield,
  Trash2,
} from 'lucide-react';
import {
  buildReport,
  clearDiagnostics,
  copyReport,
  diag,
  downloadReport,
  getActions,
  getErrors,
  getStorageInfo,
  getSystemInfo,
  mailtoUrl,
  testDiagnostic,
} from '../services/diagnostics';
import { useSession } from '../store/session';
import { useT } from '../i18n/useT';
import { toast } from '../store/toasts';
import { CONTACT_EMAIL } from '../core/constants';
import { Button, Chip, PageHeader, Toggle } from './components';
import { navigate } from '../app/router';

export function DiagnosticsScreen() {
  const t = useT();
  const match = useSession((s) => s.match);
  const [note, setNote] = useState('');
  const [includeErrors, setIncludeErrors] = useState(true);
  const [includeActions, setIncludeActions] = useState(true);
  const [refresh, setRefresh] = useState(0);
  const [storage, setStorage] = useState('—');
  const errors = getErrors();
  const actions = getActions();
  const info = getSystemInfo();

  useEffect(() => {
    void getStorageInfo().then(setStorage);
  }, [refresh]);

  const matchContext = match
    ? `${match.config.gameId} · seed ${match.seed} · ${match.seats.map((s) => `${s.name}${s.isBot ? ' (bot)' : ''}`).join(' / ')}`
    : '';
  const report = () => buildReport(note, { errors: includeErrors, actions: includeActions, match: matchContext });

  const send = () => {
    try {
      window.location.href = mailtoUrl(report(), note);
      toast(t('diag.sentHint'), 'success');
      diag('info', 'Informe de diagnóstico preparado para envío.', undefined, 'diagnostics:mailto');
    } catch {
      toast(t('diag.noMail', { email: CONTACT_EMAIL }), 'warn');
    }
  };

  return (
    <div className="screen diagnostics-screen">
      <PageHeader
        title={t('diag.title')}
        subtitle={t('diag.desc')}
        onBack={() => navigate('/')}
        actions={<Chip tone={errors.length ? 'gold' : 'green'}>{t('diag.errorsCount', { n: errors.length })}</Chip>}
      />

      <section className="diag-contact">
        <div className="diag-contact-icon"><Mail size={22} /></div>
        <div>
          <strong>{CONTACT_EMAIL}</strong>
          <p>{t('diag.contactHint', { email: CONTACT_EMAIL })}</p>
        </div>
        <Button variant="primary" icon={<Send size={16} />} onClick={send}>{t('diag.send')}</Button>
      </section>

      <div className="diag-grid">
        <section className="diag-card">
          <div className="diag-card-head">
            <Bug size={18} />
            <h2>{t('diag.errors')}</h2>
            <Chip tone={errors.length ? 'red' : 'green'}>{errors.length}</Chip>
          </div>
          <div className="diag-error-list">
            {errors.length === 0 ? (
              <div className="diag-empty"><CheckCircle2 size={20} /> {t('diag.empty')}</div>
            ) : errors.slice(0, 20).map((error) => (
              <details key={error.id} className={`diag-error level-${error.level}`}>
                <summary>
                  <span className="diag-dot" />
                  <span className="diag-error-msg">{error.message}</span>
                  <time>{new Date(error.at).toLocaleString()}</time>
                </summary>
                {error.context ? <p className="diag-context">{error.context}</p> : null}
                {error.stack ? <pre>{error.stack}</pre> : null}
              </details>
            ))}
          </div>
          <div className="diag-actions">
            <Button variant="ghost" size="sm" icon={<RefreshCw size={15} />} onClick={() => setRefresh((r) => r + 1)}>{t('diag.refresh')}</Button>
            <Button variant="ghost" size="sm" icon={<Trash2 size={15} />} onClick={() => { clearDiagnostics(); setRefresh((r) => r + 1); toast(t('diag.cleared'), 'success'); }}>{t('diag.clear')}</Button>
            <Button variant="ghost" size="sm" icon={<Bug size={15} />} onClick={() => { testDiagnostic(); setRefresh((r) => r + 1); toast(t('diag.testOk'), 'success'); }}>{t('diag.test')}</Button>
          </div>
        </section>

        <section className="diag-card">
          <div className="diag-card-head"><Shield size={18} /><h2>{t('diag.system')}</h2></div>
          <dl className="diag-system">
            {Object.entries({ ...info, storage }).map(([key, value]) => (
              <div key={key}><dt>{key}</dt><dd>{value}</dd></div>
            ))}
          </dl>
        </section>
      </div>

      <section className="diag-card diag-report-card">
        <div className="diag-card-head"><Send size={18} /><h2>{t('diag.report')}</h2></div>
        <label className="field">
          <span>{t('diag.what')}</span>
          <textarea value={note} onChange={(event) => setNote(event.target.value)} placeholder={t('diag.whatPlaceholder')} rows={3} maxLength={1200} />
        </label>
        <div className="diag-options">
          <Toggle checked={includeErrors} onChange={setIncludeErrors} label={t('diag.includeLogs')} />
          <Toggle checked={includeActions} onChange={setIncludeActions} label={t('diag.includeMatch')} />
        </div>
        <div className="diag-preview">
          <pre>{report()}</pre>
        </div>
        <div className="diag-actions">
          <Button variant="primary" icon={<Send size={16} />} onClick={send}>{t('diag.send')}</Button>
          <Button variant="secondary" icon={<Copy size={16} />} onClick={async () => { const ok = await copyReport(report()); toast(ok ? t('diag.copied') : t('notify.error'), ok ? 'success' : 'danger'); }}>{t('diag.copy')}</Button>
          <Button variant="ghost" icon={<Download size={16} />} onClick={() => { downloadReport(report()); toast(t('diag.downloaded'), 'success'); }}>{t('diag.download')}</Button>
        </div>
        <p className="diag-note"><AlertTriangle size={14} /> {t('diag.privacyNote')}</p>
        <p className="diag-note">{t('diag.copyHint')}</p>
      </section>

      <section className="diag-card">
        <div className="diag-card-head"><Bug size={18} /><h2>{t('diag.recent')}</h2><Chip tone="default">{actions.length}</Chip></div>
        <div className="diag-actions-list">
          {actions.length === 0 ? <p className="diag-empty">{t('common.never')}</p> : actions.slice(0, 30).map((action) => (
            <div key={`${action.at}-${action.name}`} className="diag-action-row">
              <time>{new Date(action.at).toLocaleTimeString()}</time>
              <strong>{action.name}</strong>
              {action.detail ? <span>{action.detail}</span> : null}
            </div>
          ))}
        </div>
      </section>

      <div className="diag-footer">
        <Button variant="ghost" size="sm" onClick={() => navigate('/privacy')}>{t('privacy.title')}</Button>
        <Button variant="ghost" size="sm" onClick={() => navigate('/')}>{t('common.home')}</Button>
      </div>
    </div>
  );
}
