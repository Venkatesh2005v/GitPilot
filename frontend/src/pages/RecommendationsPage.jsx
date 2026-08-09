import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { ShieldCheck, Zap, AlertTriangle, ArrowRight, CheckCircle2 } from 'lucide-react';
import { TopLoadingBar, RepositoryLoader } from '../components/RepositoryLoader';

export function RecommendationsPage({ selectedRepoId }) {
  const [recs, setRecs] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchRecommendations = async (signal) => {
    if (!selectedRepoId) {
      setLoading(false);
      return;
    }
    setRecs([]);
    setLoading(true);
    try {
      const res = await fetch(`/api/memory/${selectedRepoId}/recommendations`, { signal });
      if (res.ok) {
        const data = await res.json();
        setRecs(data);
      }
    } catch (e) {
      if (e.name === 'AbortError') return;
      console.error("Failed to load Recommendations:", e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const controller = new AbortController();
    fetchRecommendations(controller.signal);
    return () => controller.abort();
  }, [selectedRepoId]);

  if (loading) {
    return (
      <>
        <TopLoadingBar loading={true} />
        <RepositoryLoader />
      </>
    );
  }

  return (
    <motion.div key={selectedRepoId} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}>
      {/* Title */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
            <span className="badge badge-primary">CONTEXTUAL INTELLIGENCE</span>
            <span className="badge badge-teal">ACTIONABLE ADVISORIES</span>
          </div>
          <h1 style={{ fontSize: '2.25rem', fontWeight: 800, fontFamily: 'Space Grotesk, sans-serif' }}>
            Contextual Engineering Recommendations
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.925rem' }}>
            Module-specific recommendations with detailed priority, reason, effort, and expected impact.
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        {recs.map((item) => (
          <motion.div key={item.id} whileHover={{ y: -4 }} className="card-3xl" style={{ padding: '1.75rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem', flexWrap: 'wrap', gap: '0.5rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <span className={`badge ${item.priority === 'HIGH' ? 'badge-warning' : 'badge-primary'}`}>
                  {item.priority} PRIORITY
                </span>
                <h3 style={{ fontSize: '1.25rem', fontWeight: 800 }}>{item.title}</h3>
              </div>
              <span className="badge badge-teal">{item.targetModule}</span>
            </div>

            {/* Reason Box */}
            <div style={{
              background: 'var(--bg-secondary)',
              padding: '1.25rem',
              borderRadius: '1.25rem',
              border: '1px solid var(--border-color)',
              fontSize: '0.925rem',
              color: 'var(--text-primary)',
              lineHeight: 1.6,
              marginBottom: '1.25rem'
            }}>
              <strong style={{ color: 'var(--accent-primary)', display: 'block', marginBottom: '0.35rem' }}>Reason & Context:</strong>
              {item.reason}
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', borderTop: '1px solid var(--border-color)', paddingTop: '1rem', fontSize: '0.85rem' }}>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Estimated Effort: </span>
                <strong style={{ color: 'var(--text-primary)', fontFamily: 'JetBrains Mono' }}>{item.estimatedEffort}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Expected Impact: </span>
                <strong style={{ color: 'var(--accent-green)' }}>{item.expectedImpact}</strong>
              </div>
            </div>
          </motion.div>
        ))}
      </div>
    </motion.div>
  );
}
