import type { ReactNode } from 'react';
import { useAuth } from '../auth';
import { Link, usePath } from '../router';

export function Layout({ children }: { children: ReactNode }) {
  const { user, signOut, can } = useAuth();
  const path = usePath();
  if (!user) return null;

  const nav = [
    { to: '/', label: 'Visão geral', active: path === '/' },
    { to: '/events', label: 'Eventos', active: path.startsWith('/events') },
    ...(can('TEAM_FUNCTION_MANAGE')
      ? [{ to: '/team-functions', label: 'Funções de equipe', active: path.startsWith('/team-functions') }] : []),
    ...(can('USER_MANAGE') ? [{ to: '/users', label: 'Usuários', active: path.startsWith('/users') }] : []),
    ...(can('ROLE_MANAGE') ? [{ to: '/roles', label: 'Perfis de acesso', active: path.startsWith('/roles') }] : []),
  ];

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand__mark" aria-hidden="true" />
          <div>
            <div className="brand__name">RetirOn</div>
            <div className="brand__org" title={user.tenant.slug}>{user.tenant.name}</div>
          </div>
        </div>
        <nav className="nav" aria-label="Principal">
          {nav.map((n) => (
            <Link key={n.to} to={n.to} className={`nav__link${n.active ? ' is-active' : ''}`}
                  aria-current={n.active ? 'page' : undefined}>
              {n.label}
            </Link>
          ))}
        </nav>
        <div className="me">
          <div className="me__name">{user.name}</div>
          <div className="me__role">{user.roles.join(', ') || 'Sem perfil'}</div>
          <button type="button" className="btn btn--ghost-light btn--sm" onClick={signOut}>Sair</button>
        </div>
      </aside>
      <main className="main">{children}</main>
    </div>
  );
}
