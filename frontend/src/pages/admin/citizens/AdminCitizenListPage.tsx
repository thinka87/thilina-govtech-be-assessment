import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { citizenApi } from '../../../api/citizenApi';
import type { Citizen, CitizenStatus } from '../../../types/citizen';
import { getErrorMessage } from '../../../utils/errorUtils';
import PageHeader from '../../../components/layout/PageHeader';
import Button from '../../../components/ui/Button';
import Input from '../../../components/ui/Input';
import Select from '../../../components/ui/Select';
import Badge from '../../../components/ui/Badge';
import Alert from '../../../components/ui/Alert';
import Card from '../../../components/ui/Card';
import LoadingSpinner from '../../../components/ui/LoadingSpinner';
import ConfirmDialog from '../../../components/ui/ConfirmDialog';

const PAGE_SIZE = 10;

const STATUS_OPTIONS = [
  { value: '',         label: 'All Statuses' },
  { value: 'ACTIVE',   label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
];

const AdminCitizenListPage: React.FC = () => {
  const navigate = useNavigate();

  // ── Data state ───────────────────────────────────────────────────────────────
  const [citizens, setCitizens]           = useState<Citizen[]>([]);
  const [loading, setLoading]             = useState(false);
  const [error, setError]                 = useState('');
  const [success, setSuccess]             = useState('');
  const [page, setPage]                   = useState(0);
  const [totalPages, setTotalPages]       = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // ── Search / filter state ────────────────────────────────────────────────────
  const [searchInput, setSearchInput]       = useState('');
  const [committedSearch, setCommittedSearch] = useState('');
  const [statusFilter, setStatusFilter]     = useState('');

  // ── Deactivation state ───────────────────────────────────────────────────────
  const [deactivateTarget, setDeactivateTarget] = useState<Citizen | null>(null);
  const [deactivating, setDeactivating]         = useState(false);

  // ── Fetch ────────────────────────────────────────────────────────────────────
  const fetchCitizens = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = {
        page,
        size: PAGE_SIZE,
        ...(committedSearch ? { search: committedSearch }               : {}),
        ...(statusFilter    ? { status: statusFilter as CitizenStatus } : {}),
      };
      const data = await citizenApi.getCitizens(params);
      setCitizens(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load citizens.'));
    } finally {
      setLoading(false);
    }
  }, [page, committedSearch, statusFilter]);

  useEffect(() => { fetchCitizens(); }, [fetchCitizens]);

  // ── Handlers ─────────────────────────────────────────────────────────────────
  const handleSearch = () => {
    setPage(0);
    setCommittedSearch(searchInput.trim());
  };

  const handleClear = () => {
    setSearchInput('');
    setCommittedSearch('');
    setStatusFilter('');
    setPage(0);
  };

  const handleConfirmDeactivate = async () => {
    if (!deactivateTarget) return;
    setDeactivating(true);
    try {
      await citizenApi.deactivateCitizen(deactivateTarget.citizenReference);
      setSuccess(`Citizen "${deactivateTarget.name}" has been deactivated successfully.`);
      setDeactivateTarget(null);
      fetchCitizens();
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to deactivate citizen.'));
      setDeactivateTarget(null);
    } finally {
      setDeactivating(false);
    }
  };

  // ── Pagination helpers ───────────────────────────────────────────────────────
  const startRecord = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const endRecord   = Math.min((page + 1) * PAGE_SIZE, totalElements);

  // ── Render ───────────────────────────────────────────────────────────────────
  return (
    <div>
      <PageHeader
        title="Citizens"
        subtitle="Manage citizen accounts and service access."
        actions={
          <Button variant="primary" onClick={() => navigate('/admin/citizens/create')}>
            + Create Citizen
          </Button>
        }
      />

      {/* ── Search / filter bar ── */}
      <Card className="mb-4">
        <div className="flex flex-wrap gap-3 items-end">
          <div className="flex-1 min-w-48">
            <Input
              label="Search"
              placeholder="Search by name…"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            />
          </div>
          <div className="w-44">
            <Select
              label="Status"
              options={STATUS_OPTIONS}
              value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
            />
          </div>
          <Button variant="primary" onClick={handleSearch}>
            Search
          </Button>
          <Button variant="secondary" onClick={handleClear}>
            Clear
          </Button>
        </div>
      </Card>

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

      {/* ── Table card ── */}
      <Card padding="none">
        {loading ? (
          <div className="flex justify-center py-16">
            <LoadingSpinner size="lg" label="Loading citizens…" />
          </div>
        ) : citizens.length === 0 ? (
          <div className="text-center py-16">
            <svg className="mx-auto h-12 w-12 text-gray-300 mb-3" fill="none"
              viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" />
            </svg>
            <p className="text-sm font-medium text-gray-600">No citizens found</p>
            <p className="text-xs text-gray-400 mt-1">
              {committedSearch || statusFilter
                ? 'Try adjusting your search or clearing the filters.'
                : 'Create the first citizen using the button above.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  {[
                    'Citizen Reference',
                    'Name',
                    'NIC',
                    'Email',
                    'Mobile',
                    'Status',
                    'Actions',
                  ].map((h) => (
                    <th
                      key={h}
                      className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap"
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {citizens.map((citizen) => (
                  <tr
                    key={citizen.citizenReference}
                    className="hover:bg-gray-50 transition-colors"
                  >
                    <td className="px-4 py-3 font-mono text-xs text-gray-700 whitespace-nowrap">
                      {citizen.citizenReference}
                    </td>
                    <td className="px-4 py-3 font-medium text-gray-900 whitespace-nowrap">
                      {citizen.name}
                    </td>
                    <td className="px-4 py-3 text-gray-600 whitespace-nowrap">
                      {citizen.nic ?? '—'}
                    </td>
                    <td className="px-4 py-3 text-gray-600">
                      {citizen.email}
                    </td>
                    <td className="px-4 py-3 text-gray-600 whitespace-nowrap">
                      {citizen.mobileNumber}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <Badge
                        label={citizen.status}
                        variant={citizen.status}
                      />
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <div className="flex items-center gap-1.5">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() =>
                            navigate(`/admin/citizens/${citizen.citizenReference}`)
                          }
                        >
                          View
                        </Button>
                        <Button
                          size="sm"
                          variant="secondary"
                          onClick={() =>
                            navigate(`/admin/citizens/${citizen.citizenReference}/edit`)
                          }
                        >
                          Edit
                        </Button>
                        {citizen.status === 'ACTIVE' && (
                          <Button
                            size="sm"
                            variant="danger"
                            onClick={() => setDeactivateTarget(citizen)}
                          >
                            Deactivate
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* ── Pagination ── */}
        {!loading && totalElements > 0 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-gray-50">
            <p className="text-xs text-gray-500">
              Showing{' '}
              <span className="font-medium text-gray-700">{startRecord}–{endRecord}</span>
              {' '}of{' '}
              <span className="font-medium text-gray-700">{totalElements}</span>
              {' '}citizens
            </p>
            <div className="flex items-center gap-2">
              <Button
                size="sm"
                variant="secondary"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                ← Previous
              </Button>
              <span className="text-xs text-gray-600 px-1">
                Page {page + 1} of {totalPages}
              </span>
              <Button
                size="sm"
                variant="secondary"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                Next →
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* ── Deactivate confirm ── */}
      <ConfirmDialog
        isOpen={!!deactivateTarget}
        title="Deactivate Citizen"
        message={
          deactivateTarget
            ? `Are you sure you want to deactivate "${deactivateTarget.name}" (${deactivateTarget.citizenReference})? Their account will be disabled and they will not be able to log in.`
            : ''
        }
        confirmLabel="Deactivate"
        variant="danger"
        isLoading={deactivating}
        onConfirm={handleConfirmDeactivate}
        onCancel={() => setDeactivateTarget(null)}
      />
    </div>
  );
};

export default AdminCitizenListPage;
