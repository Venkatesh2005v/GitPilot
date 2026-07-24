import React, { useState, useEffect } from 'react';
import { 
  GitPullRequest, 
  Save, 
  HelpCircle, 
  CheckCircle, 
  AlertCircle,
  Copy,
  Lock,
  Globe
} from 'lucide-react';

export function Settings() {
  const [gitRepos, setGitRepos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [alert, setAlert] = useState(null);

  // Fetch available repos from github
  const fetchGithubRepos = async () => {
    setLoading(true);
    setAlert(null);
    try {
      const response = await fetch('/github/repositories');
      if (response.ok) {
        const data = await response.json();
        setGitRepos(data);
      } else {
        setAlert({ type: 'error', message: 'Failed to fetch repositories from GitHub. Verify OAuth tokens.' });
      }
    } catch (e) {
      console.error("Failed to query github repos:", e);
      setAlert({ type: 'error', message: 'Network error. Backend is unreachable.' });
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
      // Find all githubRepoId where selected is true
      const selectedIds = gitRepos
        .filter(r => r.selected)
        .map(r => r.githubRepoId);

      const response = await fetch('/repositories/select', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ githubRepositoryIds: selectedIds })
      });

      if (response.ok) {
        setAlert({ type: 'success', message: 'Repository selection preferences saved successfully.' });
      } else {
        const errText = await response.text();
        setAlert({ type: 'error', message: `Failed to save selections: ${errText}` });
      }
    } catch (e) {
      console.error("Save selection failed:", e);
      setAlert({ type: 'error', message: 'Network error saving selections.' });
    } finally {
      setSaving(false);
    }
  };

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    // Visual indicator would be nice, but simple alert is robust
  };

  // Determine current host for payload webhook URL instruction
  const webhookUrl = `${window.location.origin}/webhooks/github`;

  return (
    <div className="fade-in">
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '2rem', fontWeight: 700, marginBottom: '0.5rem', fontFamily: 'Outfit, sans-serif' }}>Settings</h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Configure repository tracking selection preferences and real-time webhook sync.</p>
      </div>

      {/* Alert banner */}
      {alert && (
        <div className="card" style={{
          borderColor: alert.type === 'success' ? 'var(--accent-green)' : 'var(--accent-red)',
          backgroundColor: alert.type === 'success' ? 'var(--accent-green-glow)' : 'var(--accent-red-glow)',
          padding: '1rem',
          marginBottom: '1.5rem',
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem'
        }}>
          {alert.type === 'success' ? <CheckCircle size={18} style={{ color: 'var(--accent-green)' }} /> : <AlertCircle size={18} style={{ color: 'var(--accent-red)' }} />}
          <span style={{ fontSize: '0.875rem', fontWeight: 500 }}>{alert.message}</span>
        </div>
      )}

      <div className="grid-cols-2">
        
        {/* Repo Selections Card */}
        <div className="card" style={{ display: 'flex', flexDirection: 'column', maxHeight: '620px' }}>
          <h3 style={{ fontSize: '1.25rem', marginBottom: '0.5rem', fontFamily: 'Outfit, sans-serif' }}>Repository Selections</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem', marginBottom: '1.25rem' }}>
            Check repositories to sync and analyze metrics. Uncheck to stop tracking.
          </p>

          {loading ? (
            <div style={{ display: 'flex', flexGrow: 1, justifyContent: 'center', alignItems: 'center', minHeight: '200px' }}>
              <div className="pulse-indicator running" style={{ width: '20px', height: '20px' }}></div>
            </div>
          ) : gitRepos.length === 0 ? (
            <div style={{ display: 'flex', flexGrow: 1, flexDirection: 'column', justifyContent: 'center', alignItems: 'center', minHeight: '200px', color: 'var(--text-muted)' }}>
              <GitPullRequest size={24} style={{ marginBottom: '0.5rem' }} />
              <p style={{ fontSize: '0.875rem' }}>No repositories found under your GitHub profile.</p>
            </div>
          ) : (
            <>
              <div className="settings-list" style={{ flexGrow: 1 }}>
                {gitRepos.map(repo => (
                  <div key={repo.githubRepoId} className="settings-item">
                    <div>
                      <span style={{ fontWeight: 600, fontSize: '0.9rem', color: 'var(--text-primary)', wordBreak: 'break-all' }}>
                        {repo.name}
                      </span>
                      <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.25rem' }}>
                        <span className="badge badge-outline" style={{ fontSize: '0.625rem', padding: '0.1rem 0.4rem' }}>
                          Branch: {repo.defaultBranch || 'main'}
                        </span>
                        {repo.privateRepo ? (
                          <span className="badge badge-danger" style={{ fontSize: '0.625rem', padding: '0.1rem 0.4rem', gap: '0.2rem' }}>
                            <Lock size={8} /> Private
                          </span>
                        ) : (
                          <span className="badge badge-success" style={{ fontSize: '0.625rem', padding: '0.1rem 0.4rem', gap: '0.2rem' }}>
                            <Globe size={8} /> Public
                          </span>
                        )}
                      </div>
                    </div>

                    <label className="checkbox-container">
                      <input 
                        type="checkbox" 
                        checked={!!repo.selected}
                        onChange={() => handleCheckboxChange(repo.githubRepoId)}
                      />
                      <span className="custom-checkbox"></span>
                    </label>
                  </div>
                ))}
              </div>

              <button 
                onClick={handleSaveSelection} 
                className="btn btn-primary"
                disabled={saving}
                style={{ width: '100%', padding: '0.75rem', gap: '0.5rem' }}
              >
                <Save size={16} />
                <span>{saving ? 'Saving Choices...' : 'Save Selected Repositories'}</span>
              </button>
            </>
          )}
        </div>

        {/* Webhooks Setup Guide Card */}
        <div className="card">
          <h3 style={{ fontSize: '1.25rem', marginBottom: '0.5rem', fontFamily: 'Outfit, sans-serif' }}>GitHub Webhooks Configuration</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem', marginBottom: '1.25rem' }}>
            Set up real-time webhooks in your GitHub repository to update commit activities instantaneously.
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', fontSize: '0.875rem' }}>
            <div>
              <strong style={{ display: 'block', color: 'var(--text-primary)', marginBottom: '0.25rem' }}>1. Webhook Payload URL</strong>
              <span style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                Expose GitPilot to the internet (e.g. using ngrok or localtunnel in development) and paste the URL into GitHub:
              </span>
              <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem', alignItems: 'center' }}>
                <span className="code-panel" style={{ flexGrow: 1, margin: 0, padding: '0.5rem' }}>
                  {webhookUrl}
                </span>
                <button 
                  onClick={() => copyToClipboard(webhookUrl)}
                  className="btn btn-secondary"
                  style={{ padding: '0.5rem', flexShrink: 0 }}
                  title="Copy URL to clipboard"
                >
                  <Copy size={14} />
                </button>
              </div>
            </div>

            <div>
              <strong style={{ display: 'block', color: 'var(--text-primary)', marginBottom: '0.25rem' }}>2. Content Type</strong>
              <span style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                Set content type to:
              </span>
              <div className="code-panel" style={{ padding: '0.5rem', margin: '0.25rem 0' }}>
                application/json
              </div>
            </div>

            <div>
              <strong style={{ display: 'block', color: 'var(--text-primary)', marginBottom: '0.25rem' }}>3. Secret Signature</strong>
              <span style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                Specify the same secret defined in your environment variable:
              </span>
              <div className="code-panel" style={{ padding: '0.5rem', margin: '0.25rem 0' }}>
                GITHUB_WEBHOOK_SECRET
              </div>
            </div>

            <div>
              <strong style={{ display: 'block', color: 'var(--text-primary)', marginBottom: '0.25rem' }}>4. Which events to trigger?</strong>
              <span style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                Select: <strong>"Just the push event"</strong>.
              </span>
            </div>

            <div style={{
              display: 'flex',
              gap: '0.75rem',
              padding: '0.75rem',
              backgroundColor: 'var(--bg-secondary)',
              borderRadius: '8px',
              border: '1px solid var(--border-color)',
              marginTop: '0.5rem'
            }}>
              <HelpCircle size={20} style={{ color: 'var(--accent-sky)', flexShrink: 0 }} />
              <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', lineHeight: '1.4' }}>
                <strong>Tip:</strong> In GitHub, navigate to your repository's <em>Settings &gt; Webhooks &gt; Add Webhook</em> to configure these options. Real-time synchronization starts immediately upon successful registration.
              </p>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}
