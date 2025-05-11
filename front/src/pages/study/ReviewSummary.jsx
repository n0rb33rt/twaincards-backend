// src/pages/study/ReviewSummary.jsx
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getCollectionById } from '../../api/collectionApi';
import { getSummaryStatistics } from '../../api/statisticsApi';
import { getStatusStatistics } from '../../api/learningApi';
import * as studySessionApi from '../../api/studySessionApi';
import { Chart as ChartJS, ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement } from 'chart.js';
import { Doughnut, Bar } from 'react-chartjs-2';
import '../../styles/review-summary.css';
import '../../styles/main.css';


// Register Chart.js components
ChartJS.register(
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  BarElement
);

const ReviewSummary = () => {
  const { collectionId, sessionId } = useParams();
  const navigate = useNavigate();

  const [collection, setCollection] = useState(null);
  const [sessionSummary, setSessionSummary] = useState(null);
  const [summaryStats, setSummaryStats] = useState(null);
  const [statusStats, setStatusStats] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchSummaryData = async () => {
      try {
        setIsLoading(true);
        setError(null);

        // Check if we have a session ID
        if (!collectionId && !sessionId) {
          throw new Error('No collection or session ID specified');
        }

        // Data we need to fetch
        let collectionData = null;
        let sessionData = null;
        let statusStatsData = null;
        let summaryData = null;

        // First fetch the session data if we have a session ID
        if (sessionId) {
          try {
            // The session might be in a completed state already
            sessionData = await studySessionApi.getSessionById(sessionId);
            
            // If it's not completed yet, complete it
            if (sessionData && !sessionData.isCompleted && sessionData.id) {
              const completeRequest = {
                sessionId: sessionData.id,
                cardsReviewed: sessionData.cardsReviewed || 0,
                correctAnswers: sessionData.correctAnswers || 0
              };
              sessionData = await studySessionApi.completeSession(completeRequest);
            }
          } catch (err) {
            console.error('Failed to fetch or complete session:', err);
            // Continue with other data
          }
        }

        // Get the target collection ID
        const targetCollectionId = collectionId || (sessionData && sessionData.collectionId);
        
        if (targetCollectionId) {
          // Fetch collection details and status statistics in parallel
          [collectionData, statusStatsData, summaryData] = await Promise.all([
            getCollectionById(targetCollectionId),
            getStatusStatistics(),
            getSummaryStatistics()
          ]);
        } else {
          [statusStatsData, summaryData] = await Promise.all([
            getStatusStatistics(),
            getSummaryStatistics()
          ]);
        }

        if (collectionData) {
          setCollection(collectionData);
        }

        if (sessionData) {
          setSessionSummary(sessionData);
        } else if (targetCollectionId) {
          // If we don't have session data but we have a collection, fetch recent sessions
          try {
            const recentSessions = await studySessionApi.getSessionsForCollection(targetCollectionId, 1);
            if (recentSessions && recentSessions.length > 0) {
              setSessionSummary(recentSessions[0]);
            }
          } catch (err) {
            console.error('Failed to fetch recent sessions:', err);
          }
        }

        if (statusStatsData) {
          setStatusStats(statusStatsData);
        }

        if (summaryData) {
          setSummaryStats(summaryData);
        }
      } catch (err) {
        console.error('Failed to fetch summary data:', err);
        setError('Failed to load summary data. Please try again.');
        toast.error('Failed to load summary data.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchSummaryData();
  }, [collectionId, sessionId]);

  const calculateSuccessRate = (summary) => {
    if (!summary) return 0;
    if (summary.cardsStudied === 0) return 0;
    
    return summary.successRate || Math.round((summary.correctAnswers / summary.cardsStudied) * 100);
  };

  const formatTime = (seconds) => {
    if (!seconds) return '0m 0s';
    
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}m ${secs}s`;
  };

  // Prepare chart data for cards by status
  const prepareStatusChartData = () => {
    if (!statusStats || statusStats.length === 0) return null;

    const labels = statusStats.map(stat => stat.status);
    const data = statusStats.map(stat => stat.count);
    const backgroundColors = [
      'rgba(108, 99, 255, 0.7)', // NEW - purple
      'rgba(255, 193, 7, 0.7)',  // LEARNING - yellow
      'rgba(13, 110, 253, 0.7)', // REVIEW - blue
      'rgba(25, 135, 84, 0.7)'   // KNOWN - green
    ];

    return {
      labels,
      datasets: [
        {
          data,
          backgroundColor: backgroundColors,
          borderColor: backgroundColors.map(color => color.replace('0.7', '1')),
          borderWidth: 1,
        },
      ],
    };
  };

  if (isLoading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading summary...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="error-container">
        <h2>Error Loading Summary</h2>
        <p>{error}</p>
        <div className="error-actions">
          <button onClick={() => navigate('/study')} className="back-button">
            Back to Study Dashboard
          </button>
        </div>
      </div>
    );
  }

  // Default summary data
  const defaultSummary = {
    cardsStudied: 0,
    correctAnswers: 0,
    incorrectAnswers: 0,
    timeSpentSeconds: 0,
    successRate: 0,
    collectionId: collectionId || (sessionSummary ? sessionSummary.collectionId : null)
  };

  // Use the session summary data or create default
  const summaryData = sessionSummary || defaultSummary;

  return (
    <div className="review-summary-container">
      <header className="summary-header">
        <h1>Study Session Complete!</h1>
        {collection && (
          <div className="collection-info">
            <h2>{collection.name}</h2>
            <div className="language-pair">
              <span className="source-language">{collection.sourceLanguageName || 'Not specified'}</span> →
              <span className="target-language">{collection.targetLanguageName || 'Not specified'}</span>
            </div>
          </div>
        )}
      </header>

      <div className="summary-grid">
        <div className="session-results-card">
          <h2>Session Results</h2>

          <div className="stats-grid">
            <div className="stat-item">
              <div className="stat-value">{summaryData.cardsStudied}</div>
              <div className="stat-label">Cards Studied</div>
            </div>

            <div className="stat-item correct">
              <div className="stat-value">{summaryData.correctAnswers}</div>
              <div className="stat-label">Correct</div>
            </div>

            <div className="stat-item incorrect">
              <div className="stat-value">{summaryData.incorrectAnswers}</div>
              <div className="stat-label">Incorrect</div>
            </div>

            <div className="stat-item">
              <div className="stat-value">{calculateSuccessRate(summaryData)}%</div>
              <div className="stat-label">Success Rate</div>
            </div>
          </div>

          <div className="time-spent">
            <span className="time-icon"></span>
            <span className="time-text">
              Time spent: {formatTime(summaryData.timeSpentSeconds)}
            </span>
          </div>

          <div className="actions">
            <Link to="/study" className="back-link">
              Back to Study Dashboard
            </Link>
          </div>
        </div>

        <div className="status-overview-card">
          <h2>Learning Progress</h2>
          {statusStats && statusStats.length > 0 ? (
            <div className="chart-container">
              <Doughnut 
                data={prepareStatusChartData()} 
                options={{
                  responsive: true,
                  plugins: {
                    legend: {
                      position: 'bottom',
                      labels: {
                        padding: 20
                      }
                    }
                  }
                }}
              />
            </div>
          ) : (
            <div className="no-data-message">
              <p>No learning progress data available yet.</p>
            </div>
          )}
        </div>
      </div>

      <div className="return-options">
        <Link to={`/collections/${collection?.id}`} className="option-button primary">
          View Collection
        </Link>
        <Link to="/statistics" className="option-button secondary">
          View Detailed Statistics
        </Link>
      </div>
    </div>
  );
};

export default ReviewSummary;