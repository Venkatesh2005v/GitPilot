import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Clock, Calendar, GitCommit, Sparkles, Layers } from 'lucide-react';
import { Skeleton } from '../components/Skeleton';
import { apiFetch } from '../utils/apiUtils';

export function TimelinePage({ selectedRepoId }) {
  const [timelines, setTimelines] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchTimelines = async () => {
    setLoading(true);
    try {
      const repoId = selectedRepoId || 1;
      const res = await apiFetch(`/api/memory/${repoId}/timeline`);
      if (res.ok) {
        const data = await res.json();
        setTimelines(data);
      }
    } catch (e) {
      console.error("Failed to fetch Engineering Timeline:", e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTimelines();
  }, [selectedRepoId]);

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        <Skeleton height="100px" borderRadius="1.75rem" />
        <Skeleton height="350px" borderRadius="1.75rem" />
      </div>
    );
  }

  return (
    <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.35 }}>
      {/* Title */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
            <span className="badge badge-primary">MILESTONE GROUPINGS</span>
            <span className="badge badge-teal">MONTHLY EVOLUTION</span>
          </div>
          <h1 style={{ fontSize: '2.25rem', fontWeight: 800, fontFamily: 'Space Grotesk, sans-serif' }}>
            Engineering Milestones & Timeline
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.925rem' }}>
            Commits grouped into high-level engineering achievements rather than raw line changes.
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        {timelines.map((item) => (
          <motion.div key={item.id} whileHover={{ y: -4 }} className="card-3xl" style={{ padding: '1.75rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <span className="badge badge-primary" style={{ padding: '0.4rem 0.85rem', fontSize: '0.85rem' }}>
                  <Calendar size={14} />
                  <span>{item.timePeriod}</span>
                </span>
                <h3 style={{ fontSize: '1.35rem', fontWeight: 800 }}>{item.milestoneTitle}</h3>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <span className="badge badge-teal">
                  <GitCommit size={13} />
                  <span>{item.commitCount} COMMITS</span>
                </span>
                <span className="badge badge-success">{item.impactLevel} IMPACT</span>
              </div>
            </div>

            <div style={{
              background: 'var(--bg-secondary)',
              padding: '1.25rem',
              borderRadius: '1.25rem',
              border: '1px solid var(--border-color)',
              fontSize: '0.925rem',
              color: 'var(--text-primary)',
              lineHeight: 1.6
            }}>
              {item.summary}
            </div>
          </motion.div>
        ))}
      </div>
    </motion.div>
  );
}
