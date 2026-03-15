import apiClient from './apiClient';
import { TOKEN_KEY } from '../utils/auth-context';

const BASE_PATH = '/backup';

export interface BackupMetadata {
  version: string;
  exportTimestamp: string;
  userId: number;
  username: string;
  ledgerRootHash: string | null;
  ledgerEventCount: number;
  expenseCount: number;
  investmentCount: number;
  loanCount: number;
  sipCount: number;
}

export const backupApi = {
  /**
   * Export an encrypted backup. Returns the encrypted binary as a Blob.
   * We use fetch() directly here instead of axios because we need
   * to handle a binary response as a file download.
   */
  exportBackup: async (password: string): Promise<Blob> => {
    const token = localStorage.getItem(TOKEN_KEY);

    const response = await fetch(`/api${BASE_PATH}/export`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ password }),
    });

    if (!response.ok) {
      let errorMessage = 'Export failed';
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
      } catch {
        // Response wasn't JSON — use status text
        errorMessage = response.statusText || errorMessage;
      }
      throw new Error(errorMessage);
    }

    return response.blob();
  },

  /**
   * Import an encrypted backup file.
   * Uses multipart/form-data so axios can handle the FormData natively.
   */
  importBackup: async (file: File, password: string): Promise<BackupMetadata> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('password', password);

    const response = await apiClient.post(`${BASE_PATH}/import`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120_000, // 2 min — imports can be large
    });

    return response.data.data;
  },
};
