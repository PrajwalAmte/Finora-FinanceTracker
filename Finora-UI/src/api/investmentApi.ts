import apiClient from './apiClient';
import { Investment, InvestmentSummary } from '../types/Investment';

const BASE_PATH = '/investments';

export const investmentApi = {
  getAll: async (): Promise<Investment[]> => {
    const response = await apiClient.get(BASE_PATH);
    return response.data;
  },

  getById: async (id: number): Promise<Investment> => {
    const response = await apiClient.get(`${BASE_PATH}/${id}`);
    return response.data;
  },

  create: async (investment: Omit<Investment, 'id'>): Promise<Investment> => {
    const response = await apiClient.post(BASE_PATH, investment);
    return response.data;
  },

  update: async (id: number, investment: Partial<Investment>): Promise<Investment> => {
    const response = await apiClient.put(`${BASE_PATH}/${id}`, investment);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`${BASE_PATH}/${id}`);
  },

  getSummary: async (): Promise<InvestmentSummary> => {
    const response = await apiClient.get(`${BASE_PATH}/summary`);
    return response.data;
  },

  refreshPrices: async (): Promise<void> => {
    await apiClient.post(`${BASE_PATH}/refresh-prices`);
  },

  searchMf: async (q: string): Promise<{ schemeCode: string; name: string; nav: number }[]> => {
    try {
      const response = await apiClient.get(`${BASE_PATH}/search-mf`, { params: { q } });
      return response.data;
    } catch {
      return [];
    }
  },

  addUnits: async (id: number, quantity: number, price: number): Promise<Investment> => {
    const response = await apiClient.post(`${BASE_PATH}/${id}/add-units`, { quantity, price });
    return response.data;
  },

  sellUnits: async (id: number, quantity: number, price: number): Promise<Investment | null> => {
    const response = await apiClient.post(`${BASE_PATH}/${id}/sell-units`, { quantity, price });
    return response.status === 204 ? null : response.data;
  },
};