export const MIN_PASSWORD_LENGTH = 8;
export const MAX_PASSWORD_BYTES = 72;

export function newPasswordError(password, confirmation) {
  if (password !== confirmation) return 'Şifreler eşleşmiyor.';
  if (!password.trim() || password.length < MIN_PASSWORD_LENGTH
    || new TextEncoder().encode(password).length > MAX_PASSWORD_BYTES) {
    return `Şifre en az ${MIN_PASSWORD_LENGTH} karakter, en fazla ${MAX_PASSWORD_BYTES} bayt olmalı.`;
  }
  return '';
}
