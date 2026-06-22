import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { documentApi } from '../../../api/documentApi';
import type {
  SupportingDocumentSummaryResponse,
  VerificationStatus,
} from '../../../types/document';
import { getErrorMessage } from '../../../utils/errorUtils';
import { formatDateTime } from '../../../utils/dateUtils';
import PageHeader from '../../../components/layout/PageHeader';
import Button from '../../../components/ui/Button';
import Badge from '../../../components/ui/Badge';
import Alert from '../../../components/ui/Alert';
import Card from '../../../components/ui/Card';
import Select from '../../../components/ui/Select';
import LoadingSpinner from '../../../components/ui/LoadingSpinner';

const VERIFICATION_OPTIONS = [
  { value: 'PENDING',  label: 'Pending' },
  { value: 'VERIFIED', label: 'Verified' },
  { value: 'REJECTED', label: 'Rejected' },
];

interface VerifyFormState {
  status: string;
  remarks: string;
  loading: boolean;
  error: string;
}

const AgentRequestDocumentsPage: React.FC = () => {
  const navigate = useNavigate();
  const { requestReference } = useParams<{ requestReference: string }>();

  const [documents, setDocuments] = useState<SupportingDocumentSummaryResponse[]>([]);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState('');
  const [success, setSuccess]     = useState('');

  // Per-document inline verify form: keyed by documentReference
  const [verifyForms, setVerifyForms] = useState<Record<string, VerifyFormState>>({});
  const [expandedDoc, setExpandedDoc] = useState<string | null>(null);

  const fetchDocuments = useCallback(async () => {
    if (!requestReference) return;
    setLoading(true);
    setError('');
    try {
      const data = await documentApi.getDocumentsByServiceRequest(requestReference);
      setDocuments(data);
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load documents.'));
    } finally {
      setLoading(false);
    }
  }, [requestReference]);

  useEffect(() => { fetchDocuments(); }, [fetchDocuments]);

  const getVerifyForm = (docRef: string, currentStatus: VerificationStatus): VerifyFormState =>
    verifyForms[docRef] ?? { status: currentStatus, remarks: '', loading: false, error: '' };

  const setVerifyField = (docRef: string, field: keyof VerifyFormState, value: string | boolean) => {
    setVerifyForms((prev) => ({
      ...prev,
      [docRef]: { ...getVerifyForm(docRef, 'PENDING'), [field]: value },
    }));
  };

  const handleVerify = async (docRef: string) => {
    const form = verifyForms[docRef];
    if (!form || !form.status) return;

    setVerifyForms((prev) => ({
      ...prev,
      [docRef]: { ...form, loading: true, error: '' },
    }));

    try {
      await documentApi.updateDocumentVerificationStatus(docRef, {
        verificationStatus: form.status as VerificationStatus,
        ...(form.remarks.trim() ? { remarks: form.remarks.trim() } : {}),
      });
      setSuccess(`Document verification status updated to ${form.status}.`);
      setExpandedDoc(null);
      await fetchDocuments();
    } catch (err) {
      setVerifyForms((prev) => ({
        ...prev,
        [docRef]: { ...form, loading: false, error: getErrorMessage(err, 'Failed to update verification status.') },
      }));
    }
  };

  return (
    <div>
      <PageHeader
        title="Supporting Documents"
        subtitle={`Request: ${requestReference}`}
        actions={
          <Button variant="ghost" onClick={() => navigate(`/agent/requests/${requestReference}`)}>
            ← Back to Request
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
      {error && (
        <Alert
          variant="error"
          message={error}
          onDismiss={() => setError('')}
          className="mb-4"
        />
      )}

      {loading ? (
        <div className="flex justify-center py-16">
          <LoadingSpinner size="lg" label="Loading documents…" />
        </div>
      ) : documents.length === 0 ? (
        <Card>
          <div className="text-center py-8">
            <svg className="mx-auto h-10 w-10 text-gray-300 mb-3" fill="none"
              viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
            </svg>
            <p className="text-sm font-medium text-gray-600">No documents attached</p>
            <p className="text-xs text-gray-400 mt-1">The citizen has not added any supporting documents yet.</p>
          </div>
        </Card>
      ) : (
        <div className="space-y-3">
          {documents.map((doc) => {
            const form = getVerifyForm(doc.documentReference, doc.verificationStatus);
            const isExpanded = expandedDoc === doc.documentReference;

            return (
              <Card key={doc.documentReference}>
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-3 mb-1">
                      <span className="text-sm font-semibold text-gray-800">
                        {doc.documentName}
                      </span>
                      <Badge
                        label={doc.verificationStatus}
                        variant={doc.verificationStatus as VerificationStatus}
                      />
                    </div>
                    <div className="flex flex-wrap gap-x-6 gap-y-1 text-xs text-gray-500">
                      <span>Type: <span className="text-gray-700">{doc.documentType.replace(/_/g, ' ')}</span></span>
                      <span className="font-mono">{doc.documentReference}</span>
                      <span>Added: {formatDateTime(doc.createdAt)}</span>
                    </div>
                  </div>
                  <Button
                    size="sm"
                    variant={isExpanded ? 'secondary' : 'outline'}
                    onClick={() => setExpandedDoc(isExpanded ? null : doc.documentReference)}
                  >
                    {isExpanded ? 'Cancel' : 'Verify'}
                  </Button>
                </div>

                {isExpanded && (
                  <div className="mt-4 pt-4 border-t border-gray-100 space-y-4">
                    {form.error && (
                      <Alert
                        variant="error"
                        message={form.error}
                        onDismiss={() => setVerifyField(doc.documentReference, 'error', '')}
                      />
                    )}
                    <Select
                      label="Verification Status"
                      options={VERIFICATION_OPTIONS}
                      value={form.status}
                      onChange={(e) => setVerifyField(doc.documentReference, 'status', e.target.value)}
                    />
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Remarks <span className="text-gray-400 font-normal">(optional)</span>
                      </label>
                      <textarea
                        className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm
                                   shadow-sm focus:outline-none focus:ring-2 focus:ring-primary-500
                                   focus:border-primary-500 placeholder-gray-400 resize-none"
                        rows={2}
                        placeholder="Add remarks about the verification decision…"
                        value={form.remarks}
                        onChange={(e) => setVerifyField(doc.documentReference, 'remarks', e.target.value)}
                        disabled={form.loading}
                      />
                    </div>
                    <Button
                      variant="primary"
                      size="sm"
                      isLoading={form.loading}
                      onClick={() => handleVerify(doc.documentReference)}
                    >
                      Save Verification
                    </Button>
                  </div>
                )}
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default AgentRequestDocumentsPage;
