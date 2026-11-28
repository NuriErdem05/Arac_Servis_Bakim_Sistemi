import Icon from '../components/Icon';
import { useResource } from '../hooks/useResource';
import { getServiceVehicles, getVehicles } from '../services/vehicleService';
import { createAppointment } from '../services/appointmentService';
import { positiveId } from '../utils/appointment';
import PageHeading from '../components/PageHeading';
import AsyncState from '../components/AsyncState';
import EntryForm from '../components/EntryForm';

export default function AppointmentCreatePage({ query, onSaved, user }) {
  const isAdmin = user.role === 'ADMIN';
  const { data: vehicles, loading, error, retry } = useResource(isAdmin ? getServiceVehicles : getVehicles);
  function validate(values) {
    const errors = {};
    if (!vehicles.some((item) => String(item.id) === values.vehicleId)) errors.vehicleId = 'Kayıtlı bir araç seçin.';
    const time = new Date(values.appointmentDateTime);
    if (!values.appointmentDateTime || !Number.isFinite(time.getTime()) || time <= new Date()) errors.appointmentDateTime = 'Gelecekte bir tarih ve saat seçin.';
    else if (time.getMinutes() % 30 !== 0 || time.getSeconds() !== 0) errors.appointmentDateTime = '10:00 veya 10:30 gibi, 30 dakikalık bir saat seçin.';
    if (!values.requestDescription.trim()) errors.requestDescription = 'Talebinizi açıklayın.';
    else if (values.requestDescription.trim().length > 1000) errors.requestDescription = 'Açıklama en fazla 1000 karakter olabilir.';
    return errors;
  }
  async function save(values) {
    const vehicle = vehicles.find((item) => String(item.id) === values.vehicleId);
    if (!positiveId(vehicle?.ownerId)) throw new Error('Aracın sahip bilgisi alınamadı. Araç kaydını kontrol edin.');
    const result = await createAppointment({ vehicleId: vehicle.id, ownerId: vehicle.ownerId, appointmentDateTime: values.appointmentDateTime.length === 16 ? values.appointmentDateTime + ':00' : values.appointmentDateTime, requestDescription: values.requestDescription.trim() });
    onSaved('Randevu oluşturuldu. Durumu: Onay bekliyor.', '/appointments/' + result.id);
  }
  return <section className="page-content form-page"><a className="back-link" href="#/appointments"><Icon name="back" size={17} />Randevulara dön</a><PageHeading title="Randevu oluştur" description="Araç için tarih, saat ve servis talebini belirtin." />
    {loading || error ? <AsyncState loading={loading} error={error} retry={retry} /> : vehicles.length ? <EntryForm initialValues={{ vehicleId: query.get('vehicleId') || '', appointmentDateTime: '', requestDescription: '' }} validate={validate} onSave={save} cancelHref="#/appointments" submitLabel="Randevuyu oluştur" fields={[
      { name: 'vehicleId', label: 'Araç', type: 'select', options: vehicles.map((item) => ({ value: item.id, label: `${item.plate} · ${item.brand} ${item.model}` })) },
      { name: 'appointmentDateTime', label: 'Tarih ve saat', type: 'datetime-local', step: 1800, hint: 'Saatleri 30 dakikalık dilimlerden seçin. Aynı saat için tek aktif randevu alınır.' },
      { name: 'requestDescription', label: 'Talep açıklaması', type: 'textarea', maxLength: 1000, wide: true },
    ]}><h2>Randevu bilgileri</h2><p>Yeni randevu, personel ataması ve onay için servis listesine eklenir.</p></EntryForm> : <div className="empty-state"><h2>Kayıtlı araç bulunmuyor</h2><p>Randevu oluşturmak için kayıtlı bir araç gerekiyor.</p>{!isAdmin && <a className="button button-primary" href="#/vehicles/new">Araç ekle</a>}</div>}
  </section>;
}
