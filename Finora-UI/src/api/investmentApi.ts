import apiClient from './apiClient';
import { Investment, InvestmentSummary } from '../types/Investment';

const BASE_PATH = '/investments';

export const investmentApi = {
  getAll: async (): Promise<Investment[]> => {
    try {
      const response = await apiClient.get(BASE_PATH);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch investments:', error);
      throw error;
    }
  },

  getById: async (id: number): Promise<Investment> => {
    try {
      const response = await apiClient.get(`${BASE_PATH}/${id}`);
      return response.data;
    } catch (error) {
      console.error(`Failed to fetch investment with ID ${id}:`, error);
      throw error;
    }
  },

  create: async (investment: Omit<Investment, 'id'>): Promise<Investment> => {
    try {
      const response = await apiClient.post(BASE_PATH, investment);
      return response.data;
    } catch (error) {
      console.error('Failed to create investment:', error);
      throw error;
    }
  },

  update: async (id: number, investment: Partial<Investment>): Promise<Investment> => {
    try {
      const response = await apiClient.put(`${BASE_PATH}/${id}`, investment);
      return response.data;
    } catch (error) {
      console.error(`Failed to update investment with ID ${id}:`, error);
      throw error;
    }
  },

  delete: async (id: number): Promise<void> => {
    try {
      await apiClient.delete(`${BASE_PATH}/${id}`);
    } catch (error) {
      console.error(`Failed to delete investment with ID ${id}:`, error);
      throw error;
    }
  },

  getSummary: async (): Promise<InvestmentSummary> => {
    try {
      const response = await apiClient.get(`${BASE_PATH}/summary`);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch investment summary:', error);
      throw error;
    }
  },

  refreshPrices: async (): Promise<void> => {
    try {
      // Backend returns 202 Accepted immediately; refresh runs in background.
      await apiClient.post(`${BASE_PATH}/refresh-prices`);
    } catch (error) {
      console.error('Failed to trigger price refresh:', error);
      throw error;
    }
  },

  searchMf: async (q: string): Promise<{ schemeCode: string; name: string; nav: number }[]> => {
    try {
      const response = await apiClient.get(`${BASE_PATH}/search-mf`, { params: { q } });
      return response.data;
    } catch (error) {
      console.error('Failed to search mutual funds:', error);
      return [];
    }
  },

  /**
   * Add units to an existing investment.
   * Returns the updated investment with recalculated avg buy price.
   */
  addUnits: async (id: number, quantity: number, price: number): Promise<Investment> => {
    try {
      const response = await apiClient.post(`${BASE_PATH}/${id}/add-units`, { quantity, price });
      return response.data;
    } catch (error) {
      console.error(`Failed to add units to investment ${id}:`, error);
      throw error;
    }
  },

  /**
   * Sell units from an existing investment.
   * Returns the updated investment if partially sold, or null if all units were sold (investment deleted).
   */
  sellUnits: async (id: number, quantity: number, price: number): Promise<Investment | null> => {
    try {
      const response = await apiClient.post(`${BASE_PATH}/${id}/sell-units`, { quantity, price });
      // 204 No Content = all units sold, investment deleted
      return response.status === 204 ? null : response.data;
    } catch (error) {
      console.error(`Failed to sell units from investment ${id}:`, error);
      throw error;
    }
  },
};