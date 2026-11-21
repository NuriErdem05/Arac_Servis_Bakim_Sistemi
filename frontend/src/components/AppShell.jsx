export default function AppShell({ page, children }) {
  const isCreate = page === 'create';
  return (
    <div className="service-layout">
      <a className="skip-link" href="#main-content" onClick={(event) => {
        event.preventDefault();
        document.getElementById('main-content')?.focus();
      }}>İçeriğe geç</a>
      <header className="service-header">
        <div className="service-header-inner">
          <a className="service-brand" href="#/vehicles" aria-label="Araç Servis ana sayfa">Araç Servis</a>
          <nav className="service-nav" aria-label="Ana menü">
            <a href="#/vehicles" className={!isCreate ? 'active' : ''} aria-current={!isCreate ? 'page' : undefined}>Araçlarım</a>
            <a href="#/vehicles/new" className={isCreate ? 'active' : ''} aria-current={isCreate ? 'page' : undefined}>Araç ekle</a>
          </nav>
        </div>
      </header>
      <main id="main-content" className="main-content" tabIndex={-1}>{children}</main>
      <footer className="service-footer">Araç Servis ve Bakım Yönetimi</footer>
    </div>
  );
}
