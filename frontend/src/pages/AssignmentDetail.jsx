import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import {
  cancelAssignment,
  deleteAssignment,
  getAssignment,
  getAssignmentRoomAssignments,
  getParticipants,
  getVideoReservations,
  overrideAssignment,
  removeParticipant,
  removeRoomFromAssignment,
  deleteVideoReservation,
} from '../api/assignments';
import { getEmployees } from '../api/lookups';
import { getRooms } from '../api/rooms';
import './Pages.css';

export default function AssignmentDetail() {
  const { assignmentId } = useParams();
  const { user, isAdmin, isManager } = useAuth();
  const navigate = useNavigate();
  const [assignment, setAssignment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showCancel, setShowCancel] = useState(false);
  const [showOverride, setShowOverride] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [overrideForm, setOverrideForm] = useState({ reason: '', status: 'OVERRIDDEN' });

  useEffect(() => {
    loadAssignment();
  }, [assignmentId]);

  async function loadAssignment() {
    setLoading(true);
    setError('');
    try {
      const [base, participants, roomAssignments, videoReservations, employees, rooms] = await Promise.all([
        getAssignment(assignmentId),
        getParticipants(assignmentId).catch(() => []),
        getAssignmentRoomAssignments(assignmentId).catch(() => []),
        getVideoReservations(assignmentId).catch(() => []),
        getEmployees().catch(() => []),
        getRooms().catch(() => []),
      ]);
      const employeeById = new Map(employees.map((employee) => [employee.id, employee]));
      const roomById = new Map(rooms.map((room) => [room.roomId, room]));
      setAssignment({
        ...base,
        participants: participants.map((participant) => ({
          ...employeeById.get(participant.employeeId),
          ...participant,
        })),
        roomAssignments: roomAssignments.map((roomAssignment) => ({
          ...roomById.get(roomAssignment.roomId),
          ...roomAssignment,
        })),
        videoReservations,
      });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleCancel() {
    if (!cancelReason.trim()) return;
    try {
      await cancelAssignment(assignmentId, { reason: cancelReason, cancelledBy: user?.employeeId });
      await loadAssignment();
      setShowCancel(false);
      setCancelReason('');
    } catch (err) {
      alert(err.message);
    }
  }

  async function handleOverride() {
    try {
      await overrideAssignment(assignmentId, overrideForm);
      await loadAssignment();
      setShowOverride(false);
    } catch (err) {
      alert(err.message);
    }
  }

  async function handleDelete() {
    if (!window.confirm('Delete this meeting?')) return;
    try {
      await deleteAssignment(assignmentId);
      navigate('/assignments');
    } catch (err) {
      alert(err.message);
    }
  }

  async function handleRemoveParticipant(participantId) {
    if (!window.confirm('Remove this participant?')) return;
    try {
      await removeParticipant(assignmentId, participantId);
      await loadAssignment();
    } catch (err) {
      alert(err.message);
    }
  }

  async function handleRemoveRoom(meetingAssignmentId) {
    if (!window.confirm('Release this room?')) return;
    try {
      await removeRoomFromAssignment(assignmentId, meetingAssignmentId);
      await loadAssignment();
    } catch (err) {
      alert(err.message);
    }
  }

  async function handleDeleteVideo(videoReservationId) {
    if (!window.confirm('Delete this video reservation?')) return;
    try {
      await deleteVideoReservation(videoReservationId);
      await loadAssignment();
    } catch (err) {
      alert(err.message);
    }
  }

  function formatDateTime(utc) {
    if (!utc) return 'N/A';
    return new Date(utc).toLocaleString();
  }

  const canCancel = assignment && (
    isAdmin() || isManager() || user?.employeeId === assignment.organizerId
  );
  const canOverride = isAdmin() || isManager();

  if (loading) return <div className="page-loading">Loading...</div>;
  if (error) return <div className="page"><div className="auth-error">{error}</div></div>;
  if (!assignment) return <div className="page"><p>Assignment not found.</p></div>;

  return (
    <div className="page">
      <div className="page-header">
        <h1>{assignment.meetingTitle || 'Meeting Detail'}</h1>
        <div className="page-header-actions">
          <Link to="/assignments" className="btn btn-outline">Back to List</Link>
          {canCancel && assignment.status !== 'CANCELLED' && (
            <button className="btn btn-danger" onClick={() => setShowCancel(true)}>Cancel Meeting</button>
          )}
          {canCancel && (
            <button className="btn btn-outline" onClick={handleDelete}>Delete</button>
          )}
          {canOverride && assignment.status !== 'CANCELLED' && (
            <button className="btn btn-warning" onClick={() => setShowOverride(true)}>Override</button>
          )}
        </div>
      </div>

      <div className="detail-grid">
        <div className="detail-card">
          <h3>Meeting Info</h3>
          <div className="detail-row"><span>Title:</span><span>{assignment.meetingTitle}</span></div>
          <div className="detail-row"><span>Description:</span><span>{assignment.description || 'N/A'}</span></div>
          <div className="detail-row"><span>Start:</span><span>{formatDateTime(assignment.startUtc)}</span></div>
          <div className="detail-row"><span>End:</span><span>{formatDateTime(assignment.endUtc)}</span></div>
          <div className="detail-row"><span>Priority:</span>
            <span className={`priority-tag ${(assignment.priority || 'medium').toLowerCase()}`}>{assignment.priority || 'MEDIUM'}</span>
          </div>
          <div className="detail-row"><span>Status:</span>
            <span className={`status-badge ${(assignment.status || 'pending').toLowerCase()}`}>{assignment.status || 'PENDING'}</span>
          </div>
          <div className="detail-row"><span>Recurring:</span><span>{assignment.isRecurring === 'Y' ? 'Yes' : 'No'}</span></div>
          {assignment.recurrencePattern && (
            <div className="detail-row"><span>Pattern:</span><span>{assignment.recurrencePattern}</span></div>
          )}
        </div>

        {assignment.participants && assignment.participants.length > 0 && (
          <div className="detail-card">
            <h3>Participants</h3>
            {assignment.participants.map((p, i) => (
              <div key={i} className="detail-row">
                <span>{p.firstName} {p.lastName} ({p.email})</span>
                <span>
                  <span className={`role-tag ${(p.status || '').toLowerCase()}`}>{p.status || p.responseStatus}</span>
                  {canCancel && p.participantId && (
                    <button className="btn btn-outline btn-sm" onClick={() => handleRemoveParticipant(p.participantId)}>Remove</button>
                  )}
                </span>
              </div>
            ))}
          </div>
        )}

        {assignment.roomAssignments && assignment.roomAssignments.length > 0 && (
          <div className="detail-card">
            <h3>Room Assignments</h3>
            {assignment.roomAssignments.map((r, i) => (
              <div key={i} className="detail-row">
                <span>{r.roomName || `Room ${r.roomId}`}</span>
                <span>
                  {r.isPrimaryRoom === 'Y' ? 'Primary' : 'Secondary'}
                  {canCancel && r.meetingAssignmentId && (
                    <button className="btn btn-outline btn-sm" onClick={() => handleRemoveRoom(r.meetingAssignmentId)}>Release</button>
                  )}
                </span>
              </div>
            ))}
          </div>
        )}

        {assignment.videoReservations && assignment.videoReservations.length > 0 && (
          <div className="detail-card">
            <h3>Video Reservations</h3>
            {assignment.videoReservations.map((v, i) => (
              <div key={i} className="video-res-item">
                <div className="detail-row"><span>Title:</span><span>{v.videoTitle}</span></div>
                {v.connectionLink && <div className="detail-row"><span>Link:</span><span><a href={v.connectionLink} target="_blank" rel="noopener noreferrer">{v.connectionLink}</a></span></div>}
                {v.dialInInfo && <div className="detail-row"><span>Dial-In:</span><span>{v.dialInInfo}</span></div>}
                {canCancel && v.videoReservationId && (
                  <button className="btn btn-outline btn-sm" onClick={() => handleDeleteVideo(v.videoReservationId)}>Delete Video</button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Cancel Modal */}
      {showCancel && (
        <div className="modal-overlay" onClick={() => setShowCancel(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Cancel Meeting</h3>
            <div className="form-group">
              <label>Reason</label>
              <textarea value={cancelReason} onChange={e => setCancelReason(e.target.value)}
                rows="3" placeholder="Provide a reason for cancellation" />
            </div>
            <div className="form-actions">
              <button className="btn btn-danger" onClick={handleCancel}>Confirm Cancel</button>
              <button className="btn btn-outline" onClick={() => setShowCancel(false)}>Close</button>
            </div>
          </div>
        </div>
      )}

      {/* Override Modal */}
      {showOverride && (
        <div className="modal-overlay" onClick={() => setShowOverride(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Override Assignment</h3>
            <div className="form-group">
              <label>Reason</label>
              <textarea value={overrideForm.reason}
                onChange={e => setOverrideForm({ ...overrideForm, reason: e.target.value })}
                rows="2" placeholder="Reason for override" />
            </div>
            <div className="form-group">
              <label>Status</label>
              <select value={overrideForm.status}
                onChange={e => setOverrideForm({ ...overrideForm, status: e.target.value })}>
                <option value="OVERRIDDEN">Overridden</option>
                <option value="SCHEDULED">Scheduled</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>
            <div className="form-actions">
              <button className="btn btn-warning" onClick={handleOverride}>Override</button>
              <button className="btn btn-outline" onClick={() => setShowOverride(false)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
