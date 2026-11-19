import { useState, type FormEvent, type ReactNode } from 'react';
import { api } from '../api';
import { useAuth } from '../auth';
import { Alert, Field, errorMessage, fieldErrors } from '../components/ui';
import { Link, navigate } from '../router';

function AuthFrame({ title, intro, children }: { title: string; intro: string; children: ReactNode }) {
  return (
    <main className="auth">
      <div className="auth__form-wrap">
        <span className="auth__brand">RetirOn</span>
        <h1 className="auth__title">{title}</h1>
        <p className="auth__intro">{intro}</p>
        {children}
      </div>
    </main>
  );
}

export function LoginPage() {
  const { signIn } = useAuth();
  const [form, setForm] = useState({ organizationSlug: '', email: '', password: '' });
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      signIn(await api.login(form));
      navigate('/', true);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthFrame title="Entrar" intro="Use o identificador da sua organização, seu e-mail e senha.">
      <form className="stack" onSubmit={submit}>
        {error && <Alert>{error}</Alert>}
        <Field label="Organização" hint="O identificador definido no cadastro, ex.: acme-eventos">
          <input required autoFocus value={form.organizationSlug} autoCapitalize="none"
                 onChange={(e) => setForm({ ...form, organizationSlug: e.target.value })} />
        </Field>
        <Field label="E-mail">
          <input type="email" required autoComplete="email" value={form.email}
                 onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </Field>
        <Field label="Senha">
          <input type="password" required autoComplete="current-password" value={form.password}
                 onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </Field>
        <button className="btn btn--primary btn--block" disabled={busy}>{busy ? 'Entrando…' : 'Entrar'}</button>
        <p className="muted center">Nova por aqui? <Link to="/register">Cadastre sua organização</Link></p>
      </form>
    </AuthFrame>
  );
}

function slugify(s: string) {
  return s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
    .replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 60);
}

export function RegisterPage() {
  const { signIn } = useAuth();
  const [form, setForm] = useState({ organizationName: '', organizationSlug: '', name: '', email: '', password: '' });
  const [slugTouched, setSlugTouched] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setFields({});
    try {
      signIn(await api.register(form));
      navigate('/', true);
    } catch (err) {
      setError(errorMessage(err));
      setFields(fieldErrors(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthFrame title="Cadastrar organização"
               intro="Você será o administrador e poderá convidar organizadores e membros depois.">
      <form className="stack" onSubmit={submit}>
        {error && <Alert>{error}</Alert>}
        <Field label="Nome da organização" error={fields.organizationName}>
          <input required autoFocus value={form.organizationName}
                 onChange={(e) => setForm({
                   ...form,
                   organizationName: e.target.value,
                   organizationSlug: slugTouched ? form.organizationSlug : slugify(e.target.value),
                 })} />
        </Field>
        <Field label="Identificador para login" error={fields.organizationSlug}
               hint="Letras minúsculas, números e hífens. Não pode ser alterado depois.">
          <input required value={form.organizationSlug} pattern="[a-z0-9]+(-[a-z0-9]+)*" minLength={3}
                 onChange={(e) => { setSlugTouched(true); setForm({ ...form, organizationSlug: e.target.value }); }} />
        </Field>
        <div className="grid-2">
          <Field label="Seu nome" error={fields.name}>
            <input required autoComplete="name" value={form.name}
                   onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </Field>
          <Field label="E-mail" error={fields.email}>
            <input type="email" required autoComplete="email" value={form.email}
                   onChange={(e) => setForm({ ...form, email: e.target.value })} />
          </Field>
        </div>
        <Field label="Senha" error={fields.password} hint="Mínimo de 8 caracteres">
          <input type="password" required minLength={8} autoComplete="new-password" value={form.password}
                 onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </Field>
        <button className="btn btn--primary btn--block" disabled={busy}>
          {busy ? 'Criando…' : 'Criar organização'}
        </button>
        <p className="muted center">Já tem conta? <Link to="/login">Entrar</Link></p>
      </form>
    </AuthFrame>
  );
}
