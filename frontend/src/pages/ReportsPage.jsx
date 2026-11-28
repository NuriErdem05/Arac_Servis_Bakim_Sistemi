import { useState } from 'react';
import { getReportData } from '../services/dashboardService';
import { useResource } from '../hooks/useResource';
import { formatMoney, localDate, SERVICE_LABELS, STATUS_LABELS, toCents } from '../utils/service';
import AsyncState from '../components/AsyncState';
import PageHeading from '../components/PageHeading';

export default function ReportsPage() {
  const { data, loading, error, retry } = useResource(getReportData);
  const [start, setStart] = useState(() => localDate().slice(0, 8) + '01');
  const [end, setEnd] = useState(localDate);
  const valid = Boolean(start && end && start <= end);
  const inRange = (date) => valid && date >= start && date <= end;
  const records = (data?.records || []).filter((item) => inRange(item.serviceDate));
  const appointments = (data?.appointments || []).filter((item) => inRange(item.appointmentDateTime.slice(0, 10)));
  const totalCents = records.reduce((sum, item) => sum + toCents(item.cost), 0);
  return <section className="page-content"><PageHeading eyebrow="Dönem özeti" title="Servis özetleri" description="Seçili dönemdeki randevular, tamamlanan işlemler ve maliyetler." />
    <div className="collection-toolbar toolbar-two"><div className="filter-control"><label htmlFor="report-start">Başlangıç tarihi</label><input id="report-start" type="date" value={start} onChange={(event) => setStart(event.target.value)} /></div><div className="filter-control"><label htmlFor="report-end">Bitiş tarihi</label><input id="report-end" type="date" value={end} onChange={(event) => setEnd(event.target.value)} /></div></div>
    {!valid && <p className="form-error" role="alert">Geçerli bir tarih aralığı seçin. Bitiş tarihi başlangıçtan önce olamaz.</p>}
    {loading || error ? <AsyncState loading={loading} error={error} retry={retry} /> : valid && <>
      <p className="overview-count">İki sınır günü de dahildir. Randevular randevu tarihine, servis işlemleri işlem tarihine göre sayılır.</p>
      <dl className="overview-summary">{[['Randevu', appointments.length], ['Tamamlanan servis işlemi', records.length], ['Toplam işlem maliyeti', formatMoney(totalCents / 100)], ['Ortalama işlem maliyeti', formatMoney(records.length ? totalCents / records.length / 100 : 0)]].map(([label, value]) => <div key={label}><dt>{label}</dt><dd className="summary-value">{value}</dd></div>)}</dl>
      <div className="report-grid"><section className="detail-panel"><h2>İşlem türlerine göre</h2><dl className="report-list">{Object.entries(SERVICE_LABELS).map(([type, label]) => { const items = records.filter((item) => item.serviceType === type); return <div key={type}><dt>{label}</dt><dd>{items.length} kayıt · {formatMoney(items.reduce((sum, item) => sum + toCents(item.cost), 0) / 100)}</dd></div>; })}</dl></section><section className="detail-panel"><h2>Randevu durumları</h2><dl className="report-list">{Object.entries(STATUS_LABELS).map(([status, label]) => <div key={status}><dt>{label}</dt><dd>{appointments.filter((item) => item.status === status).length}</dd></div>)}</dl></section></div>
      {records.length === 0 && appointments.length === 0 && <div className="empty-state"><p>Seçili dönemde randevu veya servis kaydı bulunmuyor.</p></div>}
    </>}
  </section>;
}
