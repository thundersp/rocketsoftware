import { useState, useEffect } from 'react';
import { getEmployees, getLocations } from '../api/lookups';
import './Pages.css';

export default function Employees() {
  const [employees, setEmployees] = useState([]);
  const [locations, setLocations] = useState([]);
  const [filters, setFilters] = useState({ locationId: '', title: '' });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getLocations().then(l => setLocations(l || [])).catch(() => {});
    loadEmployees();
  }, []);

  async function loadEmployees() {
    setLoading(true);
    try {
      const params = {};
      if (filters.locationId) params.locationId = filters.locationId;
      if (filters.title) params.title = filters.title;
      const data = await getEmployees(params);
      setEmployees(Array.isArray(data) ? data : []);
    } catch {
      setEmployees([]);
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
        <h1>Employees</h1>
      </div>

      <div className="filters-bar">
        <select name="locationId" value={filters.locationId} onChange={handleFilterChange}>
          <option value="">All Locations</option>
          {locations.map(l => (
            <option key={l.id} value={l.id}>{l.city}, {l.country}</option>
          ))}
        </select>
        <input name="title" value={filters.title} onChange={handleFilterChange} placeholder="Filter by title" />
        <button className="btn btn-outline btn-sm" onClick={loadEmployees}>Apply</button>
      </div>

      {loading ? (
        <div className="page-loading">Loading...</div>
      ) : employees.length === 0 ? (
        <div className="empty-state"><p>No employees found.</p></div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Title</th>
                <th>Location</th>
              </tr>
            </thead>
            <tbody>
              {employees.map(emp => (
                <tr key={emp.id}>
                  <td>
                    <div className="emp-name">
                      <div className="emp-avatar">{emp.firstName?.[0]}{emp.lastName?.[0]}</div>
                      <span>{emp.firstName} {emp.lastName}</span>
                    </div>
                  </td>
                  <td>{emp.email}</td>
                  <td>{emp.title || '—'}</td>
                  <td>{emp.city ? `${emp.city}, ${emp.country}` : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
