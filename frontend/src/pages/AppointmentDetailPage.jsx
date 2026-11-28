import Icon from '../components/Icon';
import { useCallback, useRef, useState } from 'react';
import { getAppointment, assignStaff, updateStatus } from '../services/appointmentService';
import { useResource } from '../hooks/useResource';
import { formatDate, positiveId } from '../utils/appointment';
import { recordCreateHref } from '../utils/service';
import AsyncState from '../components/AsyncState';
import PageHeading from '../components/PageHeading';
import StatusBadge from '../components/StatusBadge';

export default function AppointmentDetailPage({ id, user }) {
  const isAdmin = user.role === 'ADMIN';
  const isStaff = user.role === 'STAFF';
  const vehicleBase = user.role === 'USER' ? '#/vehicles' : '#/service/vehicles';
  const loader = useCallback((signal) => getAppointment(id, user, signal), [id, user]);
  const { data: item, loading, error, retry } = useResource(loader);
  const [staffId, setStaffId] = useState('');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [failure, setFailure] = useState('');
  const inFlight = useRef(false);
  const closed = item && ['COMPLETED', 'CANCELLED'].includes(item.status);
  async function mutate(operation, success) {
    if (inFlight.current) return;
    inFlight.current = true;
    setBusy(true); setFailure(''); setMessage('');
    try { await operation(); setMessage(success); retry(); }
    catch (reason) { setFailure(reason.message || 'İşlem tamamlanamadı.'); }
    finally { inFlight.current = false; setBusy(false); }
  }
  function assign(event) {
    event.preventDefault();
    if (!positiveId(staffId)) { setFailure('Geçerli bir personel numarası girin.'); return; }
    mutate(() => assignStaff(id, staffId), 'Personel ataması kaydedildi.');
  }
  function cancel() {
    if (window.confirm('Bu randevuyu iptal etmek istediğinize emin misiniz? İptal edilen randevu yeniden açılamaz.')) mutate(() => updateStatus(id, 'CANCELLED'), 'Randevu iptal edildi.');
  }
  return <section className="page-content"><a className="back-link" href="#/appointments"><Icon name="back" size={17} />Randevulara dön</a><PageHeading title="Randevu detayı" description="Talep, randevu durumu ve servis bilgileri." />
    {message && <div className="success-notice" role="status">{message}</div>}{failure && <div className="form-error" role="alert">{failure}</div>}
    {loading || error ? <AsyncState loading={loading} error={error} retry={retry} /> : item && <>
      <section className="detail-panel"><div className="section-heading"><h2 className="plate-text">{item.plate}</h2><StatusBadge status={item.status} /></div><dl className="detail-specs">{[['Tarih ve saat', formatDate(item.appointmentDateTime, true)], ['Personel', item.assignedStaffName || 'Atanmadı'], ['Randevu numarası', item.id], ['Talep', item.requestDescription]].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl></section>
      <div className="button-row section-actions">
        {!isStaff && <a className="button button-secondary" href={vehicleBase + '/' + item.vehicleId}>Araç detayı</a>}
        {(!isStaff || item.status !== 'CANCELLED') && <a className="button button-secondary" href={vehicleBase + '/' + item.vehicleId + '/history'}>Bakım geçmişi</a>}
        {isStaff && item.status === 'CONFIRMED' && <a className="button button-primary" href={recordCreateHref(item.vehicleId, item.id)}>Servis kaydı oluştur</a>}
      </div>
      {isAdmin && !closed && <section className="detail-panel"><h2>Personel ataması</h2><form className="assignment-form" onSubmit={assign}><div className="form-field"><label htmlFor="staff-id">Personel numarası</label><input id="staff-id" type="number" min="1" step="1" value={staffId} onChange={(event) => setStaffId(event.target.value)} disabled={busy} required /><small className="field-hint">Kayıtlı personelin kullanıcı numarasını girin.{item.assignedStaffName && ` Mevcut personel: ${item.assignedStaffName}.`}</small></div><button className="button button-secondary" type="submit" disabled={busy}>Personeli ata</button></form></section>}
      {isAdmin && !closed && <section className="detail-panel"><h2>Randevu işlemleri</h2><p className="section-description">{item.status === 'PENDING' ? 'Randevuyu onaylamadan önce personel atayın.' : 'Servis kaydı oluşturulduğunda randevu otomatik tamamlanır.'}</p><div className="button-row">
        {item.status === 'PENDING' && <button className="button button-primary" type="button" disabled={busy || item.assignedStaffId == null} onClick={() => mutate(() => updateStatus(id, 'CONFIRMED'), 'Randevu onaylandı.')}>Randevuyu onayla</button>}
        {item.status === 'CONFIRMED' && <a className="button button-primary" href={recordCreateHref(item.vehicleId, item.id)} aria-disabled={busy} onClick={(event) => { if (busy) event.preventDefault(); }}>Servis kaydı oluştur</a>}
        <button className="button button-secondary" type="button" disabled={busy} onClick={cancel}>Randevuyu iptal et</button>
      </div></section>}
      {isAdmin && closed && <p className="section-description">Bu randevu kapatılmıştır. Personeli ve durumu değiştirilemez.</p>}
    </>}
  </section>;
}
