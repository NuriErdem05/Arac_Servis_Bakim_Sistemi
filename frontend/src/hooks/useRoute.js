import { useEffect, useState } from 'react';

function currentPath() {
  return window.location.hash.slice(1).replace(/\/$/, '') || '/vehicles';
}

export function parseRoute(path) {
  if (path === '/vehicles') return { page: 'list', path };
  if (path === '/vehicles/new') return { page: 'create', path };
  const match = path.match(/^\/vehicles\/(\d+)(\/edit)?$/);
  if (match) return { page: match[2] ? 'edit' : 'detail', id: match[1], path };
  return { page: 'not-found', path };
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
