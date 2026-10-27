import { useEffect, useState } from 'react';

export interface Route {
  path: string;
  parts: string[];
  params: URLSearchParams;
}

function parse(): Route {
  const raw = typeof window === 'undefined' ? '' : window.location.hash.replace(/^#/, '') || '/';
  const [pathRaw = '/', query = ''] = raw.split('?');
  const path = pathRaw || '/';
  const clean = path.startsWith('/') ? path : `/${path}`;
  return {
    path: clean,
    parts: clean.split('/').filter(Boolean),
    params: new URLSearchParams(query),
  };
}

export function navigate(path: string): void {
  const target = path.startsWith('#') ? path : `#${path.startsWith('/') ? path : `/${path}`}`;
  if (window.location.hash === target) return;
  window.location.hash = target;
}

export function useRoute(): Route {
  const [route, setRoute] = useState<Route>(parse);
  useEffect(() => {
    const onChange = () => setRoute(parse());
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);
  return route;
}
