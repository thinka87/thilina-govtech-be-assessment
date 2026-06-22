import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import { serviceRequestApi } from '../../../api/serviceRequestApi';
import type {
  ServiceRequestSummaryResponse,
  ServiceRequestStatus,
} from '../../../types/serviceRequest';
import type { SupportingDocumentSummaryResponse, VerificationStatus } from '../../../types/document';
import { getErrorMessage } from '../../../utils/errorUtils';
import { formatDateTime } from '../../../utils/dateUtils';
import PageHeader from '../../../components/layout/PageHeader';
import Button from '../../../components/ui/Button';
import Badge from '../../../components/ui/Badge';
import Alert from '../../../components/ui/Alert';
import Card from '../../../components/ui/Card';
import LoadingSpinner from '../../../components/ui/LoadingSpinner';

const CitizenRequestDetailPage: React.FC = () => {
  const navigate = useNavigate();
  const { requestReference } = useParams<{ requestReference: string }>();
  const { user } = useAuth();

  const [request, setRequest] = useState<ServiceRequestSummaryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState('');

  const fetchRequest = useCallback(async () => {
    const citizenRef = user?.citizenReference;
    if (!citizenRef || !requestReference) return;

    setLoading(true);
    setError('');
    try {
      let found: ServiceRequestSummaryResponse | undefined;
      let page = 0;
      let totalPages = 1;

      while (page < totalPages && !found) {
        const data = await serviceRequestApi.getCitizenServiceRequests(citizenRef, {
          page,
          size: 50,
        });
        found = data.content.find((r) => r.requestReference === requestReference);
        totalPages = data.totalPages;
        page += 1;
      }

      if (found) {
        setRequest(found);
      } else {
        setError('Service request not found.');
      }
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load request details.'));
    } finally {
      setLoading(false);
    }
  }, [user?.citizenReference, requestReference]);

  useEffect(() => { fetchRequest(); }, [fetchRequest]);

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
        <Button variant="secondary" onClick={() => navigate('/citizen/requests')}>
          ← Back to My Requests
        </Button>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Request Details"
        subtitle={`Reference: ${request.requestReference}`}
      />

      <Card className="max-w-2xl mb-4">
        <dl className="space-y-4 text-sm">
          <div className="flex justify-between items-center">
            <dt className="text-gray-500 font-medium">Reference</dt>
            <dd className="font-mono text-gray-900">{request.requestReference}</dd>
          </div>

          <div className="flex justify-between items-center">
            <dt className="text-gray-500 font-medium">Status</dt>
            <dd>
              <Badge
                label={request.status.replace(/_/g, ' ')}
                variant={request.status as ServiceRequestStatus}
              />
            </dd>
          </div>

          <div className="flex justify-between items-center">
            <dt className="text-gray-500 font-medium">Service Type</dt>
            <dd className="text-gray-900">{request.serviceType.replace(/_/g, ' ')}</dd>
          </div>

          <div className="flex justify-between items-center">
            <dt className="text-gray-500 font-medium">Citizen Reference</dt>
            <dd className="font-mono text-gray-600 text-xs">{request.citizenReference}</dd>
          </div>

          <div className="flex justify-between items-center">
            <dt className="text-gray-500 font-medium">Submitted On</dt>
            <dd className="text-gray-600">{formatDateTime(request.createdAt)}</dd>
          </div>
        </dl>
      </Card>

      {/* Supporting Documents */}
      {Array.isArray(request.documents) && (
        <Card className="max-w-2xl mb-4">
          <h2 className="text-sm font-semibold text-gray-800 mb-3">
            Supporting Documents
            <span className="ml-2 text-xs font-normal text-gray-400">
              ({request.documents.length})
            </span>
          </h2>
          {request.documents.length === 0 ? (
            <p className="text-sm text-gray-400 italic">No documents attached yet.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    {['Document Name', 'Type', 'Verification Status', 'Uploaded'].map((h) => (
                      <th key={h} className="px-3 py-2 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {(request.documents as SupportingDocumentSummaryResponse[]).map((doc) => (
                    <tr key={doc.documentReference}>
                      <td className="px-3 py-2 text-gray-800">{doc.documentName}</td>
                      <td className="px-3 py-2 text-gray-600">{doc.documentType.replace(/_/g, ' ')}</td>
                      <td className="px-3 py-2">
                        <Badge label={doc.verificationStatus} variant={doc.verificationStatus as VerificationStatus} />
                      </td>
                      <td className="px-3 py-2 text-gray-500 text-xs whitespace-nowrap">
                        {formatDateTime(doc.createdAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      )}

      <div className="flex gap-3">
        <Button variant="secondary" onClick={() => navigate('/citizen/requests')}>
          ← Back to My Requests
        </Button>
        {request.status !== 'CANCELLED' && (
          <Button
            variant="primary"
            onClick={() =>
              navigate(
                `/citizen/requests/${request.requestReference}/documents/add`,
                { state: { request } },
              )
            }
          >
            Add Supporting Document
          </Button>
        )}
      </div>
    </div>
  );
};

export default CitizenRequestDetailPage;
