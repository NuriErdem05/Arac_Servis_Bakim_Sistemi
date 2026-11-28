export const SERVICE_LABELS = { MAINTENANCE: 'Bakım', REPAIR: 'Onarım' };
export { STATUS_LABELS, positiveId, localDate, formatDate } from './appointment.js';
export function toCents(value) {
  const match = String(value ?? '').match(/^(\d+)(?:\.(\d{1,2}))?$/);
  return match ? Number(match[1]) * 100 + Number((match[2] || '').padEnd(2, '0')) : 0;
}
export function formatMoney(value) {
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(Number(value) || 0);
}
export function recordCreateHref(vehicleId, appointmentId) {
  const query = new URLSearchParams();
  if (vehicleId) query.set('vehicleId', vehicleId);
  if (appointmentId) query.set('appointmentId', appointmentId);
  return '#/service-records/new' + (query.size ? '?' + query : '');
}
