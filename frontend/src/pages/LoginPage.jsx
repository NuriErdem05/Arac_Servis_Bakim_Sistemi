import { useRef, useState } from 'react';
import AuthLayout from '../components/AuthLayout';
import Icon from '../components/Icon';

function loginError(error) {
  if (error.status === 401) return 'E-posta adresi veya şifre hatalı.';
  if (error.status === 404 || error.status === 405) return 'Giriş servisi henüz kullanıma hazır değil.';
  if (error.status === 429) return 'Çok fazla giriş denemesi yapıldı. Bir süre sonra tekrar deneyin.';
  if (error.status >= 500) return 'Giriş servisine ulaşılamadı. Lütfen tekrar deneyin.';
  return error.message || 'Giriş yapılamadı. Lütfen tekrar deneyin.';
}

export default function LoginPage({ onLogin, notice, restoring, restoreError, onRetry, onLogout, registeredEmail }) {
  const [showPassword, setShowPassword] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const submitting = useRef(false);

  async function submit(event) {
    event.preventDefault();
    if (submitting.current) return;
    const fields = new FormData(event.currentTarget);
    submitting.current = true;
    setBusy(true);
    setError('');
    try {
      await onLogin(fields.get('email').trim(), fields.get('password'));
    } catch (failure) {
      setError(loginError(failure));
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  }

  return (
    <AuthLayout title={restoring ? 'Oturum kontrolü' : 'Giriş yap'}>
      {restoring ? (
        restoreError ? (
          <>
            <p className="login-feedback login-error" role="alert">{restoreError}</p>
            <div className="button-row">
              <button type="button" className="button button-primary" onClick={onRetry}>Tekrar dene</button>
              <button type="button" className="button button-secondary" onClick={onLogout}>Giriş ekranına dön</button>
            </div>
          </>
        ) : <p className="login-feedback" role="status"><span className="spinner" /> Oturum doğrulanıyor…</p>
      ) : (
        <form className="login-form" onSubmit={submit}>
          {(error || notice || registeredEmail) && <p className={error ? 'login-feedback login-error' : 'login-feedback'} role={error ? 'alert' : 'status'}><Icon name="info" size={18} />{error || notice || 'Hesabınız oluşturuldu. Şimdi giriş yapabilirsiniz.'}</p>}
          <label htmlFor="login-email">E-posta adresi</label>
          <input className="login-input" id="login-email" name="email" type="email" defaultValue={registeredEmail} autoComplete="username" placeholder="ornek@eposta.com" maxLength={254} disabled={busy} required />
          <label htmlFor="login-password">Şifre</label>
          <div className="login-password">
            <input className="login-input" id="login-password" name="password" type={showPassword ? 'text' : 'password'} autoComplete="current-password" disabled={busy} required />
            <button type="button" className="login-password-toggle" onClick={() => setShowPassword(!showPassword)} aria-label={showPassword ? 'Şifreyi gizle' : 'Şifreyi göster'}>
              <Icon name={showPassword ? 'eye-off' : 'eye'} size={19} />
            </button>
          </div>
          <button type="submit" className="login-submit" disabled={busy}>
            {busy ? 'Giriş yapılıyor…' : 'Giriş yap'}
            {busy ? <span className="spinner" aria-hidden="true" /> : <Icon name="arrow" size={20} />}
          </button>
        </form>
      )}
      {!restoring && <p className="auth-links"><a href="#/register">Hesap oluştur</a></p>}
    </AuthLayout>
  );
}
