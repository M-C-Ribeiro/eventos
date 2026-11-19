import { useState, type FormEvent } from 'react';
import { api, fmt, type AccessRole, type User } from '../api';
import { useAuth } from '../auth';
import { Alert, Field, Loading, Modal, errorMessage, fieldErrors, useAsync } from '../components/ui';
import { Link } from '../router';

export function UsersPage() {
  const { user: me, can, refresh } = useAuth();
  const list = useAsync(() => api.users(), []);
  const roles = useAsync(() => api.roles(), []);
  const [editing, setEditing] = useState<User | 'new' | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function remove(u: User) {
    if (!confirm(`Excluir ${u.name}? Se a pessoa organiza eventos, desative-a em vez disso.`)) return;
    setError(null);
    try {
      await api.deleteUser(u.id);
      list.reload();
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  return (
    <div className="page">
      <header className="page__head">
        <div>
          <h1>Usuários</h1>
          <p className="muted">Pessoas com acesso a {me?.tenant.name}. Elas entram com o identificador <strong>{me?.tenant.slug}</strong>.</p>
        </div>
        <button type="button" className="btn btn--primary" onClick={() => setEditing('new')}>Adicionar usuário</button>
      </header>

      <p className="muted">
        O que cada pessoa pode fazer vem dos seus perfis de acesso
        {can('ROLE_MANAGE') ? <> (<Link to="/roles">ver perfis</Link>)</> : null}
        {' '}e das funções que ela exerce nas equipes dos eventos.
      </p>

      {error && <Alert>{error}</Alert>}
      {list.error && <Alert>{list.error}</Alert>}
      {roles.error && <Alert>{roles.error}</Alert>}
      {list.loading && !list.data && <Loading />}

      {list.data && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr><th>Nome</th><th>E-mail</th><th>Perfis</th><th>Situação</th><th>Desde</th><th><span className="sr-only">Ações</span></th></tr>
            </thead>
            <tbody>
              {list.data.map((u) => (
                <tr key={u.id} className={u.active ? undefined : 'is-muted'}>
                  <td>{u.name}{u.id === me?.id && <span className="you">você</span>}</td>
                  <td>{u.email}</td>
                  <td>{u.roles.map((r) => r.name).join(', ') || <span className="muted">Nenhum</span>}</td>
                  <td>{u.active ? 'Ativo' : 'Desativado'}</td>
                  <td>{fmt.date(u.createdAt)}</td>
                  <td className="table__actions">
                    <button type="button" className="btn btn--ghost btn--sm" onClick={() => setEditing(u)}>Editar</button>
                    {u.id !== me?.id && (
                      <button type="button" className="btn btn--text btn--sm" onClick={() => remove(u)}>Excluir</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {editing && roles.data && (
        <UserModal user={editing === 'new' ? null : editing} roles={roles.data} onClose={() => setEditing(null)}
                   onSaved={() => { setEditing(null); list.reload(); refresh(); }} />
      )}
    </div>
  );
}

function UserModal({ user, roles, onClose, onSaved }: {
  user: User | null; roles: AccessRole[]; onClose: () => void; onSaved: () => void;
}) {
  const [form, setForm] = useState({
    name: user?.name ?? '',
    email: user?.email ?? '',
    password: '',
    roleIds: user?.roles.map((r) => r.id) ?? roles.filter((r) => r.name === 'Membro').map((r) => r.id),
    active: user?.active ?? true,
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
      if (user) {
        await api.updateUser(user.id, {
          name: form.name, roleIds: form.roleIds, active: form.active, password: form.password || undefined,
        });
      } else {
        await api.createUser({ name: form.name, email: form.email, password: form.password, roleIds: form.roleIds });
      }
      onSaved();
    } catch (err) {
      setError(errorMessage(err));
      setFields(fieldErrors(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title={user ? 'Editar usuário' : 'Adicionar usuário'} onClose={onClose}>
      <form className="stack" onSubmit={submit}>
        {error && <Alert>{error}</Alert>}
        <Field label="Nome" error={fields.name}>
          <input required autoFocus value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        </Field>
        <Field label="E-mail" error={fields.email} hint={user ? 'O e-mail não pode ser alterado.' : undefined}>
          <input type="email" required disabled={!!user} value={form.email}
                 onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </Field>
        <Field label={user ? 'Nova senha' : 'Senha inicial'} error={fields.password}
               hint={user ? 'Deixe em branco para manter a atual.' : 'Mínimo de 8 caracteres. Compartilhe com a pessoa.'}>
          <input type="password" required={!user} minLength={8} autoComplete="new-password" value={form.password}
                 onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </Field>
        <fieldset className="perm-list">
          <legend>Perfis de acesso</legend>
          {fields.roleIds && <p className="field__msg">{fields.roleIds}</p>}
          {roles.map((r) => (
            <label key={r.id} className="check check--top">
              <input type="checkbox" checked={form.roleIds.includes(r.id)}
                     onChange={(e) => setForm({
                       ...form,
                       roleIds: e.target.checked ? [...form.roleIds, r.id] : form.roleIds.filter((id) => id !== r.id),
                     })} />
              <span>{r.name}{r.description && <span className="check__hint">{r.description}</span>}</span>
            </label>
          ))}
        </fieldset>
        {user && (
          <label className="check">
            <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
            Acesso ativo
          </label>
        )}
        <div className="actions">
          <button className="btn btn--primary" disabled={busy}>{busy ? 'Salvando…' : user ? 'Salvar alterações' : 'Adicionar usuário'}</button>
          <button type="button" className="btn btn--ghost" onClick={onClose}>Cancelar</button>
        </div>
      </form>
    </Modal>
  );
}
