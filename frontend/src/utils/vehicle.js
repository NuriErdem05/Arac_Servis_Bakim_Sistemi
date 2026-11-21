export const FUEL_TYPES = [
  { value: 'GASOLINE', label: 'Benzin' },
  { value: 'DIESEL', label: 'Dizel' },
  { value: 'LPG', label: 'LPG' },
  { value: 'HYBRID', label: 'Hibrit' },
  { value: 'ELECTRIC', label: 'Elektrik' },
  { value: 'OTHER', label: 'Diğer' },
];

export function fuelLabel(value) {
  return FUEL_TYPES.find((fuel) => fuel.value === value)?.label || 'Belirtilmedi';
}

export function formatNumber(value) {
  if (value === null || value === undefined || value === '') return '—';
  const number = Number(value);
  return Number.isFinite(number) ? number.toLocaleString('tr-TR', { maximumFractionDigits: 0 }) : '—';
}

export function normalizePlate(value = '') {
  return String(value).replace(/\s/g, '').toUpperCase();
}

export function makeFormValues(vehicle) {
  return {
    plate: vehicle?.plate ?? '',
    brand: vehicle?.brand ?? '',
    model: vehicle?.model ?? '',
    modelYear: vehicle?.modelYear == null ? '' : String(vehicle.modelYear),
    fuelType: vehicle?.fuelType ?? '',
    engineInfo: vehicle?.engineInfo ?? '',
    currentMileage: vehicle?.currentMileage == null ? '' : String(vehicle.currentMileage),
  };
}

export function validateVehicle(values, initialVehicle) {
  const errors = {};
  const plate = normalizePlate(values.plate);
  if (!plate) errors.plate = 'Plaka girin.';
  else if (plate.length > 20 || !/^[A-Z0-9]+$/.test(plate)) errors.plate = 'Plaka en fazla 20 karakter olmalı; harf ve rakam kullanın.';
  if (!values.brand.trim()) errors.brand = 'Marka girin.';
  else if (values.brand.trim().length > 100) errors.brand = 'Marka en fazla 100 karakter olabilir.';
  if (!values.model.trim()) errors.model = 'Model girin.';
  else if (values.model.trim().length > 100) errors.model = 'Model en fazla 100 karakter olabilir.';
  const year = Number(values.modelYear);
  const maxYear = new Date().getFullYear() + 1;
  if (!values.modelYear.trim() || !Number.isSafeInteger(year) || year < 1886 || year > maxYear) {
    errors.modelYear = `Model yılı 1886 ile ${maxYear} arasında olmalı.`;
  }
  if (values.engineInfo.trim().length > 150) errors.engineInfo = 'Motor bilgisi en fazla 150 karakter olabilir.';
  if (!FUEL_TYPES.some((fuel) => fuel.value === values.fuelType)) {
    errors.fuelType = 'Yakıt türünü seçin.';
  }
  const mileage = Number(values.currentMileage);
  if (!values.currentMileage.trim() || !Number.isSafeInteger(mileage) || mileage < 0) {
    errors.currentMileage = 'Sıfır veya daha büyük, tam sayı bir kilometre girin.';
  } else if (initialVehicle?.currentMileage != null && mileage < Number(initialVehicle.currentMileage)) {
    errors.currentMileage = `Kilometre ${formatNumber(initialVehicle.currentMileage)} km değerinden düşük olamaz.`;
  }
  return errors;
}

export function buildVehiclePayload(values) {
  return {
    plate: normalizePlate(values.plate),
    brand: values.brand.trim(),
    model: values.model.trim(),
    modelYear: Number(values.modelYear),
    fuelType: values.fuelType,
    engineInfo: values.engineInfo.trim(),
    currentMileage: Number(values.currentMileage),
  };
}

export function filterVehicles(vehicles, search, brand, sort) {
  const query = normalizePlate(search);
  const result = vehicles.filter((vehicle) =>
    normalizePlate(vehicle.plate).includes(query) && (!brand || vehicle.brand === brand));
  return result.sort((a, b) => {
    if (sort === 'plate') return String(a.plate).localeCompare(String(b.plate), 'tr');
    if (sort === 'mileage') return Number(a.currentMileage ?? 0) - Number(b.currentMileage ?? 0);
    return Number(b.id) - Number(a.id);
  });
}
