import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './Landing.css';

export default function Landing() {
  const { user } = useAuth();

  return (
    <div className="landing">
      <nav className="landing-nav">
        <div className="landing-nav-brand">
          <span className="brand-icon">📅</span>
          <span className="brand-name">BuzzMeet</span>
        </div>
        <div className="landing-nav-links">
          {user ? (
            <Link to="/dashboard" className="btn btn-primary">Dashboard</Link>
          ) : (
            <>
              <Link to="/login" className="btn btn-outline">Sign In</Link>
              <Link to="/register" className="btn btn-primary">Get Started</Link>
            </>
          )}
        </div>
      </nav>

      <section className="hero-section">
        <div className="hero-content">
          <h1>Schedule Meetings <span className="highlight">Effortlessly</span></h1>
          <p className="hero-subtitle">
            BuzzMeet streamlines meeting room booking, video conferencing setup,
            and participant coordination across your entire organization.
          </p>
          <div className="hero-actions">
            <Link to={user ? '/dashboard' : '/register'} className="btn btn-primary btn-lg">
              {user ? 'Go to Dashboard' : 'Start Scheduling'}
            </Link>
            <a href="#features" className="btn btn-outline btn-lg">Learn More</a>
          </div>
        </div>
        <div className="hero-visual">
          <div className="calendar-preview">
            <div className="cal-header">
              <span>July 2026</span>
              <div className="cal-dots">
                <span className="dot green"></span>
                <span className="dot blue"></span>
                <span className="dot orange"></span>
              </div>
            </div>
            <div className="cal-grid">
              {['Mon','Tue','Wed','Thu','Fri','Sat','Sun'].map(d => (
                <div key={d} className="cal-day-name">{d}</div>
              ))}
              {Array.from({length: 31}, (_, i) => (
                <div key={i} className={`cal-day ${[3,7,12,15,20,25].includes(i+1) ? 'has-event' : ''}`}>
                  {i + 1}
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section id="features" className="features-section">
        <h2>Everything You Need</h2>
        <div className="features-grid">
          <div className="feature-card">
            <div className="feature-icon">🏢</div>
            <h3>Room Management</h3>
            <p>Browse and book conference rooms across multiple locations and buildings with real-time availability.</p>
          </div>
          <div className="feature-card">
            <div className="feature-icon">📹</div>
            <h3>Video Conferencing</h3>
            <p>Link video reservations to meetings with dial-in info, connection links, and multi-timezone support.</p>
          </div>
          <div className="feature-card">
            <div className="feature-icon">👥</div>
            <h3>Participant Coordination</h3>
            <p>Add participants, assign roles, and manage approvals for restricted room types seamlessly.</p>
          </div>
          <div className="feature-card">
            <div className="feature-icon">🔒</div>
            <h3>Role-Based Access</h3>
            <p>Employees, organizers, approvers, and admins each see features appropriate to their role.</p>
          </div>
          <div className="feature-card">
            <div className="feature-icon">🌍</div>
            <h3>Multi-Location</h3>
            <p>Manage rooms and meetings across global offices with timezone-aware scheduling.</p>
          </div>
          <div className="feature-card">
            <div className="feature-icon">📊</div>
            <h3>Audit & Notifications</h3>
            <p>Full audit trail and notification history for every meeting action taken in the system.</p>
          </div>
        </div>
      </section>

      <section className="roles-section">
        <h2>Built for Every Role</h2>
        <div className="roles-grid">
          <div className="role-card">
            <div className="role-badge employee">Employee</div>
            <ul>
              <li>View your meeting schedule</li>
              <li>Browse available rooms</li>
              <li>Check meeting details</li>
            </ul>
          </div>
          <div className="role-card">
            <div className="role-badge manager">Manager / Organizer</div>
            <ul>
              <li>Create and manage meetings</li>
              <li>Book rooms and add participants</li>
              <li>Set up video reservations</li>
              <li>Cancel or reschedule meetings</li>
            </ul>
          </div>
          <div className="role-card">
            <div className="role-badge admin">Admin</div>
            <ul>
              <li>Full room CRUD management</li>
              <li>Override any assignment</li>
              <li>Global cancel capabilities</li>
              <li>View audit logs</li>
            </ul>
          </div>
        </div>
      </section>

      <footer className="landing-footer">
        <p>&copy; 2026 BuzzMeet by Buzzword Solutions. All rights reserved.</p>
      </footer>
    </div>
  );
}
