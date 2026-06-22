export type NotificationStatus = 'UNREAD' | 'READ';

export interface NotificationResponse {
  id: number;
  citizenReference: string;
  requestReference: string;
  message: string;
  status: NotificationStatus;
  createdAt?: string;
}

export interface MarkNotificationReadResponse {
  id: number;
  status: NotificationStatus;
  message: string;
}

// Legacy type kept for backward compatibility
export interface StatusHistory {
  oldStatus: string | null;
  newStatus: string;
  changedBy: string;
  remarks?: string;
  changedAt: string;
}
