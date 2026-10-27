import { es, type TranslationKey } from './es';
import { en } from './en';
import { de, fr, it, ja, ptBR, ptPT, ru, zhCN, zhTW } from './world';
import type { CardView, LogEvent } from '../core/types';

export const LOCALES = [
  { code: 'es', label: 'Español', flag: '🇻🇪', intl: 'es-VE', country: 'VE' },
  { code: 'en', label: 'English', flag: '🇬🇧', intl: 'en-US', country: 'US' },
  { code: 'pt-BR', label: 'Português (Brasil)', flag: '🇧🇷', intl: 'pt-BR', country: 'BR' },
  { code: 'pt-PT', label: 'Português (Portugal)', flag: '🇵🇹', intl: 'pt-PT', country: 'PT' },
  { code: 'de', label: 'Deutsch', flag: '🇩🇪', intl: 'de-DE', country: 'DE' },
  { code: 'ru', label: 'Русский', flag: '🇷🇺', intl: 'ru-RU', country: 'RU' },
  { code: 'it', label: 'Italiano', flag: '🇮🇹', intl: 'it-IT', country: 'IT' },
  { code: 'fr', label: 'Français', flag: '🇫🇷', intl: 'fr-FR', country: 'FR' },
  { code: 'zh-TW', label: '繁體中文', flag: '🇹🇼', intl: 'zh-TW', country: 'TW' },
  { code: 'zh-CN', label: '简体中文', flag: '🇨🇳', intl: 'zh-CN', country: 'CN' },
  { code: 'ja', label: '日本語', flag: '🇯🇵', intl: 'ja-JP', country: 'JP' },
] as const;

export type Locale = (typeof LOCALES)[number]['code'];

export const COUNTRY_NAMES: Record<string, string> = {
  VE: 'Venezuela', CO: 'Colombia', AR: 'Argentina', MX: 'México', CL: 'Chile', PE: 'Perú',
  EC: 'Ecuador', BO: 'Bolivia', UY: 'Uruguay', PY: 'Paraguay', PA: 'Panamá', CR: 'Costa Rica',
  DO: 'República Dominicana', PR: 'Puerto Rico', CU: 'Cuba', ES: 'España', US: 'United States',
  BR: 'Brasil', PT: 'Portugal', DE: 'Deutschland', RU: 'Россия', IT: 'Italia', FR: 'France',
  TW: '台灣', CN: '中国', JP: '日本', GB: 'United Kingdom', CA: 'Canada',
};

const catalogs: Record<Locale, Record<string, string>> = {
  es: { ...es },
  en: { ...es, ...en },
  'pt-BR': { ...es, ...en, ...ptBR },
  'pt-PT': { ...es, ...en, ...ptPT },
  de: { ...es, ...en, ...de },
  ru: { ...es, ...en, ...ru },
  it: { ...es, ...en, ...it },
  fr: { ...es, ...en, ...fr },
  'zh-TW': { ...es, ...en, ...zhTW },
  'zh-CN': { ...es, ...en, ...zhCN },
  ja: { ...es, ...en, ...ja },
};

let current: Locale = 'es';

export function isLocale(value: string): value is Locale {
  return LOCALES.some((l) => l.code === value);
}

export function detectLocale(): Locale {
  const nav = typeof navigator !== 'undefined' ? navigator : undefined;
  if (!nav) return 'es';
  const languages = nav.languages?.length ? [...nav.languages] : [nav.language];
  for (const raw of languages) {
    if (!raw) continue;
    const tag = raw.toLowerCase();
    if (tag.startsWith('pt')) {
      if (tag.includes('br')) return 'pt-BR';
      if (tag.includes('pt')) return 'pt-PT';
      return 'pt-BR';
    }
    if (tag.startsWith('zh')) {
      if (tag.includes('tw') || tag.includes('hk') || tag.includes('mo') || tag.includes('hant')) return 'zh-TW';
      return 'zh-CN';
    }
    if (tag.startsWith('es')) return 'es';
    if (tag.startsWith('en')) return 'en';
    if (tag.startsWith('de')) return 'de';
    if (tag.startsWith('ru')) return 'ru';
    if (tag.startsWith('it')) return 'it';
    if (tag.startsWith('fr')) return 'fr';
    if (tag.startsWith('ja')) return 'ja';
  }
  return 'es';
}

export function localeMeta(code: Locale) {
  return LOCALES.find((l) => l.code === code) ?? LOCALES[0];
}

export function detectCountry(locale: Locale): string {
  const nav = typeof navigator !== 'undefined' ? navigator : undefined;
  const raw = nav?.languages?.[0] ?? nav?.language ?? localeMeta(locale).intl;
  const region = raw.split('-')[1]?.toUpperCase();
  if (region && COUNTRY_NAMES[region]) return region;
  return localeMeta(locale).country;
}

export function setLocale(locale: Locale): void {
  current = locale;
  if (typeof document !== 'undefined') {
    document.documentElement.lang = locale;
    document.documentElement.dir = 'ltr';
  }
}

export function getLocale(): Locale {
  return current;
}

export function interpolate(template: string, params?: Record<string, unknown>): string {
  if (!params) return template;
  return template.replace(/\{(\w+)\}/g, (match, key: string) => {
    const value = params[key];
    if (value === undefined || value === null) return match;
    if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
      return String(value);
    }
    return String(value);
  });
}

export function translateFor(locale: Locale, key: string, params?: Record<string, unknown>): string {
  const catalog = catalogs[locale] ?? catalogs.es;
  const value =
    catalog[key] ??
    catalogs.en[key] ??
    catalogs.es[key] ??
    key.split('.').slice(-1)[0] ??
    key;
  return interpolate(value, params);
}

export function t(key: string, params?: Record<string, unknown>): string {
  return translateFor(current, key, params);
}

export function hasTranslation(key: string, locale: Locale = current): boolean {
  return Boolean(catalogs[locale]?.[key]);
}

export function cardName(card: CardView, tr: (key: string, params?: Record<string, unknown>) => string = t): string {
  const suit = String(card.suit);
  if (suit === 'wild') {
    return card.action ? tr(`card.${card.action}`) : tr('card.wild');
  }
  if (['sol', 'mar', 'selva', 'magma'].includes(suit)) {
    if (card.action && card.action !== 'none') {
      const action = tr(`card.${card.action}`);
      const color = tr(`color.${suit}`);
      return `${action} · ${color}`;
    }
    return tr('card.colorOf', { rank: card.rank, color: tr(`color.${suit}`) });
  }
  const rank =
    card.rank >= 10 && card.rank <= 12 ? tr(`rank.${card.rank}`) : String(card.rank);
  return tr('card.of', { rank, suit: tr(`suit.${suit}`) });
}

export function formatLog(event: LogEvent, tr: (key: string, params?: Record<string, unknown>) => string = t): string {
  const params: Record<string, unknown> = { ...(event.params ?? {}) };
  const card = params.card as CardView | undefined;
  if (card && typeof card === 'object') params.card = cardName(card, tr);
  const tile = params.tile as { a: number; b: number } | undefined;
  if (tile && typeof tile === 'object') params.tile = `${tile.a} · ${tile.b}`;
  if (typeof params.canto === 'string' && params.canto.length > 0) {
    params.canto = tr(`canto.${params.canto}`);
  }
  if (typeof params.game === 'string' && params.game.startsWith('game.')) {
    params.game = tr(params.game);
  }
  if (typeof params.end === 'string') {
    params.end = params.end === 'left' ? tr('table.leftEnd') : params.end === 'right' ? tr('table.rightEnd') : params.end;
  }
  return tr(event.key, params);
}

export type { TranslationKey };
