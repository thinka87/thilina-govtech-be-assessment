import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { citizenApi } from '../../../api/citizenApi';
import type { Citizen, CitizenStatus, UpdateCitizenRequest } from '../../../types/citizen';
import { getErrorMessage, getFieldErrors } from '../../../utils/errorUtils';
import PageHeader from '../../../components/layout/PageHeader';
import Button from '../../../components/ui/Button';
import Input from '../../../components/ui/Input';
import Select from '../../../components/ui/Select';
import Card from '../../../components/ui/Card';
import Alert from '../../../components/ui/Alert';
import LoadingSpinner from '../../../components/ui/LoadingSpinner';

// ── Form state type ───────────────────────────────────────────────────────────

type FormData = {
  name: string;
  email: string;
  mobileNumber: string;
  address: string;
  status: CitizenStatus | '';
};

const STATUS_OPTIONS = [
  { value: 'ACTIVE',   label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
];

// ── Validation ────────────────────────────────────────────────────────────────

const MOBILE_RE = /^\d{10}$/;

const validate = (form: FormData): Record<string, string> => {
  const errs: Record<string, string> = {};
  if (!form.name.trim())  errs.name  = 'Full name is required.';
  if (!form.email.trim()) errs.email = 'Email address is required.';
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))
                          errs.email = 'Enter a valid email address.';
  if (!form.mobileNumber.trim())  errs.mobileNumber = 'Mobile number is required.';
  else if (!MOBILE_RE.test(form.mobileNumber.trim()))
                                  errs.mobileNumber = 'Mobile number must be exactly 10 digits (e.g. 0771234567).';
  if (!form.address.trim()) errs.address = 'Address is required.';
  return errs;
};

// ── Page component ────────────────────────────────────────────────────────────

const AdminCitizenEditPage: React.FC = () => {
  const { citizenReference } = useParams<{ citizenReference: string }>();
  const navigate = useNavigate();

  const [citizen, setCitizen]   = useState<Citizen | null>(null);
  const [form, setForm]         = useState<FormData>({
    name: '', email: '', mobileNumber: '', address: '', status: '',
  });
  const [errors, setErrors]     = useState<Record<string, string>>({});
  const [globalError, setGlobalError] = useState('');
  const [successMsg, setSuccessMsg]   = useState('');
  const [loading, setLoading]   = useState(true);
  const [saving, setSaving]     = useState(false);

  // ── Fetch citizen on mount ────────────────────────────────────────────────────
  useEffect(() => {
    if (!citizenReference) return;
    setLoading(true);
    citizenApi
      .getCitizenByReference(citizenReference)
      .then((data) => {
        setCitizen(data);
        setForm({
          name:         data.name,
          email:        data.email,
          mobileNumber: data.mobileNumber,
          address:      data.address,
          status:       data.status,
        });
      })
      .catch((err) =>
        setGlobalError(getErrorMessage(err, 'Failed to load citizen data.')),
      )
      .finally(() => setLoading(false));
  }, [citizenReference]);

  // ── Field change helper ───────────────────────────────────────────────────────
  const setField =
    (field: keyof FormData) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      setForm((prev) => ({ ...prev, [field]: e.target.value }));
      setErrors((prev) => ({ ...prev, [field]: '' }));
    };

  // ── Submit ────────────────────────────────────────────────────────────────────
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setGlobalError('');
    setSuccessMsg('');

    const validationErrors = validate(form);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setSaving(true);
    try {
      const payload: UpdateCitizenRequest = {
        name:         form.name.trim(),
        email:        form.email.trim(),
        mobileNumber: form.mobileNumber.trim(),
        address:      form.address.trim(),
        ...(form.status ? { status: form.status as CitizenStatus } : {}),
      };
      await citizenApi.updateCitizen(citizenReference!, payload);
      setSuccessMsg('Citizen profile updated successfully.');

      // Navigate back to detail page after a brief delay
      setTimeout(() => {
        navigate(`/admin/citizens/${citizenReference}`);
      }, 1200);
    } catch (err) {
      const fieldErrors = getFieldErrors(err);
      if (Object.keys(fieldErrors).length > 0) {
        setErrors(fieldErrors);
      } else {
        setGlobalError(getErrorMessage(err, 'Failed to update citizen. Please try again.'));
      }
    } finally {
      setSaving(false);
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

  if (globalError && !citizen) {
    return (
      <div>
        <PageHeader title="Edit Citizen" />
        <Alert variant="error" title="Failed to load citizen" message={globalError} className="mb-4" />
        <Button variant="secondary" onClick={() => navigate('/admin/citizens')}>
          ← Back to Citizens
        </Button>
      </div>
    );
  }

  // ── Main render ───────────────────────────────────────────────────────────────
  return (
    <div>
      <PageHeader
        title="Edit Citizen"
        subtitle={citizen?.citizenReference}
        actions={
          <Button
            variant="ghost"
            onClick={() => navigate(`/admin/citizens/${citizenReference}`)}
          >
            ← Back to Details
          </Button>
        }
      />

      {/* ── Alerts ── */}
      {successMsg && (
        <Alert variant="success" message={successMsg} className="mb-4" />
      )}
      {globalError && !successMsg && (
        <Alert
          variant="error"
          message={globalError}
          onDismiss={() => setGlobalError('')}
          className="mb-4"
        />
      )}

      <Card>
        {/* Read-only header fields */}
        <div className="mb-5 pb-5 border-b border-gray-100">
          <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-3">
            Read-only Information
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <p className="text-xs font-medium text-gray-500 mb-1">Citizen Reference</p>
              <p className="text-sm font-mono text-gray-700 bg-gray-100 px-3 py-2 rounded-md">
                {citizen?.citizenReference}
              </p>
            </div>
            <div>
              <p className="text-xs font-medium text-gray-500 mb-1">NIC</p>
              <p className="text-sm text-gray-700 bg-gray-100 px-3 py-2 rounded-md">
                {citizen?.nic}
              </p>
            </div>
          </div>
        </div>

        {/* Editable fields */}
        <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-4">
          Editable Information
        </p>
        <form onSubmit={handleSubmit} noValidate className="space-y-5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            <Input
              label="Full Name"
              value={form.name}
              onChange={setField('name')}
              error={errors.name}
              required
              disabled={saving}
            />
            <Input
              label="Email Address"
              type="email"
              value={form.email}
              onChange={setField('email')}
              error={errors.email}
              required
              disabled={saving}
            />
            <Input
              label="Mobile Number"
              placeholder="e.g. 0771234567"
              value={form.mobileNumber}
              onChange={setField('mobileNumber')}
              error={errors.mobileNumber}
              hint="Exactly 10 digits, must be unique."
              required
              disabled={saving}
            />
            <Select
              label="Status"
              options={STATUS_OPTIONS}
              value={form.status}
              onChange={setField('status')}
              error={errors.status}
              disabled={saving}
            />
          </div>
          <Input
            label="Address"
            value={form.address}
            onChange={setField('address')}
            error={errors.address}
            required
            disabled={saving}
          />

          <div className="flex gap-3 pt-2 border-t border-gray-100">
            <Button type="submit" variant="primary" isLoading={saving}>
              Save Changes
            </Button>
            <Button
              type="button"
              variant="secondary"
              disabled={saving}
              onClick={() => navigate(`/admin/citizens/${citizenReference}`)}
            >
              Cancel
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
};

export default AdminCitizenEditPage;
