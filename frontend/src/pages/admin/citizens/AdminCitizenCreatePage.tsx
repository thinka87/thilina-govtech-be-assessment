import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { citizenApi } from '../../../api/citizenApi';
import type { CitizenCreatedResponse, CreateCitizenRequest } from '../../../types/citizen';
import { getErrorMessage, getFieldErrors } from '../../../utils/errorUtils';
import PageHeader from '../../../components/layout/PageHeader';
import Button from '../../../components/ui/Button';
import Input from '../../../components/ui/Input';
import Card from '../../../components/ui/Card';
import Alert from '../../../components/ui/Alert';

// ── Form state type ───────────────────────────────────────────────────────────

type FormData = {
  name: string;
  nic: string;
  email: string;
  mobileNumber: string;
  address: string;
  temporaryPassword: string;
};

const EMPTY_FORM: FormData = {
  name:              '',
  nic:               '',
  email:             '',
  mobileNumber:      '',
  address:           '',
  temporaryPassword: '',
};

// ── Client-side validation ────────────────────────────────────────────────────

const SL_NIC_RE = /^\d{9}[VvXx]$|^\d{12}$/;
const MOBILE_RE = /^\d{10}$/;

const validate = (form: FormData): Record<string, string> => {
  const errs: Record<string, string> = {};
  if (!form.name.trim()) errs.name = 'Full name is required.';
  if (form.nic.trim() && !SL_NIC_RE.test(form.nic.trim()))
    errs.nic = 'Enter a valid Sri Lankan NIC — old format: 9 digits + V or X (e.g. 871840504V); new format: 12 digits only (e.g. 199012345678).';
  if (!form.email.trim()) errs.email = 'Email address is required.';
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))
    errs.email = 'Enter a valid email address.';
  if (!form.mobileNumber.trim()) errs.mobileNumber = 'Mobile number is required.';
  else if (!MOBILE_RE.test(form.mobileNumber.trim()))
    errs.mobileNumber = 'Mobile number must be exactly 10 digits (e.g. 0771234567).';
  if (!form.address.trim()) errs.address = 'Address is required.';
  if (!form.temporaryPassword) errs.temporaryPassword = 'Temporary password is required.';
  else if (form.temporaryPassword.length < 8)
    errs.temporaryPassword = 'Password must be at least 8 characters.';
  return errs;
};

// ── Component ─────────────────────────────────────────────────────────────────

const AdminCitizenCreatePage: React.FC = () => {
  const navigate = useNavigate();

  const [form, setForm]       = useState<FormData>(EMPTY_FORM);
  const [errors, setErrors]   = useState<Record<string, string>>({});
  const [globalError, setGlobalError] = useState('');
  const [loading, setLoading] = useState(false);
  const [created, setCreated] = useState<CitizenCreatedResponse | null>(null);

  const setField = (field: keyof FormData) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      setForm((prev) => ({ ...prev, [field]: e.target.value }));
      setErrors((prev) => ({ ...prev, [field]: '' }));
    };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setGlobalError('');

    const validationErrors = validate(form);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setLoading(true);
    try {
      const payload: CreateCitizenRequest = {
        name:              form.name.trim(),
        ...(form.nic.trim() ? { nic: form.nic.trim().toUpperCase() } : {}),
        email:             form.email.trim(),
        mobileNumber:      form.mobileNumber.trim(),
        address:           form.address.trim(),
        temporaryPassword: form.temporaryPassword,
      };
      const response = await citizenApi.createCitizen(payload);
      setCreated(response.data);
    } catch (err) {
      const fieldErrors = getFieldErrors(err);
      if (Object.keys(fieldErrors).length > 0) {
        setErrors(fieldErrors);
      } else {
        setGlobalError(getErrorMessage(err, 'Failed to create citizen. Please try again.'));
      }
    } finally {
      setLoading(false);
    }
  };

  // ── Success state ─────────────────────────────────────────────────────────────
  if (created) {
    return (
      <div>
        <PageHeader
          title="Create Citizen"
          actions={
            <Button variant="ghost" onClick={() => navigate('/admin/citizens')}>
              ← Back to Citizens
            </Button>
          }
        />

        <Alert
          variant="success"
          title="Citizen created successfully"
          message="The citizen account has been created. Share the temporary password securely."
          className="mb-4"
        />

        <Card>
          <h2 className="text-base font-semibold text-gray-800 mb-4">
            Account Details
          </h2>
          <dl className="divide-y divide-gray-100">
            {([
              ['Citizen Reference', created.citizenReference],
              ['Full Name',         created.name],
              ['NIC',               created.nic ?? '—'],
              ['Email / Username',  created.email],
              ['Mobile',            created.mobileNumber],
              ['Address',           created.address],
            ] as [string, string][]).map(([label, value]) => (
              <div key={label} className="py-3 flex gap-4">
                <dt className="w-44 shrink-0 text-sm font-medium text-gray-500">{label}</dt>
                <dd className="text-sm text-gray-900 break-all">{value}</dd>
              </div>
            ))}
          </dl>

          {/* Temporary password note */}
          <div className="mt-4 rounded-md bg-yellow-50 border border-yellow-200 px-4 py-3">
            <p className="text-sm font-medium text-yellow-800 mb-1">
              Temporary Password Note
            </p>
            <p className="text-sm text-yellow-700">{created.temporaryPasswordNote}</p>
          </div>

          <div className="mt-6 flex gap-3">
            <Button
              variant="primary"
              onClick={() => navigate(`/admin/citizens/${created.citizenReference}`)}
            >
              View Citizen Profile
            </Button>
            <Button
              variant="secondary"
              onClick={() => {
                setCreated(null);
                setForm(EMPTY_FORM);
                setErrors({});
              }}
            >
              Create Another Citizen
            </Button>
            <Button variant="ghost" onClick={() => navigate('/admin/citizens')}>
              Back to List
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  // ── Form state ────────────────────────────────────────────────────────────────
  return (
    <div>
      <PageHeader
        title="Create Citizen"
        subtitle="Register a new citizen account. A temporary password will be issued."
        actions={
          <Button variant="ghost" onClick={() => navigate('/admin/citizens')}>
            ← Back to Citizens
          </Button>
        }
      />

      {globalError && (
        <Alert
          variant="error"
          message={globalError}
          onDismiss={() => setGlobalError('')}
          className="mb-4"
        />
      )}

      <Card>
        <form onSubmit={handleSubmit} noValidate className="space-y-5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            <Input
              label="Full Name"
              placeholder="e.g. Kamal Perera"
              value={form.name}
              onChange={setField('name')}
              error={errors.name}
              required
              disabled={loading}
            />
            <Input
              label="NIC (optional)"
              placeholder="e.g. 990123456V or 199012345678"
              value={form.nic}
              onChange={setField('nic')}
              error={errors.nic}
              hint="Old format: 9 digits + V or X, 10 chars total (e.g. 871840504V). New format: 12 digits only (e.g. 199012345678)."
              disabled={loading}
            />
            <Input
              label="Email Address"
              type="email"
              placeholder="e.g. kamal@example.com"
              value={form.email}
              onChange={setField('email')}
              error={errors.email}
              required
              disabled={loading}
            />
            <Input
              label="Mobile Number"
              placeholder="e.g. 0771234567"
              value={form.mobileNumber}
              onChange={setField('mobileNumber')}
              error={errors.mobileNumber}
              hint="Exactly 10 digits, must be unique."
              required
              disabled={loading}
            />
          </div>

          <Input
            label="Address"
            placeholder="e.g. 123 Main Street, Colombo 03"
            value={form.address}
            onChange={setField('address')}
            error={errors.address}
            required
            disabled={loading}
          />

          <Input
            label="Temporary Password"
            type="password"
            placeholder="Minimum 8 characters"
            value={form.temporaryPassword}
            onChange={setField('temporaryPassword')}
            error={errors.temporaryPassword}
            hint="The citizen must change this password on first login."
            required
            disabled={loading}
          />

          <div className="flex gap-3 pt-2 border-t border-gray-100">
            <Button type="submit" variant="primary" isLoading={loading}>
              Create Citizen
            </Button>
            <Button
              type="button"
              variant="secondary"
              disabled={loading}
              onClick={() => navigate('/admin/citizens')}
            >
              Cancel
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
};

export default AdminCitizenCreatePage;
