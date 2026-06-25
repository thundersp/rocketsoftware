import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { getAssignments } from '../api/assignments';
import './Pages.css';

const STATUS_COLORS = {
  ACTIVE: 'green', APPROVED: 'green', CONFIRMED: 'green',
  PENDING: 'yellow',
  CANCELLED: 'red',
  OVERRIDDEN: 'blue', RESCHEDULED: 'blue',
};

export default function Assignments() {
  const { isOrganizer } = useAuth();
  const [assignments, setAssignments] = useState([]);
  const [filters, setFilters] = useState({ status: '', priority: '', fromUtc: '', toUtc: '' });
  const [loading, setLoading] = useState(true);

  useEffect(() => { loadAssignments(); }, []);

  async function loadAssignments() {
    setLoading(true);
    try {
      const params = {};
      if (filters.status) params.status = filters.status;
      if (filters.priority) params.priority = filters.priority;
      if (filters.fromUtc) params.fromUtc = new Date(filters.fromUtc).toISOString();
      if (filters.toUtc) params.toUtc = new Date(filters.toUtc).toISOString();
      const data = await getAssignments(params);
      setAssignments(Array.isArray(data) ? data : []);
    } catch {
      setAssignments([]);
    } finally {
      setLoading(false);
    }
  }

  function handleFilterChange(e) {
    setFilters({ ...filters, [e.target.name]: e.target.value });
  }

  function formatDateTime(utc) {
    if (!utc) return '';
    return new Date(utc).toLocaleString(undefined, {
      month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Meetings</h1>
        <Link to="/assignments/new" className="btn btn-primary">+ Schedule Meeting</Link>
      </div>

      <div className="filters-bar">
        <select name="status" value={filters.status} onChange={handleFilterChange}>
          <option value="">All Status</option>
          <option value="SCHEDULED">Scheduled</option>
          <option value="DRAFT">Draft</option>
          <option value="CANCELLED">Cancelled</option>
          <option value="COMPLETED">Completed</option>
          <option value="OVERRIDDEN">Overridden</option>
        </select>
        <select name="priority" value={filters.priority} onChange={handleFilterChange}>
          <option value="">All Priority</option>
          <option value="URGENT">Urgent</option>
          <option value="HIGH">High</option>
          <option value="NORMAL">Normal</option>
          <option value="LOW">Low</option>
        </select>
        <input type="date" name="fromUtc" value={filters.fromUtc} onChange={handleFilterChange} placeholder="From" />
        <input type="date" name="toUtc" value={filters.toUtc} onChange={handleFilterChange} placeholder="To" />
        <button className="btn btn-outline btn-sm" onClick={loadAssignments}>Apply</button>
      </div>

      {loading ? (
        <div className="page-loading">Loading meetings...</div>
      ) : assignments.length === 0 ? (
        <div className="empty-state">
          <p>No meetings found. {isOrganizer() ? 'Schedule your first meeting!' : 'Check back later.'}</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Date & Time</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {assignments.map(a => (
                <tr key={a.assignmentId}>
                  <td>
                    <Link to={`/assignments/${a.assignmentId}`} className="table-link">
                      {a.meetingTitle || 'Untitled Meeting'}
                    </Link>
                    {a.description && <div className="table-sub">{a.description}</div>}
                  </td>
                  <td>
                    <div>{formatDateTime(a.startUtc)}</div>
                    <div className="table-sub">to {formatDateTime(a.endUtc)}</div>
                  </td>
                  <td>
                    {a.priority && (
                      <span className={`priority-tag ${a.priority.toLowerCase()}`}>{a.priority}</span>
                    )}
                  </td>
                  <td>
                    <span className={`status-dot ${STATUS_COLORS[a.status?.toUpperCase()] || 'yellow'}`}></span>
                    {a.status || 'PENDING'}
                  </td>
                  <td>
                    <Link to={`/assignments/${a.assignmentId}`} className="btn btn-outline btn-sm">View</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
