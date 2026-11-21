import { apiRequest } from './apiClient';

function getOwnerId() {
  const ownerId = Number(import.meta.env.VITE_DEV_OWNER_ID);
  if (!Number.isSafeInteger(ownerId) || ownerId <= 0) {
    throw new Error('İşlem yapılacak kullanıcı bulunamadı. Kullanıcı ayarını kontrol edin.');
  }
  return ownerId;
}

function vehiclePath(id) {
  const number = Number(id);
  if (!Number.isSafeInteger(number) || number <= 0) {
    throw new Error('Geçersiz araç numarası.');
  }
  return `/vehicles/${number}`;
}

export async function getVehicles(signal) {
  const data = await apiRequest(`/vehicles?ownerId=${getOwnerId()}`, { signal });
  if (!Array.isArray(data)) throw new Error('Araç listesi beklenen biçimde alınamadı.');
  return data;
}

export async function getVehicle(id, signal) {
  const data = await apiRequest(`${vehiclePath(id)}?ownerId=${getOwnerId()}`, { signal });
  if (!data || typeof data !== 'object' || data.id == null) {
    throw new Error('Araç bilgileri beklenen biçimde alınamadı.');
  }
  return data;
}

export function createVehicle(vehicle) {
  return apiRequest('/vehicles', {
    method: 'POST',
    body: { ...vehicle, ownerId: getOwnerId() },
  });
}

export function updateVehicle(id, vehicle) {
  return apiRequest(`${vehiclePath(id)}?ownerId=${getOwnerId()}`, {
    method: 'PUT',
    body: vehicle,
  });
}
