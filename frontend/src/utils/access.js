export const ROLE_LABELS = {
  ADMIN: 'Yönetici',
  STAFF: 'Personel',
  USER: 'Kullanıcı',
};

const navigation = {
  ADMIN: [
    ['/dashboard', 'Genel bakış', ['dashboard']],
    ['/service/vehicles', 'Servis araçları', ['list', 'detail', 'edit', 'history']],
    ['/appointments', 'Randevular', ['appointments', 'appointment-create', 'appointment-detail']],
    ['/service-records', 'Servis kayıtları', ['records', 'record-create', 'record-detail']],
    ['/reports', 'Servis özetleri', ['reports']],
    ['/accounts', 'Hesaplar', ['accounts', 'account-create']],
  ],
  STAFF: [
    ['/appointments', 'Atanan randevular', ['appointments', 'appointment-detail', 'record-create', 'history']],
  ],
  USER: [
    ['/vehicles', 'Araçlarım', ['list', 'detail', 'edit', 'history', 'create']],
    ['/appointments', 'Randevularım', ['appointments', 'appointment-create', 'appointment-detail']],
  ],
};

export function getNavigation(role) {
  return navigation[role];
}

export function getHomePath(role) {
  return getNavigation(role)[0][0];
}

export function canOpenRoute(role, { page, scope }) {
  if (page === 'not-found') return true;
  if (['list', 'detail', 'edit', 'history'].includes(page)) {
    const allowedScope = role === 'USER' ? 'owner' : 'service';
    if (scope !== allowedScope) return false;
  }
  return getNavigation(role).some(([, , pages]) => pages.includes(page));
}
