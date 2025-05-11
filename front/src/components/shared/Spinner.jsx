import React from 'react';

/**
 * Simple loading spinner component
 * @param {Object} props Component props
 * @param {string} [props.size='medium'] - Size of the spinner (small, medium, large)
 * @param {string} [props.color] - Custom color for the spinner
 * @param {string} [props.message] - Optional loading message
 */
const Spinner = ({ size = 'medium', color, message }) => {
  // Determine size
  const sizeMap = {
    small: { width: '20px', height: '20px', border: '2px' },
    medium: { width: '40px', height: '40px', border: '3px' },
    large: { width: '60px', height: '60px', border: '4px' }
  };
  
  const { width, height, border } = sizeMap[size] || sizeMap.medium;
  const spinnerColor = color || 'var(--primary-color, #3b82f6)';
  
  const spinnerStyle = {
    width,
    height,
    border: `${border} solid rgba(0, 0, 0, 0.1)`,
    borderTopColor: spinnerColor,
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  };
  
  const containerStyle = {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '1rem',
  };
  
  const messageStyle = {
    marginTop: '0.75rem',
    color: 'var(--neutral-700, #374151)',
    fontSize: 'var(--font-size-sm, 0.875rem)',
  };
  
  // Add the animation if it doesn't exist
  React.useEffect(() => {
    if (!document.getElementById('spinner-animation')) {
      const styleElement = document.createElement('style');
      styleElement.id = 'spinner-animation';
      styleElement.textContent = `
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      `;
      document.head.appendChild(styleElement);
      
      return () => {
        const element = document.getElementById('spinner-animation');
        if (element) element.remove();
      };
    }
  }, []);
  
  return (
    <div style={containerStyle}>
      <div style={spinnerStyle}></div>
      {message && <p style={messageStyle}>{message}</p>}
    </div>
  );
};

export default Spinner; 