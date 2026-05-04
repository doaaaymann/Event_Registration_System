import { useMemo, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getDefaultRouteForUser, validateEmail, validatePassword } from '../lib/utils';

const initialSignup = {
  fullName: '',
  email: '',
  password: '',
};

export default function AuthPage() {
  const { isAuthenticated, loading, login, register, user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [mode, setMode] = useState('login');
  const [loginForm, setLoginForm] = useState({ email: '', password: '' });
  const [signupForm, setSignupForm] = useState(initialSignup);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const redirectTarget = useMemo(
    () => location.state?.from?.pathname || getDefaultRouteForUser(user),
    [location.state?.from?.pathname, user],
  );

  if (!loading && isAuthenticated) {
    return <Navigate to={redirectTarget} replace />;
  }

  function validateLogin() {
    if (!validateEmail(loginForm.email)) return 'Enter a valid email address.';
    if (!loginForm.password.trim()) return 'Password cannot be empty.';
    return '';
  }

  function validateSignup() {
    if (!signupForm.fullName.trim()) return 'Full name is required.';
    if (!validateEmail(signupForm.email)) return 'Enter a valid email address.';
    if (!signupForm.password.trim()) return 'Password cannot be empty.';
    if (!validatePassword(signupForm.password)) {
      return 'Password must be at least 8 characters and include uppercase, lowercase, and a number.';
    }
    return '';
  }

  async function handleLogin(e) {
    e.preventDefault();
    const nextError = validateLogin();
    setError(nextError);
    setSuccess('');
    if (nextError) return;

    setSubmitting(true);
    try {
      const nextUser = await login(loginForm.email, loginForm.password);
      navigate(location.state?.from?.pathname || getDefaultRouteForUser(nextUser), { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleSignup(e) {
    e.preventDefault();
    const nextError = validateSignup();
    setError(nextError);
    setSuccess('');
    if (nextError) return;

    setSubmitting(true);
    try {
      await register({ ...signupForm, role: 'PARTICIPANT' });
      setSuccess('Account created. You can sign in now.');
      setSignupForm(initialSignup);
      setMode('login');
    } catch (err) {
      setError(err.message.includes('email') ? 'That email is already in use. Try another one.' : err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="grid gap-8 lg:grid-cols-[0.95fr_1.05fr]">
      <section className="hero-shell p-8">
        <span className="hero-pill">Dedicated authentication</span>
        <h1 className="mt-5 font-display text-5xl leading-none text-slate-950">One account entry point for every Eventify role.</h1>
        <p className="mt-5 max-w-xl text-base leading-8 text-slate-600">
          Participants sign up here, while organizer and admin accounts are created through admin controls. All flows use
          strict validation for email format, unique accounts, and non-empty passwords.
        </p>
        <div className="mt-8 grid gap-4">
          <div className="info-tile">
            <p className="text-sm font-semibold text-slate-900">Participant access</p>
            <p className="mt-2 text-sm text-slate-600">Browse events, register, and track confirmed tickets without a dashboard.</p>
          </div>
          <div className="info-tile">
            <p className="text-sm font-semibold text-slate-900">Organizer access</p>
            <p className="mt-2 text-sm text-slate-600">Assigned-event view with participant visibility and notifications.</p>
          </div>
          <div className="info-tile">
            <p className="text-sm font-semibold text-slate-900">Admin access</p>
            <p className="mt-2 text-sm text-slate-600">Event creation, scheduling changes, account creation, and organizer assignment.</p>
          </div>
        </div>
      </section>

      <section className="glass-panel p-8">
        <div className="inline-flex rounded-full bg-stone-100 p-1">
          <button onClick={() => setMode('login')} className={mode === 'login' ? 'tab-chip-active' : 'tab-chip'}>
            Login
          </button>
          <button onClick={() => setMode('signup')} className={mode === 'signup' ? 'tab-chip-active' : 'tab-chip'}>
            Sign Up
          </button>
        </div>

        {success ? <p className="success-banner mt-6">{success}</p> : null}
        {error ? <p className="error-banner mt-6">{error}</p> : null}

        {mode === 'login' ? (
          <form onSubmit={handleLogin} className="mt-6 space-y-5">
            <div>
              <h2 className="text-2xl font-semibold text-slate-900">Welcome back</h2>
              <p className="mt-2 text-sm text-slate-500">Sign in and continue to the page that matches your role.</p>
            </div>

            <label className="block space-y-2 text-sm text-slate-600">
              <span>Email</span>
              <input
                type="email"
                className="field"
                value={loginForm.email}
                onChange={(e) => setLoginForm((current) => ({ ...current, email: e.target.value }))}
                required
              />
            </label>

            <label className="block space-y-2 text-sm text-slate-600">
              <span>Password</span>
              <input
                type="password"
                className="field"
                value={loginForm.password}
                onChange={(e) => setLoginForm((current) => ({ ...current, password: e.target.value }))}
                required
              />
            </label>

            <button type="submit" className="btn-primary w-full" disabled={submitting}>
              {submitting ? 'Signing in...' : 'Sign in'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleSignup} className="mt-6 space-y-5">
            <div>
              <h2 className="text-2xl font-semibold text-slate-900">Create a participant account</h2>
              <p className="mt-2 text-sm text-slate-500">Organizer and admin accounts are created inside the admin dashboard.</p>
            </div>

            <label className="block space-y-2 text-sm text-slate-600">
              <span>Full name</span>
              <input
                className="field"
                value={signupForm.fullName}
                onChange={(e) => setSignupForm((current) => ({ ...current, fullName: e.target.value }))}
                required
              />
            </label>

            <label className="block space-y-2 text-sm text-slate-600">
              <span>Email</span>
              <input
                type="email"
                className="field"
                value={signupForm.email}
                onChange={(e) => setSignupForm((current) => ({ ...current, email: e.target.value }))}
                required
              />
            </label>

            <label className="block space-y-2 text-sm text-slate-600">
              <span>Password</span>
              <input
                type="password"
                className="field"
                value={signupForm.password}
                onChange={(e) => setSignupForm((current) => ({ ...current, password: e.target.value }))}
                required
              />
              <p className="text-xs text-slate-500">Minimum 8 characters with uppercase, lowercase, and a number.</p>
            </label>

            <button type="submit" className="btn-primary w-full" disabled={submitting}>
              {submitting ? 'Creating account...' : 'Create account'}
            </button>
          </form>
        )}

        <p className="mt-6 text-sm text-slate-500">
          Looking for events first? <Link to="/events" className="font-semibold text-slate-800">Browse upcoming events</Link>
        </p>
      </section>
    </div>
  );
}
