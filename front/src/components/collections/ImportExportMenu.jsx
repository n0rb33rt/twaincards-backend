import React, { useState, useRef } from 'react';
import { toast } from 'react-toastify';
import * as exportApi from '../../api/exportApi';
import * as importApi from '../../api/importApi';
import '../../styles/importexport.css';

const ImportExportMenu = ({ collectionId, cardsCount, onImportSuccess }) => {
  const [isExporting, setIsExporting] = useState(false);
  const [isImporting, setIsImporting] = useState(false);
  const fileInputRef = useRef(null);
  
  const hasCards = cardsCount > 0;

  // Handle export CSV
  const handleExportCSV = async () => {
    if (!hasCards) {
      toast.error('Cannot export an empty collection. Add some cards first.');
      return;
    }

    try {
      setIsExporting(true);
      const exportedData = await exportApi.exportCollectionToCsv(collectionId);
      const filename = `collection_${collectionId}.csv`;
      
      exportApi.downloadExportedFile(exportedData, filename);
      toast.success('Collection exported as CSV successfully');
    } catch (error) {
      console.error('Export error:', error);
      toast.error(`Failed to export collection: ${error.message}`);
    } finally {
      setIsExporting(false);
    }
  };

  // Handle file selection for import
  const handleFileSelect = () => {
    fileInputRef.current.click();
  };

  // Handle file upload
  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    
    if (!file) return;

    try {
      setIsImporting(true);
      const importedCollection = await importApi.importCollectionFromCsv(file);

      if (!importedCollection || 
          (!importedCollection.cardCount && !importedCollection.cardsCount)) {
        toast.warning('The imported collection has no cards.');
      } else {
        toast.success('Collection imported successfully from CSV');
      }
      
      // Reset the file input
      event.target.value = '';
      
      // Call the success callback if provided
      if (onImportSuccess && importedCollection) {
        onImportSuccess(importedCollection);
      }
    } catch (error) {
      console.error('Import error:', error);
      toast.error(`Failed to import collection: ${error.message}`);
    } finally {
      setIsImporting(false);
    }
  };

  return (
    <div className="import-export-container">
      {/* Hidden file input for import */}
      <input
        type="file"
        ref={fileInputRef}
        style={{ display: 'none' }}
        onChange={handleFileUpload}
        accept=".csv"
      />
      
      <button 
        className="export-btn"
        onClick={handleExportCSV}
        disabled={!hasCards || isExporting}
      >
        {isExporting ? 'Exporting...' : 'Export CSV'}
      </button>
      
      <button 
        className="import-btn"
        onClick={handleFileSelect}
        disabled={isImporting}
      >
        {isImporting ? 'Importing...' : 'Import CSV'}
      </button>
    </div>
  );
};

export default ImportExportMenu; 