// src/api/tagApi.js
import api from '../utils/axiosConfig';

export const getAllTags = async () => {
  const response = await api.get('/api/tags');
  return response.data;
};

export const getTagById = async (id) => {
  const response = await api.get(`/api/tags/${id}`);
  return response.data;
};

export const getTagByName = async (name) => {
  const response = await api.get(`/api/tags/name/${name}`);
  return response.data;
};

export const searchTags = async (query) => {
  const response = await api.get(`/api/tags/search?query=${query}`);
  return response.data;
};

export const getTagsByCollection = async (collectionId) => {
  const response = await api.get(`/api/tags/collection/${collectionId}`);
  return response.data;
};

export const getPopularTags = async (limit = 10) => {
  const response = await api.get(`/api/tags/popular?limit=${limit}`);
  return response.data;
};

export const createTag = async (tagData) => {
  const response = await api.post('/api/tags', tagData);
  return response.data;
};

export const updateTag = async (id, tagData) => {
  const response = await api.put(`/api/tags/${id}`, tagData);
  return response.data;
};

export const deleteTag = async (id) => {
  const response = await api.delete(`/api/tags/${id}`);
  return response.data;
};