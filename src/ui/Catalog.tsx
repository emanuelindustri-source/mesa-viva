import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { ArrowLeft, ChevronRight, Clock, ExternalLink, FlaskConical, Lock, Play, Search, Users } from 'lucide-react';
import { CATALOG, GAME_BY_ID } from '../core/registry';
import { useT } from '../i18n/useT';
import { Button, Chip, PageHeader } from './components';
import { navigate } from '../app/router';
import { GITHUB_URL } from '../core/constants';

type Category = 'all' | 'cartas' | 'tablero' | 'dados' | 'herramienta';
type Status = 'all' | 'playable' | 'soon' | 'research';

export function CatalogScreen() {
  const t = useT();
  const [category, setCategory] = useState<Category>('all');
  const [status, setStatus] = useState<Status>('all');
  const [query, setQuery] = useState('');
  const entries = useMemo(() => CATALOG.filter((entry) => {
    if (category !== 'all' && entry.category !== category) return false;
    if (status !== 'all' && entry.status !== status) return false;
    if (query.trim() && !`${entry.name} ${entry.note ?? ''}`.toLowerCase().includes(query.toLowerCase())) return false;
    return true;
  }), [category, status, query]);
  const playable = CATALOG.filter((entry) => entry.status === 'playable').length;
  const categories: Array<{ value: Category; label: string }> = [
    { value: 'all', label: t('catalog.filterAll') },
    { value: 'cartas', label: t('catalog.cards') },
    { value: 'tablero', label: t('catalog.board') },
    { value: 'dados', label: t('catalog.dice') },
    { value: 'herramienta', label: t('catalog.tools') },
  ];
  const statuses: Array<{ value: Status; label: string }> = [
    { value: 'all', label: t('common.all') },
    { value: 'playable', label: t('catalog.playable') },
    { value: 'soon', label: t('catalog.soon') },
    { value: 'research', label: t('catalog.research') },
  ];

  return (
    <div className="screen catalog-screen">
      <PageHeader
        title={t('catalog.title')}
        subtitle={t('catalog.subtitle')}
        onBack={() => navigate('/')}
        actions={<Chip tone="gold">{t('catalog.count', { playable, total: CATALOG.length })}</Chip>}
      />

      <div className="catalog-toolbar">
        <label className="search-field">
          <Search size={17} />
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={t('common.search')} />
        </label>
        <div className="filter-row">
          {categories.map((item) => (
            <button key={item.value} type="button" className={category === item.value ? 'is-active' : ''} onClick={() => setCategory(item.value)}>{item.label}</button>
          ))}
        </div>
        <div className="filter-row filter-row-status">
          {statuses.map((item) => (
            <button key={item.value} type="button" className={status === item.value ? 'is-active' : ''} onClick={() => setStatus(item.value)}>{item.label}</button>
          ))}
        </div>
      </div>

      <div className="catalog-grid">
        {entries.map((entry, index) => {
          const meta = GAME_BY_ID[entry.id as keyof typeof GAME_BY_ID];
          const isPlayable = entry.status === 'playable' && Boolean(meta);
          return (
            <motion.article
              key={entry.id}
              className={`catalog-card status-${entry.status}`}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: Math.min(0.3, index * 0.02) }}
            >
              <div className="catalog-card-top">
                <span className="catalog-emoji">{entry.emoji}</span>
                <Chip tone={isPlayable ? 'green' : entry.status === 'soon' ? 'gold' : 'purple'}>
                  {isPlayable ? t('common.playable') : entry.status === 'soon' ? t('common.soon') : t('catalog.research')}
                </Chip>
              </div>
              <h3>{entry.name}</h3>
              <p>{entry.note ?? (isPlayable ? t(meta?.descKey ?? '') : t('catalog.comingSoon'))}</p>
              <div className="catalog-meta">
                <span><Users size={13} /> {entry.players}</span>
                <span><Clock size={13} /> {entry.category === 'cartas' ? t('catalog.cards') : entry.category === 'tablero' ? t('catalog.board') : entry.category === 'dados' ? t('catalog.dice') : t('catalog.tools')}</span>
              </div>
              <div className="catalog-actions">
                {isPlayable ? (
                  <Button variant="primary" size="sm" icon={<Play size={15} />} onClick={() => navigate(`/setup/${entry.id}`)}>{t('common.start')}</Button>
                ) : (
                  <span className="catalog-locked"><Lock size={14} /> {entry.status === 'research' ? t('catalog.researchNote') : t('catalog.comingSoon')}</span>
                )}
                {entry.url ? (
                  <a className="catalog-link" href={entry.url} target="_blank" rel="noreferrer">
                    <ExternalLink size={14} /> {t('catalog.rulesLink')}
                  </a>
                ) : null}
              </div>
              {entry.status !== 'playable' ? (
                <a className="catalog-request" href={`${GITHUB_URL}/issues/new?title=${encodeURIComponent(`[Juego] ${entry.name}`)}&body=${encodeURIComponent('Me gustaría ver este juego en Mesa Viva.')}`} target="_blank" rel="noreferrer">
                  {t('catalog.request')} <ChevronRight size={14} />
                </a>
              ) : null}
            </motion.article>
          );
        })}
      </div>

      {entries.length === 0 ? (
        <div className="empty-state">
          <FlaskConical size={22} />
          <h3>{t('catalog.noResults')}</h3>
          <Button variant="ghost" onClick={() => { setCategory('all'); setStatus('all'); setQuery(''); }}>{t('common.reset')}</Button>
        </div>
      ) : null}

      <div className="catalog-disclaimer">
        <p>{t('catalog.disclaimer')}</p>
        <Button variant="ghost" size="sm" icon={<ArrowLeft size={15} />} onClick={() => navigate('/')}>{t('common.home')}</Button>
      </div>
    </div>
  );
}
