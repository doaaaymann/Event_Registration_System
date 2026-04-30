import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import EventForm from '../components/EventForm';
import SectionHeader from '../components/SectionHeader';
import StatCard from '../components/StatCard';
import { useAuth } from '../context/AuthContext';
import api from '../lib/api';
import { formatDateTime, getEventStatusTone, normalizeArray, roleLabel } from '../lib/utils';

function hasRole(user, role) {
  return user?.roles?.includes(role);
}

export default function DashboardPage() {
  const { user } = useAuth();
  const [events, setEvents] = useState([]);
  const [registrations, setRegistrations] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [managedUsers, setManagedUsers] = useState({ fullName: '', email: '', password: '', role: 'ORGANIZER' });
  const [editingEvent, setEditingEvent] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const isAdmin = hasRole(user, 'ADMIN');
  const isOrganizer = hasRole(user, 'ORGANIZER') || isAdmin;
  const isParticipant = hasRole(user, 'PARTICIPANT');

  async function loadData() {
    setLoading(true);
    setError('');
    try {
      const [eventsRes, registrationsRes, notificationsRes] = await Promise.all([
        api.get('/events').catch(() => ({ data: [] })),
        isParticipant ? api.get('/registrations/me').catch(() => ({ data: [] })) : Promise.resolve({ data: [] }),
        user?.id ? api.get(`/notifications/users/${user.id}`).catch(() => ({ data: [] })) : Promise.resolve({ data: [] }),
      ]);

      setEvents(normalizeArray(eventsRes.data));
      setRegistrations(normalizeArray(registrationsRes.data));
      setNotifications(normalizeArray(notificationsRes.data));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, [user?.id, isParticipant]);

  const myEventIds = useMemo(() => new Set(registrations.map((item) => item.eventId || item.event?.id)), [registrations]);
  const organizerEvents = useMemo(() => {
    if (isAdmin) return events;
    return events.filter((event) => String(event.organizerId) === String(user?.id));
  }, [events, isAdmin, user?.id]);

  async function handleCreateOrUpdateEvent(payload) {
    setError('');
    setMessage('');

    try {
      if (editingEvent) {
        await api.put(`/events/${editingEvent.id}`, payload);
        setMessage('Event updated successfully.');
      } else {
        await api.post('/events', { ...payload, organizerId: user.id });
        setMessage('Event created successfully.');
      }
      setEditingEvent(null);
      await loadData();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleRegister(eventId) {
    setError('');
    setMessage('');
    try {
      await api.post('/registrations', { eventId });
      setMessage('Registration completed.');
      await loadData();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleCancelRegistration(registrationId) {
    setError('');
    setMessage('');
    try {
      await api.delete(`/registrations/${registrationId}`);
      setMessage('Registration cancelled.');
      await loadData();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleChangeEventState(eventId, action) {
    setError('');
    setMessage('');
    try {
      if (action === 'cancel') {
        await api.patch(`/events/${eventId}/cancel`);
        setMessage('Event cancelled.');
      } else {
        const startTime = window.prompt('New start time (YYYY-MM-DDTHH:mm)', '');
        const endTime = window.prompt('New end time (YYYY-MM-DDTHH:mm)', '');
        if (!startTime || !endTime) return;
        await api.patch(`/events/${eventId}/reschedule`, { startTime, endTime });
        setMessage('Event rescheduled.');
      }
      await loadData();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleCreateManagedUser(e) {
    e.preventDefault();
    setError('');
    setMessage('');
    try {
      await api.post('/auth/admin/users', managedUsers);
      setMessage(`${roleLabel(managedUsers.role)} created successfully.`);
      setManagedUsers({ fullName: '', email: '', password: '', role: 'ORGANIZER' });
    } catch (err) {
      setError(err.message);
    }
  }

  async function markNotificationRead(notificationId) {
    try {
      await api.patch(`/notifications/${notificationId}/read`);
      await loadData();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="space-y-8">
      <section className="panel p-8">
        <SectionHeader
          eyebrow="Dashboard"
          title={`Welcome back, ${user?.fullName || 'user'}`}
          subtitle="Use the same backend roles and APIs through a quieter, cleaner interface."
        />
        <div className="mt-8 grid gap-4 md:grid-cols-3">
          <StatCard label="Your roles" value={user?.roles?.length || 0} />
          <StatCard label="Available events" value={events.length} tone="success" />
          <StatCard label="Unread notifications" value={notifications.filter((item) => !item.read).length} tone="warning" />
        </div>
      </section>

      {message ? <p className="rounded-2xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</p> : null}
      {error ? <p className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p> : null}

      {isOrganizer ? (
        <section className="space-y-4">
          <SectionHeader
            eyebrow="Organizer tools"
            title={editingEvent ? 'Edit event' : 'Create event'}
            subtitle="Organizers and admins can manage scheduling directly from here."
          />
          <EventForm
            event={editingEvent}
            onSubmit={handleCreateOrUpdateEvent}
            onCancel={editingEvent ? () => setEditingEvent(null) : undefined}
            submitLabel={editingEvent ? 'Update event' : 'Create event'}
          />
        </section>
      ) : null}

      <section className="space-y-4">
        <SectionHeader
          eyebrow="Events"
          title="Current event list"
          subtitle="Browse upcoming sessions, seat limits, and event status."
        />

        {loading ? (
          <div className="panel p-8 text-sm text-slate-500">Loading events...</div>
        ) : (
          <div className="grid gap-4 lg:grid-cols-2">
            {events.map((event) => {
              const alreadyRegistered = myEventIds.has(event.id);
              return (
                <article key={event.id} className="panel p-6">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <Link to={`/events/${event.id}`} className="text-xl font-semibold text-slate-900 hover:text-slate-700">
                        {event.title}
                      </Link>
                      <p className="mt-2 text-sm text-slate-500">{event.location}</p>
                    </div>
                    <span className={`inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${getEventStatusTone(event.status)}`}>
                      {event.status || 'ACTIVE'}
                    </span>
                  </div>
                  <p className="mt-4 text-sm leading-6 text-slate-600">{event.description}</p>
                  <div className="mt-5 grid gap-3 text-sm text-slate-500 sm:grid-cols-2">
                    <p>Starts: {formatDateTime(event.startTime)}</p>
                    <p>Ends: {formatDateTime(event.endTime)}</p>
                    <p>Seats: {event.maxSeats ?? 'N/A'}</p>
                    <p>Organizer ID: {event.organizerId ?? 'N/A'}</p>
                  </div>

                  <div className="mt-6 flex flex-wrap gap-3">
                    {isParticipant && !alreadyRegistered ? (
                      <button onClick={() => handleRegister(event.id)} className="btn-primary">
                        Register
                      </button>
                    ) : null}

                    {isOrganizer && (isAdmin || String(event.organizerId) === String(user?.id)) ? (
                      <>
                        <button onClick={() => setEditingEvent(event)} className="btn-secondary">
                          Edit
                        </button>
                        <button onClick={() => handleChangeEventState(event.id, 'reschedule')} className="btn-secondary">
                          Reschedule
                        </button>
                        <button onClick={() => handleChangeEventState(event.id, 'cancel')} className="btn-danger">
                          Cancel event
                        </button>
                      </>
                    ) : null}
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>

      {isParticipant ? (
        <section className="space-y-4">
          <SectionHeader
            eyebrow="Registrations"
            title="My registrations"
            subtitle="Track the events you joined and cancel when needed."
          />
          <div className="grid gap-4">
            {registrations.length === 0 ? (
              <div className="panel p-6 text-sm text-slate-500">No registrations yet.</div>
            ) : (
              registrations.map((registration) => (
                <article key={registration.id} className="panel p-6">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <p className="text-lg font-semibold text-slate-900">{registration.eventTitle || registration.event?.title || `Event #${registration.eventId}`}</p>
                      <p className="mt-2 text-sm text-slate-500">Status: {registration.status || 'REGISTERED'}</p>
                    </div>
                    <button onClick={() => handleCancelRegistration(registration.id)} className="btn-secondary">
                      Cancel registration
                    </button>
                  </div>
                </article>
              ))
            )}
          </div>
        </section>
      ) : null}

      <section className="space-y-4">
        <SectionHeader
          eyebrow="Notifications"
          title="Latest updates"
          subtitle="See registration confirmations and event schedule changes."
        />
        <div className="grid gap-4">
          {notifications.length === 0 ? (
            <div className="panel p-6 text-sm text-slate-500">No notifications yet.</div>
          ) : (
            notifications.map((notification) => (
              <article key={notification.id} className="panel p-6">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-slate-900">{notification.type || 'Notification'}</p>
                    <p className="mt-2 text-sm leading-6 text-slate-600">{notification.message}</p>
                    <p className="mt-3 text-xs text-slate-400">{formatDateTime(notification.createdAt)}</p>
                  </div>
                  {!notification.read ? (
                    <button onClick={() => markNotificationRead(notification.id)} className="btn-secondary">
                      Mark as read
                    </button>
                  ) : (
                    <span className="pill">Read</span>
                  )}
                </div>
              </article>
            ))
          )}
        </div>
      </section>

      {isAdmin ? (
        <section className="space-y-4">
          <SectionHeader
            eyebrow="Admin tools"
            title="Create organizer account"
            subtitle="This uses the admin-only backend endpoint added for proper role management."
          />
          <form onSubmit={handleCreateManagedUser} className="panel grid gap-4 p-6 md:grid-cols-2">
            <input
              className="field"
              placeholder="Full name"
              value={managedUsers.fullName}
              onChange={(e) => setManagedUsers((current) => ({ ...current, fullName: e.target.value }))}
              required
            />
            <input
              type="email"
              className="field"
              placeholder="Email"
              value={managedUsers.email}
              onChange={(e) => setManagedUsers((current) => ({ ...current, email: e.target.value }))}
              required
            />
            <input
              type="password"
              className="field"
              placeholder="Temporary password"
              value={managedUsers.password}
              onChange={(e) => setManagedUsers((current) => ({ ...current, password: e.target.value }))}
              required
            />
            <select
              className="field"
              value={managedUsers.role}
              onChange={(e) => setManagedUsers((current) => ({ ...current, role: e.target.value }))}
            >
              <option value="ORGANIZER">Organizer</option>
              <option value="PARTICIPANT">Participant</option>
            </select>
            <div className="md:col-span-2">
              <button type="submit" className="btn-primary">
                Create user
              </button>
            </div>
          </form>

          <div className="grid gap-4">
            {organizerEvents.map((event) => (
              <article key={`admin-${event.id}`} className="panel p-6">
                <p className="text-base font-semibold text-slate-900">{event.title}</p>
                <p className="mt-2 text-sm text-slate-500">Organizer ID: {event.organizerId}</p>
              </article>
            ))}
          </div>
        </section>
      ) : null}
    </div>
  );
}
