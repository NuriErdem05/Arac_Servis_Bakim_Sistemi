import Icon from '../components/Icon';
import { useEffect, useState } from 'react';
import { getDashboardData } from '../services/dashboardService';
import { useResource } from '../hooks/useResource';
import AsyncState from '../components/AsyncState';
import PageHeading from '../components/PageHeading';
import AppointmentTable from '../components/AppointmentTable';

const views = { upcoming: 'Yaklaşan randevular', today: 'Bugün', unassigned: 'Personel atanmamış' };

export default function AdminHomePage() {
  const { data, loading, error, retry } = useResource(getDashboardData);
  const [view, setView] = useState('upcoming');
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 60000);
    return () => window.clearInterval(timer);
  }, []);
  const active = (data?.appointments || []).filter((item) => ['PENDING', 'CONFIRMED'].includes(item.status));
  const today = active.filter((item) => new Date(item.appointmentDateTime).toDateString() === now.toDateString());
  const unassigned = active.filter((item) => item.assignedStaffId == null);
  const upcoming = active.filter((item) => new Date(item.appointmentDateTime) >= now);
  const sorted = [...({ upcoming, today, unassigned }[view])].sort((a, b) => new Date(a.appointmentDateTime) - new Date(b.appointmentDateTime));
  return <section className="page-content">
    <PageHeading title="Genel bakış" eyebrow="Servis özeti" description="Servis genelindeki araçlar ve randevu akışı." />
    {loading || error ? <AsyncState loading={loading} error={error} retry={retry} /> : data && <>
      <dl className="overview-summary">{[['Aktif araç', data.vehicles.length, 'car'], ['Bugünkü aktif randevu', today.length, 'calendar'], ['Onay bekleyen', active.filter((item) => item.status === 'PENDING').length, 'clock'], ['Personel atanmamış', unassigned.length, 'users']].map(([label, count, icon]) => <div key={label}><dt><span>{label}</span><Icon name={icon} size={18} /></dt><dd>{count}</dd></div>)}</dl>
      <div className="quick-links quick-links-home" aria-label="Hızlı işlemler">
        <a href="#/appointments"><span className="quick-link-icon"><Icon name="calendar" size={22} /></span><span className="quick-link-copy"><strong>Randevuları görüntüle</strong><small>Talepleri, durumları ve personel atamalarını takip edin.</small></span><Icon name="arrow" size={18} /></a>
        <a href="#/appointments/new"><span className="quick-link-icon"><Icon name="plus" size={22} /></span><span className="quick-link-copy"><strong>Randevu oluştur</strong><small>Araç için uygun tarih ve servis talebini belirleyin.</small></span><Icon name="arrow" size={18} /></a>
        <a href="#/service-records/new"><span className="quick-link-icon"><Icon name="check" size={22} /></span><span className="quick-link-copy"><strong>Servis kaydı oluştur</strong><small>Tamamlanan bakım veya onarımı kaydedin.</small></span><Icon name="arrow" size={18} /></a>
      </div>
      <div className="overview-heading"><h2>Randevu akışı</h2><span>{now.toLocaleDateString('tr-TR', { dateStyle: 'long' })}</span></div>
      <div className="overview-filters" role="group" aria-label="Randevu görünümü">{Object.entries(views).map(([key, label]) => <button key={key} type="button" aria-pressed={view === key} onClick={() => setView(key)}>{label}</button>)}</div>
      <p className="overview-count" aria-live="polite">{sorted.length} randevu{sorted.length > 8 && ' · İlk 8 kayıt gösteriliyor'}</p>
      {sorted.length ? <AppointmentTable rows={sorted.slice(0, 8)} caption={views[view]} /> : <div className="empty-state"><p>Bu görünümde aktif randevu bulunmuyor.</p><a className="button button-secondary" href="#/appointments">Tüm randevulara git</a></div>}
    </>}
  </section>;
}
