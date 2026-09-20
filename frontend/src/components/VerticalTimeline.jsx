import React from 'react';
import { GitCommit, GitPullRequest, Zap, CheckCircle2, Clock } from 'lucide-react';

export function VerticalTimeline({ items = [], onSelect = null, selectedSha = null }) {
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

      {items.map((item, idx) => {
        const clickable = typeof onSelect === 'function' && !!item.sha;
        const isSelected = selectedSha && item.sha === selectedSha;
        return (
        <div
          key={idx}
          onClick={clickable ? () => onSelect(item.sha) : undefined}
          role={clickable ? 'button' : undefined}
          tabIndex={clickable ? 0 : undefined}
          onKeyDown={clickable ? (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onSelect(item.sha); } } : undefined}
          style={{
            position: 'relative',
            display: 'flex',
            flexDirection: 'column',
            gap: '0.35rem',
            cursor: clickable ? 'pointer' : 'default',
            padding: clickable ? '0.5rem 0.75rem' : 0,
            borderRadius: clickable ? '0.75rem' : 0,
            background: isSelected ? 'var(--bg-secondary)' : 'transparent',
            border: isSelected ? '1px solid var(--accent-primary)' : (clickable ? '1px solid transparent' : 'none'),
            transition: 'background 0.15s ease, border-color 0.15s ease'
          }}
        >
          {/* Timeline Dot Node */}
          <div style={{
            position: 'absolute',
            left: clickable ? '-2rem' : '-1.5rem',
            top: clickable ? '0.85rem' : '4px',
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
        );
      })}
    </div>
  );
}
