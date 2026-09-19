import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { KnowledgeGraph } from '../components/KnowledgeGraph';
import { Cpu, Layers, Sparkles } from 'lucide-react';
import { Skeleton } from '../components/Skeleton';
import { apiFetch } from '../utils/apiUtils';

export function KnowledgeMapPage({ selectedRepoId }) {
  const [mapData, setMapData] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchMap = async () => {
    setLoading(true);
    try {
      const repoId = selectedRepoId || 1;
      const res = await apiFetch(`/api/memory/${repoId}/knowledge-map`);
      if (res.ok) {
        const data = await res.json();
        setMapData(data);
      }
    } catch (e) {
      console.error("Failed to load Knowledge Map:", e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMap();
  }, [selectedRepoId]);

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        <Skeleton height="100px" borderRadius="1.75rem" />
        <Skeleton height="450px" borderRadius="1.75rem" />
      </div>
    );
  }

  return (
    <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.35 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
            <span className="badge badge-primary">MODULE RELATIONSHIPS</span>
            <span className="badge badge-teal">DEPENDENCY GRAPH</span>
          </div>
          <h1 style={{ fontSize: '2.25rem', fontWeight: 800, fontFamily: 'Space Grotesk, sans-serif' }}>
            Knowledge Map & Graph
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.925rem' }}>
            Architectural relationships, node dependencies, and structural module complexity.
          </p>
        </div>
      </div>

      {/* Graph Visualizer Panel */}
      <div className="card-3xl" style={{ padding: '1.75rem', marginBottom: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Cpu size={20} style={{ color: 'var(--accent-primary)' }} />
            <span>Interactive Module Relationship Graph</span>
          </h3>
          <span className="badge badge-teal">{(mapData?.nodes || []).length} NODES • {(mapData?.edges || []).length} EDGES</span>
        </div>

        <KnowledgeGraph nodes={mapData?.nodes || []} edges={mapData?.edges || []} />
      </div>

      {/* Module Nodes Grid */}
      <div className="grid-cols-3">
        {(mapData?.nodes || []).map((node) => (
          <div key={node.nodeKey} className="card-3xl" style={{ padding: '1.35rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
              <span className="badge badge-teal">{node.category}</span>
              <span style={{ fontSize: '0.75rem', fontFamily: 'JetBrains Mono', color: 'var(--text-muted)' }}>{node.nodeKey}</span>
            </div>
            <h4 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '0.5rem' }}>{node.name}</h4>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', lineHeight: 1.5, marginBottom: '1rem' }}>
              {node.description}
            </p>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', color: 'var(--text-muted)', borderTop: '1px solid var(--border-color)', paddingTop: '0.75rem' }}>
              <span>Complexity Score:</span>
              <strong style={{ color: 'var(--accent-primary)', fontFamily: 'JetBrains Mono' }}>{node.complexityScore}/100</strong>
            </div>
          </div>
        ))}
      </div>
    </motion.div>
  );
}
