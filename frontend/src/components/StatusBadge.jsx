import { STATUS_LABELS } from '../utils/appointment';

export default function StatusBadge({ status }) {
  return <span className={'appointment-status status-' + String(status).toLowerCase()}>{STATUS_LABELS[status] || 'Bilinmiyor'}</span>;
}
