import Icon from './Icon';

export default function AppShell({ page, children }) {
  const links = [
    ['/dashboard', 'Genel bakış', ['dashboard']],
    ['/vehicles', 'Araçlar', ['list', 'detail', 'edit', 'history']],
    ['/vehicles/new', 'Araç ekle', ['create']],
    ['/appointments', 'Randevular', ['appointments', 'appointment-create', 'appointment-detail']],
    ['/service-records', 'Servis kayıtları', ['records', 'record-create', 'record-detail']],
    ['/reports', 'Servis özetleri', ['reports']],
  ];
  return (
    <div className="service-layout">
      <a className="skip-link" href="#main-content" onClick={(event) => {
        event.preventDefault();
        document.getElementById('main-content')?.focus();
      }}>İçeriğe geç</a>
      <header className="service-header">
        <div className="service-header-inner">
          <a className="service-brand" href="#/dashboard" aria-label="Araç Servis ana sayfa">
            <span className="brand-mark"><Icon name="car" size={24} /></span>
            <span className="brand-copy"><strong>Araç Servis ve Bakım Yönetim Sistemi</strong></span>
          </a>
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
