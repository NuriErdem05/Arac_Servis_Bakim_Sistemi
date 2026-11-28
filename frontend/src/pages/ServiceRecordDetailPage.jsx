import Icon from '../components/Icon';
import { useCallback } from 'react';
import { getServiceRecord } from '../services/serviceRecordService';
import { useResource } from '../hooks/useResource';
import { formatDate, formatMoney, SERVICE_LABELS } from '../utils/service';
import { formatNumber } from '../utils/vehicle';
import AsyncState from '../components/AsyncState';
import PageHeading from '../components/PageHeading';

export default function ServiceRecordDetailPage({ id }) {
  const loader = useCallback((signal) => getServiceRecord(id, signal), [id]);
  const { data: item, loading, error, retry } = useResource(loader);
  return <section className="page-content"><a className="back-link" href="#/service-records"><Icon name="back" size={17} />Servis kayıtlarına dön</a><PageHeading eyebrow="Servis yönetimi" title="Servis kaydı detayı" description="Yapılan işlem, kullanılan parçalar ve maliyet bilgisi." />
    {loading || error ? <AsyncState loading={loading} error={error} retry={retry} /> : item && <>
      <section className="detail-panel"><h2 className="plate-text">{item.plate}</h2><dl className="detail-specs">{[['Kayıt numarası', item.id], ['İşlem tarihi', formatDate(item.serviceDate)], ['İşlem türü', SERVICE_LABELS[item.serviceType] || item.serviceType], ['İşlem kilometresi', formatNumber(item.mileage) + ' km'], ['Personel', item.performedByName || 'Belirtilmedi'], ['Toplam maliyet', formatMoney(item.cost)]].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl></section>
      <section className="detail-panel"><h2>İşlem açıklaması</h2><p className="preserve-lines">{item.description}</p><h3>Değiştirilen parçalar</h3><p className="preserve-lines">{item.replacedParts || 'Parça değişimi belirtilmedi.'}</p></section>
      <div className="button-row section-actions"><a className="button button-secondary" href={'#/service/vehicles/' + item.vehicleId}>Araç detayı</a><a className="button button-secondary" href={'#/service/vehicles/' + item.vehicleId + '/history'}>Aracın bakım geçmişi</a>{item.appointmentId && <a className="button button-secondary" href={'#/appointments/' + item.appointmentId}>Bağlı randevu</a>}</div>
    </>}
  </section>;
}
