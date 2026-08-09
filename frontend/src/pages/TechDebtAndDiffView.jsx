import React, { useState, useEffect } from 'react';
import { Gauge, GitCompare, Award, Activity, FileCode, CheckCircle, TrendingUp, Cpu } from 'lucide-react';

export function TechDebtAndDiffView({ selectedRepoId }) {
  const repoId = selectedRepoId || 1;
  const [techDebtData, setTechDebtData] = useState(null);
  const [releaseData, setReleaseData] = useState(null);
  const [diffData, setDiffData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      fetch(`/api/architecture/tech-debt/${repoId}`).then(r => r.ok ? r.json() : null),
      fetch(`/api/architecture/releases/${repoId}`).then(r => r.ok ? r.json() : null),
      fetch(`/api/architecture/diff/${repoId}`).then(r => r.ok ? r.json() : null),
    ]).then(([debt, rel, diff]) => {
      setTechDebtData(debt);
      setReleaseData(rel);
      setDiffData(diff);
    }).catch(e => {
      console.error("Failed to load tech debt / diff data:", e);
    }).finally(() => setLoading(false));
  }, [repoId]);

  return (
    <div style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      {/* Header */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
          <div style={{ padding: '0.6rem', borderRadius: '0.85rem', background: 'rgba(20, 184, 166, 0.15)', color: 'var(--accent-teal)' }}>
            <Gauge size={24} />
          </div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 800, fontFamily: 'Space Grotesk, sans-serif' }}>
            Technical Debt Explorer & Knowledge Diff
          </h1>
        </div>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
          Compute deterministic code metrics (Complexity, Fan-In/Out, Instability $I$) and compare architecture snapshot deltas.
        </p>
      </div>

      {loading ? (
        <div style={{ padding: '4rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          Computing code metrics & diff snapshot matrix...
        </div>
      ) : (
        <>
          {/* Top Summary Stats */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.25rem' }}>
            <div className="card" style={{ padding: '1.25rem', background: 'var(--bg-card)', borderRadius: '1rem', border: '1px solid var(--border-color)' }}>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Average Complexity</div>
              <div style={{ fontSize: '1.75rem', fontWeight: 800, marginTop: '0.25rem', color: 'var(--text-primary)' }}>
                {techDebtData?.averageComplexity || 12}
              </div>
            </div>

            <div className="card" style={{ padding: '1.25rem', background: 'var(--bg-card)', borderRadius: '1rem', border: '1px solid var(--border-color)' }}>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Average Instability Index (I)</div>
              <div style={{ fontSize: '1.75rem', fontWeight: 800, marginTop: '0.25rem', color: 'var(--accent-teal)' }}>
                {techDebtData?.averageInstability || 0.42}
              </div>
            </div>

            <div className="card" style={{ padding: '1.25rem', background: 'var(--bg-card)', borderRadius: '1rem', border: '1px solid var(--border-color)' }}>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Highest Tech Debt Class</div>
              <div style={{ fontSize: '1.2rem', fontWeight: 700, marginTop: '0.25rem', color: '#ec4899', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {techDebtData?.highestDebtClass || 'UserService'}
              </div>
            </div>

            <div className="card" style={{ padding: '1.25rem', background: 'var(--bg-card)', borderRadius: '1rem', border: '1px solid var(--border-color)' }}>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Release Risk Rating</div>
              <div style={{ fontSize: '1.75rem', fontWeight: 800, marginTop: '0.25rem', color: '#10b981' }}>
                {releaseData?.riskRating || 'LOW'}
              </div>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))', gap: '2rem' }}>
            {/* Technical Debt Table / Cards */}
            <div className="card" style={{ padding: '1.75rem', background: 'var(--bg-card)', borderRadius: '1.25rem', border: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              <h2 style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>
                Class Level Software Metrics & Refactoring Advisory
              </h2>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                {techDebtData?.classMetrics?.map((m, idx) => (
                  <div key={idx} style={{ padding: '1.25rem', borderRadius: '1rem', background: 'var(--bg-app)', border: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <span style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--text-primary)' }}>{m.className}</span>
                      <span className="badge badge-purple" style={{ fontWeight: 700 }}>
                        Score: {m.techDebtScore}/100
                      </span>
                    </div>

                    <div style={{ display: 'flex', gap: '1.25rem', flexWrap: 'wrap', fontSize: '0.825rem', color: 'var(--text-secondary)' }}>
                      <span>LOC: <b>{m.loc}</b></span>
                      <span>Complexity: <b>{m.cyclomaticComplexity}</b></span>
                      <span>Fan-In: <b>{m.fanIn}</b></span>
                      <span>Fan-Out: <b>{m.fanOut}</b></span>
                      <span>Instability (I): <b>{m.instabilityIndex}</b></span>
                    </div>

                    <div style={{ fontSize: '0.825rem', color: 'var(--text-secondary)', background: 'rgba(255,255,255,0.03)', padding: '0.6rem 0.85rem', borderRadius: '0.65rem' }}>
                      💡 {m.refactoringRecommendation}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Knowledge Diff & Release Intelligence Panel */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
              {/* Knowledge Diff Snapshot */}
              <div className="card" style={{ padding: '1.75rem', background: 'var(--bg-card)', borderRadius: '1.25rem', border: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <h2 style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>
                    Knowledge Diff Snapshot Matrix
                  </h2>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                    <GitCompare size={16} />
                    <span>{diffData?.sourceRef} → {diffData?.targetRef}</span>
                  </div>
                </div>

                <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', margin: 0 }}>
                  {diffData?.architectureEvolutionSummary}
                </p>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  <h4 style={{ fontSize: '0.85rem', textTransform: 'uppercase', color: 'var(--text-muted)', fontWeight: 700 }}>Added Graph Relationships</h4>
                  {diffData?.addedRelationships?.map((rel, idx) => (
                    <div key={idx} style={{ fontSize: '0.825rem', fontFamily: 'Fira Code, monospace', color: '#10b981', background: 'rgba(16, 185, 129, 0.1)', padding: '0.4rem 0.75rem', borderRadius: '0.5rem' }}>
                      {rel}
                    </div>
                  ))}
                </div>
              </div>

              {/* Release Intelligence */}
              <div className="card" style={{ padding: '1.75rem', background: 'var(--bg-card)', borderRadius: '1.25rem', border: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                <h2 style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>
                  Release Intelligence Summary ({releaseData?.releaseTag})
                </h2>

                <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                  {releaseData?.summary}
                </div>

                <div>
                  <h4 style={{ fontSize: '0.85rem', textTransform: 'uppercase', color: 'var(--text-muted)', fontWeight: 700, marginBottom: '0.5rem' }}>Technology & Architectural Additions</h4>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                    {releaseData?.technologyAdditions?.map((tech, idx) => (
                      <span key={idx} style={{ padding: '0.35rem 0.75rem', borderRadius: '0.65rem', background: 'var(--accent-primary-bg)', color: 'var(--accent-primary)', fontSize: '0.8rem', fontWeight: 600 }}>
                        {tech}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
