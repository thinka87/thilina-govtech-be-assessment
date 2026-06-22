import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import PageHeader from '../../components/layout/PageHeader';
import Card from '../../components/ui/Card';

interface ActionCardProps {
  title: string;
  description: string;
  icon: React.ReactNode;
  to?: string;
}

const ActionCard: React.FC<ActionCardProps> = ({ title, description, icon, to }) => {
  const navigate = useNavigate();
  return (
    <Card
      className={['flex items-start gap-4', to ? 'cursor-pointer hover:shadow-md transition-shadow' : ''].join(' ')}
      onClick={to ? () => navigate(to) : undefined}
    >
      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-700">
        {icon}
      </div>
      <div>
        <p className="text-sm font-semibold text-gray-800">{title}</p>
        <p className="text-xs text-gray-500 mt-0.5">{description}</p>
        {to && (
          <span className="mt-2 inline-block text-xs text-primary-600 font-medium">
            Open →
          </span>
        )}
      </div>
    </Card>
  );
};

const AgentDashboardPage: React.FC = () => {
  const { user } = useAuth();

  return (
    <div>
      <PageHeader
        title="Service Agent Dashboard"
        subtitle="Review citizens, process service requests, verify documents, and update statuses."
      />

      {/* Welcome banner */}
      <div className="mb-6 rounded-lg bg-blue-700 px-6 py-5 text-white">
        <p className="text-sm font-medium opacity-80">Welcome back,</p>
        <p className="text-xl font-semibold mt-0.5">{user?.username}</p>
        <p className="text-sm mt-1 opacity-70">
          You can review and process citizen service requests.
        </p>
      </div>

      {/* Modules */}
      <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">
        Agent Modules
      </h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <ActionCard
          title="Service Requests"
          description="Search, view details, and update the status of service requests."
          to="/agent/requests"
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

export default AgentDashboardPage;
