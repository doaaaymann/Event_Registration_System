import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useAppData } from '../context/AppDataContext';
import api from '../lib/api';
import { eventHasOrganizer, formatDateTime, formatStatusLabel, getEventStatusTone, hasRole, normalizeArray } from '../lib/utils';

export default function EventDetailsPage() {
  const { eventId } = useParams();
  const { isAuthenticated, user } = useAuth();
  const { registerForEvent, registrations } = useAppData();
  const [event, setEvent] = useState(null);
  const [participants, setParticipants] = useState([]);
  const [error, setError] = useState('');
  const [participantsError, setParticipantsError] = useState('');
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const isAdmin = hasRole(user, 'ADMIN');
  const isOrganizer = hasRole(user, 'ORGANIZER');
  const isParticipant = hasRole(user, 'PARTICIPANT');
  const canViewParticipants = isAdmin || (isOrganizer && eventHasOrganizer(event, user?.id));
  const alreadyRegistered = registrations.some((item) => String(item.eventId || item.event?.id) === String(eventId) && item.status === 'REGISTERED');

  useEffect(() => {
    async function load() {
      try {
        const eventRes = await api.get(`/events/${eventId}`);
        const nextEvent = eventRes.data;
        setEvent(nextEvent);
        setParticipants([]);
        setParticipantsError('');

        if (isAdmin || (isOrganizer && eventHasOrganizer(nextEvent, user?.id))) {
          try {
            const participantsRes = await api.get(`/registrations/events/${eventId}`);
            setParticipants(normalizeArray(participantsRes.data));
          } catch (err) {
            setParticipantsError(err.message);
          }
        }
      } catch (err) {
        setError(err.message);
      }
    }

    load();
  }, [eventId, isAdmin, isOrganizer, user?.id]);

  if (error) {
    return <div className="glass-panel p-8 text-sm text-rose-700">{error}</div>;
  }

  if (!event) {
    return <div className="glass-panel p-8 text-sm text-slate-500">Loading event...</div>;
  }

  async function handleRegister() {
    setError('');
    setMessage('');
    setSubmitting(true);
    try {
      await registerForEvent(event.id);
      setMessage('Registration confirmed. Your ticket now appears in My Tickets.');
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="space-y-8">
      <section className="hero-shell p-8">
        <Link to="/events" className="inline-flex items-center text-sm font-semibold text-slate-700 transition hover:text-slate-950">
          Back to Find Events
        </Link>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="section-kicker">Event details</p>
            <h1 className="mt-2 font-display text-4xl text-slate-950">{event.title}</h1>
            <p className="mt-3 text-sm leading-6 text-slate-600">{event.description}</p>
          </div>
          <span className={`inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${getEventStatusTone(event.status)}`}>
            {formatStatusLabel(event.status)}
          </span>
        </div>

        {message ? <p className="success-banner mt-6">{message}</p> : null}
        {error ? <p className="error-banner mt-6">{error}</p> : null}

        <div className="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <div className="info-tile">
            <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Location</p>
            <p className="mt-3 text-sm font-medium text-slate-900">{event.location}</p>
          </div>
          <div className="info-tile">
            <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Starts</p>
            <p className="mt-3 text-sm font-medium text-slate-900">{formatDateTime(event.startTime)}</p>
          </div>
          <div className="info-tile">
            <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Ends</p>
            <p className="mt-3 text-sm font-medium text-slate-900">{formatDateTime(event.endTime)}</p>
          </div>
          <div className="info-tile">
            <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Capacity</p>
            <p className="mt-3 text-sm font-medium text-slate-900">
              {event.registeredCount ?? 0} / {event.maxSeats}
            </p>
          </div>
        </div>

        {isAuthenticated && isParticipant ? (
          <div className="mt-8">
            <button
              onClick={handleRegister}
              className="btn-primary"
              disabled={submitting || alreadyRegistered || !['SCHEDULED', 'RESCHEDULED'].includes(event.status) || event.availableSeats === 0}
            >
              {alreadyRegistered ? 'Already registered' : submitting ? 'Registering...' : 'Register for this event'}
            </button>
          </div>
        ) : null}
      </section>

      {canViewParticipants ? (
        <section className="space-y-4">
          <div>
            <h2 className="text-2xl font-semibold text-slate-900">Registered participants</h2>
            <p className="mt-2 text-sm text-slate-500">Admins and organizers can review registrations for this event.</p>
          </div>
          {participantsError ? <p className="error-banner">{participantsError}</p> : null}
          <div className="grid gap-4">
            {participants.length === 0 ? (
              <div className="glass-panel p-6 text-sm text-slate-500">No participants found yet.</div>
            ) : (
              participants.map((participant) => (
                <article key={participant.id} className="glass-panel p-6">
                  <p className="text-base font-semibold text-slate-900">{participant.participantName || participant.userName || `Participant #${participant.participantId}`}</p>
                  <p className="mt-2 text-sm text-slate-500">Status: {participant.status || 'REGISTERED'}</p>
                </article>
              ))
            )}
          </div>
        </section>
      ) : null}
    </div>
  );
}
