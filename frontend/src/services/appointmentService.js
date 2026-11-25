import { apiRequest } from './apiClient';
import { positiveId } from '../utils/appointment';

export async function getAppointments(signal) {
  const data = await apiRequest('/appointments/service', { signal });
  if (!Array.isArray(data)) throw new Error('Randevu listesi beklenen biçimde alınamadı.');
  return data;
}
export async function getAppointment(id, signal) {
  if (!positiveId(id)) throw new Error('Geçersiz randevu numarası.');
  const data = await apiRequest(`/appointments/service/${Number(id)}`, { signal });
  if (!data || data.id == null) throw new Error('Randevu bilgisi alınamadı.');
  return data;
}
export function createAppointment(body) { return apiRequest('/appointments', { method: 'POST', body }); }
export function assignStaff(id, staffId) { return apiRequest(`/appointments/${Number(id)}/staff`, { method: 'PUT', body: { staffId: Number(staffId) } }); }
export function updateStatus(id, status) { return apiRequest(`/appointments/${Number(id)}/status`, { method: 'PUT', body: { status } }); }
