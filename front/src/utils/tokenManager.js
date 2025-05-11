// src/utils/tokenManager.js

// Token key in localStorage
const TOKEN_KEY = 'auth_token';
const EXPIRY_KEY = 'token_expiry';

/**
 * Save authentication token and its expiry time to localStorage
 * @param {string} token - JWT token
 * @param {number} expiresIn - Expiration time in seconds (default: 3600 seconds / 1 hour)
 */
export const saveToken = (token, expiresIn = 3600) => {
  if (!token) {
    return;
  }
  
  localStorage.setItem(TOKEN_KEY, token);
  
  // Calculate expiry timestamp
  const expiryDate = new Date();
  expiryDate.setSeconds(expiryDate.getSeconds() + (expiresIn || 3600));
  localStorage.setItem(EXPIRY_KEY, expiryDate.getTime().toString());
};

/**
 * Get the stored authentication token
 * @returns {string|null} The token or null if not found or expired
 */
export const getToken = () => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (!token) return null;
  
  // Check if token is expired
  if (isTokenExpired()) {
    clearToken();
    return null;
  }
  
  return token;
};

/**
 * Parse the JWT token and extract payload data
 * @returns {object|null} The parsed token payload or null if no valid token exists
 */
export const parseToken = () => {
  const token = getToken();
  if (!token) {
    return null;
  }
  
  try {
    // Get the payload part of the JWT (second part between dots)
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }
    
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    
    try {
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      
      return JSON.parse(jsonPayload);
    } catch (decodeError) {
      return null;
    }
  } catch (error) {
    return null;
  }
};

/**
 * Check if the token is expired
 * @returns {boolean} True if token is expired or expiry time is not set
 */
export const isTokenExpired = () => {
  const expiryTime = localStorage.getItem(EXPIRY_KEY);
  if (!expiryTime) return true;
  
  return new Date().getTime() > parseInt(expiryTime, 10);
};

/**
 * Check if a valid token exists (not expired)
 * @returns {boolean} True if a valid token exists
 */
export const isTokenValid = () => {
  return getToken() !== null;
};

/**
 * Clear the token and expiry time from localStorage
 */
export const clearToken = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(EXPIRY_KEY);
};

/**
 * For backward compatibility with the AuthContext that calls removeToken
 */
export const removeToken = clearToken;
