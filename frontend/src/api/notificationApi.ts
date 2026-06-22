import axiosClient from './axiosClient';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, PageResponse } from '../types/common';
import type { NotificationResponse, MarkNotificationReadResponse } from '../types/notification';

export const notificationApi = {
  getCitizenNotifications: async (
    citizenReference: string,
    params: { page?: number; size?: number } = {},
  ): Promise<PageResponse<NotificationResponse>> => {
    const res = await axiosClient.get<PageResponse<NotificationResponse>>(
      ENDPOINTS.citizens.notifications(citizenReference),
      { params: { size: 10, ...params } },
    );
    return res.data;
  },

  markNotificationAsRead: async (
    notificationId: number,
  ): Promise<ApiResponse<MarkNotificationReadResponse>> => {
    const res = await axiosClient.patch<ApiResponse<MarkNotificationReadResponse>>(
      ENDPOINTS.notifications.markRead(notificationId),
    );
    return res.data;
  },
};
