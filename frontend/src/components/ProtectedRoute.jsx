import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole } from '../lib/utils';

export default function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, loading, user } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="panel mx-auto max-w-xl p-10 text-center">
        <p className="text-sm text-slate-500">Checking your session...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/auth" replace state={{ from: location }} />;
  }

  if (allowedRoles?.length && !hasAnyRole(user, allowedRoles)) {
    return <Navigate to="/" replace />;
  }

  return children;
}
