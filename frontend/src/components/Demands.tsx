import { useEffect, useState, type FormEvent } from 'react';
import { api, fmt, type Demand, type DemandInput, type TeamFunction, type TeamMember } from '../api';
import { useAuth } from '../auth';
import {
  Alert, CollapseToggle, Empty, Field, Loading, Modal, errorMessage, fieldErrors, useAsync, useCollapsed,
} from './ui';

/**
 * Demandas do evento, na página do evento (não no formulário de criação).
 * Informação interna: aparece para a equipe e quem a gerencia; só quem edita o evento altera.
 */
export function DemandsSection({ eventId, team, canEdit }: {
  eventId: number; team: TeamMember[]; canEdit: boolean;
}) {
  const list = useAsync(() => api.demands(eventId), [eventId]);
  const [collapsed, toggleCollapsed] = useCollapsed('demands');
  const [editing, setEditing] = useState<Demand | 'new' | null>(null);
  const [assigning, setAssigning] = useState<Demand | null>(null);
  const [completingId, setCompletingId] = useState<number | null>(null);
  const [onlyMine, setOnlyMine] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { user } = useAuth();

  const all = list.data ?? [];
  const mineCount = all.filter((d) => d.assignedToMe).length;
  const items = onlyMine ? all.filter((d) => d.assignedToMe) : all;
  const completing = all.find((d) => d.id === completingId) ?? null;
  const showActions = canEdit || all.some((d) => d.assignedToMe);

  async function remove(d: Demand) {
    if (!confirm(`Excluir a demanda "${d.title}"?`)) return;
    setError(null);
    try {
      await api.deleteDemand(eventId, d.id);
      list.setData((prev) => prev?.filter((i) => i.id !== d.id) ?? null);
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  function replace(updated: Demand) {
    list.setData((prev) => prev?.map((d) => (d.id === updated.id ? updated : d)) ?? null);
  }

  const count = list.data ? (all.length === 1 ? '1 demanda' : `${all.length} demandas`) : '';

  return (
    <section className="section">
      <div className="section__head">
        <h2>Demandas {count && <span className="count">{count}</span>}</h2>
        <div className="section__actions">
          {!collapsed && list.data && (
            <button type="button" className={`btn btn--sm ${onlyMine ? 'btn--toggle-on' : 'btn--ghost'}`}
                    aria-pressed={onlyMine} onClick={() => setOnlyMine(!onlyMine)}>
              Minhas Demandas <span className="btn__count">{mineCount}</span>
            </button>
          )}
          {!collapsed && canEdit && (
            <button type="button" className="btn btn--primary btn--sm" onClick={() => setEditing('new')}>
              Adicionar demanda
            </button>
          )}
          <CollapseToggle collapsed={collapsed} onToggle={toggleCollapsed} controls="demands-body" label="demandas" />
        </div>
      </div>

      <div id="demands-body" className="section" hidden={collapsed}>
        {error && <Alert>{error}</Alert>}
        {list.error && <Alert>{list.error}</Alert>}
        {onlyMine && list.data && (
          <p className="muted">
            Mostrando só as demandas atribuídas a você, diretamente ou pela sua função na equipe.{' '}
            <button type="button" className="link-btn" onClick={() => setOnlyMine(false)}>Ver todas</button>
          </p>
        )}
        {list.loading && !list.data ? <Loading /> : list.data && onlyMine && items.length === 0 ? (
          <Empty title="Nenhuma demanda atribuída a você neste evento." />
        ) : list.data && items.length === 0 ? (
          <Empty title="Nenhuma demanda cadastrada.">
            <p>{canEdit
              ? 'Cadastre o que precisa ser entregue e atribua a funções ou pessoas da equipe.'
              : 'Quando a coordenação cadastrar demandas, elas aparecem aqui.'}</p>
          </Empty>
        ) : list.data && (
          <div className="table-wrap">
            <table className="table demands-table">
              <thead>
                <tr>
                  <th>Entrega</th>
                  <th>Demanda</th>
                  <th>Atribuída a</th>
                  {showActions && <th><span className="sr-only">Ações</span></th>}
                </tr>
              </thead>
              <tbody>
                {items.map((d) => (
                  <tr key={d.id}>
                    <td className="nowrap tabular" title={fmt.plainDate(d.dueDate)}>{fmt.shortDate(d.dueDate)}</td>
                    <td>
                      <div className="demands-table__main">
                        <span className="demands-table__title">{d.title}</span>
                        {d.description && <span className="demands-table__desc">{d.description}</span>}
                      </div>
                    </td>
                    <td>
                      <Assignees demand={d} />
                      <Progress demand={d} />
                    </td>
                    {showActions && (
                      <td className="table__actions">
                        {(canEdit || d.assignedToMe) && (
                          <button type="button" className="btn btn--ghost btn--sm" disabled={d.assignees.length === 0}
                                  title={d.assignees.length === 0 ? 'Atribua a demanda antes de informar a conclusão' : undefined}
                                  onClick={() => setCompletingId(d.id)}>
                            Informar conclusão
                          </button>
                        )}
                        {canEdit && (
                          <>
                            <button type="button" className="btn btn--ghost btn--sm" onClick={() => setAssigning(d)}>Atribuir</button>
                            <button type="button" className="btn btn--ghost btn--sm" onClick={() => setEditing(d)}>Editar</button>
                            <button type="button" className="btn btn--text btn--sm" onClick={() => remove(d)}>Excluir</button>
                          </>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {editing && (
        <DemandModal
          eventId={eventId}
          demand={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => { setEditing(null); list.reload(); }}
        />
      )}

      {completing && (
        <CompletionModal
          eventId={eventId}
          demand={completing}
          canEdit={canEdit}
          myId={user?.id ?? null}
          onChange={replace}
          onClose={() => setCompletingId(null)}
        />
      )}

      {assigning && (
        <AssignModal
          eventId={eventId}
          demand={assigning}
          team={team}
          onClose={() => setAssigning(null)}
          onSaved={(updated) => { replace(updated); setAssigning(null); }}
        />
      )}
    </section>
  );
}

function Progress({ demand }: { demand: Demand }) {
  const total = demand.assignees.length;
  if (total === 0) return null;
  const all = demand.completedCount === total;
  return (
    <div className="demand-progress">
      <span className={all ? 'demand-progress__done' : 'muted'}>
        {all ? 'Concluída por todos' : `${demand.completedCount} de ${total} ${total === 1 ? 'concluiu' : 'concluíram'}`}
      </span>
      {demand.assignedToMe && (
        <span className={`badge ${demand.completedByMe ? 'badge--present' : 'badge--absent'}`}>
          Você: {demand.completedByMe ? 'concluído' : 'pendente'}
        </span>
      )}
    </div>
  );
}

function CompletionModal({ eventId, demand, canEdit, myId, onChange, onClose }: {
  eventId: number; demand: Demand; canEdit: boolean; myId: number | null;
  onChange: (d: Demand) => void; onClose: () => void;
}) {
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function set(userId: number, completed: boolean) {
    setBusyId(userId);
    setError(null);
    try {
      onChange(await api.setDemandCompletion(eventId, demand.id, userId, completed));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusyId(null);
    }
  }

  const total = demand.assignees.length;

  return (
    <Modal title="Informar conclusão" onClose={onClose} wide>
      <div className="stack">
        <div>
          <p className="attendance__meeting">{demand.title}</p>
          <p className="muted">
            Entrega em {fmt.plainDate(demand.dueDate)} · <strong>{demand.completedCount} de {total} concluíram</strong>
          </p>
        </div>
        {!canEdit && <p className="field__hint">Você pode informar apenas a sua própria conclusão.</p>}
        {error && <Alert>{error}</Alert>}

        <ul className="attendance">
          {demand.assignees.map((a) => {
            const allowed = canEdit || a.userId === myId;
            const busy = busyId === a.userId;
            return (
              <li key={a.userId} className="attendance__row">
                <div className="attendance__main">
                  <div className="attendance__who">
                    <span className="attendance__name">{a.name}{a.userId === myId && <span className="you">você</span>}</span>
                    <span className="muted">{a.via.join(' · ')}</span>
                  </div>
                  {allowed ? (
                    <div className="seg" role="group" aria-label={`Situação de ${a.name}`}>
                      <button type="button" className="seg__btn seg__btn--pending" aria-pressed={!a.completed}
                              disabled={busy} onClick={() => a.completed && set(a.userId, false)}>Pendente</button>
                      <button type="button" className="seg__btn seg__btn--done" aria-pressed={a.completed}
                              disabled={busy} onClick={() => !a.completed && set(a.userId, true)}>Concluído</button>
                    </div>
                  ) : (
                    <span className={`badge ${a.completed ? 'badge--present' : 'badge--absent'}`}>
                      {a.completed ? 'Concluído' : 'Pendente'}
                    </span>
                  )}
                </div>
                {a.completed && a.completedAt && (
                  <p className="attendance__when">Concluído em {new Date(a.completedAt).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}</p>
                )}
              </li>
            );
          })}
        </ul>

        <div className="actions">
          <button type="button" className="btn btn--ghost" onClick={onClose}>Fechar</button>
        </div>
      </div>
    </Modal>
  );
}

function Assignees({ demand }: { demand: Demand }) {
  if (demand.functions.length === 0 && demand.users.length === 0) {
    return <span className="muted">Ninguém</span>;
  }
  return (
    <ul className="chips">
      {demand.functions.map((f) => (
        <li key={`f${f.id}`} className="chip chip--fn"
            title={f.members.length ? f.members.join(', ') : 'Ninguém da equipe exerce esta função'}>
          {f.name} · {f.members.length === 1 ? '1 pessoa' : `${f.members.length} pessoas`}
        </li>
      ))}
      {demand.users.map((u) => <li key={`u${u.id}`} className="chip">{u.name}</li>)}
    </ul>
  );
}

function DemandModal({ eventId, demand, onClose, onSaved }: {
  eventId: number; demand: Demand | null; onClose: () => void; onSaved: () => void;
}) {
  const [form, setForm] = useState<DemandInput>({
    title: demand?.title ?? '',
    description: demand?.description ?? '',
    dueDate: demand?.dueDate ?? '',
  });
  const [error, setError] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setFields({});
    try {
      if (demand) await api.updateDemand(eventId, demand.id, form);
      else await api.createDemand(eventId, form);
      onSaved();
    } catch (err) {
      setError(errorMessage(err));
      setFields(fieldErrors(err));
      setBusy(false);
    }
  }

  return (
    <Modal title={demand ? 'Editar demanda' : 'Adicionar demanda'} onClose={onClose}>
      <form className="stack" onSubmit={submit}>
        {error && <Alert>{error}</Alert>}
        <Field label="Título" error={fields.title}>
          <input required maxLength={160} value={form.title} data-autofocus
                 onChange={(e) => setForm({ ...form, title: e.target.value })} />
        </Field>
        <Field label="Descrição" error={fields.description} hint="O que precisa ser feito (opcional)">
          <textarea rows={4} maxLength={2000} value={form.description}
                    onChange={(e) => setForm({ ...form, description: e.target.value })} />
        </Field>
        <Field label="Data de entrega" error={fields.dueDate}>
          <input type="date" required value={form.dueDate}
                 onChange={(e) => setForm({ ...form, dueDate: e.target.value })} />
        </Field>
        <p className="field__hint">Depois de salvar, use o botão Atribuir para escolher os responsáveis.</p>
        <div className="actions">
          <button className="btn btn--primary" disabled={busy}>
            {busy ? 'Salvando…' : demand ? 'Salvar alterações' : 'Adicionar demanda'}
          </button>
          <button type="button" className="btn btn--ghost" onClick={onClose}>Cancelar</button>
        </div>
      </form>
    </Modal>
  );
}

function AssignModal({ eventId, demand, team, onClose, onSaved }: {
  eventId: number; demand: Demand; team: TeamMember[]; onClose: () => void; onSaved: (d: Demand) => void;
}) {
  const [functions, setFunctions] = useState<TeamFunction[] | null>(null);
  const [functionIds, setFunctionIds] = useState<number[]>(demand.functions.map((f) => f.id));
  const [userIds, setUserIds] = useState<number[]>(demand.users.map((u) => u.id));
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [search, setSearch] = useState('');

  useEffect(() => {
    api.teamFunctions().then(setFunctions).catch((e) => setError(errorMessage(e)));
  }, []);

  const term = search.trim().toLowerCase();
  const people = team.filter((m) => !term
    || m.userName.toLowerCase().includes(term) || m.userEmail.toLowerCase().includes(term));

  const toggle = (list: number[], id: number, on: boolean) => (on ? [...list, id] : list.filter((x) => x !== id));

  /** Pessoas já cobertas por uma função marcada: não precisam ser marcadas de novo. */
  const coveredBy = (m: TeamMember) => (functionIds.includes(m.functionId) ? m.functionName : null);

  async function save() {
    setBusy(true);
    setError(null);
    try {
      onSaved(await api.assignDemand(eventId, demand.id, { functionIds, userIds }));
    } catch (e) {
      setError(errorMessage(e));
      setBusy(false);
    }
  }

  const total = functionIds.length + userIds.length;

  return (
    <Modal title="Atribuir demanda" onClose={onClose} wide>
      <div className="stack">
        <div>
          <p className="attendance__meeting">{demand.title}</p>
          <p className="muted">Entrega em {fmt.plainDate(demand.dueDate)}</p>
        </div>
        {error && <Alert>{error}</Alert>}

        <div className="assign-grid">
          <fieldset className="perm-list">
            <legend>Por função</legend>
            <p className="field__hint">Vale para todos que exercem a função na equipe deste evento, inclusive quem entrar depois.</p>
            {!functions ? <Loading /> : functions.length === 0 ? (
              <p className="muted">Nenhuma função cadastrada.</p>
            ) : functions.map((f) => {
              const members = team.filter((m) => m.functionId === f.id);
              return (
                <label key={f.id} className="check check--top">
                  <input type="checkbox" checked={functionIds.includes(f.id)}
                         onChange={(e) => setFunctionIds(toggle(functionIds, f.id, e.target.checked))} />
                  <span>
                    {f.name}
                    <span className="check__hint">
                      {members.length ? members.map((m) => m.userName).join(', ') : 'Ninguém da equipe exerce esta função'}
                    </span>
                  </span>
                </label>
              );
            })}
          </fieldset>

          <fieldset className="perm-list">
            <legend>Por pessoa</legend>
            <p className="field__hint">Pessoas da equipe deste evento.</p>
            {team.length > 0 && (
              <input type="search" placeholder="Buscar por nome ou e-mail" aria-label="Buscar pessoa"
                     value={search} onChange={(e) => setSearch(e.target.value)} />
            )}
            {team.length === 0 ? (
              <p className="muted">A equipe deste evento está vazia.</p>
            ) : people.length === 0 ? (
              <p className="muted">Ninguém da equipe corresponde a “{search.trim()}”.</p>
            ) : people.map((m) => {
              const via = coveredBy(m);
              return (
                <label key={m.userId} className="check check--top">
                  <input type="checkbox" checked={userIds.includes(m.userId)}
                         onChange={(e) => setUserIds(toggle(userIds, m.userId, e.target.checked))} />
                  <span>
                    {m.userName}
                    <span className="check__hint">{via ? `Já incluída pela função ${via}` : m.functionName}</span>
                  </span>
                </label>
              );
            })}
          </fieldset>
        </div>

        <div className="actions">
          <button type="button" className="btn btn--primary" disabled={busy || !functions} onClick={save}>
            {busy ? 'Salvando…' : total === 0 ? 'Salvar sem responsáveis' : 'Salvar atribuição'}
          </button>
          <button type="button" className="btn btn--ghost" onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </Modal>
  );
}
