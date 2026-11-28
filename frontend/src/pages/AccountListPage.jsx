import { useRef, useState } from 'react';
import AsyncState from '../components/AsyncState';
import Icon from '../components/Icon';
import PageHeading from '../components/PageHeading';
import TemporaryPasswordPanel from '../components/TemporaryPasswordPanel';
import { useResource } from '../hooks/useResource';
import { deleteAccount, getAccounts, renewTemporaryPassword } from '../services/accountService';
import { ROLE_LABELS } from '../utils/access';

function canSetTemporaryPassword(account) {
  return account.role !== 'ADMIN' && (account.mustChangePassword || account.passwordConfigured === false);
}

function passwordStatus(account) {
  if (account.passwordConfigured === false) return 'Şifre tanımlanmamış';
  if (account.mustChangePassword) return 'İlk giriş bekleniyor';
  return 'Hazır';
}

export default function AccountListPage() {
  const { data, loading, error, retry } = useResource(getAccounts);
  const [temporary, setTemporary] = useState(null);
  const [requestError, setRequestError] = useState('');
  const [notice, setNotice] = useState('');
  const [pending, setPending] = useState(null);
  const inFlight = useRef(false);

  async function runAction(action, account, operation) {
    if (inFlight.current) return;
    inFlight.current = true;
    setPending({ action, id: account.id });
    setTemporary(null);
    setRequestError('');
    setNotice('');
    try {
      await operation();
      retry();
    } catch (failure) {
      setRequestError(failure.message);
    } finally {
      inFlight.current = false;
      setPending(null);
    }
  }

  function renew(account) {
    if (account.mustChangePassword
      && !window.confirm(account.fullName + ' için yeni bir geçici şifre oluşturulsun mu? Önceki geçici şifre geçersiz olur.')) return;
    void runAction('renew', account, async () => {
      setTemporary(await renewTemporaryPassword(account.id));
    });
  }

  function remove(account) {
    const question = `${account.fullName} (${account.email}) hesabı silinsin mi? Hesabın oturumu kapanır. Araç, randevu ve bakım kayıtları saklanır.`;
    if (!window.confirm(question)) return;
    void runAction('delete', account, async () => {
      await deleteAccount(account.id);
      setNotice(account.fullName + ' hesabı silindi.');
    });
  }

  function isPending(action, id) {
    return pending?.action === action && pending.id === id;
  }

  const actionsDisabled = pending !== null || Boolean(temporary);

  return (
    <section className="page-content">
      <PageHeading eyebrow={null} title="Hesaplar">
        <a className="button button-primary" href="#/accounts/new"><Icon name="plus" size={18} />Yeni hesap</a>
      </PageHeading>
      {requestError && <p className="account-error" role="alert">{requestError}</p>}
      {notice && <div className="success-notice" role="status"><Icon name="check" />{notice}</div>}
      {temporary && <TemporaryPasswordPanel key={temporary.user.id} account={temporary} onDismiss={() => setTemporary(null)} />}
      {loading || error ? <AsyncState loading={loading} error={error} retry={retry} /> : (
        data.length ? (
          <div className="records-panel">
            <div className="table-scroll" tabIndex={0} aria-label="Hesap listesi">
              <table className="data-table account-table">
                <caption className="sr-only">Hesap listesi</caption>
                <thead><tr><th scope="col">No</th><th scope="col">Ad soyad</th><th scope="col">E-posta</th><th scope="col">Rol</th><th scope="col">Durum</th><th scope="col">İşlem</th></tr></thead>
                <tbody>
                  {data.map((account) => (
                    <tr key={account.id}>
                      <td>{account.id}</td>
                      <td>{account.fullName}</td>
                      <td>{account.email}</td>
                      <td>{account.role === 'USER' ? 'Müşteri' : ROLE_LABELS[account.role]}</td>
                      <td>{passwordStatus(account)}</td>
                      <td>
                        {account.role === 'ADMIN' ? <span className="account-no-action" aria-label="İşlem yok">—</span> : (
                          <div className="button-row account-actions">
                            {canSetTemporaryPassword(account) && (
                              <button className="button button-secondary" type="button" onClick={() => renew(account)} disabled={actionsDisabled}>
                                {isPending('renew', account.id) ? 'Oluşturuluyor…' : account.mustChangePassword ? 'Geçici şifreyi yenile' : 'Geçici şifre oluştur'}
                              </button>
                            )}
                            <button className="button account-delete-button" type="button" onClick={() => remove(account)} disabled={actionsDisabled}
                              aria-label={account.fullName + ' hesabını sil'}>
                              {isPending('delete', account.id) ? 'Siliniyor…' : 'Sil'}
                            </button>
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : <div className="empty-state"><h2>Hesap bulunmuyor</h2></div>
      )}
    </section>
  );
}
