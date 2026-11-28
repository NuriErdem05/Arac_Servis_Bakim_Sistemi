import Icon from './Icon';
import { getHomePath, getNavigation, ROLE_LABELS } from '../utils/access';

export default function AppShell({ page, user, onLogout, children }) {
  const links = getNavigation(user.role);
  return (
    <div className="service-layout">
      <a className="skip-link" href="#main-content" onClick={(event) => {
        event.preventDefault();
        document.getElementById('main-content')?.focus();
      }}>İçeriğe geç</a>
      <header className="service-header">
        <div className="service-header-inner">
          <a className="service-brand" href={'#' + getHomePath(user.role)} aria-label="Araç Servis ana sayfa">
            <span className="brand-mark"><Icon name="car" size={24} /></span>
            <span className="brand-copy"><strong>Araç Servis ve Bakım Yönetim Sistemi</strong></span>
          </a>
          <div className="service-account">
            <span className="service-account-identity">
              <strong>{user.fullName}</strong>
              <small>{ROLE_LABELS[user.role]}</small>
            </span>
            <button type="button" onClick={onLogout}>Çıkış yap</button>
          </div>
          <nav className="service-nav" aria-label="Ana menü">
            {links.map(([path, label, pages]) => <a key={path} href={'#' + path} className={pages.includes(page) ? 'active' : ''} aria-current={pages.includes(page) ? 'page' : undefined}>{label}</a>)}
          </nav>
        </div>
      </header>
      <main id="main-content" className="main-content" tabIndex={-1}>{children}</main>
      <footer className="service-footer">Araç Servis ve Bakım Yönetim Sistemi</footer>
    </div>
  );
}
