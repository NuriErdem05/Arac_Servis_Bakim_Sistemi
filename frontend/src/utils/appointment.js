export const STATUS_LABELS = { PENDING: 'Onay bekliyor', CONFIRMED: 'Onaylandı', CANCELLED: 'İptal edildi', COMPLETED: 'Tamamlandı' };
export function positiveId(value) { return Number.isSafeInteger(Number(value)) && Number(value) > 0; }
export function localDate(value = new Date()) {
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`;
}
export function formatDate(value, withTime = false) {
  if (!value) return '—';
  const date = new Date(value.length === 10 ? `${value}T00:00:00` : value);
  if (!Number.isFinite(date.getTime())) return '—';
  return new Intl.DateTimeFormat('tr-TR', { dateStyle: 'short', ...(withTime ? { timeStyle: 'short' } : {}) }).format(date);
}
