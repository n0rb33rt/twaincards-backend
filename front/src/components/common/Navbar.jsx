import React, { useContext } from 'react';
import { Link } from 'react-router-dom';
import { NavDropdown } from 'react-bootstrap';
import { UserContext } from '../../contexts/UserContext';

const Navbar = () => {
  const { user } = useContext(UserContext);

  return (
    <NavDropdown title="User">
      {user && user.role === 'ADMIN' && (
        <NavDropdown.Item as={Link} to="/admin">
          Admin Dashboard
        </NavDropdown.Item>
      )}
    </NavDropdown>
  );
};

export default Navbar; 