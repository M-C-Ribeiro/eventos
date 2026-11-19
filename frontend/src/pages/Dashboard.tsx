import { api, fmt } from '../api';
import { useAuth } from '../auth';
import { Alert, CapacityMeter, DateStub, Empty, Loading, useAsync } from '../components/ui';
import { Link } from '../router';

export function DashboardPage() {
  const { user, can } = useAuth();
  const canCreate = can('EVENT_CREATE');
  const { data, error, loading } = useAsync(() => api.dashboard(), []);

  const firstName = user?.name.split(' ')[0] ?? '';

  return (
    <div className="page">
      <header className="page__head">
        <div>
          <h1>Olá, {firstName}</h1>
          <p className="muted">O que está acontecendo em {user?.tenant.name}.</p>
        </div>
        {canCreate && <Link to="/events/new" className="btn btn--primary">Criar evento</Link>}
      </header>

      {error && <Alert>{error}</Alert>}
      {loading && !data && <Loading />}

      {data && (
        <>
          <dl className="stats">
            <div className="stat"><dt>Eventos futuros publicados</dt><dd>{data.upcomingEvents}</dd></div>
            <div className="stat"><dt>Rascunhos</dt><dd>{data.draftEvents}</dd></div>
            <div className="stat"><dt>Inscrições ativas</dt><dd>{data.activeRegistrations}</dd></div>
            <div className="stat"><dt>Check-ins feitos</dt><dd>{data.checkIns}</dd></div>
            <div className="stat"><dt>Usuários</dt><dd>{data.users}</dd></div>
          </dl>

          <section className="section">
            <div className="section__head">
              <h2>Próximos eventos</h2>
              <Link to="/events">Ver todos</Link>
            </div>
            {data.nextEvents.length === 0 ? (
              <Empty title="Nenhum evento publicado à frente.">
                {canCreate
                  ? <p>Crie um evento e publique-o para que ele apareça aqui.</p>
                  : <p>Quando um organizador publicar um evento, ele aparece aqui.</p>}
              </Empty>
            ) : (
              <ul className="ticket-list">
                {data.nextEvents.map((e) => (
                  <li key={e.id}>
                    <Link to={`/events/${e.id}`} className="ticket">
                      <DateStub iso={e.startAt} />
                      <div className="ticket__body">
                        <span className="ticket__title">{e.title}</span>
                        <span className="ticket__meta">
                          {fmt.time(e.startAt)}{e.location ? ` · ${e.location}` : ''}
                        </span>
                      </div>
                      <div className="ticket__side">
                        <CapacityMeter count={e.confirmedCount} capacity={e.capacity} />
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </div>
  );
}
