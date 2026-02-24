export type Role = 'USER' | 'ADMIN';

export interface UserProfile {
  id: number;
  username: string;
  email: string;
  role: Role;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  lastLoginAt: string | null;
}

export interface AuthResponse {
  token: string;
  user: UserProfile;
}
