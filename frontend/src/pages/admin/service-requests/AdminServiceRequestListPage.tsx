import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { serviceRequestApi } from '../../../api/serviceRequestApi';
import type {
  ServiceRequestSummaryResponse,
  ServiceRequestStatus,
} from '../../../types/serviceRequest';
import { getErrorMessage } from '../../../utils/errorUtils';
import { formatDateTime } from '../../../utils/dateUtils';
import PageHeader from '../../../components/layout/PageHeader';
import Button from '../../../components/ui/Button';
import Input from '../../../components/ui/Input';
import Select from '../../../components/ui/Select';
import Badge from '../../../components/ui/Badge';
import Alert from '../../../components/ui/Alert';
import Card from '../../../components/ui/Card';
import LoadingSpinner from '../../../components/ui/LoadingSpinner';
import ConfirmDialog from '../../../components/ui/ConfirmDialog';

const PAGE_SIZE = 10;

const STATUS_OPTIONS = [
  { value: '',          label: 'All Statuses' },
  { value: 'SUBMITTED', label: 'Submitted' },
  { value: 'IN_REVIEW', label: 'In Review' },
  { value: 'APPROVED',  label: 'Approved' },
  { value: 'REJECTED',  label: 'Rejected' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

const AdminServiceRequestListPage: React.FC = () => {
  const navigate = useNavigate();

  const [requests, setRequests]           = useState<ServiceRequestSummaryResponse[]>([]);
  const [loading, setLoading]             = useState(false);
  const [error, setError]                 = useState('');
  const [success, setSuccess]             = useState('');
  const [page, setPage]                   = useState(0);
  const [totalPages, setTotalPages]       = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filter state (committed on Search click)
  const [citizenRefInput, setCitizenRefInput]   = useState('');
  const [serviceTypeInput, setServiceTypeInput] = useState('');
  const [statusFilter, setStatusFilter]         = useState('');
  const [committedFilters, setCommittedFilters] = useState({
    citizenReference: '',
    serviceType: '',
    status: '',
  });

  // Cancel state
  const [cancelTarget, setCancelTarget]   = useState<ServiceRequestSummaryResponse | null>(null);
  const [cancelling, setCancelling]       = useState(false);

  const fetchRequests = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await serviceRequestApi.searchServiceRequests({
        page,
        size: PAGE_SIZE,
        ...(committedFilters.citizenReference ? { citizenReference: committedFilters.citizenReference } : {}),
        ...(committedFilters.serviceType      ? { serviceType: committedFilters.serviceType }           : {}),
        ...(committedFilters.status           ? { status: committedFilters.status }                     : {}),
      });
      setRequests(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load service requests.'));
    } finally {
      setLoading(false);
    }
  }, [page, committedFilters]);

  useEffect(() => { fetchRequests(); }, [fetchRequests]);

  const handleSearch = () => {
    setPage(0);
    setCommittedFilters({
      citizenReference: citizenRefInput.trim(),
      serviceType:      serviceTypeInput.trim().replace(/\s+/g, '_'),
      status:           statusFilter,
    });
  };

  const handleClear = () => {
    setCitizenRefInput('');
    setServiceTypeInput('');
    setStatusFilter('');
    setPage(0);
    setCommittedFilters({ citizenReference: '', serviceType: '', status: '' });
  };

  const handleConfirmCancel = async () => {
    if (!cancelTarget) return;
    setCancelling(true);
    try {
      await serviceRequestApi.cancelServiceRequest(cancelTarget.requestReference);
      setSuccess(`Service request ${cancelTarget.requestReference} cancelled successfully.`);
      setCancelTarget(null);
      fetchRequests();
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to cancel service request.'));
      setCancelTarget(null);
    } finally {
      setCancelling(false);
    }
  };

  const startRecord = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const endRecord   = Math.min((page + 1) * PAGE_SIZE, totalElements);

  return (
    <div>
      <PageHeader
        title="Service Requests"
        subtitle="Search, review, and manage all citizen service requests."
      />

      {/* ── Filters ── */}
      <Card className="mb-4">
        <div className="flex flex-wrap gap-3 items-end">
          <div className="w-48">
            <Input
              label="Citizen Reference"
              placeholder="e.g. CIT-8F3A91B2"
              value={citizenRefInput}
              onChange={(e) => setCitizenRefInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            />
          </div>
          <div className="flex-1 min-w-40">
            <Input
              label="Service Type"
              placeholder="e.g. PASSPORT"
              value={serviceTypeInput}
              onChange={(e) => setServiceTypeInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            />
          </div>
          <div className="w-44">
            <Select
              label="Status"
              options={STATUS_OPTIONS}
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            />
          </div>
          <Button variant="primary" onClick={handleSearch}>Search</Button>
          <Button variant="secondary" onClick={handleClear}>Clear</Button>
        </div>
      </Card>

      {/* ── Alerts ── */}
      {success && (
        <Alert variant="success" message={success} onDismiss={() => setSuccess('')} className="mb-4" />
      )}
      {error && (
        <Alert variant="error" message={error} onDismiss={() => setError('')} className="mb-4" />
      )}

      {/* ── Table ── */}
      <Card padding="none">
        {loading ? (
          <div className="flex justify-center py-16">
            <LoadingSpinner size="lg" label="Loading service requests…" />
          </div>
        ) : requests.length === 0 ? (
          <div className="text-center py-16">
            <svg className="mx-auto h-12 w-12 text-gray-300 mb-3" fill="none"
              viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25zM6.75 12h.008v.008H6.75V12zm0 3h.008v.008H6.75V15zm0 3h.008v.008H6.75V18z" />
            </svg>
            <p className="text-sm font-medium text-gray-600">No service requests found</p>
            <p className="text-xs text-gray-400 mt-1">
              {committedFilters.citizenReference || committedFilters.serviceType || committedFilters.status
                ? 'Try adjusting your filters.'
                : 'No service requests have been submitted yet.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  {['Reference', 'Citizen Reference', 'Service Type', 'Status', 'Submitted', 'Actions'].map((h) => (
                    <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap">
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
                    <td className="px-4 py-3 font-mono text-xs text-gray-600 whitespace-nowrap">
                      {req.citizenReference}
                    </td>
                    <td className="px-4 py-3 text-gray-800 whitespace-nowrap">
                      {req.serviceType.replace(/_/g, ' ')}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <Badge label={req.status} variant={req.status as ServiceRequestStatus} />
                    </td>
                    <td className="px-4 py-3 text-gray-500 text-xs whitespace-nowrap">
                      {formatDateTime(req.createdAt)}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <div className="flex items-center gap-1.5">
                        <Button size="sm" variant="outline"
                          onClick={() => navigate(`/admin/service-requests/${req.requestReference}`)}>
                          View
                        </Button>
                        <Button size="sm" variant="secondary"
                          onClick={() => navigate(`/admin/service-requests/${req.requestReference}/documents`)}>
                          Docs
                        </Button>
                        <Button size="sm" variant="secondary"
                          onClick={() => navigate(`/admin/service-requests/${req.requestReference}/status-history`)}>
                          History
                        </Button>
                        {req.status !== 'CANCELLED' && (
                          <Button size="sm" variant="danger" onClick={() => setCancelTarget(req)}>
                            Cancel
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* ── Pagination ── */}
        {!loading && totalElements > 0 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-gray-50">
            <p className="text-xs text-gray-500">
              Showing <span className="font-medium text-gray-700">{startRecord}–{endRecord}</span>
              {' '}of <span className="font-medium text-gray-700">{totalElements}</span> requests
            </p>
            <div className="flex items-center gap-2">
              <Button size="sm" variant="secondary" disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}>
                ← Previous
              </Button>
              <span className="text-xs text-gray-600 px-1">Page {page + 1} of {totalPages}</span>
              <Button size="sm" variant="secondary" disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}>
                Next →
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* ── Cancel confirm ── */}
      <ConfirmDialog
        isOpen={!!cancelTarget}
        title="Cancel Service Request"
        message={
          cancelTarget
            ? `Are you sure you want to cancel request "${cancelTarget.requestReference}"? The request will be set to CANCELLED and the citizen will be notified.`
            : ''
        }
        confirmLabel="Cancel Request"
        variant="danger"
        isLoading={cancelling}
        onConfirm={handleConfirmCancel}
        onCancel={() => setCancelTarget(null)}
      />
    </div>
  );
};

export default AdminServiceRequestListPage;
