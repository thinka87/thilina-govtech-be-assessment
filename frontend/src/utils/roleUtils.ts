import type { Role } from '../types/auth';

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN:         'Administrator',
  SERVICE_AGENT: 'Service Agent',
  CITIZEN:       'Citizen',
};

export const getRoleLabel = (role: Role): string => ROLE_LABELS[role] ?? role;

export const getDefaultRoute = (role: Role): string => {
  switch (role) {
    case 'ADMIN':         return '/admin/dashboard';
    case 'SERVICE_AGENT': return '/agent/dashboard';
    case 'CITIZEN':       return '/citizen/dashboard';
    default:              return '/login';
  }
};

export const ROLE_BADGE_STYLES: Record<Role, string> = {
  ADMIN:         'bg-purple-100 text-purple-800',
  SERVICE_AGENT: 'bg-blue-100 text-blue-800',
  CITIZEN:       'bg-green-100 text-green-800',
};
