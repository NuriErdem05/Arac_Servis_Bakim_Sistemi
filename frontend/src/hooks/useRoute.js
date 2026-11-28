import { useCallback, useEffect, useState } from 'react';

function currentPath() {
  return window.location.hash.slice(1) || '/';
}

export function parseRoute(path) {
  const [pathname, query = ''] = path.split('?');
  const clean = pathname.replace(/\/$/, '') || '/';
  const base = { path, query: new URLSearchParams(query), scope: clean.startsWith('/service/vehicles') ? 'service' : 'owner' };
  const pages = {
    '/': 'home', '/login': 'login', '/register': 'register',
    '/password/change': 'password-change',
    '/dashboard': 'dashboard', '/admin': 'dashboard', '/overview': 'dashboard',
    '/vehicles': 'list', '/vehicles/new': 'create',
    '/appointments': 'appointments', '/appointments/new': 'appointment-create',
    '/service/vehicles': 'list',
    '/service-records': 'records', '/service-records/new': 'record-create',
    '/reports': 'reports',
    '/accounts': 'accounts', '/accounts/new': 'account-create',
  };
  if (Object.hasOwn(pages, clean)) return { ...base, page: pages[clean] };
  const vehicle = clean.match(/^\/(?:service\/)?vehicles\/([1-9]\d*)(?:\/(edit|history))?$/);
  if (vehicle) return { ...base, page: vehicle[2] === 'edit' ? 'edit' : vehicle[2] === 'history' ? 'history' : 'detail', id: vehicle[1] };
  const appointment = clean.match(/^\/appointments\/([1-9]\d*)$/);
  if (appointment) return { ...base, page: 'appointment-detail', id: appointment[1] };
  const record = clean.match(/^\/service-records\/([1-9]\d*)$/);
  if (record) return { ...base, page: 'record-detail', id: record[1] };
  return { ...base, page: 'not-found' };
}

export function useRoute() {
  const [path, setPath] = useState(currentPath);
  useEffect(() => {
    const onChange = () => setPath(currentPath());
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);

  const navigate = useCallback((nextPath, { replace = false } = {}) => {
    if (replace) window.history.replaceState(null, '', '#' + nextPath);
    else window.location.hash = nextPath;
    setPath(nextPath);
  }, []);

  return { route: parseRoute(path), navigate };
}
