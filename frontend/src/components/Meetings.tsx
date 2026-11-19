import { useState, type FormEvent } from 'react';
import { api, fmt, type AttendanceRow, type Meeting, type MeetingInput } from '../api';
import {
  Alert, CollapseToggle, Empty, Field, Loading, Modal, errorMessage, fieldErrors, useAsync, useCollapsed,
} from './ui';

/**
 * Reuniões preparatórias da equipe, na página do evento (não no formulário de criação).
 * Só aparece para quem pode ver informações internas do evento; só quem edita o evento altera.
 */
export function MeetingsSection({ eventId, canEdit }: { eventId: number; canEdit: boolean }) {
  const list = useAsync(() => api.meetings(eventId), [eventId]);
  const [collapsed, toggleCollapsed] = useCollapsed('meetings');
  const [editing, setEditing] = useState<Meeting | 'new' | null>(null);
  const [attendanceOf, setAttendanceOf] = useState<Meeting | null>(null);
  const [error, setError] = useState<string | null>(null);

  const items = list.data ?? [];

  async function remove(m: Meeting) {
    if (!confirm(`Excluir a reunião "${m.title}" e a lista de presença dela?`)) return;
    setError(null);
    try {
      await api.deleteMeeting(eventId, m.id);
      list.setData((prev) => prev?.filter((i) => i.id !== m.id) ?? null);
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  const count = list.data ? (items.length === 1 ? '1 reunião' : `${items.length} reuniões`) : '';

  return (
    <section className="section">
      <div className="section__head">
        <h2>Reuniões preparatórias {count && <span className="count">{count}</span>}</h2>
        <div className="section__actions">
          {!collapsed && canEdit && (
            <button type="button" className="btn btn--primary btn--sm" onClick={() => setEditing('new')}>
              Adicionar reunião
            </button>
          )}
          <CollapseToggle collapsed={collapsed} onToggle={toggleCollapsed} controls="meetings-body" label="reuniões" />
        </div>
      </div>

      <div id="meetings-body" className="section" hidden={collapsed}>
        {error && <Alert>{error}</Alert>}
        {list.error && <Alert>{list.error}</Alert>}
        {list.loading && !list.data ? <Loading /> : list.data && items.length === 0 ? (
          <Empty title="Nenhuma reunião preparatória cadastrada.">
            <p>{canEdit
              ? 'Cadastre os encontros da equipe e acompanhe quem participou.'
              : 'Quando a coordenação marcar reuniões da equipe, elas aparecem aqui.'}</p>
          </Empty>
        ) : list.data && (
          <div className="table-wrap">
            <table className="table meetings-table">
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Hora</th>
                  <th>Título</th>
                  <th>Local</th>
                  <th>Presença</th>
                  <th><span className="sr-only">Ações</span></th>
                </tr>
              </thead>
              <tbody>
                {items.map((m) => (
                  <tr key={m.id}>
                    <td className="nowrap" title={fmt.plainDate(m.date)}>{fmt.shortDate(m.date)}</td>
                    <td className="nowrap tabular">{m.time}</td>
                    <td className="meetings-table__title">{m.title}</td>
                    <td>{m.location}</td>
                    <td className="nowrap tabular">
                      {m.teamSize === 0 ? <span className="muted">Sem equipe</span> : `${m.presentCount} de ${m.teamSize}`}
                    </td>
                    <td className="table__actions">
                      <button type="button" className="btn btn--ghost btn--sm" onClick={() => setAttendanceOf(m)}>
                        Lista de Presença
                      </button>
                      {canEdit && (
                        <>
                          <button type="button" className="btn btn--ghost btn--sm" onClick={() => setEditing(m)}>Editar</button>
                          <button type="button" className="btn btn--text btn--sm" onClick={() => remove(m)}>Excluir</button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {editing && (
        <MeetingModal
          eventId={eventId}
          meeting={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => { setEditing(null); list.reload(); }}
        />
      )}

      {attendanceOf && (
        <AttendanceModal
          eventId={eventId}
          meeting={attendanceOf}
          canEdit={canEdit}
          onClose={() => { setAttendanceOf(null); list.reload(); }}
        />
      )}
    </section>
  );
}

function MeetingModal({ eventId, meeting, onClose, onSaved }: {
  eventId: number; meeting: Meeting | null; onClose: () => void; onSaved: () => void;
}) {
  const [form, setForm] = useState<MeetingInput>({
    title: meeting?.title ?? '',
    date: meeting?.date ?? '',
    time: meeting?.time ?? '19:00',
    location: meeting?.location ?? '',
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
      if (meeting) await api.updateMeeting(eventId, meeting.id, form);
      else await api.createMeeting(eventId, form);
      onSaved();
    } catch (err) {
      setError(errorMessage(err));
      setFields(fieldErrors(err));
      setBusy(false);
    }
  }

  return (
    <Modal title={meeting ? 'Editar reunião' : 'Adicionar reunião preparatória'} onClose={onClose}>
      <form className="stack" onSubmit={submit}>
        {error && <Alert>{error}</Alert>}
        <Field label="Título" error={fields.title}>
          <input required maxLength={160} value={form.title} data-autofocus
                 onChange={(e) => setForm({ ...form, title: e.target.value })} />
        </Field>
        <div className="grid-2">
          <Field label="Data" error={fields.date}>
            <input type="date" required value={form.date}
                   onChange={(e) => setForm({ ...form, date: e.target.value })} />
          </Field>
          <Field label="Hora" error={fields.time}>
            <input type="time" required value={form.time}
                   onChange={(e) => setForm({ ...form, time: e.target.value })} />
          </Field>
        </div>
        <Field label="Local" error={fields.location} hint="Endereço, sala ou link da chamada">
          <input required maxLength={255} value={form.location}
                 onChange={(e) => setForm({ ...form, location: e.target.value })} />
        </Field>
        <div className="actions">
          <button className="btn btn--primary" disabled={busy}>
            {busy ? 'Salvando…' : meeting ? 'Salvar alterações' : 'Adicionar reunião'}
          </button>
          <button type="button" className="btn btn--ghost" onClick={onClose}>Cancelar</button>
        </div>
      </form>
    </Modal>
  );
}

function AttendanceModal({ eventId, meeting, canEdit, onClose }: {
  eventId: number; meeting: Meeting; canEdit: boolean; onClose: () => void;
}) {
  const rows = useAsync(() => api.attendance(eventId, meeting.id), [eventId, meeting.id]);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [justifying, setJustifying] = useState<{ userId: number; text: string } | null>(null);

  async function save(userId: number, present: boolean, justification: string | null) {
    setBusyId(userId);
    setError(null);
    try {
      const updated = await api.markAttendance(eventId, meeting.id, userId, { present, justification });
      rows.setData((prev) => prev?.map((r) => (r.userId === userId ? updated : r)) ?? null);
      return true;
    } catch (e) {
      setError(errorMessage(e));
      return false;
    } finally {
      setBusyId(null);
    }
  }

  async function togglePresence(r: AttendanceRow) {
    if (justifying?.userId === r.userId) setJustifying(null);
    // Ao voltar para ausente, a pessoa fica sem justificativa até alguém registrar uma.
    await save(r.userId, !r.present, null);
  }

  async function saveJustification() {
    if (!justifying) return;
    if (await save(justifying.userId, false, justifying.text)) setJustifying(null);
  }

  const data = rows.data ?? [];
  const team = data.filter((r) => r.inTeam);
  const present = team.filter((r) => r.present).length;

  return (
    <Modal title="Lista de Presença" onClose={onClose} wide>
      <div className="stack">
        <div>
          <p className="attendance__meeting">{meeting.title}</p>
          <p className="muted">
            {fmt.plainDate(meeting.date)}, {meeting.time} · {meeting.location}
            {team.length > 0 && <> · <strong>{present} de {team.length} presentes</strong></>}
          </p>
        </div>

        {error && <Alert>{error}</Alert>}
        {rows.error && <Alert>{rows.error}</Alert>}
        {rows.loading && !rows.data && <Loading />}

        {rows.data && data.length === 0 && (
          <Empty title="A equipe deste evento está vazia.">
            <p>Adicione pessoas à equipe para registrar a presença.</p>
          </Empty>
        )}

        {data.length > 0 && (
          <ul className="attendance">
            {data.map((r) => {
              const open = justifying?.userId === r.userId;
              const busy = busyId === r.userId;
              return (
                <li key={r.userId} className={`attendance__row${r.inTeam ? '' : ' is-muted'}`}>
                  <div className="attendance__main">
                    <div className="attendance__who">
                      <span className="attendance__name">{r.userName}</span>
                      <span className="muted">{r.inTeam ? r.functionName : 'Saiu da equipe'}</span>
                    </div>
                    <span className={`badge ${r.present ? 'badge--present' : 'badge--absent'}`}>
                      {r.present ? 'Presente' : 'Ausente'}
                    </span>
                    {canEdit && (
                      <div className="attendance__actions">
                        <button type="button" className={`btn btn--sm ${r.present ? 'btn--ghost' : 'btn--checkin'}`}
                                disabled={busy} onClick={() => togglePresence(r)}>
                          {r.present ? 'Marcar ausente' : 'Marcar presente'}
                        </button>
                        <button type="button" className="btn btn--ghost btn--sm"
                                disabled={r.present || busy} aria-expanded={open}
                                title={r.present ? 'Disponível apenas para ausentes' : undefined}
                                onClick={() => setJustifying(open ? null : { userId: r.userId, text: r.justification ?? '' })}>
                          Justificativa
                        </button>
                      </div>
                    )}
                  </div>

                  {!r.present && r.justification && !open && (
                    <p className="attendance__justification">Justificativa: {r.justification}</p>
                  )}

                  {open && (
                    <div className="attendance__editor">
                      <label className="field">
                        <span className="field__label">Justificativa de {r.userName}</span>
                        <textarea rows={2} maxLength={500} autoFocus value={justifying.text}
                                  onChange={(e) => setJustifying({ userId: r.userId, text: e.target.value })} />
                      </label>
                      <div className="actions">
                        <button type="button" className="btn btn--primary btn--sm" disabled={busy}
                                onClick={saveJustification}>
                          {busy ? 'Salvando…' : 'Salvar justificativa'}
                        </button>
                        <button type="button" className="btn btn--ghost btn--sm" onClick={() => setJustifying(null)}>
                          Cancelar
                        </button>
                      </div>
                    </div>
                  )}
                </li>
              );
            })}
          </ul>
        )}

        <div className="actions">
          <button type="button" className="btn btn--ghost" onClick={onClose}>Fechar</button>
        </div>
      </div>
    </Modal>
  );
}
