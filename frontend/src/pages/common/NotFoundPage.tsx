import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { getDefaultRoute } from '../../utils/roleUtils';
import type { Role } from '../../types/auth';

const NotFoundPage: React.FC = () => {
  const { user, isAuthenticated } = useAuth();
  const homeRoute = isAuthenticated && user
    ? getDefaultRoute(user.role as Role)
    : '/login';

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center px-4 text-center">
      <div className="mb-4 text-8xl font-bold text-primary-200 select-none">404</div>
      <h1 className="text-2xl font-semibold text-gray-900 mb-2">Page not found</h1>
      <p className="text-gray-500 text-sm mb-8 max-w-sm">
        The page you are looking for does not exist or has been moved.
      </p>
      <Link
        to={homeRoute}
        className="inline-flex items-center gap-2 rounded-md bg-primary-700 px-5 py-2.5 text-sm font-medium text-white hover:bg-primary-800 transition-colors"
      >
        <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24"
          strokeWidth={1.5} stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round"
            d="M2.25 12l8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25" />
        </svg>
        Return home
      </Link>
    </div>
  );
};

export default NotFoundPage;
