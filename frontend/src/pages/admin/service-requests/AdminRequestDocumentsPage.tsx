import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { documentApi } from '../../../api/documentApi';
import type { SupportingDocumentSummaryResponse, VerificationStatus } from '../../../types/document';
import { getErrorMessage } from '../../../utils/errorUtils';
import { formatDateTime } from '../../../utils/dateUtils';
import PageHeader from '../../../components/layout/PageHeader';
import Button from '../../../components/ui/Button';
import Badge from '../../../components/ui/Badge';
import Alert from '../../../components/ui/Alert';
import Card from '../../../components/ui/Card';
import LoadingSpinner from '../../../components/ui/LoadingSpinner';
import ConfirmDialog from '../../../components/ui/ConfirmDialog';

const AdminRequestDocumentsPage: React.FC = () => {
  const navigate = useNavigate();
  const { requestReference } = useParams<{ requestReference: string }>();

  const [documents, setDocuments] = useState<SupportingDocumentSummaryResponse[]>([]);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState('');
  const [success, setSuccess]     = useState('');

  const [deleteTarget, setDeleteTarget]   = useState<SupportingDocumentSummaryResponse | null>(null);
  const [deleting, setDeleting]           = useState(false);

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

  const handleConfirmDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await documentApi.deleteDocument(deleteTarget.documentReference);
      setSuccess('Document metadata deleted successfully.');
      setDeleteTarget(null);
      fetchDocuments();
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to delete document.'));
      setDeleteTarget(null);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="Supporting Documents"
        subtitle={`Request: ${requestReference}`}
        actions={
          <Button variant="ghost"
            onClick={() => navigate(`/admin/service-requests/${requestReference}`)}>
            ← Back to Request
          </Button>
        }
      />

      {success && (
        <Alert variant="success" message={success} onDismiss={() => setSuccess('')} className="mb-4" />
      )}
      {error && (
        <Alert variant="error" message={error} onDismiss={() => setError('')} className="mb-4" />
      )}

      {loading ? (
        <div className="flex justify-center py-16">
          <LoadingSpinner size="lg" label="Loading documents…" />
        </div>
      ) : documents.length === 0 ? (
        <Card>
          <div className="text-center py-10">
            <svg className="mx-auto h-10 w-10 text-gray-300 mb-3" fill="none"
              viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
            </svg>
            <p className="text-sm font-medium text-gray-600">No documents attached</p>
            <p className="text-xs text-gray-400 mt-1">No supporting documents have been submitted for this request.</p>
          </div>
        </Card>
      ) : (
        <Card padding="none">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  {['Document Reference', 'Type', 'Name', 'Verification', 'Submitted', 'Actions'].map((h) => (
                    <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {documents.map((doc) => (
                  <tr key={doc.documentReference} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 font-mono text-xs text-gray-600 whitespace-nowrap">
                      {doc.documentReference}
                    </td>
                    <td className="px-4 py-3 text-gray-700 whitespace-nowrap">
                      {doc.documentType.replace(/_/g, ' ')}
                    </td>
                    <td className="px-4 py-3 text-gray-800">
                      {doc.documentName}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <Badge
                        label={doc.verificationStatus}
                        variant={doc.verificationStatus as VerificationStatus}
                      />
                    </td>
                    <td className="px-4 py-3 text-gray-500 text-xs whitespace-nowrap">
                      {formatDateTime(doc.createdAt)}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <Button
                        size="sm"
                        variant="danger"
                        onClick={() => setDeleteTarget(doc)}
                      >
                        Delete
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      <ConfirmDialog
        isOpen={!!deleteTarget}
        title="Delete Document Metadata"
        message={
          deleteTarget
            ? `Are you sure you want to delete document "${deleteTarget.documentName}" (${deleteTarget.documentReference})? This action cannot be undone. Note: only metadata is deleted — no actual file upload exists in this system.`
            : ''
        }
        confirmLabel="Delete"
        variant="danger"
        isLoading={deleting}
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
};

export default AdminRequestDocumentsPage;
