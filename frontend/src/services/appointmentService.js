import { ApiError, apiRequest } from './apiClient';
import { positiveId } from '../utils/appointment';

export async function getAppointments(signal) {
  const data = await apiRequest('/appointments/service', { signal });
  if (!Array.isArray(data)) throw new Error('Randevu listesi beklenen biçimde alınamadı.');
  return data;
}
export async function getScopedAppointments(user, signal) {
  if (user.role === 'ADMIN') return getAppointments(signal);

  const path = user.role === 'STAFF'
    ? `/appointments/assigned?staffId=${user.id}`
    : `/appointments?ownerId=${user.id}`;
  const data = await apiRequest(path, { signal });
  if (!Array.isArray(data)) throw new Error('Randevu listesi beklenen biçimde alınamadı.');
  return data;
}

export async function getAppointment(id, user, signal) {
  if (!positiveId(id)) throw new Error('Geçersiz randevu numarası.');
  if (user.role === 'STAFF') {
    const assignments = await getScopedAppointments(user, signal);
    const appointment = assignments.find((item) => item.id === Number(id));
    if (!appointment) throw new ApiError('Atanan randevu bulunamadı.', 404);
    return appointment;
  }

  const path = user.role === 'ADMIN'
    ? `/appointments/service/${Number(id)}`
    : `/appointments/${Number(id)}?ownerId=${user.id}`;
  const data = await apiRequest(path, { signal });
  if (!data || data.id == null) throw new Error('Randevu bilgisi alınamadı.');
  return data;
}
export function createAppointment(body) { return apiRequest('/appointments', { method: 'POST', body }); }
export function assignStaff(id, staffId) { return apiRequest(`/appointments/${Number(id)}/staff`, { method: 'PUT', body: { staffId: Number(staffId) } }); }
export function updateStatus(id, status) { return apiRequest(`/appointments/${Number(id)}/status`, { method: 'PUT', body: { status } }); }
