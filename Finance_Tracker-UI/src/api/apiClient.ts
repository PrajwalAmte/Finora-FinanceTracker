import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import { toast } from "../utils/notifications";

const BACKEND_URL = import.meta.env?.VITE_BACKEND_URL || "http://localhost:8082";
const API_BASE_URL = `${BACKEND_URL}/api`;

let jwtToken: string | null = null;

export async function initJwtToken() {
  try {
    const response = await axios.get(`${BACKEND_URL}/auth/token`);
    jwtToken = response.data;
  } catch (error) {
    console.error("Failed to load JWT token", error);
    toast.error("Cannot connect to backend. Start the backend server.");
  }
}

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 10000,
});

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (jwtToken) {
      config.headers.Authorization = `Bearer ${jwtToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.code === "ECONNABORTED") {
      toast.error("Request timeout - server slow.");
    } else if (!error.response) {
      toast.error("Cannot connect to backend. Is server running?");
    } else {
      const status = error.response.status;
      if (status === 401) toast.error("Unauthorized request.");
      else if (status === 403) toast.error("Forbidden.");
      else if (status === 404) toast.error("Not found.");
      else if (status === 500) toast.error("Server error - try again later.");
      else toast.error(`API Error (${status})`);
    }
    return Promise.reject(error);
  }
);

export default apiClient;
