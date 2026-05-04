import { Link } from 'react-router-dom';
import { useAppData } from '../context/AppDataContext';
import { useAuth } from '../context/AuthContext';
import { eventHasOrganizer, formatDateTime, formatStatusLabel, getEventStatusTone } from '../lib/utils';

export default function OrganizerPage() {
  const { events, notifications } = useAppData();
  const { user } = useAuth();
  const assignedEvents = events.filter((event) => eventHasOrganizer(event, user?.id));
  const organizerNotifications = notifications.filter((item) =>
    ['ORGANIZER_ASSIGNED', 'EVENT_CANCELLED', 'EVENT_RESCHEDULED'].includes(item.type),
  );

  return (
    <div className="space-y-8">
      <section className="hero-shell p-8">
        <p className="section-kicker">Organizer Panel</p>
        <h1 className="mt-2 font-display text-5xl leading-none text-slate-950">Assigned events only, with the context you actually need.</h1>
        <p className="mt-4 max-w-2xl text-base leading-8 text-slate-600">
          Organizers can review event details, track registered participants, and stay current on assignment or schedule changes without admin-only controls.
        </p>
      </section>

      <section className="space-y-5">
        <div>
          <p className="section-kicker">Assigned events</p>
          <h2 className="section-title">Your current workload</h2>
        </div>

        <div className="grid gap-5">
          {assignedEvents.length === 0 ? (
            <div className="glass-panel p-8 text-sm text-slate-500">No events assigned yet.</div>
          ) : (
            assignedEvents.map((event) => (
              <article key={event.id} className="glass-panel p-6">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <h2 className="text-2xl font-semibold text-slate-950">{event.title}</h2>
                    <p className="mt-3 text-sm leading-6 text-slate-600">{event.description}</p>
                  </div>
                  <span className={`status-chip ${getEventStatusTone(event.status)}`}>{formatStatusLabel(event.status)}</span>
                </div>
                <div className="mt-5 grid gap-2 text-sm text-slate-500 md:grid-cols-2">
                  <p>Start: {formatDateTime(event.startTime)}</p>
                  <p>End: {formatDateTime(event.endTime)}</p>
                  <p>Registered participants: {event.registeredCount ?? 0}</p>
                  <p>Venue: {event.location}</p>
                </div>
                <div className="mt-6 flex flex-wrap gap-3">
                  <Link to={`/events/${event.id}`} className="btn-secondary">
                    View event details
                  </Link>
                </div>
              </article>
            ))
          )}
        </div>
      </section>

      <section className="space-y-4">
        <div>
          <p className="section-kicker">Notification highlights</p>
          <h2 className="section-title">Recent organizer updates</h2>
        </div>
        <div className="grid gap-4">
          {organizerNotifications.length === 0 ? (
            <div className="glass-panel p-6 text-sm text-slate-500">No organizer notifications yet.</div>
          ) : (
            organizerNotifications.slice(0, 3).map((item) => (
              <article key={item.id} className="glass-panel p-6">
                <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                <p className="mt-2 text-sm text-slate-600">{item.message}</p>
              </article>
            ))
          )}
        </div>
      </section>
    </div>
  );
}
