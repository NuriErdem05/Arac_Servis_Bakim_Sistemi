import { useEffect, useState } from 'react';
import { getVehicle, getVehicles, getServiceVehicle, getServiceVehicles } from '../services/vehicleService';

// id yoksa liste, varsa tek araç yüklenir. Yeniden deneme de aynı akışı kullanır.
export function useVehicles(id, scope = 'owner') {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const list = scope === 'service' ? getServiceVehicles : getVehicles;
        const detail = scope === 'service' ? getServiceVehicle : getVehicle;
        const result = id === undefined
          ? await list(controller.signal)
          : await detail(id, controller.signal);
        if (!controller.signal.aborted) setData(result);
      } catch (failure) {
        if (!controller.signal.aborted) setError(failure);
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    load();
    return () => controller.abort();
  }, [id, scope, attempt]);

  return { data, loading, error, retry: () => setAttempt((value) => value + 1) };
}
