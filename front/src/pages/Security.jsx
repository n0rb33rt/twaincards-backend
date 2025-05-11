import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { getUserProfile, changePassword } from '../api/userApi';
import { useAuth } from '../context/AuthContext.jsx';
import '../styles/main.css';

const Security = () => {
  const { currentUser } = useAuth();

  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });

  const [errors, setErrors] = useState({});
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    // Wait a short time to ensure loading state is visible to prevent flickering
    const timer = setTimeout(() => {
      setIsLoading(false);
    }, 300);
    
    return () => clearTimeout(timer);
  }, []);

  const validatePasswordForm = () => {
    const newErrors = {};

    if (!passwordData.currentPassword) {
      newErrors.currentPassword = 'Current password is required';
    }

    if (!passwordData.newPassword) {
      newErrors.newPassword = 'New password is required';
    } else if (passwordData.newPassword.length < 6) {
      newErrors.newPassword = 'New password must be at least 6 characters';
    }

    if (passwordData.newPassword !== passwordData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handlePasswordChange = (e) => {
    const { name, value } = e.target;
    setPasswordData({ ...passwordData, [name]: value });

    // Clear error when user starts typing
    if (errors[name]) {
      setErrors({ ...errors, [name]: null });
    }
  };

  const handlePasswordSubmit = async (e) => {
    e.preventDefault();

    if (!validatePasswordForm()) return;

    try {
      setIsSaving(true);

      await changePassword(passwordData);

      // Reset password fields
      setPasswordData({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      });

      toast.success('Password changed successfully');
    } catch (error) {
      console.error('Error changing password:', error);
      
      // Display specific error message from the server
      if (error.response?.data?.message) {
        // Set the specific error in the form
        setErrors({ 
          ...errors, 
          currentPassword: error.response.data.message 
        });
        toast.error(error.response.data.message);
      } else {
        toast.error('Failed to change password. Please try again.');
      }
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading security settings...</p>
      </div>
    );
  }

  return (
    <div className="profile-container">
      <header className="profile-header">
        <h1>Security Settings</h1>
        <p className="profile-subtitle">
          Manage your account security and password
        </p>
      </header>

      <div className="security-container">
        <div className="security-section">
          <h3 className="security-title">Change Password</h3>
          <p className="security-subtitle">Keep your account secure by updating your password regularly</p>
          
          <div className="password-form-container">
            <form className="password-form" onSubmit={handlePasswordSubmit}>
              <div className="form-group">
                <label htmlFor="currentPassword">Current Password*</label>
                <input
                  type="password"
                  id="currentPassword"
                  name="currentPassword"
                  value={passwordData.currentPassword}
                  onChange={handlePasswordChange}
                  className={errors.currentPassword ? 'input-error' : ''}
                  disabled={isSaving}
                  autoComplete="current-password"
                />
                {errors.currentPassword && <div className="error-message">{errors.currentPassword}</div>}
              </div>

              <div className="form-group">
                <label htmlFor="newPassword">New Password*</label>
                <input
                  type="password"
                  id="newPassword"
                  name="newPassword"
                  value={passwordData.newPassword}
                  onChange={handlePasswordChange}
                  className={errors.newPassword ? 'input-error' : ''}
                  disabled={isSaving}
                  autoComplete="new-password"
                />
                {errors.newPassword && <div className="error-message">{errors.newPassword}</div>}
                <div className="password-hint">Password must be at least 6 characters long</div>
              </div>

              <div className="form-group">
                <label htmlFor="confirmPassword">Confirm New Password*</label>
                <input
                  type="password"
                  id="confirmPassword"
                  name="confirmPassword"
                  value={passwordData.confirmPassword}
                  onChange={handlePasswordChange}
                  className={errors.confirmPassword ? 'input-error' : ''}
                  disabled={isSaving}
                  autoComplete="new-password"
                />
                {errors.confirmPassword && <div className="error-message">{errors.confirmPassword}</div>}
              </div>

              <div className="form-actions">
                <button
                  type="submit"
                  className="btn-primary save-button"
                  disabled={isSaving}
                >
                  {isSaving ? (
                    <>
                      <span className="spinner-small"></span> Changing Password...
                    </>
                  ) : (
                    'Change Password'
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Security; 