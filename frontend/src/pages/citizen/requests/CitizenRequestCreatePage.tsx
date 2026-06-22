import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import { serviceRequestApi } from '../../../api/serviceRequestApi';
import type { ServiceRequestResponse } from '../../../types/serviceRequest';
import { getErrorMessage } from '../../../utils/errorUtils';
import { formatDateTime } from '../../../utils/dateUtils';
import PageHeader from '../../../components/layout/PageHeader';
import Button from '../../../components/ui/Button';
import Select from '../../../components/ui/Select';
import Alert from '../../../components/ui/Alert';
import Card from '../../../components/ui/Card';
import Badge from '../../../components/ui/Badge';

interface FormErrors {
  serviceType?: string;
  description?: string;
}

const toLabel = (value: string) =>
  value.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());

const CitizenRequestCreatePage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [serviceTypeOptions, setServiceTypeOptions] = useState<{ value: string; label: string }[]>(
    [{ value: '', label: 'Select a service type…' }],
  );
  const [typesLoading, setTypesLoading] = useState(true);

  const [serviceType, setServiceType] = useState('');
  const [description, setDescription] = useState('');
  const [errors, setErrors]           = useState<FormErrors>({});
  const [submitting, setSubmitting]   = useState(false);
  const [apiError, setApiError]       = useState('');
  const [created, setCreated]         = useState<ServiceRequestResponse | null>(null);

  useEffect(() => {
    serviceRequestApi.getServiceTypes()
      .then((types) => {
        setServiceTypeOptions([
          { value: '', label: 'Select a service type…' },
          ...types.map((t) => ({ value: t, label: toLabel(t) })),
        ]);
      })
      .catch(() => {
        // fall back to empty placeholder — user sees only the prompt option
      })
      .finally(() => setTypesLoading(false));
  }, []);

  const validate = (): boolean => {
    const next: FormErrors = {};
    if (!serviceType) next.serviceType = 'Please select a service type.';
    if (!description.trim()) {
      next.description = 'Description is required.';
    } else if (description.trim().length < 10) {
      next.description = 'Description must be at least 10 characters.';
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setSubmitting(true);
    setApiError('');
    try {
      const response = await serviceRequestApi.createServiceRequest({
        serviceType,
        description: description.trim(),
      });
      setCreated(response.data);
    } catch (err) {
      setApiError(getErrorMessage(err, 'Failed to submit service request. Please try again.'));
    } finally {
      setSubmitting(false);
    }
  };

  if (created) {
    return (
      <div>
        <PageHeader
          title="Request Submitted"
          subtitle="Your service request has been submitted successfully."
        />
        <Card className="max-w-lg">
          <Alert
            variant="success"
            title="Service Request Created"
            message="Your request has been submitted and is now under review."
            className="mb-4"
          />
          <dl className="space-y-3 text-sm">
            <div className="flex justify-between">
              <dt className="text-gray-500 font-medium">Reference</dt>
              <dd className="font-mono text-gray-900">{created.requestReference}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-gray-500 font-medium">Service Type</dt>
              <dd className="text-gray-900">{created.serviceType.replace(/_/g, ' ')}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-gray-500 font-medium">Status</dt>
              <dd>
                <Badge label="Submitted" variant="SUBMITTED" />
              </dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-gray-500 font-medium">Submitted</dt>
              <dd className="text-gray-900">{formatDateTime(created.createdAt)}</dd>
            </div>
            <div>
              <dt className="text-gray-500 font-medium mb-1">Description</dt>
              <dd className="text-gray-700 bg-gray-50 rounded p-2">{created.description}</dd>
            </div>
          </dl>
          <div className="flex gap-3 mt-5">
            <Button
              variant="primary"
              onClick={() =>
                navigate(`/citizen/requests/${created.requestReference}`)
              }
            >
              View Request
            </Button>
            <Button
              variant="secondary"
              onClick={() =>
                navigate(
                  `/citizen/requests/${created.requestReference}/documents/add`,
                  { state: { request: created } },
                )
              }
            >
              Add Document
            </Button>
            <Button variant="ghost" onClick={() => navigate('/citizen/requests')}>
              My Requests
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Submit Service Request"
        subtitle="Fill in the details below to submit a new government service request."
      />

      {!user?.citizenReference && (
        <Alert
          variant="warning"
          title="Citizen Reference Missing"
          message="Your citizen reference could not be determined. Please log out and log in again."
          className="mb-4"
        />
      )}

      {apiError && (
        <Alert
          variant="error"
          message={apiError}
          onDismiss={() => setApiError('')}
          className="mb-4"
        />
      )}

      <Card className="max-w-lg">
        <form onSubmit={handleSubmit} noValidate>
          <div className="space-y-4">
            <Select
              label="Service Type"
              options={serviceTypeOptions}
              value={serviceType}
              onChange={(e) => {
                setServiceType(e.target.value);
                if (errors.serviceType) setErrors((prev) => ({ ...prev, serviceType: undefined }));
              }}
              error={errors.serviceType}
              disabled={typesLoading}
            />

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <textarea
                rows={5}
                value={description}
                onChange={(e) => {
                  setDescription(e.target.value);
                  if (errors.description) setErrors((prev) => ({ ...prev, description: undefined }));
                }}
                placeholder="Describe your request in detail (minimum 10 characters)…"
                className={[
                  'w-full rounded-md border px-3 py-2 text-sm text-gray-900 placeholder-gray-400',
                  'focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent',
                  errors.description
                    ? 'border-red-400 bg-red-50'
                    : 'border-gray-300 bg-white',
                ].join(' ')}
              />
              {errors.description && (
                <p className="mt-1 text-xs text-red-600">{errors.description}</p>
              )}
              <p className="mt-1 text-xs text-gray-400">
                {description.length} character{description.length !== 1 ? 's' : ''} (minimum 10)
              </p>
            </div>
          </div>

          <div className="flex gap-3 mt-6">
            <Button
              type="submit"
              variant="primary"
              isLoading={submitting}
              disabled={!user?.citizenReference || typesLoading}
            >
              Submit Request
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={() => navigate('/citizen/requests')}
            >
              Cancel
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
};

export default CitizenRequestCreatePage;
