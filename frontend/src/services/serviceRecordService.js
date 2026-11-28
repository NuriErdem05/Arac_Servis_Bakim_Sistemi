import { apiRequest, ApiError } from './apiClient';
import { positiveId } from '../utils/service';

async function getList(path, signal) {
  const data = await apiRequest(path, { signal });
  if (!Array.isArray(data)) throw new Error('Servis kayıtları beklenen biçimde alınamadı.');
  return data;
}
export function getServiceRecords(signal) { return getList('/service-records/service', signal); }
export function getVehicleHistory(id, signal) {
  if (!positiveId(id)) throw new Error('Geçersiz araç numarası.');
  return getList(`/vehicles/${Number(id)}/service-records/service`, signal);
}
export function getOwnerVehicleHistory(id, ownerId, signal) {
  if (!positiveId(id) || !positiveId(ownerId)) throw new Error('Araç veya kullanıcı bilgisi geçersiz.');
  return getList(`/vehicles/${Number(id)}/service-records?ownerId=${Number(ownerId)}`, signal);
}
export async function getServiceRecord(id, signal) {
  const records = await getServiceRecords(signal);
  const record = records.find((item) => String(item.id) === String(id));
  if (!record) throw new ApiError('Servis kaydı bulunamadı.', 404);
  return record;
}
export function createServiceRecord(body) { return apiRequest('/service-records/service', { method: 'POST', body }); }
