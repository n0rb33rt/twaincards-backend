// src/api/exportApi.js
import { http, downloadBlob } from '../utils/axiosConfig';

/**
 * Export a collection to CSV format
 * @param {number} collectionId - The ID of the collection to export
 * @returns {Promise<Blob>} - A promise that resolves to a Blob containing the CSV data
 */
export const exportCollectionToCsv = async (collectionId) => {
  try {
    const response = await http.get(`/api/export/collection/${collectionId}/csv`, {
      responseType: 'blob'
    });
    return response.data;
  } catch (error) {
    console.error('Error exporting collection to CSV:', error);
    throw new Error('Failed to export collection to CSV');
  }
};

/**
 * Export a collection to JSON format
 * @param {number} collectionId - The ID of the collection to export
 * @returns {Promise<Blob>} - A promise that resolves to a Blob containing the JSON data
 */
export const exportCollectionToJson = async (collectionId) => {
  try {
    const response = await http.get(`/api/export/collection/${collectionId}/json`, {
      responseType: 'blob'
    });
    return response.data;
  } catch (error) {
    console.error('Error exporting collection to JSON:', error);
    throw new Error('Failed to export collection to JSON');
  }
};

/**
 * Export a collection to XML format
 * @param {number} collectionId - The ID of the collection to export
 * @returns {Promise<Blob>} - A promise that resolves to a Blob containing the XML data
 */
export const exportCollectionToXml = async (collectionId) => {
  try {
    const response = await http.get(`/api/export/collection/${collectionId}/xml`, {
      responseType: 'blob'
    });
    return response.data;
  } catch (error) {
    console.error('Error exporting collection to XML:', error);
    throw new Error('Failed to export collection to XML');
  }
};

/**
 * Helper function to download exported data as a file
 * @param {Blob} data - The data to download
 * @param {string} filename - The name of the file to download
 */
export const downloadExportedFile = (data, filename) => {
  downloadBlob(data, filename);
}; 