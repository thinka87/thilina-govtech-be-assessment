import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { citizenApi } from '../../../api/citizenApi';
import type { Citizen } from '../../../types/citizen';
import { getErrorMessage } from '../../../utils/errorUtils';
import { formatDateTime } from '../../../utils/dateUtils';
import PageHeader from '../../../components/layout/PageHeader';
import Button from '../../../components/ui/Button';
import Card from '../../../components/ui/Card';
import Badge from '../../../components/ui/Badge';
import Alert from '../../../components/ui/Alert';
import LoadingSpinner from '../../../components/ui/LoadingSpinner';
import ConfirmDialog from '../../../components/ui/ConfirmDialog';

// ── Detail row helper ─────────────────────────────────────────────────────────

const DetailRow: React.FC<{ label: string; value: React.ReactNode }> = ({
  label,
  value,
}) => (
  <div className="py-3 flex items-start gap-4 border-b border-gray-100 last:border-0">
    <dt className="w-44 shrink-0 text-sm font-medium text-gray-500">{label}</dt>
    <dd className="text-sm text-gray-900 break-all">{value ?? '—'}</dd>
  </div>
);

// ── Page component ────────────────────────────────────────────────────────────

const AdminCitizenDetailPage: React.FC = () => {
  const { citizenReference } = useParams<{ citizenReference: string }>();
  const navigate = useNavigate();

  const [citizen, setCitizen]         = useState<Citizen | null>(null);
  const [loading, setLoading]         = useState(true);
  const [error, setError]             = useState('');
  const [success, setSuccess]         = useState('');
  const [showConfirm, setShowConfirm] = useState(false);
  const [deactivating, setDeactivating] = useState(false);

  // ── Fetch citizen ─────────────────────────────────────────────────────────────
  useEffect(() => {
    if (!citizenReference) return;
    setLoading(true);
    setError('');
    citizenApi
      .getCitizenByReference(citizenReference)
      .then((data) => setCitizen(data))
      .catch((err) =>
        setError(getErrorMessage(err, 'Failed to load citizen details.')),
      )
      .finally(() => setLoading(false));
  }, [citizenReference]);

  // ── Deactivate handler ────────────────────────────────────────────────────────
  const handleConfirmDeactivate = async () => {
    if (!citizen) return;
    setDeactivating(true);
    try {
      await citizenApi.deactivateCitizen(citizen.citizenReference);
      setSuccess('Citizen has been deactivated successfully.');
      setShowConfirm(false);
      // Refresh the record to show updated status
      const updated = await citizenApi.getCitizenByReference(citizen.citizenReference);
      setCitizen(updated);
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to deactivate citizen.'));
      setShowConfirm(false);
    } finally {
      setDeactivating(false);
    }
  };

  // ── Loading / error states ────────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="flex justify-center py-24">
        <LoadingSpinner size="lg" label="Loading citizen…" />
      </div>
    );
  }

  if (error && !citizen) {
    return (
      <div>
        <PageHeader title="Citizen Details" />
        <Alert
          variant="error"
          title="Failed to load citizen"
          message={error}
          className="mb-4"
        />
        <Button variant="secondary" onClick={() => navigate('/admin/citizens')}>
          ← Back to Citizens
        </Button>
      </div>
    );
  }

  if (!citizen) return null;

  // ── Main render ───────────────────────────────────────────────────────────────
  return (
    <div>
      <PageHeader
        title="Citizen Details"
        subtitle={citizen.citizenReference}
        actions={
          <div className="flex gap-2">
            <Button variant="ghost" onClick={() => navigate('/admin/citizens')}>
              ← Back
            </Button>
            <Button
              variant="secondary"
              onClick={() =>
                navigate(`/admin/citizens/${citizen.citizenReference}/edit`)
              }
            >
              Edit
            </Button>
            {citizen.status === 'ACTIVE' && (
              <Button variant="danger" onClick={() => setShowConfirm(true)}>
                Deactivate
              </Button>
            )}
          </div>
        }
      />

      {/* ── Alerts ── */}
      {success && (
        <Alert
          variant="success"
          message={success}
          onDismiss={() => setSuccess('')}
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

      {/* ── Details card ── */}
      <Card>
        <h2 className="text-base font-semibold text-gray-800 mb-2">
          Profile Information
        </h2>
        <dl>
          <DetailRow label="Citizen Reference" value={
            <span className="font-mono text-xs bg-gray-100 px-2 py-0.5 rounded">
              {citizen.citizenReference}
            </span>
          } />
          <DetailRow label="Full Name"     value={citizen.name} />
          <DetailRow label="NIC"           value={citizen.nic ?? '—'} />
          <DetailRow label="Email"         value={citizen.email} />
          <DetailRow label="Mobile Number" value={citizen.mobileNumber} />
          <DetailRow label="Address"       value={citizen.address} />
          <DetailRow
            label="Status"
            value={<Badge label={citizen.status} variant={citizen.status} />}
          />
          <DetailRow
            label="Username"
            value={citizen.username ?? citizen.email}
          />
          <DetailRow
            label="Must Change Password"
            value={citizen.mustChangePassword ? 'Yes' : 'No'}
          />
          <DetailRow label="Created"  value={formatDateTime(citizen.createdAt)} />
          <DetailRow label="Updated"  value={formatDateTime(citizen.updatedAt)} />
        </dl>
      </Card>

      {/* ── Deactivate confirm ── */}
      <ConfirmDialog
        isOpen={showConfirm}
        title="Deactivate Citizen"
        message={`Are you sure you want to deactivate "${citizen.name}"? Their account will be disabled immediately.`}
        confirmLabel="Deactivate"
        variant="danger"
        isLoading={deactivating}
        onConfirm={handleConfirmDeactivate}
        onCancel={() => setShowConfirm(false)}
      />
    </div>
  );
};

export default AdminCitizenDetailPage;
