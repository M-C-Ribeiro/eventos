import { useState } from 'react';
import { api, type AccessRole } from '../api';
import { PermissionChips, PermissionSetModal, usePermissionCatalog } from '../components/PermissionSets';
import { Alert, Loading, errorMessage, useAsync } from '../components/ui';

export function RolesPage() {
  const list = useAsync(() => api.roles(), []);
  const catalog = usePermissionCatalog();
  const [editing, setEditing] = useState<AccessRole | 'new' | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function remove(r: AccessRole) {
    const who = r.userCount > 0 ? ` ${r.userCount} usuário(s) perderão este perfil.` : '';
    if (!confirm(`Excluir o perfil "${r.name}"?${who}`)) return;
    setError(null);
    try {
      await api.deleteRole(r.id);
      list.reload();
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  const current = editing === 'new' ? null : editing;

  return (
    <div className="page">
      <header className="page__head">
        <div>
          <h1>Perfis de acesso</h1>
          <p className="muted">Cada perfil reúne permissões. Um usuário pode ter vários perfis e recebe a soma das permissões.</p>
        </div>
        <button type="button" className="btn btn--primary" onClick={() => setEditing('new')}>Criar perfil</button>
      </header>

      {error && <Alert>{error}</Alert>}
      {list.error && <Alert>{list.error}</Alert>}
      {list.loading && !list.data && <Loading />}

      {list.data && (
        <ul className="set-list">
          {list.data.map((r) => (
            <li key={r.id} className="set">
              <div className="set__head">
                <div>
                  <h2 className="set__name">{r.name}{r.systemRole && <span className="you">protegido</span>}</h2>
                  {r.description && <p className="muted">{r.description}</p>}
                </div>
                <div className="set__meta">
                  <span className="muted">{r.userCount === 1 ? '1 usuário' : `${r.userCount} usuários`}</span>
                  {!r.systemRole && (
                    <>
                      <button type="button" className="btn btn--ghost btn--sm" onClick={() => setEditing(r)}>Editar</button>
                      <button type="button" className="btn btn--text btn--sm" onClick={() => remove(r)}>Excluir</button>
                    </>
                  )}
                </div>
              </div>
              <PermissionChips codes={r.permissions} catalog={catalog.data} empty="Nenhuma permissão" />
            </li>
          ))}
        </ul>
      )}

      {editing && catalog.data && (
        <PermissionSetModal
          title={current ? 'Editar perfil' : 'Criar perfil'}
          initial={current}
          catalog={catalog.data}
          intro="Valem para toda a organização. Permissões de evento dadas aqui valem em todos os eventos."
          saveLabel={current ? 'Salvar alterações' : 'Criar perfil'}
          onClose={() => setEditing(null)}
          onSave={async (body) => {
            if (current) await api.updateRole(current.id, body);
            else await api.createRole(body);
            setEditing(null);
            list.reload();
          }}
        />
      )}
    </div>
  );
}
