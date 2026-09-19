import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { 
  GitBranch, 
  Layers, 
  TrendingUp, 
  RefreshCw, 
  ChevronRight,
  ShieldCheck,
  Clock,
  GitCommit,
  Sparkles,
  Code2,
  Zap,
  CheckCircle2,
  BookOpen
} from 'lucide-react';
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';
import { CircularProgress } from '../components/CircularProgress';
import { VerticalTimeline } from '../components/VerticalTimeline';
import { RecommendationCard } from '../components/RecommendationCard';
import { TechPill } from '../components/TechPill';
import { Skeleton } from '../components/Skeleton';
import { apiFetch } from '../utils/apiUtils';

export function Dashboard({ setSelectedRepoId }) {
  const navigate = useNavigate();
  const [summary, setSummary] = useState(null);
  const [repos, setRepos] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // Aggregated data states
  const [aggregatedCommits, setAggregatedCommits] = useState([]);
  const [aggregatedContributors, setAggregatedContributors] = useState([]);
  const [selectedRepo, setSelectedRepo] = useState(null);

  useEffect(() => {
    let isSubscribed = true;
    const controller = new AbortController();
    const { signal } = controller;

    const fetchDashboardData = async () => {
      setLoading(true);
      try {
        const [summaryRes, reposRes] = await Promise.all([
          apiFetch('/dashboard/summary', { signal }),
          apiFetch('/dashboard/repositories', { signal })
        ]);

        if (!isSubscribed) return;

        let summaryData = null;
        let reposData = [];

        if (summaryRes.ok) summaryData = await summaryRes.json();
        if (reposRes.ok) reposData = await reposRes.json();

        if (isSubscribed) {
          if (summaryData) setSummary(summaryData);
          if (reposData) {
            setRepos(reposData);
            if (reposData.length > 0) {
              setSelectedRepo(reposData[0]);
            }
          }
        }

        if (reposData.length > 0 && isSubscribed) {
          const selectedRepos = reposData.filter(r => r.totalCommits > 0);
          
          const commitPromises = selectedRepos.map(async (repo) => {
            try {
              const res = await apiFetch(`/repositories/${repo.id}/commits?page=0&size=10`, { signal });
              if (res.ok) {
                const data = await res.json();
                return data.map(c => ({ ...c, repoName: repo.repositoryName, repoId: repo.id }));
              }
            } catch (ignored) {}
            return [];
          });

          const contributorPromises = selectedRepos.map(async (repo) => {
            try {
              const res = await apiFetch(`/repositories/${repo.id}/contributors`, { signal });
              if (res.ok) {
                const data = await res.json();
                return data;
              }
            } catch (ignored) {}
            return [];
          });

          const allCommitsArrays = await Promise.all(commitPromises);
          const allContributorsArrays = await Promise.all(contributorPromises);

          if (!isSubscribed) return;

          const mergedCommits = allCommitsArrays.flat()
            .sort((a, b) => new Date(b.commitDate) - new Date(a.commitDate))
            .slice(0, 5);
          
          if (mergedCommits.length > 0) {
            setAggregatedCommits(mergedCommits);
          }

          const mergedContributors = allContributorsArrays.flat();
          const contribMap = {};
          mergedContributors.forEach(c => {
            if (!contribMap[c.authorName]) {
              contribMap[c.authorName] = { ...c };
            } else {
              contribMap[c.authorName].commitCount += c.commitCount;
            }
          });
          
          const sortedContributors = Object.values(contribMap)
            .sort((a, b) => b.commitCount - a.commitCount)
            .slice(0, 4);

          if (sortedContributors.length > 0) {
            setAggregatedContributors(sortedContributors);
          }
        }
      } catch (e) {
        if (e.name !== 'AbortError' && isSubscribed) {
          console.error("Dashboard fetch error:", e);
        }
      } finally {
        if (isSubscribed) {
          setLoading(false);
        }
      }
    };

    fetchDashboardData();

    return () => {
      isSubscribed = false;
      controller.abort();
    };
  }, []);

  const handleSelectRepo = (repo) => {
    setSelectedRepo(repo);
    if (setSelectedRepoId) {
      setSelectedRepoId(repo.id.toString());
    }
  };

  const handleOpenRepoDetails = (repoId) => {
    if (setSelectedRepoId) {
      setSelectedRepoId(repoId.toString());
    }
    navigate(`/repositories/${repoId}`);
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
        <Skeleton height="160px" borderRadius="2rem" />
        <div className="bento-grid">
          <div className="bento-span-8"><Skeleton height="320px" borderRadius="1.75rem" /></div>
          <div className="bento-span-4"><Skeleton height="320px" borderRadius="1.75rem" /></div>
        </div>
      </div>
    );
  }

  const activeRepo = selectedRepo || (repos.length > 0 ? repos[0] : null);

  if (!activeRepo && repos.length === 0) {
    return (
      <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.35 }}>
        <div className="card-3xl" style={{ padding: '3rem', textAlign: 'center' }}>
          <GitBranch size={48} style={{ color: 'var(--text-muted)', marginBottom: '1rem' }} />
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.75rem' }}>No Repositories Selected</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', marginBottom: '1.5rem' }}>
            Go to Settings to select repositories from your GitHub account and start tracking them.
          </p>
          <button onClick={() => navigate('/settings')} className="btn btn-primary" style={{ padding: '0.75rem 1.5rem' }}>
            <span>Go to Settings</span>
            <ChevronRight size={16} />
          </button>
        </div>
      </motion.div>
    );
  }

  return (
    <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.35 }}>
      {/* 1. Header Metrics Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.25rem', marginBottom: '2rem' }}>
        
        <div className="card-3xl" style={{ padding: '1.5rem', background: 'var(--bg-card-solid)', border: '1px solid var(--border-color)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>SELECTED REPOS</span>
            <GitBranch size={20} style={{ color: 'var(--accent-primary)' }} />
          </div>
          <div style={{ fontSize: '2.25rem', fontWeight: 800, fontFamily: 'Space Grotesk', letterSpacing: '-0.02em', color: 'var(--text-primary)' }}>
            {summary?.selectedRepositories ?? repos.length ?? 0}
          </div>
          <div style={{ fontSize: '0.78rem', color: 'var(--accent-green)', marginTop: '0.35rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
            <CheckCircle2 size={12} />
            <span>Active Webhooks</span>
          </div>
        </div>

        <div className="card-3xl" style={{ padding: '1.5rem', background: 'var(--bg-card-solid)', border: '1px solid var(--border-color)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>TOTAL COMMITS</span>
            <GitCommit size={20} style={{ color: 'var(--accent-teal)' }} />
          </div>
          <div style={{ fontSize: '2.25rem', fontWeight: 800, fontFamily: 'Space Grotesk', letterSpacing: '-0.02em', color: 'var(--text-primary)' }}>
            {summary?.totalCommits ?? activeRepo.totalCommits ?? 0}
          </div>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', marginTop: '0.35rem' }}>
            +{summary?.commitsLast7Days ?? 0} commits this week
          </div>
        </div>

        <div className="card-3xl" style={{ padding: '1.5rem', background: 'var(--bg-card-solid)', border: '1px solid var(--border-color)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>SYSTEM HEALTH</span>
            <Sparkles size={20} style={{ color: 'var(--accent-primary)' }} />
          </div>
          <div style={{ fontSize: '2.25rem', fontWeight: 800, fontFamily: 'Space Grotesk', letterSpacing: '-0.02em', color: 'var(--accent-green)' }}>
            {activeRepo.healthScore ?? '—'}/100
          </div>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', marginTop: '0.35rem' }}>
            AI Architecture Score: Excellent
          </div>
        </div>

        <div className="card-3xl" style={{ padding: '1.5rem', background: 'var(--bg-card-solid)', border: '1px solid var(--border-color)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>LAST SYNCHRONIZED</span>
            <Clock size={20} style={{ color: 'var(--text-muted)' }} />
          </div>
          <div style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'JetBrains Mono', color: 'var(--text-primary)', marginTop: '0.35rem' }}>
            {summary?.lastSynchronization ? new Date(summary.lastSynchronization).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Just now'}
          </div>
          <div style={{ fontSize: '0.78rem', color: 'var(--accent-teal)', marginTop: '0.35rem', fontWeight: 600 }}>
            Sync Status: SUCCESS
          </div>
        </div>

      </div>

      {/* 2. Repository Selector & Overview Cards */}
      <div className="card-3xl" style={{ padding: '2rem', marginBottom: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h2 style={{ fontSize: '1.5rem', fontWeight: 800, fontFamily: 'Space Grotesk' }}>Tracked Software Repositories</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Select a repository to inspect deep telemetry and architecture insights</p>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.25rem' }}>
          {repos.map((repo) => (
            <div 
              key={repo.id}
              onClick={() => handleSelectRepo(repo)}
              style={{
                padding: '1.5rem',
                borderRadius: '1.5rem',
                background: selectedRepo?.id === repo.id ? 'var(--bg-secondary)' : 'var(--bg-card-solid)',
                border: selectedRepo?.id === repo.id ? '2px solid var(--accent-primary)' : '1px solid var(--border-color)',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
                boxShadow: selectedRepo?.id === repo.id ? '0 8px 24px var(--accent-primary-glow)' : 'none'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
                <div>
                  <h3 style={{ fontSize: '1.1rem', fontWeight: 700, fontFamily: 'Space Grotesk', color: 'var(--text-primary)' }}>
                    {repo.repositoryName}
                  </h3>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>ID: #{repo.id}</span>
                </div>
                <span className="badge badge-teal">{repo.healthScore ?? '—'} HEALTH</span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.825rem', color: 'var(--text-secondary)', marginBottom: '1.25rem' }}>
                <span>Commits: <strong style={{ color: 'var(--text-primary)', fontFamily: 'JetBrains Mono' }}>{repo.totalCommits}</strong></span>
                <span>Contributors: <strong style={{ color: 'var(--text-primary)', fontFamily: 'JetBrains Mono' }}>{repo.uniqueContributorCount}</strong></span>
              </div>

              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button 
                  onClick={(e) => {
                    e.stopPropagation();
                    handleOpenRepoDetails(repo.id);
                  }}
                  className="btn btn-primary"
                  style={{ flex: 1, fontSize: '0.8rem', padding: '0.5rem 0.75rem', justifyContent: 'center' }}
                >
                  <span>Open Details & Onboarding</span>
                  <ChevronRight size={14} />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 3. Aggregated Activity & Contributors Bento Grid */}
      <div className="bento-grid">
        <div className="card-3xl bento-span-7" style={{ padding: '1.75rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Clock size={18} style={{ color: 'var(--accent-primary)' }} />
              <span>Recent Activity Across Repositories</span>
            </h3>
          </div>
          <VerticalTimeline items={aggregatedCommits} />
          {aggregatedCommits.length === 0 && (
            <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.9rem' }}>
              No commits available. Select repositories and sync to see activity.
            </div>
          )}
        </div>

        <div className="card-3xl bento-span-5" style={{ padding: '1.75rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <TrendingUp size={18} style={{ color: 'var(--accent-teal)' }} />
              <span>Top Active Contributors</span>
            </h3>
          </div>
          
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
            {aggregatedContributors.map((c, idx) => (
              <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.85rem 1rem', background: 'var(--bg-secondary)', borderRadius: '1rem', border: '1px solid var(--border-color)' }}>
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.9rem', color: 'var(--text-primary)' }}>{c.authorName}</div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontFamily: 'JetBrains Mono' }}>{c.authorEmail}</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontWeight: 800, fontSize: '1rem', color: 'var(--accent-teal)', fontFamily: 'JetBrains Mono' }}>{c.commitCount}</div>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>commits</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </motion.div>
  );
}
