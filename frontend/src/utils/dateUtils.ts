const TIMEZONE = 'Asia/Colombo';

export const formatDateTime = (value: string | null | undefined): string => {
  if (!value) return '—';
  try {
    return new Date(value).toLocaleString('en-GB', {
      day:      '2-digit',
      month:    'short',
      year:     'numeric',
      hour:     '2-digit',
      minute:   '2-digit',
      timeZone: TIMEZONE,
    });
  } catch {
    return value;
  }
};

export const formatDate = (value: string | null | undefined): string => {
  if (!value) return '—';
  try {
    return new Date(value).toLocaleDateString('en-GB', {
      day:      '2-digit',
      month:    'short',
      year:     'numeric',
      timeZone: TIMEZONE,
    });
  } catch {
    return value;
  }
};
