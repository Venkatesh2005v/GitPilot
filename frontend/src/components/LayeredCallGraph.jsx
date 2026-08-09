import React, { useState, useEffect, useCallback } from 'react';
import ReactFlow, { Background, Controls, useNodesState, useEdgesState, MarkerType } from 'reactflow';
import 'reactflow/dist/style.css';

const NODE_COLORS = {
  CONTROLLER: '#8b5cf6',
  SERVICE: '#3b82f6',
  REPOSITORY: '#10b981',
};

const LAYER_LABELS = ['Controllers', 'Services', 'Repositories'];

function CustomNode({ data }) {
  return (
    <div style={{
      padding: '0.6rem 1.1rem',
      borderRadius: '0.75rem',
      background: data.color + '22',
      border: `2px solid ${data.color}`,
      color: '#fff',
      fontSize: '0.8rem',
      fontWeight: 600,
      fontFamily: 'JetBrains Mono, monospace',
      minWidth: '140px',
      textAlign: 'center'
    }}>
      <div style={{ fontSize: '0.6rem', textTransform: 'uppercase', opacity: 0.7, marginBottom: '0.2rem' }}>{data.type}</div>
      <div>{data.label}</div>
    </div>
  );
}

const nodeTypes = { custom: CustomNode };

export function LayeredCallGraph({ repositoryId }) {
  const [graphData, setGraphData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);

  useEffect(() => {
    if (!repositoryId) return;
    setLoading(true);
    fetch(`/architecture/repositories/${repositoryId}/callgraph`)
      .then(res => res.ok ? res.json() : null)
      .then(data => {
        setGraphData(data);
        if (data) buildGraph(data);
      })
      .catch(() => setGraphData(null))
      .finally(() => setLoading(false));
  }, [repositoryId]);

  const buildGraph = useCallback((data) => {
    const { nodes: apiNodes, edges: apiEdges } = data;

    // Group by layer
    const layers = { 0: [], 1: [], 2: [] };
    apiNodes.forEach(n => {
      const l = n.layer ?? 1;
      if (!layers[l]) layers[l] = [];
      layers[l].push(n);
    });

    const X_SPACING = 220;
    const Y_SPACING = 160;

    const flowNodes = [];
    Object.entries(layers).forEach(([layer, group]) => {
      const y = parseInt(layer) * Y_SPACING;
      const startX = -(group.length - 1) * X_SPACING / 2;
      group.forEach((n, i) => {
        flowNodes.push({
          id: n.id,
          type: 'custom',
          position: { x: startX + i * X_SPACING, y },
          data: { label: n.id, type: n.type, color: NODE_COLORS[n.type] || '#6b7280' }
        });
      });
    });

    const flowEdges = apiEdges.map((e, i) => ({
      id: `e-${i}`,
      source: e.from,
      target: e.to,
      animated: true,
      style: { stroke: '#6366f1', strokeWidth: 1.5 },
      markerEnd: { type: MarkerType.ArrowClosed, color: '#6366f1' }
    }));

    setNodes(flowNodes);
    setEdges(flowEdges);
  }, []);

  if (loading) {
    return <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>Loading call graph...</div>;
  }

  if (!graphData || !graphData.nodes?.length) {
    return <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>No architecture data available for this repository.</div>;
  }

  const stats = {
    controllers: graphData.nodes.filter(n => n.type === 'CONTROLLER').length,
    services: graphData.nodes.filter(n => n.type === 'SERVICE').length,
    repositories: graphData.nodes.filter(n => n.type === 'REPOSITORY').length,
    dependencies: graphData.edges.length,
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
      {/* Stats Header */}
      <div style={{ display: 'flex', gap: '1.25rem', flexWrap: 'wrap' }}>
        {[
          { label: 'Controllers', value: stats.controllers, color: NODE_COLORS.CONTROLLER },
          { label: 'Services', value: stats.services, color: NODE_COLORS.SERVICE },
          { label: 'Repositories', value: stats.repositories, color: NODE_COLORS.REPOSITORY },
          { label: 'Dependencies', value: stats.dependencies, color: '#6366f1' },
        ].map((s, i) => (
          <div key={i} style={{ padding: '0.75rem 1.25rem', borderRadius: '1rem', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div style={{ width: '10px', height: '10px', borderRadius: '50%', background: s.color }} />
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>{s.label}:</span>
            <strong style={{ fontSize: '1rem', color: 'var(--text-primary)', fontFamily: 'JetBrains Mono' }}>{s.value}</strong>
          </div>
        ))}
      </div>

      {/* React Flow Graph */}
      <div style={{ height: '500px', borderRadius: '1.25rem', border: '1px solid var(--border-color)', overflow: 'hidden', background: '#0d1117' }}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          nodeTypes={nodeTypes}
          fitView
          attributionPosition="bottom-left"
          style={{ background: '#0d1117' }}
        >
          <Background color="#1e293b" gap={20} />
          <Controls style={{ background: '#1e293b', borderColor: '#334155' }} />
        </ReactFlow>
      </div>
    </div>
  );
}
