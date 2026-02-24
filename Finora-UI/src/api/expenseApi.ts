import apiClient from './apiClient';
import { Expense, ExpenseSummary } from '../types/Expense';

const BASE_PATH = '/expenses';

export const expenseApi = {
  getAll: async (): Promise<Expense[]> => {
    try {
      const response = await apiClient.get(BASE_PATH);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch expenses:', error);
      throw error;
    }
  },

  getById: async (id: number): Promise<Expense> => {
    try {
      const response = await apiClient.get(`${BASE_PATH}/${id}`);
      return response.data;
    } catch (error) {
      console.error(`Failed to fetch expense with ID ${id}:`, error);
      throw error;
    }
  },

  create: async (expense: Omit<Expense, 'id'>): Promise<Expense> => {
    try {
      const response = await apiClient.post(BASE_PATH, expense);
      return response.data;
    } catch (error) {
      console.error('Failed to create expense:', error);
      throw error;
    }
  },

  update: async (id: number, expense: Partial<Expense>): Promise<Expense> => {
    try {
      const response = await apiClient.put(`${BASE_PATH}/${id}`, expense);
      return response.data;
    } catch (error) {
      console.error(`Failed to update expense with ID ${id}:`, error);
      throw error;
    }
  },

  delete: async (id: number): Promise<void> => {
    try {
      await apiClient.delete(`${BASE_PATH}/${id}`);
    } catch (error) {
      console.error(`Failed to delete expense with ID ${id}:`, error);
      throw error;
    }
  },
  
  getByDateRange: async (startDate: string, endDate: string): Promise<Expense[]> => {
    try {
      const response = await apiClient.get(`${BASE_PATH}/by-date-range`, {
        params: { startDate, endDate },
      });
      return response.data;
    } catch (error) {
      console.error(`Failed to fetch expenses between ${startDate} and ${endDate}:`, error);
      throw error;
    }
  },
  
  getByCategory: async (category: string): Promise<Expense[]> => {
    try {
      const response = await apiClient.get(`${BASE_PATH}/by-category`, {
        params: { category },
      });
      return response.data;
    } catch (error) {
      console.error(`Failed to fetch expenses for category ${category}:`, error);
      throw error;
    }
  },
  
  getSummary: async (startDate?: string, endDate?: string): Promise<ExpenseSummary> => {
    try {
      const response = await apiClient.get(`${BASE_PATH}/summary`, {
        params: { startDate, endDate },
      });
      return response.data;
    } catch (error) {
      console.error('Failed to fetch expense summary:', error);
      throw error;
    }
  },
  
  getAverageMonthly: async (category?: string): Promise<number> => {
    try {
      const response = await apiClient.get(`${BASE_PATH}/average-monthly`, {
        params: { category },
      });
      return response.data;
    } catch (error) {
      console.error('Failed to fetch average monthly expenses:', error);
      throw error;
    }
  }
};