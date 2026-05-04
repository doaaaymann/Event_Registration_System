import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { useAuth } from './AuthContext';
import api from '../lib/api';
import {
  eventHasOrganizer,
  getDefaultRouteForUser,
  hasRole,
  normalizeOrganizerIds,
  normalizeArray,
} from '../lib/utils';

const AppDataContext = createContext(null);

export function AppDataProvider({ children }) {
  const { isAuthenticated, user } = useAuth();
  const [events, setEvents] = useState([]);
  const [registrations, setRegistrations] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [organizerDirectory, setOrganizerDirectory] = useState([]);
  const [managedUsers, setManagedUsers] = useState([]);
  const [eventsLoading, setEventsLoading] = useState(true);
  const [accountLoading, setAccountLoading] = useState(false);

  async function loadEvents() {
    setEventsLoading(true);
    try {
      const response = await api.get('/events');
      setEvents(normalizeArray(response.data));
    } finally {
      setEventsLoading(false);
    }
  }

  async function loadAccountData() {
    if (!isAuthenticated || !user?.id) {
      setRegistrations([]);
      setNotifications([]);
      return;
    }

    setAccountLoading(true);
    try {
      const requests = [
        hasRole(user, 'PARTICIPANT') ? api.get('/registrations/me').catch(() => ({ data: [] })) : Promise.resolve({ data: [] }),
        api.get(`/notifications/users/${user.id}`).catch(() => ({ data: [] })),
      ];

      const [registrationsResponse, notificationsResponse] = await Promise.all(requests);
      setRegistrations(normalizeArray(registrationsResponse.data));
      setNotifications(normalizeArray(notificationsResponse.data));
    } finally {
      setAccountLoading(false);
    }
  }

  async function loadOrganizerDirectory() {
    if (!isAuthenticated || !hasRole(user, 'ADMIN')) {
      setOrganizerDirectory([]);
      return;
    }

    const organizerIds = Array.from(
      new Set(
        [
          ...events.flatMap((event) => normalizeOrganizerIds(event)),
          ...managedUsers.filter((account) => account.roles?.includes('ORGANIZER')).map((account) => account.id),
        ].filter(Boolean),
      ),
    );

    if (organizerIds.length === 0) {
      setOrganizerDirectory([]);
      return;
    }

    const settled = await Promise.allSettled(
      organizerIds.map((organizerId) => api.get(`/auth/users/${organizerId}`)),
    );

    setOrganizerDirectory(
      settled
        .filter((item) => item.status === 'fulfilled')
        .map((item) => item.value.data)
        .filter((account) => account?.roles?.includes('ORGANIZER')),
    );
  }

  useEffect(() => {
    loadEvents().catch(() => setEvents([]));
  }, []);

  useEffect(() => {
    loadAccountData().catch(() => {
      setRegistrations([]);
      setNotifications([]);
    });
  }, [isAuthenticated, user?.id]);

  useEffect(() => {
    loadOrganizerDirectory().catch(() => setOrganizerDirectory([]));
  }, [events, isAuthenticated, managedUsers, user]);

  async function refreshAll() {
    await loadEvents();
    await loadAccountData();
  }

  async function createNotification(payload) {
    const response = await api.post('/notifications', payload);
    await loadAccountData();
    return response.data;
  }

  async function createEvent(payload) {
    const response = await api.post('/events', payload);

    if (hasRole(user, 'ADMIN')) {
      await createNotification({
        userId: user.id,
        type: 'ADMIN_EVENT_CREATED',
        title: 'Event created',
        message: `${payload.title} was created successfully.`,
      }).catch(() => null);
    }

    if (hasRole(user, 'ADMIN')) {
      await Promise.all(
        normalizeOrganizerIds(payload)
          .filter((organizerId) => String(organizerId) !== String(user?.id))
          .map((organizerId) => createNotification({
            userId: organizerId,
            type: 'ORGANIZER_ASSIGNED',
            title: 'New Event Assignment',
            message: `You were assigned to organize ${payload.title}.`,
          }).catch(() => null)),
      );
    }

    await refreshAll();
    return response.data;
  }

  async function updateEvent(eventId, payload) {
    const response = await api.put(`/events/${eventId}`, payload);
    if (hasRole(user, 'ADMIN')) {
      await createNotification({
        userId: user.id,
        type: 'ADMIN_EVENT_UPDATED',
        title: 'Event updated',
        message: `${payload.title} was updated successfully.`,
      }).catch(() => null);
    }
    await refreshAll();
    return response.data;
  }

  async function cancelEvent(eventId) {
    const response = await api.patch(`/events/${eventId}/cancel`);
    const event = events.find((item) => String(item.id) === String(eventId));
    if (hasRole(user, 'ADMIN')) {
      await createNotification({
        userId: user.id,
        type: 'ADMIN_EVENT_CANCELLED',
        title: 'Event cancelled',
        message: `${event?.title || `Event #${eventId}`} was cancelled.`,
      }).catch(() => null);
    }
    await refreshAll();
    return response.data;
  }

  async function rescheduleEvent(eventId, payload) {
    const response = await api.patch(`/events/${eventId}/reschedule`, payload);
    const event = events.find((item) => String(item.id) === String(eventId));
    if (hasRole(user, 'ADMIN')) {
      await createNotification({
        userId: user.id,
        type: 'ADMIN_EVENT_RESCHEDULED',
        title: 'Event rescheduled',
        message: `${event?.title || `Event #${eventId}`} was rescheduled.`,
      }).catch(() => null);
    }
    await refreshAll();
    return response.data;
  }

  async function assignOrganizerToEvent(eventId, organizerIds) {
    const event = events.find((item) => String(item.id) === String(eventId));
    const previousOrganizerIds = normalizeOrganizerIds(event);
    const nextOrganizerIds = organizerIds.map(Number);
    const response = await api.patch(`/events/${eventId}/organizer`, { organizerIds: nextOrganizerIds });

    if (hasRole(user, 'ADMIN')) {
      await Promise.all(
        nextOrganizerIds
          .filter((organizerId) => !previousOrganizerIds.includes(organizerId))
          .filter((organizerId) => String(organizerId) !== String(user?.id))
          .map((organizerId) => createNotification({
            userId: organizerId,
            type: 'ORGANIZER_ASSIGNED',
            title: 'Event Assignment Updated',
            message: `You were assigned to organize ${event?.title || `Event #${eventId}`}.`,
          }).catch(() => null)),
      );
    }

    await refreshAll();
    return response.data;
  }

  async function registerForEvent(eventId) {
    const response = await api.post('/registrations', { eventId });
    await loadAccountData();
    await loadEvents();
    return response.data;
  }

  async function cancelRegistration(registrationId) {
    const response = await api.delete(`/registrations/${registrationId}`);
    await loadAccountData();
    await loadEvents();
    return response.data;
  }

  async function markNotificationRead(notificationId) {
    const response = await api.patch(`/notifications/${notificationId}/read`);
    await loadAccountData();
    return response.data;
  }

  async function createManagedUser(payload) {
    const response = await api.post('/auth/admin/users', payload);
    setManagedUsers((current) => [response.data, ...current.filter((item) => item.id !== response.data.id)]);
    return response.data;
  }

  const value = useMemo(
    () => ({
      accountLoading,
      assignOrganizerToEvent,
      cancelEvent,
      cancelRegistration,
      createNotification,
      createEvent,
      createManagedUser,
      events,
      eventsLoading,
      managedUsers,
      notifications,
      organizerDirectory,
      refreshAll,
      registerForEvent,
      registrations,
      rescheduleEvent,
      roleHome: getDefaultRouteForUser(user),
      unreadNotifications: notifications.filter((item) => !item.read).length,
      updateEvent,
      markNotificationRead,
    }),
    [accountLoading, events, eventsLoading, managedUsers, notifications, organizerDirectory, registrations, user],
  );

  return <AppDataContext.Provider value={value}>{children}</AppDataContext.Provider>;
}

export function useAppData() {
  const context = useContext(AppDataContext);

  if (!context) {
    throw new Error('useAppData must be used inside AppDataProvider');
  }

  return context;
}
