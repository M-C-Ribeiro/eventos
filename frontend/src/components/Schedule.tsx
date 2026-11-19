import { useState, type FormEvent } from 'react';
import { api, fmt, type ScheduleItem, type ScheduleItemInput } from '../api';
import {
  Alert, CollapseToggle, Empty, Field, Loading, Modal, errorMessage, fieldErrors, useAsync, useCollapsed,
} from './ui';

/**
 * Cronograma do evento, exibido na página do evento (não no formulário de criação).
 * Todos que veem o evento veem o cronograma; só quem pode editar o evento altera.
 */
export function ScheduleSection({ eventId, eventStart, eventEnd, canEdit }: {
  eventId: number;
  /** Início e fim do evento ("2030-05-01T09:00:00"): limitam as datas do cronograma. */
  eventStart: string;
  eventEnd: string;
  canEdit: boolean;
}) {
  const list = useAsync(() => api.schedule(eventId), [eventId]);
  const [collapsed, toggleCollapsed] = useCollapsed('schedule');
  const [editing, setEditing] = useState<ScheduleItem | 'new' | null>(null);
  const [error, setError] = useState<string | null>(null);

  const items = list.data ?? [];
  const minDate = eventStart.slice(0, 10);
  const maxDate = eventEnd.slice(0, 10);
  const multiDay = minDate !== maxDate;

  async function remove(item: ScheduleItem) {
    if (!confirm(`Excluir "${item.title}" (${item.time}) do cronograma?`)) return;
    setError(null);
    try {
      await api.deleteScheduleItem(eventId, item.id);
      list.setData((prev) => prev?.filter((i) => i.id !== item.id) ?? null);
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  /** Sugestão para um item novo: logo depois do último item, ou no início do evento. */
  function suggestion(): ScheduleItemInput {
    const last = items[items.length - 1];
    if (last) {
      const time = fmt.addMinutes(last.time, last.durationMinutes);
      return { title: '', date: last.date, time, durationMinutes: last.durationMinutes, speaker: '', contact: '' };
    }
    return { title: '', date: minDate, time: eventStart.slice(11, 16), durationMinutes: 60, speaker: '', contact: '' };
  }

  const count = list.data ? (items.length === 1 ? '1 item' : `${items.length} itens`) : '';

  return (
    <section className="section">
      <div className="section__head">
        <h2>Cronograma {count && <span className="count">{count}</span>}</h2>
        <div className="section__actions">
          {!collapsed && canEdit && (
            <button type="button" className="btn btn--primary btn--sm" onClick={() => setEditing('new')}>
              Adicionar ao cronograma
            </button>
          )}
          <CollapseToggle collapsed={collapsed} onToggle={toggleCollapsed} controls="schedule-body" label="cronograma" />
        </div>
      </div>

      <div id="schedule-body" className="section" hidden={collapsed}>
        {error && <Alert>{error}</Alert>}
        {list.error && <Alert>{list.error}</Alert>}
        {list.loading && !list.data ? <Loading /> : list.data && items.length === 0 ? (
          <Empty title="O cronograma ainda está vazio.">
            <p>{canEdit
              ? 'Adicione as atividades com título, data, hora, duração e palestrante.'
              : 'Quando a organização publicar a programação, ela aparece aqui.'}</p>
          </Empty>
        ) : list.data && (
          <div className="table-wrap">
            <table className="table schedule-table">
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Hora</th>
                  <th>Título</th>
                  <th>Duração</th>
                  <th>Palestrante</th>
                  <th>Contato</th>
                  {canEdit && <th><span className="sr-only">Ações</span></th>}
                </tr>
              </thead>
              <tbody>
                {items.map((item, i) => {
                  const newDay = multiDay && (i === 0 || items[i - 1].date !== item.date);
                  return (
                    <tr key={item.id} className={newDay && i > 0 ? 'is-new-day' : undefined}>
                      <td className="schedule-table__date" title={fmt.plainDate(item.date)}>{fmt.shortDate(item.date)}</td>
                      <td className="schedule-table__time">
                        {item.time}<span className="muted"> às {fmt.addMinutes(item.time, item.durationMinutes)}</span>
                      </td>
                      <td className="schedule-table__title">{item.title}</td>
                      <td>{fmt.duration(item.durationMinutes)}</td>
                      <td>{item.speaker}</td>
                      <td><Contact value={item.contact} /></td>
                      {canEdit && (
                        <td className="table__actions">
                          <button type="button" className="btn btn--ghost btn--sm" onClick={() => setEditing(item)}>Editar</button>
                          <button type="button" className="btn btn--text btn--sm" onClick={() => remove(item)}>Excluir</button>
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {editing && (
        <ScheduleItemModal
          eventId={eventId}
          item={editing === 'new' ? null : editing}
          initial={editing === 'new' ? suggestion() : editing}
          minDate={minDate}
          maxDate={maxDate}
          onClose={() => setEditing(null)}
          onSaved={() => { setEditing(null); list.reload(); }}
        />
      )}
    </section>
  );
}

function Contact({ value }: { value: string | null }) {
  if (!value) return <span className="muted">—</span>;
  if (/^\S+@\S+\.\S+$/.test(value)) return <a href={`mailto:${value}`}>{value}</a>;
  const digits = value.replace(/\D/g, '');
  if (digits.length >= 8 && /^[\d\s()+\-.]+$/.test(value)) return <a href={`tel:+${digits.length <= 11 ? '55' : ''}${digits}`}>{value}</a>;
  return <>{value}</>;
}

function ScheduleItemModal({ eventId, item, initial, minDate, maxDate, onClose, onSaved }: {
  eventId: number;
  item: ScheduleItem | null;
  initial: ScheduleItemInput;
  minDate: string;
  maxDate: string;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [form, setForm] = useState({
    title: initial.title,
    date: initial.date,
    time: initial.time,
    durationMinutes: String(initial.durationMinutes),
    speaker: initial.speaker,
    contact: initial.contact ?? '',
  });
  const [error, setError] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  const minutes = Number(form.durationMinutes);
  const validDuration = Number.isInteger(minutes) && minutes > 0;

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setFields({});
    const body: ScheduleItemInput = {
      title: form.title,
      date: form.date,
      time: form.time,
      durationMinutes: minutes,
      speaker: form.speaker,
      contact: form.contact || null,
    };
    try {
      if (item) await api.updateScheduleItem(eventId, item.id, body);
      else await api.createScheduleItem(eventId, body);
      onSaved();
    } catch (err) {
      setError(errorMessage(err));
      setFields(fieldErrors(err));
      setBusy(false);
    }
  }

  return (
    <Modal title={item ? 'Editar item do cronograma' : 'Adicionar ao cronograma'} onClose={onClose}>
      <form className="stack" onSubmit={submit}>
        {error && <Alert>{error}</Alert>}
        <Field label="Título" error={fields.title}>
          <input required maxLength={160} value={form.title} data-autofocus
                 onChange={(e) => setForm({ ...form, title: e.target.value })} />
        </Field>
        <div className="grid-2">
          <Field label="Data" error={fields.date}>
            <input type="date" required value={form.date} min={minDate} max={maxDate}
                   readOnly={minDate === maxDate}
                   onChange={(e) => setForm({ ...form, date: e.target.value })} />
          </Field>
          <Field label="Hora" error={fields.time}>
            <input type="time" required value={form.time}
                   onChange={(e) => setForm({ ...form, time: e.target.value })} />
          </Field>
        </div>
        <Field label="Duração (minutos)" error={fields.durationMinutes}
               hint={validDuration && form.time
                 ? `${fmt.duration(minutes)}, termina às ${fmt.addMinutes(form.time, minutes)}`
                 : 'Ex.: 45, 60, 90'}>
          <input type="number" required min={1} max={1440} inputMode="numeric" value={form.durationMinutes}
                 onChange={(e) => setForm({ ...form, durationMinutes: e.target.value })} />
        </Field>
        <Field label="Palestrante" error={fields.speaker}>
          <input required maxLength={120} value={form.speaker}
                 onChange={(e) => setForm({ ...form, speaker: e.target.value })} />
        </Field>
        <Field label="Contato" error={fields.contact} hint="Telefone ou e-mail do palestrante (opcional)">
          <input maxLength={160} value={form.contact}
                 onChange={(e) => setForm({ ...form, contact: e.target.value })} />
        </Field>
        <div className="actions">
          <button className="btn btn--primary" disabled={busy}>
            {busy ? 'Salvando…' : item ? 'Salvar alterações' : 'Adicionar ao cronograma'}
          </button>
          <button type="button" className="btn btn--ghost" onClick={onClose}>Cancelar</button>
        </div>
      </form>
    </Modal>
  );
}
