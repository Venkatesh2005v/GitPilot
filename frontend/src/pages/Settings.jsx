import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  GitPullRequest, 
  Save, 
  HelpCircle, 
  CheckCircle2, 
  AlertCircle,
  Copy,
  Lock,
  Globe,
  Monitor,
  Terminal,
  ShieldCheck,
  Zap
} from 'lucide-react';
import { ThemeToggle } from '../components/ThemeToggle';
import { useTheme } from '../context/ThemeContext';
import { Skeleton } from '../components/Skeleton';
import { apiFetch } from '../utils/apiUtils';

export function Settings() {
  const { theme, effectiveTheme } = useTheme();
  const [gitRepos, setGitRepos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [alert, setAlert] = useState(null);
  const [copied, setCopied] = useState(false);

  const webhookUrl = `${window.location.origin}/webhooks/github`;

  const fetchGithubRepos = async () => {
    setLoading(true);
    setAlert(null);
    try {
      const response = await apiFetch('/github/repositories');
      if (response.ok) {
        const data = await response.json();
        setGitRepos(data);
      } else {
        setGitRepos([]);
        setAlert({ type: 'error', message: 'Failed to load repositories from GitHub. Please ensure you are authenticated.' });
      }
    } catch (e) {
      setGitRepos([]);
      setAlert({ type: 'error', message: 'Network error loading repositories. Please check your connection.' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchGithubRepos();
  }, []);

  const handleCheckboxChange = (repoId) => {
    setGitRepos(prev => prev.map(repo => {
      if (repo.githubRepoId === repoId) {
        return { ...repo, selected: !repo.selected };
      }
      return repo;
    }));
  };

  const handleSaveSelection = async () => {
    setSaving(true);
    setAlert(null);
    try {
      const selectedIds = gitRepos
        .filter(r => r.selected)
        .map(r => r.githubRepoId);

      const response = await apiFetch('/repositories/select', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ githubRepositoryIds: selectedIds })
      });

      if (response.ok) {
        setAlert({ type: 'success', message: 'Repository tracking preferences saved successfully!' });
      } else {
        setAlert({ type: 'error', message: 'Failed to save repository preferences. Please try again.' });
      }
    } catch (e) {
      setAlert({ type: 'error', message: 'Network error saving preferences. Please check your connection.' });
    } finally {
      setSaving(false);
    }
  };

  const handleCopyWebhook = () => {
    navigator.clipboard.writeText(webhookUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
        <Skeleton height="100px" borderRadius="1.75rem" />
        <div className="bento-grid">
          <div className="bento-span-7"><Skeleton height="320px" borderRadius="1.75rem" /></div>
          <div className="bento-span-5"><Skeleton height="320px" borderRadius="1.75rem" /></div>
        </div>
      </div>
    );
  }

  return (
    <motion.div initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }} className="ambient-page-bg" style={{ padding: '0 0 3rem 0' }}>
      
      {/* Header Title */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
            <span className="badge badge-primary">SYSTEM PREFERENCES</span>
            <span className="badge badge-teal">WEBHOOK INTEGRATION</span>
          </div>
          <h1 style={{ fontSize: '2.25rem', fontWeight: 800, fontFamily: 'Space Grotesk, sans-serif' }}>
            Settings & Telemetry
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.925rem' }}>
            Configure GitHub repository tracking preferences, push event webhooks, and theme appearance.
          </p>
        </div>

        <button onClick={handleSaveSelection} className="btn btn-primary" disabled={saving}>
          <Save size={16} />
          <span>{saving ? 'Saving...' : 'Save Settings'}</span>
        </button>
      </div>

      {alert && (
        <div className="card-3xl" style={{ 
          marginBottom: '2rem', 
          padding: '1rem 1.5rem', 
          backgroundColor: alert.type === 'error' ? 'var(--accent-red-bg)' : 'var(--accent-green-bg)',
          borderColor: alert.type === 'error' ? 'var(--accent-red)' : 'var(--accent-green)',
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem'
        }}>
          <CheckCircle2 size={20} style={{ color: alert.type === 'error' ? 'var(--accent-red)' : 'var(--accent-green)' }} />
          <span style={{ fontSize: '0.9rem', fontWeight: 600 }}>{alert.message}</span>
        </div>
      )}

      {/* Bento Grid */}
      <div className="bento-grid">
        
        {/* Repository Tracking Selector (Span 7) */}
        <motion.div whileHover={{ y: -4 }} className="card-3xl bento-span-7">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <GitPullRequest size={20} style={{ color: 'var(--accent-primary)' }} />
              <span>Tracked Repositories</span>
            </h3>
            <span className="badge badge-primary">{gitRepos.filter(r => r.selected).length} ACTIVE</span>
          </div>

          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '1.25rem' }}>
            Select which connected GitHub repositories should be indexed for real-time telemetry and AI health analysis:
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginBottom: '1.5rem' }}>
            {gitRepos.map((repo) => (
              <label 
                key={repo.githubRepoId}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '0.9rem 1.25rem',
                  background: 'var(--bg-secondary)',
                  borderRadius: '1.25rem',
                  border: repo.selected ? '1px solid var(--border-color-hover)' : '1px solid var(--border-color)',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
                  <input 
                    type="checkbox"
                    checked={repo.selected || false}
                    onChange={() => handleCheckboxChange(repo.githubRepoId)}
                    style={{ width: '18px', height: '18px', accentColor: 'var(--accent-primary)', cursor: 'pointer' }}
                  />
                  <div>
                    <strong style={{ fontSize: '0.925rem', display: 'block', color: 'var(--text-primary)' }}>{repo.repositoryName || repo.fullName}</strong>
                    <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>GitHub ID: {repo.githubRepoId}</span>
                  </div>
                </div>
                {repo.selected && <span className="badge badge-teal" style={{ fontSize: '0.7rem' }}>TRACKED</span>}
              </label>
            ))}
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <button onClick={handleSaveSelection} className="btn btn-primary" disabled={saving}>
              <Save size={16} />
              <span>Save Tracking Preferences</span>
            </button>
          </div>
        </motion.div>

        {/* GitHub Webhook Endpoint & Secret (Span 5) */}
        <motion.div whileHover={{ y: -4 }} className="card-3xl bento-span-5" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h3 style={{ fontSize: '1.2rem', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Terminal size={20} style={{ color: 'var(--accent-teal)' }} />
                <span>GitHub Push Webhook</span>
              </h3>
              <span className="badge badge-teal">LIVE LISTENER</span>
            </div>

            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '1.25rem', lineHeight: 1.6 }}>
              Configure your GitHub repository Webhooks to push commit events directly to GitPilot:
            </p>

            {/* Webhook Payload URL Box */}
            <div style={{ marginBottom: '1.25rem' }}>
              <label style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', display: 'block', marginBottom: '0.4rem', textTransform: 'uppercase' }}>
                Payload URL
              </label>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <input 
                  type="text" 
                  readOnly 
                  value={webhookUrl}
                  style={{
                    flex: 1,
                    padding: '0.65rem 1rem',
                    background: 'var(--bg-secondary)',
                    border: '1px solid var(--border-color)',
                    borderRadius: '1rem',
                    color: 'var(--text-primary)',
                    fontFamily: 'JetBrains Mono',
                    fontSize: '0.8rem',
                    outline: 'none'
                  }}
                />
                <button onClick={handleCopyWebhook} className="btn btn-secondary" style={{ padding: '0.65rem 1rem' }}>
                  <Copy size={16} />
                  <span>{copied ? 'Copied!' : 'Copy'}</span>
                </button>
              </div>
            </div>

            <div style={{ padding: '1rem', background: 'var(--bg-secondary)', borderRadius: '1.15rem', border: '1px solid var(--border-color)', fontSize: '0.825rem', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem', color: 'var(--text-primary)', fontWeight: 600 }}>
                <Zap size={14} style={{ color: 'var(--accent-primary)' }} />
                <span>Webhook Event Configuration</span>
              </div>
              Select <strong>Push events</strong> and set Content type to <code>application/json</code>.
            </div>
          </div>

          {/* Theme Selector Section */}
          <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '1.25rem', marginTop: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <strong style={{ fontSize: '0.9rem', display: 'block', color: 'var(--text-primary)' }}>Interface Theme</strong>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Active: {effectiveTheme.toUpperCase()}</span>
              </div>
              <ThemeToggle />
            </div>
          </div>
        </motion.div>

      </div>
    </motion.div>
  );
}
