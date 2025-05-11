// src/utils/axiosConfig.js
import axios from 'axios';
import { getToken, removeToken, saveToken, isTokenValid } from './tokenManager';
import { refreshToken as apiRefreshToken } from '../api/authApi';

// Base URL for API requests
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// Create a custom axios instance
const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Flag to track if we're currently refreshing the token
let isRefreshing = false;
// Store pending requests that should be retried after token refresh
let failedQueue = [];
// Flag to track if we're currently fetching user profile
let isFetchingUserProfile = false;

// Process the queue of failed requests
const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  
  failedQueue = [];
};

// Add a request interceptor to include the JWT token in requests
api.interceptors.request.use(
  (config) => {
    // Skip adding token for auth endpoints
    if (config.url.includes('/api/auth/login') || config.url.includes('/api/auth/register')) {
      return config;
    }
    
    // Prevent infinite loop with /users/me
    if (config.url.includes('/api/users/me')) {
      // If we're already fetching user profile, prevent the request to avoid loops
      if (isFetchingUserProfile) {
        // Cancel the request
        const cancelToken = axios.CancelToken;
        const source = cancelToken.source();
        config.cancelToken = source.token;
        source.cancel('Request cancelled to prevent infinite loop');
        return config;
      }
      isFetchingUserProfile = true;
      // Add cleanup to reset the flag
      config.headers['X-User-Profile-Request'] = 'true';
    }
    
    const token = getToken();
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add a response interceptor to handle authentication errors
api.interceptors.response.use(
  (response) => {
    // Reset user profile fetching flag if this was a user profile request
    if (response.config.headers['X-User-Profile-Request'] === 'true') {
      isFetchingUserProfile = false;
    }
    return response;
  },
  async (error) => {
    // Reset user profile fetching flag if this was a user profile request that failed
    if (error.config?.headers?.['X-User-Profile-Request'] === 'true') {
      isFetchingUserProfile = false;
    }
    
    // If request was cancelled, just return the error
    if (axios.isCancel(error)) {
      return Promise.reject(error);
    }
    
    const originalRequest = error.config;
    
    // Handle 401 errors (token expired or invalid)
    if (error.response?.status === 401 && !originalRequest._retry) {
      // Prevent retry loop
      if (originalRequest.url.includes('/api/auth/refresh-token')) {
        removeToken();
        window.location.href = '/login';
        return Promise.reject(error);
      }
      
      originalRequest._retry = true;
      
      // If already refreshing, queue this request
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(token => {
            originalRequest.headers['Authorization'] = `Bearer ${token}`;
            return api(originalRequest);
          })
          .catch(err => {
            return Promise.reject(err);
          });
      }
      
      isRefreshing = true;
      
      try {
        // Call refresh token API
        const response = await apiRefreshToken();
        const newToken = response.token;
        
        // Save the new token
        saveToken(newToken);
        
        // Update Authorization header for the original request
        originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
        
        // Process queued requests
        processQueue(null, newToken);
        
        return api(originalRequest);
      } catch (refreshError) {
        // Token refresh failed
        processQueue(refreshError, null);
        removeToken();
        
        // Redirect to login
        if (!window.location.pathname.includes('/login') && 
            !window.location.pathname.includes('/register') &&
            !window.location.pathname.includes('/confirm-email')) {
          window.location.href = '/login';
        }
        
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }
    
    // Handle 403 Forbidden (no access)
    if (error.response?.status === 403) {
      // User is authenticated but doesn't have permission
      console.error('Access denied:', error.response.data);
    }
    
    return Promise.reject(error);
  }
);

export default api;

// Create an axios instance with base configuration
export const http = axios.create({
  baseURL: API_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  }
});

// Add a request interceptor to include auth token
http.interceptors.request.use(
  (config) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add a response interceptor to handle common errors
http.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      // Handle unauthorized access
      removeToken();
      // Redirect to login page if it's not already there
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// Helper function to download a blob
export const downloadBlob = (data, filename) => {
  const url = window.URL.createObjectURL(new Blob([data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
};