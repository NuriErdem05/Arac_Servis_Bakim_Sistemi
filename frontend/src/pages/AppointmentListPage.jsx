import { useCallback, useState } from 'react';
import { getScopedAppointments } from '../services/appointmentService';
import { useResource } from '../hooks/useResource';
import { STATUS_LABELS } from '../utils/appointment';
import { normalizePlate } from '../utils/vehicle';
import AsyncState from '../components/AsyncState';
import PageHeading from '../components/PageHeading';
import AppointmentTable from '../components/AppointmentTable';
import Icon from '../components/Icon';

const pageCopy = {
  ADMIN: ['Randevular', 'Servis randevularını takip edin, personel atayın ve durumlarını yönetin.'],
  STAFF: ['Atanan randevular', 'Size atanan servis randevularını görüntüleyin.'],
  USER: ['Randevularım', 'Araçlarınız için oluşturduğunuz servis randevularını takip edin.'],
};

export default function AppointmentListPage({ user }) {
  const isAdmin = user.role === 'ADMIN';
  const canCreate = user.role !== 'STAFF';
  const [title, description] = pageCopy[user.role];
  const load = useCallback((signal) => getScopedAppointments(user, signal), [user]);
  const { data, loading, error, retry } = useResource(load);
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [unassigned, setUnassigned] = useState(false);
  const rows = (data || []).filter((item) => (!status || item.status === status) && normalizePlate(item.plate).includes(normalizePlate(search)) && (!unassigned || (item.assignedStaffId == null && ['PENDING', 'CONFIRMED'].includes(item.status)))).sort((a, b) => new Date(b.appointmentDateTime) - new Date(a.appointmentDateTime) || b.id - a.id);
  return <section className="page-content"><PageHeading title={title} description={description}>{canCreate && <a className="button button-primary" href="#/appointments/new"><Icon name="plus" size={17} />Randevu oluştur</a>}</PageHeading>
    {loading || error ? <AsyncState loading={loading} error={error} retry={retry} /> : <>
      <div className={isAdmin ? "collection-toolbar" : "collection-toolbar toolbar-two"}><div className="filter-control"><label htmlFor="appointment-search">Plaka ara</label><input id="appointment-search" type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Örn. 58 ABC 123" /></div><div className="filter-control"><label htmlFor="appointment-status">Durum</label><select id="appointment-status" value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Tüm durumlar</option>{Object.entries(STATUS_LABELS).map(([key, label]) => <option value={key} key={key}>{label}</option>)}</select></div>{isAdmin && <label className="checkbox-control"><input type="checkbox" checked={unassigned} onChange={(event) => setUnassigned(event.target.checked)} />Personel atanmamış aktif randevular</label>}</div>
      <p className="overview-count" aria-live="polite">{rows.length} / {data.length} randevu gösteriliyor</p>
      {rows.length ? <AppointmentTable rows={rows} /> : <div className="empty-state"><h2>Randevu bulunmuyor</h2><p>{data.length ? 'Aramayı veya durum filtresini değiştirin.' : canCreate ? 'Yeni bir servis randevusu oluşturabilirsiniz.' : 'Size atanmış bir randevu olduğunda burada görünür.'}</p>{data.length > 0 && <button className="button button-secondary" type="button" onClick={() => { setStatus(''); setSearch(''); setUnassigned(false); }}>Filtreleri temizle</button>}</div>}
    </>}
  </section>;
}
