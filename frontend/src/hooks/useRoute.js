import { useEffect, useState } from 'react';

function currentPath() {
  return window.location.hash.slice(1) || '/dashboard';
}

export function parseRoute(path) {
  const [pathname, query = ''] = path.split('?');
  const clean = pathname.replace(/\/$/, '') || '/dashboard';
  const base = { path, query: new URLSearchParams(query) };
  const pages = {
    '/dashboard': 'dashboard', '/admin': 'dashboard', '/overview': 'dashboard',
    '/vehicles': 'list', '/vehicles/new': 'create',
    '/appointments': 'appointments', '/appointments/new': 'appointment-create',
  };
  if (pages[clean]) return { ...base, page: pages[clean] };
  const vehicle = clean.match(/^\/vehicles\/([1-9]\d*)(?:\/(edit))?$/);
  if (vehicle) return { ...base, page: vehicle[2] === 'edit' ? 'edit' : 'detail', id: vehicle[1] };
  const appointment = clean.match(/^\/appointments\/([1-9]\d*)$/);
  if (appointment) return { ...base, page: 'appointment-detail', id: appointment[1] };
  return { ...base, page: 'not-found' };
}

export function useRoute() {
  const [path, setPath] = useState(currentPath);
  useEffect(() => {
    const onChange = () => setPath(currentPath());
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);

  function navigate(nextPath) {
    window.location.hash = nextPath;
    setPath(nextPath);
  }

  return { route: parseRoute(path), navigate };
}
