import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Network, GitMerge, Layers, Code2, RefreshCw, ArrowRight, Info } from 'lucide-react';
import { TopLoadingBar, RepositoryLoader } from '../components/RepositoryLoader';
import { ArchitectureGraph } from '../components/ArchitectureGraph';

export function ArchitectureExplorer({ selectedRepoId }) {
  const [activeTab, setActiveTab] = useState('callgraph');
  const [apiFlowsData, setApiFlowsData] = useState([]);
  const [intelligence, setIntelligence] = useState(null);
  const [loading, setLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);

  const fetchData = async (signal) => {
    if (!selectedRepoId) { setLoading(false); return; }
    setLoading(true);
    try {
      const [flowRes, intelRes] = await Promise.all([
        fetch(`/api/architecture/api-flows/${selectedRepoId}`, { signal }),
        fetch(`/ai/repositories/${selectedRepoId}/intelligence`, { signal })
      ]);
      if (flowRes.ok) {
        const ct = flowRes.headers.get('content-type');
        if (ct && ct.includes('application/json')) setApiFlowsData(await flowRes.json());
      }
      if (intelRes.ok) {
        const ct = intelRes.headers.get('content-type');
        if (ct && ct.includes('application/json')) setIntelligence(await intelRes.json());
      }
    } catch (e) {
      if (e.name === 'AbortError') return;
    } finally {
      setLoading(false);
    }
  };

  const handleRescan = async () => {
    if (!selectedRepoId) return;
    setAnalyzing(true);
    try {
      await fetch(`/api/architecture/code-graph/${selectedRepoId}/analyze`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}' });
      const controller = new AbortController();
      await fetchData(controller.signal);
    } catch (e) {}
    finally { setAnalyzing(false); }
  };

  useEffect(() => {
    const controller = new AbortController();
    setApiFlowsData([]);
    setIntelligence(null);
    fetchData(controller.signal);
    return () => controller.abort();
  }, [selectedRepoId]);

  if (!selectedRepoId) {
    return <div style={{ padding: '4rem', textAlign: 'center', color: 'var(--text-muted)' }}>Select a repository to view architecture.</div>;
  }

  if (loading) {
    return (<><TopLoadingBar loading={true} /><RepositoryLoader /></>);
  }

  const techStack = intelligence?.techStack?.detectedTechnologies || [];
  const manifests = intelligence?.techStack?.detectedManifestFiles || [];
  const suggestions = intelligence?.suggestions || [];

  return (
    <>
    <TopLoadingBar loading={analyzing} />
    <motion.div
      key={selectedRepoId}
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '2rem' }}
    >
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
            <div style={{ padding: '0.6rem', borderRadius: '0.85rem', background: 'var(--accent-primary-bg)', color: 'var(--accent-primary)' }}>
              <Network size={24} />
            </div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 800, fontFamily: 'Space Grotesk, sans-serif' }}>
              Architecture Explorer
            </h1>
          </div>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
            Layered dependency graph, API execution flows, and structural overview.
          </p>
        </div>
        <button onClick={handleRescan} disabled={analyzing} className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', padding: '0.75rem 1.4rem', borderRadius: '1rem' }}>
          <RefreshCw size={18} className={analyzing ? "spin" : ""} />
          <span>{analyzing ? "Scanning..." : "Re-Scan"}</span>
        </button>
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '0.75rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem' }}>
        {[
          { id: 'callgraph', label: 'Layered Call Graph', icon: Layers },
          { id: 'apiflows', label: 'API Flows', icon: GitMerge },
          { id: 'structure', label: 'Project Structure', icon: Code2 },
        ].map(tab => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button key={tab.id} onClick={() => setActiveTab(tab.id)} style={{
              display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.6rem 1.25rem', borderRadius: '0.85rem',
              backgroundColor: isActive ? 'var(--accent-primary-bg)' : 'transparent',
              color: isActive ? 'var(--accent-primary)' : 'var(--text-secondary)',
              border: isActive ? '1px solid var(--border-color-hover)' : '1px solid transparent',
              fontWeight: isActive ? 600 : 500, cursor: 'pointer', transition: 'all 0.2s ease'
            }}>
              <Icon size={18} /><span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* Tab Content */}
      {activeTab === 'callgraph' && (
        <ArchitectureGraph repositoryId={selectedRepoId} />
      )}

      {activeTab === 'apiflows' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {apiFlowsData.length > 0 ? apiFlowsData.map((flow, i) => (
            <div key={i} className="card" style={{ padding: '1.5rem', background: 'var(--bg-card)', borderRadius: '1.25rem', border: '1px solid var(--border-color)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem' }}>
                <span className="badge badge-teal" style={{ fontWeight: 700 }}>{flow.httpMethod}</span>
                <code style={{ fontSize: '0.9rem', color: 'var(--accent-teal)', fontFamily: 'JetBrains Mono, monospace' }}>{flow.endpoint}</code>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                {flow.flowSteps?.map(step => (
                  <div key={step.stepOrder} style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', padding: '0.5rem 0.85rem', borderRadius: '0.75rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
                    <div style={{ width: '22px', height: '22px', borderRadius: '50%', background: 'var(--accent-primary-bg)', color: 'var(--accent-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.7rem', fontWeight: 700, flexShrink: 0 }}>{step.stepOrder}</div>
                    <div>
                      <div style={{ fontSize: '0.825rem', fontWeight: 600, color: 'var(--text-primary)' }}>{step.componentName}</div>
                      <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>{step.details}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )) : (
            <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)', background: 'var(--bg-secondary)', borderRadius: '1.25rem', border: '1px solid var(--border-color)' }}>
              <GitMerge size={36} style={{ marginBottom: '0.75rem', opacity: 0.5 }} />
              <p style={{ fontSize: '0.95rem', marginBottom: '0.5rem' }}>No API flows detected yet.</p>
              <p style={{ fontSize: '0.825rem' }}>Click "Re-Scan" to analyze controller-to-service execution chains.</p>
            </div>
          )}
        </div>
      )}

      {activeTab === 'structure' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* Tech Stack Summary */}
          <div className="card" style={{ padding: '1.5rem', background: 'var(--bg-card)', borderRadius: '1.25rem', border: '1px solid var(--border-color)' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Code2 size={18} style={{ color: 'var(--accent-teal)' }} /> Detected Technologies
            </h3>
            {techStack.length > 0 ? (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.6rem' }}>
                {techStack.map((t, i) => (
                  <span key={i} style={{ padding: '0.4rem 0.85rem', borderRadius: '0.75rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)' }}>{t}</span>
                ))}
              </div>
            ) : (
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>No technologies detected. Run the intelligence analysis first.</p>
            )}
          </div>

          {/* Manifest Files */}
          <div className="card" style={{ padding: '1.5rem', background: 'var(--bg-card)', borderRadius: '1.25rem', border: '1px solid var(--border-color)' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Layers size={18} style={{ color: 'var(--accent-primary)' }} /> Build & Configuration Files
            </h3>
            {manifests.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                {manifests.map((m, i) => (
                  <div key={i} style={{ padding: '0.5rem 0.85rem', borderRadius: '0.65rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)', fontFamily: 'JetBrains Mono, monospace', fontSize: '0.825rem', color: 'var(--accent-teal)' }}>{m}</div>
                ))}
              </div>
            ) : (
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>No manifest files detected.</p>
            )}
          </div>

          {/* Architecture Insights from AI */}
          {intelligence?.summary && (
            <div className="card" style={{ padding: '1.5rem', background: 'var(--bg-card)', borderRadius: '1.25rem', border: '1px solid var(--border-color)' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Info size={18} style={{ color: '#ec4899' }} /> Architecture Summary
              </h3>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
                <div style={{ padding: '0.75rem', background: 'var(--bg-secondary)', borderRadius: '0.85rem' }}>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>Style</div>
                  <div style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-primary)' }}>{intelligence.summary.architectureStyle || '—'}</div>
                </div>
                <div style={{ padding: '0.75rem', background: 'var(--bg-secondary)', borderRadius: '0.85rem' }}>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>Complexity</div>
                  <div style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-primary)' }}>{intelligence.summary.complexityLevel || '—'}</div>
                </div>
                <div style={{ padding: '0.75rem', background: 'var(--bg-secondary)', borderRadius: '0.85rem' }}>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>Language</div>
                  <div style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-primary)' }}>{intelligence?.techStack?.primaryLanguage || '—'}</div>
                </div>
                <div style={{ padding: '0.75rem', background: 'var(--bg-secondary)', borderRadius: '0.85rem' }}>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>Category</div>
                  <div style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-primary)' }}>{intelligence?.techStack?.category || '—'}</div>
                </div>
              </div>
            </div>
          )}

          {/* Improvement suggestions as structural recommendations */}
          {suggestions.length > 0 && (
            <div className="card" style={{ padding: '1.5rem', background: 'var(--bg-card)', borderRadius: '1.25rem', border: '1px solid var(--border-color)' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem' }}>Structural Recommendations</h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                {suggestions.slice(0, 5).map((s, i) => (
                  <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', padding: '0.6rem 1rem', borderRadius: '0.75rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
                    <span className={`badge ${s.priority === 'HIGH' ? 'badge-primary' : 'badge-teal'}`} style={{ fontSize: '0.65rem', flexShrink: 0 }}>{s.priority}</span>
                    <div>
                      <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)' }}>{s.title}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{s.description}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </motion.div>
    </>
  );
}
