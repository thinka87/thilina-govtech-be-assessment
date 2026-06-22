import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { statusHistoryApi } from '../../../api/statusHistoryApi';
import type { StatusHistoryResponse } from '../../../types/statusHistory';
import type { ServiceRequestStatus } from '../../../types/serviceRequest';
import { getErrorMessage } from '../../../utils/errorUtils';
import { formatDateTime } from '../../../utils/dateUtils';
import PageHeader from '../../../components/layout/PageHeader';
import Button from '../../../components/ui/Button';
import Badge from '../../../components/ui/Badge';
import Alert from '../../../components/ui/Alert';
import Card from '../../../components/ui/Card';
import LoadingSpinner from '../../../components/ui/LoadingSpinner';

const AgentStatusHistoryPage: React.FC = () => {
  const navigate = useNavigate();
  const { requestReference } = useParams<{ requestReference: string }>();

  const [history, setHistory] = useState<StatusHistoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState('');

  const fetchHistory = useCallback(async () => {
    if (!requestReference) return;
    setLoading(true);
    setError('');
    try {
      const data = await statusHistoryApi.getStatusHistory(requestReference);
      setHistory(data);
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load status history.'));
    } finally {
      setLoading(false);
    }
  }, [requestReference]);

  useEffect(() => { fetchHistory(); }, [fetchHistory]);

  return (
    <div>
      <PageHeader
        title="Status History"
        subtitle={`Request: ${requestReference}`}
        actions={
          <Button variant="ghost" onClick={() => navigate(`/agent/requests/${requestReference}`)}>
            ← Back to Request
          </Button>
        }
      />

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
          <LoadingSpinner size="lg" label="Loading status history…" />
        </div>
      ) : history.length === 0 ? (
        <Card>
          <div className="text-center py-8">
            <p className="text-sm font-medium text-gray-600">No status history found</p>
            <p className="text-xs text-gray-400 mt-1">No status transitions have been recorded yet.</p>
          </div>
        </Card>
      ) : (
        <Card padding="none">
          <ul className="divide-y divide-gray-100">
            {history.map((entry, index) => (
              <li key={entry.id} className="px-5 py-4">
                <div className="flex items-start gap-4">
                  {/* Timeline indicator */}
                  <div className="flex flex-col items-center mt-1">
                    <div className={[
                      'h-3 w-3 rounded-full border-2',
                      index === 0
                        ? 'border-primary-600 bg-primary-600'
                        : 'border-gray-300 bg-white',
                    ].join(' ')} />
                    {index < history.length - 1 && (
                      <div className="w-px flex-1 bg-gray-200 mt-1" style={{ minHeight: 24 }} />
                    )}
                  </div>

                  {/* Entry content */}
                  <div className="flex-1 min-w-0 pb-2">
                    {/* Status transition */}
                    <div className="flex flex-wrap items-center gap-2 mb-1">
                      {entry.oldStatus ? (
                        <>
                          <Badge
                            label={entry.oldStatus}
                            variant={entry.oldStatus as ServiceRequestStatus}
                          />
                          <span className="text-gray-400 text-sm">→</span>
                        </>
                      ) : (
                        <span className="text-xs text-gray-400 italic mr-1">Initial</span>
                      )}
                      <Badge
                        label={entry.newStatus}
                        variant={entry.newStatus as ServiceRequestStatus}
                      />
                    </div>

                    {/* Meta */}
                    <div className="flex flex-wrap gap-x-4 gap-y-0.5 text-xs text-gray-500 mb-1">
                      <span>By: <span className="text-gray-700 font-medium">{entry.changedBy}</span></span>
                      <span>{formatDateTime(entry.changedAt)}</span>
                    </div>

                    {/* Remarks */}
                    {entry.remarks && (
                      <p className="text-sm text-gray-600 bg-gray-50 rounded px-3 py-2 mt-2 leading-relaxed">
                        {entry.remarks}
                      </p>
                    )}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  );
};

export default AgentStatusHistoryPage;
