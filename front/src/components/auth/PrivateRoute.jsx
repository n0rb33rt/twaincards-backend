import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

/**
 * PrivateRoute component for role-based access control
 * Only allows access if the user is authenticated AND has the required role
 */
const PrivateRoute = ({ children, requiredRole = 'ADMIN' }) => {
  const { isAuthenticated, currentUser, loading } = useAuth();
  const location = useLocation();
  
  // Show nothing while we're checking authentication
  if (loading) {
    return <div className="text-center py-5">Loading...</div>;
  }
  
  // Check if user is authenticated
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location.pathname }} />;
  }
  
  // Check if user has the required role
  const userRole = currentUser?.role;
  const hasRequiredRole = userRole === requiredRole;
  
  if (!hasRequiredRole) {
    return <Navigate to="/dashboard" />;
  }
  
  // If all checks pass, render the protected component
  return children;
};

export default PrivateRoute; 