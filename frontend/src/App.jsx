import { useEffect, useState } from 'react';
import { useRoute } from './hooks/useRoute';
import AppShell from './components/AppShell';
import Icon from './components/Icon';
import VehicleListPage from './pages/VehicleListPage';
import VehicleCreatePage from './pages/VehicleCreatePage';
import VehicleDetailPage from './pages/VehicleDetailPage';
import VehicleEditPage from './pages/VehicleEditPage';

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
  if (route.page === 'list') page = <VehicleListPage />;
  else if (route.page === 'create') page = <VehicleCreatePage onSaved={onSaved} />;
  else if (route.page === 'detail') page = <VehicleDetailPage key={route.id} id={route.id} />;
  else if (route.page === 'edit') page = <VehicleEditPage key={route.id} id={route.id} onSaved={onSaved} />;
  else page = <section className="state-panel"><h1 id="page-title" tabIndex={-1}>Sayfa bulunamadı</h1><p>İstediğiniz sayfa mevcut değil.</p><a className="button button-primary" href="#/vehicles">Araçlarıma dön</a></section>;

  return <AppShell page={route.page}>{notice && <div className="success-notice" role="status"><Icon name="check" /><span>{notice}</span><button type="button" aria-label="Bildirimi kapat" onClick={() => setNotice('')}><Icon name="close" size={17} /></button></div>}{page}</AppShell>;
}
