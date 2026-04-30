import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { roleLabel } from '../lib/utils';

export default function AppShell() {
  const { isAuthenticated, user, logout } = useAuth();

  return (
    <div className="min-h-screen bg-transparent">
      <header className="sticky top-0 z-20 border-b border-stone-200/80 bg-[#f8f7f4]/95">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
          <Link to="/" className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-900 text-sm font-bold text-white">
              ER
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-900">Event Registration</p>
              <p className="text-xs text-slate-500">Simple event operations for teams and attendees</p>
            </div>
          </Link>

          <nav className="flex items-center gap-2">
            {isAuthenticated ? (
              <>
                <NavLink
                  to="/dashboard"
                  className={({ isActive }) =>
                    `rounded-2xl px-4 py-2 text-sm transition ${
                      isActive ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:bg-white'
                    }`
                  }
                >
                  Dashboard
                </NavLink>
                <div className="hidden rounded-2xl border border-stone-200 bg-white px-4 py-2 text-right sm:block">
                  <p className="text-sm font-semibold text-slate-900">{user?.fullName}</p>
                  <p className="text-xs text-slate-500">{user?.roles?.map(roleLabel).join(', ')}</p>
                </div>
                <button onClick={logout} className="btn-secondary">
                  Logout
                </button>
              </>
            ) : (
              <>
                <NavLink to="/login" className="btn-secondary">
                  Login
                </NavLink>
                <NavLink to="/register" className="btn-primary">
                  Create account
                </NavLink>
              </>
            )}
          </nav>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <Outlet />
      </main>
    </div>
  );
}
