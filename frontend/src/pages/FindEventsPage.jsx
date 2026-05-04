import { Link } from 'react-router-dom';
import { useAppData } from '../context/AppDataContext';
import { useAuth } from '../context/AuthContext';
import { formatShortDate, formatStatusLabel, getEventStatusTone, hasRole } from '../lib/utils';

export default function FindEventsPage() {
  const { events, eventsLoading, registerForEvent, registrations } = useAppData();
  const { isAuthenticated, user } = useAuth();
  const isParticipant = hasRole(user, 'PARTICIPANT');

  async function handleRegister(eventId) {
    await registerForEvent(eventId);
  }

  return (
    <div className="space-y-8">
      <section className="hero-shell p-8">
        <p className="section-kicker">Find events</p>
        <h1 className="mt-2 font-display text-5xl leading-none text-slate-950">Upcoming experiences worth joining.</h1>
        <p className="mt-4 max-w-2xl text-base leading-8 text-slate-600">
          Browse a card-based lineup of upcoming events, then open details or register in a few clicks.
        </p>
      </section>

      {eventsLoading ? (
        <div className="glass-panel p-8 text-sm text-slate-500">Loading events...</div>
      ) : (
        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {events.map((event) => {
            const alreadyRegistered = registrations.some(
              (item) => String(item.eventId || item.event?.id) === String(event.id) && item.status === 'REGISTERED',
            );

            return (
              <article key={event.id} className="event-card">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">{formatShortDate(event.startTime)}</p>
                    <h2 className="mt-3 text-2xl font-semibold text-slate-950">{event.title}</h2>
                  </div>
                  <span className={`status-chip ${getEventStatusTone(event.status)}`}>{formatStatusLabel(event.status)}</span>
                </div>
                <p className="mt-4 text-sm leading-6 text-slate-600">{event.description}</p>
                <div className="mt-6 grid gap-2 text-sm text-slate-500">
                  <p>{event.location}</p>
                  <p>{event.availableSeats ?? 0} seats left</p>
                </div>
                <div className="mt-6 flex flex-wrap gap-3">
                  <Link to={`/events/${event.id}`} className="btn-secondary">
                    View details
                  </Link>
                  {isAuthenticated && isParticipant ? (
                    <button
                      onClick={() => handleRegister(event.id)}
                      className="btn-primary"
                      disabled={alreadyRegistered || !['SCHEDULED', 'RESCHEDULED'].includes(event.status) || event.availableSeats === 0}
                    >
                      {alreadyRegistered ? 'Registered' : 'Register'}
                    </button>
                  ) : null}
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
