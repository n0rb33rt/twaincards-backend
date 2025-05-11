// src/context/AuthContext.jsx
import React, { createContext, useState, useEffect, useContext } from 'react';
import { login as apiLogin, register as apiRegister, logout as apiLogout } from '../api/authApi';
import { getUserProfile, changePassword } from '../api/userApi';
import { saveToken, getToken, clearToken as removeToken, isTokenValid, parseToken } from '../utils/tokenManager';

const AuthContext = createContext();

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [registrationMessage, setRegistrationMessage] = useState(null);
  const [initializing, setInitializing] = useState(true);

  // This function only checks token validity and sets basic user info from token
  // without making API calls
  const initializeFromToken = () => {
    try {
      if (isTokenValid()) {
        // Parse the token to get basic user info
        const tokenData = parseToken();
        if (tokenData && tokenData.userId) {
          // Just use the token data to initialize user state
          setCurrentUser({
            id: tokenData.userId,
            username: tokenData.sub,
            role: tokenData.role
          });
        } else {
          removeToken();
          setCurrentUser(null);
        }
      } else {
        removeToken();
        setCurrentUser(null);
      }
    } catch (err) {
      console.error('Token initialization failed:', err);
      removeToken();
      setCurrentUser(null);
    } finally {
      setLoading(false);
      setInitializing(false);
    }
  };

  // Separate function to fetch full user profile when needed
  const fetchUserProfile = async () => {
    // Skip if we're initializing or no user is set
    if (initializing || !currentUser) return;
    
    try {
      const userData = await getUserProfile();
      if (userData) {
        setCurrentUser(prevUser => ({
          ...prevUser,
          id: userData.id,
          username: userData.username,
          email: userData.email,
          firstName: userData.firstName,
          lastName: userData.lastName,
          role: userData.role
        }));
      }
    } catch (err) {
      console.error('Failed to fetch user profile:', err);
      // Don't clear the user here, just keep using token data
    }
  };

  // Initial setup - just check token and set minimal user data
  useEffect(() => {
    initializeFromToken();
  }, []);

  // Only fetch full profile data after initialization is complete
  // and we have a user from the token
  useEffect(() => {
    if (!initializing && currentUser && !currentUser.email) {
      fetchUserProfile();
    }
  }, [initializing, currentUser]);

  const login = async (usernameOrEmail, password) => {
    try {
      setError(null);
      const response = await apiLogin({ usernameOrEmail, password });

      const userData = {
        id: response.userId,
        username: response.username,
        email: response.email,
        role: response.role
      };

      setCurrentUser(userData);
      saveToken(response.token);
      return userData;
    } catch (err) {
      const errorMessage = err.response?.data?.message || 'Login failed';
      setError(errorMessage);
      throw new Error(errorMessage);
    }
  };

  const register = async (userData) => {
    try {
      setError(null);
      const response = await apiRegister(userData);
      
      if (response.message) {
        setRegistrationMessage(response.message);
      }
      
      return response;
    } catch (err) {
      const errorMessage = err.response?.data?.message || 'Registration failed';
      setError(errorMessage);
      throw new Error(errorMessage);
    }
  };

  const logout = async () => {
    try {
      await apiLogout();
    } catch (err) {
      console.error('Logout error:', err);
    } finally {
      setCurrentUser(null);
      removeToken();
    }
  };

  const value = {
    currentUser,
    loading,
    error,
    registrationMessage,
    setRegistrationMessage,
    isAuthenticated: !!currentUser,
    login,
    register,
    logout,
    refreshUserData: fetchUserProfile
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthContext;