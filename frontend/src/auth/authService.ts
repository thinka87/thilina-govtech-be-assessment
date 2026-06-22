import axiosClient from '../api/axiosClient';
import { ENDPOINTS } from '../api/endpoints';
import type { LoginRequest, LoginResponse, ChangePasswordRequest } from '../types/auth';

export const authService = {
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await axiosClient.post<LoginResponse>(
      ENDPOINTS.auth.login,
      credentials,
    );
    return response.data;
  },

  changePassword: async (request: ChangePasswordRequest): Promise<void> => {
    await axiosClient.patch(ENDPOINTS.auth.changePassword, request);
  },
};
