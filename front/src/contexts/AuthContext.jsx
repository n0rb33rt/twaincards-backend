import React, { createContext, useState, useEffect, useContext } from 'react';
import { toast } from 'react-toastify';
import { login as apiLogin, register as apiRegister, logout as apiLogout } from '../api/authApi';
import { getUserProfile, changePassword } from '../api/userApi';
import { parseToken, saveToken, removeToken, isTokenValid } from '../utils/tokenManager';

// Create the Auth Context
const AuthContext = createContext();

// Custom hook to use the auth context
export const useAuth = () => useContext(AuthContext);

/**
 * Auth Provider component that manages authentication state
 */
export const AuthProvider = ({ children }) => {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Initialize auth state from stored token
  useEffect(() => {
    const initAuthState = () => {
      try {
        if (isTokenValid()) {
          const tokenData = parseToken();
          if (tokenData) {
            setCurrentUser({
              id: tokenData.userId || tokenData.sub,
              username: tokenData.username || tokenData.sub,
              email: tokenData.email,
              role: tokenData.roles || tokenData.role || 'USER',
              token: tokenData.token
            });
          } else {
            removeToken();
          }
        } else {
          removeToken();
        }
      } catch (err) {
        console.error('Error initializing auth state:', err);
        removeToken();
      } finally {
        setLoading(false);
      }
    };

    initAuthState();
  }, []);

  // Login function
  const login = async (usernameOrEmail, password) => {
    try {
      setError(null);
      const response = await apiLogin({ usernameOrEmail, password });
      
      if (response && response.token) {
        saveToken(response.token);
        
        const userData = {
          id: response.userId,
          username: response.username,
          email: response.email,
          role: response.role,
          token: response.token
        };
        
        setCurrentUser(userData);
        return userData;
      } else {
        throw new Error('Invalid response from server');
      }
    } catch (err) {
      const errorMessage = err.message || 'Login failed';
      setError(errorMessage);
      toast.error(errorMessage);
      throw err;
    }
  };

  // Register function
  const register = async (userData) => {
    try {
      setError(null);
      const response = await apiRegister(userData);
      
      if (response && response.success) {
        toast.success(response.message || 'Registration successful');
        return response;
      } else {
        throw new Error(response.message || 'Registration failed');
      }
    } catch (err) {
      const errorMessage = err.message || 'Registration failed';
      setError(errorMessage);
      toast.error(errorMessage);
      throw err;
    }
  };

  // Logout function
  const logout = async () => {
    try {
      await apiLogout();
    } catch (err) {
      console.error('Error logging out:', err);
    } finally {
      setCurrentUser(null);
      removeToken();
      toast.info('You have been logged out');
    }
  };

  // Update user profile data
  const updateUserData = async () => {
    if (!currentUser) return null;
    
    try {
      const userData = await getUserProfile();
      if (userData) {
        setCurrentUser(prev => ({
          ...prev,
          ...userData
        }));
        return userData;
      }
    } catch (err) {
      console.error('Error updating user data:', err);
      return null;
    }
  };

  // Change password
  const updatePassword = async (currentPassword, newPassword) => {
    try {
      const response = await changePassword({
        currentPassword,
        newPassword
      });
      
      if (response && response.success) {
        toast.success('Password updated successfully');
        return true;
      }
      return false;
    } catch (err) {
      toast.error(err.message || 'Failed to update password');
      return false;
    }
  };

  // Define context value
  const value = {
    currentUser,
    loading,
    error,
    token: currentUser?.token,
    isAuthenticated: !!currentUser,
    login,
    register,
    logout,
    updateUserData,
    updatePassword
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthContext; 