import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import { notificationApi } from '../../../api/notificationApi';
import type { NotificationResponse, NotificationStatus } from '../../../types/notification';
import { getErrorMessage } from '../../../utils/errorUtils';
import { formatDateTime } from '../../../utils/dateUtils';
import PageHeader from '../../../components/layout/PageHeader';
import Button from '../../../components/ui/Button';
import Badge from '../../../components/ui/Badge';
import Alert from '../../../components/ui/Alert';
import Card from '../../../components/ui/Card';
import LoadingSpinner from '../../../components/ui/LoadingSpinner';

const PAGE_SIZE = 10;

const CitizenNotificationsPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const citizenRef = user?.citizenReference ?? null;

  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [loading, setLoading]             = useState(false);
  const [error, setError]                 = useState('');
  const [page, setPage]                   = useState(0);
  const [totalPages, setTotalPages]       = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Per-notification marking state
  const [markingIds, setMarkingIds]   = useState<Set<number>>(new Set());
  const [markError, setMarkError]     = useState('');
  const [markSuccess, setMarkSuccess] = useState('');

  const fetchNotifications = useCallback(async () => {
    if (!citizenRef) return;
    setLoading(true);
    setError('');
    try {
      const data = await notificationApi.getCitizenNotifications(citizenRef, { page, size: PAGE_SIZE });
      setNotifications(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      const msg = getErrorMessage(err, 'Unable to load notifications. Please try again.');
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [citizenRef, page]);

  useEffect(() => { fetchNotifications(); }, [fetchNotifications]);

  const handleMarkAsRead = async (notificationId: number) => {
    setMarkingIds((prev) => new Set(prev).add(notificationId));
    setMarkError('');
    setMarkSuccess('');
    try {
      await notificationApi.markNotificationAsRead(notificationId);
      setMarkSuccess('Notification marked as read.');
      // Optimistically update the status in the list
      setNotifications((prev) =>
        prev.map((n) => n.id === notificationId ? { ...n, status: 'READ' as NotificationStatus } : n),
      );
    } catch (err) {
      setMarkError(getErrorMessage(err, 'Failed to mark notification as read.'));
    } finally {
      setMarkingIds((prev) => {
        const next = new Set(prev);
        next.delete(notificationId);
        return next;
      });
    }
  };

  const unreadCount = notifications.filter((n) => n.status === 'UNREAD').length;
  const startRecord = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const endRecord   = Math.min((page + 1) * PAGE_SIZE, totalElements);

  // ── Missing citizenReference guard ──────────────────────────────────────────
  if (!citizenRef) {
    return (
      <div>
        <PageHeader title="Notifications" />
        <Alert
          variant="error"
          title="Profile reference missing"
          message="Citizen profile reference is missing. Please log in again or contact support."
          className="mb-4"
        />
        <Button variant="secondary" onClick={() => navigate('/login')}>
          Go to Login
        </Button>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Notifications"
        subtitle="Track updates related to your service requests."
      />

      {/* ── Status messages ── */}
      {markSuccess && (
        <Alert
          variant="success"
          message={markSuccess}
          onDismiss={() => setMarkSuccess('')}
          className="mb-4"
        />
      )}
      {markError && (
        <Alert
          variant="error"
          message={markError}
          onDismiss={() => setMarkError('')}
          className="mb-4"
        />
      )}
      {error && (
        <Alert
          variant="error"
          message={error}
          onDismiss={() => setError('')}
          className="mb-4"
        />
      )}

      {/* ── Unread summary ── */}
      {!loading && unreadCount > 0 && (
        <div className="mb-4 flex items-center gap-2 text-sm text-blue-700 bg-blue-50 border border-blue-200 rounded-md px-4 py-2">
          <svg className="h-4 w-4 shrink-0" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a.75.75 0 000 1.5h.253a.25.25 0 01.244.304l-.459 2.066A1.75 1.75 0 0010.747 15H11a.75.75 0 000-1.5h-.253a.25.25 0 01-.244-.304l.459-2.066A1.75 1.75 0 009.253 9H9z" clipRule="evenodd" />
          </svg>
          <span>
            You have <strong>{unreadCount}</strong> unread notification{unreadCount !== 1 ? 's' : ''} on this page.
          </span>
        </div>
      )}

      {/* ── Content ── */}
      {loading ? (
        <div className="flex justify-center py-16">
          <LoadingSpinner size="lg" label="Loading notifications…" />
        </div>
      ) : notifications.length === 0 ? (
        <Card>
          <div className="text-center py-12">
            <svg className="mx-auto h-12 w-12 text-gray-300 mb-3" fill="none"
              viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" />
            </svg>
            <p className="text-sm font-medium text-gray-600">You do not have any notifications yet.</p>
            <p className="text-xs text-gray-400 mt-1">
              Notifications will appear here when your service request status changes.
            </p>
          </div>
        </Card>
      ) : (
        <div className="space-y-3">
          {notifications.map((notif) => {
            const isUnread  = notif.status === 'UNREAD';
            const isMarking = markingIds.has(notif.id);

            return (
              <div
                key={notif.id}
                className={[
                  'rounded-lg border shadow-sm transition-colors',
                  isUnread
                    ? 'bg-blue-50 border-blue-200'
                    : 'bg-white border-gray-200',
                ].join(' ')}
              >
                <div className="p-4 sm:p-5">
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex-1 min-w-0">
                      {/* Message */}
                      <p className={[
                        'text-sm leading-relaxed mb-2',
                        isUnread ? 'font-semibold text-gray-900' : 'text-gray-700',
                      ].join(' ')}>
                        {notif.message}
                      </p>

                      {/* Meta row */}
                      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-gray-500">
                        <span className="font-mono">{notif.requestReference}</span>
                        <span>{formatDateTime(notif.createdAt)}</span>
                      </div>
                    </div>

                    {/* Right column: badge + action */}
                    <div className="flex flex-col items-end gap-2 shrink-0">
                      <Badge
                        label={notif.status}
                        variant={notif.status as NotificationStatus}
                      />
                      {isUnread && (
                        <Button
                          size="sm"
                          variant="outline"
                          isLoading={isMarking}
                          onClick={() => handleMarkAsRead(notif.id)}
                        >
                          Mark as Read
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ── Pagination ── */}
      {!loading && totalElements > 0 && (
        <div className="flex items-center justify-between mt-4 px-1">
          <p className="text-xs text-gray-500">
            Showing{' '}
            <span className="font-medium text-gray-700">{startRecord}–{endRecord}</span>
            {' '}of{' '}
            <span className="font-medium text-gray-700">{totalElements}</span>
            {' '}notifications
          </p>
          <div className="flex items-center gap-2">
            <Button
              size="sm"
              variant="secondary"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              ← Previous
            </Button>
            <span className="text-xs text-gray-600 px-1">
              Page {page + 1} of {totalPages}
            </span>
            <Button
              size="sm"
              variant="secondary"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Next →
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};

export default CitizenNotificationsPage;
