import { Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export default function ProtectedRoute({ children, requiredRole, requiredRoles }) {
  const { user, loading, hasRole } = useAuth();

  if (loading) {
    return <div className="page-loading">Loading...</div>;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  const allowedRoles = requiredRoles || (requiredRole ? [requiredRole] : []);
  if (allowedRoles.length > 0 && !allowedRoles.some(hasRole)) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}
