import { useCallback } from 'react';
import { useResource } from '../hooks/useResource';
import { getVehicleMaintenancePlan } from '../services/vehicleService';
import { formatDate } from '../utils/appointment';
import { formatNumber } from '../utils/vehicle';

const statusLabels = {
  ON_TRACK: 'Bakım planı normal',
  APPROACHING: 'Bakım yaklaşıyor',
  DUE: 'Bakım zamanı geldi',
  OVERDUE: 'Bakım gecikti',
};

function formatDays(days) {
  if (days < 0) return `${formatNumber(-days)} gün geçti`;
  if (days === 0) return 'Bakım tarihi bugün';
  return `${formatNumber(days)} gün kaldı`;
}

function formatKilometers(kilometers) {
  if (kilometers < 0) return `${formatNumber(-kilometers)} km aşıldı`;
  if (kilometers === 0) return 'Kilometre sınırına ulaşıldı';
  return `${formatNumber(kilometers)} km kaldı`;
}

export default function MaintenancePlanPanel({ vehicleId, scope }) {
  const loader = useCallback(
    (signal) => getVehicleMaintenancePlan(vehicleId, scope, signal),
    [vehicleId, scope],
  );
  const { data: plan, loading, error, retry } = useResource(loader);
  const schedule = plan?.schedule;

  return (
    <section className="detail-panel maintenance-plan" aria-labelledby="maintenance-plan-title">
      <div className="section-heading">
        <h2 id="maintenance-plan-title">Periyodik bakım</h2>
        {!loading && !error && schedule && (
          <span className="maintenance-status" data-status={schedule.status}>
            {statusLabels[schedule.status]}
          </span>
        )}
      </div>

      {loading && <p className="section-description" role="status">Bakım planı yükleniyor…</p>}
      {!loading && error && (
        <div>
          <p className="section-description" role="alert">Bakım planı yüklenemedi. {error.message}</p>
          <button type="button" className="button button-secondary" onClick={retry}>Tekrar dene</button>
        </div>
      )}

      {!loading && !error && plan && (
        <>
          <p className="section-description">
            Sistem bakım aralığı {formatNumber(plan.intervalMonths)} ay / {formatNumber(plan.intervalKilometers)} km.
            {' '}İlk dolan sınır esas alınır.
          </p>
          {!schedule ? (
            <p className="section-description">Bakım planı için bu araca bir bakım kaydı eklenmeli.</p>
          ) : (
            <>
              <dl className="detail-specs">
                <div>
                  <dt>Son bakım</dt>
                  <dd>{formatDate(schedule.lastMaintenanceDate)} · {formatNumber(schedule.lastMaintenanceMileage)} km</dd>
                </div>
                <div>
                  <dt>Sonraki bakım tarihi</dt>
                  <dd><strong>{formatDate(schedule.nextMaintenanceDate)}</strong><small>{formatDays(schedule.daysUntilMaintenance)}</small></dd>
                </div>
                <div>
                  <dt>Sonraki bakım kilometresi</dt>
                  <dd><strong>{formatNumber(schedule.nextMaintenanceMileage)} km</strong><small>{formatKilometers(schedule.kilometersUntilMaintenance)}</small></dd>
                </div>
              </dl>
              <p className="section-description">
                Hatırlatma, bakıma {formatNumber(plan.reminderDays)} gün veya {formatNumber(plan.reminderKilometers)} km kaldığında başlar.
              </p>
            </>
          )}
        </>
      )}
    </section>
  );
}
