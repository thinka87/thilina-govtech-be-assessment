import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import { serviceRequestApi } from '../../../api/serviceRequestApi';
import type { ServiceRequestSummaryResponse, ServiceRequestStatus } from '../../../types/serviceRequest';
import { getErrorMessage } from '../../../utils/errorUtils';
import { formatDateTime } from '../../../utils/dateUtils';
import PageHeader from '../../../components/layout/PageHeader';
import Button from '../../../components/ui/Button';
import Badge from '../../../components/ui/Badge';
import Alert from '../../../components/ui/Alert';
import Card from '../../../components/ui/Card';
import LoadingSpinner from '../../../components/ui/LoadingSpinner';

const PAGE_SIZE = 10;

const CitizenRequestListPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const citizenReference = user?.citizenReference ?? '';

  const [requests, setRequests]           = useState<ServiceRequestSummaryResponse[]>([]);
  const [loading, setLoading]             = useState(false);
  const [error, setError]                 = useState('');
  const [page, setPage]                   = useState(0);
  const [totalPages, setTotalPages]       = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const fetchRequests = useCallback(async () => {
    if (!citizenReference) {
      setError('Unable to determine your citizen reference. Please log out and log in again.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const data = await serviceRequestApi.getCitizenServiceRequests(citizenReference, {
        page,
        size: PAGE_SIZE,
      });
      setRequests(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load your service requests.'));
    } finally {
      setLoading(false);
    }
  }, [citizenReference, page]);

  useEffect(() => { fetchRequests(); }, [fetchRequests]);

  const startRecord = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const endRecord   = Math.min((page + 1) * PAGE_SIZE, totalElements);

  return (
    <div>
      <PageHeader
        title="My Service Requests"
        subtitle="View and track all your submitted service requests."
        actions={
          <Button variant="primary" onClick={() => navigate('/citizen/requests/create')}>
            + New Request
          </Button>
        }
      />

      {error && (
        <Alert
          variant="error"
          message={error}
          onDismiss={() => setError('')}
          className="mb-4"
        />
      )}

      <Card padding="none">
        {loading ? (
          <div className="flex justify-center py-16">
            <LoadingSpinner size="lg" label="Loading requests…" />
          </div>
        ) : requests.length === 0 ? (
          <div className="text-center py-16">
            <svg className="mx-auto h-12 w-12 text-gray-300 mb-3" fill="none"
              viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25z" />
            </svg>
            <p className="text-sm font-medium text-gray-600">No service requests yet</p>
            <p className="text-xs text-gray-400 mt-1">
              You have not submitted any service requests yet.
            </p>
            <Button
              variant="primary"
              className="mt-4"
              onClick={() => navigate('/citizen/requests/create')}
            >
              Submit Your First Request
            </Button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  {['Request Reference', 'Service Type', 'Status', 'Submitted On', 'Actions'].map((h) => (
                    <th
                      key={h}
                      className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap"
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {requests.map((req) => (
                  <tr key={req.requestReference} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 font-mono text-xs text-gray-700 whitespace-nowrap">
                      {req.requestReference}
                    </td>
                    <td className="px-4 py-3 text-gray-800 whitespace-nowrap">
                      {req.serviceType.replace(/_/g, ' ')}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <Badge
                        label={req.status.replace(/_/g, ' ')}
                        variant={req.status as ServiceRequestStatus}
                      />
                    </td>
                    <td className="px-4 py-3 text-gray-500 whitespace-nowrap">
                      {formatDateTime(req.createdAt)}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <div className="flex items-center gap-1.5">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() =>
                            navigate(`/citizen/requests/${req.requestReference}`, {
                              state: { request: req },
                            })
                          }
                        >
                          View
                        </Button>
                        <Button
                          size="sm"
                          variant="secondary"
                          onClick={() =>
                            navigate(
                              `/citizen/requests/${req.requestReference}/documents/add`,
                              { state: { request: req } },
                            )
                          }
                        >
                          Add Doc
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {!loading && totalElements > 0 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-gray-50">
            <p className="text-xs text-gray-500">
              Showing{' '}
              <span className="font-medium text-gray-700">{startRecord}–{endRecord}</span>
              {' '}of{' '}
              <span className="font-medium text-gray-700">{totalElements}</span>
              {' '}requests
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
      </Card>
    </div>
  );
};

export default CitizenRequestListPage;
