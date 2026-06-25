/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect } from 'react';
import { login as apiLogin, signup as apiSignup, getMe } from '../api/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('buzzmeet_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [loading, setLoading] = useState(() => (
    Boolean(localStorage.getItem('buzzmeet_token')) && !localStorage.getItem('buzzmeet_user')
  ));

  useEffect(() => {
    const token = localStorage.getItem('buzzmeet_token');
    if (token && !user) {
      getMe()
        .then((data) => {
          setUser(data);
          localStorage.setItem('buzzmeet_user', JSON.stringify(data));
        })
        .catch(() => {
          localStorage.removeItem('buzzmeet_token');
          localStorage.removeItem('buzzmeet_user');
          setUser(null);
        })
        .finally(() => setLoading(false));
    }
  }, []);

  async function login(email, password) {
    const data = await apiLogin(email, password);
    return establishSession(data);
  }

  async function signup(data) {
    const response = await apiSignup(data);
    return establishSession(response);
  }

  async function establishSession(data) {
    localStorage.setItem('buzzmeet_token', data.accessToken);
    const me = {
      employeeId: data.employeeId,
      email: data.email,
      roles: normalizeRoles(data.roles),
      firstName: data.firstName,
      lastName: data.lastName,
      title: data.title,
    };
    // Fetch full profile
    try {
      const profile = await getMe();
      profile.roles = normalizeRoles(profile.roles);
      Object.assign(me, profile);
    } catch {
      // use login response data
    }
    setUser(me);
    localStorage.setItem('buzzmeet_user', JSON.stringify(me));
    return me;
  }

  function logout() {
    localStorage.removeItem('buzzmeet_token');
    localStorage.removeItem('buzzmeet_user');
    setUser(null);
  }

  function hasRole(role) {
    const wanted = role.replace(/^ROLE_/, '').toUpperCase();
    return user?.roles?.some((userRole) => userRole.replace(/^ROLE_/, '').toUpperCase() === wanted) || false;
  }

  function isAdmin() {
    return hasRole('ADMIN');
  }

  function isManager() {
    return hasRole('ADMIN') || hasRole('MANAGER') || hasRole('APPROVER');
  }

  function isOrganizer() {
    return hasRole('EMPLOYEE') || hasRole('ORGANIZER') || hasRole('MANAGER') || hasRole('APPROVER') || hasRole('ADMIN');
  }

  function canManageRooms() {
    return hasRole('ADMIN') || hasRole('MANAGER');
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, signup, logout, hasRole, isAdmin, isManager, isOrganizer, canManageRooms }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

function normalizeRoles(roles = []) {
  return roles.map((role) => String(role).replace(/^ROLE_/, '').toUpperCase());
}
