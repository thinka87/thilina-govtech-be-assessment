interface BackendErrorShape {
  response?: {
    status?: number;
    data?: {
      message?: string;
      fieldErrors?: Record<string, string>;
    };
  };
  message?: string;
}

/**
 * Extract a user-friendly error message from an Axios error.
 * Handles 403, 404, 409, validation (422), and generic errors.
 */
export const getErrorMessage = (
  error: unknown,
  fallback = 'An unexpected error occurred. Please try again.',
): string => {
  const err = error as BackendErrorShape;
  if (!err.response) return err.message ?? fallback;

  const { status, data } = err.response;
  if (status === 403) return 'You do not have permission to perform this action.';
  if (status === 404) return 'The requested resource was not found.';
  if (status === 409) return data?.message ?? 'A duplicate record already exists.';
  return data?.message ?? fallback;
};

/**
 * Extract field-level validation errors (HTTP 422) from an Axios error.
 * Returns an empty object when no field errors are present.
 */
export const getFieldErrors = (error: unknown): Record<string, string> => {
  const err = error as BackendErrorShape;
  return err.response?.data?.fieldErrors ?? {};
};
