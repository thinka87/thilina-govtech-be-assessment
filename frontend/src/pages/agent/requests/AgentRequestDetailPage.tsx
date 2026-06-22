import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { serviceRequestApi } from '../../../api/serviceRequestApi';
import { citizenApi } from '../../../api/citizenApi';
import type {
  ServiceRequestResponse,
  ServiceRequestStatus,
  UpdateServiceRequestStatusRequest,
} from '../../../types/serviceRequest';
import type { Citizen, CitizenStatus } from '../../../types/citizen';
import { getErrorMessage } from '../../../utils/errorUtils';
import { formatDateTime } from '../../../utils/dateUtils';
import PageHeader from '../../../components/layout/PageHeader';
import Button from '../../../components/ui/Button';
import Badge from '../../../components/ui/Badge';
import Alert from '../../../components/ui/Alert';
import Card from '../../../components/ui/Card';
import Select from '../../../components/ui/Select';
import LoadingSpinner from '../../../components/ui/LoadingSpinner';

// Valid next statuses per current status
const NEXT_STATUSES: Partial<Record<ServiceRequestStatus, ServiceRequestStatus[]>> = {
  SUBMITTED: ['IN_REVIEW'],
  IN_REVIEW: ['APPROVED', 'REJECTED'],
};

const STATUS_LABELS: Record<ServiceRequestStatus, string> = {
  SUBMITTED: 'Submitted',
  IN_REVIEW: 'In Review',
  APPROVED:  'Approved',
  REJECTED:  'Rejected',
  CANCELLED: 'Cancelled',
};

const DetailRow: React.FC<{ label: string; value: React.ReactNode }> = ({ label, value }) => (
  <div className="py-3 flex gap-4 border-b border-gray-100 last:border-0">
    <dt className="w-40 shrink-0 text-sm font-medium text-gray-500">{label}</dt>
    <dd className="text-sm text-gray-900 flex-1">{value}</dd>
  </div>
);

const AgentRequestDetailPage: React.FC = () => {
  const navigate = useNavigate();
  const { requestReference } = useParams<{ requestReference: string }>();

  const [request, setRequest]     = useState<ServiceRequestResponse | null>(null);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState('');
  const [success, setSuccess]     = useState('');

  // Citizen info
  const [citizen, setCitizen]           = useState<Citizen | null>(null);
  const [citizenLoading, setCitizenLoading] = useState(false);
  const [citizenError, setCitizenError] = useState('');

  // Status update form
  const [newStatus, setNewStatus]     = useState('');
  const [remarks, setRemarks]         = useState('');
  const [updating, setUpdating]       = useState(false);
  const [updateError, setUpdateError] = useState('');

  const fetchRequest = useCallback(async () => {
    if (!requestReference) return;
    setLoading(true);
    setError('');
    try {
      const data = await serviceRequestApi.getServiceRequestByReference(requestReference);
      setRequest(data);
      setNewStatus('');
      setRemarks('');
      // Load citizen info in parallel
      if (data.citizenReference) {
        setCitizenLoading(true);
        setCitizenError('');
        citizenApi.getCitizenByReference(data.citizenReference)
          .then((c) => setCitizen(c))
          .catch(() => setCitizenError('Unable to load citizen information.'))
          .finally(() => setCitizenLoading(false));
      }
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load service request.'));
    } finally {
      setLoading(false);
    }
  }, [requestReference]);

  useEffect(() => { fetchRequest(); }, [fetchRequest]);

  const availableNextStatuses = request ? (NEXT_STATUSES[request.status] ?? []) : [];

  const handleStatusUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newStatus || !requestReference) return;
    setUpdating(true);
    setUpdateError('');
    try {
      const payload: UpdateServiceRequestStatusRequest = {
        status: newStatus as ServiceRequestStatus,
        ...(remarks.trim() ? { remarks: remarks.trim() } : {}),
      };
      await serviceRequestApi.updateServiceRequestStatus(requestReference, payload);
      setSuccess(`Status updated to ${STATUS_LABELS[newStatus as ServiceRequestStatus]} successfully.`);
      await fetchRequest();
    } catch (err) {
      setUpdateError(getErrorMessage(err, 'Failed to update status.'));
    } finally {
      setUpdating(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-24">
        <LoadingSpinner size="lg" label="Loading request details…" />
      </div>
    );
  }

  if (error || !request) {
    return (
      <div>
        <PageHeader title="Request Details" />
        <Alert variant="error" message={error || 'Service request not found.'} className="mb-4" />
        <Button variant="secondary" onClick={() => navigate('/agent/requests')}>
          ← Back to Requests
        </Button>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Request Details"
        subtitle={`Reference: ${request.requestReference}`}
        actions={
          <Button variant="ghost" onClick={() => navigate('/agent/requests')}>
            ← Back to Requests
          </Button>
        }
      />

      {success && (
        <Alert
          variant="success"
          message={success}
          onDismiss={() => setSuccess('')}
          className="mb-4"
        />
      )}

      {/* ── Details card ── */}
      <Card className="mb-4">
        <dl>
          <DetailRow label="Reference"       value={<span className="font-mono">{request.requestReference}</span>} />
          <DetailRow label="Status"          value={<Badge label={request.status} variant={request.status as ServiceRequestStatus} />} />
          <DetailRow label="Service Type"    value={request.serviceType.replace(/_/g, ' ')} />
          <DetailRow label="Citizen Name"    value={request.citizenName} />
          <DetailRow label="Citizen Reference" value={<span className="font-mono text-xs">{request.citizenReference}</span>} />
          <DetailRow label="Description"     value={
            <span className="block bg-gray-50 rounded p-2 leading-relaxed whitespace-pre-wrap">
              {request.description}
            </span>
          } />
          <DetailRow label="Submitted"       value={formatDateTime(request.createdAt)} />
          <DetailRow label="Last Updated"    value={formatDateTime(request.updatedAt)} />
        </dl>
      </Card>

      {/* ── Citizen Information card ── */}
      <Card className="mb-4">
        <h2 className="text-sm font-semibold text-gray-800 mb-3">Citizen Information</h2>
        {citizenLoading ? (
          <div className="flex items-center gap-2 py-4">
            <LoadingSpinner size="sm" />
            <span className="text-sm text-gray-500">Loading citizen details…</span>
          </div>
        ) : citizenError ? (
          <Alert variant="error" message={citizenError} className="mb-0" />
        ) : citizen ? (
          <dl>
            <DetailRow label="Citizen Reference" value={<span className="font-mono text-xs">{citizen.citizenReference}</span>} />
            <DetailRow label="Name"              value={citizen.name} />
            <DetailRow label="NIC"               value={citizen.nic ?? <span className="text-gray-400 italic">Not provided</span>} />
            <DetailRow label="Email"             value={citizen.email} />
            <DetailRow label="Mobile Number"     value={citizen.mobileNumber} />
            <DetailRow label="Address"           value={citizen.address} />
            <DetailRow label="Status"            value={<Badge label={citizen.status} variant={citizen.status as CitizenStatus} />} />
          </dl>
        ) : null}
      </Card>

      {/* ── Navigation actions ── */}
      <div className="flex flex-wrap gap-3 mb-6">
        {request.status === 'SUBMITTED' && (
          <Button
            variant="secondary"
            onClick={() => navigate(`/agent/requests/${request.requestReference}/edit`)}
          >
            Edit Request
          </Button>
        )}
        <Button
          variant="secondary"
          onClick={() => navigate(`/agent/requests/${request.requestReference}/documents`)}
        >
          Documents
        </Button>
        <Button
          variant="secondary"
          onClick={() => navigate(`/agent/requests/${request.requestReference}/status-history`)}
        >
          Status History
        </Button>
      </div>

      {/* ── Update Status section ── */}
      {availableNextStatuses.length > 0 ? (
        <Card>
          <h2 className="text-sm font-semibold text-gray-800 mb-4">Update Status</h2>
          {updateError && (
            <Alert
              variant="error"
              message={updateError}
              onDismiss={() => setUpdateError('')}
              className="mb-4"
            />
          )}
          <form onSubmit={handleStatusUpdate} className="space-y-4">
            <Select
              label="New Status"
              options={[
                { value: '', label: 'Select new status…' },
                ...availableNextStatuses.map((s) => ({ value: s, label: STATUS_LABELS[s] })),
              ]}
              value={newStatus}
              onChange={(e) => setNewStatus(e.target.value)}
              required
            />
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Remarks <span className="text-gray-400 font-normal">(optional)</span>
              </label>
              <textarea
                className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm
                           shadow-sm focus:outline-none focus:ring-2 focus:ring-primary-500
                           focus:border-primary-500 placeholder-gray-400 resize-none"
                rows={3}
                placeholder="Add remarks or notes about this status change…"
                value={remarks}
                onChange={(e) => setRemarks(e.target.value)}
                disabled={updating}
              />
            </div>
            <Button
              type="submit"
              variant="primary"
              isLoading={updating}
              disabled={!newStatus}
            >
              Update Status
            </Button>
          </form>
        </Card>
      ) : (
        <Card>
          <p className="text-sm text-gray-500">
            This request is in a terminal state (<strong>{STATUS_LABELS[request.status]}</strong>) and cannot be updated further.
          </p>
        </Card>
      )}
    </div>
  );
};

export default AgentRequestDetailPage;
