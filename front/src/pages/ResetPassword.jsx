import React, { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { resetPassword } from '../api/authApi';

const ResetPassword = () => {
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [token, setToken] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [errors, setErrors] = useState({});
  
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    // Extract token from URL query parameters
    const searchParams = new URLSearchParams(location.search);
    const tokenParam = searchParams.get('token');
    
    if (!tokenParam) {
      toast.error('Invalid or missing password reset token');
      navigate('/login');
    } else {
      setToken(tokenParam);
    }
  }, [location, navigate]);

  const validate = () => {
    const newErrors = {};
    
    if (!password) {
      newErrors.password = 'Password is required';
    } else if (password.length < 6) {
      newErrors.password = 'Password must be at least 6 characters';
    }
    
    if (password !== confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validate()) return;
    
    setIsSubmitting(true);
    
    try {
      await resetPassword(token, password);
      setSuccess(true);
      toast.success('Your password has been reset successfully');
    } catch (error) {
      console.error('Error resetting password:', error);
      toast.error(error.response?.data?.message || 'Failed to reset password. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        {success ? (
          <div className="success-message">
            <h2 className="auth-title">Password Reset Complete</h2>
            <p>Your password has been reset successfully.</p>
            <Link to="/login" className="auth-button">
              Log In with New Password
            </Link>
          </div>
        ) : (
          <>
            <h2 className="auth-title">Set New Password</h2>
            <p className="auth-subtitle">Enter your new password below.</p>
            
            <form className="auth-form" onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="password">New Password</label>
                <input
                  type="password"
                  id="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className={errors.password ? 'input-error' : ''}
                  disabled={isSubmitting}
                  autoComplete="new-password"
                />
                {errors.password && <div className="error-message">{errors.password}</div>}
                <div className="password-hint">Password must be at least 6 characters long</div>
              </div>
              
              <div className="form-group">
                <label htmlFor="confirmPassword">Confirm New Password</label>
                <input
                  type="password"
                  id="confirmPassword"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className={errors.confirmPassword ? 'input-error' : ''}
                  disabled={isSubmitting}
                  autoComplete="new-password"
                />
                {errors.confirmPassword && <div className="error-message">{errors.confirmPassword}</div>}
              </div>
              
              <button
                type="submit"
                className="auth-button"
                disabled={isSubmitting}
              >
                {isSubmitting ? (
                  <>
                    <span className="spinner-small"></span> Resetting Password...
                  </>
                ) : (
                  'Reset Password'
                )}
              </button>
            </form>
            
            <div className="auth-footer">
              <p>Remember your password? <Link to="/login" className="auth-link">Log In</Link></p>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default ResetPassword; 