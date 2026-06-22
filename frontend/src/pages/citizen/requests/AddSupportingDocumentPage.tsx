import React, { useState } from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { documentApi } from '../../../api/documentApi';
import type { SupportingDocument } from '../../../types/document';
import type { ServiceRequestSummaryResponse } from '../../../types/serviceRequest';
import { getErrorMessage } from '../../../utils/errorUtils';
import { formatDateTime } from '../../../utils/dateUtils';
import PageHeader from '../../../components/layout/PageHeader';
import Button from '../../../components/ui/Button';
import Select from '../../../components/ui/Select';
import Input from '../../../components/ui/Input';
import Alert from '../../../components/ui/Alert';
import Card from '../../../components/ui/Card';
import Badge from '../../../components/ui/Badge';

const DOCUMENT_TYPE_OPTIONS = [
  { value: '',                label: 'Select document type…' },
  { value: 'NIC_COPY',        label: 'NIC Copy' },
  { value: 'BIRTH_CERTIFICATE', label: 'Birth Certificate' },
  { value: 'PASSPORT_COPY',   label: 'Passport Copy' },
  { value: 'ADDRESS_PROOF',   label: 'Address Proof' },
  { value: 'OTHER',           label: 'Other' },
];

interface FormErrors {
  documentType?: string;
  documentName?: string;
}

const AddSupportingDocumentPage: React.FC = () => {
  const navigate = useNavigate();
  const { requestReference } = useParams<{ requestReference: string }>();
  const location = useLocation();

  const passedRequest = (
    location.state as { request?: ServiceRequestSummaryResponse } | null
  )?.request ?? null;

  const [documentType, setDocumentType] = useState('');
  const [documentName, setDocumentName] = useState('');
  const [errors, setErrors]             = useState<FormErrors>({});
  const [submitting, setSubmitting]     = useState(false);
  const [apiError, setApiError]         = useState('');
  const [created, setCreated]           = useState<SupportingDocument | null>(null);

  const validate = (): boolean => {
    const next: FormErrors = {};
    if (!documentType) next.documentType = 'Please select a document type.';
    if (!documentName.trim()) next.documentName = 'Document name is required.';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!requestReference || !validate()) return;

    setSubmitting(true);
    setApiError('');
    try {
      const response = await documentApi.addSupportingDocument(requestReference, {
        documentType,
        documentName: documentName.trim(),
      });
      setCreated(response.data);
    } catch (err) {
      const status = (err as { response?: { status?: number } }).response?.status;
      if (status === 403) {
        setApiError('You do not have permission to add documents to this request.');
      } else if (status === 404) {
        setApiError('Service request not found.');
      } else {
        setApiError(getErrorMessage(err, 'Failed to add document. Please try again.'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (created) {
    return (
      <div>
        <PageHeader
          title="Document Added"
          subtitle="Your supporting document has been recorded successfully."
        />
        <Card className="max-w-lg">
          <Alert
            variant="success"
            title="Document Submitted"
            message="Your document metadata has been recorded and is pending verification."
            className="mb-4"
          />
          <dl className="space-y-3 text-sm">
            <div className="flex justify-between">
              <dt className="text-gray-500 font-medium">Document Reference</dt>
              <dd className="font-mono text-gray-900">{created.documentReference}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-gray-500 font-medium">Request Reference</dt>
              <dd className="font-mono text-gray-700">{created.requestReference}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-gray-500 font-medium">Document Type</dt>
              <dd className="text-gray-900">{created.documentType.replace(/_/g, ' ')}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-gray-500 font-medium">Document Name</dt>
              <dd className="text-gray-900">{created.documentName}</dd>
            </div>
            <div className="flex justify-between items-center">
              <dt className="text-gray-500 font-medium">Verification Status</dt>
              <dd>
                <Badge label="Pending" variant="PENDING" />
              </dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-gray-500 font-medium">Submitted On</dt>
              <dd className="text-gray-600">{formatDateTime(created.createdAt)}</dd>
            </div>
          </dl>
          <div className="flex gap-3 mt-5">
            <Button
              variant="secondary"
              onClick={() =>
                navigate(`/citizen/requests/${requestReference}`, {
                  state: { request: passedRequest },
                })
              }
            >
              Back to Request
            </Button>
            <Button
              variant="ghost"
              onClick={() => {
                setCreated(null);
                setDocumentType('');
                setDocumentName('');
              }}
            >
              Add Another Document
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
        title="Add Supporting Document"
        subtitle={
          requestReference
            ? `Adding document to request ${requestReference}`
            : 'Add a supporting document to your service request.'
        }
      />

      {passedRequest && (
        <Card className="max-w-lg mb-4 bg-gray-50">
          <div className="flex items-center justify-between text-sm">
            <span className="text-gray-500">
              Request:{' '}
              <span className="font-mono text-gray-800">{passedRequest.requestReference}</span>
            </span>
            <Badge
              label={passedRequest.status.replace(/_/g, ' ')}
              variant={passedRequest.status}
            />
          </div>
          <div className="text-sm text-gray-600 mt-1">
            {passedRequest.serviceType.replace(/_/g, ' ')}
          </div>
        </Card>
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
              label="Document Type"
              options={DOCUMENT_TYPE_OPTIONS}
              value={documentType}
              onChange={(e) => {
                setDocumentType(e.target.value);
                if (errors.documentType) setErrors((prev) => ({ ...prev, documentType: undefined }));
              }}
              error={errors.documentType}
            />

            <Input
              label="Document Name"
              placeholder="e.g. National Identity Card Front Side"
              value={documentName}
              onChange={(e) => {
                setDocumentName(e.target.value);
                if (errors.documentName) setErrors((prev) => ({ ...prev, documentName: undefined }));
              }}
              error={errors.documentName}
              hint="Provide a descriptive name for the document."
            />
          </div>

          <div className="flex gap-3 mt-6">
            <Button type="submit" variant="primary" isLoading={submitting}>
              Add Document
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={() =>
                navigate(`/citizen/requests/${requestReference}`, {
                  state: { request: passedRequest },
                })
              }
            >
              Cancel
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
};

export default AddSupportingDocumentPage;
