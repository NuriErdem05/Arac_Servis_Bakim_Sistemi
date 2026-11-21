import { formatNumber, fuelLabel } from '../utils/vehicle';

export default function VehicleCard({ vehicle }) {
  return (
    <article className="vehicle-card">
      <div className="record-plate"><span className="record-mobile-label">Plaka</span><a href={`#/vehicles/${vehicle.id}`}>{vehicle.plate}</a></div>
      <div className="record-vehicle"><h2>{vehicle.brand} {vehicle.model}</h2><p>{vehicle.engineInfo || 'Motor bilgisi belirtilmedi'}</p></div>
      <div className="record-meta"><span className="record-mobile-label">Yıl / Yakıt</span><span>{vehicle.modelYear ?? '—'}</span><span>{fuelLabel(vehicle.fuelType)}</span></div>
      <div className="record-mileage"><span className="record-mobile-label">Kilometre</span><strong>{formatNumber(vehicle.currentMileage)}</strong><span> km</span></div>
      <div className="card-actions"><a href={`#/vehicles/${vehicle.id}`} aria-label={`${vehicle.plate} detaylarını gör`}>Detay</a><a href={`#/vehicles/${vehicle.id}/edit`} aria-label={`${vehicle.plate} aracını düzenle`}>Düzenle</a></div>
    </article>
  );
}
