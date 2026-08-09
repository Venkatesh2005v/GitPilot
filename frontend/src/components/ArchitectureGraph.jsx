import React, { useEffect, useRef, useState, useMemo, useCallback } from 'react';
import cytoscape from 'cytoscape';
import dagre from 'cytoscape-dagre';
import { Search, Maximize2, RotateCcw, Download, X } from 'lucide-react';

cytoscape.use(dagre);

const NODE_COLORS = { CONTROLLER: '#8b5cf6', SERVICE: '#3b82f6', REPOSITORY: '#10b981', EXTERNAL: '#f97316' };
const EDGE_COLORS = { 'CONTROLLER-SERVICE': '#8b5cf6', 'SERVICE-SERVICE': '#3b82f6', 'SERVICE-REPOSITORY': '#10b981' };

export function ArchitectureGraph({ repositoryId }) {
  const containerRef = useRef(null);
  const cyRef = useRef(null);
  const [graphData, setGraphData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedNode, setSelectedNode] = useState(null);
  const [search, setSearch] = useState('');

  // Fetch data
  useEffect(() => {
    if (!repositoryId) return;
    setLoading(true);
    setSelectedNode(null);
    const controller = new AbortController();
    fetch(`/architecture/repositories/${repositoryId}/callgraph`, { signal: controller.signal })
      .then(r => r.ok ? r.json() : null)
      .then(d => setGraphData(d))
      .catch(e => { if (e.name !== 'AbortError') setGraphData(null); })
      .finally(() => setLoading(false));
    return () => controller.abort();
  }, [repositoryId]);

  // Build cytoscape elements
  const elements = useMemo(() => {
    if (!graphData?.nodes?.length) return null;
    const nodeMap = {};
    graphData.nodes.forEach(n => { nodeMap[n.id] = n; });
    const nodes = graphData.nodes.map(n => ({
      data: { id: n.id, label: n.id, type: n.type || 'SERVICE', layer: n.layer ?? 1 }
    }));
    const edges = graphData.edges.map((e, i) => {
      const srcType = nodeMap[e.from]?.type || 'SERVICE';
      const tgtType = nodeMap[e.to]?.type || 'SERVICE';
      const colorKey = `${srcType}-${tgtType}`;
      return { data: { id: `e${i}`, source: e.from, target: e.to, colorKey } };
    });
    return [...nodes, ...edges];
  }, [graphData]);

  // Init/update cytoscape
  useEffect(() => {
    if (!elements || !containerRef.current) return;
    if (cyRef.current) cyRef.current.destroy();

    const cy = cytoscape({
      container: containerRef.current,
      elements,
      style: [
        { selector: 'node', style: {
          'label': 'data(label)', 'text-valign': 'center', 'text-halign': 'center',
          'font-size': '11px', 'font-family': 'JetBrains Mono, monospace', 'color': '#e2e8f0',
          'text-outline-color': '#0d1117', 'text-outline-width': 2,
          'width': 'label', 'height': 36, 'padding': '12px',
          'shape': 'round-rectangle', 'border-width': 2,
          'background-color': 'mapData(layer, 0, 2, #8b5cf6, #10b981)',
          'border-color': 'mapData(layer, 0, 2, #8b5cf6, #10b981)',
          'transition-property': 'opacity', 'transition-duration': '0.2s'
        }},
        { selector: 'node[type="CONTROLLER"]', style: { 'background-color': '#8b5cf6', 'border-color': '#a78bfa' }},
        { selector: 'node[type="SERVICE"]', style: { 'background-color': '#3b82f6', 'border-color': '#60a5fa' }},
        { selector: 'node[type="REPOSITORY"]', style: { 'background-color': '#10b981', 'border-color': '#34d399' }},
        { selector: 'node[type="EXTERNAL"]', style: { 'background-color': '#f97316', 'border-color': '#fb923c' }},
        { selector: 'edge', style: {
          'width': 2, 'curve-style': 'bezier', 'target-arrow-shape': 'triangle',
          'target-arrow-color': '#475569', 'line-color': '#475569', 'arrow-scale': 1.2,
          'transition-property': 'opacity, line-color', 'transition-duration': '0.2s'
        }},
        { selector: '.highlighted', style: { 'opacity': 1, 'border-width': 3 }},
        { selector: '.faded', style: { 'opacity': 0.15 }},
        { selector: '.edge-highlighted', style: { 'line-color': '#e2e8f0', 'target-arrow-color': '#e2e8f0', 'width': 3 }},
        { selector: '.searched', style: { 'border-width': 4, 'border-color': '#facc15', 'background-color': '#facc15' }}
      ],
      layout: { name: 'dagre', rankDir: 'TB', spacingFactor: 1.6, nodeSep: 60, rankSep: 100 },
      minZoom: 0.3, maxZoom: 3, wheelSensitivity: 0.3
    });

    cy.on('tap', 'node', (e) => {
      const node = e.target;
      const incoming = node.incomers('edge').length;
      const outgoing = node.outgoers('edge').length;
      const connected = [...new Set([...node.incomers('node').map(n => n.id()), ...node.outgoers('node').map(n => n.id())])];
      setSelectedNode({ id: node.id(), type: node.data('type'), layer: node.data('layer'), incoming, outgoing, connected });
    });

    cy.on('tap', (e) => { if (e.target === cy) setSelectedNode(null); });

    cy.on('mouseover', 'node', (e) => {
      const node = e.target;
      cy.elements().addClass('faded');
      node.removeClass('faded').addClass('highlighted');
      node.connectedEdges().removeClass('faded').addClass('edge-highlighted');
      node.neighborhood('node').removeClass('faded').addClass('highlighted');
    });

    cy.on('mouseout', 'node', () => {
      cy.elements().removeClass('faded highlighted edge-highlighted');
    });

    cy.fit(undefined, 40);
    cyRef.current = cy;

    return () => { cy.destroy(); cyRef.current = null; };
  }, [elements]);

  // Search highlight
  useEffect(() => {
    if (!cyRef.current) return;
    cyRef.current.elements().removeClass('searched');
    if (search.trim()) {
      const q = search.toLowerCase();
      cyRef.current.nodes().forEach(n => {
        if (n.id().toLowerCase().includes(q)) n.addClass('searched');
      });
      const matched = cyRef.current.nodes('.searched');
      if (matched.length) cyRef.current.animate({ fit: { eles: matched, padding: 60 } }, { duration: 300 });
    }
  }, [search]);

  const handleFit = () => cyRef.current?.fit(undefined, 40);
  const handleReset = () => { cyRef.current?.zoom(1); cyRef.current?.center(); };
  const handleExportPng = () => {
    if (!cyRef.current) return;
    const png = cyRef.current.png({ full: true, scale: 2, bg: '#0d1117' });
    const a = document.createElement('a'); a.href = png; a.download = 'architecture-graph.png'; a.click();
  };

  if (loading) return <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>Loading architecture graph...</div>;
  if (!graphData?.nodes?.length) return (
    <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)', background: 'var(--bg-secondary)', borderRadius: '1.25rem', border: '1px solid var(--border-color)' }}>
      <Network size={36} style={{ marginBottom: '0.75rem', opacity: 0.5 }} />
      <p>Run repository analysis to generate the architecture graph.</p>
    </div>
  );

  const stats = {
    controllers: graphData.nodes.filter(n => n.type === 'CONTROLLER').length,
    services: graphData.nodes.filter(n => n.type === 'SERVICE').length,
    repositories: graphData.nodes.filter(n => n.type === 'REPOSITORY').length,
    connections: graphData.edges.length
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
      {/* Stats */}
      <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
        {[
          { label: 'Controllers', value: stats.controllers, color: NODE_COLORS.CONTROLLER },
          { label: 'Services', value: stats.services, color: NODE_COLORS.SERVICE },
          { label: 'Repositories', value: stats.repositories, color: NODE_COLORS.REPOSITORY },
          { label: 'Connections', value: stats.connections, color: '#6366f1' }
        ].map((s, i) => (
          <div key={i} style={{ padding: '0.6rem 1rem', borderRadius: '0.85rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: s.color }} />
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{s.label}</span>
            <strong style={{ fontSize: '0.95rem', fontFamily: 'JetBrains Mono' }}>{s.value}</strong>
          </div>
        ))}
      </div>

      {/* Search + Controls */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)', borderRadius: '0.75rem', padding: '0.4rem 0.85rem', flex: '1', maxWidth: '320px' }}>
          <Search size={15} style={{ color: 'var(--text-muted)' }} />
          <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search nodes..." style={{ background: 'none', border: 'none', color: 'var(--text-primary)', outline: 'none', fontSize: '0.85rem', width: '100%' }} />
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button onClick={handleFit} className="btn btn-secondary" style={{ padding: '0.4rem 0.7rem', borderRadius: '0.6rem' }} title="Fit"><Maximize2 size={15} /></button>
          <button onClick={handleReset} className="btn btn-secondary" style={{ padding: '0.4rem 0.7rem', borderRadius: '0.6rem' }} title="Reset"><RotateCcw size={15} /></button>
          <button onClick={handleExportPng} className="btn btn-secondary" style={{ padding: '0.4rem 0.7rem', borderRadius: '0.6rem' }} title="Export PNG"><Download size={15} /></button>
        </div>
      </div>

      {/* Graph + Detail Panel */}
      <div style={{ display: 'flex', gap: '1rem', position: 'relative' }}>
        <div ref={containerRef} style={{ flex: 1, height: '520px', borderRadius: '1.25rem', border: '1px solid var(--border-color)', background: '#0d1117', overflow: 'hidden' }} />

        {selectedNode && (
          <div style={{ width: '280px', background: 'var(--bg-card)', borderRadius: '1.25rem', border: '1px solid var(--border-color)', padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '1rem', flexShrink: 0 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h4 style={{ fontSize: '1rem', fontWeight: 700, fontFamily: 'Space Grotesk' }}>Node Details</h4>
              <button onClick={() => setSelectedNode(null)} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}><X size={16} /></button>
            </div>
            <div style={{ padding: '0.6rem 0.85rem', borderRadius: '0.75rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginBottom: '0.2rem' }}>Class</div>
              <div style={{ fontSize: '0.9rem', fontWeight: 600, fontFamily: 'JetBrains Mono', color: 'var(--text-primary)' }}>{selectedNode.id}</div>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div style={{ padding: '0.5rem 0.75rem', borderRadius: '0.65rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>Layer</div>
                <div style={{ fontSize: '0.85rem', fontWeight: 600, color: NODE_COLORS[selectedNode.type] || '#fff' }}>{selectedNode.type}</div>
              </div>
              <div style={{ padding: '0.5rem 0.75rem', borderRadius: '0.65rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>Incoming</div>
                <div style={{ fontSize: '0.85rem', fontWeight: 700 }}>{selectedNode.incoming}</div>
              </div>
              <div style={{ padding: '0.5rem 0.75rem', borderRadius: '0.65rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>Outgoing</div>
                <div style={{ fontSize: '0.85rem', fontWeight: 700 }}>{selectedNode.outgoing}</div>
              </div>
              <div style={{ padding: '0.5rem 0.75rem', borderRadius: '0.65rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>Connections</div>
                <div style={{ fontSize: '0.85rem', fontWeight: 700 }}>{selectedNode.connected.length}</div>
              </div>
            </div>
            {selectedNode.connected.length > 0 && (
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '0.4rem' }}>Connected Classes</div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem', maxHeight: '200px', overflowY: 'auto' }}>
                  {selectedNode.connected.map(c => (
                    <div key={c} style={{ fontSize: '0.8rem', fontFamily: 'JetBrains Mono', color: 'var(--accent-teal)', padding: '0.3rem 0.6rem', borderRadius: '0.5rem', background: 'var(--bg-secondary)' }}>{c}</div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function Network({ size, style }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={style}><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/><circle cx="5" cy="12" r="1"/><circle cx="12" cy="5" r="1"/><circle cx="12" cy="19" r="1"/><line x1="12" y1="6" x2="12" y2="11"/><line x1="12" y1="13" x2="12" y2="18"/><line x1="6" y1="12" x2="11" y2="12"/><line x1="13" y1="12" x2="18" y2="12"/></svg>;
}
