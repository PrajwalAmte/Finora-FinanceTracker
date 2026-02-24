import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import { toast } from "../utils/notifications";
import { TOKEN_KEY } from "../utils/auth-context";

const BACKEND_URL = import.meta.env?.VITE_BACKEND_URL || "http://localhost:8082";
const API_BASE_URL = `${BACKEND_URL}/api`;

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 10000,
});

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
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
      if (status === 401) {
        // Clear stale session and redirect to login
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem("auth_user");
        if (!window.location.pathname.startsWith("/login")) {
          window.location.href = "/login";
        }
      } else if (status === 403) {
        toast.error("Forbidden.");
      } else if (status === 404) {
        toast.error("Not found.");
      } else if (status === 500) {
        toast.error("Server error - try again later.");
      } else if (status !== 400) {
        // 400 errors have field-level messages handled by the caller
        toast.error(`API Error (${status})`);
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
