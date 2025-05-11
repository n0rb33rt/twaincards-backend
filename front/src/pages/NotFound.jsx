// // src/pages/NotFound.jsx
// import React from 'react';
// import { Link } from 'react-router-dom';
// import '../styles/main.css';
//
// const NotFound = () => {
//   return (
//     <div className="not-found-container">
//       <div className="not-found-content">
//         <h1 className="not-found-title">404</h1>
//         <h2 className="not-found-subtitle">Page Not Found</h2>
//         <p className="not-found-message">
//           Oops! The page you're looking for doesn't exist or has been moved.
//         </p>
//         <div className="not-found-actions">
//           <Link to="/" className="home-button">
//             Go to Homepage
//           </Link>
//           <Link to="/collections" className="collections-button">
//             Browse Collections
//           </Link>
//         </div>
//       </div>
//     </div>
//   );
// };
//
// export default NotFound;

import React from 'react';
import { Link } from 'react-router-dom';
import '../styles/main.css';

const NotFound = () => {
  return (
    <div className="not-found-container">
      <div className="not-found-content">
        <h1 className="not-found-title">404</h1>
        <h2 className="not-found-subtitle">Page Not Found</h2>
        <p className="not-found-message">
          Oops! The page you're looking for doesn't exist or has been moved.
        </p>
        <div className="not-found-actions">
          <Link to="/" className="not-found-button primary">
            Go to Homepage
          </Link>
          <Link to="/collections" className="not-found-button secondary">
            Browse Collections
          </Link>
        </div>
      </div>
    </div>
  );
};

export default NotFound;