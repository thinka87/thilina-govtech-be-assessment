import axiosClient from './axiosClient';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, PageResponse } from '../types/common';
import type {
  CreateServiceRequestRequest,
  ServiceRequestSummaryResponse,
  ServiceRequestResponse,
  UpdateServiceRequestRequest,
  UpdateServiceRequestStatusRequest,
  StatusUpdateResponse,
} from '../types/serviceRequest';

export const serviceRequestApi = {
  createServiceRequest: async (
    data: CreateServiceRequestRequest,
  ): Promise<ApiResponse<ServiceRequestResponse>> => {
    const res = await axiosClient.post<ApiResponse<ServiceRequestResponse>>(
      ENDPOINTS.serviceRequests.base,
      data,
    );
    return res.data;
  },

  getServiceRequestByReference: async (
    requestReference: string,
  ): Promise<ServiceRequestResponse> => {
    const res = await axiosClient.get<ServiceRequestResponse>(
      ENDPOINTS.serviceRequests.byRef(requestReference),
    );
    return res.data;
  },

  getCitizenServiceRequests: async (
    citizenReference: string,
    params: { page?: number; size?: number } = {},
  ): Promise<PageResponse<ServiceRequestSummaryResponse>> => {
    const res = await axiosClient.get<PageResponse<ServiceRequestSummaryResponse>>(
      ENDPOINTS.citizens.serviceRequests(citizenReference),
      { params: { size: 10, ...params } },
    );
    return res.data;
  },

  searchServiceRequests: async (params: {
    citizenReference?: string;
    status?: string;
    serviceType?: string;
    page?: number;
    size?: number;
  } = {}): Promise<PageResponse<ServiceRequestSummaryResponse>> => {
    const clean: Record<string, unknown> = {};
    (Object.entries(params) as [string, unknown][]).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') clean[k] = v;
    });
    if (!('size' in clean)) clean.size = 10;

    const res = await axiosClient.get<PageResponse<ServiceRequestSummaryResponse>>(
      ENDPOINTS.serviceRequests.base,
      { params: clean },
    );
    return res.data;
  },

  updateServiceRequest: async (
    requestReference: string,
    data: UpdateServiceRequestRequest,
  ): Promise<ServiceRequestResponse> => {
    const res = await axiosClient.put<ServiceRequestResponse>(
      ENDPOINTS.serviceRequests.byRef(requestReference),
      data,
    );
    return res.data;
  },

  updateServiceRequestStatus: async (
    requestReference: string,
    data: UpdateServiceRequestStatusRequest,
  ): Promise<ApiResponse<StatusUpdateResponse>> => {
    const res = await axiosClient.patch<ApiResponse<StatusUpdateResponse>>(
      ENDPOINTS.serviceRequests.updateStatus(requestReference),
      data,
    );
    return res.data;
  },

  cancelServiceRequest: async (requestReference: string): Promise<void> => {
    await axiosClient.delete(ENDPOINTS.serviceRequests.byRef(requestReference));
  },

  getServiceTypes: async (): Promise<string[]> => {
    const res = await axiosClient.get<string[]>(ENDPOINTS.serviceRequests.types);
    return res.data;
  },
};
