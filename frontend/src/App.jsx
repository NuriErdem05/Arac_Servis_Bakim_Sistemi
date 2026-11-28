import { useEffect, useState } from 'react';
import { useRoute } from './hooks/useRoute';
import AppShell from './components/AppShell';
import Icon from './components/Icon';
import VehicleListPage from './pages/VehicleListPage';
import VehicleCreatePage from './pages/VehicleCreatePage';
import VehicleDetailPage from './pages/VehicleDetailPage';
import VehicleEditPage from './pages/VehicleEditPage';
import AdminHomePage from './pages/AdminHomePage';
import AppointmentListPage from './pages/AppointmentListPage';
import AppointmentCreatePage from './pages/AppointmentCreatePage';
import AppointmentDetailPage from './pages/AppointmentDetailPage';
import ServiceRecordListPage from './pages/ServiceRecordListPage';
import ServiceRecordCreatePage from './pages/ServiceRecordCreatePage';
import ServiceRecordDetailPage from './pages/ServiceRecordDetailPage';
import VehicleHistoryPage from './pages/VehicleHistoryPage';
import ReportsPage from './pages/ReportsPage';

export default function App() {
  const { route, navigate } = useRoute();
  const [notice, setNotice] = useState('');

  useEffect(() => {
    document.getElementById('page-title')?.focus();
    window.scrollTo(0, 0);
  }, [route.path]);

  useEffect(() => {
    if (!notice) return;
    const timer = window.setTimeout(() => setNotice(''), 7000);
    return () => window.clearTimeout(timer);
  }, [notice]);

  function onSaved(message, destination) {
    setNotice(message);
    navigate(destination);
  }

  let page;
  if (route.page === 'dashboard') page = <AdminHomePage />;
  else if (route.page === 'list') page = <VehicleListPage scope={route.scope} />;
  else if (route.page === 'create') page = <VehicleCreatePage onSaved={onSaved} />;
  else if (route.page === 'detail') page = <VehicleDetailPage key={route.id} id={route.id} scope={route.scope} />;
  else if (route.page === 'edit') page = <VehicleEditPage key={route.id} id={route.id} scope={route.scope} onSaved={onSaved} />;
  else if (route.page === 'history') page = <VehicleHistoryPage id={route.id} scope={route.scope} />;
  else if (route.page === 'appointments') page = <AppointmentListPage />;
  else if (route.page === 'appointment-create') page = <AppointmentCreatePage query={route.query} onSaved={onSaved} />;
  else if (route.page === 'appointment-detail') page = <AppointmentDetailPage id={route.id} onSaved={onSaved} />;
  else if (route.page === 'records') page = <ServiceRecordListPage />;
  else if (route.page === 'record-create') page = <ServiceRecordCreatePage query={route.query} onSaved={onSaved} />;
  else if (route.page === 'record-detail') page = <ServiceRecordDetailPage id={route.id} />;
  else if (route.page === 'reports') page = <ReportsPage />;
  else page = <section className="state-panel"><h1 id="page-title" tabIndex={-1}>Sayfa bulunamadı</h1><p>İstediğiniz sayfa mevcut değil.</p><a className="button button-primary" href="#/dashboard">Genel bakışa dön</a></section>;

  return <AppShell page={route.page}>{notice && <div className="success-notice" role="status"><Icon name="check" /><span>{notice}</span><button type="button" aria-label="Bildirimi kapat" onClick={() => setNotice('')}><Icon name="close" size={17} /></button></div>}<div key={route.path}>{page}</div></AppShell>;
}
