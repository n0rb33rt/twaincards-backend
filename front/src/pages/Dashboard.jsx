// src/pages/Dashboard.jsx
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from '../context/AuthContext.jsx';
import { getUserStatistics, getLanguageStatistics } from '../api/statisticsApi';
import { getCardsForReview } from '../api/learningApi';
import { getUserCollections, getRecentCollections } from '../api/collectionApi';
import { getLearningProgress } from '../api/learningApi';
import { Chart as ChartJS, ArcElement, Tooltip, Legend, CategoryScale, LinearScale, PointElement, LineElement, Title, BarElement } from 'chart.js';
import { Doughnut, Bar } from 'react-chartjs-2';
import '../styles/main.css';
import axios from 'axios';

// Register Chart.js components
ChartJS.register(
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title
);

const Dashboard = () => {
  const { currentUser } = useAuth();
  const [userStats, setUserStats] = useState(null);
  const [languageStats, setLanguageStats] = useState([]);
  const [recentCollections, setRecentCollections] = useState([]);
  const [cardsToReview, setCardsToReview] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statistics, setStatistics] = useState(null);
  const [learningProgress, setLearningProgress] = useState(null);

  // Function to sanitize collection data and extract language info
  const sanitizeCollection = (collection) => {
    if (!collection) return null;

    try {
      // Make sure id exists
      if (!collection.id) {
        console.warn('Collection missing ID:', collection);
        return null;
      }

      // Ensure required fields exist
      return {
        id: collection.id,
        name: collection.name || 'Unnamed Collection',
        description: collection.description || '',
        isPublic: !!collection.isPublic,
        cardsCount: collection.cardsCount || collection.cardCount || 0,
        completionPercentage: typeof collection.completionPercentage === 'number' ? collection.completionPercentage : null,
        // Handle both object format and direct field format for languages
        sourceLanguageName: collection.sourceLanguageName || collection.sourceLanguage?.name || 'Not specified',
        sourceLanguageCode: collection.sourceLanguageCode || collection.sourceLanguage?.code || 'N/A',
        targetLanguageName: collection.targetLanguageName || collection.targetLanguage?.name || 'Not specified',
        targetLanguageCode: collection.targetLanguageCode || collection.targetLanguage?.code || 'N/A',
        userId: collection.userId || collection.user?.id || null
      };
    } catch (err) {
      console.error('Error sanitizing collection:', err, collection);
      return null;
    }
  };

  useEffect(() => {
    const fetchDashboardData = async () => {
      setIsLoading(true);
      setError(null);
      
      try {
        // Fetch user's statistics
        const statsData = await getUserStatistics();
        setStatistics(statsData);
        
        // Fetch recent collections using the dedicated endpoint
        const collectionsData = await getRecentCollections(5);
        const sanitizedCollections = Array.isArray(collectionsData) 
          ? collectionsData.map(sanitizeCollection).filter(Boolean)
          : [];
        setRecentCollections(sanitizedCollections);
        
        // Fetch learning progress
        const progressData = await getLearningProgress();
        setLearningProgress(progressData);
      } catch (err) {
        console.error('Error fetching dashboard data:', err);
        setError('Failed to load dashboard data. Please try again later.');
      } finally {
        setIsLoading(false);
      }
    };
    
    fetchDashboardData();
  }, []);

  // Chart data for progress overview
  const progressChartData = {
    labels: ['Learned', 'In Progress'],
    datasets: [
      {
        data: [
          userStats?.learnedCards || 0,
          (userStats?.totalCards || 0) - (userStats?.learnedCards || 0)
        ],
        backgroundColor: ['#4CAF50', '#FFC107'],
        borderColor: ['#388E3C', '#FFA000'],
        borderWidth: 1,
      },
    ],
  };

  // Chart data for language progress
  const languageChartData = {
    labels: languageStats.map(lang => lang.languageName),
    datasets: [
      {
        label: 'Completion (%)',
        data: languageStats.map(lang => lang.completionPercentage),
        backgroundColor: 'rgba(54, 162, 235, 0.5)',
        borderColor: 'rgba(54, 162, 235, 1)',
        borderWidth: 1,
      },
    ],
  };

  if (isLoading) {
    return (
      <div className="dashboard-loading">
        <div className="spinner"></div>
        <p>Loading your dashboard...</p>
      </div>
    );
  }
  
  if (error) {
    return (
      <div className="dashboard-error">
        <h2>Oops! Something went wrong</h2>
        <p>{error}</p>
        <button onClick={() => window.location.reload()} className="retry-button">
          Try Again
        </button>
      </div>
    );
  }

  return (
    <div className="dashboard-container">
      <section className="dashboard-welcome">
        <div className="welcome-text">
          <h1>Welcome back, {currentUser?.firstName || currentUser?.username}!</h1>
          <p>Track your progress and continue your language learning journey.</p>
        </div>
        
        <div className="quick-actions">
          <Link to="/collections" className="action-button">
            <span className="action-icon">📚</span>
            <span>My Collections</span>
          </Link>
          <Link to="/study" className="action-button">
            <span className="action-icon">🎯</span>
            <span>Study Now</span>
          </Link>
          <Link to="/collections/new" className="action-button">
            <span className="action-icon">➕</span>
            <span>New Collection</span>
          </Link>
        </div>
      </section>

      <div className="dashboard-grid">
        <section className="dashboard-card learning-stats">
          <h2>Learning Statistics</h2>
          {statistics ? (
            <div className="stats-grid">
              <div className="stat-item">
                <div className="stat-value">{statistics.totalCards || 0}</div>
                <div className="stat-label">Total Cards</div>
              </div>
              <div className="stat-item">
                <div className="stat-value">{statistics.learnedCards || 0}</div>
                <div className="stat-label">Learned Cards</div>
              </div>
              <div className="stat-item">
                <div className="stat-value">{statistics.learningStreakDays || 0}</div>
                <div className="stat-label">Day Streak</div>
              </div>
              <div className="stat-item">
                <div className="stat-value">{(statistics.completionPercentage || 0).toFixed(2)}%</div>
                <div className="stat-label">Completion</div>
              </div>
            </div>
          ) : (
            <p className="empty-state">No statistics available yet. Start learning to see your progress!</p>
          )}
          <Link to="/statistics" className="view-all-link">View Detailed Statistics →</Link>
        </section>

        <section className="dashboard-card recent-collections">
          <h2>Recent Collections</h2>
          {recentCollections.length > 0 ? (
            <div className="collections-list">
              {recentCollections.map((collection) => (
                <div key={collection.id} className="collection-item">
                  <div className="collection-info">
                    <h3>{collection.name}</h3>
                    <div className="language-pair">
                      {collection.sourceLanguageName || 'Not specified'} → {collection.targetLanguageName || 'Not specified'}
                    </div>
                    <div className="card-count">{collection.cardCount || 0} cards</div>
                  </div>
                  <Link to={`/collections/${collection.id}`} className="view-button">
                    View
                  </Link>
                </div>
              ))}
            </div>
          ) : (
            <p className="empty-state">You don't have any collections yet. Create one to get started!</p>
          )}
          <Link to="/collections" className="view-all-link">View All Collections →</Link>
        </section>
      </div>
    </div>
  );
};

export default Dashboard;