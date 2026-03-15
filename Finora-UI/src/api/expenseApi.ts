import apiClient from './apiClient';
import { Expense, ExpenseSummary } from '../types/Expense';

const BASE_PATH = '/expenses';

export const expenseApi = {
  getAll: async (): Promise<Expense[]> => {
    const response = await apiClient.get(BASE_PATH);
    return response.data;
  },

  getById: async (id: number): Promise<Expense> => {
    const response = await apiClient.get(`${BASE_PATH}/${id}`);
    return response.data;
  },

  create: async (expense: Omit<Expense, 'id'>): Promise<Expense> => {
    const response = await apiClient.post(BASE_PATH, expense);
    return response.data;
  },

  update: async (id: number, expense: Partial<Expense>): Promise<Expense> => {
    const response = await apiClient.put(`${BASE_PATH}/${id}`, expense);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`${BASE_PATH}/${id}`);
  },
  
  getByDateRange: async (startDate: string, endDate: string): Promise<Expense[]> => {
    const response = await apiClient.get(`${BASE_PATH}/by-date-range`, {
      params: { startDate, endDate },
    });
    return response.data;
  },
  
  getByCategory: async (category: string): Promise<Expense[]> => {
    const response = await apiClient.get(`${BASE_PATH}/by-category`, {
      params: { category },
    });
    return response.data;
  },
  
  getSummary: async (startDate?: string, endDate?: string): Promise<ExpenseSummary> => {
    const response = await apiClient.get(`${BASE_PATH}/summary`, {
      params: { startDate, endDate },
    });
    return response.data;
  },
  
  getAverageMonthly: async (category?: string): Promise<number> => {
    const response = await apiClient.get(`${BASE_PATH}/average-monthly`, {
      params: { category },
    });
    return response.data;
  },
};