import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { confirmEmail } from '../../api/authApi';
import '../../styles/main.css';

const EmailConfirmation = () => {
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState('loading'); // loading, success, error
  const navigate = useNavigate();

  useEffect(() => {
    const token = searchParams.get('token');
    if (!token) {
      setStatus('error');
      toast.error('Invalid confirmation link.');
      return;
    }

    const confirmAccount = async () => {
      try {
        await confirmEmail(token);
        setStatus('success');
        toast.success('Account activated successfully!');
      } catch (error) {
        console.error('Email confirmation error:', error);
        setStatus('error');
        toast.error('Failed to activate account. The link may be expired or invalid.');
      }
    };

    confirmAccount();
  }, [searchParams]);

  return (
    <div className="auth-container">
      <div className="auth-card confirmation-card">
        <h1 className="auth-title">
          {status === 'loading' && 'Verifying Your Email...'}
          {status === 'success' && 'Email Verified Successfully!'}
          {status === 'error' && 'Verification Failed'}
        </h1>

        <div className="confirmation-content">
          {status === 'loading' && (
            <div className="loading-spinner">
              <div className="spinner"></div>
              <p>Please wait while we verify your email address...</p>
            </div>
          )}

          {status === 'success' && (
            <div className="success-message">
              <p>Your email has been verified and your account is now active.</p>
              <p>You can now sign in to your account and start using TwainCards!</p>
              <button 
                className="auth-button" 
                onClick={() => navigate('/login')}
              >
                Go to Login
              </button>
            </div>
          )}

          {status === 'error' && (
            <div className="error-message">
              <p>We couldn't verify your email address. The verification link may have expired or is invalid.</p>
              <p>Please try to register again or contact support if the problem persists.</p>
              <div className="confirmation-buttons">
                <button 
                  className="auth-button secondary" 
                  onClick={() => navigate('/register')}
                >
                  Register Again
                </button>
                <button 
                  className="auth-button" 
                  onClick={() => navigate('/')}
                >
                  Go to Home
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default EmailConfirmation; 