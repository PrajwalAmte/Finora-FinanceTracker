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
  // Vault encryption
  vaultEnabled: boolean;
  vaultSalt: string | null;
}

export interface AuthResponse {
  token: string;
  user: UserProfile;
}

export interface VaultStatus {
  vaultEnabled: boolean;
  vaultSalt: string | null;
}

export interface VaultEnableRequest {
  passphrase: string;
  confirmation: string;
}

export interface VaultDisableRequest {
  passphrase: string;
}
