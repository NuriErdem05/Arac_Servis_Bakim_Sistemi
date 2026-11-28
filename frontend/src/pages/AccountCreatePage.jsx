import { useRef, useState } from 'react';
import PageHeading from '../components/PageHeading';
import TemporaryPasswordPanel from '../components/TemporaryPasswordPanel';
import { createAccount } from '../services/accountService';

export default function AccountCreatePage({ onDone }) {
  const [busy, setBusy] = useState(false);
  const [temporary, setTemporary] = useState(null);
  const [error, setError] = useState('');
  const requestInFlight = useRef(false);

  async function submit(event) {
    event.preventDefault();
    if (requestInFlight.current) return;
    const form = new FormData(event.currentTarget);
    requestInFlight.current = true;
    setBusy(true);
    setError('');
    try {
      setTemporary(await createAccount({ fullName: form.get('fullName').trim(),
        email: form.get('email').trim(), role: form.get('role') }));
    } catch (failure) {
      setError(Object.values(failure.fieldErrors ?? {})[0] || failure.message);
    } finally {
      requestInFlight.current = false;
      setBusy(false);
    }
  }

  return (
    <section className="page-content">
      <PageHeading eyebrow={null} title="Yeni hesap">
        <a className="button button-secondary" href="#/accounts">Hesaplar</a>
      </PageHeading>
      {temporary ? <TemporaryPasswordPanel account={temporary} onDismiss={onDone} /> : (
        <form className="vehicle-form account-create-form" onSubmit={submit}>
          {error && <p className="account-error" role="alert">{error}</p>}
          <fieldset className="form-grid" disabled={busy}>
            <legend className="sr-only">Hesap bilgileri</legend>
            <div className="form-field">
              <label htmlFor="account-name">Ad soyad</label>
              <input id="account-name" name="fullName" autoComplete="off" maxLength={150} required />
            </div>
            <div className="form-field">
              <label htmlFor="account-email">E-posta adresi</label>
              <input id="account-email" name="email" type="email" autoComplete="off" maxLength={254} required />
            </div>
            <div className="form-field">
              <label htmlFor="account-role">Hesap türü</label>
              <select id="account-role" name="role" defaultValue="USER" required>
                <option value="USER">Müşteri</option>
                <option value="STAFF">Personel</option>
              </select>
            </div>
          </fieldset>
          <div className="button-row section-actions">
            <button className="button button-primary" type="submit" disabled={busy}>{busy ? 'Oluşturuluyor…' : 'Hesabı oluştur'}</button>
          </div>
        </form>
      )}
    </section>
  );
}
