import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LogoutIcon, ProfileIcon } from './Icons';
import { getDefaultRouteForUser, getInitials, hasRole, roleLabel } from '../lib/utils';

function NavItem({ to, children }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `nav-item whitespace-nowrap rounded-full px-3 py-2 text-sm font-medium transition ${
          isActive ? 'nav-item-active bg-slate-900 text-white shadow-lg shadow-slate-900/15' : 'nav-item-idle text-slate-600'
        }`
      }
    >
      <span className="nav-item-label">{children}</span>
    </NavLink>
  );
}

export default function AppShell() {
  const { isAuthenticated, user, logout } = useAuth();
  const [isScrolled, setIsScrolled] = useState(false);
  const roleHome = getDefaultRouteForUser(user);
  const isAdmin = hasRole(user, 'ADMIN');
  const isOrganizer = hasRole(user, 'ORGANIZER');
  const isParticipant = hasRole(user, 'PARTICIPANT');

  useEffect(() => {
    function handleScroll() {
      setIsScrolled(window.scrollY > 18);
    }

    handleScroll();
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className="min-h-screen bg-transparent text-slate-900">
      <div className="app-glow app-glow-left" />
      <div className="app-glow app-glow-right" />

      <header className={`sticky top-0 z-30 transition-all duration-300 ${isScrolled ? 'app-header-scrolled' : 'app-header-top'}`}>
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-3 sm:px-5 lg:px-6">
          <Link to="/" className="flex shrink-0 items-center gap-3">
            <img src="/eventsnow-logo.png" alt="EventsNow" className="brand-logo" />
            <div className="min-w-0">
              <p className="whitespace-nowrap text-base font-semibold tracking-tight text-slate-950">EventsNow</p>
              <p className="brand-subtitle">Event discovery, scheduling, and registration</p>
            </div>
          </Link>

          <nav className="hidden flex-1 items-center justify-center gap-1 lg:flex xl:gap-2">
            <NavItem to="/">Home</NavItem>
            <NavItem to="/events">Find Events</NavItem>
            {isParticipant ? <NavItem to="/tickets">My Tickets</NavItem> : null}
            {isAuthenticated ? <NavItem to="/notifications">Notifications</NavItem> : null}
            {isAdmin ? <NavItem to="/admin">System Administration</NavItem> : null}
            {isOrganizer && !isAdmin ? <NavItem to="/organizer">Organizer Panel</NavItem> : null}
            <NavItem to="/help">Help Center</NavItem>
          </nav>

          <div className="flex shrink-0 items-center gap-2">
            {isAuthenticated ? (
              <>
                <Link to="/profile" className="profile-chip hidden items-center gap-2 text-right lg:flex">
                  <span className="profile-avatar">
                    {getInitials(user?.fullName)}
                  </span>
                  <span className="min-w-0">
                    <p className="whitespace-nowrap text-sm font-semibold text-slate-900">{user?.fullName}</p>
                    <p className="whitespace-nowrap text-[11px] text-slate-500">{user?.roles?.map(roleLabel).join(', ')}</p>
                  </span>
                  <span className="icon-badge">
                    <ProfileIcon className="h-4 w-4" />
                  </span>
                </Link>
                <Link to="/profile" className="mobile-icon-btn" aria-label="Profile">
                  <ProfileIcon className="h-5 w-5" />
                </Link>
                <button onClick={logout} className="btn-secondary icon-btn header-logout-btn">
                  <LogoutIcon className="h-4 w-4" />
                  <span>Log out</span>
                </button>
              </>
            ) : (
              <Link to="/auth" className="btn-primary">
                Account
              </Link>
            )}
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <Outlet />
      </main>

      <footer className="border-t border-white/70 bg-[rgba(252,248,242,0.88)] backdrop-blur">
        <div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 text-sm text-slate-600 sm:px-6 lg:grid-cols-4 lg:px-8">
          <div className="space-y-3">
            <p className="text-base font-semibold text-slate-900">EventsNow</p>
            <p>Discover events, manage schedules, and keep every registration journey feeling smooth from launch to check-in.</p>
          </div>
          <div>
            <p className="footer-title">Quick Links</p>
            <div className="mt-3 space-y-2">
              <Link to="/" className="footer-link">Home</Link>
              <Link to="/events" className="footer-link">Find Events</Link>
              <Link to="/help" className="footer-link">Help Center</Link>
            </div>
          </div>
          <div>
            <p className="footer-title">Account</p>
            <div className="mt-3 space-y-2">
              {isAuthenticated ? (
                <>
                  <Link to={roleHome} className="footer-link">Role Home</Link>
                  {isParticipant ? <Link to="/tickets" className="footer-link">My Tickets</Link> : null}
                  <Link to="/profile" className="footer-link">Profile</Link>
                  <Link to="/notifications" className="footer-link">Notifications</Link>
                </>
              ) : (
                <Link to="/auth" className="footer-link">Sign Up / Login</Link>
              )}
            </div>
          </div>
          <div>
            <p className="footer-title">Connect</p>
            <div className="mt-3 flex gap-3">
              <span className="social-chip">IG</span>
              <span className="social-chip">LI</span>
              <span className="social-chip">X</span>
            </div>
            <p className="mt-4 text-xs text-slate-500">Built for reliable event scheduling, registrations, and notifications.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
