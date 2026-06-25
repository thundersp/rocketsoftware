import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './Auth.css';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(email, password);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-container">
        <div className="auth-left">
          <Link to="/" className="auth-brand">
            <span className="brand-icon">📅</span>
            <span>BuzzMeet</span>
          </Link>
          <h1>Welcome Back</h1>
          <p>Sign in to manage your meetings, rooms, and schedule.</p>
          <div className="auth-features">
            <div className="auth-feature">
              <span>✓</span> Book conference rooms instantly
            </div>
            <div className="auth-feature">
              <span>✓</span> Manage participants and video links
            </div>
            <div className="auth-feature">
              <span>✓</span> Multi-timezone scheduling
            </div>
          </div>
        </div>
        <div className="auth-right">
          <form className="auth-form" onSubmit={handleSubmit}>
            <h2>Sign In</h2>
            {error && <div className="auth-error">{error}</div>}
            <div className="form-group">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@buzzmeet.com"
                required
                autoComplete="email"
              />
            </div>
            <div className="form-group">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter your password"
                required
                autoComplete="current-password"
              />
            </div>
            <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
              {loading ? 'Signing in...' : 'Sign In'}
            </button>
            <p className="auth-switch">
              Don't have an account? <Link to="/register">Register</Link>
            </p>
            <div className="auth-hint">
              <strong>Demo Credentials</strong><br />
              Email: <code>Timothee.Greswell@BuzzwordSolutions.com</code><br />
              Password: <code>Password123!</code>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
