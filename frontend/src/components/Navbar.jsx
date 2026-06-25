import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import './Navbar.css';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/');
  }

  return (
    <header className="navbar">
      <div className="navbar-left">
        <h2 className="navbar-greeting">
          Welcome, {user?.firstName || user?.email || 'User'}
        </h2>
      </div>
      <div className="navbar-right">
        <div className="navbar-roles">
          {user?.roles?.map((role) => (
            <span key={role} className={`role-tag ${role.toLowerCase().replace('role_', '')}`}>
              {role.replace('ROLE_', '')}
            </span>
          ))}
        </div>
        <div className="navbar-user">
          <div className="user-avatar">
            {(user?.firstName?.[0] || 'U')}{(user?.lastName?.[0] || '')}
          </div>
          <button className="btn btn-outline btn-sm" onClick={handleLogout}>
            Sign Out
          </button>
        </div>
      </div>
    </header>
  );
}
