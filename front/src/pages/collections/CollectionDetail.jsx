// src/pages/collections/CollectionDetail.jsx
import React, { useState, useEffect, useMemo } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { 
  faArrowLeft, faArrowRight, faEdit, faTrash, 
  faPlus, faSync, faTimes, faChevronLeft, faChevronRight 
} from '@fortawesome/free-solid-svg-icons';
import { toast } from 'react-toastify';
import { getCollectionById, deleteCollection } from '../../api/collectionApi';
import { getCardsByCollection, deleteCard } from '../../api/cardApi';
import { getCollectionLearningStats } from '../../api/learningApi';
import { getTagsByCollection } from '../../api/tagApi';
import { useAuth } from '../../context/AuthContext.jsx';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';
import { Pie } from 'react-chartjs-2';
import ImportExportMenu from '../../components/collections/ImportExportMenu';
import CollectionActionsMenu from '../../components/collections/CollectionActionsMenu';
import Spinner from "../../components/shared/Spinner";
import ConfirmDialog from "../../components/shared/ConfirmDialog";
import '../../styles/main.css';
import '../../styles/collectionDetail.css';

ChartJS.register(ArcElement, Tooltip, Legend);

// Status chart colors config
const STATUS_COLORS = {
  KNOWN: { bg: '#4CAF50', border: '#388E3C' },
  REVIEW: { bg: '#FFC107', border: '#FFA000' },
  LEARNING: { bg: '#2196F3', border: '#1976D2' },
  NEW: { bg: '#9E9E9E', border: '#757575' }
};

const CollectionDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { currentUser } = useAuth();

  // Collection data state
  const [collection, setCollection] = useState(null);
  const [cards, setCards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // UI state
  const [activeTab, setActiveTab] = useState('cards');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedTags, setSelectedTags] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);
  const [cardToDelete, setCardToDelete] = useState(null);
  const [confirmCardDeleteOpen, setConfirmCardDeleteOpen] = useState(false);
  const [learningStats, setLearningStats] = useState(null);
  const [loadingStats, setLoadingStats] = useState(false);
  
  const cardsPerPage = 12;
  
  // Calculate if user is collection owner
  const isOwner = useMemo(() => 
    currentUser && collection?.userId === currentUser.id,
    [currentUser, collection]
  );

  // Fetch collection data
  useEffect(() => {
    const fetchCollectionData = async () => {
      try {
        setLoading(true);
        const collectionData = await getCollectionById(id);
        console.log('Collection data received:', collectionData);
        setCollection(collectionData);

        const cardsData = await getCardsByCollection(id);
        setCards(Array.isArray(cardsData) ? cardsData : cardsData.content || []);
        
        setLoading(false);
      } catch (err) {
        console.error('Error fetching collection data:', err);
        setError(err.message || 'Failed to load collection');
        setLoading(false);
        toast.error('Failed to load collection');
      }
    };

    fetchCollectionData();
  }, [id]);
  
  // Extract unique tags
  const allTags = useMemo(() => {
    if (!cards.length) return [];
    
    const tagsSet = new Set();
    cards.forEach(card => {
      if (card.tags && card.tags.length) {
        card.tags.forEach(tag => tagsSet.add(tag));
      }
    });
    
    return Array.from(tagsSet).sort();
  }, [cards]);
  
  // Filter and paginate cards
  const filteredCards = useMemo(() => {
    if (!cards.length) return [];
    
    return cards.filter(card => {
      // Only proceed with text search if search term exists
      if (searchTerm) {
        // Add safe checks for card properties and use correct field names from CardDTO
        const frontMatch = card.frontText && typeof card.frontText === 'string' 
          ? card.frontText.toLowerCase().includes(searchTerm.toLowerCase()) 
          : false;
          
        const backMatch = card.backText && typeof card.backText === 'string'
          ? card.backText.toLowerCase().includes(searchTerm.toLowerCase())
          : false;
          
        // Also check example text if it exists
        const exampleMatch = card.exampleUsage && typeof card.exampleUsage === 'string'
          ? card.exampleUsage.toLowerCase().includes(searchTerm.toLowerCase())
          : false;
          
        // If we have a search term but no matches, filter this card out
        if (!(frontMatch || backMatch || exampleMatch)) {
          return false;
        }
      }
      
      // Filter by selected tags
      if (selectedTags.length > 0) {
        // Make sure card.tags exists and is an array before using some()
        return card.tags && Array.isArray(card.tags) && 
          card.tags.some(tag => selectedTags.includes(tag.name || tag));
      }
      
      // If we got here, include the card
      return true;
    });
  }, [cards, searchTerm, selectedTags]);
  
  const totalPages = Math.ceil(filteredCards.length / cardsPerPage);
  
  const paginatedCards = useMemo(() => {
    const startIndex = (currentPage - 1) * cardsPerPage;
    const endIndex = startIndex + cardsPerPage;
    return filteredCards.slice(startIndex, endIndex);
  }, [filteredCards, currentPage]);
  
  // Reset pagination when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, selectedTags]);
  
  // Fetch learning statistics
  useEffect(() => {
    if (activeTab === 'stats' && collection && !learningStats) {
      const fetchStats = async () => {
        try {
          setLoadingStats(true);
          const stats = await getCollectionLearningStats(collection.id);
          
          // Process stats into a consistent format
          if (Array.isArray(stats)) {
            // If stats is already an array, use it directly
            setLearningStats(stats);
          } else if (stats && typeof stats === 'object') {
            // If stats is an object with counts, transform it for chart use
            const transformedStats = [];
            
            // Add masteredCount
            if (stats.masteredCount > 0) {
              transformedStats.push({
                status: 'KNOWN',
                count: stats.masteredCount
              });
            }
            
            // Add learningCount
            if (stats.learningCount > 0) {
              transformedStats.push({
                status: 'LEARNING',
                count: stats.learningCount
              });
            }
            
            // Add not started count
            if (stats.notStartedCount > 0) {
              transformedStats.push({
                status: 'NEW',
                count: stats.notStartedCount
              });
            }
            
            // Store both formats
            setLearningStats({
              ...stats,
              chartData: transformedStats
            });
          }
          
          setLoadingStats(false);
        } catch (err) {
          console.error('Failed to load statistics:', err);
          setLoadingStats(false);
        }
      };
      
      fetchStats();
    }
  }, [activeTab, collection, learningStats]);
  
  // Handle collection deletion
  const handleDeleteCollection = async () => {
    try {
      await deleteCollection(collection.id);
      toast.success('Collection deleted successfully');
      navigate('/collections');
    } catch (err) {
      toast.error(err.message || 'Failed to delete collection');
    }
  };
  
  // Handle card deletion
  const handleDeleteCard = async () => {
    if (!cardToDelete) return;
    
    try {
      await deleteCard(cardToDelete.id);
      setCards(cards.filter(card => card.id !== cardToDelete.id));
      toast.success('Card deleted successfully');
      setConfirmCardDeleteOpen(false);
    } catch (err) {
      toast.error(err.message || 'Failed to delete card');
    }
  };
  
  // Handle tag selection
  const toggleTagSelection = (tag) => {
    setSelectedTags(prev => 
      prev.includes(tag) 
        ? prev.filter(t => t !== tag)
        : [...prev, tag]
    );
  };
  
  // Clear all filters
  const clearFilters = () => {
    setSearchTerm('');
    setSelectedTags([]);
  };
  
  // Prepare chart data for learning status visualization
  const statusChartData = useMemo(() => {
    // Check if learningStats is available
    if (!learningStats) {
      return {
        labels: [],
        datasets: [{
          data: [],
          backgroundColor: [],
          borderColor: [],
          borderWidth: 1,
        }],
      };
    }
    
    // Use chartData from learningStats if available, otherwise use the array directly
    const statsData = learningStats.chartData || (Array.isArray(learningStats) ? learningStats : []);
    
    if (!statsData || !statsData.length) {
      return {
        labels: [],
        datasets: [{
          data: [],
          backgroundColor: [],
          borderColor: [],
          borderWidth: 1,
        }],
      };
    }
    
    return {
      labels: statsData.map(stat => stat.status),
      datasets: [{
        data: statsData.map(stat => stat.count),
        backgroundColor: statsData.map(stat => STATUS_COLORS[stat.status]?.bg || '#CCCCCC'),
        borderColor: statsData.map(stat => STATUS_COLORS[stat.status]?.border || '#AAAAAA'),
        borderWidth: 1,
      }],
  };
  }, [learningStats]);

  if (loading) {
    return (
      <div className="spinner-container">
        <Spinner />
      </div>
    );
  }

  if (error) {
    return (
      <div className="error-message">
        <h3>Error</h3>
        <p>{error}</p>
        <Link to="/collections">Back to Collections</Link>
      </div>
    );
  }

  if (!collection) {
    return (
      <div className="not-found">
        <h3>Collection not found</h3>
        <Link to="/collections">Back to Collections</Link>
      </div>
    );
  }

  return (
    <div className="collection-detail-container">
      {/* Navigation */}
      <div className="navigation-header">
        <Link to="/collections" className="back-link">
          <FontAwesomeIcon icon={faArrowLeft} className="back-icon" />
          Back to Collections
        </Link>
      </div>
      
      {/* Collection Header */}
      <div className="collection-header">
        <div className="collection-title-wrapper">
          <h1>{collection.name}</h1>
          {collection.public && <span className="public-tag">Public</span>}
        </div>

        <div className="collection-tools">
          <Link 
            to={`/collections/${collection.id}/edit`}
            className="btn btn-secondary"
          >
            <FontAwesomeIcon icon={faEdit} /> Edit
          </Link>
          
          <Link 
            to={`/collections/${collection.id}/cards/new`}
            className="btn btn-secondary"
          >
            <FontAwesomeIcon icon={faPlus} /> Add Card
          </Link>
          
          <button 
            className="btn btn-danger"
            onClick={() => setConfirmDeleteOpen(true)}
          >
            <FontAwesomeIcon icon={faTrash} /> Delete
          </button>
        </div>
      </div>
      
      {/* Collection Meta Information */}
      <div className="collection-meta-section">
        <div className="meta-columns">
          <div>
            <div className="collection-languages">
        <div className="language-pair">
                <span className="language-name">{collection.sourceLanguageName || 'Unknown language'}</span>
                <FontAwesomeIcon icon={faArrowRight} className="language-arrow" />
                <span className="language-name">{collection.targetLanguageName || 'Unknown language'}</span>
          </div>
        </div>

        {collection.description && (
          <div className="collection-description">
            <p>{collection.description}</p>
          </div>
        )}
          </div>

          <div className="collection-stats">
            <div className="stat-item">
              <span className="stat-label">Cards</span>
              <span className="stat-value">{cards.length}</span>
            </div>
            
            {collection.lastModified && (
              <div className="stat-item">
                <span className="stat-label">Updated</span>
                <span className="stat-value">{new Date(collection.lastModified).toLocaleDateString()}</span>
            </div>
          )}
          </div>
        </div>
      </div>

      {/* Collection Tabs */}
      <div className="collection-tabs">
        <button 
          className={`tab-button ${activeTab === 'cards' ? 'active' : ''}`}
          onClick={() => setActiveTab('cards')}
        >
          Cards
        </button>
        <button 
          className={`tab-button ${activeTab === 'stats' ? 'active' : ''}`}
          onClick={() => setActiveTab('stats')}
        >
          Learning Statistics
        </button>
          </div>

      {/* Collection Content */}
      <div className="collection-content">
        {activeTab === 'cards' && (
          <div className="cards-tab">
            {/* Card Controls */}
            <div className="card-controls">
              <div className="search-filter-row">
                <div className="search-container">
              <input
                type="text"
                    className="search-input"
                placeholder="Search cards..."
                value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
              />
              {searchTerm && (
                <button
                  className="clear-search"
                  onClick={() => setSearchTerm('')}
                >
                      <FontAwesomeIcon icon={faTimes} />
                    </button>
                  )}
                </div>
                
                {(searchTerm || selectedTags.length > 0) && (
                  <button 
                    className="btn btn-sm btn-secondary"
                    onClick={clearFilters}
                  >
                    Clear Filters
                </button>
              )}
            </div>

              {allTags.length > 0 && (
              <div className="tags-filter">
                  <div className="filter-label">Filter by tags:</div>
                <div className="tags-list">
                    {allTags.map(tag => (
                    <button
                        key={tag}
                        className={`tag-button ${selectedTags.includes(tag) ? 'active' : ''}`}
                        onClick={() => toggleTagSelection(tag)}
                      >
                        {tag}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>

            {/* Cards Display */}
            {filteredCards.length > 0 ? (
              <>
                <div className="cards-grid">
                  {paginatedCards.map(card => (
                <div key={card.id} className="card-item">
                      {/* Card header with actions */}
                      <div className="card-header">
                        <div className="card-actions">
                          <Link 
                            to={`/collections/${collection.id}/cards/${card.id}/edit`}
                            className="card-action-button edit-button"
                          >
                            <FontAwesomeIcon icon={faEdit} /> Edit
                          </Link>
                          <button 
                            className="card-action-button delete-button"
                            onClick={() => {
                              setCardToDelete(card);
                              setConfirmCardDeleteOpen(true);
                            }}
                          >
                            <FontAwesomeIcon icon={faTrash} /> Delete
                          </button>
                        </div>
                      </div>

                      {/* Card content - only front side */}
                      <div className="card-body">
                        <div className="card-single-side">
                          <div className="side-label">Term:</div>
                          <div className="card-text">{card.frontText}</div>
                      {card.phoneticText && (
                            <div className="phonetic-text">{card.phoneticText}</div>
                      )}
                    </div>

                        {card.exampleUsage && (
                          <div className="card-example">
                            <div className="example-label">Example:</div>
                            <div className="example-text">{card.exampleUsage}</div>
                    </div>
                        )}
                  </div>

                      {/* Card footer with tags */}
                  {card.tags && card.tags.length > 0 && (
                        <div className="card-footer">
                    <div className="card-tags">
                      {card.tags.map(tag => (
                              <span 
                                key={tag.id || tag} 
                                className="card-tag"
                                onClick={() => toggleTagSelection(tag.name || tag)}
                              >
                                {tag.name || tag}
                        </span>
                      ))}
                          </div>
                    </div>
                  )}
                    </div>
                  ))}
                </div>
                
                {/* Pagination */}
                {totalPages > 1 && (
                  <div className="pagination">
                    <button 
                      className="pagination-button"
                      onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                      disabled={currentPage === 1}
                    >
                      <FontAwesomeIcon icon={faChevronLeft} />
                    </button>
                    
                    <span className="pagination-info">
                      Page {currentPage} of {totalPages}
                    </span>
                    
                    <button 
                      className="pagination-button"
                      onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                      disabled={currentPage === totalPages}
                    >
                      <FontAwesomeIcon icon={faChevronRight} />
                    </button>
                  </div>
                )}
              </>
            ) : (
              <div className="empty-state">
                {cards.length === 0 ? (
                  <>
                    <h3>No cards in this collection yet</h3>
                    <p>Start by adding some cards to study!</p>
                    <Link 
                      to={`/collections/${collection.id}/cards/new`}
                      className="btn btn-primary mt-3"
                    >
                      <FontAwesomeIcon icon={faPlus} /> Add First Card
                  </Link>
                  </>
                ) : (
                  <>
                    <h3>No cards match your filters</h3>
                    <p>Try changing your search or filter criteria</p>
                  <button
                      className="btn btn-secondary mt-3"
                      onClick={clearFilters}
                    >
                      <FontAwesomeIcon icon={faSync} /> Clear Filters
                  </button>
                  </>
                )}
              </div>
            )}
          </div>
        )}
        
        {activeTab === 'stats' && (
          <div className="stats-tab">
            {loadingStats ? (
              <div className="text-center py-4">
                <Spinner />
              </div>
            ) : !learningStats || Object.keys(learningStats).length === 0 ? (
              <div className="empty-stats">
                <h3>No learning statistics available</h3>
                <p>Start studying to see your progress!</p>
              </div>
            ) : (
              <div className="stats-content">
                <div className="stats-grid">
                  <div className="stats-card">
                    <h3>Learning Progress</h3>
              <div className="chart-container">
                      <div className="chart-wrapper">
                <Pie
                  data={statusChartData}
                  options={{
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                      legend: {
                        position: 'bottom',
                                labels: {
                                  padding: 20,
                                  font: {
                                    size: 12
                                  }
                                }
                      },
                      tooltip: {
                        callbacks: {
                          label: function(context) {
                            const label = context.label || '';
                            const value = context.parsed || 0;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = ((value / total) * 100).toFixed(1);
                                    return `${label}: ${value} cards (${percentage}%)`;
                          }
                        }
                      }
                    }
                  }}
                />
                      </div>
                      
                      <div className="status-summary">
                        <div className="status-item">
                          <div className="status-color" style={{ backgroundColor: '#4CAF50' }}></div>
                          <span className="status-label">Mastered:</span>
                          <span className="status-count">{learningStats.masteredCount || 0}</span>
                        </div>
                        
                        <div className="status-item">
                          <div className="status-color" style={{ backgroundColor: '#FFC107' }}></div>
                          <span className="status-label">Learning:</span>
                          <span className="status-count">{learningStats.learningCount || 0}</span>
                        </div>
                        
                        <div className="status-item">
                          <div className="status-color" style={{ backgroundColor: '#F44336' }}></div>
                          <span className="status-label">Not Started:</span>
                          <span className="status-count">{learningStats.notStartedCount || 0}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <div className="stats-card">
                    <h3>Study Suggestions</h3>
                    <div className="suggestion-list">
                      {(learningStats.notStartedCount > 0) && (
                        <div className="suggestion-item">
                          <div className="suggestion-icon start-icon">
                            <FontAwesomeIcon icon={faPlay} />
                          </div>
                          <div className="suggestion-content">
                            <h4>Start Learning</h4>
                            <p>You have {learningStats.notStartedCount} cards that you haven't started learning yet.</p>
                </div>
              </div>
                      )}
                      
                      {(learningStats.reviewDueCount > 0) && (
                        <div className="suggestion-item">
                          <div className="suggestion-icon review-icon">
                            <FontAwesomeIcon icon={faSync} />
                          </div>
                          <div className="suggestion-content">
                            <h4>Review Due Cards</h4>
                            <p>You have {learningStats.reviewDueCount} cards due for review today.</p>
                          </div>
          </div>
                      )}
                      
                      {(learningStats.masteredCount === cards.length && cards.length > 0) && (
                        <div className="suggestion-item">
                          <div className="suggestion-icon new-icon">
                            <FontAwesomeIcon icon={faPlus} />
                          </div>
                          <div className="suggestion-content">
                            <h4>All Cards Mastered!</h4>
                            <p>Congratulations! Consider adding new cards or starting a new collection.</p>
              </div>
            </div>
          )}
        </div>
      </div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
      
      {/* Confirm Delete Collection Dialog */}
      <ConfirmDialog
        isOpen={confirmDeleteOpen}
        title="Delete Collection"
        message={`Are you sure you want to delete "${collection.name}"? This action cannot be undone.`}
        confirmText="Delete"
        cancelText="Cancel"
        onConfirm={handleDeleteCollection}
        onCancel={() => setConfirmDeleteOpen(false)}
      />
      
      {/* Confirm Delete Card Dialog */}
      <ConfirmDialog
        isOpen={confirmCardDeleteOpen}
        title="Delete Card"
        message={cardToDelete ? `Are you sure you want to delete this card?` : ''}
        confirmText="Delete"
        cancelText="Cancel"
        onConfirm={handleDeleteCard}
        onCancel={() => setConfirmCardDeleteOpen(false)}
      />
    </div>
  );
};

export default CollectionDetail;