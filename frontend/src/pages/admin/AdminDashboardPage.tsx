import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import PageHeader from '../../components/layout/PageHeader';
import Card from '../../components/ui/Card';

// ── Module card ───────────────────────────────────────────────────────────────

interface ModuleCardProps {
  title: string;
  description: string;
  icon: React.ReactNode;
  to: string;
}

const ModuleCard: React.FC<ModuleCardProps> = ({ title, description, icon, to }) => {
  const navigate = useNavigate();
  return (
    <Card
      className="flex items-start gap-4 cursor-pointer hover:border-primary-300 hover:shadow-md transition-all"
      onClick={() => navigate(to)}
    >
      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-primary-50 text-primary-700">
        {icon}
      </div>
      <div>
        <p className="text-sm font-semibold text-gray-800">{title}</p>
        <p className="text-xs text-gray-500 mt-0.5">{description}</p>
        <span className="mt-2 inline-block text-xs text-primary-600 font-medium">
          Open →
        </span>
      </div>
    </Card>
  );
};

// ── Page ──────────────────────────────────────────────────────────────────────

const AdminDashboardPage: React.FC = () => {
  const { user } = useAuth();

  return (
    <div>
      <PageHeader
        title="Admin Dashboard"
        subtitle="Manage citizens, service requests, documents, and platform data."
      />

      {/* Welcome banner */}
      <div className="mb-6 rounded-lg bg-primary-700 px-6 py-5 text-white">
        <p className="text-sm font-medium opacity-80">Welcome back,</p>
        <p className="text-xl font-semibold mt-0.5">{user?.username}</p>
        <p className="text-sm mt-1 opacity-70">
          You have full administrative access to the platform.
        </p>
      </div>

      {/* Quick-action tiles */}
      <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">
        Platform Modules
      </h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <ModuleCard
          title="Citizen Management"
          description="Create, search, update, and deactivate citizen accounts."
          to="/admin/citizens"
          icon={
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24"
              strokeWidth={1.5} stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round"
                d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" />
            </svg>
          }
        />
        <ModuleCard
          title="Service Requests"
          description="Search, view, and cancel submitted service requests."
          to="/admin/service-requests"
          icon={
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24"
              strokeWidth={1.5} stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round"
                d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25zM6.75 12h.008v.008H6.75V12zm0 3h.008v.008H6.75V15zm0 3h.008v.008H6.75V18z" />
            </svg>
          }
        />
      </div>
    </div>
  );
};

export default AdminDashboardPage;
