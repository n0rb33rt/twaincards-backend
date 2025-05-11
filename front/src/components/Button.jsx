import React from 'react';
import PropTypes from 'prop-types';

/**
 * Reusable Button component with different variants
 * 
 * @param {Object} props - Component props
 * @param {string} props.variant - Button variant (primary, secondary, danger, etc.)
 * @param {string} props.size - Button size (small, medium, large)
 * @param {boolean} props.disabled - Whether the button is disabled
 * @param {boolean} props.fullWidth - Whether the button should take full width
 * @param {string} props.type - Button type (button, submit, reset)
 * @param {Function} props.onClick - Function to call when button is clicked
 * @param {React.ReactNode} props.children - Button content
 */
const Button = ({ 
  variant = 'primary', 
  size = 'medium',
  disabled = false,
  fullWidth = false, 
  type = 'button',
  onClick,
  children,
  ...props
}) => {
  const getVariantClasses = () => {
    switch (variant) {
      case 'primary':
        return 'bg-primary-color text-white hover:bg-primary-dark active:bg-primary-darker';
      case 'secondary':
        return 'bg-neutral-200 text-neutral-700 hover:bg-neutral-300 active:bg-neutral-400';
      case 'danger':
        return 'bg-error-color text-white hover:bg-error-dark active:bg-error-darker';
      case 'ghost':
        return 'bg-transparent text-neutral-700 hover:bg-neutral-100 active:bg-neutral-200';
      case 'outline':
        return 'bg-transparent border border-primary-color text-primary-color hover:bg-primary-ultralight';
      default:
        return 'bg-primary-color text-white hover:bg-primary-dark active:bg-primary-darker';
    }
  };

  const getSizeClasses = () => {
    switch (size) {
      case 'small':
        return 'text-sm py-1 px-3';
      case 'medium':
        return 'text-base py-2 px-4';
      case 'large':
        return 'text-lg py-3 px-6';
      default:
        return 'text-base py-2 px-4';
    }
  };

  const baseClasses = 'rounded-md font-medium transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-color';
  const widthClasses = fullWidth ? 'w-full' : '';
  const disabledClasses = disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer';
  
  const classes = `${baseClasses} ${getVariantClasses()} ${getSizeClasses()} ${widthClasses} ${disabledClasses}`;

  return (
    <button
      type={type}
      className={classes}
      disabled={disabled}
      onClick={onClick}
      {...props}
    >
      {children}
    </button>
  );
};

Button.propTypes = {
  variant: PropTypes.oneOf(['primary', 'secondary', 'danger', 'ghost', 'outline']),
  size: PropTypes.oneOf(['small', 'medium', 'large']),
  disabled: PropTypes.bool,
  fullWidth: PropTypes.bool,
  type: PropTypes.oneOf(['button', 'submit', 'reset']),
  onClick: PropTypes.func,
  children: PropTypes.node.isRequired,
};

export default Button; 