import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { GitBranch, Settings, Bell, RefreshCw } from 'lucide-react';

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
      }
    } catch (e) {
      console.error("Failed to load repositories in navbar:", e);
    }
  };

  useEffect(() => {
    fetchTrackedRepos();
  }, [location.pathname]); // refetch when page changes to ensure new selections appear

  // Sync state drop-down with URL path if on details page
  useEffect(() => {
    const match = location.pathname.match(/^\/repositories\/(\d+)/);
    if (match) {
      setSelectedRepoId(match[1]);
    } else {
      setSelectedRepoId('');
    }
  }, [location.pathname, setSelectedRepoId]);

  const handleRepoChange = (e) => {
    const id = e.target.value;
    setSelectedRepoId(id);
    if (id) {
      navigate(`/repositories/${id}`);
    } else {
      navigate('/dashboard');
    }
  };

  // Determine page title
  const getPageTitle = () => {
    if (location.pathname === '/dashboard') return 'Dashboard';
    if (location.pathname === '/analytics') return 'Overall Analytics';
    if (location.pathname === '/insights') return 'AI Insights';
    if (location.pathname === '/settings') return 'Settings';
    if (location.pathname.startsWith('/repositories/')) {
      const activeRepo = repos.find(r => r.id.toString() === selectedRepoId);
      return activeRepo ? activeRepo.repositoryName : 'Repository Details';
    }
    return 'GitPilot';
  };

  return (
    <header style={{
      height: 'var(--header-height)',
      backgroundColor: 'var(--bg-primary)',
      borderBottom: '1px solid var(--border-color)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 2rem',
      position: 'sticky',
      top: 0,
      zIndex: 90
    }}>
      {/* Title / Repo Selector */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 600, fontFamily: 'Outfit, sans-serif' }}>
          {getPageTitle()}
        </h2>

        {repos.length > 0 && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', backgroundColor: 'var(--bg-card)', padding: '0.25rem 0.75rem', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
            <GitBranch size={14} style={{ color: 'var(--accent-sky)' }} />
            <select
              value={selectedRepoId}
              onChange={handleRepoChange}
              style={{
                background: 'none',
                border: 'none',
                color: 'var(--text-primary)',
                fontFamily: 'Inter, sans-serif',
                fontSize: '0.8125rem',
                fontWeight: 500,
                outline: 'none',
                cursor: 'pointer',
                paddingRight: '0.5rem'
              }}
            >
              <option value="" style={{ background: 'var(--bg-card)', color: 'var(--text-secondary)' }}>
                -- Quick Repo Jump --
              </option>
              {repos.map(r => (
                <option key={r.id} value={r.id.toString()} style={{ background: 'var(--bg-card)', color: 'var(--text-primary)' }}>
                  {r.repositoryName}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* Quick Actions */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        {/* Sync Settings Link */}
        <Link 
          to="/settings" 
          style={{
            color: 'var(--text-secondary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: '36px',
            height: '36px',
            borderRadius: '8px',
            border: '1px solid var(--border-color)',
            backgroundColor: 'var(--bg-card)',
            transition: 'all 0.15s ease',
            textDecoration: 'none'
          }}
          title="Configure webhooks and repositories"
        >
          <Settings size={16} />
        </Link>
      </div>
    </header>
  );
}
