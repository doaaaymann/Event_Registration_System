import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getDefaultRouteForUser } from '../lib/utils';

export default function RoleRedirectPage() {
  const { user } = useAuth();
  return <Navigate to={getDefaultRouteForUser(user)} replace />;
}
