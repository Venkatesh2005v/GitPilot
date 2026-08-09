import React from 'react';

export function CircularProgress({ score = 0, size = 120, strokeWidth = 8, title = "Health Score" }) {
  // Defensive coercion to guarantee numeric primitive value
  let numericScore = 0;
  if (typeof score === 'number' && !isNaN(score)) {
    numericScore = score;
  } else if (typeof score === 'object' && score !== null && typeof score.overallHealthScore === 'number') {
    numericScore = score.overallHealthScore;
  } else if (typeof score === 'string') {
    const parsed = parseFloat(score);
    numericScore = !isNaN(parsed) ? parsed : 0;
  }
  numericScore = Math.max(0, Math.min(100, Math.round(numericScore)));

  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - (numericScore / 100) * circumference;

  const getColor = (s) => {
    if (s >= 80) return 'var(--accent-green)';
    if (s >= 60) return 'var(--accent-yellow)';
    return 'var(--accent-red)';
  };

  const strokeColor = getColor(numericScore);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ position: 'relative', width: size, height: size }}>
        <svg width={size} height={size} style={{ transform: 'rotate(-90deg)' }}>
          {/* Background Ring */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            stroke="var(--bg-tertiary)"
            strokeWidth={strokeWidth}
            fill="transparent"
          />
          {/* Animated Value Ring */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            stroke={strokeColor}
            strokeWidth={strokeWidth}
            fill="transparent"
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
            style={{
              transition: 'stroke-dashoffset 1s ease-in-out, stroke 0.3s ease',
              filter: `drop-shadow(0 0 6px ${strokeColor}44)`
            }}
          />
        </svg>
        <div style={{
          position: 'absolute',
          top: 0,
          left: 0,
          width: '100%',
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          textAlign: 'center'
        }}>
          <span style={{
            fontSize: size > 100 ? '1.75rem' : '1.25rem',
            fontWeight: 800,
            fontFamily: 'Space Grotesk, sans-serif',
            color: 'var(--text-primary)',
            lineHeight: 1
          }}>
            {numericScore}
          </span>
          <span style={{
            fontSize: '0.65rem',
            color: 'var(--text-muted)',
            textTransform: 'uppercase',
            letterSpacing: '0.05em',
            marginTop: '0.2rem'
          }}>
            / 100
          </span>
        </div>
      </div>
      {title && (
        <span style={{
          marginTop: '0.5rem',
          fontSize: '0.75rem',
          fontWeight: 600,
          color: 'var(--text-secondary)',
          textTransform: 'uppercase',
          letterSpacing: '0.05em'
        }}>
          {title}
        </span>
      )}
    </div>
  );
}
