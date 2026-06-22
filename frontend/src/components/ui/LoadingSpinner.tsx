import React from 'react';

type SpinnerSize = 'sm' | 'md' | 'lg' | 'xl';

interface LoadingSpinnerProps {
  size?: SpinnerSize;
  className?: string;
  label?: string;
}

const SIZE_STYLES: Record<SpinnerSize, string> = {
  sm:  'h-4 w-4 border-2',
  md:  'h-6 w-6 border-2',
  lg:  'h-10 w-10 border-4',
  xl:  'h-16 w-16 border-4',
};

const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  size = 'md',
  className = '',
  label,
}) => (
  <div className={['flex flex-col items-center gap-2', className].join(' ')}>
    <div
      className={[
        'animate-spin rounded-full border-primary-200 border-t-primary-700',
        SIZE_STYLES[size],
      ].join(' ')}
      role="status"
      aria-label={label ?? 'Loading'}
    />
    {label && (
      <p className="text-sm text-gray-500">{label}</p>
    )}
  </div>
);

export default LoadingSpinner;
