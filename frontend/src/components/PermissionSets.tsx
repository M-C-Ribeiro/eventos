// Componentes compartilhados entre "Perfis de acesso" e "Funções de equipe":
// ambos são um nome + descrição + um conjunto de permissões.
import { useState, type FormEvent } from 'react';
import { api, type Permission, type PermissionInfo, type RoleInput } from '../api';
import { Alert, Field, Modal, errorMessage, fieldErrors, useAsync } from './ui';

let catalogCache: Promise<PermissionInfo[]> | null = null;

/** Catálogo de permissões vindo da API (fonte única dos nomes e descrições). */
export function usePermissionCatalog() {
  return useAsync(() => {
    catalogCache ??= api.permissions().catch((e) => { catalogCache = null; throw e; });
    return catalogCache;
  }, []);
}

export function PermissionChips({ codes, catalog, empty }: {
  codes: Permission[]; catalog: PermissionInfo[] | null; empty: string;
}) {
  if (codes.length === 0) return <span className="muted">{empty}</span>;
  const label = (c: Permission) => catalog?.find((p) => p.code === c)?.label ?? c;
  return (
    <ul className="chips">
      {codes.map((c) => <li key={c} className="chip">{label(c)}</li>)}
    </ul>
  );
}

export function PermissionSetModal(props: {
  title: string;
  initial: { name: string; description: string | null; permissions: Permission[] } | null;
  catalog: PermissionInfo[];
  /** Restringe às permissões que podem ser dadas por função de equipe. */
  eventScopedOnly?: boolean;
  intro: string;
  saveLabel: string;
  onSave: (body: RoleInput) => Promise<unknown>;
  onClose: () => void;
}) {
  const [form, setForm] = useState<RoleInput>({
    name: props.initial?.name ?? '',
    description: props.initial?.description ?? '',
    permissions: props.initial?.permissions ?? [],
  });
  const [error, setError] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const options = props.catalog.filter((p) => !props.eventScopedOnly || p.eventScoped);

  function toggle(code: Permission, on: boolean) {
    setForm((f) => ({
      ...f,
      permissions: on ? [...f.permissions, code] : f.permissions.filter((c) => c !== code),
    }));
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setFields({});
    try {
      await props.onSave(form);
    } catch (err) {
      setError(errorMessage(err));
      setFields(fieldErrors(err));
      setBusy(false);
    }
  }

  return (
    <Modal title={props.title} onClose={props.onClose}>
      <form className="stack" onSubmit={submit}>
        {error && <Alert>{error}</Alert>}
        <Field label="Nome" error={fields.name}>
          <input required maxLength={60} autoFocus value={form.name}
                 onChange={(e) => setForm({ ...form, name: e.target.value })} />
        </Field>
        <Field label="Descrição" error={fields.description}>
          <input maxLength={255} value={form.description}
                 onChange={(e) => setForm({ ...form, description: e.target.value })} />
        </Field>
        <fieldset className="perm-list">
          <legend>Permissões</legend>
          <p className="field__hint">{props.intro}</p>
          {options.map((p) => (
            <label key={p.code} className="check">
              <input type="checkbox" checked={form.permissions.includes(p.code)}
                     onChange={(e) => toggle(p.code, e.target.checked)} />
              {p.label}
            </label>
          ))}
        </fieldset>
        <div className="actions">
          <button className="btn btn--primary" disabled={busy}>{busy ? 'Salvando…' : props.saveLabel}</button>
          <button type="button" className="btn btn--ghost" onClick={props.onClose}>Cancelar</button>
        </div>
      </form>
    </Modal>
  );
}
