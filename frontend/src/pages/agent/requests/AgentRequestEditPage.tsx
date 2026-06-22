import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { serviceRequestApi } from '../../../api/serviceRequestApi';
import type { ServiceRequestResponse } from '../../../types/serviceRequest';
import { getErrorMessage, getFieldErrors } from '../../../utils/errorUtils';
import PageHeader from '../../../components/layout/PageHeader';
import Button from '../../../components/ui/Button';
import Select from '../../../components/ui/Select';
import Alert from '../../../components/ui/Alert';
import Card from '../../../components/ui/Card';
import LoadingSpinner from '../../../components/ui/LoadingSpinner';

const toLabel = (value: string) =>
  value.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());

type FormData = {
  serviceType: string;
  description: string;
};

const validate = (form: FormData): Record<string, string> => {
  const errs: Record<string, string> = {};
  if (!form.serviceType.trim()) errs.serviceType = 'Service type is required.';
  if (!form.description.trim()) errs.description = 'Description is required.';
  return errs;
};

const AgentRequestEditPage: React.FC = () => {
  const navigate = useNavigate();
  const { requestReference } = useParams<{ requestReference: string }>();

  const [request, setRequest]   = useState<ServiceRequestResponse | null>(null);
  const [loading, setLoading]   = useState(true);
  const [fetchError, setFetchError] = useState('');

  const [serviceTypeOptions, setServiceTypeOptions] = useState<{ value: string; label: string }[]>([]);
  const [typesLoading, setTypesLoading] = useState(true);

  const [form, setForm]         = useState<FormData>({ serviceType: '', description: '' });
  const [errors, setErrors]     = useState<Record<string, string>>({});
  const [globalError, setGlobalError] = useState('');
  const [saving, setSaving]     = useState(false);

  const fetchRequest = useCallback(async () => {
    if (!requestReference) return;
    setLoading(true);
    try {
      const data = await serviceRequestApi.getServiceRequestByReference(requestReference);
      setRequest(data);
      setForm({ serviceType: data.serviceType, description: data.description });
    } catch (err) {
      setFetchError(getErrorMessage(err, 'Failed to load service request.'));
    } finally {
      setLoading(false);
    }
  }, [requestReference]);

  useEffect(() => { fetchRequest(); }, [fetchRequest]);

  useEffect(() => {
    serviceRequestApi.getServiceTypes()
      .then((types) =>
        setServiceTypeOptions(types.map((t) => ({ value: t, label: toLabel(t) })))
      )
      .finally(() => setTypesLoading(false));
  }, []);

  const setField = (field: keyof FormData) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
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

    if (!requestReference) return;
    setSaving(true);
    try {
      await serviceRequestApi.updateServiceRequest(requestReference, {
        serviceType: form.serviceType.trim(),
        description: form.description.trim(),
      });
      navigate(`/agent/requests/${requestReference}`, { replace: true });
    } catch (err) {
      const fieldErrors = getFieldErrors(err);
      if (Object.keys(fieldErrors).length > 0) {
        setErrors(fieldErrors);
      } else {
        setGlobalError(getErrorMessage(err, 'Failed to update service request.'));
      }
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-24">
        <LoadingSpinner size="lg" label="Loading…" />
      </div>
    );
  }

  if (fetchError || !request) {
    return (
      <div>
        <PageHeader title="Edit Request" />
        <Alert variant="error" message={fetchError || 'Request not found.'} className="mb-4" />
        <Button variant="secondary" onClick={() => navigate('/agent/requests')}>
          ← Back to Requests
        </Button>
      </div>
    );
  }

  if (request.status !== 'SUBMITTED') {
    return (
      <div>
        <PageHeader
          title="Edit Request"
          actions={
            <Button variant="ghost" onClick={() => navigate(`/agent/requests/${requestReference}`)}>
              ← Back to Details
            </Button>
          }
        />
        <Alert
          variant="error"
          title="Cannot edit this request"
          message={`This request is currently "${request.status.replace(/_/g, ' ')}". Editing is only allowed while the status is SUBMITTED.`}
          className="mb-4"
        />
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Edit Request"
        subtitle={`Reference: ${request.requestReference}`}
        actions={
          <Button variant="ghost" onClick={() => navigate(`/agent/requests/${requestReference}`)}>
            ← Back to Details
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
          <Select
            label="Service Type"
            options={serviceTypeOptions}
            value={form.serviceType}
            onChange={setField('serviceType')}
            error={errors.serviceType}
            disabled={saving || typesLoading}
          />

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Description <span className="text-red-500">*</span>
            </label>
            <textarea
              className={[
                'block w-full rounded-md border px-3 py-2 text-sm shadow-sm resize-none',
                'focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500',
                'placeholder-gray-400',
                errors.description
                  ? 'border-red-400 focus:ring-red-400 focus:border-red-400'
                  : 'border-gray-300',
              ].join(' ')}
              rows={5}
              placeholder="Describe the service request…"
              value={form.description}
              onChange={setField('description')}
              disabled={saving}
            />
            {errors.description && (
              <p className="mt-1 text-xs text-red-600">{errors.description}</p>
            )}
          </div>

          <div className="flex gap-3 pt-2 border-t border-gray-100">
            <Button type="submit" variant="primary" isLoading={saving}>
              Save Changes
            </Button>
            <Button
              type="button"
              variant="secondary"
              disabled={saving}
              onClick={() => navigate(`/agent/requests/${requestReference}`)}
            >
              Cancel
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
};

export default AgentRequestEditPage;
