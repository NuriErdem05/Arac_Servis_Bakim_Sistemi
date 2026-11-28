import { useVehicles } from '../hooks/useVehicles';
import { formatNumber, fuelLabel } from '../utils/vehicle';
import AsyncState from '../components/AsyncState';
import Icon from '../components/Icon';

export default function VehicleDetailPage({ id, scope = 'owner' }) {
  const service = scope === 'service';
  const basePath = service ? '/service/vehicles' : '/vehicles';
  const { data: vehicle, loading, error, retry } = useVehicles(id, scope);

  return (
    <section className="page-content">
      <a className="back-link" href={'#' + basePath}><Icon name="back" size={17} />{service ? 'Servis araçlarına dön' : 'Araçlarıma dön'}</a>
      <div className="page-heading">
        <div><h1 id="page-title" tabIndex={-1}>Araç detayı</h1><p className="page-description">Aracın kayıtlı kimlik ve teknik bilgileri.</p></div>
        {vehicle && !loading && !error && <a className="button button-primary" href={'#' + basePath + '/' + id + '/edit'}>Bilgileri düzenle</a>}
      </div>
      {loading || error ? <AsyncState loading={loading} error={error} retry={retry} /> : vehicle && <>
        <div className="detail-hero">
          <div className="vehicle-identification">
            <span className="detail-plate">{vehicle.plate}</span>
            <h2>{vehicle.brand} {vehicle.model}</h2>
            <p>{vehicle.modelYear ?? '—'} model · {fuelLabel(vehicle.fuelType)}</p>
          </div>
          <div className="mileage-panel"><span>Güncel kilometre</span><strong>{formatNumber(vehicle.currentMileage)} <small>km</small></strong></div>
        </div>
        <section className="detail-panel">
          <h2>Kayıt bilgileri</h2>
          <dl className="detail-specs">{[
            ['Plaka', vehicle.plate], ['Marka', vehicle.brand], ['Model', vehicle.model],
            ['Model yılı', vehicle.modelYear ?? '—'], ['Yakıt türü', fuelLabel(vehicle.fuelType)],
            ['Motor bilgisi', vehicle.engineInfo || 'Belirtilmedi'],
          ].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl>
        </section>
        <div className="quick-links vehicle-quick-links" aria-label="Araç işlemleri"><a href={'#' + basePath + '/' + id + '/history'}><span className="quick-link-icon"><Icon name="clock" size={21} /></span><span className="quick-link-copy"><strong>Bakım ve onarım geçmişi</strong><small>Araca ait geçmiş servis kayıtlarını görüntüleyin.</small></span><Icon name="arrow" size={18} /></a><a href={service ? '#/service-records/new?vehicleId=' + id : '#/appointments/new?vehicleId=' + id}><span className="quick-link-icon"><Icon name="plus" size={21} /></span><span className="quick-link-copy"><strong>{service ? 'Servis kaydı oluştur' : 'Randevu oluştur'}</strong><small>{service ? 'Yapılan bakım veya onarım işlemini kaydedin.' : 'Araç için yeni bir servis randevusu oluşturun.'}</small></span><Icon name="arrow" size={18} /></a></div>
      </>}
    </section>
  );
}
