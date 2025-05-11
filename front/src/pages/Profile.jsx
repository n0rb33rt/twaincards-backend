// src/pages/Profile.jsx
import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { getUserProfile, updateUserProfile } from '../api/userApi';
import { useAuth } from '../context/AuthContext.jsx';
import '../styles/main.css';

const Profile = () => {
  const { currentUser, refreshUserData } = useAuth();

  const [profileData, setProfileData] = useState({
    id: '',
    username: '',
    email: '',
    firstName: '',
    lastName: ''
  });

  const [errors, setErrors] = useState({});
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    const fetchProfileData = async () => {
      try {
        setIsLoading(true);

        // Fetch user profile or use current user data
        let profileData;
        try {
          profileData = await getUserProfile();
        } catch (error) {
          // If the API call fails but we have current user data, use that instead
          if (currentUser) {
            profileData = currentUser;
            // Don't show error toast if we have fallback data
          } else {
            throw error; // Re-throw if we don't have fallback data
          }
        }

        setProfileData({
          id: profileData.id,
          username: profileData.username,
          email: profileData.email,
          firstName: profileData.firstName || '',
          lastName: profileData.lastName || ''
        });
      } catch (error) {
        console.error('Error fetching profile data:', error);
        toast.error('Failed to load profile data. Please try refreshing the page.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchProfileData();
  }, [currentUser]);

  const validateProfileForm = () => {
    const newErrors = {};

    if (!profileData.email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(profileData.email)) {
      newErrors.email = 'Email is invalid';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleProfileChange = (e) => {
    const { name, value } = e.target;
    setProfileData({ ...profileData, [name]: value });

    // Clear error when user starts typing
    if (errors[name]) {
      setErrors({ ...errors, [name]: null });
    }
  };

  const handleProfileSubmit = async (e) => {
    e.preventDefault();

    if (!validateProfileForm()) return;

    try {
      setIsSaving(true);

      // Include username in the profile update request
      const updatedProfile = {
        username: profileData.username, // Add username to the request
        email: profileData.email,
        firstName: profileData.firstName,
        lastName: profileData.lastName
      };

      await updateUserProfile(updatedProfile);
      toast.success('Profile updated successfully');
      // Refresh user data in auth context after successful update
      refreshUserData();
    } catch (error) {
      console.error('Error updating profile:', error);
      if (error.response?.data?.message) {
        const errorMessage = error.response.data.message;
        
        // Check for specific error messages
        if (errorMessage.includes('Email already in use')) {
          setErrors({ ...errors, email: 'Email already in use' });
        }
        
        toast.error(errorMessage);
      } else {
        toast.error('Failed to update profile. Please try again.');
      }
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading profile...</p>
      </div>
    );
  }

  return (
    <div className="profile-container">
      <header className="profile-header">
        <h1>My Profile</h1>
        <p className="profile-subtitle">
          Manage your personal information
        </p>
      </header>

      <div className="profile-content">
        <div className="profile-form-container">
          <form className="profile-form" onSubmit={handleProfileSubmit}>
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="username">Username</label>
                <input
                  type="text"
                  id="username"
                  name="username"
                  value={profileData.username}
                  onChange={handleProfileChange}
                  className="disabled-input"
                  disabled={true}
                />
                <div className="field-hint">Username cannot be changed</div>
              </div>

              <div className="form-group">
                <label htmlFor="email">Email*</label>
                <input
                  type="email"
                  id="email"
                  name="email"
                  value={profileData.email}
                  onChange={handleProfileChange}
                  className={errors.email ? 'input-error' : ''}
                  disabled={isSaving}
                />
                {errors.email && <div className="error-message">{errors.email}</div>}
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="firstName">First Name</label>
                <input
                  type="text"
                  id="firstName"
                  name="firstName"
                  value={profileData.firstName}
                  onChange={handleProfileChange}
                  disabled={isSaving}
                />
              </div>

              <div className="form-group">
                <label htmlFor="lastName">Last Name</label>
                <input
                  type="text"
                  id="lastName"
                  name="lastName"
                  value={profileData.lastName}
                  onChange={handleProfileChange}
                  disabled={isSaving}
                />
              </div>
            </div>

            <div className="form-actions">
              <button
                type="submit"
                className="btn-primary save-button"
                disabled={isSaving}
              >
                {isSaving ? (
                  <>
                    <span className="spinner-small"></span> Saving...
                  </>
                ) : (
                  'Save Changes'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Profile;