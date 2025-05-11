// src/api/userApi.js
import api from './axiosConfig';

export const getUserProfile = async () => {
  const response = await api.get('/api/users/me');
  return response.data;
};

export const getUserById = async (id) => {
  const response = await api.get(`/api/users/${id}`);
  return response.data;
};

export const updateUserProfile = async (userData) => {
  const response = await api.put('/api/users/me', userData);
  return response.data;
};

export const changePassword = async (passwordData) => {
  // Use /users/change-password endpoint (from backend controller)
  const response = await api.post('/api/users/change-password', passwordData);
  return response.data;
};

export const getAllUsers = async () => {
  const response = await api.get('/api/users');
  return response.data;
};