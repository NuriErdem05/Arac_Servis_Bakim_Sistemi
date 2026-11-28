import { ApiError, apiRequest } from './apiClient';
import { ROLE_LABELS } from '../utils/access';

export function validateUser(user) {
  if (!Number.isSafeInteger(user?.id) || user.id <= 0 || !Object.hasOwn(ROLE_LABELS, user.role)
    || typeof user.fullName !== 'string' || !user.fullName.trim()
    || typeof user.email !== 'string' || !user.email.trim()) {
    throw new ApiError('Hesap bilgileri beklenen biçimde alınamadı.');
  }
  return user;
}

export async function login(email, password) {
  const data = await apiRequest('/auth/login', {
    method: 'POST',
    body: { email, password },
    authenticated: false,
  });
  return readSession(data);
}

function readSession(data) {
  if (typeof data?.accessToken !== 'string' || !data.accessToken.trim()) {
    throw new ApiError('Giriş yanıtında oturum anahtarı alınamadı.');
  }
  return { token: data.accessToken, user: validateUser(data.user) };
}

export async function registerAccount(body) {
  return validateUser(await apiRequest('/auth/register', { method: 'POST', body, authenticated: false }));
}

export async function changeInitialPassword(password) {
  return readSession(await apiRequest('/auth/change-password', { method: 'POST', body: { password } }));
}

export async function getCurrentUser(signal) {
  return validateUser(await apiRequest('/auth/me', { signal }));
}
