export const ENDPOINTS = {
  auth: {
    login:          '/auth/login',
    changePassword: '/auth/change-password',
  },

  citizens: {
    base:            '/citizens',
    byRef:           (ref: string) => `/citizens/${ref}`,
    deactivate:      (ref: string) => `/citizens/${ref}`,
    serviceRequests: (ref: string) => `/citizens/${ref}/service-requests`,
    notifications:   (ref: string) => `/citizens/${ref}/notifications`,
  },

  serviceRequests: {
    base:          '/service-requests',
    types:         '/service-requests/types',
    byRef:         (ref: string) => `/service-requests/${ref}`,
    updateStatus:  (ref: string) => `/service-requests/${ref}/status`,
    cancel:        (ref: string) => `/service-requests/${ref}/cancel`,
    statusHistory: (ref: string) => `/service-requests/${ref}/status-history`,
    documents:     (ref: string) => `/service-requests/${ref}/documents`,
  },

  documents: {
    base:               '/documents',
    byRef:              (ref: string) => `/documents/${ref}`,
    verificationStatus: (ref: string) => `/documents/${ref}/verification-status`,
  },

  notifications: {
    base:     '/notifications',
    markRead: (id: number) => `/notifications/${id}/read`,
  },
};
