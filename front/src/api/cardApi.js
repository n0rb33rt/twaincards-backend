// src/api/cardApi.js
import api from '../utils/axiosConfig';

export const getCardsByCollection = async (collectionId, page = 0, size = 20) => {
  const response = await api.get(`/api/cards/collection/${collectionId}/page?page=${page}&size=${size}`);
  return response.data;
};

export const getCardById = async (id) => {
  const response = await api.get(`/api/cards/${id}`);
  return response.data;
};

export const createCard = async (cardData) => {
  const response = await api.post('/api/cards', cardData);
  return response.data;
};

export const updateCard = async (id, cardData) => {
  const response = await api.put(`/api/cards/${id}`, cardData);
  return response.data;
};

export const deleteCard = async (id) => {
  const response = await api.delete(`/api/cards/${id}`);
  return response.data;
};

export const searchCards = async (collectionId, query) => {
  const response = await api.get(`/api/cards/search?collectionId=${collectionId}&query=${query}`);
  return response.data;
};

export const getCardsByTag = async (collectionId, tagName) => {
  const response = await api.get(`/api/cards/by-tag?collectionId=${collectionId}&tagName=${tagName}`);
  return response.data;
};

export const addTagsToCard = async (cardId, tagNames) => {
  const response = await api.post(`/api/cards/${cardId}/tags`, tagNames);
  return response.data;
};

export const removeTagFromCard = async (cardId, tagName) => {
  const response = await api.delete(`/api/cards/${cardId}/tags/${tagName}`);
  return response.data;
};