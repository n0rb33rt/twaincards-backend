// src/pages/cards/CardList.jsx
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getCardsByCollection, searchCards, getCardsByTag, deleteCard } from '../../api/cardApi';
import { getCollectionById } from '../../api/collectionApi';
import { getTagsByCollection } from '../../api/tagApi';
import { useAuth } from '../../context/AuthContext.jsx';
import '../../styles/cards.css';

const CardList = () => {
  const { collectionId } = useParams();
  const navigate = useNavigate();
  const { currentUser } = useAuth();

  const [collection, setCollection] = useState(null);
  const [cards, setCards] = useState([]);
  const [tags, setTags] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedTag, setSelectedTag] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [deleteConfirmation, setDeleteConfirmation] = useState(null);

  const pageSize = 12;

  useEffect(() => {
    const fetchCollectionData = async () => {
      try {
        setIsLoading(true);

        // Fetch collection details
        const collectionData = await getCollectionById(collectionId);
        setCollection(collectionData);

        // Fetch tags for filtering
        const tagsData = await getTagsByCollection(collectionId);
        setTags(tagsData);

        // Initial cards fetch happens in the fetchCards function
      } catch (error) {
        console.error('Error fetching collection data:', error);
        setError('Failed to load collection data');
        toast.error('Failed to load collection data');
      } finally {
        setIsLoading(false);
      }
    };

    fetchCollectionData();
  }, [collectionId]);

  // Fetch cards based on current filters and pagination
  useEffect(() => {
    const fetchCards = async () => {
      try {
        setIsLoading(true);
        let cardsData;

        if (searchTerm) {
          // Search cards by term
          cardsData = await searchCards(collectionId, searchTerm);
          setCards(cardsData);
          setTotalPages(Math.ceil(cardsData.length / pageSize));
        } else if (selectedTag) {
          // Filter cards by tag
          cardsData = await getCardsByTag(collectionId, selectedTag);
          setCards(cardsData);
          setTotalPages(Math.ceil(cardsData.length / pageSize));
        } else {
          // Get paginated cards
          const response = await getCardsByCollection(collectionId, currentPage, pageSize);
          setCards(response.content || []);
          setTotalPages(response.totalPages || 0);
        }
      } catch (error) {
        console.error('Error fetching cards:', error);
        toast.error('Failed to load cards');
      } finally {
        setIsLoading(false);
      }
    };

    if (collection) {
      fetchCards();
    }
  }, [collectionId, currentPage, searchTerm, selectedTag]);

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
    setCurrentPage(0);
    setSelectedTag('');
  };

  const handleTagSelect = (tagName) => {
    setSelectedTag(tagName === selectedTag ? '' : tagName);
    setSearchTerm('');
    setCurrentPage(0);
  };

  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage);
    }
  };

  const handleDeleteClick = (cardId) => {
    setDeleteConfirmation(cardId);
  };

  const handleConfirmDelete = async () => {
    if (!deleteConfirmation) return;

    try {
      await deleteCard(deleteConfirmation);

      // Update the cards list by removing the deleted card
      setCards(cards.filter(card => card.id !== deleteConfirmation));

      toast.success('Card deleted successfully');
    } catch (error) {
      console.error('Error deleting card:', error);
      toast.error('Failed to delete card');
    } finally {
      // Close the confirmation dialog
      setDeleteConfirmation(null);
    }
  };

  const handleCancelDelete = () => {
    setDeleteConfirmation(null);
  };

  // If still loading initial data
  if (isLoading && !collection) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading cards...</p>
      </div>
    );
  }

  // If there was an error loading the data
  if (error || !collection) {
    return (
      <div className="error-container">
        <h2>Error</h2>
        <p>{error || 'Collection not found'}</p>
        <button onClick={() => navigate('/collections')} className="retry-button">
          Back to Collections
        </button>
      </div>
    );
  }

  // Check if the user is the owner of the collection
  const isOwner = currentUser && collection.userId === currentUser.id;

  // Get displayed cards (for pagination)
  const displayedCards = searchTerm || selectedTag
    ? cards.slice(currentPage * pageSize, (currentPage + 1) * pageSize)
    : cards;

  return (
    <div className="card-list-container">
      <header className="list-header">
        <div className="header-content">
          <h1>Cards in {collection.name}</h1>
          <div className="collection-info">
            <span className="language-pair">
              <span className="source-language">{collection.sourceLanguageName}</span> →
              <span className="target-language">{collection.targetLanguageName}</span>
            </span>
            {collection.isPublic && <span className="public-badge">Public</span>}
          </div>
        </div>
        <div className="header-actions">
          <Link to={`/collections/${collectionId}`} className="back-to-collection-btn">
            Back to Collection
          </Link>
          {isOwner && (
            <Link to={`/collections/${collectionId}/cards/new`} className="add-card-btn">
              Add New Card
            </Link>
          )}
        </div>
      </header>

      <div className="filter-section">
        <div className="search-box">
          <input
            type="text"
            placeholder="Search cards..."
            value={searchTerm}
            onChange={handleSearchChange}
            className="search-input"
          />
          {searchTerm && (
            <button className="clear-search" onClick={() => setSearchTerm('')}>
              ×
            </button>
          )}
        </div>

        {tags.length > 0 && (
          <div className="tags-filter">
            <span className="filter-label">Filter by tag:</span>
            <div className="tags-list">
              {tags.map(tag => (
                <button
                  key={tag.id}
                  className={`tag-button ${selectedTag === tag.name ? 'active' : ''}`}
                  onClick={() => handleTagSelect(tag.name)}
                >
                  {tag.name}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {isLoading ? (
        <div className="loading-overlay">
          <div className="spinner"></div>
          <p>Loading cards...</p>
        </div>
      ) : displayedCards.length > 0 ? (
        <div className="cards-grid">
          {displayedCards.map(card => (
            <div key={card.id} className="card-item">
              <div className="card-content">
                <div className="card-front">
                  <div className="language-label">{collection.sourceLanguageName}</div>
                  <div className="card-text">{card.frontText}</div>
                  {card.imageUrl && (
                    <div className="card-image">
                      <img src={card.imageUrl} alt={card.frontText} />
                    </div>
                  )}
                </div>
                <div className="card-divider">→</div>
                <div className="card-back">
                  <div className="language-label">{collection.targetLanguageName}</div>
                  <div className="card-text">{card.backText}</div>
                  {card.phoneticText && (
                    <div className="phonetic-text">[{card.phoneticText}]</div>
                  )}
                </div>
              </div>

              {card.exampleUsage && (
                <div className="card-example">
                  <div className="example-label">Example:</div>
                  <div className="example-text">{card.exampleUsage}</div>
                </div>
              )}

              {card.tags && card.tags.length > 0 && (
                <div className="card-tags">
                  {card.tags.map(tag => (
                    <span key={tag.id} className="card-tag">
                      {tag.name}
                    </span>
                  ))}
                </div>
              )}

              {isOwner && (
                <div className="card-actions">
                  <Link to={`/cards/${card.id}/edit`} className="edit-card-btn">
                    Edit
                  </Link>
                  <button
                    className="delete-card-btn"
                    onClick={() => handleDeleteClick(card.id)}
                  >
                    Delete
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      ) : (
        <div className="empty-state">
          <div className="empty-state-content">
            <h2>No Cards Found</h2>
            {searchTerm || selectedTag ? (
              <>
                <p>No cards match your search criteria.</p>
                <button
                  className="clear-filters-btn"
                  onClick={() => {
                    setSearchTerm('');
                    setSelectedTag('');
                  }}
                >
                  Clear Filters
                </button>
              </>
            ) : (
              <>
                <p>This collection doesn't have any cards yet.</p>
                {isOwner && (
                  <Link
                    to={`/collections/${collectionId}/cards/new`}
                    className="add-first-card-btn"
                  >
                    Add Your First Card
                  </Link>
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
          <span className="page-info">
            Page {currentPage + 1} of {totalPages}
          </span>
          <button
            className="pagination-btn"
            disabled={currentPage === totalPages - 1}
            onClick={() => handlePageChange(currentPage + 1)}
          >
            Next
          </button>
        </div>
      )}

      {deleteConfirmation && (
        <div className="delete-modal">
          <div className="delete-modal-content">
            <h2>Confirm Deletion</h2>
            <p>Are you sure you want to delete this card? This action cannot be undone.</p>
            <div className="delete-modal-actions">
              <button
                className="cancel-btn"
                onClick={handleCancelDelete}
              >
                Cancel
              </button>
              <button
                className="confirm-btn"
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

export default CardList;