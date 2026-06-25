import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getRoom, createRoom, updateRoom } from '../api/rooms';
import { getBuildings, getRoomTypes } from '../api/lookups';
import './Pages.css';

export default function RoomForm() {
  const { roomId } = useParams();
  const isEdit = Boolean(roomId);
  const navigate = useNavigate();

  const [form, setForm] = useState({
    buildingId: '',
    roomTypeId: '',
    roomCode: '',
    roomName: '',
    capacity: '',
    floor: '',
    isVideoRoom: 'N',
    dialInInfo: '',
    status: 'ACTIVE',
    notes: '',
  });
  const [buildings, setBuildings] = useState([]);
  const [roomTypes, setRoomTypes] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    Promise.all([getBuildings(), getRoomTypes()])
      .then(([blds, types]) => {
        setBuildings(blds || []);
        setRoomTypes(types || []);
      })
      .catch(() => {});
    if (isEdit) {
      getRoom(roomId)
        .then(data => {
          setForm({
            buildingId: data.buildingId || '',
            roomTypeId: data.roomTypeId || '',
            roomCode: data.roomCode || '',
            roomName: data.roomName || '',
            capacity: data.capacity || '',
            floor: data.floor || '',
            isVideoRoom: data.isVideoRoom || 'N',
            dialInInfo: data.dialInInfo || '',
            status: data.status || 'ACTIVE',
            notes: data.notes || '',
          });
        })
        .catch(err => setError(err.message));
    }
  }, [roomId]);

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const payload = { ...form, capacity: Number(form.capacity), floor: Number(form.floor) };
      if (isEdit) {
        await updateRoom(roomId, payload);
      } else {
        await createRoom(payload);
      }
      navigate('/rooms');
    } catch (err) {
      setError(err.message || 'Failed to save room');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>{isEdit ? 'Edit Room' : 'Add Room'}</h1>
      </div>

      <div className="form-card">
        {error && <div className="auth-error">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-row">
            <div className="form-group">
              <label>Building</label>
              <select name="buildingId" value={form.buildingId} onChange={handleChange} required>
                <option value="">Select Building</option>
                {buildings.map(b => (
                  <option key={b.buildingId} value={b.buildingId}>{b.buildingName}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>Room Type</label>
              <select name="roomTypeId" value={form.roomTypeId} onChange={handleChange} required>
                <option value="">Select Type</option>
                {roomTypes.map(t => (
                  <option key={t.roomTypeId} value={t.roomTypeId}>{t.typeName}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Room Code</label>
              <input name="roomCode" value={form.roomCode} onChange={handleChange} placeholder="e.g. DAL-410" required />
            </div>
            <div className="form-group">
              <label>Room Name</label>
              <input name="roomName" value={form.roomName} onChange={handleChange} placeholder="e.g. Mockingbird Conference" required />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Capacity</label>
              <input name="capacity" type="number" min="1" value={form.capacity} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label>Floor</label>
              <input name="floor" type="number" value={form.floor} onChange={handleChange} required />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Video Room</label>
              <select name="isVideoRoom" value={form.isVideoRoom} onChange={handleChange}>
                <option value="Y">Yes</option>
                <option value="N">No</option>
              </select>
            </div>
            <div className="form-group">
              <label>Status</label>
              <select name="status" value={form.status} onChange={handleChange}>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
                <option value="MAINTENANCE">Maintenance</option>
              </select>
            </div>
          </div>

          <div className="form-group">
            <label>Dial-In Info</label>
            <input name="dialInInfo" value={form.dialInInfo} onChange={handleChange} placeholder="Dial-in details" />
          </div>

          <div className="form-group">
            <label>Notes</label>
            <textarea name="notes" value={form.notes} onChange={handleChange} rows="3" placeholder="Additional notes" />
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Saving...' : isEdit ? 'Update Room' : 'Create Room'}
            </button>
            <button type="button" className="btn btn-outline" onClick={() => navigate('/rooms')}>Cancel</button>
          </div>
        </form>
      </div>
    </div>
  );
}
