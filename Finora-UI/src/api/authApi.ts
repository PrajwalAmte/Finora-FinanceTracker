import apiClient from './apiClient';
import { AuthResponse, UserProfile } from '../types/User';

export async function loginApi(email: string, password: string): Promise<AuthResponse> {
  const response = await apiClient.post('/auth/login', { email, password });
  return response.data.data;
}

export async function updateProfileApi(username: string): Promise<UserProfile> {
  const response = await apiClient.patch('/users/me', { username });
  return response.data.data;
}

export async function registerApi(
  username: string,
  email: string,
  password: string
): Promise<AuthResponse> {
  const response = await apiClient.post('/auth/register', { username, email, password });
  return response.data.data;
}

export async function getMeApi(): Promise<UserProfile> {
  const response = await apiClient.get('/users/me');
  return response.data.data;
}
