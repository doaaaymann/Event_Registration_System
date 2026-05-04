import { useAuth } from '../context/AuthContext';
import { roleLabel } from '../lib/utils';

export default function ProfilePage() {
  const { user } = useAuth();

  return (
    <div className="mx-auto max-w-3xl space-y-8">
      <section className="hero-shell p-8">
        <p className="section-kicker">Profile</p>
        <h1 className="mt-2 font-display text-5xl leading-none text-slate-950">Your account at a glance.</h1>
        <p className="mt-4 max-w-2xl text-base leading-8 text-slate-600">
          This page is a simple account overview for every user.
        </p>
      </section>

      <section className="glass-panel p-8">
        <div className="grid gap-4 md:grid-cols-2">
          <article className="info-tile">
            <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Full name</p>
            <p className="mt-3 text-base font-semibold text-slate-900">{user?.fullName || 'Unknown user'}</p>
          </article>
          <article className="info-tile">
            <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Email</p>
            <p className="mt-3 text-base font-semibold text-slate-900">{user?.email || 'No email available'}</p>
          </article>
          <article className="info-tile">
            <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Role</p>
            <p className="mt-3 text-base font-semibold text-slate-900">{user?.roles?.map(roleLabel).join(', ') || 'No role assigned'}</p>
          </article>
          <article className="info-tile">
            <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Status</p>
            <p className="mt-3 text-base font-semibold text-slate-900">{user?.status || 'ACTIVE'}</p>
          </article>
        </div>
      </section>
    </div>
  );
}
