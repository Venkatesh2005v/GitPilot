import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from './context/ThemeContext';
import { Sidebar } from './components/Sidebar';
import { TopNav } from './components/TopNav';
import { LandingPage } from './pages/LandingPage';
import { Dashboard } from './pages/Dashboard';
import { RepositoryOverview } from './pages/RepositoryOverview';
import { Analytics } from './pages/Analytics';
import { AIInsights } from './pages/AIInsights';
import { Settings } from './pages/Settings';
import { OnboardingPage } from './pages/OnboardingPage';
import { RecommendationsPage } from './pages/RecommendationsPage';
import { ArchitectureExplorer } from './pages/ArchitectureExplorer';
import { TeamIntelligenceView } from './pages/TeamIntelligenceView';

function AppContent() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedRepoId, setSelectedRepoId] = useState(() => localStorage.getItem('gitpilot_selected_repo_id') || '');

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
        backgroundColor: 'var(--bg-app)',
        color: 'var(--text-primary)'
      }}>
        <div style={{
          width: '48px',
          height: '48px',
          borderRadius: '1.25rem',
          background: 'linear-gradient(135deg, var(--accent-primary), var(--accent-teal))',
          marginBottom: '1.5rem',
          animation: 'spin 1.5s linear infinite'
        }}></div>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', letterSpacing: '0.05em', fontFamily: 'Space Grotesk' }}>
          CONNECTING TO GITPILOT TELEMETRY...
        </p>
      </div>
    );
  }

  if (!user) {
    return (
      <LandingPage />
    );
  }

  return (
    <BrowserRouter>
      <div className="app-container">
        <Sidebar user={user} />
        <div className="main-content">
          <TopNav user={user} selectedRepoId={selectedRepoId} setSelectedRepoId={setSelectedRepoId} />
          <div className="page-wrapper">
            <Routes>
              <Route path="/" element={<Dashboard setSelectedRepoId={setSelectedRepoId} />} />
              <Route path="/dashboard" element={<Dashboard setSelectedRepoId={setSelectedRepoId} />} />
              <Route path="/onboarding" element={<OnboardingPage selectedRepoId={selectedRepoId} />} />
              <Route path="/architecture" element={<ArchitectureExplorer selectedRepoId={selectedRepoId} />} />
              <Route path="/team-intelligence" element={<TeamIntelligenceView selectedRepoId={selectedRepoId} />} />
              <Route path="/recommendations" element={<RecommendationsPage selectedRepoId={selectedRepoId} />} />
              <Route path="/repositories/:id" element={<RepositoryOverview setSelectedRepoId={setSelectedRepoId} />} />
              <Route path="/analytics" element={<Analytics selectedRepoId={selectedRepoId} />} />
              <Route path="/insights" element={<AIInsights selectedRepoId={selectedRepoId} />} />
              <Route path="/settings" element={<Settings />} />
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </div>
        </div>
      </div>
    </BrowserRouter>
  );
}

function App() {
  return (
    <ThemeProvider>
      <AppContent />
    </ThemeProvider>
  );
}

export default App;
