import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { getRooms, deleteRoom } from '../api/rooms';
import { getLocations, getBuildings, getRoomTypes } from '../api/lookups';
import './Pages.css';

export default function Rooms() {
  const { canManageRooms } = useAuth();
  const [rooms, setRooms] = useState([]);
  const [locations, setLocations] = useState([]);
  const [buildings, setBuildings] = useState([]);
  const [roomTypes, setRoomTypes] = useState([]);
  const [filters, setFilters] = useState({ locationId: '', buildingId: '', roomTypeId: '', isVideoRoom: '' });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getLocations(), getBuildings(), getRoomTypes()])
      .then(([locs, blds, types]) => {
        setLocations(locs || []);
        setBuildings(blds || []);
        setRoomTypes(types || []);
      })
      .catch(() => {});
    loadRooms();
  }, []);

  async function loadRooms() {
    setLoading(true);
    try {
      const params = {};
      if (filters.locationId) params.locationId = filters.locationId;
      if (filters.buildingId) params.buildingId = filters.buildingId;
      if (filters.roomTypeId) params.roomTypeId = filters.roomTypeId;
      if (filters.isVideoRoom) params.isVideoRoom = filters.isVideoRoom;
      const data = await getRooms(params);
      setRooms(Array.isArray(data) ? data : []);
    } catch {
      setRooms([]);
    } finally {
      setLoading(false);
    }
  }

  function handleFilterChange(e) {
    setFilters({ ...filters, [e.target.name]: e.target.value });
  }

  async function handleDelete(roomId) {
    if (!window.confirm('Are you sure you want to delete this room?')) return;
    try {
      await deleteRoom(roomId);
      setRooms(rooms.filter(r => r.roomId !== roomId));
    } catch (err) {
      alert(err.message || 'Failed to delete room');
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Meeting Rooms</h1>
        {canManageRooms() && (
          <Link to="/rooms/new" className="btn btn-primary">+ Add Room</Link>
        )}
      </div>

      <div className="filters-bar">
        <select name="locationId" value={filters.locationId} onChange={handleFilterChange}>
          <option value="">All Locations</option>
          {locations.map(l => (
            <option key={l.id} value={l.id}>{l.city}, {l.country}</option>
          ))}
        </select>
        <select name="buildingId" value={filters.buildingId} onChange={handleFilterChange}>
          <option value="">All Buildings</option>
          {buildings.map(b => (
            <option key={b.buildingId} value={b.buildingId}>{b.buildingName}</option>
          ))}
        </select>
        <select name="roomTypeId" value={filters.roomTypeId} onChange={handleFilterChange}>
          <option value="">All Types</option>
          {roomTypes.map(t => (
            <option key={t.roomTypeId} value={t.roomTypeId}>{t.typeName}</option>
          ))}
        </select>
        <select name="isVideoRoom" value={filters.isVideoRoom} onChange={handleFilterChange}>
          <option value="">Video Room?</option>
          <option value="true">Yes</option>
          <option value="false">No</option>
        </select>
        <button className="btn btn-outline btn-sm" onClick={loadRooms}>Apply</button>
      </div>

      {loading ? (
        <div className="page-loading">Loading rooms...</div>
      ) : rooms.length === 0 ? (
        <div className="empty-state">
          <p>No rooms found. {canManageRooms() ? 'Start by adding a room.' : 'Try adjusting your filters.'}</p>
        </div>
      ) : (
        <div className="card-grid">
          {rooms.map(room => (
            <div key={room.roomId} className="room-card">
              <div className="room-card-header">
                <h3>{room.roomName || room.roomCode}</h3>
                <span className={`status-badge ${(room.status || 'active').toLowerCase()}`}>
                  {room.status || 'ACTIVE'}
                </span>
              </div>
              <div className="room-card-body">
                <div className="room-detail"><strong>Code:</strong> {room.roomCode}</div>
                <div className="room-detail"><strong>Capacity:</strong> {room.capacity}</div>
                <div className="room-detail"><strong>Floor:</strong> {room.floor}</div>
                <div className="room-detail"><strong>Video:</strong> {room.isVideoRoom === 'Y' ? '✅ Yes' : '❌ No'}</div>
                {room.buildingName && <div className="room-detail"><strong>Building:</strong> {room.buildingName}</div>}
                {room.notes && <div className="room-detail"><strong>Notes:</strong> {room.notes}</div>}
              </div>
              <div className="room-card-actions">
                <Link to={`/rooms/${room.roomId}`} className="btn btn-outline btn-sm">View</Link>
                {canManageRooms() && (
                  <>
                    <Link to={`/rooms/${room.roomId}/edit`} className="btn btn-outline btn-sm">Edit</Link>
                    <button className="btn btn-danger btn-sm" onClick={() => handleDelete(room.roomId)}>Delete</button>
                  </>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
