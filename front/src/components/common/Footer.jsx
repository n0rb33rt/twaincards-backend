// src/components/common/Footer.jsx
import React from 'react';
import { Link } from 'react-router-dom';
import '../../styles/main.css';

const Footer = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="app-footer">
      <div className="footer-container">
        <div className="footer-content">
          <div className="footer-section">
            <h3 className="footer-heading">TwainCards</h3>
            <p className="footer-description">
              An interactive language learning platform to help you master vocabulary through spaced repetition.
            </p>
          </div>

          <div className="footer-section">
            <h3 className="footer-heading">Quick Links</h3>
            <ul className="footer-links">
              <li>
                <Link to="/dashboard" className="footer-link">Dashboard</Link>
              </li>
              <li>
                <Link to="/collections" className="footer-link">Collections</Link>
              </li>
              <li>
                <Link to="/study" className="footer-link">Study</Link>
              </li>
              <li>
                <Link to="/statistics" className="footer-link">Statistics</Link>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;