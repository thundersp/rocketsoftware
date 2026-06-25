import { NavLink } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './Sidebar.css';

export default function Sidebar() {
  const { isManager, canManageRooms } = useAuth();

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <span className="sidebar-logo">📅</span>
        <span className="sidebar-title">BuzzMeet</span>
      </div>

      <nav className="sidebar-nav">
        <div className="nav-section">
          <NavLink to="/dashboard" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <span className="nav-icon">🏠</span>
            <span>Home</span>
          </NavLink>
        </div>

        <div className="nav-section">
          <div className="nav-section-label">Meetings</div>
          <NavLink to="/assignments" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <span className="nav-icon">📋</span>
            <span>All Meetings</span>
          </NavLink>
          <NavLink to="/assignments/new" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <span className="nav-icon">➕</span>
            <span>Schedule Meeting</span>
          </NavLink>
        </div>

        <div className="nav-section">
          <div className="nav-section-label">Rooms</div>
          <NavLink to="/rooms" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <span className="nav-icon">🚪</span>
            <span>Browse Rooms</span>
          </NavLink>
          {canManageRooms() && (
            <NavLink to="/rooms/new" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <span className="nav-icon">➕</span>
              <span>Add Room</span>
            </NavLink>
          )}
        </div>

        <div className="nav-section">
          <div className="nav-section-label">People</div>
          <NavLink to="/employees" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <span className="nav-icon">👥</span>
            <span>Employees</span>
          </NavLink>
        </div>

        <div className="nav-section">
          <div className="nav-section-label">Schedule</div>
          <NavLink to="/room-assignments" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <span className="nav-icon">📅</span>
            <span>Room Schedule</span>
          </NavLink>
        </div>

        <div className="nav-section">
          <div className="nav-section-label">Admin</div>
          <NavLink to="/notifications" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <span className="nav-icon">🔔</span>
            <span>Notifications</span>
          </NavLink>
          {isManager() && (
            <NavLink to="/audit-logs" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <span className="nav-icon">📜</span>
              <span>Audit Logs</span>
            </NavLink>
          )}
        </div>
      </nav>
    </aside>
  );
}
