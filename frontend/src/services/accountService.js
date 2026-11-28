import { ApiError, apiRequest } from './apiClient';
import { validateUser } from './authService';

export async function getAccounts(signal) {
  const accounts = await apiRequest('/users', { signal });
  if (!Array.isArray(accounts)) throw new ApiError('Hesap listesi alınamadı.');
  return accounts.map(validateUser);
}

function readTemporaryAccount(result) {
  if (typeof result?.temporaryPassword !== 'string' || !result.temporaryPassword) {
    throw new ApiError('Geçici şifre alınamadı.');
  }
  return { user: validateUser(result.user), temporaryPassword: result.temporaryPassword };
}

export async function createAccount(body) {
  return readTemporaryAccount(await apiRequest('/users', { method: 'POST', body }));
}

export async function renewTemporaryPassword(id) {
  return readTemporaryAccount(await apiRequest('/users/' + id + '/temporary-password', { method: 'POST' }));
}

export function deleteAccount(id) {
  return apiRequest('/users/' + id, { method: 'DELETE' });
}
