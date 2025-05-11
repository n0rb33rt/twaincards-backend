// src/components/common/Header.jsx
import React, { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import '../../styles/header.css';

const Header = () => {
  const { currentUser, isAuthenticated, logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);
  const navigate = useNavigate();
  
  // Check if user is admin
  const isAdmin = currentUser?.role === 'ADMIN';

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setDropdownOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const toggleMobileMenu = () => {
    setMobileMenuOpen(!mobileMenuOpen);
  };

  const toggleDropdown = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDropdownOpen(!dropdownOpen);
  };

  // Force close dropdown when route changes
  useEffect(() => {
    setDropdownOpen(false);
  }, [navigate]);

  return (
    <header className="app-header">
      <div className="header-container">
        <div className="logo-container">
          <Link to="/" className="logo">
            <span className="logo-text">TwainCards</span>
          </Link>
        </div>

        <button
          className={`mobile-menu-button ${mobileMenuOpen ? 'active' : ''}`}
          onClick={toggleMobileMenu}
          aria-label="Toggle menu"
        >
          <span className="menu-icon"></span>
        </button>

        <nav className={`main-nav ${mobileMenuOpen ? 'mobile-open' : ''}`}>
          <ul className="nav-links">
            {isAuthenticated ? (
              <>
                <li className="nav-item">
                  <Link to="/dashboard" className="nav-link">Dashboard</Link>
                </li>
                <li className="nav-item">
                  <Link to="/collections" className="nav-link">Collections</Link>
                </li>
                <li className="nav-item">
                  <Link to="/study" className="nav-link">Study</Link>
                </li>
                <li className="nav-item">
                  <Link to="/statistics" className="nav-link">Statistics</Link>
                </li>
                {/*{isAdmin && (*/}
                {/*  <li className="nav-item">*/}
                {/*    <Link to="/admin/languages" className="nav-link">Languages</Link>*/}
                {/*  </li>*/}
                {/*)}*/}
                <li className="nav-item dropdown" ref={dropdownRef}>
                  <button 
                    className="dropdown-toggle" 
                    onClick={toggleDropdown}
                    aria-expanded={dropdownOpen}
                  >
                    {currentUser?.username || 'Account'}
                  </button>
                  <div className={`dropdown-menu ${dropdownOpen ? 'show' : ''}`}>
                    <Link to="/profile" className="dropdown-item">My Profile</Link>
                    <Link to="/security" className="dropdown-item">Security</Link>
                    {isAdmin && (
                      <Link to="/admin" className="dropdown-item">Admin Dashboard</Link>
                    )}
                    <div className="dropdown-divider"></div>
                    <button onClick={handleLogout} className="dropdown-item">
                      Logout
                    </button>
                  </div>
                </li>
              </>
            ) : (
              <>
                <li className="nav-item">
                  <Link to="/login" className="nav-link">Login</Link>
                </li>
                <li className="nav-item">
                  <Link to="/register" className="nav-link">Register</Link>
                </li>
              </>
            )}
          </ul>
        </nav>
      </div>
    </header>
  );
};

export default Header;