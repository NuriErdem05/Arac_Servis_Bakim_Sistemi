import { useRef, useState } from 'react';

export default function EntryForm({ initialValues, fields, validate, onSave, cancelHref, submitLabel = 'Kaydet', children }) {
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const inFlight = useRef(false);
  const summary = useRef(null);
  const definitions = typeof fields === 'function' ? fields(values) : fields;
  async function submit(event) {
    event.preventDefault();
    if (inFlight.current) return;
    const invalid = validate(values);
    setErrors(invalid);
    setError('');
    if (Object.keys(invalid).length) {
      document.getElementById('entry-' + Object.keys(invalid)[0])?.focus();
      return;
    }
    inFlight.current = true;
    setSaving(true);
    try { await onSave(values); }
    catch (failure) {
      setError(failure.message || 'İşlem tamamlanamadı.');
      setErrors(failure.fieldErrors || {});
      requestAnimationFrame(() => summary.current?.focus());
    } finally { inFlight.current = false; setSaving(false); }
  }
  function change(event) {
    const { name, value } = event.target;
    setValues((old) => ({ ...old, [name]: value }));
    setErrors((old) => ({ ...old, [name]: '' }));
  }
  return <form className="vehicle-form" onSubmit={submit} noValidate>
    {children && <div className="form-section-heading">{children}</div>}
    {error && <div className="form-error" role="alert" ref={summary} tabIndex={-1}>{error}</div>}
    <fieldset className="form-grid" disabled={saving}><legend className="sr-only">Kayıt bilgileri</legend>{definitions.map((field) => {
      const id = 'entry-' + field.name;
      const common = { id, name: field.name, value: field.value ?? values[field.name] ?? '', onChange: change, required: !field.optional, 'aria-invalid': Boolean(errors[field.name]), 'aria-describedby': errors[field.name] ? id + '-error' : field.hint ? id + '-hint' : undefined, readOnly: field.readOnly };
      return <div className={'form-field' + (field.wide ? ' field-wide' : '')} key={field.name}><label htmlFor={id}>{field.label}{field.optional ? <span className="optional">İsteğe bağlı</span> : <span className="required-mark" aria-hidden="true">*</span>}</label>
        {field.type === 'select' ? <select {...common}><option value="">{field.placeholder || 'Seçin'}</option>{field.options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select> : field.type === 'textarea' ? <textarea {...common} rows={4} maxLength={field.maxLength} /> : <input {...common} type={field.type || 'text'} min={field.min} max={field.max} step={field.step} maxLength={field.maxLength} placeholder={field.placeholder} />}
        {field.hint && <small className="field-hint" id={id + '-hint'}>{field.hint}</small>}{errors[field.name] && <small className="field-error" id={id + '-error'}>{errors[field.name]}</small>}
      </div>;
    })}</fieldset>
    <div className="form-footer"><span className="required-note">* Zorunlu alanlar</span><div className="button-row"><a className="button button-secondary" href={cancelHref} aria-disabled={saving} onClick={(event) => { if (saving) event.preventDefault(); }}>Vazgeç</a><button className="button button-primary" disabled={saving} type="submit">{saving ? 'Kaydediliyor…' : submitLabel}</button></div></div>
  </form>;
}
