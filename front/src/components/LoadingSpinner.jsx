import React from 'react';
import PropTypes from 'prop-types';

/**
 * Loading spinner component to indicate loading state
 * 
 * @param {Object} props - Component props
 * @param {string} props.message - Message to display while loading
 * @param {string} props.size - Size of the spinner (small, medium, large)
 * @param {boolean} props.fullPage - Whether to display the spinner centered on the full page
 */
const LoadingSpinner = ({ 
  message = 'Loading...', 
  size = 'medium', 
  fullPage = false 
}) => {
  const getSizeStyles = () => {
    switch (size) {
      case 'small':
        return { width: '24px', height: '24px', borderWidth: '2px' };
      case 'medium':
        return { width: '48px', height: '48px', borderWidth: '3px' };
      case 'large':
        return { width: '64px', height: '64px', borderWidth: '4px' };
      default:
        return { width: '48px', height: '48px', borderWidth: '3px' };
    }
  };

  const spinnerSize = getSizeStyles();
  
  const containerStyles = {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '2rem',
    ...(fullPage ? {
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      backgroundColor: 'rgba(255, 255, 255, 0.8)',
      zIndex: 9999,
    } : {
      minHeight: '200px',
    }),
  };
  
  const spinnerStyles = {
    ...spinnerSize,
    border: `${spinnerSize.borderWidth} solid rgba(0, 0, 0, 0.1)`,
    borderTopColor: 'var(--primary-color, #4f46e5)',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  };

  // Add the animation if it doesn't exist
  if (!document.getElementById('loading-spinner-keyframes')) {
    const styleElement = document.createElement('style');
    styleElement.id = 'loading-spinner-keyframes';
    styleElement.textContent = `
      @keyframes spin {
        to { transform: rotate(360deg); }
      }
    `;
    document.head.appendChild(styleElement);
  }

  return (
    <div className="loading-spinner-container" style={containerStyles}>
      <div className="spinner" style={spinnerStyles}></div>
      {message && (
        <p style={{
          marginTop: '1rem',
          color: 'var(--neutral-700, #374151)',
          fontSize: 'var(--font-size-md, 1rem)'
        }}>
          {message}
        </p>
      )}
    </div>
  );
};

LoadingSpinner.propTypes = {
  message: PropTypes.string,
  size: PropTypes.oneOf(['small', 'medium', 'large']),
  fullPage: PropTypes.bool,
};

export default LoadingSpinner; 