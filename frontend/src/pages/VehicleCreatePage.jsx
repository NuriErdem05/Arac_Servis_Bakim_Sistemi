import VehicleForm from '../components/VehicleForm';
import Icon from '../components/Icon';
import { createVehicle } from '../services/vehicleService';

export default function VehicleCreatePage({ onSaved }) {
  async function handleSave(values) {
    await createVehicle(values);
    onSaved('Araç kaydı oluşturuldu.', '/vehicles');
  }

  return (
    <section className="page-content form-page">
      <a className="back-link" href="#/vehicles"><Icon name="back" size={17} />Araçlarıma dön</a>
      <div className="page-heading">
        <div><h1 id="page-title" tabIndex={-1}>Yeni araç ekle</h1><p className="page-description">Aracın kimlik ve teknik bilgilerini doldurun.</p></div>
      </div>
      <div className="form-layout"><VehicleForm onSave={handleSave} cancelHref="#/vehicles" /></div>
    </section>
  );
}
