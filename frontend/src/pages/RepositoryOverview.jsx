import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
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
  Info,
  CheckCircle2,
  Settings as SettingsIcon,
  GitBranch,
  Globe,
  Sparkles,
  RefreshCw,
  BookOpen,
  ShieldCheck,
  Code2,
  Zap,
  Layers,
  ArrowRight
} from 'lucide-react';
import { CircularProgress } from '../components/CircularProgress';
import { TechPill } from '../components/TechPill';
import { RecommendationCard } from '../components/RecommendationCard';
import { VerticalTimeline } from '../components/VerticalTimeline';
import { Skeleton } from '../components/Skeleton';
import { TopLoadingBar, RepositoryLoader } from '../components/RepositoryLoader';
import { OnboardingPage } from './OnboardingPage';
import { ArchitectureExplorer } from './ArchitectureExplorer';
import { TeamIntelligenceView } from './TeamIntelligenceView';
import { safeFetchJson, getNumericHealthScore, getSummaryText, getTechStackArray } from '../utils/apiUtils';

export function RepositoryOverview({ setSelectedRepoId }) {
  const { id } = useParams();
  
  const [activeTab, setActiveTab] = useState('overview'); // 'overview' | 'onboarding' | 'architecture' | 'team'
  const [showWelcomeBanner, setShowWelcomeBanner] = useState(false);
  
  const [activity, setActivity] = useState(null);
  const [commits, setCommits] = useState([]);
  const [contributors, setContributors] = useState([]);
  const [intelligence, setIntelligence] = useState(null);
  
  const [loading, setLoading] = useState(true);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState(null);

  useEffect(() => {
    let isSubscribed = true;
    const controller = new AbortController();
    const { signal } = controller;

    if (setSelectedRepoId && id) {
      setSelectedRepoId(id.toString());
    }

    // Reset state on repo switch for clean transition
    setActivity(null);
    setCommits([]);
    setContributors([]);
    setIntelligence(null);

    // Pre-fetch onboarding guide in background & check welcome banner state
    const viewedKey = `gitpilot_viewed_onboarding_${id}`;
    if (!localStorage.getItem(viewedKey)) {
      setShowWelcomeBanner(true);
    } else {
      setShowWelcomeBanner(false);
    }
    safeFetchJson(`/api/ai/onboarding/${id}`, { signal });

    const fetchRepoCoreData = async () => {
      setLoading(true);
      try {
        const [actData, contribData, commitData, intelData] = await Promise.all([
          safeFetchJson(`/repositories/${id}/activity`, { signal }),
          safeFetchJson(`/repositories/${id}/contributors`, { signal }),
          safeFetchJson(`/repositories/${id}/commits?page=0&size=6`, { signal }),
          safeFetchJson(`/ai/repositories/${id}/intelligence`, { signal })
        ]);

        if (!isSubscribed) return;

        if (isSubscribed) {
          if (actData) setActivity(actData);
          if (Array.isArray(contribData)) setContributors(contribData);
          if (Array.isArray(commitData)) setCommits(commitData);
          if (intelData) setIntelligence(intelData);
        }
      } catch (e) {
        if (e.name !== 'AbortError' && isSubscribed) {
          console.error("Error fetching repository overview details:", e);
        }
      } finally {
        if (isSubscribed) {
          setLoading(false);
        }
      }
    };

    fetchRepoCoreData();

    return () => {
      isSubscribed = false;
      controller.abort();
    };
  }, [id, setSelectedRepoId]);

  const handleRunAiAnalysis = async () => {
    setAiLoading(true);
    setAiError(null);
    try {
      const intelData = await safeFetchJson(`/ai/repositories/${id}/intelligence/refresh`, { method: 'POST' });
      if (intelData) {
        setIntelligence(intelData);
      } else {
        setAiError("Analysis request failed");
      }
    } catch (e) {
      setAiError("Failed to trigger AI deep analysis");
    } finally {
      setAiLoading(false);
    }
  };

  const repoName = activity?.repositoryName || intelligence?.repositoryName || 'Repository';
  const healthScore = getNumericHealthScore(intelligence, activity?.healthScore);
  const totalCommits = activity?.totalCommits || (Array.isArray(commits) ? commits.length : 0);
  const contributorCount = Array.isArray(contributors) ? contributors.length : (activity?.uniqueContributorCount || 0);
  const techStack = getTechStackArray(intelligence);
  const summaryText = getSummaryText(intelligence);

  if (loading) {
    return (
      <>
        <TopLoadingBar loading={true} />
        <RepositoryLoader />
      </>
    );
  }

  return (
    <>
      <TopLoadingBar loading={aiLoading} />
      <motion.div
        key={id}
        initial={{ opacity: 0, y: 15 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
        className="ambient-page-bg"
        style={{ padding: '0 0 3rem 0' }}
      >
      
      {/* 1. First-time Welcome Banner */}
      {showWelcomeBanner && (
        <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} className="card-3xl" style={{ 
          marginBottom: '1.5rem', 
          padding: '1.5rem 2rem', 
          background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(20, 184, 166, 0.2))', 
          border: '1px solid var(--accent-primary)',
          display: 'flex',
          justify: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '1rem'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div style={{ fontSize: '2rem' }}>👋</div>
            <div>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-primary)', fontFamily: 'Space Grotesk' }}>
                Welcome to this repository!
              </h3>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', margin: '0.2rem 0 0 0' }}>
                We've prepared a personalized AI-powered onboarding guide to help you understand the project quickly.
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <button 
              onClick={() => {
                setActiveTab('onboarding');
                setShowWelcomeBanner(false);
                localStorage.setItem(`gitpilot_viewed_onboarding_${id}`, 'true');
              }}
              className="btn btn-primary"
              style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.6rem 1.25rem' }}
            >
              <BookOpen size={16} />
              <span>View Guide</span>
            </button>

            <button 
              onClick={() => {
                setShowWelcomeBanner(false);
                localStorage.setItem(`gitpilot_viewed_onboarding_${id}`, 'true');
              }}
              className="btn btn-secondary"
              style={{ padding: '0.6rem 1rem' }}
            >
              Dismiss
            </button>
          </div>
        </motion.div>
      )}

      {/* 2. Hero Header Banner */}
      <div className="card-3xl" style={{ 
        marginBottom: '2rem',
        padding: '2.5rem',
        background: 'radial-gradient(circle at 70% 30%, var(--accent-primary-glow), transparent 70%), var(--bg-card-solid)',
        border: '1px solid var(--border-color-hover)',
        boxShadow: 'var(--shadow-lg), var(--shadow-glow)',
        position: 'relative',
        overflow: 'hidden'
      }}>
        {/* Soft Ambient Background Orbs */}
        <div className="bg-glow-teal" style={{ top: '-100px', right: '-50px', opacity: 0.4 }}></div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1.5rem', position: 'relative', zIndex: 1 }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.85rem' }}>
              <Link to="/dashboard" style={{ color: 'var(--text-secondary)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.85rem', fontWeight: 500 }}>
                <ChevronLeft size={16} />
                <span>Dashboard</span>
              </Link>
              <span style={{ color: 'var(--border-color)' }}>/</span>
              <span className="badge badge-primary">PRIMARY REPOSITORY</span>
              <span className="badge badge-teal" style={{ gap: '0.35rem' }}>
                <GitBranch size={12} />
                <span>{activity?.repositoryName?.includes('/') ? '' : ''}{intelligence?.commitSummary?.activityTrend || 'active'}</span>
              </span>
            </div>

            <h1 style={{ fontSize: 'clamp(2rem, 4vw, 3rem)', fontWeight: 800, fontFamily: 'Space Grotesk, sans-serif', letterSpacing: '-0.03em', marginBottom: '0.65rem' }}>
              {repoName}
            </h1>

            <p style={{ color: 'var(--text-secondary)', fontSize: '1rem', maxWidth: '650px', lineHeight: 1.6 }}>
              {summaryText || 'Loading repository intelligence...'}
            </p>
          </div>

          {/* Quick Actions & Sync Controls */}
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '1rem' }}>
            <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
              <button 
                onClick={() => {
                  setActiveTab('onboarding');
                  localStorage.setItem(`gitpilot_viewed_onboarding_${id}`, 'true');
                }} 
                className="btn btn-primary"
                style={{ background: 'linear-gradient(135deg, var(--accent-primary), var(--accent-teal))', color: '#fff', display: 'flex', alignItems: 'center', gap: '0.5rem' }}
              >
                <BookOpen size={16} />
                <span>Open Onboarding Guide</span>
              </button>

              <button 
                onClick={handleRunAiAnalysis} 
                className="btn btn-secondary"
                disabled={aiLoading}
              >
                <Sparkles size={16} className={aiLoading ? 'spin' : ''} />
                <span>{aiLoading ? 'Analyzing AI...' : 'Run Deep AI Scan'}</span>
              </button>
            </div>

            <div style={{ display: 'flex', gap: '1rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              <span>Status: <strong style={{ color: 'var(--accent-green)' }}>SYNCHRONIZED</strong></span>
              <span>•</span>
              <span>Commits: <strong style={{ color: 'var(--text-primary)', fontFamily: 'JetBrains Mono' }}>{totalCommits}</strong></span>
              <span>•</span>
              <span>Contributors: <strong style={{ color: 'var(--text-primary)', fontFamily: 'JetBrains Mono' }}>{contributorCount}</strong></span>
            </div>
          </div>
        </div>
      </div>

      {/* 3. Dedicated Tab Navigation Bar */}
      <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '2rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem', flexWrap: 'wrap' }}>
        <button 
          onClick={() => setActiveTab('overview')}
          className={`btn ${activeTab === 'overview' ? 'btn-primary' : 'btn-secondary'}`}
          style={{ borderRadius: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}
        >
          <Sparkles size={16} />
          <span>Overview & Bento Grid</span>
        </button>

        <button 
          onClick={() => {
            setActiveTab('onboarding');
            localStorage.setItem(`gitpilot_viewed_onboarding_${id}`, 'true');
          }}
          className={`btn ${activeTab === 'onboarding' ? 'btn-primary' : 'btn-secondary'}`}
          style={{ borderRadius: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}
        >
          <BookOpen size={16} />
          <span>Onboarding Guide</span>
        </button>

        <button 
          onClick={() => setActiveTab('architecture')}
          className={`btn ${activeTab === 'architecture' ? 'btn-primary' : 'btn-secondary'}`}
          style={{ borderRadius: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}
        >
          <Code2 size={16} />
          <span>Architecture & Call Graph</span>
        </button>

        <button 
          onClick={() => setActiveTab('team')}
          className={`btn ${activeTab === 'team' ? 'btn-primary' : 'btn-secondary'}`}
          style={{ borderRadius: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}
        >
          <Users size={16} />
          <span>Team Intelligence</span>
        </button>
      </div>

      {/* 4. Tab Content Views */}
      {activeTab === 'onboarding' && (
        <OnboardingPage selectedRepoId={id} />
      )}

      {activeTab === 'architecture' && (
        <ArchitectureExplorer selectedRepoId={id} />
      )}

      {activeTab === 'team' && (
        <TeamIntelligenceView selectedRepoId={id} />
      )}

      {activeTab === 'overview' && (
        <div className="bento-grid">

          {/* Bento Panel 1: Repository Health */}
          <motion.div whileHover={{ y: -4 }} className="card-3xl bento-span-4" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
                <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Sparkles size={18} style={{ color: 'var(--accent-primary)' }} />
                  <span>Repository Health</span>
                </h3>
                <span className={`badge ${healthScore >= 80 ? 'badge-success' : healthScore >= 65 ? 'badge-primary' : 'badge-teal'}`}>
                  {intelligence?.grade || intelligence?.healthScore?.grade || (healthScore >= 95 ? 'EXCELLENT' : healthScore >= 80 ? 'GOOD' : healthScore >= 65 ? 'FAIR' : 'NEEDS ATTENTION')}
                </span>
              </div>

              <div style={{ padding: '1rem 0', display: 'flex', justifyContent: 'center' }}>
                <CircularProgress score={healthScore} size={140} strokeWidth={10} title="Quality Score" />
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem', marginTop: '1rem' }}>
                {[
                  { label: 'Documentation', score: intelligence?.healthScore?.documentationScore, color: 'var(--accent-green)' },
                  { label: 'Activity', score: intelligence?.healthScore?.activityScore, color: 'var(--accent-primary)' },
                  { label: 'Collaboration', score: intelligence?.healthScore?.collaborationScore, color: 'var(--accent-teal)' },
                  { label: 'Structure', score: intelligence?.healthScore?.structureScore, color: '#ec4899' },
                ].map((m, idx) => (
                  <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.65rem 0.9rem', background: 'var(--bg-secondary)', borderRadius: '1rem', border: '1px solid var(--border-color)' }}>
                    <span style={{ fontSize: '0.825rem', color: 'var(--text-secondary)', fontWeight: 500 }}>{m.label}</span>
                    <strong style={{ color: m.color, fontFamily: 'JetBrains Mono', fontSize: '0.85rem' }}>{m.score != null ? m.score + '/100' : '—'}</strong>
                  </div>
                ))}
              </div>
            </div>
          </motion.div>

          {/* Bento Panel 2: AI Summary */}
          <motion.div whileHover={{ y: -4 }} className="card-3xl bento-span-8" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <div style={{ width: '36px', height: '36px', borderRadius: '1rem', background: 'var(--accent-primary-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <Cpu size={20} style={{ color: 'var(--accent-primary)' }} />
                  </div>
                  <div>
                    <h3 style={{ fontSize: '1.2rem', fontWeight: 800 }}>AI Repository Insight</h3>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      {intelligence?.summary?.architectureStyle || 'Continuous analysis'}
                    </span>
                  </div>
                </div>
                <span className="badge badge-teal">{intelligence?.analysisConfidence || 0}% CONFIDENCE</span>
              </div>

              <div style={{ background: 'var(--bg-secondary)', padding: '1.5rem', borderRadius: '1.5rem', border: '1px solid var(--border-color)', fontSize: '0.95rem', color: 'var(--text-primary)', lineHeight: 1.7, marginBottom: '1.5rem' }}>
                {summaryText ? `"${summaryText}"` : 'No AI analysis available yet. Click "Run Deep AI Scan" to generate.'}
              </div>

              {/* AI Explanation Panel */}
              {(intelligence?.aiExplanation || intelligence?.healthScore?.aiExplanation) && (
                <details style={{ background: 'var(--bg-secondary)', borderRadius: '1rem', border: '1px solid var(--border-color)', padding: '0.85rem 1.1rem', marginBottom: '1rem' }}>
                  <summary style={{ cursor: 'pointer', fontSize: '0.85rem', fontWeight: 600, color: 'var(--accent-primary)' }}>
                    Why did GitPilot assign this score?
                  </summary>
                  <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.6, marginTop: '0.75rem' }}>
                    {intelligence?.aiExplanation || intelligence?.healthScore?.aiExplanation}
                  </p>
                </details>
              )}

              {aiError && (
                <div style={{ color: '#ef4444', fontSize: '0.85rem', marginBottom: '1rem' }}>⚠️ {aiError}</div>
              )}
            </div>

            <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                {intelligence?.generatedTime ? `Generated: ${new Date(intelligence.generatedTime).toLocaleString()}` : ''}
              </span>
              <button onClick={handleRunAiAnalysis} className="btn btn-primary" style={{ fontSize: '0.8rem', padding: '0.5rem 1rem' }}>
                <span>Re-run Analysis</span>
              </button>
            </div>
          </motion.div>

          {/* Bento Panel 3: Technology Stack */}
          <motion.div whileHover={{ y: -4 }} className="card-3xl bento-span-5">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Code2 size={18} style={{ color: 'var(--accent-teal)' }} />
                <span>Technology Stack</span>
              </h3>
              <span className="badge badge-teal" style={{ fontSize: '0.7rem' }}>{techStack.length} DETECTED</span>
            </div>

            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.65rem', marginBottom: '1.5rem' }}>
              {techStack.length > 0 ? techStack.map((tech, i) => (
                <TechPill key={i} name={tech} category="Stack" />
              )) : (
                <span style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>No technologies detected. Run analysis to detect.</span>
              )}
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div style={{ padding: '0.6rem 0.85rem', background: 'var(--bg-secondary)', borderRadius: '0.85rem', border: '1px solid var(--border-color)' }}>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Primary Language</span>
                <strong style={{ fontSize: '0.85rem', color: 'var(--text-primary)' }}>{intelligence?.techStack?.primaryLanguage && intelligence.techStack.primaryLanguage !== 'Unknown' ? intelligence.techStack.primaryLanguage : '—'}</strong>
              </div>
              <div style={{ padding: '0.6rem 0.85rem', background: 'var(--bg-secondary)', borderRadius: '0.85rem', border: '1px solid var(--border-color)' }}>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Category</span>
                <strong style={{ fontSize: '0.85rem', color: 'var(--text-primary)' }}>{intelligence?.techStack?.category && intelligence.techStack.category !== 'Unknown' ? intelligence.techStack.category : '—'}</strong>
              </div>
            </div>
          </motion.div>

          {/* Bento Panel 4: README Summary */}
          <motion.div whileHover={{ y: -4 }} className="card-3xl bento-span-7">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <BookOpen size={18} style={{ color: 'var(--accent-primary)' }} />
                <span>README Summary</span>
              </h3>
              <span className={`badge ${intelligence?.readmeSummary?.readmePresent ? 'badge-success' : 'badge-teal'}`} style={{ fontSize: '0.7rem' }}>
                {intelligence?.readmeSummary?.readmePresent ? 'PARSED' : 'NOT FOUND'}
              </span>
            </div>

            <div style={{ background: 'var(--bg-secondary)', padding: '1.35rem', borderRadius: '1.35rem', border: '1px solid var(--border-color)', fontSize: '0.9rem', lineHeight: 1.6 }}>
              <p style={{ color: 'var(--text-primary)', marginBottom: '0.75rem', fontWeight: 600 }}>
                {intelligence?.readmeSummary?.projectOverview || 'README analysis will appear after AI scan.'}
              </p>
              {intelligence?.readmeSummary?.features && intelligence.readmeSummary.features.length > 0 && (
                <ul style={{ margin: '0.5rem 0 0 1rem', color: 'var(--text-secondary)' }}>
                  {intelligence.readmeSummary.features.slice(0, 4).map((f, i) => (
                    <li key={i} style={{ marginBottom: '0.3rem', fontSize: '0.85rem' }}>{f}</li>
                  ))}
                </ul>
              )}
            </div>
          </motion.div>

          {/* Bento Panel 5: Recent Changes Timeline */}
          <motion.div whileHover={{ y: -4 }} className="card-3xl bento-span-6">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Clock size={18} style={{ color: 'var(--accent-primary)' }} />
                <span>Recent Changes</span>
              </h3>
              <span className="badge badge-outline" style={{ fontSize: '0.7rem' }}>{commits.length} COMMITS</span>
            </div>

            {commits.length > 0 ? (
              <VerticalTimeline items={commits} />
            ) : (
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', textAlign: 'center', padding: '2rem 0' }}>
                No commits available. Sync the repository to load commit history.
              </p>
            )}
          </motion.div>

          {/* Bento Panel 6: AI Recommendations */}
          <motion.div whileHover={{ y: -4 }} className="card-3xl bento-span-6" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
                <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <ShieldCheck size={18} style={{ color: 'var(--accent-teal)' }} />
                  <span>AI Recommendations</span>
                </h3>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                {intelligence?.suggestions && intelligence.suggestions.length > 0 ? (
                  intelligence.suggestions.slice(0, 3).map((s, idx) => (
                    <RecommendationCard
                      key={idx}
                      title={s.title}
                      category={s.category}
                      priority={s.priority}
                      description={s.description}
                      actionText="View Details"
                    />
                  ))
                ) : (
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', textAlign: 'center', padding: '1.5rem 0' }}>
                    Run AI analysis to generate recommendations.
                  </p>
                )}
              </div>
            </div>
          </motion.div>

        </div>
      )}
    </motion.div>
    </>
  );
}
