import React, { useState, useEffect } from 'react';
import { 
  Cpu, 
  ChevronRight, 
  ThumbsUp, 
  ThumbsDown, 
  CheckCircle2, 
  Info,
  Clock,
  Layers,
  AlertTriangle,
  AlertCircle
} from 'lucide-react';
import { Link } from 'react-router-dom';

export function AIInsights() {
  const [repos, setRepos] = useState([]);
  const [selectedRepo, setSelectedRepo] = useState(null);
  
  // AI report states
  const [health, setHealth] = useState(null);
  const [summary, setSummary] = useState(null);
  const [recs, setRecs] = useState(null);
  const [loading, setLoading] = useState(true);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState(null);

  const fetchTrackedRepos = async () => {
    try {
      const response = await fetch('/dashboard/repositories');
      if (response.ok) {
        const data = await response.json();
        setRepos(data);
        if (data.length > 0) {
          // Select the first repo by default
          setSelectedRepo(data[0]);
        }
      }
    } catch (e) {
      console.error("Failed to load tracking repositories:", e);
    } finally {
      setLoading(false);
    }
  };

  const fetchAIReport = async (repoId) => {
    if (!repoId) return;
    setAiLoading(true);
    setAiError(null);
    try {
      const [healthRes, summaryRes, recsRes] = await Promise.all([
        fetch(`/ai/repositories/${repoId}/health`),
        fetch(`/ai/repositories/${repoId}/summary`),
        fetch(`/ai/repositories/${repoId}/recommendations`)
      ]);

      if (healthRes.status === 503 || summaryRes.status === 503 || recsRes.status === 503) {
        throw new Error("AI Gateway is currently offline (All Providers Failed).");
      }

      if (healthRes.ok && summaryRes.ok && recsRes.ok) {
        const hData = await healthRes.json();
        const sData = await summaryRes.json();
        const rData = await recsRes.json();

        setHealth(hData);
        setSummary(sData);
        setRecs(rData);
      } else {
        throw new Error("Failed to compile AI insights reports.");
      }
    } catch (e) {
      console.error("AI Report compile failed:", e);
      setAiError(e.message);
    } finally {
      setAiLoading(false);
    }
  };

  useEffect(() => {
    fetchTrackedRepos();
  }, []);

  useEffect(() => {
    if (selectedRepo) {
      fetchAIReport(selectedRepo.id);
    }
  }, [selectedRepo]);

  const handleRepoClick = (repo) => {
    setSelectedRepo(repo);
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <div className="pulse-indicator running" style={{ width: '24px', height: '24px' }}></div>
      </div>
    );
  }

  if (repos.length === 0) {
    return (
      <div className="fade-in">
        <h1 style={{ fontSize: '2rem', fontWeight: 700, marginBottom: '0.5rem', fontFamily: 'Outfit, sans-serif' }}>AI Insights</h1>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>Automatic health checks and engineering recommendations.</p>
        
        <div className="card" style={{ textAlign: 'center', padding: '4rem 2rem' }}>
          <AlertTriangle size={36} style={{ color: 'var(--accent-yellow)', marginBottom: '1.5rem' }} />
          <h3 style={{ marginBottom: '0.5rem' }}>No repositories to analyze</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '2rem' }}>
            To view AI insights, first choose repositories to track on settings.
          </p>
          <Link to="/settings" className="btn btn-primary">
            Settings Page
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="fade-in">
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '2rem', fontWeight: 700, marginBottom: '0.5rem', fontFamily: 'Outfit, sans-serif' }}>AI Insights</h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Automated code activity and health assessment compiled via multiple LLMs.</p>
      </div>

      <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap', alignItems: 'flex-start' }}>
        {/* Left Column: Repo Picker */}
        <div style={{ flex: '1 1 300px', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          <h3 style={{ fontSize: '1rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.25rem' }}>
            Select Repository
          </h3>
          {repos.map(r => {
            const isSelected = selectedRepo?.id === r.id;
            return (
              <div
                key={r.id}
                onClick={() => handleRepoClick(r)}
                style={{
                  padding: '1rem',
                  backgroundColor: isSelected ? 'var(--bg-card-hover)' : 'var(--bg-card)',
                  border: '1px solid',
                  borderColor: isSelected ? 'var(--accent-sky)' : 'var(--border-color)',
                  borderRadius: '10px',
                  cursor: 'pointer',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  transition: 'all 0.15s ease'
                }}
              >
                <div>
                  <h4 style={{ fontSize: '0.925rem', fontWeight: 600, color: 'var(--text-primary)' }}>{r.repositoryName}</h4>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Commits: {r.totalCommits || 0}</span>
                </div>
                
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  {r.healthScore !== null && (
                    <span className="badge badge-info" style={{ fontWeight: 600 }}>
                      {r.healthScore}
                    </span>
                  )}
                  <ChevronRight size={16} style={{ color: isSelected ? 'var(--accent-sky)' : 'var(--text-muted)' }} />
                </div>
              </div>
            );
          })}
        </div>

        {/* Right Column: AI Report detail */}
        <div style={{ flex: '2 1 600px', minWidth: 0 }}>
          {aiLoading ? (
            <div className="card" style={{ textAlign: 'center', padding: '5rem 2rem' }}>
              <div className="pulse-indicator running" style={{ width: '32px', height: '32px', marginBottom: '1.5rem' }}></div>
              <h3 style={{ fontFamily: 'Outfit, sans-serif', marginBottom: '0.5rem' }}>Analyzing "{selectedRepo?.repositoryName}"</h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Processing commits and metrics through the AI Gateway failover stack.</p>
            </div>
          ) : aiError ? (
            <div className="card" style={{ borderColor: 'var(--accent-red)', padding: '2.5rem' }}>
              <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
                <AlertCircle size={24} style={{ color: 'var(--accent-red)' }} />
                <div>
                  <h3 style={{ marginBottom: '0.5rem' }}>AI Report Generation Interrupted</h3>
                  <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '1.5rem' }}>{aiError}</p>
                  <button onClick={() => fetchAIReport(selectedRepo?.id)} className="btn btn-secondary">
                    Retry Analysis
                  </button>
                </div>
              </div>
            </div>
          ) : health ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              
              {/* Health Score Overview */}
              <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '2rem' }}>
                <div style={{
                  width: '100px',
                  height: '100px',
                  borderRadius: '50%',
                  border: '6px solid',
                  borderColor: health.healthScore >= 80 ? 'var(--accent-green)' : health.healthScore >= 50 ? 'var(--accent-yellow)' : 'var(--accent-red)',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0
                }}>
                  <strong style={{ fontSize: '1.75rem', fontFamily: 'Outfit' }}>{health.healthScore}</strong>
                  <span style={{ fontSize: '0.625rem', color: 'var(--text-secondary)', textTransform: 'uppercase' }}>Health</span>
                </div>
                <div>
                  <h2 style={{ fontSize: '1.5rem', fontFamily: 'Outfit' }}>{selectedRepo?.repositoryName}</h2>
                  <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                    Calculated by resolving developer frequencies and metadata commits updates.
                  </p>
                </div>
              </div>

              {/* Weekly summary */}
              <div className="card">
                <h4 style={{ fontSize: '0.95rem', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
                  <Info size={16} style={{ color: 'var(--accent-sky)' }} />
                  <span>Weekly Summary</span>
                </h4>
                <p style={{ color: 'var(--text-primary)', fontSize: '0.9rem', lineHeight: '1.6' }}>
                  {summary?.summary}
                </p>
              </div>

              {/* Strengths & Weaknesses */}
              <div className="grid-cols-2" style={{ marginBottom: 0 }}>
                <div className="card" style={{ borderColor: 'rgba(16, 185, 129, 0.15)' }}>
                  <h4 style={{ color: 'var(--accent-green)', display: 'flex', alignItems: 'center', gap: '0.35rem', marginBottom: '0.75rem', fontSize: '0.95rem' }}>
                    <ThumbsUp size={14} />
                    <span>Strengths</span>
                  </h4>
                  <ul style={{ paddingLeft: '1.15rem', fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                    {health.strengths?.map((s, idx) => (
                      <li key={idx} style={{ marginBottom: '0.35rem' }}>{s}</li>
                    ))}
                  </ul>
                </div>
                <div className="card" style={{ borderColor: 'rgba(239, 68, 68, 0.15)' }}>
                  <h4 style={{ color: 'var(--accent-red)', display: 'flex', alignItems: 'center', gap: '0.35rem', marginBottom: '0.75rem', fontSize: '0.95rem' }}>
                    <ThumbsDown size={14} />
                    <span>Weaknesses</span>
                  </h4>
                  <ul style={{ paddingLeft: '1.15rem', fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                    {health.weaknesses?.map((w, idx) => (
                      <li key={idx} style={{ marginBottom: '0.35rem' }}>{w}</li>
                    ))}
                  </ul>
                </div>
              </div>

              {/* Actionable Engineering Suggestions */}
              <div className="card">
                <h4 style={{ fontSize: '0.95rem', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
                  <CheckCircle2 size={16} style={{ color: 'var(--accent-indigo)' }} />
                  <span>AI Recommendations</span>
                </h4>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {recs?.recommendations?.map((r, idx) => (
                    <div key={idx} style={{ padding: '0.625rem 0.875rem', backgroundColor: 'var(--bg-secondary)', borderRadius: '6px', fontSize: '0.8125rem', color: 'var(--text-primary)', border: '1px solid var(--border-color)' }}>
                      {r}
                    </div>
                  ))}
                </div>
              </div>

              {/* Provider Config info */}
              <div className="card" style={{ padding: '1rem 1.5rem', display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-muted)', flexWrap: 'wrap', gap: '0.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                  <Cpu size={12} />
                  <span>Provider: <strong style={{ color: 'var(--text-secondary)' }}>{health.providerName}</strong> ({health.model})</span>
                </div>
                {health.fallbackUsed !== 'None' && (
                  <span style={{ color: 'var(--accent-yellow)' }}>Fallback Resolved: {health.fallbackUsed}</span>
                )}
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                  <Clock size={12} />
                  <span>Report Date: {formatDate(health.generatedTime)}</span>
                </div>
              </div>

            </div>
          ) : (
            <div className="card" style={{ textAlign: 'center', padding: '4rem 2rem' }}>
              <Cpu size={32} style={{ color: 'var(--text-muted)', marginBottom: '1rem' }} />
              <h4>Select a repository on the left to review its AI analysis.</h4>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
