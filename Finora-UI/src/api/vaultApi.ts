import apiClient from './apiClient';
import { VaultStatus, VaultEnableRequest, VaultDisableRequest } from '../types/User';

const BASE_PATH = '/users/vault';

export const vaultApi = {
  getStatus: async (): Promise<VaultStatus> => {
    const response = await apiClient.get(`${BASE_PATH}/status`);
    return response.data.data;
  },

  enable: async (request: VaultEnableRequest): Promise<VaultStatus> => {
    const response = await apiClient.post(`${BASE_PATH}/enable`, request);
    return response.data.data;
  },

  disable: async (request: VaultDisableRequest): Promise<VaultStatus> => {
    const response = await apiClient.post(`${BASE_PATH}/disable`, request);
    return response.data.data;
  },
};
