import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await register({ ...form, role: 'PARTICIPANT' });
      navigate('/login');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-2xl">
      <form onSubmit={handleSubmit} className="panel space-y-5 p-8">
        <div>
          <span className="pill">Participant signup</span>
          <h1 className="mt-5 text-3xl font-semibold text-slate-900">Create a participant account</h1>
          <p className="mt-3 text-sm leading-6 text-slate-500">
            Public registration is limited to participants. Organizers should be created by an admin from the dashboard.
          </p>
        </div>

        <label className="block space-y-2 text-sm text-slate-600">
          <span>Full name</span>
          <input
            className="field"
            value={form.fullName}
            onChange={(e) => setForm((current) => ({ ...current, fullName: e.target.value }))}
            required
          />
        </label>

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

        <div className="flex flex-wrap gap-3">
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Creating account...' : 'Create account'}
          </button>
          <Link to="/login" className="btn-secondary">
            Go to login
          </Link>
        </div>
      </form>
    </div>
  );
}
