import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { serviceRequestApi } from '../../../api/serviceRequestApi';
import { citizenApi } from '../../../api/citizenApi';
import type { ServiceRequestResponse, ServiceRequestStatus } from '../../../types/serviceRequest';
import type { Citizen } from '../../../types/citizen';
import { getErrorMessage } from '../../../utils/errorUtils';
import { formatDateTime } from '../../../utils/dateUtils';
import PageHeader from '../../../components/layout/PageHeader';
import Button from '../../../components/ui/Button';
import Badge from '../../../components/ui/Badge';
import Alert from '../../../components/ui/Alert';
import Card from '../../../components/ui/Card';
import LoadingSpinner from '../../../components/ui/LoadingSpinner';
import ConfirmDialog from '../../../components/ui/ConfirmDialog';

const DetailRow: React.FC<{ label: string; value: React.ReactNode }> = ({ label, value }) => (
  <div className="py-3 flex gap-4 border-b border-gray-100 last:border-0">
    <dt className="w-40 shrink-0 text-sm font-medium text-gray-500">{label}</dt>
    <dd className="text-sm text-gray-900 flex-1 break-words">{value ?? '—'}</dd>
  </div>
);

const AdminServiceRequestDetailPage: React.FC = () => {
  const navigate = useNavigate();
  const { requestReference } = useParams<{ requestReference: string }>();

  const [request, setRequest]   = useState<ServiceRequestResponse | null>(null);
  const [citizen, setCitizen]   = useState<Citizen | null>(null);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState('');
  const [success, setSuccess]   = useState('');

  const [showCancel, setShowCancel]   = useState(false);
  const [cancelling, setCancelling]   = useState(false);

  const fetchAll = useCallback(async () => {
    if (!requestReference) return;
    setLoading(true);
    setError('');
    try {
      const req = await serviceRequestApi.getServiceRequestByReference(requestReference);
      setRequest(req);
      // Load citizen profile in parallel
      const cit = await citizenApi.getCitizenByReference(req.citizenReference);
      setCitizen(cit);
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load service request.'));
    } finally {
      setLoading(false);
    }
  }, [requestReference]);

  useEffect(() => { fetchAll(); }, [fetchAll]);

  const handleConfirmCancel = async () => {
    if (!requestReference) return;
    setCancelling(true);
    try {
      await serviceRequestApi.cancelServiceRequest(requestReference);
      setSuccess('Service request cancelled successfully.');
      setShowCancel(false);
      await fetchAll();
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to cancel service request.'));
      setShowCancel(false);
    } finally {
      setCancelling(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-24">
        <LoadingSpinner size="lg" label="Loading…" />
      </div>
    );
  }

  if (error && !request) {
    return (
      <div>
        <PageHeader title="Request Details" />
        <Alert variant="error" message={error} className="mb-4" />
        <Button variant="secondary" onClick={() => navigate('/admin/service-requests')}>
          ← Back to Service Requests
        </Button>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Request Details"
        subtitle={`Reference: ${request?.requestReference}`}
        actions={
          <Button variant="ghost" onClick={() => navigate('/admin/service-requests')}>
            ← Back to Service Requests
          </Button>
        }
      />

      {success && (
        <Alert variant="success" message={success} onDismiss={() => setSuccess('')} className="mb-4" />
      )}
      {error && (
        <Alert variant="error" message={error} onDismiss={() => setError('')} className="mb-4" />
      )}

      {request && (
        <>
          {/* ── Service Request Details ── */}
          <Card className="mb-4">
            <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wide mb-2">
              Request Information
            </h2>
            <dl>
              <DetailRow label="Reference"    value={<span className="font-mono">{request.requestReference}</span>} />
              <DetailRow label="Status"       value={<Badge label={request.status} variant={request.status as ServiceRequestStatus} />} />
              <DetailRow label="Service Type" value={request.serviceType.replace(/_/g, ' ')} />
              <DetailRow label="Description"  value={
                <span className="block bg-gray-50 rounded p-2 leading-relaxed whitespace-pre-wrap">
                  {request.description}
                </span>
              } />
              <DetailRow label="Submitted"    value={formatDateTime(request.createdAt)} />
              <DetailRow label="Last Updated" value={formatDateTime(request.updatedAt)} />
            </dl>
          </Card>

          {/* ── Citizen Details ── */}
          <Card className="mb-4">
            <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wide mb-2">
              Citizen Information
            </h2>
            {citizen ? (
              <dl>
                <DetailRow label="Name"       value={citizen.name} />
                <DetailRow label="NIC"        value={citizen.nic ?? '—'} />
                <DetailRow label="Email"      value={citizen.email} />
                <DetailRow label="Mobile"     value={citizen.mobileNumber} />
                <DetailRow label="Address"    value={citizen.address} />
                <DetailRow label="Status"     value={
                  <Badge label={citizen.status} variant={citizen.status} />
                } />
                <DetailRow label="Reference"  value={
                  <button
                    className="font-mono text-xs text-primary-600 hover:underline text-left"
                    onClick={() => navigate(`/admin/citizens/${citizen.citizenReference}`)}
                  >
                    {citizen.citizenReference}
                  </button>
                } />
              </dl>
            ) : (
              <p className="text-sm text-gray-500">Citizen information could not be loaded.</p>
            )}
          </Card>

          {/* ── Actions ── */}
          <div className="flex flex-wrap gap-3">
            <Button variant="secondary"
              onClick={() => navigate(`/admin/service-requests/${requestReference}/documents`)}>
              View Documents
            </Button>
            <Button variant="secondary"
              onClick={() => navigate(`/admin/service-requests/${requestReference}/status-history`)}>
              Status History
            </Button>
            {request.status !== 'CANCELLED' && (
              <Button variant="danger" onClick={() => setShowCancel(true)}>
                Cancel Request
              </Button>
            )}
          </div>
        </>
      )}

      <ConfirmDialog
        isOpen={showCancel}
        title="Cancel Service Request"
        message={`Are you sure you want to cancel request "${requestReference}"? The status will be set to CANCELLED and the citizen will be notified.`}
        confirmLabel="Cancel Request"
        variant="danger"
        isLoading={cancelling}
        onConfirm={handleConfirmCancel}
        onCancel={() => setShowCancel(false)}
      />
    </div>
  );
};

export default AdminServiceRequestDetailPage;
