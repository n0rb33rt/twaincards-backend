// src/api/collectionApi.js
import api from '../utils/axiosConfig';

// Get all collections for current user
export const getUserCollections = async (params = {}) => {
  const queryParams = new URLSearchParams();
  
  if (params.limit) queryParams.append('size', params.limit);
  if (params.page) queryParams.append('page', params.page);
  
  const query = queryParams.toString() ? `?${queryParams.toString()}` : '';
  const response = await api.get(`/api/collections/user${query}`);
  return response.data;
};

// Get paginated user collections
export const getUserCollectionsPaginated = async (page = 0, size = 10) => {
  const response = await api.get(`/api/collections/user/page?page=${page}&size=${size}`);
  return response.data;
};

// Get collection by ID
export const getCollectionById = async (id) => {
  const response = await api.get(`/api/collections/${id}`);
  return response.data;
};

// Create new collection
export const createCollection = async (collectionData) => {
  const response = await api.post('/api/collections', collectionData);
  return response.data;
};

// Update collection
export const updateCollection = async (id, collectionData) => {
  const response = await api.put(`/api/collections/${id}`, collectionData);
  return response.data;
};

// Delete collection
export const deleteCollection = async (id) => {
  const response = await api.delete(`/api/collections/${id}`);
  return response.data;
};

// Get public collections (paginated)
export const getPublicCollections = async (page = 0, size = 10) => {
  const response = await api.get(`/api/collections/public?page=${page}&size=${size}`);
  return response.data;
};

// Search public collections
export const searchPublicCollections = async (query, page = 0, size = 10) => {
  const response = await api.get(`/api/collections/public/search?query=${query}&page=${page}&size=${size}`);
  return response.data;
};

// Search user collections
export const searchUserCollections = async (query, page = 0, size = 10) => {
  const response = await api.get(`/api/collections/user/search?query=${query}&page=${page}&size=${size}`);
  return response.data;
};

// Get recent collections
export const getRecentCollections = async (limit = 5) => {
  const response = await api.get(`/api/collections/user/recent?limit=${limit}`);
  return response.data;
};

// Update collection public status
export const updateCollectionPublicStatus = async (id, isPublic) => {
  const response = await api.put(`/api/collections/${id}/public`, { isPublic });
  return response.data;
};

// Get number of users studying a collection
export const getCollectionUsersCount = async (id) => {
  const response = await api.get(`/api/collections/${id}/users-count`);
  return response.data;
};