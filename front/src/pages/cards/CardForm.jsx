// src/pages/cards/CardForm.jsx
import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faLanguage, faArrowRight } from '@fortawesome/free-solid-svg-icons';

import { useAuth } from '../../context/AuthContext';
import { fetchData, postData, putData } from '../../services/api';
import LoadingSpinner from '../../components/LoadingSpinner';
import Button from '../../components/Button';
import '../../styles/cards.css';

// Simple translation function to replace the i18next functionality
const useSimpleTranslation = () => {
  // Translation dictionary
  const translations = {
    'cards.frontSide': 'Front Side (Term)',
    'cards.backSide': 'Back Side (Definition)',
    'cards.exampleUsage': 'Example Usage',
    'cards.frontSidePlaceholder': 'Enter the term...',
    'cards.backSidePlaceholder': 'Enter the definition...',
    'cards.exampleUsagePlaceholder': 'Enter an example usage (optional)...',
    'cards.translateFromBack': 'Translate from Back',
    'cards.translateFromFront': 'Translate from Front',
    'cards.translateToBack': 'Translate to definition',
    'cards.noTextToTranslate': 'Please enter text to translate',
    'cards.translationSuccess': 'Translation successful',
    'cards.translationFailed': 'Translation failed',
    'cards.cardCreatedSuccessfully': 'Card created successfully',
    'cards.cardUpdatedSuccessfully': 'Card updated successfully',
    'cards.errors.frontTextRequired': 'Front text is required',
    'cards.errors.backTextRequired': 'Back text is required',
    'cards.editCard': 'Edit Card',
    'cards.createCard': 'Create Card',
    'common.unknown': 'Unknown',
    'common.translating': 'Translating...',
    'common.errors.somethingWentWrong': 'Something went wrong',
    'common.cancel': 'Cancel',
    'common.save': 'Save',
    'common.create': 'Create',
    'common.saving': 'Saving...',
    'common.errors.pleaseEnterText': 'Please enter text to translate',
    'common.errors.languagesNotConfigured': 'Languages are not configured',
    'common.success.translationComplete': 'Translation complete',
    'cards.translateExample': 'Translate Example',
    'cards.success': 'Card saved successfully!',
    'cards.error': 'Failed to save card.',
    'cards.fillRequiredFields': 'Please fill all required fields.'
  };

  // Return a simple translation function
  const t = (key) => translations[key] || key;
  
  return { t };
};

const CardForm = ({ isEdit }) => {
  const { t } = useSimpleTranslation();
  const { collectionId: urlCollectionId, id: urlCardId, cardId } = useParams();
  const { token } = useAuth();
  const navigate = useNavigate();
  
  // Use either the cardId or id param, whichever is provided
  const actualCardId = cardId || urlCardId;
  // Use the collectionId from URL params
  const actualCollectionId = urlCollectionId;
  
  const [frontText, setFrontText] = useState('');
  const [exampleText, setExampleText] = useState('');
  const [backText, setBackText] = useState('');
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [isEditing, setIsEditing] = useState(isEdit || !!actualCardId);
  const [collection, setCollection] = useState(null);
  const [translating, setTranslating] = useState(null);
  
  const frontInputRef = useRef(null);
  const hasFetchedRef = useRef(false); // Track if initial fetch happened
  
  const validateForm = () => {
    const newErrors = {};
    if (!frontText.trim()) {
      newErrors.frontText = t('cards.errors.frontTextRequired');
    }
    if (!backText.trim()) {
      newErrors.backText = t('cards.errors.backTextRequired');
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Fetch card data if editing
  useEffect(() => {
    // Skip if we've already fetched data and collectionId hasn't changed
    if (hasFetchedRef.current && !actualCardId) {
      return;
    }
    
    const fetchCardAndCollection = async () => {
      // Don't fetch if already loading
      if (loading) return;
      
      setLoading(true);
      try {
        let collectionIdForFetch = actualCollectionId;
        
        if (actualCardId) {
          setIsEditing(true);
          
          // Add protection against fetching the same card repeatedly
          if (!hasFetchedRef.current) {
            const cardData = await fetchData(`/api/cards/${actualCardId}`, token);
            setFrontText(cardData.frontText || '');
            setBackText(cardData.backText || '');
            setExampleText(cardData.exampleText || '');
            
            // If we don't have a collection ID from the URL, use the one from the card
            if (!collectionIdForFetch && cardData.collectionId) {
              collectionIdForFetch = cardData.collectionId;
            }
          }
        }
        
        if (!collectionIdForFetch) {
          console.error('No collection ID available');
          toast.error('Missing collection information');
          return;
        }
        
        // Fetch collection data only if we haven't already or if we need to refresh
        if (!hasFetchedRef.current) {
          // Always fetch collection data to ensure we have the latest
          console.log('Fetching collection data for ID:', collectionIdForFetch);
          const collectionData = await fetchData(`/api/collections/${collectionIdForFetch}`, token);
          console.log('Collection data received:', JSON.stringify(collectionData, null, 2));
          
          if (collectionData) {
            // Map the flat language properties to the expected nested objects
            // This handles the API response format which has separate fields instead of nested objects
            const enhancedCollectionData = {
              ...collectionData,
              sourceLanguage: {
                id: collectionData.sourceLanguageId,
                name: collectionData.sourceLanguageName,
                code: collectionData.sourceLanguageCode
              },
              targetLanguage: {
                id: collectionData.targetLanguageId,
                name: collectionData.targetLanguageName,
                code: collectionData.targetLanguageCode
              }
            };
            
            console.log('Enhanced collection data:', enhancedCollectionData);
            setCollection(enhancedCollectionData);
          } else {
            console.error('Collection data is null or undefined');
            toast.error('Could not load collection information');
          }
        }
        
        // Mark that we've fetched the data
        hasFetchedRef.current = true;
      } catch (error) {
        console.error('Error fetching data:', error);
        toast.error(t('common.errors.somethingWentWrong'));
      } finally {
        setLoading(false);
      }
    };
    
    fetchCardAndCollection();
  }, [actualCollectionId, actualCardId, token, t, loading]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }
    
    setLoading(true);
    
    const cardData = {
      frontText,
      backText,
      exampleText,
      collectionId: actualCollectionId
    };
    
    try {
      if (isEditing) {
        await putData(`/api/cards/${actualCardId}`, cardData, token);
        toast.success(t('cards.cardUpdatedSuccessfully'));
      } else {
        await postData('/api/cards', cardData, token);
        toast.success(t('cards.cardCreatedSuccessfully'));
      }
      navigate(`/collections/${actualCollectionId}`);
    } catch (error) {
      console.error('Error saving card:', error);
      toast.error(t('common.errors.somethingWentWrong'));
    } finally {
      setLoading(false);
    }
  };

  const translateText = async (text, from, to, field) => {
    if (!text.trim()) {
      toast.warning(t('common.errors.pleaseEnterText'));
      return;
    }

    const fromCode = collection?.sourceLanguage?.code;
    const toCode = collection?.targetLanguage?.code;

    if (!fromCode || !toCode) {
      console.error('Missing language codes:', { fromCode, toCode });
      toast.error(t('common.errors.languagesNotConfigured'));
      return;
    }

    console.log(`Translating from ${fromCode} to ${toCode}:`, text);

    setTranslating(field);
    try {
      // Determine correct language direction based on field
      let sourceLanguage, targetLanguage;
      
      // If translating front field (Ukrainian)
      if (field === 'front') {
        // We want back→front, so target→source
        sourceLanguage = toCode;  // English
        targetLanguage = fromCode; // Ukrainian
      } 
      // If translating back field (English) or example
      else {
        // We want front→back, so source→target
        sourceLanguage = fromCode; // Ukrainian
        targetLanguage = toCode;   // English
      }
      
      // Format request to match AzureTranslatorRequest on backend
      const requestBody = {
        to_translate: text,
        from_language: sourceLanguage,
        to_language: targetLanguage
      };
      
      console.log('Translation request body:', requestBody);
      
      const response = await postData('/api/translate', requestBody, token);
      console.log('Translation response:', response);
      
      // Check if response has translated property
      if (!response || (!response.translatedText && !response.translated)) {
        console.error('Invalid translation response:', response);
        toast.error(t('common.errors.translationFailed'));
        return;
      }

      // Get translated text from the appropriate property
      const translatedText = response.translatedText || response.translated;
      
      if (field === 'front') {
        setFrontText(translatedText);
      } else if (field === 'back') {
        setBackText(translatedText);
      } else if (field === 'example') {
        // Limit example text to 255 characters
        setExampleText(translatedText.substring(0, 255));
      }
      
      toast.success(t('common.success.translationComplete'));
    } catch (error) {
      console.error('Translation error:', error);
      toast.error(t('common.errors.translationFailed'));
    } finally {
      setTranslating(null);
    }
  };

  if (loading && !collection) {
    return <LoadingSpinner />;
  }

  return (
    <div className="container py-4">
      <div className="card-form-header">
        <h1>{isEditing ? t('cards.editCard') : t('cards.createCard')}</h1>
        <h2>{collection?.name || 'Loading collection...'}</h2>
      </div>

      <form onSubmit={handleSubmit} className="unified-card-form">
        {/* Front Side (Term) Field */}
        <div className="card-field-container">
          <div className="language-indicators">
            <span className="source-language-indicator">
              {collection && collection.sourceLanguage && collection.sourceLanguage.name 
                ? collection.sourceLanguage.name 
                : t('common.unknown')}
            </span>
          </div>

          <div className="field-header">
            <label htmlFor="frontText">{t('cards.frontSide')} <span className="required">*</span></label>
            <div className="translate-actions">
              <button 
                type="button" 
                className="translate-button translate-to-front"
                onClick={() => translateText(backText, 'back', 'front', 'front')}
                disabled={!backText || translating === 'front'}
              >
                <FontAwesomeIcon icon={faLanguage} />
                {translating === 'front' ? t('common.translating') : t('cards.translateFromBack')}
              </button>
            </div>
          </div>
          
          <textarea
            id="frontText"
            ref={frontInputRef}
            className="card-field"
            value={frontText}
            onChange={(e) => setFrontText(e.target.value)}
            placeholder={t('cards.frontSidePlaceholder')}
            maxLength={255}
          />
          {errors.frontText && <div className="error-message">{errors.frontText}</div>}
        </div>

        {/* Back Side (Definition) Field */}
        <div className="card-field-container">
          <div className="language-indicators">
            <span className="target-language-indicator">
              {collection && collection.targetLanguage && collection.targetLanguage.name 
                ? collection.targetLanguage.name 
                : t('common.unknown')}
            </span>
          </div>
          
          <div className="field-header">
            <label htmlFor="backText">{t('cards.backSide')} <span className="required">*</span></label>
            <div className="translate-actions">
              <button 
                type="button" 
                className="translate-button translate-to-back"
                onClick={() => translateText(frontText, 'front', 'back', 'back')}
                disabled={!frontText || translating === 'back'}
              >
                <FontAwesomeIcon icon={faLanguage} />
                {translating === 'back' ? t('common.translating') : t('cards.translateFromFront')}
              </button>
            </div>
          </div>
          
          <textarea
            id="backText"
            className="card-field"
            value={backText}
            onChange={(e) => setBackText(e.target.value)}
            placeholder={t('cards.backSidePlaceholder')}
            maxLength={255}
          />
          {errors.backText && <div className="error-message">{errors.backText}</div>}
        </div>

        {/* Example Usage Field */}
        <div className="card-field-container">
          <div className="field-header">
            <label htmlFor="exampleText">{t('cards.exampleUsage')}</label>
          </div>
          
          <textarea
            id="exampleText"
            className="card-field"
            value={exampleText}
            onChange={(e) => setExampleText(e.target.value)}
            placeholder={t('cards.exampleUsagePlaceholder')}
            maxLength={255}
          />
        </div>

        {/* Action Buttons */}
        <div className="form-actions">
          <Button type="button" variant="secondary" onClick={() => navigate(`/collections/${actualCollectionId}`)}>
            {t('common.cancel')}
          </Button>
          <Button type="submit" variant="primary" disabled={loading}>
            {loading ? t('common.saving') : (isEditing ? t('common.save') : t('common.create'))}
          </Button>
        </div>
      </form>
    </div>
  );
};

export default CardForm;