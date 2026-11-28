import { apiRequest } from './apiClient';
import { getAppointments } from './appointmentService';
import { getServiceRecords } from './serviceRecordService';

export async function getDashboardData(signal) {
  const [vehicles, appointments] = await Promise.all([
    apiRequest('/vehicles/service', { signal }),
    getAppointments(signal),
  ]);
  if (!Array.isArray(vehicles)) throw new Error('Araç listesi beklenen biçimde alınamadı.');
  return { vehicles, appointments };
}

export async function getReportData(signal) {
  const [appointments, records] = await Promise.all([getAppointments(signal), getServiceRecords(signal)]);
  return { appointments, records };
}
