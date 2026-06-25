import { useState, useEffect } from 'react';
import { getAuditLogs } from '../api/notifications';
import './Pages.css';

export default function AuditLogs() {
  const [logs, setLogs] = useState([]);
  const [filters, setFilters] = useState({ entityType: '', entityId: '' });
  const [loading, setLoading] = useState(true);

  useEffect(() => { loadLogs(); }, []);

  async function loadLogs() {
    setLoading(true);
    try {
      const params = {};
      if (filters.entityType) params.entityType = filters.entityType;
      if (filters.entityId) params.entityId = filters.entityId;
      const data = await getAuditLogs(params);
      setLogs(Array.isArray(data) ? data : []);
    } catch {
      setLogs([]);
    } finally {
      setLoading(false);
    }
  }

  function handleFilterChange(e) {
    setFilters({ ...filters, [e.target.name]: e.target.value });
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Audit Logs</h1>
      </div>

      <div className="filters-bar">
        <select name="entityType" value={filters.entityType} onChange={handleFilterChange}>
          <option value="">All Entity Types</option>
          <option value="ASSIGNMENT">Assignment</option>
          <option value="ROOM">Room</option>
          <option value="ROOM_ASSIGNMENT">Room Assignment</option>
          <option value="VIDEO_RESERVATION">Video Reservation</option>
        </select>
        <input name="entityId" value={filters.entityId} onChange={handleFilterChange} placeholder="Entity ID" type="number" />
        <button className="btn btn-outline btn-sm" onClick={loadLogs}>Apply</button>
      </div>

      {loading ? (
        <div className="page-loading">Loading...</div>
      ) : logs.length === 0 ? (
        <div className="empty-state"><p>No audit logs found.</p></div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Timestamp</th>
                <th>Entity</th>
                <th>Action</th>
                <th>Changed By</th>
                <th>Details</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log, i) => (
                <tr key={log.auditLogId || i}>
                  <td>{log.createdAt ? new Date(log.createdAt).toLocaleString() : '—'}</td>
                  <td>
                    <span className="entity-tag">{log.entityType}</span>
                    <span className="table-sub"> #{log.entityId}</span>
                  </td>
                  <td>{log.action || log.changeType || '—'}</td>
                  <td>{log.changedBy || log.employeeName || '—'}</td>
                  <td className="audit-details">
                    {log.details || log.description || log.newValues || log.oldValues || '—'}
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
