export type ServiceRequestStatus =
  | 'SUBMITTED'
  | 'IN_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED';

// Full response — returned by POST /service-requests and GET /service-requests/{ref} (AGENT/ADMIN)
export interface ServiceRequestResponse {
  requestReference: string;
  serviceType: string;
  description: string;
  status: ServiceRequestStatus;
  citizenReference: string;
  citizenName: string;
  createdAt?: string;
  updatedAt?: string;
}

// Summary — returned by citizen list endpoint GET /citizens/{ref}/service-requests
export interface ServiceRequestSummaryResponse {
  requestReference: string;
  serviceType: string;
  status: ServiceRequestStatus;
  citizenReference: string;
  createdAt?: string;
  documents?: import('./document').SupportingDocumentSummaryResponse[];
}

// Kept for backward compatibility
export interface ServiceRequest {
  requestReference: string;
  citizenReference: string;
  citizenName: string;
  serviceType: string;
  description: string;
  status: ServiceRequestStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateServiceRequestRequest {
  serviceType: string;
  description: string;
  citizenReference?: string;
}

export interface UpdateServiceRequestRequest {
  serviceType?: string;
  description?: string;
}

export interface UpdateServiceRequestStatusRequest {
  status: ServiceRequestStatus;
  remarks?: string;
}

export interface StatusUpdateResponse {
  requestReference: string;
  previousStatus: ServiceRequestStatus;
  newStatus: ServiceRequestStatus;
  remarks?: string;
}

export interface ServiceRequestSearchParams {
  citizenReference?: string;
  status?: ServiceRequestStatus;
  serviceType?: string;
  page?: number;
  size?: number;
}
