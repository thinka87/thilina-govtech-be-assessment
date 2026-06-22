import React from 'react';
import { useAuth } from '../../auth/AuthContext';
import { getRoleLabel, ROLE_BADGE_STYLES } from '../../utils/roleUtils';
import type { Role } from '../../types/auth';

const Topbar: React.FC = () => {
  const { user, logout } = useAuth();

  return (
    <header className="h-14 bg-white border-b border-gray-200 flex items-center justify-between px-6 shrink-0">
      {/* Left — platform breadcrumb placeholder */}
      <div className="flex items-center gap-2">
        <span className="text-sm text-gray-400">
          Digital Government Service Platform
        </span>
      </div>

      {/* Right — user info + logout */}
      {user && (
        <div className="flex items-center gap-3">
          {/* Role badge */}
          <span
            className={[
              'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
              ROLE_BADGE_STYLES[user.role as Role],
            ].join(' ')}
          >
            {getRoleLabel(user.role as Role)}
          </span>

          {/* Username */}
          <span className="text-sm text-gray-700 font-medium hidden sm:block">
            {user.username}
          </span>

          {/* Separator */}
          <span className="hidden sm:block text-gray-300">|</span>

          {/* Logout */}
          <button
            onClick={logout}
            className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-red-600 transition-colors"
          >
            <svg
              className="h-4 w-4"
              fill="none" viewBox="0 0 24 24"
              strokeWidth={1.5} stroke="currentColor"
            >
              <path
                strokeLinecap="round" strokeLinejoin="round"
                d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75"
              />
            </svg>
            <span className="hidden sm:inline">Sign out</span>
          </button>
        </div>
      )}
    </header>
  );
};

export default Topbar;
