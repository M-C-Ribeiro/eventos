import { useState } from 'react';
import { api, type TeamFunction } from '../api';
import { PermissionChips, PermissionSetModal, usePermissionCatalog } from '../components/PermissionSets';
import { Alert, Empty, Loading, errorMessage, useAsync } from '../components/ui';

export function TeamFunctionsPage() {
  const list = useAsync(() => api.teamFunctions(), []);
  const catalog = usePermissionCatalog();
  const [editing, setEditing] = useState<TeamFunction | 'new' | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function remove(f: TeamFunction) {
    if (!confirm(`Excluir a função "${f.name}"?`)) return;
    setError(null);
    try {
      await api.deleteTeamFunction(f.id);
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
          <h1>Funções de equipe</h1>
          <p className="muted">O papel de cada pessoa na equipe de um evento. As permissões valem só no evento em que ela está na equipe.</p>
        </div>
        <button type="button" className="btn btn--primary" onClick={() => setEditing('new')}>Criar função</button>
      </header>

      {error && <Alert>{error}</Alert>}
      {list.error && <Alert>{list.error}</Alert>}
      {list.loading && !list.data && <Loading />}

      {list.data && list.data.length === 0 && (
        <Empty title="Nenhuma função cadastrada.">
          <p>Crie funções como Coordenação ou Recepção para montar as equipes dos eventos.</p>
        </Empty>
      )}

      {list.data && list.data.length > 0 && (
        <ul className="set-list">
          {list.data.map((f) => (
            <li key={f.id} className="set">
              <div className="set__head">
                <div>
                  <h2 className="set__name">{f.name}</h2>
                  {f.description && <p className="muted">{f.description}</p>}
                </div>
                <div className="set__meta">
                  <span className="muted">{f.memberCount === 1 ? 'Em 1 equipe' : `Em ${f.memberCount} equipes`}</span>
                  <button type="button" className="btn btn--ghost btn--sm" onClick={() => setEditing(f)}>Editar</button>
                  <button type="button" className="btn btn--text btn--sm" onClick={() => remove(f)}>Excluir</button>
                </div>
              </div>
              <PermissionChips codes={f.permissions} catalog={catalog.data}
                               empty="Sem permissões extras: vê o evento, mesmo em rascunho" />
            </li>
          ))}
        </ul>
      )}

      {editing && catalog.data && (
        <PermissionSetModal
          title={current ? 'Editar função' : 'Criar função'}
          initial={current}
          catalog={catalog.data}
          eventScopedOnly
          intro="Quem estiver na equipe com esta função ganha estas permissões apenas naquele evento. Fazer parte da equipe já permite ver o evento."
          saveLabel={current ? 'Salvar alterações' : 'Criar função'}
          onClose={() => setEditing(null)}
          onSave={async (body) => {
            if (current) await api.updateTeamFunction(current.id, body);
            else await api.createTeamFunction(body);
            setEditing(null);
            list.reload();
          }}
        />
      )}
    </div>
  );
}
