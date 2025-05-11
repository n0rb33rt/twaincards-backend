// src/api/learningApi.js
import api from '../utils/axiosConfig';

export const getCardsToLearn = async (collectionId, limit = 10) => {
  const response = await api.get(`/api/learning/cards-to-learn?collectionId=${collectionId}&limit=${limit}`);
  return response.data;
};

export const getCardsForReview = async (collectionId = null, limit = 10) => {
  if (collectionId) {
    return getCardsForReviewByCollection(collectionId, limit);
  }
  
  const response = await api.get(`/api/learning/cards-for-review?limit=${limit}`);
  return response.data;
};

export const getCardsForReviewByCollection = async (collectionId, limit = 10) => {
  const response = await api.get(`/api/learning/cards-for-review/collection/${collectionId}?limit=${limit}`);
  return response.data;
};

export const answerCard = async (cardAnswerData) => {
  /**
   * Submit an answer for a card
   * @param {Object} cardAnswerData - The answer data
   * @param {number} cardAnswerData.cardId - The ID of the card being answered
   * @param {boolean} cardAnswerData.isCorrect - Whether the answer was correct
   * @param {number} [cardAnswerData.sessionId] - The ID of the study session (optional)
   * @param {number} [cardAnswerData.responseTimeMs] - Response time in milliseconds (optional)
   * @returns {Promise<Object>} The updated learning progress
   */
  const response = await api.post('/api/learning/answer', cardAnswerData);
  return response.data;
};

export const resetCardProgress = async (cardId) => {
  const response = await api.post(`/api/learning/reset-progress/card/${cardId}`);
  return response.data;
};

export const resetCollectionProgress = async (collectionId) => {
  const response = await api.post(`/api/learning/reset-progress/collection/${collectionId}`);
  return response.data;
};

export const getStatusStatistics = async () => {
  const response = await api.get('/api/learning/status-statistics');
  return response.data;
};

// Get user learning progress (daily stats)
export const getLearningProgress = async () => {
  const response = await api.get('/api/learning/progress');
  return response.data;
};

// Submit a card review result - using the correct endpoint
export const submitCardReview = async (reviewData) => {
  const response = await api.post('/api/learning/answer', reviewData);
  return response.data;
};

// Get learning history - ensure this endpoint exists on backend
export const getLearningHistory = async (startDate, endDate) => {
  const response = await api.get(`/api/statistics/learning-history?startDate=${startDate}&endDate=${endDate}`);
  return response.data;
};

// Get learning statistics for a collection
export const getCollectionLearningStats = async (collectionId) => {
  const response = await api.get(`/api/learning/status-statistics/collection/${collectionId}`);
  return response.data;
};

// Reset learning progress for a collection - this is a duplicate of resetCollectionProgress
export const resetLearningProgress = async (collectionId) => {
  return resetCollectionProgress(collectionId);
};