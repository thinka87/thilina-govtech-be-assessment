import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { authService } from '../../auth/authService';
import { getDefaultRoute } from '../../utils/roleUtils';
import type { Role } from '../../types/auth';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import Alert from '../../components/ui/Alert';

interface FormErrors {
  currentPassword?: string;
  newPassword?: string;
  confirmPassword?: string;
}

const ForceChangePasswordPage: React.FC = () => {
  const navigate = useNavigate();
  const { user, logout, clearMustChangePassword } = useAuth();

  const [currentPassword, setCurrentPassword]   = useState('');
  const [newPassword, setNewPassword]           = useState('');
  const [confirmPassword, setConfirmPassword]   = useState('');
  const [errors, setErrors]                     = useState<FormErrors>({});
  const [isLoading, setIsLoading]               = useState(false);
  const [apiError, setApiError]                 = useState('');
  const [success, setSuccess]                   = useState(false);

  const PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$/;

  const validate = (): boolean => {
    const next: FormErrors = {};
    if (!currentPassword) next.currentPassword = 'Current password is required.';
    if (!newPassword) {
      next.newPassword = 'New password is required.';
    } else if (newPassword.length < 8) {
      next.newPassword = 'New password must be at least 8 characters.';
    } else if (!PASSWORD_PATTERN.test(newPassword)) {
      next.newPassword = 'Password must contain at least one uppercase letter, one digit, and one special character.';
    } else if (newPassword === currentPassword) {
      next.newPassword = 'New password must be different from your current password.';
    }
    if (!confirmPassword) {
      next.confirmPassword = 'Please confirm your new password.';
    } else if (confirmPassword !== newPassword) {
      next.confirmPassword = 'Passwords do not match.';
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setIsLoading(true);
    setApiError('');
    try {
      await authService.changePassword({ currentPassword, newPassword });
      clearMustChangePassword();
      setSuccess(true);
      setTimeout(() => {
        navigate(getDefaultRoute(user?.role as Role), { replace: true });
      }, 1500);
    } catch (err: unknown) {
      const axiosErr = err as {
        response?: {
          status?: number;
          data?: { message?: string; fieldErrors?: Record<string, string> };
        };
      };
      if (axiosErr?.response?.status === 400) {
        const fieldErrs = axiosErr?.response?.data?.fieldErrors;
        if (fieldErrs?.newPassword) {
          setErrors((prev) => ({ ...prev, newPassword: fieldErrs.newPassword }));
        } else {
          setApiError(axiosErr?.response?.data?.message ?? 'Failed to change password. Please try again.');
        }
      } else {
        setApiError('Failed to change password. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center px-4">
      <div className="w-full max-w-md">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="inline-flex h-16 w-16 items-center justify-center rounded-2xl bg-primary-700 mb-4">
            <svg className="h-9 w-9 text-white" viewBox="0 0 24 24" fill="currentColor">
              <path d="M11.47 3.84a.75.75 0 011.06 0l8.69 8.69a.75.75 0 101.06-1.06l-8.689-8.69a2.25 2.25 0 00-3.182 0l-8.69 8.69a.75.75 0 001.061 1.06l8.69-8.69z" />
              <path d="M12 5.432l8.159 8.159c.03.03.06.058.091.086v6.198c0 1.035-.84 1.875-1.875 1.875H15a.75.75 0 01-.75-.75v-4.5a.75.75 0 00-.75-.75h-3a.75.75 0 00-.75.75V21a.75.75 0 01-.75.75H5.625a1.875 1.875 0 01-1.875-1.875v-6.198a2.29 2.29 0 00.091-.086L12 5.43z" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-gray-900">
            Password Change Required
          </h1>
          <p className="mt-1 text-sm text-gray-500">
            You are using a temporary password.
            <br />
            Please set a new password to continue.
          </p>
        </div>

        {/* Form card */}
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-8">
          {/* Forced-change notice */}
          <div className="mb-5 flex items-start gap-3 rounded-lg bg-amber-50 border border-amber-200 px-4 py-3">
            <svg className="h-5 w-5 text-amber-500 shrink-0 mt-0.5" fill="none"
              viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round"
                d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
            </svg>
            <p className="text-sm text-amber-800">
              You must change your temporary password before you can access the platform.
            </p>
          </div>

          {success && (
            <Alert
              variant="success"
              title="Password changed successfully!"
              message="Redirecting you to the platform…"
              className="mb-5"
            />
          )}

          {apiError && (
            <Alert
              variant="error"
              message={apiError}
              onDismiss={() => setApiError('')}
              className="mb-5"
            />
          )}

          {!success && (
            <form onSubmit={handleSubmit} noValidate className="space-y-4">
              <Input
                label="Current (Temporary) Password"
                type="password"
                autoComplete="current-password"
                placeholder="Enter your temporary password"
                value={currentPassword}
                onChange={(e) => {
                  setCurrentPassword(e.target.value);
                  if (errors.currentPassword)
                    setErrors((p) => ({ ...p, currentPassword: undefined }));
                }}
                error={errors.currentPassword}
                disabled={isLoading}
              />

              <Input
                label="New Password"
                type="password"
                autoComplete="new-password"
                placeholder="At least 8 characters"
                value={newPassword}
                onChange={(e) => {
                  setNewPassword(e.target.value);
                  if (errors.newPassword)
                    setErrors((p) => ({ ...p, newPassword: undefined }));
                }}
                error={errors.newPassword}
                hint="Min. 8 characters with at least one uppercase letter, one digit, and one special character."
                disabled={isLoading}
              />

              <Input
                label="Confirm New Password"
                type="password"
                autoComplete="new-password"
                placeholder="Re-enter your new password"
                value={confirmPassword}
                onChange={(e) => {
                  setConfirmPassword(e.target.value);
                  if (errors.confirmPassword)
                    setErrors((p) => ({ ...p, confirmPassword: undefined }));
                }}
                error={errors.confirmPassword}
                disabled={isLoading}
              />

              <Button
                type="submit"
                variant="primary"
                size="lg"
                isLoading={isLoading}
                className="w-full mt-2"
              >
                Change Password & Continue
              </Button>
            </form>
          )}
        </div>

        {/* Sign out escape */}
        <p className="mt-4 text-center text-xs text-gray-400">
          Logged in as{' '}
          <span className="font-medium text-gray-600">{user?.username}</span>
          {' · '}
          <button
            onClick={logout}
            className="text-primary-600 hover:underline focus:outline-none"
          >
            Sign out
          </button>
        </p>
      </div>
    </div>
  );
};

export default ForceChangePasswordPage;
