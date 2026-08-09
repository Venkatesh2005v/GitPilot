import React from 'react';
import { Code2, Server, Database, Container, Shield, Cpu, Layers } from 'lucide-react';

export function TechPill({ name, category }) {
  const getTechMeta = (techName) => {
    const nameLower = (techName || '').toLowerCase();

    if (nameLower.includes('spring')) {
      return { bg: 'rgba(16, 185, 129, 0.12)', color: '#10b981', border: 'rgba(16, 185, 129, 0.25)', icon: Server };
    }
    if (nameLower.includes('react')) {
      return { bg: 'rgba(56, 189, 248, 0.12)', color: '#38bdf8', border: 'rgba(56, 189, 248, 0.25)', icon: Code2 };
    }
    if (nameLower.includes('docker')) {
      return { bg: 'rgba(2, 132, 199, 0.12)', color: '#0284c7', border: 'rgba(2, 132, 199, 0.25)', icon: Container };
    }
    if (nameLower.includes('postgres') || nameLower.includes('mysql') || nameLower.includes('mongo')) {
      return { bg: 'rgba(99, 102, 241, 0.12)', color: '#6366f1', border: 'rgba(99, 102, 241, 0.25)', icon: Database };
    }
    if (nameLower.includes('oauth') || nameLower.includes('jwt') || nameLower.includes('security')) {
      return { bg: 'rgba(168, 85, 247, 0.12)', color: '#a855f7', border: 'rgba(168, 85, 247, 0.25)', icon: Shield };
    }
    if (nameLower.includes('flyway') || nameLower.includes('hibernate')) {
      return { bg: 'rgba(245, 158, 11, 0.12)', color: '#f59e0b', border: 'rgba(245, 158, 11, 0.25)', icon: Layers };
    }

    return { bg: 'var(--accent-primary-bg)', color: 'var(--accent-primary)', border: 'var(--border-color-hover)', icon: Cpu };
  };

  const meta = getTechMeta(name);
  const Icon = meta.icon;

  return (
    <span
      className="tech-pill"
      style={{
        backgroundColor: meta.bg,
        color: meta.color,
        borderColor: meta.border,
        padding: '0.4rem 0.85rem',
        fontSize: '0.8125rem',
        fontWeight: 600,
        fontFamily: 'Inter, sans-serif'
      }}
    >
      <Icon size={14} />
      <span>{name}</span>
    </span>
  );
}
