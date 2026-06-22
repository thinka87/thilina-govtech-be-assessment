export type VerificationStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

// Lightweight summary returned by GET /service-requests/{ref}/documents (no requestReference, no updatedAt)
export interface SupportingDocumentSummaryResponse {
  documentReference: string;
  documentType: string;
  documentName: string;
  verificationStatus: VerificationStatus;
  createdAt?: string;
}

// Matches backend SupportingDocumentResponse
export interface SupportingDocument {
  documentReference: string;
  requestReference: string;
  documentType: string;
  documentName: string;
  verificationStatus: VerificationStatus;
  createdAt?: string;
  updatedAt?: string;
}

// Kept for backward compatibility
export type Document = SupportingDocument;

// Matches backend CreateSupportingDocumentRequest
export interface CreateDocumentRequest {
  documentType: string;
  documentName: string;
  documentReference?: string;
}

// Kept for backward compatibility
export type AddDocumentRequest = CreateDocumentRequest;

export interface UpdateDocumentRequest {
  documentType?: string;
  documentName?: string;
}

export interface UpdateVerificationStatusRequest {
  verificationStatus: VerificationStatus;
  remarks?: string;
}
