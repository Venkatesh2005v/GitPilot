import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom';
import { Sidebar } from './components/Sidebar';
import { TopNav } from './components/TopNav';
import { Dashboard } from './pages/Dashboard';
import { RepositoryOverview } from './pages/RepositoryOverview';
import { Analytics } from './pages/Analytics';
import { AIInsights } from './pages/AIInsights';
import { Settings } from './pages/Settings';
import { Github, Cpu, Lock, ArrowRight, ShieldCheck, Zap, BarChart2 } from 'lucide-react';

function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedRepoId, setSelectedRepoId] = useState('');

  const checkAuth = async () => {
    try {
      const response = await fetch('/me');
      if (response.ok) {
        const data = await response.json();
        setUser(data);
      } else {
        setUser(null);
      }
    } catch (e) {
      console.error("Auth check failed:", e);
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  const handleMockLogin = async () => {
    try {
      const response = await fetch('/auth/mock');
      if (response.ok) {
        await checkAuth();
      }
    } catch (e) {
      console.error("Mock login failed:", e);
    }
  };

  useEffect(() => {
    checkAuth();
  }, []);

  if (loading) {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        width: '100vw',
        backgroundColor: '#07090e',
        color: '#f8fafc',
        fontFamily: 'sans-serif'
      }}>
        <div className="pulse-indicator running" style={{ width: '48px', height: '48px', marginBottom: '1.5rem' }}></div>
        <p style={{ color: '#94a3b8', fontSize: '0.875rem', letterSpacing: '0.05em' }}>CONNECTING TO GITPILOT...</p>
      </div>
    );
  }

  // Render Landing Page if unauthenticated
  if (!user) {
    return (
      <div className="landing-page">
        <div className="landing-card">
          <div className="landing-logo">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{ width: '32px', height: '32px' }}>
              <path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4" />
              <path d="M9 18c-4.51 2-5-2-7-2" />
            </svg>
          </div>
          <h1 className="landing-title">GitPilot</h1>
          <p className="landing-subtitle">AI-Powered Engineering Analytics & Real-Time Sync</p>
          
          <div className="landing-features">
            <div className="landing-feature-item">
              <Zap size={18} />
              <div>
                <strong>Real-Time Synchronization:</strong> Updates commits and metadata immediately via GitHub Webhooks.
              </div>
            </div>
            <div className="landing-feature-item">
              <Cpu size={18} />
              <div>
                <strong>AI-Powered Insights:</strong> Auto-calculates repository health and generates actionable suggestions.
              </div>
            </div>
            <div className="landing-feature-item">
              <BarChart2 size={18} />
              <div>
                <strong>Interactive Charts:</strong> Visualizes commit activity and contributor splits.
              </div>
            </div>
          </div>

          <button 
            className="login-btn"
            onClick={() => window.location.href = '/oauth2/authorization/github'}
          >
            <Github size={20} />
            <span>Sign in with GitHub</span>
            <ArrowRight size={16} style={{ marginLeft: 'auto' }} />
          </button>

          <button 
            className="login-btn btn-secondary"
            onClick={handleMockLogin}
            style={{ marginTop: '0.75rem', width: '100%', display: 'flex', justifyContent: 'center', backgroundColor: '#1e293b', border: '1px solid #334155' }}
          >
            <ShieldCheck size={20} style={{ marginRight: '0.5rem', color: 'var(--accent-sky)' }} />
            <span>Developer Sandbox Sign-In</span>
          </button>
        </div>
      </div>
    );
  }

  // Render Core Application
  return (
    <BrowserRouter>
      <div className="app-container">
        <Sidebar user={user} />
        <div className="main-content">
          <TopNav user={user} selectedRepoId={selectedRepoId} setSelectedRepoId={setSelectedRepoId} />
          <div className="page-wrapper">
            <Routes>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<Dashboard setSelectedRepoId={setSelectedRepoId} />} />
              <Route path="/repositories/:id" element={<RepositoryOverview setSelectedRepoId={setSelectedRepoId} />} />
              <Route path="/analytics" element={<Analytics />} />
              <Route path="/insights" element={<AIInsights />} />
              <Route path="/settings" element={<Settings />} />
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </div>
        </div>
      </div>
    </BrowserRouter>
  );
}

export default App;
