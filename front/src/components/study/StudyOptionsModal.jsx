import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

/**
 * Study Options Modal component
 * @param {Object} props Component props
 * @param {boolean} props.isOpen - Whether the modal is visible
 * @param {Function} props.onClose - Function to call when modal is closed
 * @param {string} props.collectionId - ID of the collection to study
 * @param {string} props.collectionName - Name of the collection
 * @param {number} props.cardCount - Number of cards in the collection
 */
const StudyOptionsModal = ({
  isOpen,
  onClose,
  collectionId,
  collectionName,
  cardCount
}) => {
  const navigate = useNavigate();
  const [showSideFirst, setShowSideFirst] = useState('front');
  const [isNavigating, setIsNavigating] = useState(false);
  
  if (!isOpen) return null;
  
  // Prevent click propagation from modal content to backdrop
  const handleContentClick = (e) => {
    e.stopPropagation();
  };
  
  const startStudySession = () => {
    // Prevent double navigation
    if (isNavigating) return;
    
    setIsNavigating(true);
    
    // Build the query parameters for the study session
    const queryParams = new URLSearchParams({
      startSide: showSideFirst
    }).toString();
    
    // Close modal first to prevent any React state updates after navigation
    onClose();
    
    // Navigate to the study session page with the options
    navigate(`/study/${collectionId}?${queryParams}`);
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
      maxWidth: '500px',
      boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
    },
    header: {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      marginBottom: '20px',
    },
    title: {
      fontSize: '1.25rem',
      fontWeight: 600,
      color: 'var(--neutral-900)',
      margin: 0,
    },
    closeButton: {
      background: 'none',
      border: 'none',
      fontSize: '1.5rem',
      cursor: 'pointer',
      color: 'var(--neutral-500)',
    },
    subtitle: {
      color: 'var(--neutral-600)',
      marginBottom: '24px',
    },
    formGroup: {
      marginBottom: '20px',
    },
    label: {
      display: 'block',
      marginBottom: '8px',
      fontWeight: 500,
      color: 'var(--neutral-700)',
    },
    select: {
      width: '100%',
      padding: '10px',
      border: '1px solid var(--neutral-300)',
      borderRadius: '4px',
      fontSize: '14px',
    },
    checkbox: {
      display: 'flex',
      alignItems: 'center',
      marginBottom: '10px',
    },
    checkboxInput: {
      marginRight: '8px',
    },
    radioGroup: {
      marginBottom: '10px',
    },
    radio: {
      display: 'flex',
      alignItems: 'center',
      marginBottom: '8px',
    },
    radioInput: {
      marginRight: '8px',
    },
    actions: {
      display: 'flex',
      justifyContent: 'flex-end',
      gap: '12px',
      marginTop: '24px',
    },
    cancelButton: {
      padding: '10px 16px',
      backgroundColor: 'var(--neutral-100)',
      border: '1px solid var(--neutral-300)',
      borderRadius: '4px',
      color: 'var(--neutral-700)',
      cursor: 'pointer',
      fontSize: '14px',
    },
    startButton: {
      padding: '10px 16px',
      backgroundColor: 'var(--primary-color, #3b82f6)',
      border: 'none',
      borderRadius: '4px',
      color: 'white',
      cursor: 'pointer',
      fontSize: '14px',
    }
  };
  
  return (
    <div style={styles.backdrop} onClick={onClose}>
      <div style={styles.modal} onClick={handleContentClick}>
        <div style={styles.header}>
          <h3 style={styles.title}>Study Options</h3>
          <button style={styles.closeButton} onClick={onClose}>&times;</button>
        </div>
        
        <p style={styles.subtitle}>
          Collection: <strong>{collectionName}</strong> ({cardCount} cards)
        </p>
        
        <div style={styles.formGroup}>
          <label style={styles.label}>Card Side to Show First</label>
          
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            gap: '10px',
            marginBottom: '10px'
          }}>
            <div style={{
              flex: 1,
              padding: '12px',
              backgroundColor: 'var(--neutral-50, #f9fafb)',
              borderRadius: '4px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              border: showSideFirst === 'front' ? '2px solid var(--primary-color, #3b82f6)' : '1px solid var(--neutral-200, #e5e7eb)'
            }}>
              <label 
                htmlFor="showFrontFirst" 
                style={{ 
                  marginBottom: '8px',
                  cursor: 'pointer',
                  fontWeight: showSideFirst === 'front' ? '600' : '400'
                }}
              >
                Front Side (Term)
              </label>
              <input 
                type="checkbox" 
                id="showFrontFirst" 
                name="showSideFirst"
                value="front"
                checked={showSideFirst === 'front'}
                onChange={() => setShowSideFirst('front')}
                style={{ cursor: 'pointer' }}
              />
            </div>
            
            <div style={{
              flex: 1,
              padding: '12px',
              backgroundColor: 'var(--neutral-50, #f9fafb)',
              borderRadius: '4px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              border: showSideFirst === 'back' ? '2px solid var(--primary-color, #3b82f6)' : '1px solid var(--neutral-200, #e5e7eb)'
            }}>
              <label 
                htmlFor="showBackFirst" 
                style={{ 
                  marginBottom: '8px',
                  cursor: 'pointer',
                  fontWeight: showSideFirst === 'back' ? '600' : '400'
                }}
              >
                Back Side (Definition)
              </label>
              <input 
                type="checkbox" 
                id="showBackFirst" 
                name="showSideFirst"
                value="back"
                checked={showSideFirst === 'back'}
                onChange={() => setShowSideFirst('back')}
                style={{ cursor: 'pointer' }}
              />
            </div>
            
            <div style={{
              flex: 1,
              padding: '12px',
              backgroundColor: 'var(--neutral-50, #f9fafb)',
              borderRadius: '4px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              border: showSideFirst === 'random' ? '2px solid var(--primary-color, #3b82f6)' : '1px solid var(--neutral-200, #e5e7eb)'
            }}>
              <label 
                htmlFor="showRandom" 
                style={{ 
                  marginBottom: '8px',
                  cursor: 'pointer',
                  fontWeight: showSideFirst === 'random' ? '600' : '400',
                  textAlign: 'center'
                }}
              >
                Random (Mix of front and back)
              </label>
              <input 
                type="checkbox" 
                id="showRandom" 
                name="showSideFirst"
                value="random"
                checked={showSideFirst === 'random'}
                onChange={() => setShowSideFirst('random')}
                style={{ cursor: 'pointer' }}
              />
            </div>
          </div>
        </div>
        
        <div style={styles.actions}>
          <button 
            style={styles.cancelButton} 
            onClick={onClose}
          >
            Cancel
          </button>
          <button 
            style={styles.startButton} 
            onClick={startStudySession}
            disabled={cardCount === 0}
          >
            Start Studying
          </button>
        </div>
        
        {cardCount === 0 && (
          <div style={{
            textAlign: 'center',
            color: 'var(--error-color, #dc3545)',
            marginTop: '12px',
            fontSize: '14px'
          }}>
            You need to add cards to this collection before you can study
          </div>
        )}
      </div>
    </div>
  );
};

export default StudyOptionsModal; 