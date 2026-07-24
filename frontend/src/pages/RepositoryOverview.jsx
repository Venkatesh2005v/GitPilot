import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { 
  GitCommit, 
  Users, 
  BarChart2, 
  Cpu, 
  Clock, 
  ExternalLink,
  ChevronLeft,
  Calendar,
  AlertCircle,
  ThumbsUp,
  ThumbsDown,
  Info,
  CheckCircle,
  Settings as SettingsIcon,
  GitBranch,
  ShieldAlert,
  Globe,
  Lock,
  Copy
} from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell, PieChart, Pie } from 'recharts';

export function RepositoryOverview({ setSelectedRepoId }) {
  const { id } = useParams();
  const [activeTab, setActiveTab] = useState('overview');
  
  // Data States
  const [activity, setActivity] = useState(null);
  const [commits, setCommits] = useState([]);
  const [contributors, setContributors] = useState([]);
  
  // AI report states
  const [aiHealth, setAiHealth] = useState(null);
  const [aiSummary, setAiSummary] = useState(null);
  const [aiRecs, setAiRecs] = useState(null);
  const [aiError, setAiError] = useState(null);
  
  // Load States
  const [loading, setLoading] = useState(true);
  const [aiLoading, setAiLoading] = useState(false);
  const [commitPage, setCommitPage] = useState(0);
  const [hasMoreCommits, setHasMoreCommits] = useState(true);

  const fetchRepoCoreData = async () => {
    setLoading(true);
    try {
      const [activityRes, contributorsRes] = await Promise.all([
        fetch(`/repositories/${id}/activity`),
        fetch(`/repositories/${id}/contributors`)
      ]);

      if (activityRes.ok) {
        const actData = await activityRes.json();
        setActivity(actData);
      }
      if (contributorsRes.ok) {
        const contribData = await contributorsRes.json();
        setContributors(contribData);
      }

      // Fetch initial page of commits
      const commitsRes = await fetch(`/repositories/${id}/commits?page=0&size=10`);
      if (commitsRes.ok) {
        const commitsData = await commitsRes.json();
        setCommits(commitsData);
        setHasMoreCommits(commitsData.length === 10);
      }
    } catch (e) {
      console.error("Error fetching repository core details:", e);
    } finally {
      setLoading(false);
    }
  };

  const loadMoreCommits = async () => {
    const nextPage = commitPage + 1;
    try {
      const response = await fetch(`/repositories/${id}/commits?page=${nextPage}&size=10`);
      if (response.ok) {
        const data = await response.json();
        if (data.length > 0) {
          setCommits(prev => [...prev, ...data]);
          setCommitPage(nextPage);
          setHasMoreCommits(data.length === 10);
        } else {
          setHasMoreCommits(false);
        }
      }
    } catch (e) {
      console.error("Error loading extra commits:", e);
    }
  };

  const fetchAIData = async () => {
    // Only fetch if not loaded already to save API calls
    if (aiHealth && aiSummary && aiRecs) return;
    setAiLoading(true);
    setAiError(null);
    try {
      const [healthRes, summaryRes, recsRes] = await Promise.all([
        fetch(`/ai/repositories/${id}/health`),
        fetch(`/ai/repositories/${id}/summary`),
        fetch(`/ai/repositories/${id}/recommendations`)
      ]);

      if (healthRes.status === 503 || summaryRes.status === 503 || recsRes.status === 503) {
        throw new Error("AI Gateway is currently offline. All provider failover attempts exhausted.");
      }

      if (healthRes.ok && summaryRes.ok && recsRes.ok) {
        const hData = await healthRes.json();
        const sData = await summaryRes.json();
        const rData = await recsRes.json();

        setAiHealth(hData);
        setAiSummary(sData);
        setAiRecs(rData);
      } else {
        throw new Error("Failed to load AI engineering insights.");
      }
    } catch (e) {
      console.error("AI Report generation failed:", e);
      setAiError(e.message);
    } finally {
      setAiLoading(false);
    }
  };

  useEffect(() => {
    fetchRepoCoreData();
    setSelectedRepoId(id);
  }, [id, setSelectedRepoId]);

  // Load AI details when matching tabs open
  useEffect(() => {
    if (activeTab === 'ai' || activeTab === 'overview') {
      fetchAIData();
    }
  }, [activeTab, id]);

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    const date = new Date(dateStr);
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
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

  const COLORS = ['#38bdf8', '#6366f1', '#10b981', '#f59e0b', '#ef4444', '#a855f7'];

  return (
    <div className="fade-in">
      {/* Breadcrumb back */}
      <div style={{ marginBottom: '1rem' }}>
        <Link to="/dashboard" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem', color: 'var(--text-secondary)', fontSize: '0.875rem', textDecoration: 'none', transition: 'color 0.15s ease' }} className="hover-sky">
          <ChevronLeft size={16} />
          <span>Back to Dashboard</span>
        </Link>
      </div>

      {/* Repo Details Header */}
      <div className="card" style={{ padding: '1.75rem', marginBottom: '2rem', background: 'linear-gradient(135deg, var(--bg-card), rgba(22, 31, 48, 0.4))' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 800, marginBottom: '0.5rem', fontFamily: 'Outfit, sans-serif' }}>
              {activity?.repositoryName}
            </h1>
            <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                <Clock size={14} />
                <span>Last commit: {formatDate(activity?.latestCommitDate)}</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                <Users size={14} />
                <span>Active Contributors: {contributors.length}</span>
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <a 
              href={`https://github.com/check-or-mock-repo-link`}
              target="_blank" 
              rel="noopener noreferrer" 
              className="btn btn-secondary"
              style={{ fontSize: '0.8125rem' }}
            >
              <span>View GitHub</span>
              <ExternalLink size={12} />
            </a>
          </div>
        </div>

        {/* Sync Mini Metrics */}
        <div style={{ display: 'flex', gap: '2rem', marginTop: '1.5rem', borderTop: '1px solid var(--border-color)', paddingTop: '1rem', flexWrap: 'wrap' }}>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>TOTAL COMMITS</span>
            <div style={{ fontSize: '1.25rem', fontWeight: 700, fontFamily: 'Outfit' }}>{activity?.totalCommits || 0}</div>
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>7-DAY COMMITS</span>
            <div style={{ fontSize: '1.25rem', fontWeight: 700, fontFamily: 'Outfit' }}>{activity?.commitsLast7Days || 0}</div>
          </div>
          {aiHealth?.healthScore && (
            <div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>AI HEALTH SCORE</span>
              <div style={{ fontSize: '1.25rem', fontWeight: 700, fontFamily: 'Outfit', color: 'var(--accent-sky)' }}>{aiHealth.healthScore}/100</div>
            </div>
          )}
        </div>
      </div>

      {/* Tabs Layout */}
      <div className="tabs-container">
        <button className={`tab-btn ${activeTab === 'overview' ? 'active' : ''}`} onClick={() => setActiveTab('overview')}>
          Overview
        </button>
        <button className={`tab-btn ${activeTab === 'analytics' ? 'active' : ''}`} onClick={() => setActiveTab('analytics')}>
          Analytics
        </button>
        <button className={`tab-btn ${activeTab === 'commits' ? 'active' : ''}`} onClick={() => setActiveTab('commits')}>
          Commits
        </button>
        <button className={`tab-btn ${activeTab === 'ai' ? 'active' : ''}`} onClick={() => setActiveTab('ai')}>
          AI Insights
        </button>
        <button className={`tab-btn ${activeTab === 'contributors' ? 'active' : ''}`} onClick={() => setActiveTab('contributors')}>
          Contributors
        </button>
        <button className={`tab-btn ${activeTab === 'settings' ? 'active' : ''}`} onClick={() => setActiveTab('settings')}>
          Settings
        </button>
      </div>

      {/* Tab Content: Overview */}
      {activeTab === 'overview' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* Metadata Cards */}
          <div className="grid-cols-4" style={{ marginBottom: 0 }}>
            <div className="card">
              <span className="card-title">Default Branch</span>
              <div className="card-value" style={{ fontSize: '1.25rem', margin: '0.5rem 0' }}>
                <GitBranch size={16} style={{ color: 'var(--accent-sky)', marginRight: '0.5rem' }} />
                main
              </div>
              <span className="card-desc">Production branch tracking</span>
            </div>

            <div className="card">
              <span className="card-title">Visibility</span>
              <div className="card-value" style={{ fontSize: '1.25rem', margin: '0.5rem 0' }}>
                <Globe size={16} style={{ color: 'var(--accent-green)', marginRight: '0.5rem' }} />
                Public
              </div>
              <span className="card-desc">GitHub repository visibility</span>
            </div>

            <div className="card">
              <span className="card-title">Active Contributors</span>
              <div className="card-value" style={{ fontSize: '1.25rem', margin: '0.5rem 0' }}>
                <Users size={16} style={{ color: 'var(--accent-indigo)', marginRight: '0.5rem' }} />
                {contributors.length}
              </div>
              <span className="card-desc">Code committers ranking</span>
            </div>

            <div className="card">
              <span className="card-title">Health Index</span>
              <div className="card-value" style={{ fontSize: '1.25rem', margin: '0.5rem 0', color: 'var(--accent-sky)' }}>
                {aiHealth?.healthScore ? `${aiHealth.healthScore}/100` : '-'}
              </div>
              <span className="card-desc">Calculated by AI Gateway</span>
            </div>
          </div>

          {/* AI Weekly summary */}
          <div className="card">
            <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Info size={16} style={{ color: 'var(--accent-sky)' }} />
              <span>Weekly Engineering Summary</span>
            </h3>
            {aiLoading ? (
              <div className="pulse-indicator running" style={{ width: '16px', height: '16px' }}></div>
            ) : aiError ? (
              <span style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>AI Report unavailable. Check provider key variables.</span>
            ) : (
              <p style={{ color: 'var(--text-primary)', fontSize: '0.925rem', lineHeight: '1.6' }}>
                {aiSummary?.summary}
              </p>
            )}
          </div>
        </div>
      )}

      {/* Tab Content: Analytics */}
      {activeTab === 'analytics' && (
        <div className="grid-cols-2">
          {/* Contribution splits bar chart */}
          <div className="card">
            <h3 style={{ fontSize: '1.1rem', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <BarChart2 size={16} style={{ color: 'var(--accent-sky)' }} />
              <span>Contribution Volume (Commits count)</span>
            </h3>
            {contributors.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>No data available for charts.</p>
            ) : (
              <div style={{ width: '100%', height: 260 }}>
                <ResponsiveContainer>
                  <BarChart data={contributors.slice(0, 6)} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                    <XAxis dataKey="authorName" stroke="var(--text-muted)" fontSize={10} tickLine={false} />
                    <YAxis stroke="var(--text-muted)" fontSize={10} tickLine={false} />
                    <Tooltip contentStyle={{ backgroundColor: 'var(--bg-card)', borderColor: 'var(--border-color)', borderRadius: '8px' }} />
                    <Bar dataKey="commitCount" radius={[4, 4, 0, 0]}>
                      {contributors.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </div>

          {/* Distribution splits Pie Chart */}
          <div className="card" style={{ display: 'flex', flexDirection: 'column' }}>
            <h3 style={{ fontSize: '1.1rem', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Users size={16} style={{ color: 'var(--accent-indigo)' }} />
              <span>Percentage Distribution</span>
            </h3>
            {contributors.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>No data available for charts.</p>
            ) : (
              <div style={{ display: 'flex', flexGrow: 1, alignItems: 'center', justifyContent: 'center' }}>
                <div style={{ width: '100%', height: 200 }}>
                  <ResponsiveContainer>
                    <PieChart>
                      <Pie
                        data={contributors.slice(0, 6)}
                        dataKey="commitCount"
                        nameKey="authorName"
                        cx="50%"
                        cy="50%"
                        outerRadius={70}
                        fill="#8884d8"
                        label={({ name, percent }) => `${name.substring(0, 8)} (${(percent * 100).toFixed(0)}%)`}
                        labelLine={false}
                        style={{ fontSize: '10px', fill: 'var(--text-primary)' }}
                      >
                        {contributors.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Tab Content: Commits */}
      {activeTab === 'commits' && (
        <div className="card">
          <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
            Repository Commits List
          </h3>
          {commits.length === 0 ? (
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>No commits found. Send a push or sync commits to get started.</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {commits.map((c, idx) => (
                <div key={c.sha || idx} style={{ borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.25rem' }}>
                    <a href={c.commitUrl} target="_blank" rel="noopener noreferrer" style={{ color: 'var(--accent-sky)', textDecoration: 'none', fontWeight: 600, fontSize: '0.875rem', fontFamily: 'monospace' }}>
                      {c.sha ? c.sha.substring(0, 7) : 'Commit'}
                    </a>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                      <Calendar size={12} />
                      {formatDate(c.commitDate)}
                    </span>
                  </div>
                  <p style={{ fontSize: '0.875rem', color: 'var(--text-primary)', marginBottom: '0.25rem' }}>
                    {c.message}
                  </p>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                    Author: {c.authorName} ({c.authorEmail})
                  </span>
                </div>
              ))}

              {hasMoreCommits && (
                <button onClick={loadMoreCommits} className="btn btn-secondary" style={{ width: '100%', marginTop: '0.5rem' }}>
                  Load More Commits
                </button>
              )}
            </div>
          )}
        </div>
      )}

      {/* Tab Content: AI Insights */}
      {activeTab === 'ai' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {aiLoading && (
            <div className="card" style={{ textAlign: 'center', padding: '4rem 2rem' }}>
              <div className="pulse-indicator running" style={{ width: '32px', height: '32px', marginBottom: '1rem' }}></div>
              <h4 style={{ color: 'var(--text-primary)', marginBottom: '0.5rem', fontFamily: 'Outfit' }}>
                Fetching Deep AI Analysis...
              </h4>
            </div>
          )}

          {!aiLoading && aiError && (
            <div className="card" style={{ borderColor: 'var(--accent-red)', padding: '2rem' }}>
              <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
                <AlertCircle size={24} style={{ color: 'var(--accent-red)', flexShrink: 0 }} />
                <div>
                  <h4 style={{ color: 'var(--text-primary)', marginBottom: '0.5rem' }}>AI Report Generation failed</h4>
                  <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '1.5rem' }}>
                    {aiError}
                  </p>
                  <button onClick={fetchAIData} className="btn btn-secondary">
                    Retry Analysis
                  </button>
                </div>
              </div>
            </div>
          )}

          {!aiLoading && !aiError && aiHealth && (
            <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              
              {/* Score / Metadata row */}
              <div className="grid-cols-2" style={{ marginBottom: 0 }}>
                {/* Health Score Card */}
                <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '2rem' }}>
                  <div style={{
                    width: '100px',
                    height: '100px',
                    borderRadius: '50%',
                    border: '6px solid var(--border-color)',
                    borderColor: aiHealth.healthScore >= 80 ? 'var(--accent-green)' : aiHealth.healthScore >= 50 ? 'var(--accent-yellow)' : 'var(--accent-red)',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0
                  }}>
                    <span style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-primary)', fontFamily: 'Outfit' }}>
                      {aiHealth.healthScore}
                    </span>
                    <span style={{ fontSize: '0.625rem', color: 'var(--text-secondary)', textTransform: 'uppercase' }}>Score</span>
                  </div>
                  <div>
                    <h3 style={{ fontSize: '1.15rem', fontFamily: 'Outfit' }}>Repository Health</h3>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                      Determined dynamically based on commit volume trends.
                    </p>
                  </div>
                </div>

                {/* AI Configuration details */}
                <div className="card">
                  <h4 style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.75rem' }}>
                    AI Gateway Configuration
                  </h4>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', fontSize: '0.8125rem' }}>
                    <div style={{ display: 'flex', justifyBetween: 'space-between' }}>
                      <span style={{ color: 'var(--text-muted)' }}>Provider: </span>
                      <strong style={{ color: 'var(--text-primary)' }}>{aiHealth.providerName}</strong>
                    </div>
                    <div style={{ display: 'flex', justifyBetween: 'space-between' }}>
                      <span style={{ color: 'var(--text-muted)' }}>Model: </span>
                      <span style={{ color: 'var(--text-secondary)', fontFamily: 'monospace' }}>{aiHealth.model}</span>
                    </div>
                    <div style={{ display: 'flex', justifyBetween: 'space-between' }}>
                      <span style={{ color: 'var(--text-muted)' }}>Fallback: </span>
                      <span className={`badge ${aiHealth.fallbackUsed !== 'None' ? 'badge-warning' : 'badge-outline'}`}>
                        {aiHealth.fallbackUsed}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Strengths & Weaknesses */}
              <div className="grid-cols-2" style={{ marginBottom: 0 }}>
                <div className="card" style={{ borderColor: 'rgba(16, 185, 129, 0.2)' }}>
                  <h4 style={{ color: 'var(--accent-green)', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', fontSize: '0.95rem' }}>
                    <ThumbsUp size={14} />
                    <span>Strengths</span>
                  </h4>
                  <ul style={{ paddingLeft: '1.25rem', fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                    {aiHealth.strengths?.map((str, idx) => (
                      <li key={idx} style={{ marginBottom: '0.5rem' }}>{str}</li>
                    ))}
                  </ul>
                </div>

                <div className="card" style={{ borderColor: 'rgba(239, 68, 68, 0.2)' }}>
                  <h4 style={{ color: 'var(--accent-red)', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', fontSize: '0.95rem' }}>
                    <ThumbsDown size={14} />
                    <span>Weaknesses</span>
                  </h4>
                  <ul style={{ paddingLeft: '1.25rem', fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                    {aiHealth.weaknesses?.map((weak, idx) => (
                      <li key={idx} style={{ marginBottom: '0.5rem' }}>{weak}</li>
                    ))}
                  </ul>
                </div>
              </div>

              {/* Recommendations list */}
              <div className="card">
                <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <CheckCircle size={16} style={{ color: 'var(--accent-indigo)' }} />
                  <span>Actionable Recommendations</span>
                </h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {aiRecs?.recommendations?.map((rec, idx) => (
                    <div key={idx} style={{ display: 'flex', gap: '0.75rem', padding: '0.75rem', backgroundColor: 'var(--bg-secondary)', borderRadius: '6px', border: '1px solid var(--border-color)', fontSize: '0.8125rem' }}>
                      <span style={{ color: 'var(--accent-indigo)', fontWeight: 'bold' }}>{idx + 1}.</span>
                      <p style={{ color: 'var(--text-primary)' }}>{rec}</p>
                    </div>
                  ))}
                </div>
              </div>

            </div>
          )}
        </div>
      )}

      {/* Tab Content: Contributors */}
      {activeTab === 'contributors' && (
        <div className="card">
          <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
            Contributor Rankings
          </h3>
          {contributors.length === 0 ? (
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>No contributor ranking aggregates found.</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {contributors.map((contrib, index) => (
                <div key={contrib.authorName || index} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem 0' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <span style={{
                      width: '24px',
                      height: '24px',
                      borderRadius: '50%',
                      backgroundColor: index === 0 ? 'var(--accent-yellow)' : 'var(--border-color)',
                      color: index === 0 ? '#000' : 'var(--text-secondary)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: 'bold',
                      fontSize: '0.75rem'
                    }}>
                      {index + 1}
                    </span>
                    <span style={{ fontSize: '0.875rem', fontWeight: 500 }}>{contrib.authorName}</span>
                  </div>
                  <span className="badge badge-outline" style={{ fontSize: '0.8rem' }}>
                    {contrib.commitCount} commits
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Tab Content: Settings */}
      {activeTab === 'settings' && (
        <div className="grid-cols-2">
          {/* Sync status card */}
          <div className="card">
            <h3 style={{ fontSize: '1.15rem', marginBottom: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <SettingsIcon size={16} style={{ color: 'var(--accent-sky)' }} />
              <span>Synchronization Status</span>
            </h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', fontSize: '0.875rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-muted)' }}>Status:</span>
                <span className={`badge ${activity?.lastSyncStatus === 'SUCCESS' ? 'badge-success' : 'badge-danger'}`}>
                  {activity?.lastSyncStatus || 'Pending'}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-muted)' }}>Last Synced Time:</span>
                <span style={{ color: 'var(--text-primary)' }}>{formatDate(activity?.latestCommitDate)}</span>
              </div>
              {activity?.lastSyncDuration && (
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Duration:</span>
                  <span style={{ color: 'var(--text-primary)' }}>{activity.lastSyncDuration} ms</span>
                </div>
              )}
            </div>
          </div>

          {/* Webhook Instructions details */}
          <div className="card">
            <h3 style={{ fontSize: '1.15rem', marginBottom: '0.5rem', fontFamily: 'Outfit, sans-serif' }}>Webhook Parameters</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem', marginBottom: '1rem' }}>
              To receive updates in real-time, configure the following Webhook parameters inside your repository settings on GitHub:
            </p>
            <div style={{ fontSize: '0.8125rem' }}>
              <strong style={{ color: 'var(--text-primary)', display: 'block', marginBottom: '0.25rem' }}>Payload URL</strong>
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginBottom: '0.75rem' }}>
                <span className="code-panel" style={{ flexGrow: 1, margin: 0, padding: '0.4rem' }}>
                  {`${window.location.origin}/webhooks/github`}
                </span>
              </div>
              
              <strong style={{ color: 'var(--text-primary)', display: 'block', marginBottom: '0.25rem' }}>Content Type</strong>
              <div className="code-panel" style={{ padding: '0.4rem', margin: '0 0 0.75rem 0' }}>
                application/json
              </div>

              <strong style={{ color: 'var(--text-primary)', display: 'block', marginBottom: '0.25rem' }}>Secret</strong>
              <div className="code-panel" style={{ padding: '0.4rem', margin: '0 0 0.75rem 0' }}>
                GITHUB_WEBHOOK_SECRET
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
