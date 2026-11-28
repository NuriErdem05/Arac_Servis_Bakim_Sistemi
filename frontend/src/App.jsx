import { useEffect, useState, useSyncExternalStore } from 'react';
import { parseRoute, useRoute } from './hooks/useRoute';
import { changeInitialPassword, getCurrentUser, login, registerAccount } from './services/authService';
import { getSession, setSession, subscribeToSession } from './services/authSession';
import { canOpenRoute, getHomePath } from './utils/access';
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
import LoginPage from './pages/LoginPage';
import PasswordSetupPage from './pages/PasswordSetupPage';
import AccountListPage from './pages/AccountListPage';
import AccountCreatePage from './pages/AccountCreatePage';

export default function App() {
  const { route, navigate } = useRoute();
  const { token, user, notice } = useSyncExternalStore(subscribeToSession, getSession);
  const [restoreAttempt, setRestoreAttempt] = useState(0);
  const [restoreError, setRestoreError] = useState('');
  const [registeredEmail, setRegisteredEmail] = useState('');
  const homePath = user ? getHomePath(user.role) : null;
  const showHome = ['login', 'home', 'register', 'password-change'].includes(route.page);
  const redirectPath = user?.mustChangePassword ? '/password/change' : showHome ? homePath : null;
  const activeRoute = redirectPath && route.path !== redirectPath ? parseRoute(redirectPath) : route;

  useEffect(() => {
    if (!token || user) return;
    const controller = new AbortController();
    getCurrentUser(controller.signal)
      .then((account) => {
        if (!controller.signal.aborted && getSession().token === token) setSession(token, account);
      })
      .catch((error) => {
        if (!controller.signal.aborted && getSession().token === token) {
          setRestoreError(error.message || 'Oturum doğrulanamadı.');
        }
      });
    return () => controller.abort();
  }, [token, user, restoreAttempt]);

  useEffect(() => {
    if (redirectPath && route.path !== redirectPath) navigate(redirectPath, { replace: true });
  }, [redirectPath, route.path, navigate]);

  useEffect(() => {
    document.getElementById('page-title')?.focus();
    window.scrollTo(0, 0);
  }, [activeRoute.path, token, user]);

  async function onLogin(email, password) {
    const result = await login(email, password);
    const canResume = !showHome && route.page !== 'not-found' && canOpenRoute(result.user.role, route);
    const destination = result.user.mustChangePassword ? '/password/change'
      : canResume ? route.path : getHomePath(result.user.role);
    setRegisteredEmail('');
    setSession(result.token, result.user);
    navigate(destination, { replace: true });
  }

  async function onRegister(values) {
    const account = await registerAccount(values);
    if (parseRoute(window.location.hash.slice(1)).page !== 'register') return;
    setRegisteredEmail(account.email);
    navigate('/login', { replace: true });
  }

  async function onPasswordChange(password) {
    const activeToken = getSession().token;
    const result = await changeInitialPassword(password);
    if (getSession().token !== activeToken) return;
    setSession(result.token, result.user);
    navigate(getHomePath(result.user.role), { replace: true });
  }

  function onLogout() {
    setRegisteredEmail('');
    setRestoreError('');
    setSession(null);
    navigate('/login', { replace: true });
  }

  if (!user && !token && route.page === 'register') {
    return <PasswordSetupPage registration onSubmit={onRegister} />;
  }

  if (user?.mustChangePassword) {
    return <PasswordSetupPage onSubmit={onPasswordChange} onLogout={onLogout} />;
  }

  if (!user) {
    return <LoginPage
      registeredEmail={registeredEmail}
      onLogin={onLogin}
      notice={notice}
      restoring={Boolean(token)}
      restoreError={restoreError}
      onRetry={() => {
        setRestoreError('');
        setRestoreAttempt((attempt) => attempt + 1);
      }}
      onLogout={onLogout}
    />;
  }

  return <AuthenticatedApp user={user} route={activeRoute} navigate={navigate} onLogout={onLogout} />;
}

function AuthenticatedApp({ user, route, navigate, onLogout }) {
  const [notice, setNotice] = useState('');

  useEffect(() => {
    if (!notice) return;
    const timer = window.setTimeout(() => setNotice(''), 7000);
    return () => window.clearTimeout(timer);
  }, [notice]);

  function onSaved(message, destination) {
    if (getSession().user !== user) return;
    setNotice(message);
    navigate(destination);
  }

  let page;
  if (!canOpenRoute(user.role, route)) {
    page = <PageMessage title="Bu sayfaya erişim yetkiniz yok" message="Hesabınızın izin verdiği işlemlere ana menüden ulaşabilirsiniz." homePath={getHomePath(user.role)} />;
  } else if (route.page === 'dashboard') page = <AdminHomePage />;
  else if (route.page === 'list') page = <VehicleListPage scope={route.scope} />;
  else if (route.page === 'create') page = <VehicleCreatePage onSaved={onSaved} />;
  else if (route.page === 'detail') page = <VehicleDetailPage key={route.id} id={route.id} scope={route.scope} />;
  else if (route.page === 'edit') page = <VehicleEditPage key={route.id} id={route.id} scope={route.scope} onSaved={onSaved} />;
  else if (route.page === 'history') page = <VehicleHistoryPage id={route.id} scope={route.scope} user={user} />;
  else if (route.page === 'appointments') page = <AppointmentListPage user={user} />;
  else if (route.page === 'appointment-create') page = <AppointmentCreatePage query={route.query} onSaved={onSaved} user={user} />;
  else if (route.page === 'appointment-detail') page = <AppointmentDetailPage id={route.id} user={user} />;
  else if (route.page === 'records') page = <ServiceRecordListPage />;
  else if (route.page === 'record-create') page = <ServiceRecordCreatePage query={route.query} onSaved={onSaved} user={user} />;
  else if (route.page === 'record-detail') page = <ServiceRecordDetailPage id={route.id} />;
  else if (route.page === 'reports') page = <ReportsPage />;
  else if (route.page === 'accounts') page = <AccountListPage />;
  else if (route.page === 'account-create') page = <AccountCreatePage onDone={() => onSaved('Hesap oluşturuldu.', '/accounts')} />;
  else page = <PageMessage title="Sayfa bulunamadı" message="İstediğiniz sayfa mevcut değil." homePath={getHomePath(user.role)} />;

  return <AppShell page={route.page} user={user} onLogout={onLogout}>{notice && <div className="success-notice" role="status"><Icon name="check" /><span>{notice}</span><button type="button" aria-label="Bildirimi kapat" onClick={() => setNotice('')}><Icon name="close" size={17} /></button></div>}<div key={route.path}>{page}</div></AppShell>;
}

function PageMessage({ title, message, homePath }) {
  return (
    <section className="state-panel">
      <h1 id="page-title" tabIndex={-1}>{title}</h1>
      <p>{message}</p>
      <a className="button button-primary" href={'#' + homePath}>Ana sayfaya dön</a>
    </section>
  );
}
