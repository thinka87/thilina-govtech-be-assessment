import React from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import ProtectedRoute from '../auth/ProtectedRoute';
import RoleBasedRoute from '../auth/RoleBasedRoute';
import AppLayout from '../components/layout/AppLayout';
import LoginPage from '../pages/auth/LoginPage';
import ForceChangePasswordPage from '../pages/auth/ForceChangePasswordPage';
import AdminDashboardPage from '../pages/admin/AdminDashboardPage';
import AdminCitizenListPage from '../pages/admin/citizens/AdminCitizenListPage';
import AdminCitizenCreatePage from '../pages/admin/citizens/AdminCitizenCreatePage';
import AdminCitizenDetailPage from '../pages/admin/citizens/AdminCitizenDetailPage';
import AdminCitizenEditPage from '../pages/admin/citizens/AdminCitizenEditPage';
import AdminServiceRequestListPage from '../pages/admin/service-requests/AdminServiceRequestListPage';
import AdminServiceRequestDetailPage from '../pages/admin/service-requests/AdminServiceRequestDetailPage';
import AdminRequestDocumentsPage from '../pages/admin/service-requests/AdminRequestDocumentsPage';
import AdminStatusHistoryPage from '../pages/admin/service-requests/AdminStatusHistoryPage';
import AgentDashboardPage from '../pages/agent/AgentDashboardPage';
import AgentRequestListPage from '../pages/agent/requests/AgentRequestListPage';
import AgentRequestDetailPage from '../pages/agent/requests/AgentRequestDetailPage';
import AgentRequestEditPage from '../pages/agent/requests/AgentRequestEditPage';
import AgentRequestDocumentsPage from '../pages/agent/requests/AgentRequestDocumentsPage';
import AgentStatusHistoryPage from '../pages/agent/requests/AgentStatusHistoryPage';
import CitizenDashboardPage from '../pages/citizen/CitizenDashboardPage';
import CitizenRequestListPage from '../pages/citizen/requests/CitizenRequestListPage';
import CitizenRequestCreatePage from '../pages/citizen/requests/CitizenRequestCreatePage';
import CitizenRequestDetailPage from '../pages/citizen/requests/CitizenRequestDetailPage';
import AddSupportingDocumentPage from '../pages/citizen/requests/AddSupportingDocumentPage';
import CitizenNotificationsPage from '../pages/citizen/notifications/CitizenNotificationsPage';
import ForbiddenPage from '../pages/common/ForbiddenPage';
import NotFoundPage from '../pages/common/NotFoundPage';
import { getDefaultRoute } from '../utils/roleUtils';
import type { Role } from '../types/auth';

// ── Root redirect ─────────────────────────────────────────────────────────────
// Sends the user to their role-based dashboard or to /login if unauthenticated.

const RootRedirect: React.FC = () => {
  const { isAuthenticated, user } = useAuth();
  if (!isAuthenticated || !user) return <Navigate to="/login" replace />;
  return <Navigate to={getDefaultRoute(user.role as Role)} replace />;
};

// ── Route tree ────────────────────────────────────────────────────────────────

const AppRoutes: React.FC = () => (
  <BrowserRouter>
    <Routes>
      {/* ── Public ── */}
      <Route path="/login" element={<LoginPage />} />

      {/* ── Forced password change (authenticated, no AppLayout) ── */}
      <Route
        path="/change-password"
        element={
          <ProtectedRoute>
            <ForceChangePasswordPage />
          </ProtectedRoute>
        }
      />

      {/* ── Admin ── */}
      <Route
        path="/admin"
        element={
          <ProtectedRoute>
            <RoleBasedRoute allowedRoles={['ADMIN']}>
              <AppLayout />
            </RoleBasedRoute>
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard"                              element={<AdminDashboardPage />} />
        <Route path="citizens"                               element={<AdminCitizenListPage />} />
        <Route path="citizens/create"                        element={<AdminCitizenCreatePage />} />
        <Route path="citizens/:citizenReference"             element={<AdminCitizenDetailPage />} />
        <Route path="citizens/:citizenReference/edit"                              element={<AdminCitizenEditPage />} />
        <Route path="service-requests"                                             element={<AdminServiceRequestListPage />} />
        <Route path="service-requests/:requestReference"                           element={<AdminServiceRequestDetailPage />} />
        <Route path="service-requests/:requestReference/documents"                 element={<AdminRequestDocumentsPage />} />
        <Route path="service-requests/:requestReference/status-history"            element={<AdminStatusHistoryPage />} />
      </Route>

      {/* ── Service Agent ── */}
      <Route
        path="/agent"
        element={
          <ProtectedRoute>
            <RoleBasedRoute allowedRoles={['SERVICE_AGENT']}>
              <AppLayout />
            </RoleBasedRoute>
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard"                                                    element={<AgentDashboardPage />} />
        <Route path="requests"                                                     element={<AgentRequestListPage />} />
        <Route path="requests/:requestReference"                                   element={<AgentRequestDetailPage />} />
        <Route path="requests/:requestReference/edit"                              element={<AgentRequestEditPage />} />
        <Route path="requests/:requestReference/documents"                         element={<AgentRequestDocumentsPage />} />
        <Route path="requests/:requestReference/status-history"                    element={<AgentStatusHistoryPage />} />
      </Route>

      {/* ── Citizen ── */}
      <Route
        path="/citizen"
        element={
          <ProtectedRoute>
            <RoleBasedRoute allowedRoles={['CITIZEN']}>
              <AppLayout />
            </RoleBasedRoute>
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard"                                          element={<CitizenDashboardPage />} />
        <Route path="requests"                                           element={<CitizenRequestListPage />} />
        <Route path="requests/create"                                    element={<CitizenRequestCreatePage />} />
        <Route path="requests/:requestReference"                         element={<CitizenRequestDetailPage />} />
        <Route path="requests/:requestReference/documents/add"           element={<AddSupportingDocumentPage />} />
        <Route path="notifications"                                          element={<CitizenNotificationsPage />} />
      </Route>

      {/* ── Common ── */}
      <Route path="/forbidden" element={<ForbiddenPage />} />
      <Route path="/"          element={<RootRedirect />} />
      <Route path="*"          element={<NotFoundPage />} />
    </Routes>
  </BrowserRouter>
);

export default AppRoutes;
