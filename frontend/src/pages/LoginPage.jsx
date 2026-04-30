import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login(form.email, form.password);
      navigate(location.state?.from?.pathname || '/dashboard', { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="grid gap-8 lg:grid-cols-[0.95fr_1.05fr]">
      <section className="panel p-8">
        <span className="pill">Sign in</span>
        <h1 className="mt-5 text-3xl font-semibold text-slate-900">Access your event workspace</h1>
        <p className="mt-3 text-sm leading-6 text-slate-500">
          Use the API gateway-backed login flow for admins, organizers, or participants.
        </p>
        <div className="mt-8 rounded-3xl border border-stone-200 bg-stone-50 p-5 text-sm text-slate-600">
          <p className="font-medium text-slate-900">Default admin account</p>
          <p className="mt-2">Fresh Docker databases should use `admin@event.local / EventAdmin123!`.</p>
          <p className="mt-2">If you reused an older Docker volume, the previous password may still exist in the seeded row.</p>
        </div>
      </section>

      <form onSubmit={handleSubmit} className="panel space-y-5 p-8">
        <div>
          <h2 className="text-xl font-semibold text-slate-900">Login</h2>
          <p className="mt-2 text-sm text-slate-500">Enter your email and password to continue.</p>
        </div>

        <label className="block space-y-2 text-sm text-slate-600">
          <span>Email</span>
          <input
            type="email"
            className="field"
            value={form.email}
            onChange={(e) => setForm((current) => ({ ...current, email: e.target.value }))}
            required
          />
        </label>

        <label className="block space-y-2 text-sm text-slate-600">
          <span>Password</span>
          <input
            type="password"
            className="field"
            value={form.password}
            onChange={(e) => setForm((current) => ({ ...current, password: e.target.value }))}
            required
          />
        </label>

        {error ? <p className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p> : null}

        <button type="submit" className="btn-primary w-full" disabled={loading}>
          {loading ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
    </div>
  );
}
