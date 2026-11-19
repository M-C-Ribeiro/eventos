import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react';
import { ApiError, EVENT_STATUS_LABEL, REG_STATUS_LABEL, fmt, type EventStatus, type RegistrationStatus } from '../api';

/** Carrega dados assíncronos e expõe recarga. */
export function useAsync<T>(loader: () => Promise<T>, deps: unknown[]) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const seq = useRef(0);

  const reload = useCallback(() => {
    const id = ++seq.current;
    setLoading(true);
    loader()
      .then((d) => { if (id === seq.current) { setData(d); setError(null); } })
      .catch((e: unknown) => { if (id === seq.current) setError(errorMessage(e)); })
      .finally(() => { if (id === seq.current) setLoading(false); });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => { reload(); }, [reload]);
  return { data, setData, error, loading, reload };
}

export function errorMessage(e: unknown): string {
  if (e instanceof ApiError) return e.message;
  if (e instanceof Error) return e.message;
  return 'Algo deu errado.';
}

export function fieldErrors(e: unknown): Record<string, string> {
  return e instanceof ApiError && e.fields ? e.fields : {};
}

export function Field(props: {
  label: string;
  error?: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <label className={`field${props.error ? ' field--error' : ''}`}>
      <span className="field__label">{props.label}</span>
      {props.children}
      {props.error ? <span className="field__msg">{props.error}</span>
        : props.hint ? <span className="field__hint">{props.hint}</span> : null}
    </label>
  );
}

export function Alert({ children, tone = 'error' }: { children: ReactNode; tone?: 'error' | 'ok' }) {
  return <div className={`alert alert--${tone}`} role={tone === 'error' ? 'alert' : 'status'}>{children}</div>;
}

export function EventStatusBadge({ status }: { status: EventStatus }) {
  return <span className={`badge badge--${status.toLowerCase()}`}>{EVENT_STATUS_LABEL[status]}</span>;
}

export function RegStatusBadge({ status }: { status: RegistrationStatus }) {
  return <span className={`badge badge--reg-${status.toLowerCase()}`}>{REG_STATUS_LABEL[status]}</span>;
}

/** O canhoto de ingresso com a data do evento: a assinatura visual da interface. */
export function DateStub({ iso, size = 'md' }: { iso: string; size?: 'md' | 'lg' }) {
  return (
    <div className={`stub stub--${size}`} aria-label={fmt.date(iso)}>
      <span className="stub__month">{fmt.month(iso)}</span>
      <span className="stub__day">{fmt.day(iso)}</span>
      <span className="stub__weekday">{fmt.weekday(iso)}</span>
    </div>
  );
}

export function CapacityMeter({ count, capacity }: { count: number; capacity: number | null }) {
  if (capacity == null) {
    return <span className="meter-text">{count} inscritos · sem limite</span>;
  }
  const pct = Math.min(100, Math.round((count / capacity) * 100));
  return (
    <div className="meter" title={`${count} de ${capacity} vagas`}>
      <div className="meter__bar"><div className={`meter__fill${pct >= 100 ? ' meter__fill--full' : ''}`} style={{ width: `${pct}%` }} /></div>
      <span className="meter-text">{count}/{capacity} vagas</span>
    </div>
  );
}

export function Modal({ title, onClose, children, wide = false }: {
  title: string; onClose: () => void; children: ReactNode; wide?: boolean;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = ref.current;
    dialog?.showModal();
    // showModal() põe o foco no primeiro botão (o ✕). Leva o foco para o campo marcado com
    // data-autofocus ou, se não houver, para o primeiro campo de texto da janela.
    const field = dialog?.querySelector<HTMLElement>('[data-autofocus]')
      ?? dialog?.querySelector<HTMLElement>(
        'input:not([type=radio]):not([type=checkbox]):not([readonly]):not([disabled]), select, textarea');
    field?.focus();
    return () => dialog?.close();
  }, []);
  return (
    <dialog ref={ref} className={`modal${wide ? ' modal--wide' : ''}`} onCancel={(e) => { e.preventDefault(); onClose(); }}>
      <div className="modal__head">
        <h2>{title}</h2>
        <button type="button" className="icon-btn" onClick={onClose} aria-label="Fechar">✕</button>
      </div>
      {children}
    </dialog>
  );
}

export function Empty({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="empty">
      <p className="empty__title">{title}</p>
      {children}
    </div>
  );
}

export function Loading() {
  return <div className="loading" aria-busy="true">Carregando…</div>;
}

/**
 * Estado de seção recolhida, lembrado neste navegador (vale para todos os eventos).
 * Se o armazenamento do navegador não estiver disponível, a seção apenas começa aberta.
 */
export function useCollapsed(key: string): [boolean, () => void] {
  const storageKey = `eventhub.collapsed.${key}`;
  const [collapsed, setCollapsed] = useState<boolean>(() => {
    try { return localStorage.getItem(storageKey) === '1'; } catch { return false; }
  });
  const toggle = useCallback(() => {
    setCollapsed((prev) => {
      const next = !prev;
      try { localStorage.setItem(storageKey, next ? '1' : '0'); } catch { /* armazenamento indisponível */ }
      return next;
    });
  }, [storageKey]);
  return [collapsed, toggle];
}

/** Botão do cabeçalho que oculta ou exibe o conteúdo de uma seção. */
export function CollapseToggle({ collapsed, onToggle, controls, label }: {
  collapsed: boolean; onToggle: () => void; controls: string; label: string;
}) {
  return (
    <button type="button" className="collapse-btn" onClick={onToggle}
            aria-expanded={!collapsed} aria-controls={controls}>
      <span className="collapse-btn__icon" aria-hidden="true" />
      {collapsed ? `Exibir ${label}` : `Ocultar ${label}`}
    </button>
  );
}
