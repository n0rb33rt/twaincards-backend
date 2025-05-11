import React, { useState, useEffect, useRef } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faXmark, faPlus } from '@fortawesome/free-solid-svg-icons';

// Simple translation function for TagInput
const useSimpleTranslation = () => {
  const translations = {
    'common.addTag': 'Add tag',
    'common.add': 'Add'
  };
  
  const t = (key) => translations[key] || key;
  return { t };
};

/**
 * TagInput component for adding and managing tags
 * 
 * @param {Object} props - Component props
 * @param {Array} props.tags - Array of tags
 * @param {Array} props.suggestions - Array of tag suggestions
 * @param {Function} props.onAdd - Function to call when a tag is added
 * @param {Function} props.onRemove - Function to call when a tag is removed
 * @param {string} props.placeholder - Placeholder text for the input
 */
const TagInput = ({ tags = [], suggestions = [], onAdd, onRemove, placeholder }) => {
  const { t } = useSimpleTranslation();
  const [input, setInput] = useState('');
  const [filteredSuggestions, setFilteredSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const inputRef = useRef(null);
  const suggestionsRef = useRef(null);

  // Filter suggestions based on input and already selected tags
  useEffect(() => {
    if (input.trim() === '') {
      setFilteredSuggestions([]);
      return;
    }

    const filtered = suggestions
      .filter(suggestion => 
        suggestion.toLowerCase().includes(input.toLowerCase()) && 
        !tags.includes(suggestion)
      )
      .slice(0, 5); // Limit to 5 suggestions
    
    setFilteredSuggestions(filtered);
  }, [input, suggestions, tags]);

  // Close suggestions dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (
        suggestionsRef.current && 
        !suggestionsRef.current.contains(event.target) &&
        !inputRef.current.contains(event.target)
      ) {
        setShowSuggestions(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleInputChange = (e) => {
    setInput(e.target.value);
    setShowSuggestions(true);
  };

  const handleAddTag = () => {
    if (input.trim() === '') return;
    
    onAdd(input.trim());
    setInput('');
    setShowSuggestions(false);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleAddTag();
    } else if (e.key === 'Escape') {
      setShowSuggestions(false);
    }
  };

  const handleSuggestionClick = (suggestion) => {
    onAdd(suggestion);
    setInput('');
    setShowSuggestions(false);
  };

  return (
    <div className="tag-input-wrapper">
      <div className="selected-tags">
        {tags.map((tag, index) => (
          <div key={`tag-${index}`} className="tag">
            <span className="tag-text">{tag}</span>
            <button 
              type="button" 
              className="remove-tag" 
              onClick={() => onRemove(index)}
              aria-label={`Remove tag ${tag}`}
            >
              <FontAwesomeIcon icon={faXmark} />
            </button>
          </div>
        ))}
      </div>
      
      <div className="tag-input-container">
        <input
          ref={inputRef}
          type="text"
          className="tag-input"
          value={input}
          onChange={handleInputChange}
          onKeyDown={handleKeyDown}
          onFocus={() => setShowSuggestions(true)}
          placeholder={placeholder || t('common.addTag')}
        />
        
        <button 
          type="button" 
          className="add-tag-button" 
          onClick={handleAddTag}
          disabled={!input.trim()}
        >
          <FontAwesomeIcon icon={faPlus} />
          {t('common.add')}
        </button>
      </div>
      
      {showSuggestions && filteredSuggestions.length > 0 && (
        <div ref={suggestionsRef} className="tag-suggestions">
          {filteredSuggestions.map((suggestion, index) => (
            <div 
              key={`suggestion-${index}`}
              className="tag-suggestion"
              onClick={() => handleSuggestionClick(suggestion)}
            >
              {suggestion}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default TagInput; 