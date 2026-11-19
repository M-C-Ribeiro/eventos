// Cliente HTTP e tipos da API do RetirOn.

export type Permission =
  | 'EVENT_VIEW' | 'EVENT_VIEW_ALL' | 'EVENT_CREATE' | 'EVENT_EDIT' | 'EVENT_DELETE'
  | 'REGISTRATION_VIEW' | 'REGISTRATION_MANAGE' | 'CHECKIN' | 'TEAM_MANAGE'
  | 'TEAM_FUNCTION_MANAGE' | 'USER_MANAGE' | 'ROLE_MANAGE' | 'ORGANIZATION_MANAGE';
export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED';
export type RegistrationStatus = 'CONFIRMED' | 'CHECKED_IN' | 'CANCELLED';

export interface TenantInfo { id: number; name: string; slug: string }
export interface Me {
  id: number; name: string; email: string; roles: string[]; permissions: Permission[]; tenant: TenantInfo;
}
export interface AuthResponse { token: string; expiresIn: number; user: Me }

export interface RoleSummary { id: number; name: string }

export interface User {
  id: number; name: string; email: string; roles: RoleSummary[]; active: boolean; createdAt: string;
}

export interface UserOption { id: number; name: string; email: string }

export interface PermissionInfo { code: Permission; label: string; eventScoped: boolean }

export interface AccessRole {
  id: number; name: string; description: string | null; systemRole: boolean;
  permissions: Permission[]; userCount: number;
}

export interface RoleInput { name: string; description: string; permissions: Permission[] }

export interface TeamFunction {
  id: number; name: string; description: string | null; permissions: Permission[]; memberCount: number;
}

export interface TeamMember {
  id: number; userId: number; userName: string; userEmail: string; functionId: number; functionName: string;
}

export interface TeamMemberInput { userId: number; functionId: number }

export interface ScheduleItem {
  id: number;
  title: string;
  /** "2030-05-01" */
  date: string;
  /** "09:30" */
  time: string;
  durationMinutes: number;
  speaker: string;
  contact: string | null;
}

export type ScheduleItemInput = Omit<ScheduleItem, 'id'>;

export interface Meeting {
  id: number;
  title: string;
  /** "2030-05-01" */
  date: string;
  /** "19:00" */
  time: string;
  location: string;
  /** Presentes entre os membros atuais da equipe. */
  presentCount: number;
  teamSize: number;
}

export type MeetingInput = Pick<Meeting, 'title' | 'date' | 'time' | 'location'>;

export interface Demand {
  id: number;
  title: string;
  description: string | null;
  /** "2030-05-01" */
  dueDate: string;
  /** Funções atribuídas e quem as exerce hoje na equipe do evento. */
  functions: { id: number; name: string; members: string[] }[];
  users: { id: number; name: string }[];
  /** Responsáveis efetivos (por função e/ou diretamente) e a situação de cada um. */
  assignees: DemandAssignee[];
  completedCount: number;
  assignedToMe: boolean;
  completedByMe: boolean;
}

export interface DemandAssignee {
  userId: number;
  name: string;
  /** Origem da responsabilidade: nomes das funções e/ou "Atribuição direta". */
  via: string[];
  completed: boolean;
  completedAt: string | null;
}

export interface DemandInput { title: string; description: string; dueDate: string }

export interface AttendanceRow {
  userId: number;
  userName: string;
  /** Nulo para quem saiu da equipe depois de ter a presença registrada. */
  functionName: string | null;
  present: boolean;
  justification: string | null;
  inTeam: boolean;
}

export interface EventItem {
  id: number;
  title: string;
  description: string | null;
  location: string | null;
  startAt: string;
  endAt: string;
  capacity: number | null;
  status: EventStatus;
  organizer: { id: number; name: string };
  confirmedCount: number;
  createdAt: string;
  /** Só no detalhe: o que o usuário atual pode fazer neste evento. */
  myPermissions: Permission[] | null;
  /** Só no detalhe. */
  team: TeamMember[] | null;
}

export interface EventInput {
  title: string;
  description: string;
  location: string;
  startAt: string;
  endAt: string;
  capacity: number | null;
  status: EventStatus;
  /** Quando enviado, substitui a equipe inteira. */
  team?: TeamMemberInput[];
}

export interface Registration {
  id: number;
  eventId: number;
  userId: number | null;
  name: string;
  email: string;
  status: RegistrationStatus;
  registeredAt: string;
  checkedInAt: string | null;
}

export interface Dashboard {
  upcomingEvents: number;
  draftEvents: number;
  activeRegistrations: number;
  checkIns: number;
  users: number;
  nextEvents: EventItem[];
}

export class ApiError extends Error {
  constructor(public status: number, message: string, public fields?: Record<string, string>) {
    super(message);
  }
}

const TOKEN_KEY = 'eventhub.token';

export const tokenStore = {
  get: (): string | null => {
    try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
  },
  set: (token: string) => {
    try { localStorage.setItem(TOKEN_KEY, token); } catch { /* armazenamento indisponível */ }
  },
  clear: () => {
    try { localStorage.removeItem(TOKEN_KEY); } catch { /* armazenamento indisponível */ }
  },
};

let onUnauthorized: () => void = () => {};
export function setUnauthorizedHandler(fn: () => void) {
  onUnauthorized = fn;
}

const BASE = import.meta.env.VITE_API_URL ?? '';

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  const token = tokenStore.get();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  let res: Response;
  try {
    res = await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Não foi possível conectar ao servidor. Verifique se a API está rodando.');
  }

  if (res.status === 401 && token) {
    onUnauthorized();
  }

  const text = await res.text();
  const data = text ? JSON.parse(text) : null;

  if (!res.ok) {
    const message = data?.message ?? (
      res.status === 401 ? 'Sessão expirada. Entre novamente.'
        : res.status >= 500 ? `Erro no servidor (${res.status}). Veja o terminal da API para os detalhes.`
          : `Erro ${res.status}`);
    throw new ApiError(res.status, message, data?.fields ?? undefined);
  }
  return data as T;
}

export const api = {
  register: (body: {
    organizationName: string; organizationSlug: string; name: string; email: string; password: string;
  }) => request<AuthResponse>('POST', '/api/auth/register', body),
  login: (body: { organizationSlug: string; email: string; password: string }) =>
    request<AuthResponse>('POST', '/api/auth/login', body),
  me: () => request<Me>('GET', '/api/auth/me'),

  dashboard: () => request<Dashboard>('GET', '/api/dashboard'),

  events: (params: { status?: EventStatus | ''; q?: string; upcoming?: boolean } = {}) => {
    const qs = new URLSearchParams();
    if (params.status) qs.set('status', params.status);
    if (params.q) qs.set('q', params.q);
    if (params.upcoming) qs.set('upcoming', 'true');
    const suffix = qs.toString() ? `?${qs}` : '';
    return request<EventItem[]>('GET', `/api/events${suffix}`);
  },
  event: (id: number) => request<EventItem>('GET', `/api/events/${id}`),
  createEvent: (body: EventInput) => request<EventItem>('POST', '/api/events', body),
  updateEvent: (id: number, body: EventInput) => request<EventItem>('PUT', `/api/events/${id}`, body),
  deleteEvent: (id: number) => request<void>('DELETE', `/api/events/${id}`),

  registrations: (eventId: number) => request<Registration[]>('GET', `/api/events/${eventId}/registrations`),
  addRegistration: (eventId: number, body: { name: string; email: string }) =>
    request<Registration>('POST', `/api/events/${eventId}/registrations`, body),
  checkIn: (eventId: number, id: number) =>
    request<Registration>('POST', `/api/events/${eventId}/registrations/${id}/check-in`),
  undoCheckIn: (eventId: number, id: number) =>
    request<Registration>('DELETE', `/api/events/${eventId}/registrations/${id}/check-in`),
  cancelRegistration: (eventId: number, id: number) =>
    request<Registration>('POST', `/api/events/${eventId}/registrations/${id}/cancel`),

  team: (eventId: number) => request<TeamMember[]>('GET', `/api/events/${eventId}/team`),

  schedule: (eventId: number) => request<ScheduleItem[]>('GET', `/api/events/${eventId}/schedule`),
  createScheduleItem: (eventId: number, body: ScheduleItemInput) =>
    request<ScheduleItem>('POST', `/api/events/${eventId}/schedule`, body),
  updateScheduleItem: (eventId: number, id: number, body: ScheduleItemInput) =>
    request<ScheduleItem>('PUT', `/api/events/${eventId}/schedule/${id}`, body),
  deleteScheduleItem: (eventId: number, id: number) =>
    request<void>('DELETE', `/api/events/${eventId}/schedule/${id}`),

  demands: (eventId: number) => request<Demand[]>('GET', `/api/events/${eventId}/demands`),
  createDemand: (eventId: number, body: DemandInput) =>
    request<Demand>('POST', `/api/events/${eventId}/demands`, body),
  updateDemand: (eventId: number, id: number, body: DemandInput) =>
    request<Demand>('PUT', `/api/events/${eventId}/demands/${id}`, body),
  deleteDemand: (eventId: number, id: number) =>
    request<void>('DELETE', `/api/events/${eventId}/demands/${id}`),
  setDemandCompletion: (eventId: number, id: number, userId: number, completed: boolean) =>
    request<Demand>('PUT', `/api/events/${eventId}/demands/${id}/completion/${userId}`, { completed }),
  assignDemand: (eventId: number, id: number, body: { functionIds: number[]; userIds: number[] }) =>
    request<Demand>('PUT', `/api/events/${eventId}/demands/${id}/assignments`, body),

  meetings: (eventId: number) => request<Meeting[]>('GET', `/api/events/${eventId}/meetings`),
  createMeeting: (eventId: number, body: MeetingInput) =>
    request<Meeting>('POST', `/api/events/${eventId}/meetings`, body),
  updateMeeting: (eventId: number, id: number, body: MeetingInput) =>
    request<Meeting>('PUT', `/api/events/${eventId}/meetings/${id}`, body),
  deleteMeeting: (eventId: number, id: number) =>
    request<void>('DELETE', `/api/events/${eventId}/meetings/${id}`),
  attendance: (eventId: number, meetingId: number) =>
    request<AttendanceRow[]>('GET', `/api/events/${eventId}/meetings/${meetingId}/attendance`),
  markAttendance: (eventId: number, meetingId: number, userId: number,
                   body: { present: boolean; justification?: string | null }) =>
    request<AttendanceRow>('PUT', `/api/events/${eventId}/meetings/${meetingId}/attendance/${userId}`, body),
  replaceTeam: (eventId: number, members: TeamMemberInput[]) =>
    request<TeamMember[]>('PUT', `/api/events/${eventId}/team`, { members }),

  users: () => request<User[]>('GET', '/api/users'),
  userOptions: () => request<UserOption[]>('GET', '/api/users/options'),
  createUser: (body: { name: string; email: string; password: string; roleIds: number[] }) =>
    request<User>('POST', '/api/users', body),
  updateUser: (id: number, body: { name: string; roleIds: number[]; active: boolean; password?: string }) =>
    request<User>('PUT', `/api/users/${id}`, body),
  deleteUser: (id: number) => request<void>('DELETE', `/api/users/${id}`),

  permissions: () => request<PermissionInfo[]>('GET', '/api/permissions'),
  roles: () => request<AccessRole[]>('GET', '/api/roles'),
  createRole: (body: RoleInput) => request<AccessRole>('POST', '/api/roles', body),
  updateRole: (id: number, body: RoleInput) => request<AccessRole>('PUT', `/api/roles/${id}`, body),
  deleteRole: (id: number) => request<void>('DELETE', `/api/roles/${id}`),

  teamFunctions: () => request<TeamFunction[]>('GET', '/api/team-functions'),
  createTeamFunction: (body: RoleInput) => request<TeamFunction>('POST', '/api/team-functions', body),
  updateTeamFunction: (id: number, body: RoleInput) => request<TeamFunction>('PUT', `/api/team-functions/${id}`, body),
  deleteTeamFunction: (id: number) => request<void>('DELETE', `/api/team-functions/${id}`),
};

// ---------- formatação ----------

export const EVENT_STATUS_LABEL: Record<EventStatus, string> = {
  DRAFT: 'Rascunho',
  PUBLISHED: 'Publicado',
  CANCELLED: 'Cancelado',
  COMPLETED: 'Concluído',
};

export const REG_STATUS_LABEL: Record<RegistrationStatus, string> = {
  CONFIRMED: 'Confirmada',
  CHECKED_IN: 'Presente',
  CANCELLED: 'Cancelada',
};

const dateFmt = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' });
const timeFmt = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' });
const monthFmt = new Intl.DateTimeFormat('pt-BR', { month: 'short' });
const weekdayFmt = new Intl.DateTimeFormat('pt-BR', { weekday: 'short' });

/** "2030-05-01" como data local (new Date("2030-05-01") seria meia-noite UTC e cairia no dia anterior no Brasil). */
const localDate = (ymd: string) => {
  const [y, m, d] = ymd.split('-').map(Number);
  return new Date(y, m - 1, d);
};

export const fmt = {
  /** Data sem hora, ex.: "01 de mai. de 2030". */
  plainDate: (ymd: string) => dateFmt.format(localDate(ymd)),
  /** Data curta para tabelas, ex.: "04/11". */
  shortDate: (ymd: string) => `${ymd.slice(8, 10)}/${ymd.slice(5, 7)}`,
  /** Duração em minutos, ex.: 45 → "45 min", 90 → "1h30", 120 → "2h". */
  duration: (min: number) => {
    const h = Math.floor(min / 60);
    const m = min % 60;
    if (h === 0) return `${m} min`;
    return m === 0 ? `${h}h` : `${h}h${String(m).padStart(2, '0')}`;
  },
  /** Soma minutos a "HH:mm" e devolve "HH:mm" (passando da meia-noite, volta a 00:00). */
  addMinutes: (hhmm: string, min: number) => {
    const [h, m] = hhmm.split(':').map(Number);
    const total = (((h * 60 + m + min) % 1440) + 1440) % 1440;
    return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
  },
  date: (iso: string) => dateFmt.format(new Date(iso)),
  time: (iso: string) => timeFmt.format(new Date(iso)),
  day: (iso: string) => String(new Date(iso).getDate()).padStart(2, '0'),
  month: (iso: string) => monthFmt.format(new Date(iso)).replace('.', ''),
  weekday: (iso: string) => weekdayFmt.format(new Date(iso)).replace('.', ''),
  range: (start: string, end: string) => {
    const s = new Date(start);
    const e = new Date(end);
    const sameDay = s.toDateString() === e.toDateString();
    return sameDay
      ? `${dateFmt.format(s)}, ${timeFmt.format(s)} às ${timeFmt.format(e)}`
      : `${dateFmt.format(s)} ${timeFmt.format(s)} até ${dateFmt.format(e)} ${timeFmt.format(e)}`;
  },
};
