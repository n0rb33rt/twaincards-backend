import React from 'react';

/**
 * Confirm Dialog component
 * @param {Object} props Component props
 * @param {boolean} props.isOpen - Whether the dialog is visible
 * @param {string} props.title - Dialog title
 * @param {string} props.message - Dialog message content
 * @param {string} props.confirmText - Text for confirm button
 * @param {string} props.cancelText - Text for cancel button
 * @param {Function} props.onConfirm - Function to call when confirmed
 * @param {Function} props.onCancel - Function to call when canceled
 */
const ConfirmDialog = ({
  isOpen,
  title = 'Confirm',
  message = 'Are you sure?',
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  onConfirm,
  onCancel
}) => {
  if (!isOpen) return null;
  
  // Prevent click propagation from modal content to backdrop
  const handleContentClick = (e) => {
    e.stopPropagation();
  };
  
  const styles = {
    backdrop: {
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      backgroundColor: 'rgba(0, 0, 0, 0.5)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000,
    },
    modal: {
      backgroundColor: 'white',
      borderRadius: '8px',
      padding: '24px',
      width: '100%',
      maxWidth: '400px',
      boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
    },
    header: {
      fontSize: '1.25rem',
      fontWeight: 600,
      marginBottom: '16px',
      color: 'var(--neutral-900)',
    },
    message: {
      marginBottom: '24px',
      color: 'var(--neutral-700)',
      lineHeight: 1.5,
    },
    actions: {
      display: 'flex',
      justifyContent: 'flex-end',
      gap: '12px',
    },
    cancelButton: {
      padding: '8px 16px',
      backgroundColor: 'var(--neutral-100)',
      border: '1px solid var(--neutral-300)',
      borderRadius: '4px',
      color: 'var(--neutral-700)',
      cursor: 'pointer',
      fontSize: '14px',
    },
    confirmButton: {
      padding: '8px 16px',
      backgroundColor: 'var(--danger-color, #EF4444)',
      border: 'none',
      borderRadius: '4px',
      color: 'white',
      cursor: 'pointer',
      fontSize: '14px',
    }
  };
  
  return (
    <div style={styles.backdrop} onClick={onCancel}>
      <div style={styles.modal} onClick={handleContentClick}>
        <h3 style={styles.header}>{title}</h3>
        <div style={styles.message}>{message}</div>
        <div style={styles.actions}>
          <button 
            style={styles.cancelButton} 
            onClick={onCancel}
          >
            {cancelText}
          </button>
          <button 
            style={styles.confirmButton} 
            onClick={onConfirm}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmDialog; 