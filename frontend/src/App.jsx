import { Navigate, Route, Routes } from 'react-router-dom';
import AppShell from './components/AppShell';
import ProtectedRoute from './components/ProtectedRoute';
import AdminDashboardPage from './pages/AdminDashboardPage';
import AuthPage from './pages/AuthPage';
import EventDetailsPage from './pages/EventDetailsPage';
import FindEventsPage from './pages/FindEventsPage';
import HelpCenterPage from './pages/HelpCenterPage';
import HomePage from './pages/HomePage';
import MyTicketsPage from './pages/MyTicketsPage';
import NotificationsPage from './pages/NotificationsPage';
import OrganizerPage from './pages/OrganizerPage';
import ProfilePage from './pages/ProfilePage';
import RoleRedirectPage from './pages/RoleRedirectPage';

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<HomePage />} />
        <Route path="/auth" element={<AuthPage />} />
        <Route path="/login" element={<Navigate to="/auth" replace />} />
        <Route path="/register" element={<Navigate to="/auth" replace />} />
        <Route path="/events" element={<FindEventsPage />} />
        <Route path="/events/:eventId" element={<EventDetailsPage />} />
        <Route path="/help" element={<HelpCenterPage />} />
        <Route
          path="/profile"
          element={
            <ProtectedRoute allowedRoles={['ADMIN', 'ORGANIZER', 'PARTICIPANT']}>
              <ProfilePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/notifications"
          element={
            <ProtectedRoute allowedRoles={['ADMIN', 'ORGANIZER', 'PARTICIPANT']}>
              <NotificationsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/tickets"
          element={
            <ProtectedRoute allowedRoles={['PARTICIPANT']}>
              <MyTicketsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <AdminDashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/organizer"
          element={
            <ProtectedRoute allowedRoles={['ORGANIZER']}>
              <OrganizerPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute allowedRoles={['ADMIN', 'ORGANIZER', 'PARTICIPANT']}>
              <RoleRedirectPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
