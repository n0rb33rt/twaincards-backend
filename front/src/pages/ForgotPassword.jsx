import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { requestPasswordReset } from '../api/authApi';

const ForgotPassword = () => {
  const [email, setEmail] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [errors, setErrors] = useState({});

  const validate = () => {
    const newErrors = {};
    
    if (!email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(email)) {
      newErrors.email = 'Email is invalid';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validate()) return;
    
    setIsSubmitting(true);
    
    try {
      await requestPasswordReset(email);
      setSuccess(true);
      toast.success('Password reset instructions have been sent to your email');
    } catch (error) {
      console.error('Error requesting password reset:', error);
      toast.error(error.response?.data?.message || 'Failed to request password reset. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        {success ? (
          <div className="success-message">
            <h2 className="auth-title">Check Your Email</h2>
            <p>We've sent password reset instructions to <strong>{email}</strong></p>
            <p>If you don't see the email, check your spam folder or make sure the email address is correct.</p>
            <Link to="/login" className="auth-button">
              Return to Login
            </Link>
          </div>
        ) : (
          <>
            <h2 className="auth-title">Reset Your Password</h2>
            <p className="auth-subtitle">Enter your email address and we'll send you instructions to reset your password.</p>
            
            <form className="auth-form" onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="email">Email Address</label>
                <input
                  type="email"
                  id="email"
                  name="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className={errors.email ? 'input-error' : ''}
                  disabled={isSubmitting}
                  autoComplete="email"
                />
                {errors.email && <div className="error-message">{errors.email}</div>}
              </div>
              
              <button
                type="submit"
                className="auth-button"
                disabled={isSubmitting}
              >
                {isSubmitting ? (
                  <>
                    <span className="spinner-small"></span> Sending...
                  </>
                ) : (
                  'Send Reset Instructions'
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

export default ForgotPassword; 