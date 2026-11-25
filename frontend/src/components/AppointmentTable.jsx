import StatusBadge from './StatusBadge';
import { formatDate } from '../utils/appointment';

export default function AppointmentTable({ rows, caption = 'Randevular' }) {
  return <div className="table-scroll" role="region" aria-label={caption} tabIndex={0}><table className="data-table"><caption className="sr-only">{caption}</caption><thead><tr><th scope="col">Tarih ve saat</th><th scope="col">Plaka</th><th scope="col">Talep</th><th scope="col">Personel</th><th scope="col">Durum</th><th scope="col">İşlem</th></tr></thead><tbody>{rows.map((item) => <tr key={item.id}>
    <td><time dateTime={item.appointmentDateTime}>{formatDate(item.appointmentDateTime, true)}</time></td><th scope="row"><span className="plate-text">{item.plate}</span></th><td className="description-cell">{item.requestDescription}</td><td>{item.assignedStaffName || 'Atanmadı'}</td><td><StatusBadge status={item.status} /></td><td><a className="table-link" href={'#/appointments/' + item.id} aria-label={`${item.plate} randevu detayını aç`}>Detay</a></td>
  </tr>)}</tbody></table></div>;
}
