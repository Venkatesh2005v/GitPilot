import React, { useState } from 'react';
import { GitCommit, User, Clock, Plus, Minus, FileText, X, ChevronDown, ChevronRight, ExternalLink, Zap } from 'lucide-react';
import { Skeleton } from './Skeleton';

const STATUS_STYLE = {
  added: { label: 'ADDED', color: 'var(--accent-green)' },
  modified: { label: 'MODIFIED', color: 'var(--accent-primary)' },
  removed: { label: 'REMOVED', color: 'var(--accent-red)' },
  renamed: { label: 'RENAMED', color: 'var(--accent-teal)' },
};

function statusStyle(status) {
  return STATUS_STYLE[(status || '').toLowerCase()] || { label: (status || 'CHANGED').toUpperCase(), color: 'var(--text-muted)' };
}

function formatDate(dateStr) {
  if (!dateStr) return 'Unknown date';
  return new Date(dateStr).toLocaleString('en-US', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function FileRow({ file, commitUrl }) {
  const [expanded, setExpanded] = useState(false);
  const s = statusStyle(file.status);
  // Prefer the file's GitHub blob URL; fall back to the commit page when unavailable.
  const fullDiffUrl = file.blobUrl || commitUrl || null;
  return (
    <div style={{ border: '1px solid var(--border-color)', borderRadius: '0.85rem', background: 'var(--bg-secondary)', overflow: 'hidden' }}>
      <div
        onClick={() => setExpanded(e => !e)}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setExpanded(x => !x); } }}
        style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '0.75rem', padding: '0.65rem 0.85rem', cursor: 'pointer' }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', minWidth: 0 }}>
          {expanded ? <ChevronDown size={15} style={{ flexShrink: 0, color: 'var(--text-muted)' }} /> : <ChevronRight size={15} style={{ flexShrink: 0, color: 'var(--text-muted)' }} />}
          <span className="badge" style={{ fontSize: '0.62rem', fontWeight: 700, color: s.color, border: `1px solid ${s.color}`, flexShrink: 0 }}>{s.label}</span>
          <span style={{ fontFamily: 'JetBrains Mono', fontSize: '0.8rem', color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {file.filename}
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexShrink: 0, fontFamily: 'JetBrains Mono', fontSize: '0.75rem' }}>
          <span style={{ color: 'var(--accent-green)' }}>+{file.additions ?? 0}</span>
          <span style={{ color: 'var(--accent-red)' }}>-{file.deletions ?? 0}</span>
        </div>
      </div>

      {file.previousFilename && (
        <div style={{ padding: '0 0.85rem 0.4rem 2.1rem', fontSize: '0.72rem', color: 'var(--text-muted)', fontFamily: 'JetBrains Mono' }}>
          renamed from {file.previousFilename}
        </div>
      )}

      {expanded && (
        <div style={{ borderTop: '1px solid var(--border-color)' }}>
          {file.patch ? (
            <>
              <pre style={{
                margin: 0, padding: '0.85rem', overflowX: 'auto', fontFamily: 'JetBrains Mono',
                fontSize: '0.75rem', lineHeight: 1.5, color: 'var(--text-primary)', background: 'var(--bg-app)', whiteSpace: 'pre'
              }}>
                {file.patch.split('\n').map((line, i) => {
                  let color = 'var(--text-secondary)';
                  if (line.startsWith('+') && !line.startsWith('+++')) color = 'var(--accent-green)';
                  else if (line.startsWith('-') && !line.startsWith('---')) color = 'var(--accent-red)';
                  else if (line.startsWith('@@')) color = 'var(--accent-primary)';
                  return <div key={i} style={{ color }}>{line || ' '}</div>;
                })}
              </pre>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '0.5rem', padding: '0.5rem 0.85rem', borderTop: '1px solid var(--border-color)', flexWrap: 'wrap' }}>
                {file.patchTruncated ? (
                  <span style={{ fontSize: '0.72rem', color: 'var(--accent-yellow, #d69e2e)' }}>
                    ⚠ Diff truncated — showing the first portion of a large patch.
                  </span>
                ) : (
                  <span style={{ fontSize: '0.72rem', color: 'var(--accent-green)' }}>
                    Complete diff shown.
                  </span>
                )}
                {fullDiffUrl && (
                  <a href={fullDiffUrl} target="_blank" rel="noopener noreferrer" style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.72rem', color: 'var(--accent-primary)', textDecoration: 'none' }}>
                    <ExternalLink size={12} /> Open full diff on GitHub
                  </a>
                )}
              </div>
            </>
          ) : (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '0.5rem', padding: '0.85rem', flexWrap: 'wrap' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Diff unavailable for this file (binary or not provided by GitHub).
              </span>
              {fullDiffUrl && (
                <a href={fullDiffUrl} target="_blank" rel="noopener noreferrer" style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.72rem', color: 'var(--accent-primary)', textDecoration: 'none' }}>
                  <ExternalLink size={12} /> Open on GitHub
                </a>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export function CommitDetailPanel({ loading, error, detail, onClose, onRetry }) {
  return (
    <div className="card-3xl" style={{ padding: '1.75rem', marginTop: '1.5rem', border: '1px solid var(--border-color-hover)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
        <h3 style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Grotesk', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <GitCommit size={20} style={{ color: 'var(--accent-primary)' }} />
          <span>Commit Details</span>
        </h3>
        {onClose && (
          <button onClick={onClose} className="btn btn-ghost" style={{ padding: '0.35rem 0.5rem' }} aria-label="Close commit details">
            <X size={16} />
          </button>
        )}
      </div>

      {loading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          <Skeleton height="60px" borderRadius="0.85rem" />
          <Skeleton height="40px" borderRadius="0.85rem" />
          <Skeleton height="120px" borderRadius="0.85rem" />
        </div>
      ) : error ? (
        <div style={{ padding: '1.25rem', borderRadius: '0.85rem', background: 'var(--accent-red-bg, rgba(239,68,68,0.08))', border: '1px solid var(--accent-red)', color: 'var(--text-primary)' }}>
          <p style={{ margin: '0 0 0.75rem 0', fontSize: '0.9rem' }}>⚠️ {error}</p>
          {onRetry && (
            <button onClick={onRetry} className="btn btn-secondary" style={{ fontSize: '0.8rem' }}>Retry</button>
          )}
        </div>
      ) : detail ? (
        <>
          {/* Header */}
          <p style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-primary)', margin: '0 0 0.75rem 0', whiteSpace: 'pre-wrap' }}>
            {detail.message || '(no commit message)'}
          </p>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '1.25rem' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}><User size={13} /> {detail.authorName || 'Unknown'}</span>
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}><Clock size={13} /> {formatDate(detail.date)}</span>
            <span style={{ fontFamily: 'JetBrains Mono' }}>{detail.sha ? detail.sha.substring(0, 10) : ''}</span>
            {detail.htmlUrl && (
              <a href={detail.htmlUrl} target="_blank" rel="noopener noreferrer" style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', color: 'var(--accent-primary)', textDecoration: 'none' }}>
                <ExternalLink size={13} /> View on GitHub
              </a>
            )}
          </div>

          {/* Summary */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', marginBottom: '1.25rem' }}>
            <span className="badge" style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', border: '1px solid var(--border-color)' }}>
              <FileText size={13} /> {detail.changedFileCount ?? (detail.files ? detail.files.length : 0)} files
            </span>
            <span className="badge" style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', color: 'var(--accent-green)', border: '1px solid var(--border-color)' }}>
              <Plus size={13} /> {detail.additions ?? 0}
            </span>
            <span className="badge" style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', color: 'var(--accent-red)', border: '1px solid var(--border-color)' }}>
              <Minus size={13} /> {detail.deletions ?? 0}
            </span>
          </div>

          {/* Deterministic Technical Impact (evidence-based, never invented) */}
          {detail.technicalImpact && (
            <div style={{ padding: '1rem 1.15rem', borderRadius: '1rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)', marginBottom: '1.25rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.6rem' }}>
                <Zap size={15} style={{ color: 'var(--accent-primary)' }} />
                <strong style={{ fontSize: '0.85rem', color: 'var(--text-primary)' }}>Technical Impact</strong>
              </div>
              <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', margin: '0 0 0.6rem 0', lineHeight: 1.5 }}>
                {detail.technicalImpact.summary}
              </p>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
                {(detail.technicalImpact.affectedAreas || []).map((a, i) => (
                  <span key={'a' + i} className="badge badge-primary" style={{ fontSize: '0.65rem' }}>{a}</span>
                ))}
                {(detail.technicalImpact.technologies || []).map((t, i) => (
                  <span key={'t' + i} className="badge badge-teal" style={{ fontSize: '0.65rem' }}>{t}</span>
                ))}
              </div>
              {detail.technicalImpact.architecturalArea && (
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.6rem' }}>
                  Architectural area: <strong style={{ color: 'var(--text-secondary)' }}>{detail.technicalImpact.architecturalArea}</strong>
                </div>
              )}
            </div>
          )}

          {/* Changed files */}
          {detail.files && detail.files.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
              {detail.files.map((f, idx) => <FileRow key={idx} file={f} commitUrl={detail.htmlUrl} />)}
            </div>
          ) : (
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>No changed files reported for this commit.</p>
          )}
        </>
      ) : null}
    </div>
  );
}
