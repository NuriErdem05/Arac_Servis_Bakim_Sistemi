const storageKey = 'vehicle-service.access-token';
const listeners = new Set();

function readToken() {
  try {
    return window.sessionStorage.getItem(storageKey);
  } catch {
    return null;
  }
}

let session = { token: readToken(), user: null, notice: '' };

export function getSession() {
  return session;
}

export function subscribeToSession(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function setSession(token, user = null, notice = '') {
  session = { token, user, notice };
  try {
    if (token) window.sessionStorage.setItem(storageKey, token);
    else window.sessionStorage.removeItem(storageKey);
  } catch {
    // Oturum, tarayıcı depolaması kapalıysa bu sayfada bellekte tutulur.
  }
  listeners.forEach((listener) => listener());
}

export function expireSession(token) {
  if (token === session.token) {
    setSession(null, null, 'Oturumunuz sona erdi. Devam etmek için tekrar giriş yapın.');
  }
}
