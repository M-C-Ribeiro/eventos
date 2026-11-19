import { useEffect, useState, type FormEvent } from 'react';
import {
  api, EVENT_STATUS_LABEL, fmt, type EventInput, type EventItem, type EventStatus, type Permission, type TeamFunction,
  type TeamMember, type TeamMemberInput, type UserOption,
} from '../api';
import { Alert, CollapseToggle, Field, Loading, Modal, errorMessage, fieldErrors, useCollapsed } from '../components/ui';
import { Link, navigate } from '../router';

/** O backend usa LocalDateTime ("2030-05-01T09:00:00"); o input usa "2030-05-01T09:00". */
const toInput = (iso: string) => iso.slice(0, 16);
const toApi = (value: string) => (value.length === 16 ? `${value}:00` : value);

function defaultStart() {
  const d = new Date();
  d.setDate(d.getDate() + 7);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T09:00`;
}

type Fields = Omit<EventInput, 'team'>;

export function EventFormPage({ id }: { id?: number }) {
  const editing = id !== undefined;
  const [form, setForm] = useState<Fields>(() => {
    const start = defaultStart();
    return {
      title: '', description: '', location: '',
      startAt: start, endAt: start.replace('T09:00', 'T18:00'),
      capacity: null, status: 'DRAFT',
    };
  });
  const [team, setTeam] = useState<TeamMemberInput[]>([]);
  const [currentTeam, setCurrentTeam] = useState<TeamMember[]>([]);
  // Ao criar, quem cria é o responsável e pode tudo no evento.
  const [perms, setPerms] = useState<Permission[] | null>(editing ? null : ['EVENT_EDIT', 'TEAM_MANAGE']);
  const [error, setError] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (id === undefined) return;
    api.event(id)
      .then((e) => {
        setForm({
          title: e.title,
          description: e.description ?? '',
          location: e.location ?? '',
          startAt: toInput(e.startAt),
          endAt: toInput(e.endAt),
          capacity: e.capacity,
          status: e.status,
        });
        setCurrentTeam(e.team ?? []);
        setTeam((e.team ?? []).map((m) => ({ userId: m.userId, functionId: m.functionId })));
        setPerms(e.myPermissions ?? []);
      })
      .catch((e) => { setError(errorMessage(e)); setPerms([]); });
  }, [id]);

  const canEdit = !!perms?.includes('EVENT_EDIT');
  const canTeam = !!perms?.includes('TEAM_MANAGE');

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setFields({});
    const body: EventInput = {
      ...form,
      startAt: toApi(form.startAt),
      endAt: toApi(form.endAt),
      team: canTeam ? team : undefined,
    };
    try {
      if (id === undefined) {
        const saved = await api.createEvent(body);
        navigate(`/events/${saved.id}`, true);
      } else {
        if (canEdit) await api.updateEvent(id, body);
        else await api.replaceTeam(id, team);
        navigate(`/events/${id}`, true);
      }
    } catch (err) {
      setError(errorMessage(err));
      setFields(fieldErrors(err));
    } finally {
      setBusy(false);
    }
  }

  if (perms === null) return <div className="page"><Loading /></div>;

  const back = editing ? `/events/${id}` : '/events';

  if (editing && !canEdit && !canTeam) {
    return (
      <div className="page page--narrow">
        <Link to={back} className="back">Voltar</Link>
        <Alert>{error ?? 'Você não tem permissão para alterar este evento.'}</Alert>
      </div>
    );
  }

  return (
    <div className="page page--narrow">
      <header className="page__head">
        <div>
          <Link to={back} className="back">Voltar</Link>
          <h1>{editing ? 'Editar evento' : 'Novo evento'}</h1>
          {editing && !canEdit && (
            <p className="muted">Você pode alterar apenas a equipe deste evento.</p>
          )}
        </div>
      </header>

      <form className="stack" onSubmit={submit}>
        {error && <Alert>{error}</Alert>}

        <fieldset className="panel stack form-section" disabled={!canEdit}>
          <legend className="form-section__title">Dados do evento</legend>
          <Field label="Título" error={fields.title}>
            <input required maxLength={160} value={form.title} autoFocus={!editing}
                   onChange={(e) => setForm({ ...form, title: e.target.value })} />
          </Field>
          <Field label="Descrição" error={fields.description}>
            <textarea rows={5} maxLength={4000} value={form.description}
                      onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </Field>
          <Field label="Local" error={fields.location} hint="Endereço, sala ou link da transmissão">
            <input maxLength={255} value={form.location}
                   onChange={(e) => setForm({ ...form, location: e.target.value })} />
          </Field>
          <div className="grid-2">
            <Field label="Início" error={fields.startAt}>
              <input type="datetime-local" required value={form.startAt}
                     onChange={(e) => setForm({ ...form, startAt: e.target.value })} />
            </Field>
            <Field label="Término" error={fields.endAt}>
              <input type="datetime-local" required value={form.endAt} min={form.startAt}
                     onChange={(e) => setForm({ ...form, endAt: e.target.value })} />
            </Field>
          </div>
          <div className="grid-2">
            <Field label="Vagas" error={fields.capacity} hint="Deixe em branco para não limitar">
              <input type="number" min={1} inputMode="numeric" value={form.capacity ?? ''}
                     onChange={(e) => setForm({ ...form, capacity: e.target.value ? Number(e.target.value) : null })} />
            </Field>
            <Field label="Status" hint="Rascunhos ficam visíveis só para a equipe e quem pode ver todos os eventos">
              <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as EventStatus })}>
                {(Object.keys(EVENT_STATUS_LABEL) as EventStatus[]).map((s) => (
                  <option key={s} value={s}>{EVENT_STATUS_LABEL[s]}</option>
                ))}
              </select>
            </Field>
          </div>
        </fieldset>

        {canTeam
          ? <TeamEditor team={team} onChange={setTeam} error={fields.team} currentEventId={id} />
          : <TeamReadOnly team={currentTeam} />}

        <div className="actions">
          <button className="btn btn--primary" disabled={busy}>
            {busy ? 'Salvando…' : editing ? 'Salvar alterações' : 'Criar evento'}
          </button>
          <Link to={back} className="btn btn--ghost">Cancelar</Link>
        </div>
      </form>
    </div>
  );
}

function TeamEditor({ team, onChange, error, currentEventId }: {
  team: TeamMemberInput[]; onChange: (t: TeamMemberInput[]) => void; error?: string; currentEventId?: number;
}) {
  const [copying, setCopying] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [collapsed, toggleCollapsed] = useCollapsed('team');
  const [people, setPeople] = useState<UserOption[] | null>(null);
  const [functions, setFunctions] = useState<TeamFunction[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [newUser, setNewUser] = useState('');
  const [newFunction, setNewFunction] = useState('');

  useEffect(() => {
    Promise.all([api.userOptions(), api.teamFunctions()])
      .then(([p, f]) => {
        setPeople(p);
        setFunctions(f);
        if (f.length > 0) setNewFunction(String(f[0].id));
      })
      .catch((e) => setLoadError(errorMessage(e)));
  }, []);

  const name = (userId: number) => people?.find((p) => p.id === userId);
  const available = (people ?? []).filter((p) => !team.some((m) => m.userId === p.id));

  function add() {
    if (!newUser || !newFunction) return;
    onChange([...team, { userId: Number(newUser), functionId: Number(newFunction) }]);
    setNewUser('');
  }

  return (
    <section className="panel stack form-section" aria-labelledby="team-title">
      <div className="collapse-head">
        <h2 id="team-title" className="form-section__title">
          Equipe <span className="count">{teamCount(team.length)}</span>
        </h2>
        <CollapseToggle collapsed={collapsed} onToggle={toggleCollapsed} controls="team-body" label="equipe" />
      </div>
      {/* A validação do servidor continua visível mesmo com a seção recolhida. */}
      {error && <Alert>{error}</Alert>}
      <div id="team-body" className="stack" hidden={collapsed}>
      <div className="team-head">
        <p className="field__hint">
          Pessoas da organização que trabalham neste evento. A função define o que cada uma pode fazer aqui.
        </p>
        <button type="button" className="btn btn--ghost btn--sm" disabled={!people || !functions}
                onClick={() => { setNotice(null); setCopying(true); }}>
          Carregar de evento anterior
        </button>
      </div>
      {notice && <Alert tone="ok">{notice}</Alert>}
      {loadError && <Alert>{loadError}</Alert>}
      {!people || !functions ? (!loadError && <Loading />) : (
        <>
          {functions.length === 0 && (
            <Alert>Nenhuma função de equipe cadastrada. Peça a quem administra a organização para criar funções.</Alert>
          )}

          {team.length > 0 && (
            <ul className="team-rows">
              {team.map((m, i) => {
                const person = name(m.userId);
                return (
                  <li key={m.userId} className="team-row">
                    <div className="team-row__who">
                      <span className="team-row__name">{person?.name ?? 'Pessoa desativada'}</span>
                      {person && <span className="muted">{person.email}</span>}
                    </div>
                    <select aria-label={`Função de ${person?.name ?? 'pessoa'}`} value={m.functionId}
                            onChange={(e) => onChange(team.map((t, j) => (j === i ? { ...t, functionId: Number(e.target.value) } : t)))}>
                      {functions.map((f) => <option key={f.id} value={f.id}>{f.name}</option>)}
                    </select>
                    <button type="button" className="btn btn--text btn--sm"
                            onClick={() => onChange(team.filter((_, j) => j !== i))}>Remover</button>
                  </li>
                );
              })}
            </ul>
          )}

          {functions.length > 0 && (
            <div className="team-add">
              <select aria-label="Pessoa" value={newUser} onChange={(e) => setNewUser(e.target.value)}>
                <option value="">{available.length ? 'Escolha uma pessoa' : 'Todos já estão na equipe'}</option>
                {available.map((p) => <option key={p.id} value={p.id}>{p.name} ({p.email})</option>)}
              </select>
              <select aria-label="Função" value={newFunction} onChange={(e) => setNewFunction(e.target.value)}>
                {functions.map((f) => <option key={f.id} value={f.id}>{f.name}</option>)}
              </select>
              <button type="button" className="btn btn--ghost" disabled={!newUser} onClick={add}>Adicionar à equipe</button>
            </div>
          )}
          {team.length === 0 && <p className="muted">Ninguém na equipe ainda.</p>}
        </>
      )}
      </div>

      {copying && people && functions && (
        <CopyTeamModal
          currentEventId={currentEventId}
          hasTeam={team.length > 0}
          onClose={() => setCopying(false)}
          onPick={(source, members, mode) => {
            const result = mergeTeam(mode === 'replace' ? [] : team, members, people, functions);
            onChange(result.team);
            setCopying(false);
            setNotice(copySummary(source.title, result));
          }}
        />
      )}
    </section>
  );
}

interface MergeResult { team: TeamMemberInput[]; added: number; kept: number; skipped: string[] }

/** Junta a equipe do evento escolhido com a atual. Quem já está na equipe atual mantém a função atual. */
function mergeTeam(current: TeamMemberInput[], source: TeamMember[], people: UserOption[],
                   functions: TeamFunction[]): MergeResult {
  const result: MergeResult = { team: [...current], added: 0, kept: 0, skipped: [] };
  for (const m of source) {
    if (result.team.some((t) => t.userId === m.userId)) {
      result.kept++;
    } else if (!people.some((p) => p.id === m.userId) || !functions.some((f) => f.id === m.functionId)) {
      // Pessoa desativada (ou função removida): não pode entrar numa equipe nova.
      result.skipped.push(m.userName);
    } else {
      result.team.push({ userId: m.userId, functionId: m.functionId });
      result.added++;
    }
  }
  return result;
}

function copySummary(title: string, r: MergeResult) {
  const parts = [`${r.added === 1 ? '1 pessoa carregada' : `${r.added} pessoas carregadas`} de "${title}".`];
  if (r.kept) parts.push(`${r.kept === 1 ? '1 já estava' : `${r.kept} já estavam`} na equipe e ${r.kept === 1 ? 'manteve' : 'mantiveram'} a função atual.`);
  if (r.skipped.length) parts.push(`Ficaram de fora por estarem desativadas: ${r.skipped.join(', ')}.`);
  parts.push('Revise e salve para confirmar.');
  return parts.join(' ');
}

function CopyTeamModal({ currentEventId, hasTeam, onClose, onPick }: {
  currentEventId?: number;
  hasTeam: boolean;
  onClose: () => void;
  onPick: (source: EventItem, members: TeamMember[], mode: 'add' | 'replace') => void;
}) {
  const [events, setEvents] = useState<EventItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [mode, setMode] = useState<'add' | 'replace'>('add');
  const [loadingId, setLoadingId] = useState<number | null>(null);

  useEffect(() => {
    api.events()
      .then((list) => setEvents(
        list.filter((e) => e.id !== currentEventId)
          // Mais recentes primeiro: o evento anterior costuma ser o último realizado.
          .sort((a, b) => b.startAt.localeCompare(a.startAt)),
      ))
      .catch((e) => setError(errorMessage(e)));
  }, [currentEventId]);

  async function pick(e: EventItem) {
    setLoadingId(e.id);
    setError(null);
    try {
      const members = await api.team(e.id);
      if (members.length === 0) {
        setError(`"${e.title}" não tem ninguém na equipe. Escolha outro evento.`);
        return;
      }
      onPick(e, members, mode);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoadingId(null);
    }
  }

  const term = search.trim().toLowerCase();
  const visible = (events ?? []).filter((e) => !term || e.title.toLowerCase().includes(term)
    || (e.location ?? '').toLowerCase().includes(term));

  return (
    <Modal title="Carregar equipe de outro evento" onClose={onClose}>
      <div className="stack">
        <p className="muted">Escolha o evento: as pessoas da equipe dele entram nesta equipe com as mesmas funções.</p>
        {hasTeam && (
          <div className="segmented" role="radiogroup" aria-label="O que fazer com a equipe atual">
            <label className="check">
              <input type="radio" name="copy-mode" checked={mode === 'add'} onChange={() => setMode('add')} />
              Adicionar à equipe atual
            </label>
            <label className="check">
              <input type="radio" name="copy-mode" checked={mode === 'replace'} onChange={() => setMode('replace')} />
              Substituir a equipe atual
            </label>
          </div>
        )}
        {error && <Alert>{error}</Alert>}
        {!events && !error && <Loading />}
        {events && events.length === 0 && <p className="muted">Não há outros eventos cadastrados.</p>}
        {events && events.length > 0 && (
          <>
            <input type="search" placeholder="Buscar por título ou local" aria-label="Buscar evento"
                   value={search} onChange={(ev) => setSearch(ev.target.value)} autoFocus />
            <ul className="pick-list">
              {visible.map((e) => (
                <li key={e.id}>
                  <button type="button" className="pick" disabled={loadingId !== null} onClick={() => pick(e)}>
                    <span className="pick__title">{e.title}</span>
                    <span className="pick__meta">{fmt.date(e.startAt)}{e.location ? ` · ${e.location}` : ''}</span>
                    {loadingId === e.id && <span className="pick__meta">Carregando equipe…</span>}
                  </button>
                </li>
              ))}
              {visible.length === 0 && <li className="muted">Nenhum evento corresponde à busca.</li>}
            </ul>
          </>
        )}
        <div className="actions">
          <button type="button" className="btn btn--ghost" onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </Modal>
  );
}

function TeamReadOnly({ team }: { team: TeamMember[] }) {
  const [collapsed, toggleCollapsed] = useCollapsed('team');
  return (
    <section className="panel stack form-section">
      <div className="collapse-head">
        <h2 className="form-section__title">Equipe <span className="count">{teamCount(team.length)}</span></h2>
        <CollapseToggle collapsed={collapsed} onToggle={toggleCollapsed} controls="team-body" label="equipe" />
      </div>
      <div id="team-body" className="stack" hidden={collapsed}>
      {team.length === 0 ? <p className="muted">Ninguém na equipe.</p> : (
        <ul className="team-rows">
          {team.map((m) => (
            <li key={m.id} className="team-row">
              <div className="team-row__who"><span className="team-row__name">{m.userName}</span></div>
              <span className="badge badge--fn">{m.functionName}</span>
            </li>
          ))}
        </ul>
      )}
      <p className="field__hint">Você não tem permissão para alterar a equipe deste evento.</p>
      </div>
    </section>
  );
}

function teamCount(n: number) {
  return n === 1 ? '1 pessoa' : `${n} pessoas`;
}
