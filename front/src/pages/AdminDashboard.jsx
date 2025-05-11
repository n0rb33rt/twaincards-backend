import React, { useState, useEffect } from 'react';
import { Tabs, Tab, Container, Button, Modal, Form, FormControl, Table, Alert, InputGroup } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import axios from '../utils/axiosConfig';
import { useAuth } from '../context/AuthContext';

const AdminDashboard = ({ activeTab: initialActiveTab = 'languages' }) => {
  const [activeTab, setActiveTab] = useState(initialActiveTab);
  const [languages, setLanguages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [modalMode, setModalMode] = useState('create');
  const [currentLanguage, setCurrentLanguage] = useState({ id: null, code: '', name: '', nativeName: '' });
  const [searchQuery, setSearchQuery] = useState('');
  const [validationErrors, setValidationErrors] = useState({});
  const navigate = useNavigate();
  const { currentUser, isAuthenticated, loading: authLoading } = useAuth();

  // Update active tab when initialActiveTab prop changes
  useEffect(() => {
    setActiveTab(initialActiveTab);
  }, [initialActiveTab]);

  // Check if user is an admin
  useEffect(() => {
    if (authLoading) {
      return;
    }
    
    if (!currentUser) {
      navigate('/login');
      return;
    }
    
    if (currentUser.role !== 'ADMIN') {
      navigate('/dashboard');
    }
  }, [currentUser, navigate, authLoading]);

  // Fetch languages
  useEffect(() => {
    if (activeTab === 'languages') {
      fetchLanguages();
    }
  }, [activeTab]);

  const fetchLanguages = async () => {
    try {
      setLoading(true);
      const response = await axios.get('/api/languages');
      setLanguages(response.data);
      setError('');
    } catch (err) {
      setError('Failed to load languages: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  const searchLanguages = async (query) => {
    if (!query.trim()) {
      fetchLanguages();
      return;
    }
    
    try {
      setLoading(true);
      const response = await axios.get(`/api/languages/search?query=${encodeURIComponent(query)}`);
      setLanguages(response.data);
      setError('');
    } catch (err) {
      setError('Search failed: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    searchLanguages(searchQuery);
  };

  const handleSearchChange = (e) => {
    setSearchQuery(e.target.value);
    if (e.target.value === '') {
      fetchLanguages();
    }
  };

  const handleShowModal = (mode, language = { id: null, code: '', name: '', nativeName: '' }) => {
    setModalMode(mode);
    setCurrentLanguage(language);
    setValidationErrors({}); // Clear validation errors when opening modal
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setCurrentLanguage({ id: null, code: '', name: '', nativeName: '' });
    setValidationErrors({});
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setCurrentLanguage({ ...currentLanguage, [name]: value });
    
    // Clear validation error for this field when user changes it
    if (validationErrors[name]) {
      setValidationErrors({
        ...validationErrors,
        [name]: undefined
      });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setValidationErrors({});
      
      if (modalMode === 'create') {
        await axios.post('/api/languages', currentLanguage);
      } else {
        await axios.put(`/api/languages/${currentLanguage.id}`, currentLanguage);
      }
      fetchLanguages();
      handleCloseModal();
    } catch (err) {
      if (err.response?.status === 400 && err.response?.data?.errors) {
        // Handle validation errors returned from the server
        setValidationErrors(err.response.data.errors);
      } else {
        setError('Failed to save language: ' + (err.response?.data?.message || err.message));
      }
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this language?')) {
      try {
        await axios.delete(`/api/languages/${id}`);
        fetchLanguages();
      } catch (err) {
        setError('Failed to delete language: ' + (err.response?.data?.message || err.message));
      }
    }
  };

  return (
    <Container className="py-4">
      <h1 className="mb-4">Admin Dashboard</h1>
      
      <Tabs
        activeKey={activeTab}
        onSelect={(k) => setActiveTab(k)}
        className="mb-4"
      >
        <Tab eventKey="languages" title="Languages">
          <div className="d-flex justify-content-between mb-3">
            <h2>Language Management</h2>
            <Button variant="primary" onClick={() => handleShowModal('create')}>
              Add New Language
            </Button>
          </div>

          {/* Search bar */}
          <Form onSubmit={handleSearch} className="mb-4">
            <InputGroup>
              <FormControl
                placeholder="Search languages by name..."
                value={searchQuery}
                onChange={handleSearchChange}
                aria-label="Search languages"
              />
              <Button variant="outline-secondary" type="submit">
                Search
              </Button>
              {searchQuery && (
                <Button 
                  variant="outline-danger" 
                  onClick={() => {
                    setSearchQuery('');
                    fetchLanguages();
                  }}
                >
                  Clear
                </Button>
              )}
            </InputGroup>
          </Form>

          {error && <Alert variant="danger">{error}</Alert>}

          {loading ? (
            <p>Loading languages...</p>
          ) : languages.length === 0 ? (
            <Alert variant="info">
              {searchQuery ? 'No languages found matching your search.' : 'No languages available.'}
            </Alert>
          ) : (
            <Table striped bordered hover>
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Name</th>
                  <th>Native Name</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {languages.map((language) => (
                  <tr key={language.id}>
                    <td>{language.code}</td>
                    <td>{language.name}</td>
                    <td>{language.nativeName}</td>
                    <td>
                      <Button
                        variant="outline-primary"
                        size="sm"
                        className="me-2"
                        onClick={() => handleShowModal('edit', language)}
                      >
                        Edit
                      </Button>
                      <Button
                        variant="outline-danger"
                        size="sm"
                        onClick={() => handleDelete(language.id)}
                      >
                        Delete
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </Tab>
      </Tabs>

      {/* Modal for creating/editing languages */}
      <Modal show={showModal} onHide={handleCloseModal}>
        <Modal.Header closeButton>
          <Modal.Title>
            {modalMode === 'create' ? 'Add New Language' : 'Edit Language'}
          </Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleSubmit}>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label>Language Code</Form.Label>
              <FormControl
                type="text"
                name="code"
                value={currentLanguage.code}
                onChange={handleInputChange}
                disabled={modalMode === 'edit'}
                required
                maxLength={10}
                placeholder="e.g., en, fr, es"
                isInvalid={!!validationErrors.code}
              />
              <Form.Text className="text-muted">
                Language code (up to 10 characters). Common formats include ISO 639-1 (2 letters) or custom codes. Cannot be changed after creation.
              </Form.Text>
              {validationErrors.code && (
                <Form.Control.Feedback type="invalid">
                  {validationErrors.code}
                </Form.Control.Feedback>
              )}
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Display Name</Form.Label>
              <FormControl
                type="text"
                name="name"
                value={currentLanguage.name}
                onChange={handleInputChange}
                required
                maxLength={50}
                placeholder="e.g., English, French, Spanish"
                isInvalid={!!validationErrors.name}
              />
              {validationErrors.name && (
                <Form.Control.Feedback type="invalid">
                  {validationErrors.name}
                </Form.Control.Feedback>
              )}
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Native Name</Form.Label>
              <FormControl
                type="text"
                name="nativeName"
                value={currentLanguage.nativeName}
                onChange={handleInputChange}
                maxLength={50}
                placeholder="e.g., English, Français, Español"
                isInvalid={!!validationErrors.nativeName}
              />
              <Form.Text className="text-muted">
                The name of the language in the language itself.
              </Form.Text>
              {validationErrors.nativeName && (
                <Form.Control.Feedback type="invalid">
                  {validationErrors.nativeName}
                </Form.Control.Feedback>
              )}
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={handleCloseModal}>
              Cancel
            </Button>
            <Button variant="primary" type="submit">
              {modalMode === 'create' ? 'Create Language' : 'Update Language'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </Container>
  );
};

export default AdminDashboard; 