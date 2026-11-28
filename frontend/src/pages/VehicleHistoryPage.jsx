import Icon from '../components/Icon';
import { useCallback } from 'react';
import { getVehicle, getServiceVehicle } from '../services/vehicleService';
import { getVehicleHistory, getOwnerVehicleHistory, getStaffVehicleHistory } from '../services/serviceRecordService';
import { useResource } from '../hooks/useResource';
import { recordCreateHref } from '../utils/service';
import AsyncState from '../components/AsyncState';
import PageHeading from '../components/PageHeading';
import ServiceRecordTable from '../components/ServiceRecordTable';

export default function VehicleHistoryPage({ id, scope = 'owner', user }) {
  const service = scope === 'service';
  const isStaff = user.role === 'STAFF';
  const basePath = service ? '/service/vehicles' : '/vehicles';
  const loader = useCallback(async (signal) => {
    if (isStaff) {
      return { records: await getStaffVehicleHistory(id, user.id, signal) };
    }
    const vehicle = await (scope === 'service' ? getServiceVehicle : getVehicle)(id, signal);
    const records = scope === 'service' ? await getVehicleHistory(id, signal) : await getOwnerVehicleHistory(id, user.id, signal);
    return { vehicle, records };
  }, [id, scope, isStaff, user.id]);
  const { data, loading, error, retry } = useResource(loader);
  return <section className="page-content"><a className="back-link" href={isStaff ? '#/appointments' : '#' + basePath + '/' + id}><Icon name="back" size={17} />{isStaff ? 'Randevulara dön' : 'Araç detayına dön'}</a><PageHeading eyebrow="Araç geçmişi" title="Bakım ve onarım geçmişi" description={data?.vehicle ? `${data.vehicle.plate} · ${data.vehicle.brand} ${data.vehicle.model}` : 'Aracın geçmiş servis işlemleri.'}>{service && !isStaff && <a className="button button-primary" href={recordCreateHref(id)}>Servis kaydı oluştur</a>}</PageHeading>
    {loading || error ? <AsyncState loading={loading} error={error} retry={retry} /> : data && (data.records.length ? <ServiceRecordTable vehicleBasePath={isStaff ? null : basePath} showDetails={user.role === 'ADMIN'} rows={[...data.records].sort((a, b) => b.serviceDate.localeCompare(a.serviceDate) || b.id - a.id)} /> : <div className="empty-state"><h2>Henüz servis kaydı bulunmuyor</h2><p>Bu araç için oluşturulan bakım ve onarım kayıtları burada görünür.</p></div>)}
  </section>;
}
