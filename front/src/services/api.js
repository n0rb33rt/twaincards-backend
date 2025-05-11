import { isTokenValid, parseToken, getToken } from '../utils/tokenManager';

// Remove the /api suffix from the base URL to prevent duplicate api/api paths
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/**
 * Get the authorization headers for API requests
 * @param {string} token - Optional token to use instead of the stored token
 * @returns {Object} The headers object with authorization if token is available
 */
const getAuthHeaders = (token) => {
  const headers = {
    'Content-Type': 'application/json',
  };

  // If a token is explicitly provided, use it
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
    return headers;
  }

  // Otherwise check if we have a token in storage (even if not validated)
  const storedToken = getToken();
  if (storedToken) {
    headers['Authorization'] = `Bearer ${storedToken}`;
    return headers;
  }

  return headers;
};

/**
 * Fetch data from the API
 * @param {string} endpoint - API endpoint to fetch from
 * @param {string} token - Optional authentication token
 * @returns {Promise<any>} The response data
 */
export const fetchData = async (endpoint, token) => {
  const url = endpoint.startsWith('http') 
    ? endpoint 
    : `${API_BASE_URL}${endpoint.startsWith('/') ? endpoint : '/' + endpoint}`;
  
  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: getAuthHeaders(token),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => null);
      throw new Error(errorData?.message || `Error: ${response.status} ${response.statusText}`);
    }

    return await response.json();
  } catch (error) {
    console.error(`Error fetching data from ${endpoint}:`, error);
    throw error;
  }
};

/**
 * Send POST request to the API
 * @param {string} endpoint - API endpoint to post to
 * @param {Object} data - Data to send in the request body
 * @param {string} token - Optional authentication token
 * @returns {Promise<any>} The response data
 */
export const postData = async (endpoint, data, token) => {
  const url = endpoint.startsWith('http') 
    ? endpoint 
    : `${API_BASE_URL}${endpoint.startsWith('/') ? endpoint : '/' + endpoint}`;
  
  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: getAuthHeaders(token),
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => null);
      throw new Error(errorData?.message || `Error: ${response.status} ${response.statusText}`);
    }

    return await response.json();
  } catch (error) {
    console.error(`Error posting data to ${endpoint}:`, error);
    throw error;
  }
};

/**
 * Send PUT request to the API
 * @param {string} endpoint - API endpoint to put to
 * @param {Object} data - Data to send in the request body
 * @param {string} token - Optional authentication token
 * @returns {Promise<any>} The response data
 */
export const putData = async (endpoint, data, token) => {
  const url = endpoint.startsWith('http') 
    ? endpoint 
    : `${API_BASE_URL}${endpoint.startsWith('/') ? endpoint : '/' + endpoint}`;
  
  try {
    const response = await fetch(url, {
      method: 'PUT',
      headers: getAuthHeaders(token),
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => null);
      throw new Error(errorData?.message || `Error: ${response.status} ${response.statusText}`);
    }

    return await response.json();
  } catch (error) {
    console.error(`Error updating data at ${endpoint}:`, error);
    throw error;
  }
};

/**
 * Send DELETE request to the API
 * @param {string} endpoint - API endpoint to delete from
 * @param {string} token - Optional authentication token
 * @returns {Promise<any>} The response data
 */
export const deleteData = async (endpoint, token) => {
  const url = endpoint.startsWith('http') 
    ? endpoint 
    : `${API_BASE_URL}${endpoint.startsWith('/') ? endpoint : '/' + endpoint}`;
  
  try {
    const response = await fetch(url, {
      method: 'DELETE',
      headers: getAuthHeaders(token),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => null);
      throw new Error(errorData?.message || `Error: ${response.status} ${response.statusText}`);
    }

    // Some DELETE endpoints may not return data
    if (response.status === 204) {
      return null;
    }

    return await response.json();
  } catch (error) {
    console.error(`Error deleting data at ${endpoint}:`, error);
    throw error;
  }
}; 