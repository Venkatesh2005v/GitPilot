import React from 'react';
import { ArrowUpRight, CheckCircle2, AlertCircle, Info } from 'lucide-react';

export function RecommendationCard({ title, category, priority, description, actionText = "Apply Recommendation" }) {
  const getPriorityStyle = (p) => {
    switch ((p || '').toUpperCase()) {
      case 'HIGH':
        return { badgeClass: 'badge-danger', iconColor: 'var(--accent-red)' };
      case 'MEDIUM':
        return { badgeClass: 'badge-warning', iconColor: 'var(--accent-yellow)' };
      default:
        return { badgeClass: 'badge-info', iconColor: 'var(--accent-sky)' };
    }
  };

  const style = getPriorityStyle(priority);

  return (
    <div className="card" style={{
      display: 'flex',
      flexDirection: 'column',
      justify: 'space-between',
      gap: '1rem',
      padding: '1.25rem',
      borderRadius: '1.25rem'
    }}>
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
          <span className="badge badge-outline" style={{ fontSize: '0.7rem', textTransform: 'uppercase' }}>
            {category || 'Quality'}
          </span>
          <span className={`badge ${style.badgeClass}`} style={{ fontSize: '0.65rem', fontWeight: 700 }}>
            {priority || 'MEDIUM'} PRIORITY
          </span>
        </div>

        <h4 style={{ fontSize: '1rem', fontWeight: 700, fontFamily: 'Space Grotesk, sans-serif', marginBottom: '0.5rem' }}>
          {title}
        </h4>

        <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', lineHeight: '1.5' }}>
          {description}
        </p>
      </div>

      <div style={{ display: 'flex', justifyContent: 'flex-end', borderTop: '1px solid var(--border-color)', paddingTop: '0.75rem' }}>
        <button className="btn btn-ghost" style={{ fontSize: '0.75rem', padding: '0.35rem 0.65rem', color: 'var(--accent-primary)' }}>
          <span>{actionText}</span>
          <ArrowUpRight size={14} />
        </button>
      </div>
    </div>
  );
}
