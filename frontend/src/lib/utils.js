export function formatDateTime(value) {
  if (!value) return 'Not scheduled';

  try {
    return new Intl.DateTimeFormat('en-US', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(value));
  } catch {
    return value;
  }
}

export function formatShortDate(value) {
  if (!value) return 'TBA';

  try {
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    }).format(new Date(value));
  } catch {
    return value;
  }
}

export function roleLabel(role) {
  return role
    ?.toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

export function normalizeArray(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data)) return payload.data;
  return [];
}

export function hasRole(user, role) {
  return user?.roles?.includes(role);
}

export function normalizeOrganizerIds(event) {
  if (Array.isArray(event?.organizerIds) && event.organizerIds.length) {
    return event.organizerIds.map((id) => Number(id)).filter(Number.isFinite);
  }
  if (event?.organizerId != null) {
    return [Number(event.organizerId)].filter(Number.isFinite);
  }
  return [];
}

export function eventHasOrganizer(event, userId) {
  return normalizeOrganizerIds(event).some((organizerId) => String(organizerId) === String(userId));
}

export function hasAnyRole(user, roles) {
  return roles.some((role) => hasRole(user, role));
}

export function getPrimaryRole(user) {
  if (hasRole(user, 'ADMIN')) return 'ADMIN';
  if (hasRole(user, 'ORGANIZER')) return 'ORGANIZER';
  if (hasRole(user, 'PARTICIPANT')) return 'PARTICIPANT';
  return '';
}

export function getDefaultRouteForUser(user) {
  const primaryRole = getPrimaryRole(user);

  if (primaryRole === 'ADMIN') return '/admin';
  if (primaryRole === 'ORGANIZER') return '/organizer';
  if (primaryRole === 'PARTICIPANT') return '/events';
  return '/';
}

export function getEventStatusTone(status) {
  if (status === 'CANCELLED') return 'bg-rose-100 text-rose-700 border-rose-200';
  if (status === 'RESCHEDULED') return 'bg-amber-100 text-amber-800 border-amber-200';
  return 'bg-emerald-100 text-emerald-800 border-emerald-200';
}

export function formatStatusLabel(status) {
  if (status === 'SCHEDULED') return 'Upcoming';
  if (status === 'RESCHEDULED') return 'Rescheduled';
  if (status === 'CANCELLED') return 'Cancelled';
  return status || 'Upcoming';
}

export function isConfirmedRegistration(registration) {
  return (registration?.status || '').toUpperCase() === 'REGISTERED';
}

export function validateEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

export function validatePassword(password) {
  return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,100}$/.test(password || '');
}

export function getInitials(name) {
  return (name || 'User')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('');
}
