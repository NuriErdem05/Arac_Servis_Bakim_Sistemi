import { useRef, useState } from 'react';
import AuthLayout from '../components/AuthLayout';
import Icon from '../components/Icon';
import { MAX_PASSWORD_BYTES, MIN_PASSWORD_LENGTH, newPasswordError } from '../utils/password';

export default function PasswordSetupPage({ registration = false, onSubmit, onLogout }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const submitting = useRef(false);
  const action = registration ? 'Hesap oluştur' : 'Şifreyi kaydet';

  async function submit(event) {
    event.preventDefault();
    if (submitting.current) return;
    const form = new FormData(event.currentTarget);
    const password = form.get('password');
    setError('');

    const confirmation = form.get('confirmation');
    const validationError = newPasswordError(password, confirmation);
    if (validationError) {
      setError(validationError);
      document.getElementById(password !== confirmation ? 'new-password-confirmation' : 'new-password')?.focus();
      return;
    }

    submitting.current = true;
    setBusy(true);
    try {
      if (registration) {
        await onSubmit({ fullName: form.get('fullName').trim(), email: form.get('email').trim(), password });
      } else {
        await onSubmit(password);
      }
    } catch (failure) {
      setError(Object.values(failure.fieldErrors ?? {})[0] || failure.message);
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  }

  return (
    <AuthLayout title={registration ? 'Hesap oluştur' : 'Yeni şifre belirle'}>
      {!registration && <p className="auth-description">Devam etmek için geçici şifrenizi değiştirin.</p>}
      <form className="login-form" onSubmit={submit}>
        {error && <p className="login-feedback login-error" role="alert"><Icon name="info" size={18} />{error}</p>}
        {registration && <>
          <label htmlFor="register-name">Ad soyad</label>
          <input className="login-input" id="register-name" name="fullName" autoComplete="name" maxLength={150} disabled={busy} required />
          <label htmlFor="register-email">E-posta adresi</label>
          <input className="login-input" id="register-email" name="email" type="email" autoComplete="username" maxLength={254} disabled={busy} required />
        </>}
        <label htmlFor="new-password">{registration ? 'Şifre' : 'Yeni şifre'}</label>
        <div className="login-password">
          <input className="login-input" id="new-password" name="password" type={showPassword ? 'text' : 'password'}
            autoComplete="new-password" minLength={MIN_PASSWORD_LENGTH} maxLength={MAX_PASSWORD_BYTES}
            aria-describedby="password-rule" disabled={busy} required />
          <button type="button" className="login-password-toggle" onClick={() => setShowPassword((visible) => !visible)}
            aria-label={showPassword ? 'Şifreyi gizle' : 'Şifreyi göster'}>
            <Icon name={showPassword ? 'eye-off' : 'eye'} size={19} />
          </button>
        </div>
        <p className="auth-password-rule" id="password-rule">En az {MIN_PASSWORD_LENGTH} karakter kullanın.</p>
        <label htmlFor="new-password-confirmation">Şifre tekrarı</label>
        <input className="login-input" id="new-password-confirmation" name="confirmation"
          type={showPassword ? 'text' : 'password'} autoComplete="new-password" maxLength={MAX_PASSWORD_BYTES} disabled={busy} required />
        <button type="submit" className="login-submit" disabled={busy}>
          {busy ? 'Kaydediliyor…' : action}
        </button>
      </form>
      <p className="auth-links">
        {registration ? <a href="#/login">Giriş yap</a> : <button type="button" onClick={onLogout}>Çıkış yap</button>}
      </p>
    </AuthLayout>
  );
}
