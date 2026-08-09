import React, { useState, useEffect } from 'react';
import { ShieldAlert, AlertTriangle, Radio, Compass, ArrowRight, Zap, CheckCircle2, Info } from 'lucide-react';

export function ImpactAndDriftView({ selectedRepoId }) {
  const repoId = selectedRepoId || 1;
  const [impactData, setImpactData] = useState(null);
  const [driftData, setDriftData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [modifiedInput, setModifiedInput] = useState('UserService.java, UserRepository.java');
  const [analyzingImpact, setAnalyzingImpact] = useState(false);

  const fetchDrift = async () => {
    try {
      const res = await fetch(`/api/architecture/drift/${repoId}`);
      if (res.ok) {
        const data = await res.json();
        setDriftData(data);
      }
    } catch (e) {
      console.error("Failed to load drift data:", e);
    }
  };

  const handleRunImpact = async () => {
    setAnalyzingImpact(true);
    try {
      const files = modifiedInput.split(',').map(f => f.trim()).filter(Boolean);
      const res = await fetch(`/api/architecture/impact/${repoId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(files)
      });
      if (res.ok) {
        const data = await res.json();
        setImpactData(data);
      }
    } catch (e) {
      console.error("Impact analysis failed:", e);
    } finally {
      setAnalyzingImpact(false);
    }
  };

  useEffect(() => {
    setLoading(true);
    Promise.all([fetchDrift(), handleRunImpact()]).finally(() => setLoading(false));
  }, [repoId]);

  return (
    <div style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      {/* Header */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
          <div style={{ padding: '0.6rem', borderRadius: '0.85rem', background: 'rgba(239, 68, 68, 0.15)', color: '#ef4444' }}>
            <Radio size={24} />
          </div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 800, fontFamily: 'Space Grotesk, sans-serif' }}>
            Change Impact & Architecture Drift Detector
          </h1>
        </div>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
          Predict downstream blast radius and detect architectural violations before deployment.
        </p>
      </div>

      {loading ? (
        <div style={{ padding: '4rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          Evaluating architectural rules & graph relationships...
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '2rem' }}>
          {/* Change Impact Panel */}
          <div className="card" style={{ padding: '1.75rem', background: 'var(--bg-card)', borderRadius: '1.25rem', border: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>
                Change Impact & Blast Radius Radar
              </h2>
              {impactData && (
                <span className={`badge ${impactData.riskLevel === 'CRITICAL' ? 'badge-pink' : 'badge-gold'}`} style={{ fontWeight: 700 }}>
                  Risk: {impactData.riskLevel}
                </span>
              )}
            </div>

            {/* Input Form for simulating file changes */}
            <div style={{ display: 'flex', gap: '0.75rem' }}>
              <input
                type="text"
                value={modifiedInput}
                onChange={e => setModifiedInput(e.target.value)}
                placeholder="Modified files (comma separated)"
                style={{
                  flexGrow: 1,
                  padding: '0.75rem 1rem',
                  borderRadius: '0.85rem',
                  background: 'var(--bg-app)',
                  border: '1px solid var(--border-color)',
                  color: 'var(--text-primary)',
                  fontSize: '0.9rem'
                }}
              />
              <button
                onClick={handleRunImpact}
                disabled={analyzingImpact}
                className="btn btn-primary"
                style={{ padding: '0.75rem 1.25rem', borderRadius: '0.85rem', whiteSpace: 'nowrap' }}
              >
                {analyzingImpact ? 'Evaluating...' : 'Simulate Impact'}
              </button>
            </div>

            {impactData && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                {/* Blast Radius Score Meter */}
                <div style={{ padding: '1.25rem', borderRadius: '1rem', background: 'var(--bg-app)', border: '1px solid var(--border-color)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                    <span style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Blast Radius Score</span>
                    <span style={{ fontSize: '1.1rem', fontWeight: 800, color: 'var(--accent-primary)' }}>{impactData.blastRadiusScore} / 100</span>
                  </div>
                  <div style={{ height: '8px', background: 'rgba(255,255,255,0.1)', borderRadius: '4px', overflow: 'hidden' }}>
                    <div style={{ height: '100%', width: `${impactData.blastRadiusScore}%`, background: 'linear-gradient(90deg, #6366f1, #ec4899)', transition: 'width 0.4s ease' }}></div>
                  </div>
                </div>

                {/* Direct & Downstream Breakdown */}
                <div>
                  <h4 style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>Directly Modified Classes</h4>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                    {impactData.directlyModifiedClasses?.map((cls, idx) => (
                      <span key={idx} style={{ padding: '0.4rem 0.8rem', borderRadius: '0.65rem', background: 'rgba(99, 102, 241, 0.15)', color: 'var(--accent-primary)', fontSize: '0.85rem', fontWeight: 600 }}>
                        {cls}
                      </span>
                    ))}
                  </div>
                </div>

                <div>
                  <h4 style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>Affected Downstream APIs & Services</h4>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                    {impactData.affectedDownstreamClasses?.map((cls, idx) => (
                      <span key={idx} style={{ padding: '0.4rem 0.8rem', borderRadius: '0.65rem', background: 'rgba(236, 72, 153, 0.15)', color: '#ec4899', fontSize: '0.85rem', fontWeight: 600 }}>
                        {cls}
                      </span>
                    ))}
                  </div>
                </div>

                <div style={{ padding: '1rem', borderRadius: '0.85rem', background: 'rgba(99, 102, 241, 0.08)', border: '1px solid rgba(99, 102, 241, 0.2)', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                  <div style={{ fontWeight: 700, color: 'var(--text-primary)', marginBottom: '0.25rem' }}>Deterministic Analysis Summary:</div>
                  {impactData.aiExplanation}
                </div>
              </div>
            )}
          </div>

          {/* Architecture Drift Panel */}
          <div className="card" style={{ padding: '1.75rem', background: 'var(--bg-card)', borderRadius: '1.25rem', border: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>
                Architecture Drift & Layer Violations
              </h2>
              {driftData && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Health Score:</span>
                  <span style={{ fontSize: '1.1rem', fontWeight: 800, color: driftData.architecturalHealthScore > 80 ? '#10b981' : '#f59e0b' }}>
                    {driftData.architecturalHealthScore}%
                  </span>
                </div>
              )}
            </div>

            {driftData?.violations?.length ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                {driftData.violations.map((v, idx) => (
                  <div key={idx} style={{ padding: '1.25rem', borderRadius: '1rem', background: 'var(--bg-app)', border: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <span className={`badge ${v.severity === 'CRITICAL' ? 'badge-pink' : 'badge-gold'}`} style={{ textTransform: 'uppercase', fontWeight: 700 }}>
                        {v.severity} • {v.violationType}
                      </span>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{v.ruleName}</span>
                    </div>

                    <div style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                      {v.sourceComponent} → {v.targetComponent}
                    </div>

                    <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', margin: 0 }}>
                      {v.description}
                    </p>

                    <div style={{ fontSize: '0.825rem', color: 'var(--accent-teal)', fontWeight: 600, background: 'rgba(20, 184, 166, 0.1)', padding: '0.5rem 0.75rem', borderRadius: '0.65rem' }}>
                      💡 Recommendation: {v.recommendation}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                <CheckCircle2 size={32} style={{ color: '#10b981', marginBottom: '0.75rem' }} />
                <div>Zero architecture drift violations detected! Codebase respects DDD layer boundaries.</div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
