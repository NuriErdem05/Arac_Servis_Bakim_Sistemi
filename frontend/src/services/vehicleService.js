import { apiRequest } from './apiClient';
import { getSession } from './authSession';

function getOwnerId() {
  const ownerId = getSession().user?.id;
  if (!Number.isSafeInteger(ownerId) || ownerId <= 0) {
    throw new Error('Kullanıcı oturumu bulunamadı. Lütfen tekrar giriş yapın.');
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

export async function getServiceVehicles(signal) {
  const data = await apiRequest('/vehicles/service', { signal });
  if (!Array.isArray(data)) throw new Error('Servis araçları beklenen biçimde alınamadı.');
  return data;
}

export async function getServiceVehicle(id, signal) {
  vehiclePath(id);
  const data = await apiRequest(`/vehicles/service/${Number(id)}`, { signal });
  if (!data || typeof data !== 'object' || data.id == null) throw new Error('Araç bilgileri beklenen biçimde alınamadı.');
  return data;
}

export function updateServiceVehicle(id, vehicle) {
  vehiclePath(id);
  return apiRequest(`/vehicles/service/${Number(id)}`, { method: 'PUT', body: vehicle });
}

export function getVehicleMaintenancePlan(id, scope, signal) {
  const path = vehiclePath(id);
  const endpoint = scope === 'service'
    ? `/vehicles/service/${Number(id)}/maintenance-plan`
    : `${path}/maintenance-plan?ownerId=${getOwnerId()}`;
  return apiRequest(endpoint, { signal });
}
