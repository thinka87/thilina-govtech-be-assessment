import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { getDefaultRoute } from '../../utils/roleUtils';
import type { Role } from '../../types/auth';

const ForbiddenPage: React.FC = () => {
  const { user, isAuthenticated } = useAuth();
  const homeRoute = isAuthenticated && user
    ? getDefaultRoute(user.role as Role)
    : '/login';

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center px-4 text-center">
      {/* Shield icon */}
      <div className="flex h-20 w-20 items-center justify-center rounded-full bg-red-100 mb-4">
        <svg className="h-10 w-10 text-red-500" fill="none" viewBox="0 0 24 24"
          strokeWidth={1.5} stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round"
            d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z" />
        </svg>
      </div>

      <div className="mb-2 text-5xl font-bold text-red-200 select-none">403</div>
      <h1 className="text-2xl font-semibold text-gray-900 mb-2">Access denied</h1>
      <p className="text-gray-500 text-sm mb-8 max-w-sm">
        You do not have permission to access this page.
        If you believe this is an error, contact your administrator.
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
        Return to dashboard
      </Link>
    </div>
  );
};

export default ForbiddenPage;
