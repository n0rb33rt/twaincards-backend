import React from 'react';

const LoadingIndicator = ({ message = 'Loading...' }) => {
  return (
    <div className="loading-container" style={styles.container}>
      <div className="spinner" style={styles.spinner}></div>
      <p style={styles.message}>{message}</p>
    </div>
  );
};

const styles = {
  container: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '2rem',
    minHeight: '200px',
  },
  spinner: {
    width: '40px',
    height: '40px',
    border: '3px solid rgba(0, 0, 0, 0.1)',
    borderTopColor: 'var(--primary-color, #3b82f6)',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },
  message: {
    marginTop: '1rem',
    color: 'var(--neutral-700, #374151)',
    fontSize: 'var(--font-size-md, 1rem)',
  },
};

// Add the animation
const styleElement = document.createElement('style');
styleElement.textContent = `
  @keyframes spin {
    to { transform: rotate(360deg); }
  }
`;
document.head.appendChild(styleElement);

export default LoadingIndicator; 