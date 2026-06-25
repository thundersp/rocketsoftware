import { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { getAssignments } from '../api/assignments';
import './Dashboard.css';

const DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const MONTHS = ['January','February','March','April','May','June','July','August','September','October','November','December'];
const EVENT_COLORS = ['#22c55e', '#3b82f6', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];

export default function Dashboard() {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [assignments, setAssignments] = useState([]);
  const [view, setView] = useState('month');
  const [loading, setLoading] = useState(true);

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  async function loadAssignments() {
    await Promise.resolve();
    setLoading(true);
    try {
      const fromUtc = new Date(year, month, 1).toISOString();
      const toUtc = new Date(year, month + 1, 0, 23, 59, 59).toISOString();
      const data = await getAssignments({ fromUtc, toUtc });
      setAssignments(Array.isArray(data) ? data : []);
    } catch {
      setAssignments([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(loadAssignments, 0);
    return () => window.clearTimeout(timer);
  }, [month, year]);

  function prevMonth() {
    setCurrentDate(new Date(year, month - 1, 1));
  }

  function nextMonth() {
    setCurrentDate(new Date(year, month + 1, 1));
  }

  function goToday() {
    const today = new Date();
    setCurrentDate(today);
    setSelectedDate(today);
  }

  const calendarDays = useMemo(() => {
    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const days = [];
    // Fill leading empty cells
    for (let i = 0; i < firstDay; i++) days.push(null);
    for (let d = 1; d <= daysInMonth; d++) days.push(d);
    return days;
  }, [year, month]);

  function getEventsForDay(day) {
    if (!day) return [];
    return assignments.filter(a => {
      const start = new Date(a.startUtc);
      return start.getFullYear() === year && start.getMonth() === month && start.getDate() === day;
    });
  }

  function getSelectedDayEvents() {
    return assignments.filter(a => {
      const start = new Date(a.startUtc);
      return start.toDateString() === selectedDate.toDateString();
    });
  }

  function formatTime(utc) {
    return new Date(utc).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  const isToday = (day) => {
    const today = new Date();
    return day === today.getDate() && month === today.getMonth() && year === today.getFullYear();
  };

  const isSelected = (day) => {
    return day === selectedDate.getDate() && month === selectedDate.getMonth() && year === selectedDate.getFullYear();
  };

  // Activity summary
  const activitySummary = useMemo(() => {
    const summary = { approved: 0, pending: 0, rescheduled: 0, cancelled: 0 };
    assignments.forEach(a => {
      const status = (a.status || '').toUpperCase();
      if (status === 'APPROVED' || status === 'CONFIRMED' || status === 'ACTIVE' || status === 'SCHEDULED') summary.approved++;
      else if (status === 'PENDING' || status === 'DRAFT') summary.pending++;
      else if (status === 'RESCHEDULED' || status === 'OVERRIDDEN') summary.rescheduled++;
      else if (status === 'CANCELLED') summary.cancelled++;
      else summary.pending++; // default
    });
    return summary;
  }, [assignments]);

  return (
    <div className="dashboard">
      <div className="dashboard-main">
        {/* Calendar Header */}
        <div className="calendar-header">
          <div className="calendar-nav">
            <button className="btn btn-outline btn-sm" onClick={goToday}>Today</button>
            <button className="cal-arrow" onClick={prevMonth}>‹</button>
            <h2 className="cal-month-title">{MONTHS[month]} {year}</h2>
            <button className="cal-arrow" onClick={nextMonth}>›</button>
          </div>
          <div className="calendar-view-toggle">
            {['month', 'week', 'day'].map(v => (
              <button
                key={v}
                className={`view-btn ${view === v ? 'active' : ''}`}
                onClick={() => setView(v)}
              >
                {v.charAt(0).toUpperCase() + v.slice(1)}
              </button>
            ))}
          </div>
        </div>

        {/* Calendar Grid */}
        <div className="calendar-grid">
          {DAYS.map(d => (
            <div key={d} className="cal-day-header">{d}</div>
          ))}
          {calendarDays.map((day, idx) => {
            const events = getEventsForDay(day);
            return (
              <div
                key={idx}
                className={`cal-cell ${!day ? 'empty' : ''} ${isToday(day) ? 'today' : ''} ${isSelected(day) ? 'selected' : ''}`}
                onClick={() => day && setSelectedDate(new Date(year, month, day))}
              >
                {day && (
                  <>
                    <span className="cal-cell-day">{day}</span>
                    <div className="cal-cell-events">
                      {events.slice(0, 3).map((evt, i) => (
                        <div
                          key={evt.assignmentId || i}
                          className="cal-event-dot"
                          style={{ background: EVENT_COLORS[i % EVENT_COLORS.length] }}
                          title={evt.meetingTitle || 'Meeting'}
                        >
                          {evt.meetingTitle || 'Meeting'}
                        </div>
                      ))}
                      {events.length > 3 && (
                        <span className="cal-more">+{events.length - 3} more</span>
                      )}
                    </div>
                  </>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Right Panel */}
      <div className="dashboard-panel">
        {/* Meetings Section */}
        <div className="panel-section">
          <div className="panel-section-header">
            <h3>Meetings</h3>
            <Link to="/assignments/new" className="btn btn-primary btn-sm">
              + Request Meeting
            </Link>
          </div>
        </div>

        {/* Activity Section */}
        <div className="panel-section">
          <h3>Activity</h3>
          <div className="activity-grid">
            <div className="activity-item">
              <span className="activity-dot green"></span>
              <span className="activity-label">Approved</span>
              <span className="activity-count">{activitySummary.approved}</span>
            </div>
            <div className="activity-item">
              <span className="activity-dot yellow"></span>
              <span className="activity-label">Pending</span>
              <span className="activity-count">{activitySummary.pending}</span>
            </div>
            <div className="activity-item">
              <span className="activity-dot blue"></span>
              <span className="activity-label">Reschedule</span>
              <span className="activity-count">{activitySummary.rescheduled}</span>
            </div>
            <div className="activity-item">
              <span className="activity-dot red"></span>
              <span className="activity-label">Cancel</span>
              <span className="activity-count">{activitySummary.cancelled}</span>
            </div>
          </div>
        </div>

        {/* Details Day */}
        <div className="panel-section">
          <h3>Details — {selectedDate.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })}</h3>
          <div className="day-details">
            {loading ? (
              <p className="no-events">Loading...</p>
            ) : getSelectedDayEvents().length === 0 ? (
              <p className="no-events">No meetings scheduled for this day.</p>
            ) : (
              getSelectedDayEvents().map((evt, i) => (
                <Link
                  key={evt.assignmentId || i}
                  to={`/assignments/${evt.assignmentId}`}
                  className="day-event"
                >
                  <div className="day-event-color" style={{ background: EVENT_COLORS[i % EVENT_COLORS.length] }}></div>
                  <div className="day-event-info">
                    <div className="day-event-title">{evt.meetingTitle || 'Meeting'}</div>
                    <div className="day-event-time">
                      {formatTime(evt.startUtc)} — {formatTime(evt.endUtc)}
                    </div>
                    {evt.priority && <span className={`priority-tag ${evt.priority.toLowerCase()}`}>{evt.priority}</span>}
                  </div>
                </Link>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
