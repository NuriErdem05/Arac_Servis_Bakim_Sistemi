import { useRef, useState } from 'react';
import { buildVehiclePayload, FUEL_TYPES, formatNumber, makeFormValues, validateVehicle } from '../utils/vehicle';
import Icon from './Icon';

const FIELDS = [
  { name: 'plate', label: 'Plaka', placeholder: 'Örn. 58 ABC 123', autoComplete: 'off', maxLength: 20 },
  { name: 'brand', label: 'Marka', placeholder: 'Örn. Renault', maxLength: 100 },
  { name: 'model', label: 'Model', placeholder: 'Örn. Clio', maxLength: 100 },
  { name: 'modelYear', label: 'Model yılı', placeholder: 'Örn. 2022', type: 'number', min: 1886, max: new Date().getFullYear() + 1 },
  { name: 'fuelType', label: 'Yakıt türü', type: 'select' },
  { name: 'engineInfo', label: 'Motor bilgisi', placeholder: 'Örn. 1.0 TCe', optional: true, maxLength: 150 },
  { name: 'currentMileage', label: 'Güncel kilometre', placeholder: 'Örn. 45000', type: 'number', min: 0 },
];

export default function VehicleForm({ initialVehicle, onSave, cancelHref }) {
  const [values, setValues] = useState(() => makeFormValues(initialVehicle));
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const inFlight = useRef(false);
  const editing = Boolean(initialVehicle);
  const errorSummary = useRef(null);

  function handleChange(event) {
    const { name, value } = event.target;
    setValues((previous) => ({ ...previous, [name]: value }));
    setFieldErrors((previous) => ({ ...previous, [name]: '' }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (inFlight.current) return;
    setError('');
    const validation = validateVehicle(values, initialVehicle);
    setFieldErrors(validation);
    if (Object.keys(validation).length) {
      document.getElementById(`vehicle-${Object.keys(validation)[0]}`)?.focus();
      return;
    }

    inFlight.current = true;
    setSaving(true);
    try {
      await onSave(buildVehiclePayload(values));
    } catch (failure) {
      setError(failure.message || 'Araç kaydedilemedi. Tekrar deneyin.');
      setFieldErrors(failure.fieldErrors || {});
      requestAnimationFrame(() => errorSummary.current?.focus());
    } finally {
      inFlight.current = false;
      setSaving(false);
    }
  }

  return (
    <form className="vehicle-form" onSubmit={handleSubmit} noValidate>
      <div className="form-section-heading"><h2>Araç bilgileri</h2><p>Plaka, marka ve model bilgilerini ruhsatınıza göre girin.</p></div>
      {error && <div className="form-error" role="alert" ref={errorSummary} tabIndex={-1}><Icon name="info" /><span>{error}</span></div>}
      <fieldset disabled={saving} className="form-grid">
        <legend className="sr-only">Araç bilgileri</legend>
        {FIELDS.map((field) => {
          const message = fieldErrors[field.name];
          const id = `vehicle-${field.name}`;
          const attributes = {
            id, name: field.name, value: values[field.name], onChange: handleChange,
            'aria-invalid': Boolean(message),
            'aria-describedby': message ? `${id}-error` : field.name === 'currentMileage' && editing ? 'mileage-hint' : undefined,
            required: !field.optional,
          };
          return (
            <div className={`form-field ${field.name === 'currentMileage' ? 'mileage-field' : ''}`} key={field.name}>
              <label htmlFor={id}>{field.label}{field.optional ? <span className="optional">İsteğe bağlı</span> : <span aria-hidden="true" className="required-mark">*</span>}</label>
              {field.type === 'select'
                ? <select {...attributes}><option value="">Yakıt türü seçin</option>{FUEL_TYPES.map((fuel) => <option value={fuel.value} key={fuel.value}>{fuel.label}</option>)}</select>
                : <input {...attributes} type={field.type || 'text'} placeholder={field.placeholder} autoComplete={field.autoComplete || 'off'} maxLength={field.maxLength} max={field.max}
                  min={field.name === 'currentMileage' ? initialVehicle?.currentMileage ?? 0 : field.min} step={field.type === 'number' ? 1 : undefined} />}
              {field.name === 'currentMileage' && editing && <small className="field-hint" id="mileage-hint">Kayıtlı kilometre: {formatNumber(initialVehicle.currentMileage)} km. Bu değer azaltılamaz.</small>}
              {message && <small className="field-error" id={`${id}-error`}>{message}</small>}
            </div>
          );
        })}
      </fieldset>
      <div className="form-footer"><span className="required-note">* Zorunlu alanlar</span><div className="button-row">
        <a className="button button-secondary" href={cancelHref} aria-disabled={saving} onClick={(event) => { if (saving) event.preventDefault(); }}>Vazgeç</a>
        <button className="button button-primary" type="submit" disabled={saving}>{saving ? <span className="spinner" /> : <Icon name="check" size={18} />}{saving ? 'Kaydediliyor…' : editing ? 'Değişiklikleri kaydet' : 'Aracı kaydet'}</button>
      </div></div>
    </form>
  );
}
