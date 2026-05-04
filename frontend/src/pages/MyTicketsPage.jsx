import { Link } from 'react-router-dom';
import { useAppData } from '../context/AppDataContext';
import { formatDateTime, isConfirmedRegistration } from '../lib/utils';

export default function MyTicketsPage() {
  const { registrations, cancelRegistration } = useAppData();
  const confirmedTickets = registrations.filter(isConfirmedRegistration);

  return (
    <div className="space-y-8">
      <section className="hero-shell p-8">
        <p className="section-kicker">My tickets</p>
        <h1 className="mt-2 font-display text-5xl leading-none text-slate-950">Your confirmed registrations.</h1>
        <p className="mt-4 max-w-2xl text-base leading-8 text-slate-600">
          This page only shows confirmed registrations so attendees can quickly find what they are actually attending.
        </p>
      </section>

      <div className="grid gap-5">
        {confirmedTickets.length === 0 ? (
          <div className="glass-panel p-8 text-sm text-slate-500">
            No confirmed tickets yet. <Link to="/events" className="font-semibold text-slate-800">Explore events</Link> to register.
          </div>
        ) : (
          confirmedTickets.map((ticket) => (
            <article key={ticket.id} className="ticket-card">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Confirmed ticket</p>
                  <h2 className="mt-3 text-2xl font-semibold text-slate-950">
                    {ticket.eventTitle || ticket.event?.title || `Event #${ticket.eventId}`}
                  </h2>
                  <p className="mt-3 text-sm text-slate-600">
                    Registered on {formatDateTime(ticket.createdAt || ticket.updatedAt || ticket.registeredAt)}
                  </p>
                </div>
                <button onClick={() => cancelRegistration(ticket.id)} className="btn-secondary">
                  Cancel registration
                </button>
              </div>
            </article>
          ))
        )}
      </div>
    </div>
  );
}
