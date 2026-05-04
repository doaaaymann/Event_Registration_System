import { useEffect, useState } from 'react';
import { hasRole } from '../lib/utils';

const initialState = {
  title: '',
  description: '',
  location: '',
  startTime: '',
  endTime: '',
  maxSeats: 20,
  organizerIds: [],
};

function toLocalDateTimeInput(value) {
  if (!value) return '';
  return value.slice(0, 16);
}

export default function EventForm({
  currentUser,
  event,
  onSubmit,
  onCancel,
  organizerOptions = [],
  showOrganizerField = false,
  submitLabel = 'Save event',
}) {
  const [form, setForm] = useState(initialState);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    if (!event) {
      setForm({
        ...initialState,
        organizerIds: showOrganizerField
          ? (organizerOptions[0]?.id ? [organizerOptions[0].id] : [])
          : (currentUser?.id ? [currentUser.id] : []),
      });
      return;
    }

    setForm({
      title: event.title || '',
      description: event.description || '',
      location: event.location || '',
      startTime: toLocalDateTimeInput(event.startTime),
      endTime: toLocalDateTimeInput(event.endTime),
      maxSeats: event.maxSeats || 20,
      organizerIds: event.organizerIds || (event.organizerId ? [event.organizerId] : currentUser?.id ? [currentUser.id] : []),
    });
  }, [currentUser?.id, event, organizerOptions, showOrganizerField]);

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((current) => ({
      ...current,
      [name]: name === 'maxSeats' ? Number(value) : value,
    }));
  }

  function toggleOrganizer(organizerId) {
    if (!organizerId) return;
    setForm((current) => ({
      ...current,
      organizerIds: current.organizerIds.includes(organizerId)
        ? current.organizerIds.filter((id) => id !== organizerId)
        : [...current.organizerIds, organizerId],
    }));
  }

  function validateForm() {
    const nextErrors = {};
    if (!form.title.trim()) nextErrors.title = 'Title is required.';
    if (!form.description.trim()) nextErrors.description = 'Description is required.';
    if (!form.location.trim()) nextErrors.location = 'Location is required.';
    if (!form.startTime) nextErrors.startTime = 'Start time is required.';
    if (!form.endTime) nextErrors.endTime = 'End time is required.';
    if (form.startTime && form.endTime && new Date(form.endTime) <= new Date(form.startTime)) {
      nextErrors.endTime = 'End time must be after the start time.';
    }
    if (!form.maxSeats || Number(form.maxSeats) < 1) nextErrors.maxSeats = 'Capacity must be at least 1.';
    if (showOrganizerField && !form.organizerIds.length) nextErrors.organizerIds = 'Choose at least one organizer.';
    return nextErrors;
  }

  function handleSubmit(e) {
    e.preventDefault();
    const nextErrors = validateForm();
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    onSubmit({
      ...form,
      maxSeats: Number(form.maxSeats),
      organizerIds: showOrganizerField ? form.organizerIds.map(Number) : [Number(currentUser?.id)],
    });
  }

  return (
    <form onSubmit={handleSubmit} className="glass-panel space-y-4 p-6">
      <div className="grid gap-4 md:grid-cols-2">
        <label className="space-y-2 text-sm text-slate-600">
          <span>Title</span>
          <input name="title" value={form.title} onChange={handleChange} className="field" required />
          {errors.title ? <p className="field-error">{errors.title}</p> : null}
        </label>
        <label className="space-y-2 text-sm text-slate-600">
          <span>Location</span>
          <input name="location" value={form.location} onChange={handleChange} className="field" required />
          {errors.location ? <p className="field-error">{errors.location}</p> : null}
        </label>
      </div>

      <label className="space-y-2 text-sm text-slate-600">
        <span>Description</span>
        <textarea
          name="description"
          value={form.description}
          onChange={handleChange}
          className="field min-h-28"
          required
        />
        {errors.description ? <p className="field-error">{errors.description}</p> : null}
      </label>

      <div className="grid gap-4 md:grid-cols-3">
        <label className="space-y-2 text-sm text-slate-600">
          <span>Start time</span>
          <input type="datetime-local" name="startTime" value={form.startTime} onChange={handleChange} className="field" required />
          {errors.startTime ? <p className="field-error">{errors.startTime}</p> : null}
        </label>
        <label className="space-y-2 text-sm text-slate-600">
          <span>End time</span>
          <input type="datetime-local" name="endTime" value={form.endTime} onChange={handleChange} className="field" required />
          {errors.endTime ? <p className="field-error">{errors.endTime}</p> : null}
        </label>
        <label className="space-y-2 text-sm text-slate-600">
          <span>Capacity</span>
          <input type="number" min="1" name="maxSeats" value={form.maxSeats} onChange={handleChange} className="field" required />
          {errors.maxSeats ? <p className="field-error">{errors.maxSeats}</p> : null}
        </label>
      </div>

      {showOrganizerField ? (
        <div className="space-y-2 text-sm text-slate-600">
          <span className="block">Organizers</span>
          <div className="grid gap-3 md:grid-cols-2">
            {organizerOptions.map((organizer) => (
              <label key={organizer.id} className="organizer-option">
                <input
                  type="checkbox"
                  checked={form.organizerIds.includes(organizer.id)}
                  onChange={() => toggleOrganizer(organizer.id)}
                />
                <span>{organizer.fullName} ({organizer.email})</span>
              </label>
            ))}
            {!organizerOptions.length && hasRole(currentUser, 'ADMIN') ? (
              <label className="organizer-option">
                <input
                  type="checkbox"
                  checked={form.organizerIds.includes(currentUser?.id)}
                  onChange={() => toggleOrganizer(currentUser?.id)}
                />
                <span>Current admin account</span>
              </label>
            ) : null}
          </div>
          {errors.organizerIds ? <p className="field-error">{errors.organizerIds}</p> : null}
        </div>
      ) : null}

      <div className="flex flex-wrap gap-3">
        <button type="submit" className="btn-primary">
          {submitLabel}
        </button>
        {onCancel ? (
          <button type="button" onClick={onCancel} className="btn-secondary">
            Cancel
          </button>
        ) : null}
      </div>
    </form>
  );
}
