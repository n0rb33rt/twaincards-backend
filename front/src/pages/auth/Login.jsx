// src/pages/auth/Login.jsx
import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from '../../context/AuthContext.jsx';
import '../../styles/main.css';

const Login = () => {
  const [credentials, setCredentials] = useState({
    usernameOrEmail: '',
    password: '',
  });
  const [errors, setErrors] = useState({});
  const [isLoading, setIsLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Get the redirect URL from location state or default to dashboard
  const from = location.state?.from?.pathname || '/dashboard';

  const validateForm = () => {
    const newErrors = {};

    if (!credentials.usernameOrEmail.trim()) {
      newErrors.usernameOrEmail = 'Username or email is required';
    }

    if (!credentials.password) {
      newErrors.password = 'Password is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setCredentials({ ...credentials, [name]: value });

    // Clear error when user starts typing
    if (errors[name]) {
      setErrors({ ...errors, [name]: null });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) return;

    setIsLoading(true);

    try {
      await login(credentials.usernameOrEmail, credentials.password);
      toast.success('Login successful');
      navigate(from, { replace: true });
    } catch (error) {
      console.error('Login error:', error);
      toast.error(error.message || 'Login failed. Please check your credentials.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card login-card">
        <h1 className="auth-title">Welcome Back</h1>
        <p className="auth-subtitle">Sign in to continue to TwainCards</p>

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="usernameOrEmail">Username or Email</label>
            <input
              type="text"
              id="usernameOrEmail"
              name="usernameOrEmail"
              value={credentials.usernameOrEmail}
              onChange={handleChange}
              className={errors.usernameOrEmail ? 'input-error' : ''}
              disabled={isLoading}
              placeholder="Enter your username or email"
              style={{ fontSize: '16px', padding: '14px 16px' }}
            />
            {errors.usernameOrEmail && (
              <div className="error-message">{errors.usernameOrEmail}</div>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              type="password"
              id="password"
              name="password"
              value={credentials.password}
              onChange={handleChange}
              className={errors.password ? 'input-error' : ''}
              disabled={isLoading}
              placeholder="Enter your password"
              style={{ fontSize: '16px', padding: '14px 16px' }}
            />
            {errors.password && (
              <div className="error-message">{errors.password}</div>
            )}
            <div className="forgot-password">
              <Link to="/forgot-password" className="auth-link">Forgot Password?</Link>
            </div>
          </div>

          <div className="form-group" style={{ marginTop: '30px' }}>
            <button
              type="submit"
              className="auth-button"
              disabled={isLoading}
              style={{ padding: '16px', fontSize: '18px' }}
            >
              {isLoading ? (
                <>
                  <span className="spinner-small"></span> Signing in...
                </>
              ) : (
                'Sign In'
              )}
            </button>
          </div>
        </form>

        <div className="auth-footer">
          <p>
            Don't have an account?{' '}
            <Link to="/register" className="auth-link">
              Sign up
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;