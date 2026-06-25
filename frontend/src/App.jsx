import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';

import Landing from './pages/Landing';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Rooms from './pages/Rooms';
import RoomForm from './pages/RoomForm';
import RoomDetail from './pages/RoomDetail';
import Assignments from './pages/Assignments';
import AssignmentDetail from './pages/AssignmentDetail';
import AssignmentForm from './pages/AssignmentForm';
import RoomAssignments from './pages/RoomAssignments';
import Employees from './pages/Employees';
import Notifications from './pages/Notifications';
import AuditLogs from './pages/AuditLogs';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public routes */}
          <Route path="/" element={<Landing />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* Protected routes with sidebar layout */}
          <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
            <Route path="/dashboard" element={<Dashboard />} />

            {/* Rooms */}
            <Route path="/rooms" element={<Rooms />} />
            <Route path="/rooms/new" element={
              <ProtectedRoute requiredRoles={['ADMIN', 'MANAGER']}><RoomForm /></ProtectedRoute>
            } />
            <Route path="/rooms/:roomId" element={<RoomDetail />} />
            <Route path="/rooms/:roomId/edit" element={
              <ProtectedRoute requiredRoles={['ADMIN', 'MANAGER']}><RoomForm /></ProtectedRoute>
            } />

            {/* Assignments / Meetings */}
            <Route path="/assignments" element={<Assignments />} />
            <Route path="/assignments/new" element={<AssignmentForm />} />
            <Route path="/assignments/:assignmentId" element={<AssignmentDetail />} />

            {/* Room Assignments / Schedule */}
            <Route path="/room-assignments" element={<RoomAssignments />} />

            {/* Employees */}
            <Route path="/employees" element={<Employees />} />

            {/* Notifications */}
            <Route path="/notifications" element={<Notifications />} />

            {/* Audit Logs - Admin only */}
            <Route path="/audit-logs" element={
              <ProtectedRoute requiredRoles={['ADMIN', 'MANAGER', 'APPROVER']}><AuditLogs /></ProtectedRoute>
            } />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
