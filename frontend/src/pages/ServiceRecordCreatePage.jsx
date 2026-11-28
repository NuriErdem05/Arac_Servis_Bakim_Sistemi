import Icon from '../components/Icon';
import { useResource } from '../hooks/useResource';
import { getServiceVehicles } from '../services/vehicleService';
import { getAppointments } from '../services/appointmentService';
import { createServiceRecord } from '../services/serviceRecordService';
import { localDate, positiveId, SERVICE_LABELS } from '../utils/service';
import { formatNumber } from '../utils/vehicle';
import AsyncState from '../components/AsyncState';
import PageHeading from '../components/PageHeading';
import EntryForm from '../components/EntryForm';

async function loadOptions(signal) {
  const [vehicles, appointments] = await Promise.all([getServiceVehicles(signal), getAppointments(signal)]);
  return { vehicles, appointments };
}

export default function ServiceRecordCreatePage({ query, onSaved }) {
  const { data, loading, error, retry } = useResource(loadOptions);
  const getAppointment = (values) => data.appointments.find((item) => String(item.id) === values.appointmentId);
  function validate(values) {
    const errors = {};
    const vehicle = data.vehicles.find((item) => String(item.id) === values.vehicleId);
    const appointment = getAppointment(values);
    if (!vehicle) errors.vehicleId = 'Kayıtlı bir araç seçin.';
    if (values.appointmentId && (!appointment || String(appointment.vehicleId) !== values.vehicleId || appointment.status !== 'CONFIRMED' || !positiveId(appointment.assignedStaffId))) errors.appointmentId = 'Seçili araca ait, personel atanmış ve onaylanmış bir randevu seçin.';
    if (!positiveId(appointment?.assignedStaffId ?? values.performedById)) errors.performedById = 'İşlemi yapan personelin numarasını girin.';
    if (!SERVICE_LABELS[values.serviceType]) errors.serviceType = 'İşlem türünü seçin.';
    if (!/^\d{4}-\d{2}-\d{2}$/.test(values.serviceDate) || !Number.isFinite(new Date(values.serviceDate).getTime()) || values.serviceDate > localDate()) errors.serviceDate = 'Bugün veya geçmiş bir işlem tarihi seçin.';
    if (!values.mileage.trim() || !Number.isSafeInteger(Number(values.mileage)) || Number(values.mileage) < 0 || Number(values.mileage) > 2147483647) errors.mileage = '0 ile 2147483647 arasında tam sayı bir kilometre girin.';
    else if (values.serviceDate === localDate() && vehicle && Number(values.mileage) < vehicle.currentMileage) errors.mileage = `Bugünkü işlem kilometresi ${formatNumber(vehicle.currentMileage)} km değerinden düşük olamaz.`;
    if (!values.description.trim()) errors.description = 'Yapılan işlemi açıklayın.';
    else if (values.description.length > 2000) errors.description = 'Açıklama en fazla 2000 karakter olabilir.';
    if (values.replacedParts.length > 2000) errors.replacedParts = 'Parça bilgisi en fazla 2000 karakter olabilir.';
    if (!/^\d{1,10}([.,]\d{1,2})?$/.test(values.cost.trim())) errors.cost = 'Negatif olmayan, en fazla 2 ondalıklı bir maliyet girin.';
    return errors;
  }
  async function save(values) {
    const appointment = getAppointment(values);
    const result = await createServiceRecord({ vehicleId: Number(values.vehicleId), appointmentId: values.appointmentId ? Number(values.appointmentId) : null, performedById: Number(appointment?.assignedStaffId ?? values.performedById), serviceType: values.serviceType, serviceDate: values.serviceDate, mileage: Number(values.mileage), replacedParts: values.replacedParts.trim(), description: values.description.trim(), cost: values.cost.trim().replace(',', '.') });
    onSaved(appointment ? 'Servis kaydı oluşturuldu ve randevu tamamlandı.' : 'Servis kaydı oluşturuldu.', '/service-records/' + result.id);
  }
  function changeValues(values, changedField) {
    if (changedField === 'vehicleId') {
      const selected = data.vehicles.find((item) => String(item.id) === values.vehicleId);
      return { ...values, appointmentId: '', performedById: '', mileage: selected ? String(selected.currentMileage) : '' };
    }
    if (changedField === 'appointmentId') {
      const selected = getAppointment(values);
      return { ...values, performedById: selected ? String(selected.assignedStaffId) : '' };
    }
    return values;
  }
  function fields(values) {
    const vehicle = data.vehicles.find((item) => String(item.id) === values.vehicleId);
    const appointment = getAppointment(values);
    const options = data.appointments.filter((item) => item.status === 'CONFIRMED' && item.assignedStaffId != null && String(item.vehicleId) === values.vehicleId);
    return [
      { name: 'vehicleId', label: 'Araç', type: 'select', options: data.vehicles.map((item) => ({ value: item.id, label: `${item.plate} · ${item.brand} ${item.model}` })) },
      { name: 'appointmentId', label: 'Bağlı randevu', type: 'select', optional: true, placeholder: 'Randevusuz işlem', options: options.map((item) => ({ value: item.id, label: `#${item.id} · ${item.plate} · ${item.appointmentDateTime.replace('T', ' ')}` })), hint: 'Araca ait onaylı randevular gösterilir. Kayıt oluşturulunca seçili randevu tamamlanır.' },
      { name: 'performedById', label: 'İşlemi yapan personel numarası', type: 'number', min: 1, step: 1, ...(appointment ? { value: String(appointment.assignedStaffId ?? ''), readOnly: true } : {}), hint: appointment ? `Randevu personeli: ${appointment.assignedStaffName || 'Atanmadı'}` : 'İşlemi yapan kayıtlı personelin kullanıcı numarası.' },
      { name: 'serviceType', label: 'İşlem türü', type: 'select', options: Object.entries(SERVICE_LABELS).map(([value, label]) => ({ value, label })) },
      { name: 'serviceDate', label: 'İşlem tarihi', type: 'date', max: localDate() },
      { name: 'mileage', label: 'İşlem kilometresi', type: 'number', min: 0, max: 2147483647, step: 1, hint: vehicle ? `Aracın güncel kilometresi: ${formatNumber(vehicle.currentMileage)} km.` : 'İşlem tarihindeki kilometreyi girin.' },
      { name: 'cost', label: 'Toplam işlem maliyeti (TL)', placeholder: 'Örn. 1500,00' },
      { name: 'replacedParts', label: 'Değiştirilen parçalar', type: 'textarea', optional: true, maxLength: 2000, wide: true },
      { name: 'description', label: 'Yapılan işlem', type: 'textarea', maxLength: 2000, wide: true },
    ];
  }
  const preselectedAppointment = data?.appointments.find((item) => String(item.id) === query.get('appointmentId'));
  const initialVehicleId = query.get('vehicleId') || (preselectedAppointment ? String(preselectedAppointment.vehicleId) : '');
  const vehicle = data?.vehicles.find((item) => String(item.id) === initialVehicleId);
  return <section className="page-content form-page"><a className="back-link" href="#/service-records"><Icon name="back" size={17} />Servis kayıtlarına dön</a><PageHeading eyebrow="Servis yönetimi" title="Servis kaydı oluştur" description="Tamamlanan bakım veya onarımın bilgilerini kaydedin." />
    {loading || error ? <AsyncState loading={loading} error={error} retry={retry} /> : data.vehicles.length ? <EntryForm initialValues={{ vehicleId: initialVehicleId, appointmentId: query.get('appointmentId') || '', performedById: preselectedAppointment ? String(preselectedAppointment.assignedStaffId ?? '') : import.meta.env.VITE_DEV_STAFF_ID || '', serviceType: 'MAINTENANCE', serviceDate: localDate(), mileage: vehicle ? String(vehicle.currentMileage) : '', cost: '', replacedParts: '', description: '' }} fields={fields} validate={validate} onSave={save} onValuesChange={changeValues} cancelHref="#/service-records" submitLabel="Servis kaydını oluştur"><h2>İşlem bilgileri</h2><p>Randevulu veya randevusuz bir servis işlemi kaydedebilirsiniz.</p></EntryForm> : <div className="empty-state"><h2>Önce araç ekleyin</h2><p>Servis kaydı için kayıtlı bir araç gerekiyor.</p><a className="button button-primary" href="#/vehicles/new">Araç ekle</a></div>}
  </section>;
}
