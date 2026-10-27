import { useMemo } from 'react';
import { useAppStore } from '../store/appStore';
import { translateFor, type Locale } from './index';

export type TFn = (key: string, params?: Record<string, unknown>) => string;

export function useT(): TFn {
  const locale = useAppStore((s) => s.settings.language) as Locale;
  return useMemo(() => (key: string, params?: Record<string, unknown>) => translateFor(locale, key, params), [locale]);
}

export function useLocale(): Locale {
  return useAppStore((s) => s.settings.language) as Locale;
}
