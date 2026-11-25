import { apiRequest } from './apiClient';
import { getAppointments } from './appointmentService';

export async function getDashboardData(signal) {
  const [vehicles, appointments] = await Promise.all([
    apiRequest('/vehicles/service', { signal }),
    getAppointments(signal),
  ]);
  if (!Array.isArray(vehicles)) throw new Error('Araç listesi beklenen biçimde alınamadı.');
  return { vehicles, appointments };
}
