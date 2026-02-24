import apiClient from './apiClient';
import { Loan, LoanSummary } from '../types/Loan';

const BASE_PATH = '/loans';

export const loanApi = {
  getAll: async (): Promise<Loan[]> => {
    try {
      const response = await apiClient.get(BASE_PATH);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch loans:', error);
      throw error;
    }
  },

  getById: async (id: number): Promise<Loan> => {
    try {
      const response = await apiClient.get(`${BASE_PATH}/${id}`);
      return response.data;
    } catch (error) {
      console.error(`Failed to fetch loan with ID ${id}:`, error);
      throw error;
    }
  },

  create: async (loan: Omit<Loan, 'id'>): Promise<Loan> => {
    try {
      const response = await apiClient.post(BASE_PATH, loan);
      return response.data;
    } catch (error) {
      console.error('Failed to create loan:', error);
      throw error;
    }
  },

  update: async (id: number, loan: Partial<Loan>): Promise<Loan> => {
    try {
      const response = await apiClient.put(`${BASE_PATH}/${id}`, loan);
      return response.data;
    } catch (error) {
      console.error(`Failed to update loan with ID ${id}:`, error);
      throw error;
    }
  },

  delete: async (id: number): Promise<void> => {
    try {
      await apiClient.delete(`${BASE_PATH}/${id}`);
    } catch (error) {
      console.error(`Failed to delete loan with ID ${id}:`, error);
      throw error;
    }
  },

  getSummary: async (): Promise<LoanSummary> => {
    try {
      const response = await apiClient.get(`${BASE_PATH}/summary`);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch loan summary:', error);
      throw error;
    }
  }
};