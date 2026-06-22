import React, { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { getDefaultRoute } from '../../utils/roleUtils';
import type { Role } from '../../types/auth';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import Alert from '../../components/ui/Alert';

const LoginPage: React.FC = () => {
  const { isAuthenticated, user, login } = useAuth();

  const [username, setUsername]   = useState('');
  const [password, setPassword]   = useState('');
  const [error, setError]         = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Already authenticated → redirect to role dashboard
  if (isAuthenticated && user) {
    return <Navigate to={getDefaultRoute(user.role as Role)} replace />;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!username.trim() || !password.trim()) {
      setError('Please enter your email and password.');
      return;
    }

    setIsLoading(true);
    try {
      await login({ username: username.trim(), password });
      // AuthContext.login updates state → re-render triggers the Navigate above
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string }; status?: number } };
      if (axiosErr?.response?.status === 401) {
        const msg = axiosErr?.response?.data?.message ?? '';
        if (msg.toLowerCase().includes('deactivated')) {
          setError(msg);
        } else {
          setError('Invalid email or password. Please try again.');
        }
      } else {
        setError(
          axiosErr?.response?.data?.message ??
          'Unable to connect to the server. Please try again later.',
        );
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center px-4">
      {/* Card */}
      <div className="w-full max-w-md">
        {/* Header */}
        <div className="text-center mb-8">
          {/* Government emblem placeholder */}
          <div className="inline-flex h-16 w-16 items-center justify-center rounded-2xl bg-primary-700 mb-4">
            <svg className="h-9 w-9 text-white" viewBox="0 0 24 24" fill="currentColor">
              <path d="M11.47 3.84a.75.75 0 011.06 0l8.69 8.69a.75.75 0 101.06-1.06l-8.689-8.69a2.25 2.25 0 00-3.182 0l-8.69 8.69a.75.75 0 001.061 1.06l8.69-8.69z" />
              <path d="M12 5.432l8.159 8.159c.03.03.06.058.091.086v6.198c0 1.035-.84 1.875-1.875 1.875H15a.75.75 0 01-.75-.75v-4.5a.75.75 0 00-.75-.75h-3a.75.75 0 00-.75.75V21a.75.75 0 01-.75.75H5.625a1.875 1.875 0 01-1.875-1.875v-6.198a2.29 2.29 0 00.091-.086L12 5.43z" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-gray-900">
            Digital Government Service Platform
          </h1>
          <p className="mt-1 text-sm text-gray-500">
            Sign in to access your account
          </p>
        </div>

        {/* Form card */}
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-8">
          {error && (
            <Alert
              variant="error"
              message={error}
              onDismiss={() => setError('')}
              className="mb-5"
            />
          )}

          <form onSubmit={handleSubmit} noValidate className="space-y-5">
            <Input
              label="Email address"
              type="email"
              autoComplete="username"
              placeholder="you@example.gov.lk"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              disabled={isLoading}
            />

            <Input
              label="Password"
              type="password"
              autoComplete="current-password"
              placeholder="Enter your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              disabled={isLoading}
            />

            <Button
              type="submit"
              variant="primary"
              size="lg"
              isLoading={isLoading}
              className="w-full mt-2"
            >
              Sign in
            </Button>
          </form>
        </div>

        {/* Footer note */}
        <p className="mt-6 text-center text-xs text-gray-400">
          For account access, please contact your system administrator.
        </p>
      </div>
    </div>
  );
};

export default LoginPage;
