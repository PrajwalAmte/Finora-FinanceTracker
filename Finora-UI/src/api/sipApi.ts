import apiClient from './apiClient';
import { Sip, SipSummary } from '../types/Sip';

const BASE_PATH = '/sips';

export const sipApi = {
  getAll: async (): Promise<Sip[]> => {
    try {
      const response = await apiClient.get(BASE_PATH);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch SIPs:', error);
      throw error;
    }
  },

  getById: async (id: number): Promise<Sip> => {
    try {
      const response = await apiClient.get(`${BASE_PATH}/${id}`);
      return response.data;
    } catch (error) {
      console.error(`Failed to fetch SIP with ID ${id}:`, error);
      throw error;
    }
  },

  create: async (sip: Omit<Sip, 'id'>): Promise<Sip> => {
    try {
      const response = await apiClient.post(BASE_PATH, sip);
      return response.data;
    } catch (error) {
      console.error('Failed to create SIP:', error);
      throw error;
    }
  },

  update: async (id: number, sip: Partial<Sip>): Promise<Sip> => {
    try {
      const response = await apiClient.put(`${BASE_PATH}/${id}`, sip);
      return response.data;
    } catch (error) {
      console.error(`Failed to update SIP with ID ${id}:`, error);
      throw error;
    }
  },

  delete: async (id: number): Promise<void> => {
    try {
      await apiClient.delete(`${BASE_PATH}/${id}`);
    } catch (error) {
      console.error(`Failed to delete SIP with ID ${id}:`, error);
      throw error;
    }
  },

  getSummary: async (): Promise<SipSummary> => {
    try {
      const response = await apiClient.get(`${BASE_PATH}/summary`);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch SIP summary:', error);
      throw error;
    }
  }
};