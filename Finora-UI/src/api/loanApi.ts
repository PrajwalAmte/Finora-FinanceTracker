import apiClient from './apiClient';
import { Loan, LoanSummary } from '../types/Loan';

const BASE_PATH = '/loans';

export const loanApi = {
  getAll: async (): Promise<Loan[]> => {
    const response = await apiClient.get(BASE_PATH);
    return response.data;
  },

  getById: async (id: number): Promise<Loan> => {
    const response = await apiClient.get(`${BASE_PATH}/${id}`);
    return response.data;
  },

  create: async (loan: Omit<Loan, 'id'>): Promise<Loan> => {
    const response = await apiClient.post(BASE_PATH, loan);
    return response.data;
  },

  update: async (id: number, loan: Partial<Loan>): Promise<Loan> => {
    const response = await apiClient.put(`${BASE_PATH}/${id}`, loan);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`${BASE_PATH}/${id}`);
  },

  getSummary: async (): Promise<LoanSummary> => {
    const response = await apiClient.get(`${BASE_PATH}/summary`);
    return response.data;
  }
};