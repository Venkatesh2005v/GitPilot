import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { GitBranch, Settings } from 'lucide-react';
import { ThemeToggle } from './ThemeToggle';

export function TopNav({ user, selectedRepoId, setSelectedRepoId }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [repos, setRepos] = useState([]);

  const fetchTrackedRepos = async () => {
    try {
      const response = await fetch('/dashboard/repositories');
      if (response.ok) {
        const data = await response.json();
        setRepos(data);
        // If selectedRepoId is empty or not in the list, default to first tracked repo
        if (data.length > 0) {
          const storedId = localStorage.getItem('gitpilot_selected_repo_id');
          const isValidStored = storedId && data.some(r => r.id.toString() === storedId);
          const defaultId = isValidStored ? storedId : data[0].id.toString();
          if (!selectedRepoId || !data.some(r => r.id.toString() === selectedRepoId)) {
            setSelectedRepoId(defaultId);
            localStorage.setItem('gitpilot_selected_repo_id', defaultId);
          }
        }
      }
    } catch (e) {
      console.error("Failed to load repositories in navbar:", e);
    }
  };

  useEffect(() => {
    fetchTrackedRepos();
  }, [location.pathname]);

  useEffect(() => {
    const match = location.pathname.match(/^\/repositories\/(\d+)/);
    if (match) {
      const urlId = match[1];
      if (urlId !== selectedRepoId) {
        setSelectedRepoId(urlId);
        localStorage.setItem('gitpilot_selected_repo_id', urlId);
      }
    }
  }, [location.pathname, selectedRepoId, setSelectedRepoId]);

  const handleRepoChange = (e) => {
    const id = e.target.value;
    if (id) {
      setSelectedRepoId(id);
      localStorage.setItem('gitpilot_selected_repo_id', id);
      if (location.pathname.startsWith('/repositories/')) {
        navigate(`/repositories/${id}`);
      }
    }
  };

  const getPageTitle = () => {
    if (location.pathname === '/dashboard') return 'Dashboard Overview';
    if (location.pathname === '/onboarding') return 'Developer Onboarding';
    if (location.pathname === '/recommendations') return 'AI Recommendations';
    if (location.pathname === '/architecture') return 'Architecture View';
    if (location.pathname === '/team-intelligence') return 'Team Intelligence';
    if (location.pathname === '/settings') return 'Settings & Webhooks';
    if (location.pathname === '/analytics') return 'Repository Analytics';
    if (location.pathname === '/insights') return 'AI Insights';
    if (location.pathname.startsWith('/repositories/')) {
      const activeRepo = repos.find(r => r.id.toString() === selectedRepoId);
      return activeRepo ? activeRepo.repositoryName : 'Repository Overview';
    }
    return 'GitPilot';
  };

  return (
    <header style={{
      height: '76px',
      backgroundColor: 'var(--bg-topnav)',
      backdropFilter: 'blur(20px)',
      WebkitBackdropFilter: 'blur(20px)',
      borderBottom: '1px solid var(--border-color)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 2.25rem',
      position: 'sticky',
      top: 0,
      zIndex: 90
    }}>
      {/* Title & Repo Quick Selector */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '1.75rem' }}>
        <h2 style={{ fontSize: '1.35rem', fontWeight: 800, fontFamily: 'Space Grotesk, sans-serif', letterSpacing: '-0.02em' }}>
          {getPageTitle()}
        </h2>

        {repos.length > 0 && (
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.6rem',
            backgroundColor: 'var(--bg-card)',
            padding: '0.45rem 1rem',
            borderRadius: '9999px',
            border: '1px solid var(--border-color)',
            boxShadow: 'var(--shadow-sm)'
          }}>
            <GitBranch size={15} style={{ color: 'var(--accent-primary)' }} />
            <select
              value={selectedRepoId}
              onChange={handleRepoChange}
              style={{
                background: 'none',
                border: 'none',
                color: 'var(--text-primary)',
                fontFamily: 'Inter, sans-serif',
                fontSize: '0.85rem',
                fontWeight: 600,
                outline: 'none',
                cursor: 'pointer'
              }}
            >
              {repos.map(r => (
                <option key={r.id} value={r.id.toString()} style={{ background: 'var(--bg-card-solid)', color: 'var(--text-primary)' }}>
                  {r.repositoryName}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* Right Controls: Theme Toggle & Settings */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <ThemeToggle />

        <Link 
          to="/settings" 
          className="btn btn-secondary"
          style={{
            padding: '0.55rem',
            borderRadius: '9999px'
          }}
          title="Configure webhooks and settings"
        >
          <Settings size={17} />
        </Link>
      </div>
    </header>
  );
}
