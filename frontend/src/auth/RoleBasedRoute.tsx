import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from './AuthContext';
import type { Role } from '../types/auth';

interface RoleBasedRouteProps {
  allowedRoles: Role[];
  children: React.ReactNode;
}

/**
 * Redirects authenticated users who do not hold one of the allowed roles
 * to /forbidden.
 */
const RoleBasedRoute: React.FC<RoleBasedRouteProps> = ({ allowedRoles, children }) => {
  const { user } = useAuth();

  if (!user || !allowedRoles.includes(user.role)) {
    return <Navigate to="/forbidden" replace />;
  }

  return <>{children}</>;
};

export default RoleBasedRoute;
