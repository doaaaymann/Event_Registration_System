import { useAppData } from '../context/AppDataContext';
import { formatDateTime } from '../lib/utils';

export default function NotificationsPage() {
  const { markNotificationRead, notifications } = useAppData();

  return (
    <div className="space-y-8">
      <section className="hero-shell p-8">
        <p className="section-kicker">Notifications</p>
        <h1 className="mt-2 font-display text-5xl leading-none text-slate-950">Updates that matter to your role.</h1>
        <p className="mt-4 max-w-2xl text-base leading-8 text-slate-600">
          Organizer assignment, cancellation, and rescheduling updates are all surfaced here in a cleaner timeline.
        </p>
      </section>

      <div className="grid gap-4">
        {notifications.length === 0 ? (
          <div className="glass-panel p-8 text-sm text-slate-500">No notifications yet.</div>
        ) : (
          notifications.map((notification) => (
            <article key={notification.id} className="glass-panel p-6">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-2">
                  <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">{notification.type}</p>
                  <h2 className="text-lg font-semibold text-slate-950">{notification.title}</h2>
                  <p className="text-sm leading-6 text-slate-600">{notification.message}</p>
                  <p className="text-xs text-slate-400">{formatDateTime(notification.createdAt)}</p>
                </div>
                {!notification.read ? (
                  <button onClick={() => markNotificationRead(notification.id)} className="btn-secondary">
                    Mark as read
                  </button>
                ) : (
                  <span className="status-chip neutral">Read</span>
                )}
              </div>
            </article>
          ))
        )}
      </div>
    </div>
  );
}
