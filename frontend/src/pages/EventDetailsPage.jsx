import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../lib/api';
import { formatDateTime, getEventStatusTone, normalizeArray } from '../lib/utils';

export default function EventDetailsPage() {
  const { eventId } = useParams();
  const { user } = useAuth();
  const [event, setEvent] = useState(null);
  const [participants, setParticipants] = useState([]);
  const [error, setError] = useState('');

  const canViewParticipants = user?.roles?.includes('ADMIN') || user?.roles?.includes('ORGANIZER');

  useEffect(() => {
    async function load() {
      try {
        const eventRes = await api.get(`/events/${eventId}`);
        setEvent(eventRes.data);

        if (canViewParticipants) {
          const participantsRes = await api.get(`/registrations/events/${eventId}`);
          setParticipants(normalizeArray(participantsRes.data));
        }
      } catch (err) {
        setError(err.message);
      }
    }

    load();
  }, [canViewParticipants, eventId]);

  if (error) {
    return <div className="panel p-8 text-sm text-rose-700">{error}</div>;
  }

  if (!event) {
    return <div className="panel p-8 text-sm text-slate-500">Loading event...</div>;
  }

  return (
    <div className="space-y-8">
      <section className="panel p-8">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-3xl font-semibold text-slate-900">{event.title}</h1>
            <p className="mt-3 text-sm leading-6 text-slate-600">{event.description}</p>
          </div>
          <span className={`inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${getEventStatusTone(event.status)}`}>
            {event.status || 'ACTIVE'}
          </span>
        </div>

        <div className="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <div className="panel-soft p-5">
            <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Location</p>
            <p className="mt-3 text-sm font-medium text-slate-900">{event.location}</p>
          </div>
          <div className="panel-soft p-5">
            <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Starts</p>
            <p className="mt-3 text-sm font-medium text-slate-900">{formatDateTime(event.startTime)}</p>
          </div>
          <div className="panel-soft p-5">
            <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Ends</p>
            <p className="mt-3 text-sm font-medium text-slate-900">{formatDateTime(event.endTime)}</p>
          </div>
          <div className="panel-soft p-5">
            <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Seat limit</p>
            <p className="mt-3 text-sm font-medium text-slate-900">{event.maxSeats}</p>
          </div>
        </div>
      </section>

      {canViewParticipants ? (
        <section className="space-y-4">
          <div>
            <h2 className="text-2xl font-semibold text-slate-900">Registered participants</h2>
            <p className="mt-2 text-sm text-slate-500">Admins and organizers can review registrations for this event.</p>
          </div>
          <div className="grid gap-4">
            {participants.length === 0 ? (
              <div className="panel p-6 text-sm text-slate-500">No participants found yet.</div>
            ) : (
              participants.map((participant) => (
                <article key={participant.id} className="panel p-6">
                  <p className="text-base font-semibold text-slate-900">{participant.participantName || participant.userName || `Participant #${participant.userId}`}</p>
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
