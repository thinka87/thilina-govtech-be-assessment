import axiosClient from './axiosClient';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse } from '../types/common';
import type {
  SupportingDocument,
  SupportingDocumentSummaryResponse,
  CreateDocumentRequest,
  UpdateDocumentRequest,
  UpdateVerificationStatusRequest,
} from '../types/document';

export const documentApi = {
  addSupportingDocument: async (
    requestReference: string,
    data: CreateDocumentRequest,
  ): Promise<ApiResponse<SupportingDocument>> => {
    const res = await axiosClient.post<ApiResponse<SupportingDocument>>(
      ENDPOINTS.serviceRequests.documents(requestReference),
      data,
    );
    return res.data;
  },

  getDocumentsByServiceRequest: async (
    requestReference: string,
  ): Promise<SupportingDocumentSummaryResponse[]> => {
    const res = await axiosClient.get<SupportingDocumentSummaryResponse[]>(
      ENDPOINTS.serviceRequests.documents(requestReference),
    );
    return res.data;
  },

  getDocumentByReference: async (
    documentReference: string,
  ): Promise<SupportingDocument> => {
    const res = await axiosClient.get<SupportingDocument>(
      ENDPOINTS.documents.byRef(documentReference),
    );
    return res.data;
  },

  updateDocumentMetadata: async (
    documentReference: string,
    data: UpdateDocumentRequest,
  ): Promise<SupportingDocument> => {
    const res = await axiosClient.put<SupportingDocument>(
      ENDPOINTS.documents.byRef(documentReference),
      data,
    );
    return res.data;
  },

  updateDocumentVerificationStatus: async (
    documentReference: string,
    data: UpdateVerificationStatusRequest,
  ): Promise<ApiResponse<SupportingDocument>> => {
    const res = await axiosClient.patch<ApiResponse<SupportingDocument>>(
      ENDPOINTS.documents.verificationStatus(documentReference),
      data,
    );
    return res.data;
  },

  deleteDocument: async (documentReference: string): Promise<void> => {
    await axiosClient.delete(ENDPOINTS.documents.byRef(documentReference));
  },
};
