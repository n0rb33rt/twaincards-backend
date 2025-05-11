// src/pages/study/StudyDashboard.jsx
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getCardsForReview } from '../../api/learningApi';
import { getUserCollections, getPublicCollections } from '../../api/collectionApi';
import { getStatusStatistics } from '../../api/learningApi';
import '../../styles/main.css';

const StudyDashboard = () => {
  const [cardsToReview, setCardsToReview] = useState([]);
  const [collections, setCollections] = useState([]);
  const [studyStats, setStudyStats] = useState([]);
  const [selectedLanguage, setSelectedLanguage] = useState('all');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // Group collections by language pairs
  const groupedCollections = collections.reduce((acc, collection) => {
    const sourceCode = collection.sourceLanguageCode || 'unknown';
    const targetCode = collection.targetLanguageCode || 'unknown';
    const sourceName = collection.sourceLanguageName || 'Not specified';
    const targetName = collection.targetLanguageName || 'Not specified';
    
    const key = `${sourceCode}-${targetCode}`;
    const displayName = `${sourceName} → ${targetName}`;

    if (!acc[key]) {
      acc[key] = {
        displayName,
        sourceLanguage: sourceName,
        targetLanguage: targetName,
        collections: []
      };
    }

    acc[key].collections.push(collection);
    return acc;
  }, {});

  // Group review cards by collection
  const reviewsByCollection = cardsToReview.reduce((acc, card) => {
    if (!acc[card.collectionId]) {
      acc[card.collectionId] = [];
    }
    acc[card.collectionId].push(card);
    return acc;
  }, {});

  useEffect(() => {
    const fetchStudyData = async () => {
      try {
        setIsLoading(true);

        // Fetch all required data in parallel
        const [reviewCards, userCollections, publicCollectionsData, learningStats] = await Promise.all([
          getCardsForReview(), // Don't pass a collection ID here to get all due cards
          getUserCollections(),
          getPublicCollections(0, 50), // Fetch up to 50 public collections
          getStatusStatistics()
        ]);

        setCardsToReview(reviewCards);
        
        // Combine user and public collections
        let allCollections = [];
        
        // Process user collections
        if (Array.isArray(userCollections)) {
          allCollections = [...userCollections];
        } else if (userCollections && userCollections.content) {
          allCollections = [...userCollections.content];
        }
        
        // Process public collections
        if (Array.isArray(publicCollectionsData)) {
          allCollections = [...allCollections, ...publicCollectionsData];
        } else if (publicCollectionsData && publicCollectionsData.content) {
          allCollections = [...allCollections, ...publicCollectionsData.content];
        }
        
        // Remove duplicates if any (based on collection ID)
        const uniqueCollections = allCollections.filter((collection, index, self) =>
          index === self.findIndex(c => c.id === collection.id)
        );
        
        setCollections(uniqueCollections);
        setStudyStats(learningStats);
      } catch (error) {
        console.error('Error fetching study data:', error);
        setError('Failed to load study data');
        toast.error('Failed to load study data');
      } finally {
        setIsLoading(false);
      }
    };

    fetchStudyData();
  }, []);

  // Filter collections by selected language
  const filteredCollections = selectedLanguage === 'all'
    ? Object.values(groupedCollections)
    : Object.values(groupedCollections).filter(group => {
      const [source, target] = selectedLanguage.split('-');
      return group.sourceLanguage === source && group.targetLanguage === target;
    });

  // Get unique language pairs for filter dropdown
  const languagePairs = Object.values(groupedCollections).map(group => ({
    value: `${group.sourceLanguage}-${group.targetLanguage}`,
    label: group.displayName
  }));

  if (isLoading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading study data...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="error-container">
        <h2>Error</h2>
        <p>{error}</p>
        <button onClick={() => window.location.reload()} className="retry-button">
          Retry
        </button>
      </div>
    );
  }

  // Check if the user has any collections
  if (collections.length === 0) {
    return (
      <div className="empty-state-container">
        <div className="empty-state">
          <h2>No Collections Found</h2>
          <p>You need to create collections with cards before you can start studying.</p>
          <Link to="/collections/new" className="create-collection-btn">
            Create Your First Collection
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="study-dashboard-container">
      <header className="dashboard-header">
        <h1>Study Dashboard</h1>
        <p className="dashboard-subtitle">
          Review your cards and track your learning progress
        </p>
      </header>

      <div className="dashboard-content">
        <section className="review-section">
          <h2>Cards Due for Review</h2>

          {cardsToReview.length > 0 ? (
            <div className="review-cards">
              {Object.entries(reviewsByCollection).map(([collectionId, cards]) => {
                const collection = collections.find(c => c.id === parseInt(collectionId));
                if (!collection) return null;

                return (
                  <div key={collectionId} className="review-collection-card">
                    <div className="collection-info">
                      <h3>{collection.name}</h3>
                      <div className="language-pair">
                        {collection.sourceLanguageName || 'Not specified'} → {collection.targetLanguageName || 'Not specified'}
                      </div>
                      <div className="card-count">
                        {cards.length} card{cards.length !== 1 ? 's' : ''} to review
                      </div>
                    </div>

                    <Link to={`/study/session/${collectionId}`} className="start-review-btn">
                      Review Now
                    </Link>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="no-reviews-message">
              <p>You don't have any cards due for review right now. Great job!</p>
              <p>Why not study some new cards?</p>
            </div>
          )}
        </section>

        <section className="learning-status-section">
          <h2>Your Learning Status</h2>

          {studyStats.length > 0 ? (
            <div className="status-cards">
              {/* Ensure we display all status types even if count is zero */}
              {['NEW', 'LEARNING', 'REVIEW', 'KNOWN'].map(statusType => {
                // Find the stat for this status, or create one with count 0
                const stat = studyStats.find(s => s.status === statusType) || { status: statusType, count: 0 };
                return (
                  <div key={statusType} className={`status-card ${stat.status.toLowerCase()}`}>
                    <div className="status-count">{stat.count}</div>
                    <div className="status-name">{stat.status}</div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="no-stats-message">
              <p>Start studying to see your learning progress!</p>
            </div>
          )}
        </section>

        <section className="collections-section">
          <div className="section-header-with-filter">
            <h2>Available Collections</h2>

            {languagePairs.length > 1 && (
              <div className="language-filter">
                <label htmlFor="language-select">Filter by language:</label>
                <select
                  id="language-select"
                  value={selectedLanguage}
                  onChange={(e) => setSelectedLanguage(e.target.value)}
                >
                  <option value="all">All Languages</option>
                  {languagePairs.map((pair, index) => (
                    <option key={index} value={pair.value}>
                      {pair.label}
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>

          {filteredCollections.length > 0 ? (
            <div className="collections-list">
              {filteredCollections.map((languageGroup, groupIndex) => (
                <div key={groupIndex} className="language-group">
                  <div className="language-header">
                    <h3>{languageGroup.displayName}</h3>
                  </div>

                  <div className="group-collections">
                    {languageGroup.collections.map(collection => (
                      <div key={collection.id} className="study-collection-card">
                        <div className="collection-details">
                          <h4>{collection.name}</h4>
                          {collection.description && (
                            <p className="collection-description">{collection.description}</p>
                          )}
                          <div className="collection-meta">
                            <span className="card-count">{collection.cardCount || 0} cards</span>
                            {collection.completionPercentage !== undefined && collection.completionPercentage !== null && (
                              <span className="completion-percentage">
                                {collection.completionPercentage.toFixed(2)}% complete
                              </span>
                            )}
                          </div>
                        </div>

                        <div className="collection-actions">
                          <Link to={`/study/session/${collection.id}`} className="study-btn">
                            Study
                          </Link>
                          <Link to={`/collections/${collection.id}`} className="view-btn">
                            View
                          </Link>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="no-collections-message">
              <p>No collections match the selected language filter.</p>
              <button
                onClick={() => setSelectedLanguage('all')}
                className="show-all-btn"
              >
                Show All Collections
              </button>
            </div>
          )}
        </section>
      </div>
    </div>
  );
};

export default StudyDashboard;