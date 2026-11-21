import { useVehicles } from '../hooks/useVehicles';
import { updateVehicle } from '../services/vehicleService';
import VehicleForm from '../components/VehicleForm';
import AsyncState from '../components/AsyncState';
import Icon from '../components/Icon';

export default function VehicleEditPage({ id, onSaved }) {
  const { data: vehicle, loading, error, retry } = useVehicles(id);

  async function handleSave(values) {
    await updateVehicle(id, values);
    onSaved('Araç bilgileri güncellendi.', '/vehicles/' + id);
  }

  return (
    <section className="page-content form-page">
      <a className="back-link" href={'#/vehicles/' + id}><Icon name="back" size={17} />Araç detayına dön</a>
      <div className="page-heading">
        <div>
          <h1 id="page-title" tabIndex={-1}>Aracı düzenle</h1>
          <p className="page-description">{vehicle && !loading && !error ? vehicle.plate + ' plakalı aracın bilgilerini güncelleyin.' : 'Kayıtlı araç bilgilerini güncelleyin.'}</p>
        </div>
      </div>
      {loading || error ? <AsyncState loading={loading} error={error} retry={retry} /> : vehicle && <div className="form-layout"><VehicleForm key={vehicle.id} initialVehicle={vehicle} onSave={handleSave} cancelHref={'#/vehicles/' + id} /></div>}
    </section>
  );
}
