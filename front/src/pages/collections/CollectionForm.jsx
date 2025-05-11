// src/pages/collections/CollectionForm.jsx
import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { createCollection, updateCollection, getCollectionById } from '../../api/collectionApi';
import { getAllEnabledLanguages } from '../../api/languageApi';
import '../../styles/main.css';

// Constants for field restrictions
const MAX_NAME_LENGTH = 100;
const MAX_DESCRIPTION_LENGTH = 255;

const CollectionForm = ({ isEdit = false }) => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    sourceLanguageId: '',
    targetLanguageId: '',
    isPublic: false
  });

  const [languages, setLanguages] = useState([]);
  const [errors, setErrors] = useState({});
  const [isLoading, setIsLoading] = useState(false);
  const [isFetching, setIsFetching] = useState(false);

  // Add state to track character counts
  const [nameLengthCount, setNameLengthCount] = useState(0);
  const [descriptionLengthCount, setDescriptionLengthCount] = useState(0);

  useEffect(() => {
    // Update character counts
    setNameLengthCount(formData.name.length);
    setDescriptionLengthCount(formData.description.length);
  }, [formData.name, formData.description]);

  useEffect(() => {
    const fetchLanguages = async () => {
      try {
        const languagesData = await getAllEnabledLanguages();
        setLanguages(languagesData);
      } catch (error) {
        console.error('Error fetching languages:', error);
        toast.error('Failed to load languages');
      }
    };

    const fetchCollection = async () => {
      if (!isEdit || !id) return;

      try {
        setIsFetching(true);
        const collectionData = await getCollectionById(id);

        setFormData({
          name: collectionData.name,
          description: collectionData.description || '',
          sourceLanguageId: collectionData.sourceLanguageId,
          targetLanguageId: collectionData.targetLanguageId,
          isPublic: collectionData.isPublic || false
        });
      } catch (error) {
        console.error('Error fetching collection:', error);
        toast.error('Failed to load collection data');
        navigate('/collections');
      } finally {
        setIsFetching(false);
      }
    };

    fetchLanguages();
    fetchCollection();
  }, [isEdit, id, navigate]);

  const validateForm = () => {
    const newErrors = {};

    if (!formData.name.trim()) {
      newErrors.name = 'Collection name is required';
    } else if (formData.name.length < 2) {
      newErrors.name = 'Collection name must be at least 2 characters';
    } else if (formData.name.length > MAX_NAME_LENGTH) {
      newErrors.name = `Collection name must be at most ${MAX_NAME_LENGTH} characters`;
    }

    if (formData.description.length > MAX_DESCRIPTION_LENGTH) {
      newErrors.description = `Description must be at most ${MAX_DESCRIPTION_LENGTH} characters`;
    }

    if (!formData.sourceLanguageId) {
      newErrors.sourceLanguageId = 'Source language is required';
    }

    if (!formData.targetLanguageId) {
      newErrors.targetLanguageId = 'Target language is required';
    }

    if (formData.sourceLanguageId === formData.targetLanguageId) {
      newErrors.targetLanguageId = 'Source and target languages must be different';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;

    // Apply restrictions directly during input
    if (name === 'name' && value.length > MAX_NAME_LENGTH) {
      return;
    }
    
    if (name === 'description' && value.length > MAX_DESCRIPTION_LENGTH) {
      return;
    }

    setFormData({
      ...formData,
      [name]: type === 'checkbox' ? checked : value
    });

    // Clear error when user changes a field
    if (errors[name]) {
      setErrors({ ...errors, [name]: null });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) return;

    try {
      setIsLoading(true);

      // Convert language IDs to numbers
      const payload = {
        ...formData,
        sourceLanguageId: parseInt(formData.sourceLanguageId, 10),
        targetLanguageId: parseInt(formData.targetLanguageId, 10)
      };

      if (isEdit) {
        await updateCollection(id, payload);
        toast.success('Collection updated successfully');
      } else {
        await createCollection(payload);
        toast.success('Collection created successfully');
      }

      navigate('/collections');
    } catch (error) {
      console.error('Error saving collection:', error);
      toast.error(isEdit ? 'Failed to update collection' : 'Failed to create collection');
    } finally {
      setIsLoading(false);
    }
  };

  if (isFetching) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading collection data...</p>
      </div>
    );
  }

  return (
    <div className="collection-form-container">
      <div className="form-header">
        <h1>{isEdit ? 'Edit Collection' : 'Create New Collection'}</h1>
        <p className="form-subtitle">
          {isEdit
            ? 'Update your collection details'
            : 'Create a new collection of flashcards to study'}
        </p>
      </div>

      <form className="collection-form" onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="name">Collection Name* (Max {MAX_NAME_LENGTH} characters)</label>
          <input
            type="text"
            id="name"
            name="name"
            value={formData.name}
            onChange={handleChange}
            className={errors.name ? 'input-error' : ''}
            placeholder="Enter collection name"
            disabled={isLoading}
            maxLength={MAX_NAME_LENGTH}
          />
          <div className="character-count">
            <small className={nameLengthCount > MAX_NAME_LENGTH * 0.8 ? "text-danger" : ""}>
              {nameLengthCount}/{MAX_NAME_LENGTH}
            </small>
          </div>
          {errors.name && <div className="error-message">{errors.name}</div>}
        </div>

        <div className="form-group">
          <label htmlFor="description">Description (Max {MAX_DESCRIPTION_LENGTH} characters)</label>
          <textarea
            id="description"
            name="description"
            value={formData.description}
            onChange={handleChange}
            placeholder="Enter collection description"
            rows="3"
            disabled={isLoading}
            maxLength={MAX_DESCRIPTION_LENGTH}
          />
          <div className="character-count">
            <small className={descriptionLengthCount > MAX_DESCRIPTION_LENGTH * 0.8 ? "text-danger" : ""}>
              {descriptionLengthCount}/{MAX_DESCRIPTION_LENGTH}
            </small>
          </div>
          {errors.description && <div className="error-message">{errors.description}</div>}
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="sourceLanguageId">Source Language*</label>
            <select
              id="sourceLanguageId"
              name="sourceLanguageId"
              value={formData.sourceLanguageId}
              onChange={handleChange}
              className={errors.sourceLanguageId ? 'input-error' : ''}
              disabled={isLoading}
            >
              <option value="">Select source language</option>
              {languages.map(language => (
                <option key={`source-${language.id}`} value={language.id}>
                  {language.name} {language.nativeName ? `(${language.nativeName})` : ''}
                </option>
              ))}
            </select>
            {errors.sourceLanguageId && <div className="error-message">{errors.sourceLanguageId}</div>}
          </div>

          <div className="form-group">
            <label htmlFor="targetLanguageId">Target Language*</label>
            <select
              id="targetLanguageId"
              name="targetLanguageId"
              value={formData.targetLanguageId}
              onChange={handleChange}
              className={errors.targetLanguageId ? 'input-error' : ''}
              disabled={isLoading}
            >
              <option value="">Select target language</option>
              {languages.map(language => (
                <option key={`target-${language.id}`} value={language.id}>
                  {language.name} {language.nativeName ? `(${language.nativeName})` : ''}
                </option>
              ))}
            </select>
            {errors.targetLanguageId && <div className="error-message">{errors.targetLanguageId}</div>}
          </div>
        </div>

        <div className="form-row checkbox-group">
          <div className="form-check">
            <input
              type="checkbox"
              id="isPublic"
              name="isPublic"
              checked={formData.isPublic}
              onChange={handleChange}
              disabled={isLoading}
            />
            <label htmlFor="isPublic">Make collection public</label>
          </div>
        </div>

        <div className="form-actions">
          <button
            type="button"
            className="cancel-button"
            onClick={() => navigate('/collections')}
            disabled={isLoading}
          >
            Cancel
          </button>

          <button
            type="submit"
            className="primary-button"
            disabled={isLoading}
          >
            {isLoading ? 'Saving...' : isEdit ? 'Update Collection' : 'Create Collection'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default CollectionForm;