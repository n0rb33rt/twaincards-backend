// src/api/authApi.js
import api from './axiosConfig';

export const login = async (loginData) => {
  const response = await api.post('/api/auth/login', loginData);
  return response.data;
};

export const register = async (registerData) => {
  const response = await api.post('/api/auth/register', registerData);
  return response.data;
};

export const refreshToken = async () => {
  const response = await api.post('/api/auth/refresh-token');
  return response.data;
};

// This endpoint might need modification based on actual backend implementation
export const confirmEmail = async (token) => {
  const response = await api.get(`/api/v1/confirm?token=${token}`);
  return response.data;
};

// The backend doesn't have a logout API endpoint, so we'll handle this client-side only
export const logout = async () => {
  // Simply return a successful response as logout is handled client-side
  return { success: true };
};

export const checkAuthStatus = async () => {
  const response = await api.get('/api/auth/status');
  return response.data;
};

// Add functions for password reset
export const requestPasswordReset = async (email) => {
  const response = await api.post('/api/auth/request-password-reset', { email });
  return response.data;
};

export const resetPassword = async (token, newPassword) => {
  const response = await api.post('/api/auth/reset-password', { token, newPassword });
  return response.data;
};