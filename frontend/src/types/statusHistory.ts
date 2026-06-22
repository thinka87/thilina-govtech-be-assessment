import type { ServiceRequestStatus } from './serviceRequest';

export interface StatusHistoryResponse {
  id: number;
  requestReference: string;
  oldStatus: ServiceRequestStatus | null;
  newStatus: ServiceRequestStatus;
  changedBy: string;
  remarks?: string;
  changedAt: string;
}
