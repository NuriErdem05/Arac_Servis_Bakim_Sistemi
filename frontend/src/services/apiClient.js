export class ApiError extends Error {
  constructor(message, status = 0, fieldErrors = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = normalizeFieldErrors(fieldErrors);
  }
}

function normalizeFieldErrors(errors) {
  if (!errors || typeof errors !== 'object') return {};
  if (Array.isArray(errors)) {
    return Object.fromEntries(errors
      .filter((item) => item && (item.field || item.fieldName))
      .map((item) => [item.field || item.fieldName, String(item.message || item.defaultMessage || 'Geçersiz değer.')]));
  }
  return Object.fromEntries(Object.entries(errors).map(([field, message]) => [
    field,
    Array.isArray(message) ? message.join(' ') : String(message ?? 'Geçersiz değer.'),
  ]));
}

export async function apiRequest(path, { method = 'GET', body, signal } = {}) {
  try {
    const response = await fetch(`/api${path}`, {
      method,
      signal,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    });

    const text = await response.text();
    let data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch {
        if (response.ok) {
          throw new ApiError('Sunucudan beklenen veri alınamadı. Lütfen tekrar deneyin.', response.status);
        }
      }
    }

    if (!response.ok) {
      const message = typeof data?.message === 'string'
        ? data.message
        : `İşlem tamamlanamadı. Lütfen tekrar deneyin. (${response.status})`;
      throw new ApiError(message, response.status, data?.fieldErrors);
    }

    return data;
  } catch (error) {
    if (error.name === 'AbortError' || error instanceof ApiError) throw error;
    throw new ApiError('Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin.');
  }
}
