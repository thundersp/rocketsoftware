import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { getRoom, getRoomAvailability, deleteRoom } from '../api/rooms';
import './Pages.css';

export default function RoomDetail() {
  const { roomId } = useParams();
  const { canManageRooms } = useAuth();
  const navigate = useNavigate();
  const [room, setRoom] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [availCheck, setAvailCheck] = useState({ startUtc: '', endUtc: '', result: null });

  useEffect(() => {
    getRoom(roomId)
      .then(setRoom)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, [roomId]);

  async function checkAvailability() {
    if (!availCheck.startUtc || !availCheck.endUtc) return;
    try {
      const result = await getRoomAvailability(roomId, availCheck.startUtc, availCheck.endUtc);
      setAvailCheck({ ...availCheck, result });
    } catch (err) {
      setAvailCheck({ ...availCheck, result: { error: err.message } });
    }
  }

  async function handleDelete() {
    if (!window.confirm('Delete this room?')) return;
    try {
      await deleteRoom(roomId);
      navigate('/rooms');
    } catch (err) {
      alert(err.message);
    }
  }

  if (loading) return <div className="page-loading">Loading...</div>;
  if (error) return <div className="page"><div className="auth-error">{error}</div></div>;
  if (!room) return <div className="page"><p>Room not found.</p></div>;

  return (
    <div className="page">
      <div className="page-header">
        <h1>{room.roomName || room.roomCode}</h1>
        <div className="page-header-actions">
          {canManageRooms() && (
            <>
              <Link to={`/rooms/${roomId}/edit`} className="btn btn-outline">Edit</Link>
              <button className="btn btn-danger" onClick={handleDelete}>Delete</button>
            </>
          )}
        </div>
      </div>

      <div className="detail-grid">
        <div className="detail-card">
          <h3>Room Info</h3>
          <div className="detail-row"><span>Code:</span><span>{room.roomCode}</span></div>
          <div className="detail-row"><span>Name:</span><span>{room.roomName}</span></div>
          <div className="detail-row"><span>Capacity:</span><span>{room.capacity}</span></div>
          <div className="detail-row"><span>Floor:</span><span>{room.floor}</span></div>
          <div className="detail-row"><span>Video Room:</span><span>{room.isVideoRoom === 'Y' ? 'Yes' : 'No'}</span></div>
          <div className="detail-row"><span>Status:</span>
            <span className={`status-badge ${(room.status || '').toLowerCase()}`}>{room.status}</span>
          </div>
          {room.dialInInfo && <div className="detail-row"><span>Dial-In:</span><span>{room.dialInInfo}</span></div>}
          {room.notes && <div className="detail-row"><span>Notes:</span><span>{room.notes}</span></div>}
        </div>

        <div className="detail-card">
          <h3>Check Availability</h3>
          <div className="form-group">
            <label>Start (UTC)</label>
            <input type="datetime-local" value={availCheck.startUtc}
              onChange={e => setAvailCheck({ ...availCheck, startUtc: e.target.value, result: null })} />
          </div>
          <div className="form-group">
            <label>End (UTC)</label>
            <input type="datetime-local" value={availCheck.endUtc}
              onChange={e => setAvailCheck({ ...availCheck, endUtc: e.target.value, result: null })} />
          </div>
          <button className="btn btn-primary btn-sm" onClick={checkAvailability}>Check</button>
          {availCheck.result && (
            <div className="avail-result" style={{ marginTop: '0.75rem' }}>
              {availCheck.result.error
                ? <span className="text-error">{availCheck.result.error}</span>
                : <pre>{JSON.stringify(availCheck.result, null, 2)}</pre>}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
