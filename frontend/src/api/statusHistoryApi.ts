import axiosClient from './axiosClient';
import { ENDPOINTS } from './endpoints';
import type { StatusHistoryResponse } from '../types/statusHistory';

export const statusHistoryApi = {
  getStatusHistory: async (requestReference: string): Promise<StatusHistoryResponse[]> => {
    const res = await axiosClient.get<StatusHistoryResponse[]>(
      ENDPOINTS.serviceRequests.statusHistory(requestReference),
    );
    return res.data;
  },
};
