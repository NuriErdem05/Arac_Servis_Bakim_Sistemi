import Icon from './Icon';

export default function AsyncState({ loading, error, retry }) {
  if (loading) return <div className="state-panel" role="status"><span className="spinner" />Araç bilgileri yükleniyor…</div>;
  if (!error) return null;
  const unavailable = error.status === 404;
  return (
    <section className="state-panel error-panel" role="alert">
      <Icon name="info" size={30} />
      <h2>{unavailable ? 'Araç bulunamadı' : 'Bilgiler yüklenemedi'}</h2>
      <p>{error.message || 'İşlem tamamlanamadı.'}</p>
      <div className="button-row">
        <button type="button" className="button button-primary" onClick={retry}>Tekrar dene</button>
        <a className="button button-secondary" href="#/vehicles">Araçlarıma dön</a>
      </div>
    </section>
  );
}
