import { useState, useEffect } from 'react';
import { getAssignments, getRoomAssignments } from '../api/assignments';
import { getRooms } from '../api/rooms';
import { getLocations } from '../api/lookups';
import './Pages.css';

export default function RoomAssignments() {
  const [roomAssignments, setRoomAssignments] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [locations, setLocations] = useState([]);
  const [filters, setFilters] = useState({ roomId: '', locationId: '', status: '', fromUtc: '', toUtc: '' });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getRooms(), getLocations()])
      .then(([r, l]) => { setRooms(r || []); setLocations(l || []); })
      .catch(() => {});
    loadData();
  }, []);

  async function loadData() {
    setLoading(true);
    try {
      const params = {};
      if (filters.roomId) params.roomId = filters.roomId;
      if (filters.locationId) params.locationId = filters.locationId;
      if (filters.status) params.status = filters.status;
      if (filters.fromUtc) params.fromUtc = new Date(filters.fromUtc).toISOString();
      if (filters.toUtc) params.toUtc = new Date(filters.toUtc).toISOString();
      const [data, roomData, assignmentData] = await Promise.all([
        getRoomAssignments(params),
        rooms.length > 0 ? Promise.resolve(rooms) : getRooms(),
        getAssignments({ fromUtc: params.fromUtc, toUtc: params.toUtc }),
      ]);
      const roomById = new Map((roomData || []).map((room) => [room.roomId, room]));
      const assignmentById = new Map((assignmentData || []).map((assignment) => [assignment.assignmentId, assignment]));
      setRoomAssignments(Array.isArray(data)
        ? data.map((roomAssignment) => ({
          ...roomById.get(roomAssignment.roomId),
          ...assignmentById.get(roomAssignment.assignmentId),
          ...roomAssignment,
        }))
        : []);
    } catch {
      setRoomAssignments([]);
    } finally {
      setLoading(false);
    }
  }

  function handleFilterChange(e) {
    setFilters({ ...filters, [e.target.name]: e.target.value });
  }

  function formatDateTime(utc) {
    if (!utc) return '';
    return new Date(utc).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Room Schedule</h1>
      </div>

      <div className="filters-bar">
        <select name="roomId" value={filters.roomId} onChange={handleFilterChange}>
          <option value="">All Rooms</option>
          {rooms.map(r => (
            <option key={r.roomId} value={r.roomId}>{r.roomName || r.roomCode}</option>
          ))}
        </select>
        <select name="locationId" value={filters.locationId} onChange={handleFilterChange}>
          <option value="">All Locations</option>
          {locations.map(l => (
            <option key={l.id} value={l.id}>{l.city}, {l.country}</option>
          ))}
        </select>
        <select name="status" value={filters.status} onChange={handleFilterChange}>
          <option value="">All Status</option>
          <option value="RESERVED">Reserved</option>
          <option value="CANCELLED">Cancelled</option>
          <option value="RELEASED">Released</option>
        </select>
        <input type="date" name="fromUtc" value={filters.fromUtc} onChange={handleFilterChange} />
        <input type="date" name="toUtc" value={filters.toUtc} onChange={handleFilterChange} />
        <button className="btn btn-outline btn-sm" onClick={loadData}>Apply</button>
      </div>

      {loading ? (
        <div className="page-loading">Loading...</div>
      ) : roomAssignments.length === 0 ? (
        <div className="empty-state"><p>No room assignments found.</p></div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Room</th>
                <th>Meeting</th>
                <th>Time</th>
                <th>Primary</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {roomAssignments.map((ra, i) => (
                <tr key={ra.meetingAssignmentId || i}>
                  <td>{ra.roomName || `Room ${ra.roomId}`}</td>
                  <td>{ra.meetingTitle || `Assignment ${ra.assignmentId}`}</td>
                  <td>
                    <div>{formatDateTime(ra.startUtc)}</div>
                    <div className="table-sub">to {formatDateTime(ra.endUtc)}</div>
                  </td>
                  <td>{ra.isPrimaryRoom === 'Y' ? 'Primary' : 'Secondary'}</td>
                  <td>
                    <span className={`status-badge ${(ra.status || '').toLowerCase()}`}>{ra.status}</span>
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
