export default function AuthLayout({ title, children }) {
  return (
    <main className="login-layout">
      <section className="login-group" aria-labelledby="page-title">
        <p className="login-system-name">Araç Servis ve Bakım Yönetim Sistemi</p>
        <div className="login-panel">
          <h1 id="page-title" tabIndex={-1}>{title}</h1>
          {children}
        </div>
      </section>
    </main>
  );
}
