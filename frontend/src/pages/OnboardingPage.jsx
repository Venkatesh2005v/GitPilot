import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { BookOpen, RefreshCw, CheckCircle2, Code2, Layers, Cpu, Sparkles, FileText, ArrowRight } from 'lucide-react';
import { RepositoryLoader, TopLoadingBar, AnimatedCard } from '../components/RepositoryLoader';
import { getSummaryText, apiFetch } from '../utils/apiUtils';

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.1, delayChildren: 0.05 }
  }
};

const cardVariants = {
  hidden: { opacity: 0, y: 24 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: [0.16, 1, 0.3, 1] } }
};

export function OnboardingPage({ selectedRepoId }) {
  const params = useParams();
  const repoId = selectedRepoId || params?.id;
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [regenerating, setRegenerating] = useState(false);

  const fetchReport = async (signal) => {
    if (!repoId) {
      setLoading(false);
      setError("No repository selected.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const res = await apiFetch(`/ai/repositories/${repoId}/intelligence`, { signal });
      if (res.ok) {
        const data = await res.json();
        setReport(data);
      } else {
        setError("Failed to fetch onboarding guide from server.");
      }
    } catch (e) {
      if (e.name === 'AbortError') return;
      console.error("Failed to load AI Onboarding Report:", e);
      setError("Network or server connection error.");
    } finally {
      setLoading(false);
    }
  };

  const handleRegenerate = async () => {
    setRegenerating(true);
    setReport(null);
    try {
      const res = await apiFetch(`/ai/repositories/${repoId}/intelligence/refresh`, { method: 'POST' });
      if (res.ok) {
        const data = await res.json();
        setReport(data);
      }
    } catch (e) {
      console.error("Regeneration failed:", e);
    } finally {
      setRegenerating(false);
    }
  };

  useEffect(() => {
    const controller = new AbortController();
    setReport(null);
    fetchReport(controller.signal);
    return () => controller.abort();
  }, [repoId]);

  if (!repoId) {
    return (
      <div className="card-3xl" style={{ padding: '3rem', textAlign: 'center' }}>
        <BookOpen size={48} style={{ color: 'var(--text-muted)', marginBottom: '1rem' }} />
        <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem' }}>Select a Repository</h3>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>Choose a repository from the top selector to generate an onboarding guide.</p>
      </div>
    );
  }

  return (
    <>
      <TopLoadingBar loading={loading || regenerating} />

      <AnimatePresence mode="wait">
        {(loading || regenerating) && !report ? (
          <motion.div key="loader" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <RepositoryLoader message={regenerating ? "Regenerating with fresh AI analysis..." : undefined} />
          </motion.div>
        ) : error && !report ? (
          <motion.div
            key="error"
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
          >
            <div className="card-3xl" style={{ padding: '3rem', textAlign: 'center' }}>
              <BookOpen size={48} style={{ color: '#ef4444', marginBottom: '1rem' }} />
              <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem' }}>Unable to load Onboarding Guide</h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1.5rem' }}>{error}</p>
              <button onClick={() => { const c = new AbortController(); fetchReport(c.signal); }} className="btn btn-primary">
                <RefreshCw size={16} />
                <span>Retry Loading</span>
              </button>
            </div>
          </motion.div>
        ) : report ? (
          <motion.div
            key="content"
            variants={containerVariants}
            initial="hidden"
            animate="visible"
          >
            {/* Title Header */}
            <motion.div variants={cardVariants} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
                  <span className="badge badge-primary">AI POWERED ONBOARDING</span>
                  <span className="badge badge-teal">{report.isCached ? 'CACHED' : 'FRESHLY GENERATED'}</span>
                </div>
                <h1 style={{ fontSize: '2.25rem', fontWeight: 800, fontFamily: 'Space Grotesk, sans-serif' }}>
                  New Contributor Onboarding Guide
                </h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.925rem' }}>
                  Everything a new developer needs to understand this codebase within minutes.
                </p>
              </div>

              <button
                onClick={handleRegenerate}
                disabled={regenerating}
                className="btn btn-primary"
                style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', padding: '0.75rem 1.4rem', borderRadius: '1.25rem' }}
              >
                <RefreshCw size={18} className={regenerating ? "spin" : ""} />
                <span>{regenerating ? "Regenerating..." : "Regenerate"}</span>
              </button>
            </motion.div>

            {/* Welcome Banner */}
            <motion.div variants={cardVariants} className="card-3xl" style={{ padding: '2rem', marginBottom: '2rem', background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.15), rgba(20, 184, 166, 0.15))', border: '1px solid var(--border-color-hover)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.75rem' }}>
                <Sparkles size={24} style={{ color: 'var(--accent-primary)' }} />
                <h2 style={{ fontSize: '1.5rem', fontWeight: 800, fontFamily: 'Space Grotesk' }}>
                  Welcome to {report.repositoryName}
                </h2>
              </div>
              <p style={{ fontSize: '1.05rem', color: 'var(--text-primary)', lineHeight: 1.7, marginBottom: '1rem' }}>
                {report.projectPurpose || getSummaryText(report) || ''}
              </p>
              {(report.highLevelDescription || report.readmeSummary?.architectureSummary || report.commitSummary?.highLevelSummary) && (
                <p style={{ fontSize: '0.95rem', color: 'var(--text-secondary)', lineHeight: 1.6, margin: 0 }}>
                  {report.highLevelDescription || report.readmeSummary?.architectureSummary || report.commitSummary?.highLevelSummary}
                </p>
              )}
            </motion.div>

            {/* Grid: Tech Stack & Learning Path */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))', gap: '1.75rem', marginBottom: '2rem' }}>
              {/* Tech Stack */}
              <motion.div variants={cardVariants} className="card-3xl" style={{ padding: '1.75rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1.25rem' }}>
                  <Cpu size={20} style={{ color: 'var(--accent-teal)' }} />
                  <h3 style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>Technology Stack</h3>
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.6rem' }}>
                  {(() => {
                    const technologies = report?.technologyStack ?? report?.techStack?.detectedTechnologies ?? [];
                    return technologies.length > 0 ? technologies.map((tech, idx) => (
                    <motion.span
                      key={idx}
                      initial={{ opacity: 0, scale: 0.8 }}
                      animate={{ opacity: 1, scale: 1 }}
                      transition={{ delay: idx * 0.05, duration: 0.3 }}
                      style={{ padding: '0.45rem 0.85rem', borderRadius: '0.85rem', background: 'var(--bg-secondary)', color: 'var(--text-primary)', fontSize: '0.875rem', border: '1px solid var(--border-color)', fontWeight: 600 }}
                    >
                      {tech}
                    </motion.span>
                  )) : (
                    <span style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>No technologies detected yet.</span>
                  );
                  })()}
                </div>
              </motion.div>

              {/* Learning Path */}
              <motion.div variants={cardVariants} className="card-3xl" style={{ padding: '1.75rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1.25rem' }}>
                  <BookOpen size={20} style={{ color: 'var(--accent-primary)' }} />
                  <h3 style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>Recommended Learning Path</h3>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                  {(() => {
                    const steps = report.recommendedLearningPath?.length > 0
                      ? report.recommendedLearningPath
                      : (report.suggestions || []).map(s => `[${s.priority}] ${s.title} — ${s.description}`);
                    return steps.length > 0 ? steps.map((step, idx) => (
                    <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', padding: '0.6rem 0.85rem', borderRadius: '0.85rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
                      <ArrowRight size={16} style={{ color: 'var(--accent-primary)', flexShrink: 0 }} />
                      <span style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-primary)' }}>{typeof step === 'string' ? step : step.title}</span>
                    </div>
                  )) : (
                    <span style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Learning path will appear after AI analysis.</span>
                  );
                  })()}
                </div>
              </motion.div>
            </div>

            {/* Modules */}
            {report.mainModules?.length > 0 && (
              <motion.div variants={cardVariants} className="card-3xl" style={{ padding: '1.75rem', marginBottom: '2rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1.5rem' }}>
                  <Layers size={20} style={{ color: '#ec4899' }} />
                  <h3 style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>Main Modules & Responsibilities</h3>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.25rem' }}>
                  {report.mainModules.map((mod, idx) => (
                    <motion.div
                      key={idx}
                      initial={{ opacity: 0, y: 16 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: 0.3 + idx * 0.08, duration: 0.4 }}
                      style={{ padding: '1.25rem', borderRadius: '1.25rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', gap: '0.65rem' }}
                    >
                      <div style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--text-primary)' }}>{mod.moduleName}</div>
                      <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', margin: 0, lineHeight: 1.5 }}>
                        {mod.responsibility}
                      </p>
                      {mod.keyFiles?.length > 0 && (
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem', marginTop: '0.25rem' }}>
                          {mod.keyFiles.map((f, fIdx) => (
                            <span key={fIdx} style={{ fontSize: '0.75rem', fontFamily: 'JetBrains Mono', color: 'var(--accent-teal)', background: 'var(--bg-app)', padding: '0.25rem 0.6rem', borderRadius: '0.5rem', border: '1px solid var(--border-color)' }}>
                              {f}
                            </span>
                          ))}
                        </div>
                      )}
                    </motion.div>
                  ))}
                </div>
              </motion.div>
            )}

            {/* Config Files & Key APIs */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))', gap: '1.75rem', marginBottom: '2rem' }}>
              {report.importantConfigFiles?.length > 0 && (
                <motion.div variants={cardVariants} className="card-3xl" style={{ padding: '1.75rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1.25rem' }}>
                    <FileText size={20} style={{ color: 'var(--accent-teal)' }} />
                    <h3 style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>Important Configuration Files</h3>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {report.importantConfigFiles.map((cfg, idx) => (
                      <div key={idx} style={{ fontSize: '0.85rem', fontFamily: 'JetBrains Mono', color: 'var(--text-primary)', padding: '0.5rem 0.85rem', borderRadius: '0.75rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
                        {cfg}
                      </div>
                    ))}
                  </div>
                </motion.div>
              )}

              {report.keyRestApis?.length > 0 && (
                <motion.div variants={cardVariants} className="card-3xl" style={{ padding: '1.75rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1.25rem' }}>
                    <Code2 size={20} style={{ color: 'var(--accent-primary)' }} />
                    <h3 style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>Key Entry Points</h3>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {report.keyRestApis.map((api, idx) => (
                      <div key={idx} style={{ fontSize: '0.85rem', fontFamily: 'JetBrains Mono', color: 'var(--accent-teal)', padding: '0.5rem 0.85rem', borderRadius: '0.75rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
                        {api}
                      </div>
                    ))}
                  </div>
                </motion.div>
              )}
            </div>

            {/* README Overview */}
            {report.readmeSummary?.projectOverview && (
              <motion.div variants={cardVariants} className="card-3xl" style={{ padding: '1.75rem', marginBottom: '2rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1.25rem' }}>
                  <BookOpen size={20} style={{ color: 'var(--accent-primary)' }} />
                  <h3 style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>README Overview</h3>
                </div>
                <p style={{ fontSize: '0.95rem', color: 'var(--text-primary)', lineHeight: 1.7, marginBottom: '0.75rem' }}>
                  {report.readmeSummary.projectOverview}
                </p>
                {report.readmeSummary.features?.length > 0 && (
                  <ul style={{ margin: '0.5rem 0 0 1.25rem', color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: 1.8 }}>
                    {report.readmeSummary.features.slice(0, 5).map((f, i) => <li key={i}>{f}</li>)}
                  </ul>
                )}
              </motion.div>
            )}

            {/* Suggested Contributions */}
            {report.suggestedFirstContributions?.length > 0 && (
              <motion.div variants={cardVariants} className="card-3xl" style={{ padding: '1.75rem', background: 'rgba(16, 185, 129, 0.08)', border: '1px solid rgba(16, 185, 129, 0.25)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1.25rem' }}>
                  <CheckCircle2 size={22} style={{ color: '#10b981' }} />
                  <h3 style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Grotesk', color: '#10b981' }}>Suggested First Contributions</h3>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1rem' }}>
                  {report.suggestedFirstContributions.map((idea, idx) => (
                    <motion.div
                      key={idx}
                      initial={{ opacity: 0, x: -10 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: 0.5 + idx * 0.1, duration: 0.35 }}
                      style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', padding: '0.75rem 1rem', borderRadius: '0.85rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}
                    >
                      <CheckCircle2 size={16} style={{ color: '#10b981', flexShrink: 0 }} />
                      <span style={{ fontSize: '0.9rem', color: 'var(--text-primary)', fontWeight: 500 }}>{idea}</span>
                    </motion.div>
                  ))}
                </div>
              </motion.div>
            )}
          </motion.div>
        ) : null}
      </AnimatePresence>
    </>
  );
}
