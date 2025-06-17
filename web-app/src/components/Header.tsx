import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const Header: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!user) {
    return null;
  }

  return (
    <header className="header">
      <div className="container">
        <h1>SPICE Medical Review</h1>
        <nav className="nav">
          <Link to="/medical-review" className="nav-link">
            Medical Review
          </Link>
          <span className="nav-link">
            Welcome, {user.firstName || user.username}
          </span>
          <button onClick={handleLogout} className="nav-link" style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
            Logout
          </button>
        </nav>
      </div>
    </header>
  );
};

export default Header;
