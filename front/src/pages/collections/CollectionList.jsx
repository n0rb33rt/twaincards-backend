// src/pages/collections/CollectionList.jsx
import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import { 
  getUserCollectionsPaginated, 
  getPublicCollections, 
  deleteCollection, 
  searchPublicCollections, 
  searchUserCollections,
  getCollectionUsersCount
} from '../../api/collectionApi';
import * as importApi from '../../api/importApi';
import { toast } from 'react-toastify';
import CollectionActionsMenu from '../../components/collections/CollectionActionsMenu';
import '../../styles/main.css';
import '../../styles/importexport.css';

const CollectionList = () => {
  const { currentUser } = useAuth();
  const navigate = useNavigate();
  const [userCollections, setUserCollections] = useState([]);
  const [publicCollections, setPublicCollections] = useState([]);
  const [activeTab, setActiveTab] = useState('my');
  const [searchTerm, setSearchTerm] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [deleteConfirmation, setDeleteConfirmation] = useState(null);
  const [isImporting, setIsImporting] = useState(false);
  const [userCounts, setUserCounts] = useState({});
  const fileInputRef = useRef(null);

  const pageSize = 12;

  // Check if collection data has required fields
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

  // Additional function to fetch user counts for public collections
  const fetchUserCounts = async (collections) => {
    const counts = {};
    
    try {
      // Create an array of promises for all the requests
      const promises = collections.map(collection => 
        getCollectionUsersCount(collection.id)
          .then(count => {
            counts[collection.id] = count;
          })
          .catch(error => {
            console.error(`Error fetching user count for collection ${collection.id}:`, error);
            counts[collection.id] = 0;
          })
      );
      
      // Wait for all promises to resolve
      await Promise.all(promises);
      
      setUserCounts(counts);
    } catch (error) {
      console.error('Error fetching user counts:', error);
    }
  };

  const fetchCollections = useCallback(async (page = 0) => {
    try {
      setIsLoading(true);
      setError(null);

      if (activeTab === 'my') {
        try {
          let response;
          if (searchTerm.trim()) {
            response = await searchUserCollections(searchTerm, page, pageSize);
          } else {
            response = await getUserCollectionsPaginated(page, pageSize);
          }
          
          console.log('User collections API response:', response);
          
          // Handle different response formats
          let collections = [];
          if (Array.isArray(response)) {
            collections = response;
            setTotalPages(1);
          } else if (response && Array.isArray(response.content)) {
            collections = response.content;
            setTotalPages(response.totalPages || 1);
          } else if (response) {
            // If response is not an array and doesn't have content property,
            // assume it's a direct collection object
            collections = [response];
            setTotalPages(1);
          }

          // Sanitize collections
          const sanitizedCollections = collections
            .map(sanitizeCollection)
            .filter(item => item !== null);
          
          console.log('Sanitized user collections:', sanitizedCollections);
          setUserCollections(sanitizedCollections);
        } catch (error) {
          console.error('Error fetching user collections:', error);
          setUserCollections([]);
          throw error;
        }
      } else if (activeTab === 'public') {
        try {
          let response;
          if (searchTerm.trim()) {
            response = await searchPublicCollections(searchTerm, page, pageSize);
          } else {
            response = await getPublicCollections(page, pageSize);
          }
          
          console.log('Public collections API response:', response);
          
          // Handle different response formats
          let collections = [];
          if (Array.isArray(response)) {
            collections = response;
            setTotalPages(1);
          } else if (response && Array.isArray(response.content)) {
            collections = response.content;
            setTotalPages(response.totalPages || 1);
          } else if (response) {
            // If response is not an array and doesn't have content property,
            // assume it's a direct collection object
            collections = [response];
            setTotalPages(1);
          }

          // Sanitize collections
          const sanitizedCollections = collections
            .map(sanitizeCollection)
            .filter(item => item !== null);
          
          console.log('Sanitized public collections:', sanitizedCollections);
          setPublicCollections(sanitizedCollections);
          
          // Fetch user counts for public collections
          if (sanitizedCollections.length > 0) {
            fetchUserCounts(sanitizedCollections);
          }
        } catch (error) {
          console.error('Error fetching public collections:', error);
          setPublicCollections([]);
          throw error;
        }
      }
    } catch (error) {
      console.error('Error fetching collections:', error);
      setError('Failed to load collections. Please try again.');
      toast.error('Failed to load collections: ' + (error.message || 'Unknown error'));
    } finally {
      setIsLoading(false);
    }
  }, [activeTab, searchTerm, pageSize]);

  // Single combined useEffect that handles all state changes
  useEffect(() => {
    // For searchTerm changes, we want to reset to first page and apply a debounce
    let debounceTimer;
    
    if (searchTerm !== '') {
      // Only apply debounce for search term changes
      debounceTimer = setTimeout(() => {
        fetchCollections(0);
        if (currentPage !== 0) {
          setCurrentPage(0);
        }
      }, 500);
    } else {
      // For tab changes and pagination, fetch immediately
      fetchCollections(currentPage);
    }
    
    return () => {
      if (debounceTimer) {
        clearTimeout(debounceTimer);
      }
    };
  }, [activeTab, currentPage, searchTerm, fetchCollections]);

  const handleTabChange = (tab) => {
    if (tab !== activeTab) {
      setActiveTab(tab);
      setCurrentPage(0);
      setSearchTerm('');
    }
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  };

  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage);
    }
  };

  const handleDeleteClick = (collectionId) => {
    setDeleteConfirmation(collectionId);
  };

  const handleConfirmDelete = async () => {
    if (!deleteConfirmation) return;

    try {
      await deleteCollection(deleteConfirmation);
      setUserCollections(userCollections.filter(c => c.id !== deleteConfirmation));
      toast.success('Collection deleted successfully');
    } catch (error) {
      console.error('Error deleting collection:', error);
      toast.error('Failed to delete collection');
    } finally {
      setDeleteConfirmation(null);
    }
  };

  const handleCancelDelete = () => {
    setDeleteConfirmation(null);
  };

  const handleImportSuccess = (importedCollection) => {
    if (importedCollection && importedCollection.id) {
      navigate(`/collections/${importedCollection.id}`);
    } else {
      // Refresh collections list
      fetchCollections(currentPage);
    }
  };

  const displayedCollections = activeTab === 'my' ? userCollections : publicCollections;

  // Add retry functionality
  const handleRetry = () => {
    fetchCollections(currentPage);
  };

  // Handle import file selection
  const handleImportSelect = () => {
    fileInputRef.current.click();
  };

  // Handle file upload for import
  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    
    if (!file) return;

    try {
      setIsImporting(true);
      const importedCollection = await importApi.importCollectionFromCsv(file);

      toast.success('Collection imported successfully from CSV');
      
      // Reset the file input
      event.target.value = '';
      
      // Navigate to the imported collection
      handleImportSuccess(importedCollection);
    } catch (error) {
      console.error('Import error:', error);
      toast.error(`Failed to import collection: ${error.message}`);
    } finally {
      setIsImporting(false);
    }
  };

  return (
    <div className="collections-container">
      {/* Hidden file input for import */}
      <input
        type="file"
        ref={fileInputRef}
        style={{ display: 'none' }}
        onChange={handleFileUpload}
        accept=".csv"
      />
    
      <header className="collections-header">
        <h1>Collections</h1>
        <div className="header-actions">
          <Link to="/collections/new" className="create-collection-btn">
            Create New Collection
          </Link>
          <button 
            className="import-collection-btn"
            onClick={() => handleImportSelect()}
            disabled={isImporting}
          >
            {isImporting ? 'Importing...' : 'Import CSV'}
          </button>
        </div>
      </header>

      <div className="collections-tabs">
        <button
          className={`tab-btn ${activeTab === 'my' ? 'active' : ''}`}
          onClick={() => handleTabChange('my')}
        >
          My Collections
        </button>
        <button
          className={`tab-btn ${activeTab === 'public' ? 'active' : ''}`}
          onClick={() => handleTabChange('public')}
        >
          Public Collections
        </button>
      </div>

      <div className="collections-search">
        <input
          type="text"
          placeholder={`Search ${activeTab === 'my' ? 'your' : 'public'} collections...`}
          value={searchTerm}
          onChange={handleSearchChange}
          className="search-input"
        />
        {searchTerm && (
          <button
            className="clear-search"
            onClick={() => setSearchTerm('')}
            aria-label="Clear search"
          >
            ×
          </button>
        )}
      </div>

      {isLoading ? (
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Loading collections...</p>
        </div>
      ) : error ? (
        <div className="error-container">
          <p>{error}</p>
          <button onClick={handleRetry} className="retry-button">
            Retry
          </button>
        </div>
      ) : (
        <>
          {displayedCollections && displayedCollections.length > 0 ? (
            <div className="collections-grid">
              {displayedCollections.map((collection) => (
                <div key={collection.id} className="collection-card">
                  <div className="collection-header">
                    <h3>{collection.name}</h3>
                    <div className="collection-header-actions">
                      {collection.isPublic && (
                        <span className="public-tag">Public</span>
                      )}
                      {(activeTab === 'my' || collection.userId === currentUser?.id) && (
                        <CollectionActionsMenu 
                          collection={collection} 
                          onDelete={handleDeleteClick}
                          onImportSuccess={handleImportSuccess}
                        />
                      )}
                    </div>
                  </div>

                  <div className="collection-languages">
                    <p className="collection-language">
                      <span className="language-name">
                        {collection.sourceLanguageName || 'Unknown'}
                      </span>
                      <span className="language-arrow">→</span>
                      <span className="language-name">
                        {collection.targetLanguageName || 'Unknown'}
                      </span>
                    </p>
                  </div>

                  {collection.description && (
                    <p className="collection-description">{collection.description}</p>
                  )}

                  <div className="collection-stats">
                    <div className="stat-item">
                      <span className="stat-label">Cards:</span>
                      <span className="stat-value">
                        {collection.cardsCount || 0}
                      </span>
                    </div>

                    {collection.completionPercentage !== undefined && collection.completionPercentage !== null && (
                      <div className="stat-item">
                        <span className="stat-label">Completed:</span>
                        <span className="stat-value">{collection.completionPercentage.toFixed(2)}%</span>
                      </div>
                    )}
                    
                    {activeTab === 'public' && userCounts[collection.id] !== undefined && (
                      <div className="stat-item users-count">
                        <span className="stat-label">Users:</span>
                        <span className="stat-value">{userCounts[collection.id]}</span>
                      </div>
                    )}
                  </div>

                  <Link to={`/collections/${collection.id}`} className="collection-link">
                    <span className="view-text">View Collection</span>
                  </Link>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <div className="empty-state-content">
                <h2>No collections found</h2>
                {activeTab === 'my' ? (
                  <>
                    <p>You haven't created any collections yet.</p>
                    <Link to="/collections/new" className="create-collection-link">
                      Create your first collection
                    </Link>
                  </>
                ) : (
                  <>
                    <p>No public collections available{searchTerm ? ' matching your search' : ''}.</p>
                    {searchTerm && (
                      <button
                        className="clear-search-btn button-secondary"
                        onClick={() => setSearchTerm('')}
                      >
                        Clear search
                      </button>
                    )}
                  </>
                )}
              </div>
            </div>
          )}

          {totalPages > 1 && (
            <div className="pagination">
              <button
                className="pagination-btn"
                disabled={currentPage === 0}
                onClick={() => handlePageChange(currentPage - 1)}
              >
                Previous
              </button>

              <div className="pagination-info">
                Page {currentPage + 1} of {totalPages}
              </div>

              <button
                className="pagination-btn"
                disabled={currentPage === totalPages - 1}
                onClick={() => handlePageChange(currentPage + 1)}
              >
                Next
              </button>
            </div>
          )}
        </>
      )}

      {deleteConfirmation && (
        <div className="delete-confirmation-modal">
          <div className="delete-confirmation-content">
            <h3>Confirm Deletion</h3>
            <p>Are you sure you want to delete this collection? This action cannot be undone.</p>
            <div className="delete-confirmation-actions">
              <button
                className="cancel-button"
                onClick={handleCancelDelete}
              >
                Cancel
              </button>
              <button
                className="confirm-delete-button"
                onClick={handleConfirmDelete}
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default CollectionList;