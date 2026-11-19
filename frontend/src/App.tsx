import { useEffect, type ReactNode } from 'react';
import { useAuth } from './auth';
import { Layout } from './components/Layout';
import { Loading } from './components/ui';
import { DashboardPage } from './pages/Dashboard';
import { EventDetailPage } from './pages/EventDetail';
import { EventFormPage } from './pages/EventForm';
import { EventsPage } from './pages/Events';
import { LoginPage, RegisterPage } from './pages/AuthPages';
import { UsersPage } from './pages/Users';
import { RolesPage } from './pages/Roles';
import { TeamFunctionsPage } from './pages/TeamFunctions';
import { matchPath, navigate, usePath } from './router';

function Redirect({ to }: { to: string }) {
  useEffect(() => { navigate(to, true); }, [to]);
  return null;
}

export function App() {
  const { user, loading, can } = useAuth();
  const path = usePath();

  if (loading) return <Loading />;

  if (!user) {
    if (path === '/register') return <RegisterPage />;
    if (path === '/login') return <LoginPage />;
    return <Redirect to="/login" />;
  }

  if (path === '/login' || path === '/register') return <Redirect to="/" />;

  let page: ReactNode;
  let m: Record<string, string> | null;

  if (path === '/') page = <DashboardPage />;
  else if (path === '/events') page = <EventsPage />;
  else if (path === '/events/new') page = can('EVENT_CREATE') ? <EventFormPage /> : <Redirect to="/events" />;
  // A permissão de editar depende do evento (perfil, responsável ou equipe): o formulário confere.
  else if ((m = matchPath('/events/:id/edit', path))) page = <EventFormPage key={m.id} id={Number(m.id)} />;
  else if ((m = matchPath('/events/:id', path))) page = <EventDetailPage key={m.id} id={Number(m.id)} />;
  else if (path === '/users') page = can('USER_MANAGE') ? <UsersPage /> : <Redirect to="/" />;
  else if (path === '/roles') page = can('ROLE_MANAGE') ? <RolesPage /> : <Redirect to="/" />;
  else if (path === '/team-functions') page = can('TEAM_FUNCTION_MANAGE') ? <TeamFunctionsPage /> : <Redirect to="/" />;
  else page = <div className="page"><h1>Página não encontrada</h1><p className="muted">Confira o endereço ou volte para a visão geral.</p></div>;

  return <Layout>{page}</Layout>;
}
