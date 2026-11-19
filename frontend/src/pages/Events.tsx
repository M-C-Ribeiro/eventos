import { useEffect, useState } from 'react';
import { api, EVENT_STATUS_LABEL, fmt, type EventStatus } from '../api';
import { useAuth } from '../auth';
import { Alert, CapacityMeter, DateStub, Empty, EventStatusBadge, Loading, useAsync } from '../components/ui';
import { Link } from '../router';

export function EventsPage() {
  const { can } = useAuth();
  const canCreate = can('EVENT_CREATE');
  const canSeeDrafts = can('EVENT_VIEW_ALL') || canCreate;
  const [status, setStatus] = useState<EventStatus | ''>('');
  const [upcoming, setUpcoming] = useState(true);
  const [search, setSearch] = useState('');
  const [q, setQ] = useState('');

  // Busca só depois que a pessoa para de digitar.
  useEffect(() => {
    const t = setTimeout(() => setQ(search.trim()), 300);
    return () => clearTimeout(t);
  }, [search]);

  const { data, error, loading } = useAsync(() => api.events({ status, q, upcoming }), [status, q, upcoming]);
  const filtered = status !== '' || q !== '';

  return (
    <div className="page">
      <header className="page__head">
        <div>
          <h1>Eventos</h1>
          <p className="muted">{upcoming ? 'Em andamento e futuros' : 'Todos, incluindo os que já passaram'}</p>
        </div>
        {canCreate && <Link to="/events/new" className="btn btn--primary">Criar evento</Link>}
      </header>

      <div className="toolbar">
        <input type="search" className="toolbar__search" placeholder="Buscar por título ou local"
               aria-label="Buscar eventos" value={search} onChange={(e) => setSearch(e.target.value)} />
        <select aria-label="Status" value={status} onChange={(e) => setStatus(e.target.value as EventStatus | '')}>
          <option value="">Todos os status</option>
          {(Object.keys(EVENT_STATUS_LABEL) as EventStatus[])
            .filter((s) => canSeeDrafts || s !== 'DRAFT')
            .map((s) => <option key={s} value={s}>{EVENT_STATUS_LABEL[s]}</option>)}
        </select>
        <label className="check">
          <input type="checkbox" checked={!upcoming} onChange={(e) => setUpcoming(!e.target.checked)} />
          Incluir passados
        </label>
      </div>

      {error && <Alert>{error}</Alert>}
      {loading && !data && <Loading />}

      {data && data.length === 0 && (
        <Empty title={filtered ? 'Nenhum evento corresponde aos filtros.' : 'Ainda não há eventos.'}>
          {filtered
            ? <p>Ajuste a busca ou o status para ver mais resultados.</p>
            : canCreate
              ? <p><Link to="/events/new">Crie o primeiro evento</Link> da organização.</p>
              : <p>Os eventos publicados pela organização aparecem aqui.</p>}
        </Empty>
      )}

      {data && data.length > 0 && (
        <ul className="ticket-list" aria-busy={loading}>
          {data.map((e) => (
            <li key={e.id}>
              <Link to={`/events/${e.id}`} className={`ticket ticket--${e.status.toLowerCase()}`}>
                <DateStub iso={e.startAt} />
                <div className="ticket__body">
                  <span className="ticket__title">{e.title}</span>
                  <span className="ticket__meta">
                    {fmt.time(e.startAt)}{e.location ? ` · ${e.location}` : ''} · {e.organizer.name}
                  </span>
                </div>
                <div className="ticket__side">
                  <EventStatusBadge status={e.status} />
                  <CapacityMeter count={e.confirmedCount} capacity={e.capacity} />
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
