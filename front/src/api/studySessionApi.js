import api from '../utils/axiosConfig';

/**
 * Create a new study session
 * @param {object} createRequest - Session creation request
 * @param {number} createRequest.collectionId - ID of the collection to study
 * @param {string} [createRequest.deviceType] - Device type
 * @param {string} [createRequest.platform] - Platform info
 * @returns {Promise<object>} Created session data
 */
export const createSession = async (createRequest) => {
  try {
    const response = await api.post('/api/study-sessions', createRequest);
    return response.data;
  } catch (error) {
    console.error('Error creating study session:', error);
    throw error;
  }
};

/**
 * Get session by ID
 * @param {number} sessionId - Session ID
 * @returns {Promise<object>} Session data
 */
export const getSessionById = async (sessionId) => {
  try {
    const response = await api.get(`/api/study-sessions/${sessionId}`);
    return response.data;
  } catch (error) {
    console.error('Error fetching study session:', error);
    throw error;
  }
};

/**
 * Complete a study session
 * @param {object} completeRequest - Session completion request
 * @param {number} completeRequest.sessionId - Session ID
 * @param {number} [completeRequest.cardsReviewed] - Number of cards reviewed (optional)
 * @param {number} [completeRequest.correctAnswers] - Number of correct answers (optional)
 * @returns {Promise<object>} Session summary with timeSpentSeconds
 */
export const completeSession = async (completeRequest) => {
  try {
    console.log("API: About to complete session with data:", completeRequest);
    const response = await api.post('/api/study-sessions/complete', completeRequest);
    console.log("API: Completed session successfully, received:", response.data);
    return response.data;
  } catch (error) {
    console.error('Error completing study session:', error);
    console.error('API Error details:', error.response?.data || error.message);
    throw error;
  }
};

/**
 * Get user's session summaries
 * @param {number} page - Page number
 * @param {number} size - Page size
 * @returns {Promise<object>} Page of session summaries
 */
export const getUserSessionSummaries = async (page = 0, size = 10) => {
  try {
    const response = await api.get('/api/study-sessions/user/summaries', {
      params: { page, size }
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching session summaries:', error);
    throw error;
  }
};

/**
 * Get recent sessions for a collection
 * @param {number} collectionId - Collection ID
 * @param {number} limit - Maximum number of results
 * @returns {Promise<Array>} List of session summaries
 */
export const getSessionsForCollection = async (collectionId, limit = 5) => {
  try {
    const response = await api.get(`/api/study-sessions/collection/${collectionId}`, {
      params: { limit }
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching collection sessions:', error);
    throw error;
  }
};

/**
 * Get user's study statistics
 * @param {number} days - Number of days to include in stats
 * @returns {Promise<object>} Study session statistics
 */
export const getStudyStats = async (days = 30) => {
  try {
    const response = await api.get('/api/study-sessions/stats', {
      params: { days }
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching study stats:', error);
    throw error;
  }
}; 