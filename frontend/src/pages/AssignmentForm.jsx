import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import {
  addParticipant as addParticipantToAssignment,
  addRoomToAssignment,
  addVideoReservation,
  createAssignment,
  getAssignmentRoomAssignments,
  getAssignments,
} from '../api/assignments';
import { getRooms } from '../api/rooms';
import { getEmployees, getTimeZones, getLocations } from '../api/lookups';
import './Pages.css';

export default function AssignmentForm() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    organizerId: user?.employeeId || '',
    meetingTitle: '',
    description: '',
    startUtc: '',
    endUtc: '',
    secondaryTimeZoneId: '',
    priority: 'NORMAL',
    isRecurring: 'N',
    recurrencePattern: '',
  });
  const [participants, setParticipants] = useState([]);
  const [roomAssignments, setRoomAssignments] = useState([]);
  const [videoReservations, setVideoReservations] = useState([]);

  const [employees, setEmployees] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [timeZones, setTimeZones] = useState([]);
  const [locations, setLocations] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    Promise.all([getEmployees(), getTimeZones(), getLocations()])
      .then(([emps, tzs, locs]) => {
        setEmployees(emps || []);
        setTimeZones(tzs || []);
        setLocations(locs || []);
      })
      .catch(() => {});
    getRooms().then(r => setRooms(r || [])).catch(() => {});
  }, []);

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  function addParticipant() {
    setParticipants([...participants, { employeeId: '', status: 'ATTENDEE', responseStatus: 'PENDING', responsibility: '' }]);
  }

  function updateParticipant(idx, field, value) {
    const updated = [...participants];
    updated[idx] = { ...updated[idx], [field]: value };
    setParticipants(updated);
  }

  function removeParticipant(idx) {
    setParticipants(participants.filter((_, i) => i !== idx));
  }

  function addRoom() {
    setRoomAssignments([...roomAssignments, { roomId: '', isPrimaryRoom: roomAssignments.length === 0 ? 'Y' : 'N' }]);
  }

  function updateRoom(idx, field, value) {
    const updated = [...roomAssignments];
    updated[idx] = { ...updated[idx], [field]: value };
    setRoomAssignments(updated);
  }

  function removeRoom(idx) {
    setRoomAssignments(roomAssignments.filter((_, i) => i !== idx));
  }

  function addVideo() {
    setVideoReservations([...videoReservations, {
      locationId: '', timeZoneId: '', videoTitle: '', isPrimaryLocation: videoReservations.length === 0 ? 'Y' : 'N',
      isVideoEnabled: 'Y', connectionLink: '', dialInInfo: ''
    }]);
  }

  function updateVideo(idx, field, value) {
    const updated = [...videoReservations];
    updated[idx] = { ...updated[idx], [field]: value };
    setVideoReservations(updated);
  }

  function removeVideo(idx) {
    setVideoReservations(videoReservations.filter((_, i) => i !== idx));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    if (new Date(form.startUtc) >= new Date(form.endUtc)) {
      setError('Start time must be before end time');
      return;
    }

    setLoading(true);
    try {
      const payload = {
        ...form,
        organizerId: Number(form.organizerId),
        secondaryTimeZoneId: form.secondaryTimeZoneId ? Number(form.secondaryTimeZoneId) : null,
        startUtc: new Date(form.startUtc).toISOString(),
        endUtc: new Date(form.endUtc).toISOString(),
      };
      const selectedParticipants = participants
        .filter(p => p.employeeId)
        .map(p => ({ ...p, employeeId: Number(p.employeeId) }));
      const selectedRooms = roomAssignments
        .filter(r => r.roomId)
        .map(r => ({
          ...r,
          roomId: Number(r.roomId),
          startUtc: payload.startUtc,
          endUtc: payload.endUtc,
          status: 'RESERVED',
        }));
      const selectedVideos = videoReservations
        .filter(v => v.videoTitle)
        .map(v => ({
          ...v,
          locationId: v.locationId ? Number(v.locationId) : null,
          timeZoneId: v.timeZoneId ? Number(v.timeZoneId) : null,
          status: 'CONFIRMED',
        }));

      if (selectedVideos.length > 0 && selectedRooms.length === 0) {
        setError('Add at least one room before adding a video reservation');
        setLoading(false);
        return;
      }

      await createAssignment(payload);
      const assignmentId = await findCreatedAssignmentId(payload);
      await Promise.all(selectedParticipants.map(p => addParticipantToAssignment(assignmentId, p)));
      for (const room of selectedRooms) {
        await addRoomToAssignment(assignmentId, room);
      }
      if (selectedVideos.length > 0) {
        const savedRooms = await getAssignmentRoomAssignments(assignmentId);
        const primaryRoom = savedRooms.find(room => room.isPrimaryRoom === 'Y') || savedRooms[0];
        await Promise.all(selectedVideos.map(video => addVideoReservation(assignmentId, {
          ...video,
          meetingAssignmentId: primaryRoom.meetingAssignmentId,
        })));
      }
      navigate('/assignments');
    } catch (err) {
      setError(err.message || 'Failed to create meeting');
    } finally {
      setLoading(false);
    }
  }

  async function findCreatedAssignmentId(payload) {
    const matches = await getAssignments({
      organizerId: payload.organizerId,
      fromUtc: payload.startUtc,
      toUtc: payload.endUtc,
    });
    const targetStart = new Date(payload.startUtc).getTime();
    const targetEnd = new Date(payload.endUtc).getTime();
    const created = [...(matches || [])]
      .sort((a, b) => (b.assignmentId || 0) - (a.assignmentId || 0))
      .find((assignment) => (
        assignment.meetingTitle === payload.meetingTitle &&
        new Date(assignment.startUtc).getTime() === targetStart &&
        new Date(assignment.endUtc).getTime() === targetEnd
      ))
      || matches?.[matches.length - 1];

    if (!created?.assignmentId) {
      throw new Error('Meeting was created, but the new assignment ID could not be resolved');
    }
    return created.assignmentId;
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Schedule Meeting</h1>
      </div>

      <div className="form-card">
        {error && <div className="auth-error">{error}</div>}
        <form onSubmit={handleSubmit}>
          {/* Basic Info */}
          <h3 className="form-section-title">Meeting Details</h3>
          <div className="form-group">
            <label>Title</label>
            <input name="meetingTitle" value={form.meetingTitle} onChange={handleChange} placeholder="Meeting title" required />
          </div>
          <div className="form-group">
            <label>Description</label>
            <textarea name="description" value={form.description} onChange={handleChange} rows="2" placeholder="Meeting description" />
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>Start</label>
              <input type="datetime-local" name="startUtc" value={form.startUtc} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label>End</label>
              <input type="datetime-local" name="endUtc" value={form.endUtc} onChange={handleChange} required />
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>Priority</label>
              <select name="priority" value={form.priority} onChange={handleChange}>
                <option value="HIGH">High</option>
                <option value="NORMAL">Normal</option>
                <option value="LOW">Low</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>
            <div className="form-group">
              <label>Secondary Timezone</label>
              <select name="secondaryTimeZoneId" value={form.secondaryTimeZoneId} onChange={handleChange}>
                <option value="">None</option>
                {timeZones.map(tz => (
                  <option key={tz.timeZoneId} value={tz.timeZoneId}>{tz.zoneName}</option>
                ))}
              </select>
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>Recurring</label>
              <select name="isRecurring" value={form.isRecurring} onChange={handleChange}>
                <option value="N">No</option>
                <option value="Y">Yes</option>
              </select>
            </div>
            {form.isRecurring === 'Y' && (
              <div className="form-group">
                <label>Recurrence Pattern</label>
                <input name="recurrencePattern" value={form.recurrencePattern} onChange={handleChange} placeholder="e.g. WEEKLY" />
              </div>
            )}
          </div>

          {/* Participants */}
          <h3 className="form-section-title">
            Participants
            <button type="button" className="btn btn-outline btn-sm" onClick={addParticipant} style={{marginLeft: '0.5rem'}}>+ Add</button>
          </h3>
          {participants.map((p, idx) => (
            <div key={idx} className="form-row inline-form">
              <div className="form-group" style={{flex:2}}>
                <select value={p.employeeId} onChange={e => updateParticipant(idx, 'employeeId', e.target.value)}>
                  <option value="">Select Employee</option>
                  {employees.map(emp => (
                    <option key={emp.id} value={emp.id}>{emp.firstName} {emp.lastName}</option>
                  ))}
                </select>
              </div>
              <div className="form-group" style={{flex:1}}>
                <select value={p.status} onChange={e => updateParticipant(idx, 'status', e.target.value)}>
                  <option value="ATTENDEE">Attendee</option>
                  <option value="ORGANIZER">Organizer</option>
                  <option value="APPROVER">Approver</option>
                </select>
              </div>
              <div className="form-group" style={{flex:1}}>
                <input value={p.responsibility} onChange={e => updateParticipant(idx, 'responsibility', e.target.value)} placeholder="Role" />
              </div>
              <button type="button" className="btn btn-danger btn-sm" onClick={() => removeParticipant(idx)}>✕</button>
            </div>
          ))}

          {/* Room Assignments */}
          <h3 className="form-section-title">
            Rooms
            <button type="button" className="btn btn-outline btn-sm" onClick={addRoom} style={{marginLeft: '0.5rem'}}>+ Add</button>
          </h3>
          {roomAssignments.map((r, idx) => (
            <div key={idx} className="form-row inline-form">
              <div className="form-group" style={{flex:2}}>
                <select value={r.roomId} onChange={e => updateRoom(idx, 'roomId', e.target.value)}>
                  <option value="">Select Room</option>
                  {rooms.map(room => (
                    <option key={room.roomId} value={room.roomId}>{room.roomName || room.roomCode}</option>
                  ))}
                </select>
              </div>
              <div className="form-group" style={{flex:1}}>
                <select value={r.isPrimaryRoom} onChange={e => updateRoom(idx, 'isPrimaryRoom', e.target.value)}>
                  <option value="Y">Primary</option>
                  <option value="N">Secondary</option>
                </select>
              </div>
              <button type="button" className="btn btn-danger btn-sm" onClick={() => removeRoom(idx)}>✕</button>
            </div>
          ))}

          {/* Video Reservations */}
          <h3 className="form-section-title">
            Video Reservations
            <button type="button" className="btn btn-outline btn-sm" onClick={addVideo} style={{marginLeft: '0.5rem'}}>+ Add</button>
          </h3>
          {videoReservations.map((v, idx) => (
            <div key={idx} className="video-form-block">
              <div className="form-row">
                <div className="form-group">
                  <label>Title</label>
                  <input value={v.videoTitle} onChange={e => updateVideo(idx, 'videoTitle', e.target.value)} placeholder="Video session title" />
                </div>
                <div className="form-group">
                  <label>Location</label>
                  <select value={v.locationId} onChange={e => updateVideo(idx, 'locationId', e.target.value)}>
                    <option value="">Select Location</option>
                    {locations.map(l => (
                      <option key={l.id} value={l.id}>{l.city}, {l.country}</option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Timezone</label>
                  <select value={v.timeZoneId} onChange={e => updateVideo(idx, 'timeZoneId', e.target.value)}>
                    <option value="">Select Timezone</option>
                    {timeZones.map(tz => (
                      <option key={tz.timeZoneId} value={tz.timeZoneId}>{tz.zoneName}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Primary Location</label>
                  <select value={v.isPrimaryLocation} onChange={e => updateVideo(idx, 'isPrimaryLocation', e.target.value)}>
                    <option value="Y">Yes</option>
                    <option value="N">No</option>
                  </select>
                </div>
              </div>
              <div className="form-group">
                <label>Connection Link</label>
                <input value={v.connectionLink} onChange={e => updateVideo(idx, 'connectionLink', e.target.value)} placeholder="https://meet.example.com/..." />
              </div>
              <div className="form-group">
                <label>Dial-In Info</label>
                <input value={v.dialInInfo} onChange={e => updateVideo(idx, 'dialInInfo', e.target.value)} placeholder="+1-555-0101,,991001#" />
              </div>
              <button type="button" className="btn btn-danger btn-sm" onClick={() => removeVideo(idx)}>Remove Video</button>
            </div>
          ))}

          <div className="form-actions" style={{marginTop: '2rem'}}>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Creating...' : 'Create Meeting'}
            </button>
            <button type="button" className="btn btn-outline" onClick={() => navigate('/assignments')}>Cancel</button>
          </div>
        </form>
      </div>
    </div>
  );
}
