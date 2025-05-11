import React, { useState, useEffect } from 'react';
import { Alert, Badge, ProgressBar } from 'react-bootstrap';
import { useAuth } from '../../context/AuthContext';
import axios from '../../api/axiosConfig';

/**
 * Component to display study limit information for basic users
 */
const CardLimitIndicator = () => {
  const [limitInfo, setLimitInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { user } = useAuth();

  useEffect(() => {
    // Only fetch limit info if user is logged in
    if (user) {
      fetchLimitInfo();
    }
  }, [user]);

  const fetchLimitInfo = async () => {
    try {
      setLoading(true);
      const response = await axios.get('/api/study-sessions/limit-status');
      setLimitInfo(response.data);
      setError(null);
    } catch (err) {
      console.error('Error fetching study limit info:', err);
      setError('Failed to load study limit information');
    } finally {
      setLoading(false);
    }
  };

  // Don't show anything if we're loading or have no limit info
  if (loading || !limitInfo) {
    return null;
  }

  // Don't show for premium or admin users who have no limits
  if (!limitInfo.hasLimit) {
    return null;
  }

  // Calculate progress percentage
  const usedCards = limitInfo.dailyLimit - limitInfo.remainingCards;
  const percentUsed = (usedCards / limitInfo.dailyLimit) * 100;

  return (
    <div className="card-limit-indicator mb-3">
      <div className="d-flex justify-content-between align-items-center mb-1">
        <span>Daily study limit:</span>
        <Badge 
          bg={limitInfo.remainingCards > 5 ? 'primary' : 'warning'}
        >
          {limitInfo.remainingCards} / {limitInfo.dailyLimit} cards left
        </Badge>
      </div>
      <ProgressBar 
        now={percentUsed} 
        variant={percentUsed > 90 ? 'danger' : percentUsed > 70 ? 'warning' : 'success'} 
      />
      {limitInfo.remainingCards < 5 && (
        <Alert variant="warning" className="mt-2 p-2 small">
          <i className="fas fa-exclamation-triangle me-2"></i>
          You're almost at your daily limit. <a href="/pricing">Upgrade to Premium</a> for unlimited studying!
        </Alert>
      )}
    </div>
  );
};

export default CardLimitIndicator; 