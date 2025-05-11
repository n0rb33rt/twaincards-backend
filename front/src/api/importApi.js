// src/api/importApi.js
import { http } from '../utils/axiosConfig';

/**
 * Import a collection from a CSV file
 * @param {File} file - The CSV file to import
 * @returns {Promise<Object>} - A promise that resolves to the imported collection data
 */
export const importCollectionFromCsv = async (file) => {
  try {
    const formData = new FormData();
    formData.append('file', file);
    
    const response = await http.post('/api/import/collection/csv', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    
    return response.data;
  } catch (error) {
    console.error('Error importing collection from CSV:', error);
    throw new Error('Failed to import collection from CSV');
  }
};

/**
 * Import a collection from a JSON file
 * @param {File} file - The JSON file to import
 * @returns {Promise<Object>} - A promise that resolves to the imported collection data
 */
export const importCollectionFromJson = async (file) => {
  try {
    const formData = new FormData();
    formData.append('file', file);
    
    const response = await http.post('/api/import/collection/json', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    
    return response.data;
  } catch (error) {
    console.error('Error importing collection from JSON:', error);
    throw new Error('Failed to import collection from JSON');
  }
};

/**
 * Import a collection from an XML file
 * @param {File} file - The XML file to import
 * @returns {Promise<Object>} - A promise that resolves to the imported collection data
 */
export const importCollectionFromXml = async (file) => {
  try {
    const formData = new FormData();
    formData.append('file', file);
    
    const response = await http.post('/api/import/collection/xml', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    
    return response.data;
  } catch (error) {
    console.error('Error importing collection from XML:', error);
    throw new Error('Failed to import collection from XML');
  }
}; 