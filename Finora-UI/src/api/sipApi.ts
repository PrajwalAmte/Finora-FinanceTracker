import apiClient from './apiClient';
import { Sip, SipSummary } from '../types/Sip';

const BASE_PATH = '/sips';

export const sipApi = {
  getAll: async (): Promise<Sip[]> => {
    const response = await apiClient.get(BASE_PATH);
    return response.data;
  },

  getById: async (id: number): Promise<Sip> => {
    const response = await apiClient.get(`${BASE_PATH}/${id}`);
    return response.data;
  },

  create: async (sip: Omit<Sip, 'id'>): Promise<Sip> => {
    const response = await apiClient.post(BASE_PATH, sip);
    return response.data;
  },

  update: async (id: number, sip: Partial<Sip>): Promise<Sip> => {
    const response = await apiClient.put(`${BASE_PATH}/${id}`, sip);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`${BASE_PATH}/${id}`);
  },

  getSummary: async (): Promise<SipSummary> => {
    const response = await apiClient.get(`${BASE_PATH}/summary`);
    return response.data;
  },

  pay: async (id: number): Promise<Sip> => {
    const response = await apiClient.post(`${BASE_PATH}/${id}/pay`);
    return response.data;
  },

  refreshNavs: async (): Promise<void> => {
    await apiClient.post('/investments/refresh-prices');
  },
};