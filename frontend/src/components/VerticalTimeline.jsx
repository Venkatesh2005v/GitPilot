import React from 'react';
import { GitCommit, GitPullRequest, Zap, CheckCircle2, Clock } from 'lucide-react';

export function VerticalTimeline({ items = [] }) {
  if (!items || items.length === 0) {
    return (
      <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
        No recent activity timeline recorded.
      </div>
    );
  }

  const formatDate = (dateStr) => {
    if (!dateStr) return 'Recently';
    return new Date(dateStr).toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div style={{ position: 'relative', paddingLeft: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Vertical Connecting Line */}
      <div style={{
        position: 'absolute',
        top: '10px',
        bottom: '10px',
        left: '7px',
        width: '2px',
        background: 'var(--border-color)'
      }} />

      {items.map((item, idx) => (
        <div key={idx} style={{ position: 'relative', display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
          {/* Timeline Dot Node */}
          <div style={{
            position: 'absolute',
            left: '-1.5rem',
            top: '4px',
            width: '16px',
            height: '16px',
            borderRadius: '50%',
            backgroundColor: 'var(--bg-app)',
            border: '3px solid var(--accent-primary)',
            boxShadow: '0 0 10px var(--accent-primary-glow)'
          }} />

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '0.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span className="font-mono" style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--accent-primary)' }}>
                {item.sha ? item.sha.substring(0, 7) : 'Commit'}
              </span>
              <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                {item.authorName || 'Developer'}
              </span>
            </div>
            <span style={{ fontSize: '0.725rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
              <Clock size={12} />
              {formatDate(item.commitDate)}
            </span>
          </div>

          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', margin: 0 }}>
            {item.message}
          </p>
        </div>
      ))}
    </div>
  );
}
