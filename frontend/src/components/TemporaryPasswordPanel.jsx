import { useState } from 'react';

export default function TemporaryPasswordPanel({ account, onDismiss }) {
  const [copied, setCopied] = useState('');

  async function copyPassword() {
    try {
      await navigator.clipboard.writeText(account.temporaryPassword);
      setCopied('Geçici şifre kopyalandı.');
    } catch {
      document.getElementById('temporary-password')?.select();
      setCopied('Seçili şifreyi kopyalayabilirsiniz.');
    }
  }

  return (
    <section className="detail-panel temporary-password-panel" aria-labelledby="temporary-password-title">
      <h2 id="temporary-password-title">Geçici şifre</h2>
      <p>{account.user.fullName} · {account.user.email}</p>
      <p>Bu şifreyi hesap sahibine iletin. İlk girişte kendi şifresini belirlemesi gerekecek.</p>
      <div className="form-field">
        <label htmlFor="temporary-password">Geçici şifre</label>
        <input id="temporary-password" readOnly value={account.temporaryPassword} autoComplete="off" spellCheck={false} />
      </div>
      <div className="button-row">
        <button type="button" className="button button-primary" onClick={copyPassword}>Şifreyi kopyala</button>
        <button type="button" className="button button-secondary" onClick={onDismiss}>Tamam</button>
      </div>
      {copied && <p role="status">{copied}</p>}
    </section>
  );
}
