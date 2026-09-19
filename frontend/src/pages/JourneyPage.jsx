import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { GitBranch, Lock, Database, Box, Zap, Sparkles, Filter, Calendar, User, Tag } from 'lucide-react';
import { Skeleton } from '../components/Skeleton';
import { apiFetch } from '../utils/apiUtils';

export function JourneyPage({ selectedRepoId }) {
  const [journeys, setJourneys] = useState([]);
  const [loading, setLoading] = useState(true);

  // Filters
  const [month, setMonth] = useState('');
  const [year, setYear] = useState('');
  const [release, setRelease] = useState('');
  const [contributor, setContributor] = useState('');

  const fetchJourney = async () => {
    setLoading(true);
    try {
      const repoId = selectedRepoId || 1;
      let url = `/api/memory/${repoId}/journey?`;
      if (month) url += `month=${month}&`;
      if (year) url += `year=${year}&`;
      if (release) url += `release=${release}&`;
      if (contributor) url += `contributor=${contributor}&`;

      const res = await apiFetch(url);
      if (res.ok) {
        const data = await res.json();
        setJourneys(data);
      }
    } catch (e) {
      console.error("Failed to fetch repository journey:", e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchJourney();
  }, [selectedRepoId, month, year, release, contributor]);

  const getIcon = (iconName) => {
    switch (iconName) {
      case 'lock': return <Lock size={20} style={{ color: 'var(--accent-red)' }} />;
      case 'database': return <Database size={20} style={{ color: '#3b82f6' }} />;
      case 'box': return <Box size={20} style={{ color: '#ec4899' }} />;
      case 'zap': return <Zap size={20} style={{ color: 'var(--accent-yellow)' }} />;
      case 'sparkles': return <Sparkles size={20} style={{ color: 'var(--accent-primary)' }} />;
      default: return <GitBranch size={20} style={{ color: 'var(--accent-teal)' }} />;
    }
  };

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
            <span className="badge badge-primary">ENGINEERING MEMORY</span>
            <span className="badge badge-teal">AUTOMATED TIMELINE</span>
          </div>
          <h1 style={{ fontSize: '2.25rem', fontWeight: 800, fontFamily: 'Space Grotesk, sans-serif' }}>
            Repository Journey
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.925rem' }}>
            Visual engineering milestones generated from repository commit evolution over time.
          </p>
        </div>
      </div>

      {/* Filter Controls Bar */}
      <div className="card-3xl" style={{ marginBottom: '2rem', padding: '1.25rem', display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)' }}>
          <Filter size={16} style={{ color: 'var(--accent-primary)' }} />
          <span>Filters:</span>
        </div>

        {/* Month Filter */}
        <select value={month} onChange={(e) => setMonth(e.target.value)} style={{ padding: '0.45rem 0.85rem', backgroundColor: 'var(--bg-secondary)', color: 'var(--text-primary)', border: '1px solid var(--border-color)', borderRadius: '0.85rem', outline: 'none' }}>
          <option value="">All Months</option>
          <option value="July">July</option>
          <option value="June">June</option>
          <option value="May">May</option>
        </select>

        {/* Year Filter */}
        <select value={year} onChange={(e) => setYear(e.target.value)} style={{ padding: '0.45rem 0.85rem', backgroundColor: 'var(--bg-secondary)', color: 'var(--text-primary)', border: '1px solid var(--border-color)', borderRadius: '0.85rem', outline: 'none' }}>
          <option value="">All Years</option>
          <option value="2026">2026</option>
          <option value="2025">2025</option>
        </select>

        {/* Release Filter */}
        <select value={release} onChange={(e) => setRelease(e.target.value)} style={{ padding: '0.45rem 0.85rem', backgroundColor: 'var(--bg-secondary)', color: 'var(--text-primary)', border: '1px solid var(--border-color)', borderRadius: '0.85rem', outline: 'none' }}>
          <option value="">All Releases</option>
          <option value="v2.0.0">v2.0.0</option>
          <option value="v1.5.0">v1.5.0</option>
          <option value="v1.0.0">v1.0.0</option>
        </select>

        {/* Contributor Filter */}
        <input
          type="text"
          placeholder="Filter by contributor..."
          value={contributor}
          onChange={(e) => setContributor(e.target.value)}
          style={{ padding: '0.45rem 0.85rem', backgroundColor: 'var(--bg-secondary)', color: 'var(--text-primary)', border: '1px solid var(--border-color)', borderRadius: '0.85rem', outline: 'none' }}
        />

        {(month || year || release || contributor) && (
          <button onClick={() => { setMonth(''); setYear(''); setRelease(''); setContributor(''); }} className="btn btn-secondary" style={{ padding: '0.45rem 0.85rem', fontSize: '0.8rem' }}>
            Reset Filters
          </button>
        )}
      </div>

      {/* Visual Vertical Journey Timeline */}
      <div style={{ position: 'relative', paddingLeft: '2.5rem' }}>
        <div style={{ position: 'absolute', left: '1.15rem', top: 0, bottom: 0, width: '3px', background: 'linear-gradient(to bottom, var(--accent-primary), var(--accent-teal))', borderRadius: '9999px' }}></div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
          {journeys.map((item, idx) => (
            <motion.div
              key={item.id || idx}
              whileHover={{ x: 6 }}
              className="card-3xl"
              style={{ position: 'relative', padding: '1.5rem' }}
            >
              {/* Timeline Point Dot */}
              <div style={{
                position: 'absolute',
                left: '-3.15rem',
                top: '1.5rem',
                width: '38px',
                height: '38px',
                borderRadius: '50%',
                backgroundColor: 'var(--bg-card-solid)',
                border: '3px solid var(--accent-primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: 'var(--shadow-sm)'
              }}>
                {getIcon(item.iconName)}
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.65rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
                  <span className="badge badge-teal">{item.milestoneCategory}</span>
                  <h3 style={{ fontSize: '1.25rem', fontWeight: 800 }}>{item.milestoneName}</h3>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                    <Calendar size={13} />
                    <span>{new Date(item.eventDate).toLocaleDateString()}</span>
                  </span>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                    <User size={13} />
                    <span>{item.contributor}</span>
                  </span>
                  <span className="badge badge-primary" style={{ fontSize: '0.7rem' }}>
                    <Tag size={11} />
                    <span>{item.releaseVersion}</span>
                  </span>
                </div>
              </div>

              <p style={{ color: 'var(--text-secondary)', fontSize: '0.925rem', lineHeight: 1.6, margin: 0 }}>
                {item.description}
              </p>
            </motion.div>
          ))}
        </div>
      </div>
    </motion.div>
  );
}
