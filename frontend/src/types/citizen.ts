export type CitizenStatus = 'ACTIVE' | 'INACTIVE';

// ── Full citizen record (GET single / GET list content) ───────────────────────

export interface Citizen {
  citizenReference: string;
  name: string;
  nic?: string;
  email: string;
  mobileNumber: string;
  address: string;
  status: CitizenStatus;
  username?: string;            // email used as login username
  mustChangePassword?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

// ── Summary record returned in paginated lists ────────────────────────────────

export interface CitizenSummary {
  citizenReference: string;
  name: string;
  nic?: string;
  email: string;
  mobileNumber: string;
  status: CitizenStatus;
}

// ── Response returned after successful citizen creation ───────────────────────

export interface CitizenCreatedResponse {
  citizenReference: string;
  name: string;
  nic?: string;
  email: string;
  mobileNumber: string;
  address: string;
  status: CitizenStatus;
  username?: string;
  mustChangePassword: boolean;
  temporaryPasswordNote: string;
}

// ── Request payloads ──────────────────────────────────────────────────────────

export interface CreateCitizenRequest {
  name: string;
  nic?: string;
  email: string;
  mobileNumber: string;
  address: string;
  temporaryPassword: string;
}

export interface UpdateCitizenRequest {
  name?: string;
  email?: string;
  mobileNumber?: string;
  address?: string;
  status?: CitizenStatus;
}

// ── Query parameters for GET /admin/citizens ──────────────────────────────────

export interface CitizenSearchParams {
  search?: string;
  status?: CitizenStatus;
  page?: number;
  size?: number;
}
