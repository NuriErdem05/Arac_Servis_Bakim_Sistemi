import Icon from './Icon';
import { getSession } from '../services/authSession';
import { getHomePath } from '../utils/access';

export default function AsyncState({ loading, error, retry, loadingText = 'Bilgiler yükleniyor…' }) {
  if (loading) return <div className="state-panel" role="status"><span className="spinner" />{loadingText}</div>;
  if (!error) return null;
  const title = { 403: 'Erişim yetkiniz bulunmuyor', 404: 'Kayıt bulunamadı' }[error.status] || 'Bilgiler yüklenemedi';
  const role = getSession().user?.role;
  return (
    <section className="state-panel error-panel" role="alert">
      <Icon name="info" size={30} />
      <h2>{title}</h2>
      <p>{error.message || 'İşlem tamamlanamadı.'}</p>
      <div className="button-row">
        <button type="button" className="button button-primary" onClick={retry}>Tekrar dene</button>
        {role && <a className="button button-secondary" href={'#' + getHomePath(role)}>Ana sayfaya dön</a>}
      </div>
    </section>
  );
}
