import { useState } from 'react';
import { useVehicles } from '../hooks/useVehicles';
import { filterVehicles } from '../utils/vehicle';
import VehicleCard from '../components/VehicleCard';
import AsyncState from '../components/AsyncState';
import Icon from '../components/Icon';

export default function VehicleListPage({ scope = 'owner' }) {
  const service = scope === 'service';
  const basePath = service ? '/service/vehicles' : '/vehicles';
  const { data, loading, error, retry } = useVehicles(undefined, scope);
  const [search, setSearch] = useState('');
  const [brand, setBrand] = useState('');
  const [sort, setSort] = useState('newest');
  const vehicles = data || [];
  const filtered = filterVehicles(vehicles, search, brand, sort);
  const brands = [...new Set(vehicles.map((vehicle) => vehicle.brand))]
    .sort((a, b) => a.localeCompare(b, 'tr'));

  function clearFilters() {
    setSearch('');
    setBrand('');
  }

  return (
    <section className="page-content">
      <div className="page-heading">
        <div>
          <h1 id="page-title" tabIndex={-1}>{service ? 'Servis araçları' : 'Araçlarım'}</h1>
          <p className="page-description">{service ? 'Servis genelindeki aktif araçları ve bakım geçmişlerini görüntüleyin.' : 'Kayıtlı araçlarınızı görüntüleyin ve bilgilerini güncelleyin.'}</p>
        </div>
        <div className="button-row"><a className="button button-secondary" href={service ? '#/vehicles' : '#/service/vehicles'}>{service ? 'Araçlarım' : 'Servis araçları'}</a>{service ? <a className="button button-primary" href="#/service-records/new"><Icon name="plus" size={18} />Servis kaydı oluştur</a> : <a className="button button-primary" href="#/vehicles/new"><Icon name="plus" size={18} />Araç ekle</a>}</div>
      </div>
      {loading || error ? <AsyncState loading={loading} error={error} retry={retry} /> : <>
        <div className="collection-toolbar">
          <div className="search-field">
            <label htmlFor="plate-search">Plaka ara</label>
            <div className="search-control"><Icon name="search" size={18} /><input id="plate-search" type="search" placeholder="Örn. 58 ABC 123" value={search} onChange={(event) => setSearch(event.target.value)} /></div>
          </div>
          <div className="filter-control">
            <label htmlFor="brand-filter">Markaya göre filtrele</label>
            <select id="brand-filter" value={brand} onChange={(event) => setBrand(event.target.value)}>
              <option value="">Tüm markalar</option>
              {brands.map((name) => <option key={name} value={name}>{name}</option>)}
            </select>
          </div>
          <div className="filter-control">
            <label htmlFor="sort-order">Araçları sırala</label>
            <select id="sort-order" value={sort} onChange={(event) => setSort(event.target.value)}>
              <option value="newest">Son eklenen</option>
              <option value="plate">Plaka: A–Z</option>
              <option value="mileage">Kilometre: düşükten yükseğe</option>
            </select>
          </div>
        </div>
        <div className="list-caption">
          <p className="result-count" aria-live="polite">{filtered.length} / {vehicles.length} araç gösteriliyor</p>
          {(search || brand) && filtered.length > 0 && <button type="button" className="text-button" onClick={clearFilters}>Filtreleri temizle</button>}
        </div>
        {filtered.length ? <div className="records-panel">
          <div className="records-heading" aria-hidden="true"><span>Plaka</span><span>Araç</span><span>Yıl / Yakıt</span><span>Kilometre</span><span>İşlemler</span></div>
          <ul className="vehicle-records">{filtered.map((vehicle) => <li key={vehicle.id}><VehicleCard vehicle={vehicle} basePath={basePath} /></li>)}</ul>
        </div> : <div className="empty-state">
          <h2>{vehicles.length ? 'Aradığınız araç bulunamadı' : 'Kayıtlı araç bulunmuyor'}</h2>
          <p>{vehicles.length ? 'Plakayı kontrol edin veya filtreleri temizleyin.' : service ? 'Servise kayıtlı aktif bir araç bulunmuyor.' : 'Araç ekle düğmesini kullanarak yeni bir kayıt oluşturabilirsiniz.'}</p>
          {vehicles.length ? <button className="button button-secondary" type="button" onClick={clearFilters}>Filtreleri temizle</button> : !service && <a className="button button-primary" href="#/vehicles/new">Araç ekle</a>}
        </div>}
      </>}
    </section>
  );
}
