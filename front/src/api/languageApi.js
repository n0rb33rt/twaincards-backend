// src/api/languageApi.js
import api from '../utils/axiosConfig';

export const getAllLanguages = async () => {
  const response = await api.get('/api/languages');
  return response.data;
};

export const getAllEnabledLanguages = async () => {
  const response = await api.get('/api/languages');
  return response.data;
};

export const getLanguageById = async (id) => {
  const response = await api.get(`/api/languages/${id}`);
  return response.data;
};

export const getLanguageByCode = async (code) => {
  const response = await api.get(`/api/languages/code/${code}`);
  return response.data;
};

export const searchLanguages = async (query) => {
  const response = await api.get(`/api/languages/search?query=${query}`);
  return response.data;
};

export const createLanguage = async (languageData) => {
  const response = await api.post('/api/languages', languageData);
  return response.data;
};

export const updateLanguage = async (id, languageData) => {
  const response = await api.put(`/api/languages/${id}`, languageData);
  return response.data;
};

export const enableLanguage = async (id) => {
  const response = await api.post(`/api/languages/${id}/enable`);
  return response.data;
};

export const disableLanguage = async (id) => {
  const response = await api.post(`/api/languages/${id}/disable`);
  return response.data;
};