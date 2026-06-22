import React from 'react';

type BadgeVariant =
  | 'default'
  | 'SUBMITTED'
  | 'IN_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'ACTIVE'
  | 'INACTIVE'
  | 'PENDING'
  | 'VERIFIED'
  | 'UNREAD'
  | 'READ'
  | 'success'
  | 'warning'
  | 'danger'
  | 'info';

interface BadgeProps {
  label: string;
  variant?: BadgeVariant;
  className?: string;
}

const VARIANT_STYLES: Record<BadgeVariant, string> = {
  default:   'bg-gray-100 text-gray-700',
  SUBMITTED: 'bg-blue-100 text-blue-800',
  IN_REVIEW: 'bg-yellow-100 text-yellow-800',
  APPROVED:  'bg-green-100 text-green-800',
  REJECTED:  'bg-red-100 text-red-800',
  CANCELLED: 'bg-gray-100 text-gray-700',
  ACTIVE:    'bg-green-100 text-green-800',
  INACTIVE:  'bg-red-100 text-red-800',
  PENDING:   'bg-yellow-100 text-yellow-800',
  VERIFIED:  'bg-green-100 text-green-800',
  UNREAD:    'bg-blue-100 text-blue-800',
  READ:      'bg-gray-100 text-gray-600',
  success:   'bg-green-100 text-green-800',
  warning:   'bg-yellow-100 text-yellow-800',
  danger:    'bg-red-100 text-red-800',
  info:      'bg-blue-100 text-blue-800',
};

const VARIANT_LABELS: Partial<Record<BadgeVariant, string>> = {
  IN_REVIEW: 'In Review',
};

const Badge: React.FC<BadgeProps> = ({ label, variant = 'default', className = '' }) => {
  const displayLabel = VARIANT_LABELS[variant] ?? label;

  return (
    <span
      className={[
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
        VARIANT_STYLES[variant],
        className,
      ].join(' ')}
    >
      {displayLabel}
    </span>
  );
};

export default Badge;
