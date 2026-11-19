import { useState, type FormEvent } from 'react';
import { api, fmt, type Permission, type Registration, type TeamMember } from '../api';
import {
  Alert, CapacityMeter, DateStub, Empty, EventStatusBadge, Field, Loading, Modal, RegStatusBadge,
  CollapseToggle, errorMessage, fieldErrors, useAsync, useCollapsed,
} from '../components/ui';
import { DemandsSection } from '../components/Demands';
import { MeetingsSection } from '../components/Meetings';
import { ScheduleSection } from '../components/Schedule';
import { useAuth } from '../auth';
import { Link, navigate } from '../router';

export function EventDetailPage({ id }: { id: number }) {
  const { user, can } = useAuth();
  const event = useAsync(() => api.event(id), [id]);
  const [actionError, setActionError] = useState<string | null>(null);

  async function remove() {
    if (!event.data || !confirm(`Excluir "${event.data.title}" e todas as inscrições? Isso não pode ser desfeito.`)) return;
    try {
      await api.deleteEvent(id);
      navigate('/events', true);
    } catch (e) {
      setActionError(errorMessage(e));
    }
  }

  if (event.loading && !event.data) return <div className="page"><Loading /></div>;
  if (event.error || !event.data) {
    return (
      <div className="page">
        <Link to="/events" className="back">Voltar</Link>
        <Alert>{event.error ?? 'Evento não encontrado.'}</Alert>
      </div>
    );
  }

  const e = event.data;
  const perms = new Set<Permission>(e.myPermissions ?? []);
  const canEditSomething = perms.has('EVENT_EDIT') || perms.has('TEAM_MANAGE');
  // Mesma regra do backend (EventAccess.canViewInternal): reuniões são só para a equipe e quem a gerencia.
  const canSeeInternal = canEditSomething || can('EVENT_VIEW_ALL')
    || (e.team ?? []).some((m) => m.userId === user?.id);
  return (
    <div className="page">
      <Link to="/events" className="back">Eventos</Link>

      <header className="event-hero">
        <DateStub iso={e.startAt} size="lg" />
        <div className="event-hero__body">
          <EventStatusBadge status={e.status} />
          <h1>{e.title}</h1>
          <p className="event-hero__when">{fmt.range(e.startAt, e.endAt)}</p>
          {e.location && <p className="event-hero__where">{e.location}</p>}
          <p className="muted">Organizado por {e.organizer.name}</p>
        </div>
        {(canEditSomething || perms.has('EVENT_DELETE')) && (
          <div className="event-hero__actions">
            {canEditSomething && (
              <Link to={`/events/${id}/edit`} className="btn btn--ghost">
                {perms.has('EVENT_EDIT') ? 'Editar' : 'Editar equipe'}
              </Link>
            )}
            {perms.has('EVENT_DELETE') && (
              <button type="button" className="btn btn--danger-ghost" onClick={remove}>Excluir</button>
            )}
          </div>
        )}
      </header>

      {actionError && <Alert>{actionError}</Alert>}

      <div className="event-grid">
        <section className="panel">
          <h2>Sobre</h2>
          {e.description
            ? <p className="prose">{e.description}</p>
            : <p className="muted">Sem descrição.</p>}
          <div className="divider" />
          <CapacityMeter count={e.confirmedCount} capacity={e.capacity} />
        </section>

        <div className="stack">
          <TeamPanel team={e.team ?? []} />
        </div>
      </div>

      <ScheduleSection eventId={id} eventStart={e.startAt} eventEnd={e.endAt} canEdit={perms.has('EVENT_EDIT')} />

      {canSeeInternal && <MeetingsSection eventId={id} canEdit={perms.has('EVENT_EDIT')} />}

      {canSeeInternal && <DemandsSection eventId={id} team={e.team ?? []} canEdit={perms.has('EVENT_EDIT')} />}

      {perms.has('REGISTRATION_VIEW') && (
        <Registrations eventId={id} closed={e.status === 'CANCELLED' || e.status === 'COMPLETED'}
                       perms={perms} onChange={event.reload} />
      )}
    </div>
  );
}

function TeamPanel({ team }: { team: TeamMember[] }) {
  const [collapsed, toggleCollapsed] = useCollapsed('team');
  return (
    <section className="panel">
      <div className="collapse-head">
        <h2>Equipe <span className="count">{team.length === 1 ? '1 pessoa' : `${team.length} pessoas`}</span></h2>
        <CollapseToggle collapsed={collapsed} onToggle={toggleCollapsed} controls="detail-team-body" label="equipe" />
      </div>
      <div id="detail-team-body" hidden={collapsed}>
        {team.length === 0 ? <p className="muted">Ninguém na equipe ainda.</p> : (
          <ul className="team-list">
            {team.map((m) => (
              <li key={m.id}>
                <span>{m.userName}</span>
                <span className="badge badge--fn">{m.functionName}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}

function Registrations({ eventId, closed, perms, onChange }: {
  eventId: number; closed: boolean; perms: Set<Permission>; onChange: () => void;
}) {
  const canManage = perms.has('REGISTRATION_MANAGE');
  const canCheckIn = perms.has('CHECKIN');
  const list = useAsync(() => api.registrations(eventId), [eventId]);
  const [adding, setAdding] = useState(false);
  const [filter, setFilter] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [collapsed, toggleCollapsed] = useCollapsed('registrations');

  async function act(fn: () => Promise<Registration>) {
    setError(null);
    try {
      const updated = await fn();
      list.setData((prev) => prev?.map((r) => (r.id === updated.id ? updated : r)) ?? null);
      onChange();
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  const rows = (list.data ?? []).filter((r) => {
    const f = filter.trim().toLowerCase();
    return !f || r.name.toLowerCase().includes(f) || r.email.toLowerCase().includes(f);
  });
  const present = list.data?.filter((r) => r.status === 'CHECKED_IN').length ?? 0;
  const active = list.data?.filter((r) => r.status !== 'CANCELLED').length ?? 0;

  return (
    <section className="section">
      <div className="section__head">
        <h2>Participantes <span className="count">{present} de {active} presentes</span></h2>
        <div className="section__actions">
          {!collapsed && !closed && canManage && (
            <button type="button" className="btn btn--primary btn--sm" onClick={() => setAdding(true)}>Adicionar participante</button>
          )}
          <CollapseToggle collapsed={collapsed} onToggle={toggleCollapsed} controls="registrations-body" label="participantes" />
        </div>
      </div>

      <div id="registrations-body" className="section" hidden={collapsed}>
      {error && <Alert>{error}</Alert>}
      {list.error && <Alert>{list.error}</Alert>}

      {list.data && list.data.length > 0 && (
        <input type="search" className="toolbar__search" placeholder="Filtrar por nome ou e-mail"
               aria-label="Filtrar participantes" value={filter} onChange={(e) => setFilter(e.target.value)} />
      )}

      {list.loading && !list.data ? <Loading /> : list.data && list.data.length === 0 ? (
        <Empty title="Ninguém inscrito ainda.">
          <p>{canManage ? 'Adicione participantes aqui ou peça' : 'Peça'} que as pessoas se inscrevam pela página do evento.</p>
        </Empty>
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr><th>Nome</th><th>E-mail</th><th>Situação</th><th><span className="sr-only">Ações</span></th></tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id} className={r.status === 'CANCELLED' ? 'is-muted' : undefined}>
                  <td>{r.name}</td>
                  <td>{r.email}</td>
                  <td><RegStatusBadge status={r.status} /></td>
                  <td className="table__actions">
                    {canCheckIn && r.status === 'CONFIRMED' && (
                      <button type="button" className="btn btn--checkin btn--sm"
                              onClick={() => act(() => api.checkIn(eventId, r.id))}>Fazer check-in</button>
                    )}
                    {canCheckIn && r.status === 'CHECKED_IN' && (
                      <button type="button" className="btn btn--ghost btn--sm"
                              onClick={() => act(() => api.undoCheckIn(eventId, r.id))}>Desfazer check-in</button>
                    )}
                    {canManage && r.status !== 'CANCELLED' && (
                      <button type="button" className="btn btn--text btn--sm"
                              onClick={() => confirm(`Cancelar a inscrição de ${r.name}?`) && act(() => api.cancelRegistration(eventId, r.id))}>
                        Cancelar
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      </div>

      {adding && (
        <AddParticipant eventId={eventId} onClose={() => setAdding(false)}
                        onAdded={() => { setAdding(false); list.reload(); onChange(); }} />
      )}
    </section>
  );
}

function AddParticipant({ eventId, onClose, onAdded }: { eventId: number; onClose: () => void; onAdded: () => void }) {
  const [form, setForm] = useState({ name: '', email: '' });
  const [error, setError] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api.addRegistration(eventId, form);
      onAdded();
    } catch (err) {
      setError(errorMessage(err));
      setFields(fieldErrors(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title="Adicionar participante" onClose={onClose}>
      <form className="stack" onSubmit={submit}>
        {error && <Alert>{error}</Alert>}
        <Field label="Nome" error={fields.name}>
          <input required autoFocus value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        </Field>
        <Field label="E-mail" error={fields.email}>
          <input type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </Field>
        <div className="actions">
          <button className="btn btn--primary" disabled={busy}>{busy ? 'Adicionando…' : 'Adicionar'}</button>
          <button type="button" className="btn btn--ghost" onClick={onClose}>Cancelar</button>
        </div>
      </form>
    </Modal>
  );
}
