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
  }
};