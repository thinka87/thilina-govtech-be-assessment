import axiosClient from './axiosClient';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, PageResponse } from '../types/common';
import type {
  Citizen,
  CitizenCreatedResponse,
  CreateCitizenRequest,
  UpdateCitizenRequest,
  CitizenSearchParams,
} from '../types/citizen';

const PAGE_SIZE = 10;

export const citizenApi = {
  /**
   * GET /admin/citizens
   * Returns a paginated list of citizens.
   * Omits undefined / empty-string params before sending.
   */
  getCitizens: async (params: CitizenSearchParams = {}): Promise<PageResponse<Citizen>> => {
    const clean: Record<string, unknown> = {};
    (Object.entries(params) as [string, unknown][]).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') clean[k] = v;
    });
    if (!('size' in clean)) clean.size = PAGE_SIZE;

    const res = await axiosClient.get<PageResponse<Citizen>>(
      ENDPOINTS.citizens.base,
      { params: clean },
    );
    return res.data;
  },

  /**
   * GET /admin/citizens/{citizenReference}
   * Returns the full citizen record.
   */
  getCitizenByReference: async (citizenReference: string): Promise<Citizen> => {
    const res = await axiosClient.get<Citizen>(
      ENDPOINTS.citizens.byRef(citizenReference),
    );
    return res.data;
  },

  /**
   * POST /admin/citizens
   * Creates a new citizen. Returns ApiResponse<CitizenCreatedResponse>.
   */
  createCitizen: async (
    data: CreateCitizenRequest,
  ): Promise<ApiResponse<CitizenCreatedResponse>> => {
    const res = await axiosClient.post<ApiResponse<CitizenCreatedResponse>>(
      ENDPOINTS.citizens.base,
      data,
    );
    return res.data;
  },

  /**
   * PUT /admin/citizens/{citizenReference}
   * Updates an existing citizen.
   */
  updateCitizen: async (
    citizenReference: string,
    data: UpdateCitizenRequest,
  ): Promise<ApiResponse<Citizen>> => {
    const res = await axiosClient.put<ApiResponse<Citizen>>(
      ENDPOINTS.citizens.byRef(citizenReference),
      data,
    );
    return res.data;
  },

  deactivateCitizen: async (citizenReference: string): Promise<void> => {
    await axiosClient.delete(ENDPOINTS.citizens.deactivate(citizenReference));
  },
};
