import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Users, AlertCircle, ShieldAlert, Award, ArrowUpRight, CheckCircle2, UserCheck } from 'lucide-react';
import { TopLoadingBar, RepositoryLoader } from '../components/RepositoryLoader';
import { apiFetch } from '../utils/apiUtils';

export function TeamIntelligenceView({ selectedRepoId }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!selectedRepoId) {
      setLoading(false);
      setData(null);
      return;
    }
    const controller = new AbortController();
    setData(null);
    setLoading(true);
    setError(null);
    apiFetch(`/api/team/bus-factor/${selectedRepoId}`, { signal: controller.signal })
      .then(res => {
        if (!res.ok) throw new Error(`Failed to load (${res.status})`);
        return res.json();
      })
      .then(resData => setData(resData))
      .catch(e => {
        if (e.name === 'AbortError') return;
        console.error("Failed to load team intelligence:", e);
        setError(e.message);
      })
      .finally(() => setLoading(false));

    return () => controller.abort();
  }, [selectedRepoId]);

  if (!selectedRepoId) {
    return <div style={{ padding: '4rem', textAlign: 'center', color: 'var(--text-muted)' }}>Select a repository to view team intelligence.</div>;
  }

  if (loading) {
    return (
      <>
        <TopLoadingBar loading={true} />
        <RepositoryLoader />
      </>
    );
  }

  if (error) {
    return <div style={{ padding: '4rem', textAlign: 'center', color: '#ef4444' }}>Error loading team intelligence: {error}</div>;
  }

  if (!data || data.totalContributors === 0) {
    return (
      <div style={{ padding: '4rem', textAlign: 'center', color: 'var(--text-muted)' }}>
        <Users size={48} style={{ marginBottom: '1rem', opacity: 0.5 }} />
        <p>No commit data available for this repository. Sync the repository first to generate team intelligence.</p>
      </div>
    );
  }

  return (
    <motion.div
      key={selectedRepoId}
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
      style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '2rem' }}
    >
      {/* Title */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
          <div style={{ padding: '0.6rem', borderRadius: '0.85rem', background: 'rgba(99, 102, 241, 0.15)', color: 'var(--accent-primary)' }}>
            <Users size={24} />
          </div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 800, fontFamily: 'Space Grotesk, sans-serif' }}>
            Team Intelligence & Bus-Factor Estimation
          </h1>
        </div>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
          Analyze contributor ownership mapping, commit distribution, and identify modules at risk of knowledge failure.
        </p>
      </div>

      {data && (
        <>
          {/* Top Summary Cards */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1.25rem' }}>
            <div className="card" style={{ padding: '1.25rem', background: 'var(--bg-card)', borderRadius: '1rem', border: '1px solid var(--border-color)' }}>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Contributors</div>
              <div style={{ fontSize: '1.75rem', fontWeight: 800, marginTop: '0.25rem', color: 'var(--text-primary)' }}>
                {data.totalContributors}
              </div>
            </div>

            <div className="card" style={{ padding: '1.25rem', background: 'var(--bg-card)', borderRadius: '1rem', border: '1px solid var(--border-color)' }}>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Total Commits</div>
              <div style={{ fontSize: '1.75rem', fontWeight: 800, marginTop: '0.25rem', color: 'var(--text-primary)' }}>
                {data.contributorOwnerships?.reduce((sum, c) => sum + (c.commitCount || 0), 0) || 0}
              </div>
            </div>

            <div className="card" style={{ padding: '1.25rem', background: 'var(--bg-card)', borderRadius: '1rem', border: '1px solid var(--border-color)' }}>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Estimated Bus Factor</div>
              <div style={{ fontSize: '1.75rem', fontWeight: 800, marginTop: '0.25rem', color: data.busFactorScore === 1 ? '#ef4444' : '#10b981' }}>
                {data.busFactorScore} {data.busFactorScore === 1 ? "(Single Maintainer Risk)" : "Maintainers"}
              </div>
            </div>

            <div className="card" style={{ padding: '1.25rem', background: 'var(--bg-card)', borderRadius: '1rem', border: '1px solid var(--border-color)' }}>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Knowledge Risk Level</div>
              <div style={{ fontSize: '1.75rem', fontWeight: 800, marginTop: '0.25rem', color: data.overallRiskLevel === 'HIGH' ? '#ef4444' : '#f59e0b' }}>
                {data.overallRiskLevel}
              </div>
            </div>
          </div>

          {/* Single contributor info */}
          {data.totalContributors === 1 && (
            <div style={{ padding: '1rem 1.5rem', borderRadius: '1rem', background: 'rgba(99, 102, 241, 0.08)', border: '1px solid rgba(99, 102, 241, 0.2)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <UserCheck size={20} style={{ color: 'var(--accent-primary)', flexShrink: 0 }} />
              <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
                This repository has a single contributor. Collaboration metrics like bus factor and ownership distribution are limited. Consider inviting additional contributors for improved knowledge sharing.
              </span>
            </div>
          )}

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '2rem' }}>
            {/* Contributor Ownership Table */}
            <div className="card" style={{ padding: '1.75rem', background: 'var(--bg-card)', borderRadius: '1.25rem', border: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              <h2 style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>
                Contributor Ownership Distribution
              </h2>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                {data.contributorOwnerships?.map((c, idx) => (
                  <div key={idx} style={{ padding: '1rem 1.25rem', borderRadius: '1rem', background: 'var(--bg-app)', border: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div style={{ fontWeight: 700, color: 'var(--text-primary)', fontSize: '0.95rem' }}>{c.authorName}</div>
                      <span style={{ fontWeight: 800, color: 'var(--accent-primary)', fontSize: '1rem' }}>{c.ownershipPercentage}%</span>
                    </div>

                    <div style={{ height: '6px', background: 'rgba(255,255,255,0.08)', borderRadius: '3px', overflow: 'hidden' }}>
                      <div style={{ height: '100%', width: `${c.ownershipPercentage}%`, background: 'var(--accent-primary)' }}></div>
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      <span>{c.commitCount} total commits</span>
                      <span>Primary: {c.primaryModules?.join(', ')}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* High Risk Modules & Recommendations */}
            <div className="card" style={{ padding: '1.75rem', background: 'var(--bg-card)', borderRadius: '1.25rem', border: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              <div>
                <h2 style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Grotesk', color: '#ef4444', marginBottom: '0.5rem' }}>
                  High-Risk Single Maintainer Modules
                </h2>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {data.highRiskModules?.map((mod, idx) => (
                    <div key={idx} style={{ padding: '0.75rem 1rem', borderRadius: '0.75rem', background: 'rgba(239, 68, 68, 0.1)', color: '#ef4444', border: '1px solid rgba(239, 68, 68, 0.2)', fontSize: '0.875rem', fontWeight: 600 }}>
                      ⚠️ {mod}
                    </div>
                  ))}
                </div>
              </div>

              <div>
                <h2 style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Grotesk', marginBottom: '0.5rem' }}>
                  Team Resilience Recommendations
                </h2>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
                  {data.teamRecommendations?.map((rec, idx) => (
                    <div key={idx} style={{ padding: '0.75rem 1rem', borderRadius: '0.75rem', background: 'var(--bg-app)', border: '1px solid var(--border-color)', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                      💡 {rec}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </motion.div>
  );
}
