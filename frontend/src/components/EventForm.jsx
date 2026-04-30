import { useEffect, useState } from 'react';

const initialState = {
  title: '',
  description: '',
  location: '',
  startTime: '',
  endTime: '',
  maxSeats: 20,
};

function toLocalDateTimeInput(value) {
  if (!value) return '';
  return value.slice(0, 16);
}

export default function EventForm({ event, onSubmit, onCancel, submitLabel = 'Save event' }) {
  const [form, setForm] = useState(initialState);

  useEffect(() => {
    if (!event) {
      setForm(initialState);
      return;
    }

    setForm({
      title: event.title || '',
      description: event.description || '',
      location: event.location || '',
      startTime: toLocalDateTimeInput(event.startTime),
      endTime: toLocalDateTimeInput(event.endTime),
      maxSeats: event.maxSeats || 20,
    });
  }, [event]);

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((current) => ({
      ...current,
      [name]: name === 'maxSeats' ? Number(value) : value,
    }));
  }

  function handleSubmit(e) {
    e.preventDefault();
    onSubmit({
      ...form,
      maxSeats: Number(form.maxSeats),
    });
  }

  return (
    <form onSubmit={handleSubmit} className="panel space-y-4 p-6">
      <div className="grid gap-4 md:grid-cols-2">
        <label className="space-y-2 text-sm text-slate-600">
          <span>Title</span>
          <input name="title" value={form.title} onChange={handleChange} className="field" required />
        </label>
        <label className="space-y-2 text-sm text-slate-600">
          <span>Location</span>
          <input name="location" value={form.location} onChange={handleChange} className="field" required />
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
      </label>

      <div className="grid gap-4 md:grid-cols-3">
        <label className="space-y-2 text-sm text-slate-600">
          <span>Start time</span>
          <input type="datetime-local" name="startTime" value={form.startTime} onChange={handleChange} className="field" required />
        </label>
        <label className="space-y-2 text-sm text-slate-600">
          <span>End time</span>
          <input type="datetime-local" name="endTime" value={form.endTime} onChange={handleChange} className="field" required />
        </label>
        <label className="space-y-2 text-sm text-slate-600">
          <span>Seats</span>
          <input type="number" min="1" name="maxSeats" value={form.maxSeats} onChange={handleChange} className="field" required />
        </label>
      </div>

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
