import axios, { AxiosError } from 'axios';
import { toast } from '../utils/notifications';

// Try to get the API base URL from environment variables, otherwise use a default
const API_BASE_URL = import.meta.env?.VITE_API_BASE_URL || 'http://localhost:8082/api';

// Create axios instance with common configuration
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000, // 10 seconds timeout
});

// Add request interceptor
apiClient.interceptors.request.use(
  (config) => {
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error: AxiosError) => {
    if (error.code === 'ECONNABORTED') {
      console.error('Request timeout - server took too long to respond');
      toast.error('Request timeout - server took too long to respond');
    } else if (!error.response) {
      console.error('Network error - cannot connect to the server. Please check if the server is running.');
      toast.error('Cannot connect to server. Is it running?');
    } else {
      const status = error.response.status;
      
      // Handle specific HTTP status codes
      if (status === 401) {
        console.error('Unauthorized - please log in again');
        toast.error('Unauthorized - please log in again');
      } else if (status === 403) {
        console.error('Forbidden - you do not have permission to access this resource');
        toast.error('Forbidden - insufficient permissions');
      } else if (status === 404) {
        console.error('Resource not found');
        toast.error('Resource not found');
      } else if (status === 500) {
        console.error('Server error - please try again later');
        toast.error('Server error - please try again later');
      } else {
        console.error(`API Error (${status}):`, error.response?.data || error.message);
        toast.error(`API Error (${status})`);
      }
    }
    
    return Promise.reject(error);
  }
);

export default apiClient;