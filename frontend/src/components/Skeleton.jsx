import React from 'react';

export function Skeleton({ width = '100%', height = '20px', borderRadius = '0.75rem', style = {} }) {
  return (
    <div
      className="skeleton"
      style={{
        width,
        height,
        borderRadius,
        ...style
      }}
    />
  );
}

export function CardSkeleton() {
  return (
    <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: '1rem', padding: '1.75rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Skeleton width="40%" height="24px" />
        <Skeleton width="60px" height="24px" borderRadius="9999px" />
      </div>
      <Skeleton width="90%" height="16px" />
      <Skeleton width="75%" height="16px" />
      <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
        <Skeleton width="80px" height="28px" borderRadius="9999px" />
        <Skeleton width="80px" height="28px" borderRadius="9999px" />
      </div>
    </div>
  );
}
