import { useState } from 'react';
import { getServiceRecords } from '../services/serviceRecordService';
import { useResource } from '../hooks/useResource';
import { SERVICE_LABELS } from '../utils/service';
import { normalizePlate } from '../utils/vehicle';
import AsyncState from '../components/AsyncState';
import PageHeading from '../components/PageHeading';
import ServiceRecordTable from '../components/ServiceRecordTable';
import Icon from '../components/Icon';

export default function ServiceRecordListPage() {
  const { data, loading, error, retry } = useResource(getServiceRecords);
  const [search, setSearch] = useState('');
  const [type, setType] = useState('');
  const rows = (data || []).filter((item) => (!type || item.serviceType === type) && normalizePlate(item.plate).includes(normalizePlate(search))).sort((a, b) => b.serviceDate.localeCompare(a.serviceDate) || b.id - a.id);
  return <section className="page-content"><PageHeading eyebrow="Servis yönetimi" title="Servis kayıtları" description="Tamamlanan bakım ve onarım işlemlerini görüntüleyin."><button className="button button-secondary" type="button" disabled={loading} onClick={retry}><Icon name="refresh" size={16} />Yenile</button><a className="button button-secondary" href="#/service/vehicles">Servis araçları</a><a className="button button-primary" href="#/service-records/new"><Icon name="plus" size={17} />Servis kaydı oluştur</a></PageHeading>
    {loading || error ? <AsyncState loading={loading} error={error} retry={retry} /> : <>
      <div className="collection-toolbar toolbar-two"><div className="filter-control"><label htmlFor="record-search">Plaka ara</label><input id="record-search" type="search" value={search} onChange={(event) => setSearch(event.target.value)} /></div><div className="filter-control"><label htmlFor="record-type">İşlem türü</label><select id="record-type" value={type} onChange={(event) => setType(event.target.value)}><option value="">Tüm işlemler</option>{Object.entries(SERVICE_LABELS).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></div></div>
      <p className="overview-count" aria-live="polite">{rows.length} / {data.length} servis kaydı gösteriliyor</p>
      {rows.length ? <ServiceRecordTable rows={rows} /> : <div className="empty-state"><h2>Servis kaydı bulunmuyor</h2><p>{data.length ? 'Aramayı veya işlem filtresini değiştirin.' : 'Bakım veya onarım tamamlandığında yeni bir servis kaydı oluşturabilirsiniz.'}</p>{data.length > 0 && <button className="button button-secondary" type="button" onClick={() => { setSearch(''); setType(''); }}>Filtreleri temizle</button>}</div>}
    </>}
  </section>;
}
