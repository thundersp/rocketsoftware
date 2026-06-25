import { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { getNotifications } from '../api/notifications';
import './Pages.css';

export default function Notifications() {
  const { user } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadNotifications();
  }, []);

  async function loadNotifications() {
    setLoading(true);
    try {
      const data = await getNotifications(user?.employeeId);
      setNotifications(Array.isArray(data) ? data : []);
    } catch {
      setNotifications([]);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Notifications</h1>
        <button className="btn btn-outline btn-sm" onClick={loadNotifications}>Refresh</button>
      </div>

      {loading ? (
        <div className="page-loading">Loading...</div>
      ) : notifications.length === 0 ? (
        <div className="empty-state"><p>No notifications yet.</p></div>
      ) : (
        <div className="notification-list">
          {notifications.map((n, i) => (
            <div key={n.notificationId || i} className="notification-item">
              <div className="notification-icon">🔔</div>
              <div className="notification-body">
                <div className="notification-message">{n.message || n.content || 'Notification'}</div>
                <div className="notification-meta">
                  {(n.sentAt || n.createdAt) && new Date(n.sentAt || n.createdAt).toLocaleString()}
                  {n.type && <span className="notification-type">{n.type}</span>}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
