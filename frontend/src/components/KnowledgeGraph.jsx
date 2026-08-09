import React, { useState } from 'react';

export function KnowledgeGraph({ nodes = [], edges = [] }) {
  const [selectedNode, setSelectedNode] = useState(null);

  // Position nodes in a clean circular layout
  const width = 800;
  const height = 450;
  const centerX = width / 2;
  const centerY = height / 2;
  const radius = 170;

  const positions = {};
  nodes.forEach((node, idx) => {
    const angle = (idx / (nodes.length || 1)) * 2 * Math.PI - Math.PI / 2;
    positions[node.nodeKey] = {
      x: centerX + radius * Math.cos(angle),
      y: centerY + radius * Math.sin(angle)
    };
  });

  const getCategoryColor = (category) => {
    switch ((category || '').toUpperCase()) {
      case 'SECURITY': return 'var(--accent-red, #ef4444)';
      case 'INTEGRATION': return 'var(--accent-teal, #14b8a6)';
      case 'CORE': return 'var(--accent-primary, #8b5cf6)';
      case 'EVENT': return 'var(--accent-yellow, #f59e0b)';
      case 'AI': return '#ec4899';
      case 'DATABASE': return '#3b82f6';
      default: return 'var(--accent-primary, #8b5cf6)';
    }
  };

  return (
    <div style={{ width: '100%', position: 'relative', overflow: 'hidden' }}>
      <div style={{
        position: 'relative',
        width: '100%',
        height: `${height}px`,
        backgroundColor: 'var(--bg-secondary)',
        borderRadius: '1.5rem',
        border: '1px solid var(--border-color)',
        boxShadow: 'inset 0 2px 8px rgba(0,0,0,0.1)'
      }}>
        <svg width="100%" height="100%" viewBox={`0 0 ${width} ${height}`}>
          <defs>
            <marker
              id="arrow"
              viewBox="0 0 10 10"
              refX="22"
              refY="5"
              markerWidth="6"
              markerHeight="6"
              orient="auto-start-reverse"
            >
              <path d="M 0 0 L 10 5 L 0 10 z" fill="var(--border-color-hover)" />
            </marker>
          </defs>

          {/* Render Edge Connections */}
          {edges.map((edge, idx) => {
            const source = positions[edge.sourceNodeKey];
            const target = positions[edge.targetNodeKey];
            if (!source || !target) return null;

            return (
              <g key={edge.id || idx}>
                <line
                  x1={source.x}
                  y1={source.y}
                  x2={target.x}
                  y2={target.y}
                  stroke="var(--border-color-hover)"
                  strokeWidth="2"
                  strokeDasharray={edge.relationshipType === 'DEPENDS_ON' ? '4,4' : 'none'}
                  markerEnd="url(#arrow)"
                />
                <text
                  x={(source.x + target.x) / 2}
                  y={(source.y + target.y) / 2 - 6}
                  fill="var(--text-muted)"
                  fontSize="10"
                  fontFamily="JetBrains Mono"
                  textAnchor="middle"
                >
                  {edge.label}
                </text>
              </g>
            );
          })}

          {/* Render Module Nodes */}
          {nodes.map((node) => {
            const pos = positions[node.nodeKey];
            if (!pos) return null;
            const color = getCategoryColor(node.category);
            const isSelected = selectedNode?.nodeKey === node.nodeKey;

            return (
              <g
                key={node.nodeKey}
                transform={`translate(${pos.x}, ${pos.y})`}
                onClick={() => setSelectedNode(node)}
                style={{ cursor: 'pointer' }}
              >
                <circle
                  r={isSelected ? 26 : 22}
                  fill="var(--bg-card-solid)"
                  stroke={color}
                  strokeWidth={isSelected ? 4 : 2}
                  style={{ transition: 'all 0.2s ease', filter: isSelected ? `drop-shadow(0 0 12px ${color})` : 'none' }}
                />
                <circle r={8} fill={color} />
                <text
                  y={38}
                  fill="var(--text-primary)"
                  fontSize="11"
                  fontWeight="600"
                  fontFamily="Inter, sans-serif"
                  textAnchor="middle"
                >
                  {node.name}
                </text>
              </g>
            );
          })}
        </svg>
      </div>

      {/* Selected Node Details Card */}
      {selectedNode && (
        <div style={{
          marginTop: '1.25rem',
          padding: '1.25rem',
          backgroundColor: 'var(--bg-card)',
          borderRadius: '1.25rem',
          border: '1px solid var(--border-color-hover)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.35rem' }}>
              <span className="badge" style={{ backgroundColor: getCategoryColor(selectedNode.category) + '22', color: getCategoryColor(selectedNode.category) }}>
                {selectedNode.category}
              </span>
              <strong style={{ fontSize: '1.05rem' }}>{selectedNode.name}</strong>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>({selectedNode.nodeKey})</span>
            </div>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>{selectedNode.description}</p>
          </div>
          <div style={{ textAlign: 'right' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Complexity Score</span>
            <strong style={{ fontSize: '1.35rem', color: 'var(--accent-primary)', fontFamily: 'JetBrains Mono' }}>
              {selectedNode.complexityScore}/100
            </strong>
          </div>
        </div>
      )}
    </div>
  );
}
