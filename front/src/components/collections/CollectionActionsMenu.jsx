import React, { useState, useRef } from 'react';
import { toast } from 'react-toastify';
import { Link } from 'react-router-dom';
import * as exportApi from '../../api/exportApi';
import '../../styles/importexport.css';
import '../../styles/collections.css';
import '../../styles/actionMenu.css';

const CollectionActionsMenu = ({ collection, onDelete }) => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const menuRef = useRef(null);
  
  const hasCards = (collection.cardCount > 0 || collection.cardsCount > 0);

  // Open/close the dropdown menu
  const toggleMenu = () => {
    setIsMenuOpen(!isMenuOpen);
  };

  // Close the dropdown when clicking outside
  React.useEffect(() => {
    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setIsMenuOpen(false);
      }
    };

    if (isMenuOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isMenuOpen]);

  // Handle exporting collection in CSV format
  const handleExport = async () => {
    if (!hasCards) {
      toast.error('Cannot export an empty collection. Add some cards first.');
      setIsMenuOpen(false);
      return;
    }

    try {
      setIsExporting(true);
      const exportedData = await exportApi.exportCollectionToCsv(collection.id);
      
      // Create a sanitized filename from the collection name
      const sanitizedName = collection.name
        .replace(/[^a-z0-9]/gi, '_') // Replace non-alphanumeric with underscore
        .toLowerCase()
        .replace(/_+/g, '_') // Replace multiple underscores with a single one
        .replace(/^_|_$/g, ''); // Remove leading/trailing underscores
      
      const filename = `${sanitizedName}_exported.csv`;
      
      exportApi.downloadExportedFile(exportedData, filename);
      toast.success('Collection exported as CSV successfully');
    } catch (error) {
      console.error('Export error:', error);
      toast.error(`Failed to export collection: ${error.message}`);
    } finally {
      setIsExporting(false);
      setIsMenuOpen(false);
    }
  };

  return (
    <div className="collection-actions-menu" ref={menuRef}>
      <button className="menu-trigger" onClick={toggleMenu}>
        <span className="menu-dots">⋮</span>
      </button>
      
      {isMenuOpen && (
        <div className="actions-dropdown">
          <Link to={`/collections/${collection.id}`} className="action-item">
            View
          </Link>
          
          <Link to={`/collections/${collection.id}/edit`} className="action-item">
            Edit
          </Link>
          
          <button 
            onClick={handleExport} 
            className="action-item"
            disabled={!hasCards || isExporting}
          >
            Export CSV
          </button>
          
          <button 
            onClick={() => {
              if (onDelete) {
                onDelete(collection.id);
                setIsMenuOpen(false);
              }
            }} 
            className="action-item delete-action"
          >
            Delete
          </button>
        </div>
      )}
    </div>
  );
};

export default CollectionActionsMenu; 