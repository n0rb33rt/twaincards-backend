// src/api/statisticsApi.js
import api from '../utils/axiosConfig';

// Get user statistics
export const getUserStatistics = async () => {
  const response = await api.get('/api/statistics/user');
  return response.data;
};

// Get detailed user statistics
export const getDetailedUserStatistics = async () => {
  const response = await api.get('/api/statistics/user/detailed');
  return response.data;
};

// Get daily activity statistics (for time period)
export const getDailyActivityStatistics = async (startDate, endDate) => {
  const response = await api.get(`/api/statistics/daily-activity?startDate=${startDate}&endDate=${endDate}`);
  return response.data;
};

// Get learning progress by collection
export const getLearningProgressByCollection = async () => {
  const response = await api.get('/api/statistics/collections-progress');
  return response.data;
};

// Get statistics for a specific collection
export const getCollectionStatistics = async (collectionId) => {
  const response = await api.get(`/api/statistics/collections/${collectionId}`);
  return response.data;
};

// Get learning progress over time
export const getLearningProgressOverTime = async (startDate, endDate, interval = 'day') => {
  const response = await api.get(`/api/statistics/progress-over-time?startDate=${startDate}&endDate=${endDate}&interval=${interval}`);
  return response.data;
};

// Get statistics by language
export const getLanguageStatistics = async () => {
  const response = await api.get('/api/statistics/languages');
  return response.data;
};

export const getActivityStatistics = async (days = 30) => {
  const response = await api.get(`/api/statistics/activity?days=${days}`);
  return response.data;
};

export const getSummaryStatistics = async (days = 30) => {
  const response = await api.get(`/api/statistics/summary?days=${days}`);
  return response.data;
};

export const getTopUsersByLearnedCards = async (limit = 10) => {
  const response = await api.get(`/api/statistics/top-users/learned-cards?limit=${limit}`);
  return response.data;
};

// NOTE: The backend endpoint for getting top users by learning streak no longer exists
// Use getTopUsersByLearnedCards instead
export const getTopUsers = async (limit = 10) => {
  return getTopUsersByLearnedCards(limit);
};

