import { useMemo, useState } from 'react';
import EventForm from '../components/EventForm';
import SectionHeader from '../components/SectionHeader';
import { useAppData } from '../context/AppDataContext';
import { useAuth } from '../context/AuthContext';
import {
  formatDateTime,
  formatStatusLabel,
  getEventStatusTone,
  normalizeOrganizerIds,
  roleLabel,
  validateEmail,
  validatePassword,
} from '../lib/utils';

const tabs = [
  { id: 'create', label: 'Create Event' },
  { id: 'events', label: 'Current Events' },
  { id: 'users', label: 'User Management' },
  { id: 'assign', label: 'Assign Organizers' },
];

const initialUserForm = {
  fullName: '',
  email: '',
  password: '',
  role: 'ORGANIZER',
};

export default function AdminDashboardPage() {
  const { user } = useAuth();
  const {
    assignOrganizerToEvent,
    createEvent,
    createManagedUser,
    events,
    organizerDirectory,
    cancelEvent,
    rescheduleEvent,
    updateEvent,
  } = useAppData();
  const [activeTab, setActiveTab] = useState('create');
  const [editingEvent, setEditingEvent] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [userForm, setUserForm] = useState(initialUserForm);
  const [assignDraft, setAssignDraft] = useState({ eventId: '', organizerIds: [] });
  const [rescheduleDraft, setRescheduleDraft] = useState({ eventId: '', startTime: '', endTime: '' });

  const activeEvents = useMemo(() => events.filter((event) => event.status !== 'CANCELLED'), [events]);
  const cancelledEvents = useMemo(() => events.filter((event) => event.status === 'CANCELLED'), [events]);

  async function handleCreateOrUpdateEvent(payload) {
    setError('');
    setMessage('');

    try {
      if (editingEvent) {
        await updateEvent(editingEvent.id, payload);
        setMessage('Event updated successfully.');
        setEditingEvent(null);
        setActiveTab('events');
      } else {
        await createEvent(payload);
        setMessage('Event created and organizer assignment saved.');
      }
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleCancel(eventId) {
    setError('');
    setMessage('');
    try {
      await cancelEvent(eventId);
      setMessage('Event cancelled successfully.');
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleReschedule(event) {
    if (rescheduleDraft.eventId !== event.id) {
      setRescheduleDraft({
        eventId: event.id,
        startTime: event.startTime?.slice(0, 16) || '',
        endTime: event.endTime?.slice(0, 16) || '',
      });
      return;
    }

    setError('');
    setMessage('');
    try {
      await rescheduleEvent(event.id, {
        startTime: rescheduleDraft.startTime,
        endTime: rescheduleDraft.endTime,
      });
      setRescheduleDraft({ eventId: '', startTime: '', endTime: '' });
      setMessage('Event rescheduled successfully.');
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleAssignOrganizer(e) {
    e.preventDefault();
    setError('');
    setMessage('');

    if (!assignDraft.eventId || !assignDraft.organizerIds.length) {
      setError('Choose an event and at least one organizer.');
      return;
    }

    try {
      await assignOrganizerToEvent(Number(assignDraft.eventId), assignDraft.organizerIds);
      setMessage('Organizers assigned successfully.');
    } catch (err) {
      setError(err.message);
    }
  }

  function toggleAssignedOrganizer(organizerId) {
    setAssignDraft((current) => ({
      ...current,
      organizerIds: current.organizerIds.includes(organizerId)
        ? current.organizerIds.filter((id) => id !== organizerId)
        : [...current.organizerIds, organizerId],
    }));
  }

  async function handleCreateUser(e) {
    e.preventDefault();
    setError('');
    setMessage('');

    if (!userForm.fullName.trim()) {
      setError('Full name is required.');
      return;
    }
    if (!validateEmail(userForm.email)) {
      setError('Enter a valid email address.');
      return;
    }
    if (!userForm.password.trim()) {
      setError('Password cannot be empty.');
      return;
    }
    if (!validatePassword(userForm.password)) {
      setError('Password must be at least 8 characters with uppercase, lowercase, and a number.');
      return;
    }

    try {
      const createdUser = await createManagedUser(userForm);
      setUserForm(initialUserForm);
      setMessage(`${roleLabel(createdUser.roles?.[0] || userForm.role)} account created successfully.`);
    } catch (err) {
      setError(err.message.includes('email') ? 'That email is already in use. Try another one.' : err.message);
    }
  }

  return (
    <div className="grid gap-6 xl:grid-cols-[260px_1fr]">
      <aside className="glass-panel h-fit p-5">
        <p className="section-kicker">System Administration</p>
        <h1 className="mt-2 text-2xl font-semibold text-slate-950">Welcome back, {user?.fullName}</h1>
        <p className="mt-2 text-sm text-slate-500">Role: {roleLabel(user?.roles?.[0])}</p>

        <div className="mt-6 grid gap-2">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={activeTab === tab.id ? 'sidebar-tab-active' : 'sidebar-tab'}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </aside>

      <div className="space-y-6">
        {message ? <p className="success-banner">{message}</p> : null}
        {error ? <p className="error-banner">{error}</p> : null}

        {activeTab === 'create' ? (
          <section className="space-y-4">
            <SectionHeader
              eyebrow="Create Event"
              title={editingEvent ? 'Update selected event' : 'Create a new event'}
              subtitle="Admins can create events, assign an organizer immediately, and keep scheduling accurate."
            />
            <EventForm
              currentUser={user}
              event={editingEvent}
              onSubmit={handleCreateOrUpdateEvent}
              onCancel={editingEvent ? () => setEditingEvent(null) : undefined}
              organizerOptions={organizerDirectory}
              showOrganizerField
              submitLabel={editingEvent ? 'Save changes' : 'Create event'}
            />
          </section>
        ) : null}

        {activeTab === 'events' ? (
          <section className="space-y-5">
            <SectionHeader
              eyebrow="Current Events"
              title="Manage live and upcoming events"
              subtitle="Edit, reschedule, or cancel from one place. Cancelled events stay visible in their own section."
            />

            <div className="grid gap-4">
              {activeEvents.map((event) => (
                <article key={event.id} className="glass-panel p-6">
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                      <h2 className="text-xl font-semibold text-slate-950">{event.title}</h2>
                      <p className="mt-2 text-sm text-slate-600">{event.description}</p>
                      <div className="mt-4 grid gap-1 text-sm text-slate-500">
                        <p>Starts: {formatDateTime(event.startTime)}</p>
                        <p>Ends: {formatDateTime(event.endTime)}</p>
                        <p>Capacity: {event.registeredCount ?? 0} / {event.maxSeats}</p>
                        <p>Organizers: {normalizeOrganizerIds(event).length}</p>
                      </div>
                    </div>
                    <span className={`status-chip ${getEventStatusTone(event.status)}`}>{formatStatusLabel(event.status)}</span>
                  </div>
                  <div className="mt-6 flex flex-wrap gap-3">
                    <button onClick={() => { setEditingEvent(event); setActiveTab('create'); }} className="btn-secondary">
                      Edit
                    </button>
                    <button onClick={() => handleReschedule(event)} className="btn-secondary">
                      {rescheduleDraft.eventId === event.id ? 'Save reschedule' : 'Reschedule'}
                    </button>
                    {rescheduleDraft.eventId === event.id ? (
                      <button onClick={() => setRescheduleDraft({ eventId: '', startTime: '', endTime: '' })} className="btn-ghost">
                        Close
                      </button>
                    ) : null}
                    <button onClick={() => handleCancel(event.id)} className="btn-danger">
                      Cancel
                    </button>
                  </div>
                  {rescheduleDraft.eventId === event.id ? (
                    <div className="mini-tab mt-4 grid gap-4 md:grid-cols-2">
                      <label className="space-y-2 text-sm text-slate-600">
                        <span>New start time</span>
                        <input
                          type="datetime-local"
                          className="field"
                          value={rescheduleDraft.startTime}
                          onChange={(e) => setRescheduleDraft((current) => ({ ...current, startTime: e.target.value }))}
                        />
                      </label>
                      <label className="space-y-2 text-sm text-slate-600">
                        <span>New end time</span>
                        <input
                          type="datetime-local"
                          className="field"
                          value={rescheduleDraft.endTime}
                          onChange={(e) => setRescheduleDraft((current) => ({ ...current, endTime: e.target.value }))}
                        />
                      </label>
                    </div>
                  ) : null}
                </article>
              ))}
            </div>

            <div className="space-y-4">
              <h3 className="text-xl font-semibold text-slate-900">Cancelled events</h3>
              {cancelledEvents.length === 0 ? (
                <div className="glass-panel p-6 text-sm text-slate-500">No cancelled events.</div>
              ) : (
                cancelledEvents.map((event) => (
                  <article key={event.id} className="glass-panel p-6">
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <p className="text-lg font-semibold text-slate-950">{event.title}</p>
                        <p className="mt-2 text-sm text-slate-500">{event.description}</p>
                      </div>
                      <span className={`status-chip ${getEventStatusTone(event.status)}`}>{formatStatusLabel(event.status)}</span>
                    </div>
                  </article>
                ))
              )}
            </div>
          </section>
        ) : null}

        {activeTab === 'users' ? (
          <section className="space-y-4">
            <SectionHeader
              eyebrow="User Management"
              title="Create participant and organizer accounts"
              subtitle="Email format, unique-account checks, and password requirements are enforced before the request is sent."
            />
            <form onSubmit={handleCreateUser} className="glass-panel grid gap-4 p-6 md:grid-cols-2">
              <input
                className="field"
                placeholder="Full name"
                value={userForm.fullName}
                onChange={(e) => setUserForm((current) => ({ ...current, fullName: e.target.value }))}
                required
              />
              <input
                type="email"
                className="field"
                placeholder="Email"
                value={userForm.email}
                onChange={(e) => setUserForm((current) => ({ ...current, email: e.target.value }))}
                required
              />
              <input
                type="password"
                className="field"
                placeholder="Temporary password"
                value={userForm.password}
                onChange={(e) => setUserForm((current) => ({ ...current, password: e.target.value }))}
                required
              />
              <select
                className="field"
                value={userForm.role}
                onChange={(e) => setUserForm((current) => ({ ...current, role: e.target.value }))}
              >
                <option value="ORGANIZER">Organizer</option>
                <option value="PARTICIPANT">Participant</option>
              </select>
              <div className="md:col-span-2">
                <button type="submit" className="btn-primary">
                  Create account
                </button>
              </div>
            </form>
          </section>
        ) : null}

        {activeTab === 'assign' ? (
          <section className="space-y-4">
            <SectionHeader
              eyebrow="Assign Organizers"
              title="Review organizer ownership"
              subtitle="New events can be assigned during creation, and existing events can be reassigned here when plans change."
            />

            <form onSubmit={handleAssignOrganizer} className="glass-panel p-6">
              <div className="grid gap-4">
                <select
                  className="field"
                  value={assignDraft.eventId}
                  onChange={(e) => {
                    const selectedEvent = activeEvents.find((event) => String(event.id) === e.target.value);
                    setAssignDraft({
                      eventId: e.target.value,
                      organizerIds: selectedEvent ? normalizeOrganizerIds(selectedEvent) : [],
                    });
                  }}
                >
                  <option value="">Select event</option>
                  {activeEvents.map((event) => (
                    <option key={event.id} value={event.id}>
                      {event.title}
                    </option>
                  ))}
                </select>
                <div className="grid gap-3 md:grid-cols-2">
                  {organizerDirectory.map((organizer) => (
                    <label key={organizer.id} className="organizer-option">
                      <input
                        type="checkbox"
                        checked={assignDraft.organizerIds.includes(organizer.id)}
                        onChange={() => toggleAssignedOrganizer(organizer.id)}
                      />
                      <span>{organizer.fullName} ({organizer.email})</span>
                    </label>
                  ))}
                </div>
              </div>
              <div className="mt-4">
                <button type="submit" className="btn-primary">
                  Assign organizer
                </button>
              </div>
              <p className="mt-4 text-sm text-slate-600">
                Organizer ownership can be updated here for existing events, and the newly assigned organizer will see the event in their organizer panel.
              </p>
            </form>

            <div className="grid gap-4">
              {activeEvents.map((event) => {
                const organizers = normalizeOrganizerIds(event)
                  .map((organizerId) => organizerDirectory.find((item) => String(item.id) === String(organizerId)) || { id: organizerId })
                  .map((organizer) => organizer.fullName ? `${organizer.fullName} (${organizer.email})` : `User #${organizer.id}`);
                return (
                  <article key={event.id} className="glass-panel p-6">
                    <p className="text-lg font-semibold text-slate-950">{event.title}</p>
                    <p className="mt-2 text-sm text-slate-600">
                      Assigned organizers: {organizers.length ? organizers.join(', ') : 'No organizers assigned'}
                    </p>
                  </article>
                );
              })}
            </div>
          </section>
        ) : null}
      </div>
    </div>
  );
}
