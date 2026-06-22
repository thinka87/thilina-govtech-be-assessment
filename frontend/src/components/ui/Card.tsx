import React from 'react';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  padding?: 'none' | 'sm' | 'md' | 'lg';
  onClick?: () => void;
}

const PADDING_STYLES = {
  none: '',
  sm:   'p-4',
  md:   'p-6',
  lg:   'p-8',
};

const Card: React.FC<CardProps> = ({
  children,
  className = '',
  padding = 'md',
  onClick,
}) => (
  <div
    role={onClick ? 'button' : undefined}
    tabIndex={onClick ? 0 : undefined}
    onClick={onClick}
    onKeyDown={onClick ? (e) => (e.key === 'Enter' || e.key === ' ') && onClick() : undefined}
    className={[
      'bg-white rounded-lg border border-gray-200 shadow-sm',
      PADDING_STYLES[padding],
      className,
    ].join(' ')}
  >
    {children}
  </div>
);

export default Card;
