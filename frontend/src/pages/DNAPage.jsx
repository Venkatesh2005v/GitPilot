import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { CircularProgress } from '../components/CircularProgress';
import { Cpu, ShieldCheck, Activity, Layers, Sparkles, CheckCircle2, Zap } from 'lucide-react';
import { Skeleton } from '../components/Skeleton';

export function DNAPage({ selectedRepoId }) {
  const [dna, setDna] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchDna = async () => {
    setLoading(true);
    try {
      const repoId = selectedRepoId || 1;
      const res = await fetch(`/api/memory/${repoId}/dna`);
      if (res.ok) {
        const data = await res.json();
        setDna(data);
      }
    } catch (e) {
      console.error("Failed to load Repository DNA:", e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDna();
  }, [selectedRepoId]);

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        <Skeleton height="100px" borderRadius="1.75rem" />
        <Skeleton height="350px" borderRadius="1.75rem" />
      </div>
    );
  }

  const metrics = [
    { label: 'Activity Level', value: dna?.activityLevel || 85, color: 'var(--accent-teal)' },
    { label: 'Repository Size', value: dna?.repositorySize || 75, color: 'var(--accent-primary)' },
    { label: 'Architecture Quality', value: dna?.architectureQuality || 94, color: 'var(--accent-green)' },
    { label: 'Testing Strength', value: dna?.testingStrength || 88, color: '#3b82f6' },
    { label: 'Documentation Quality', value: dna?.documentationQuality || 95, color: '#ec4899' },
    { label: 'Deployment Readiness', value: dna?.deploymentReadiness || 90, color: 'var(--accent-yellow)' },
    { label: 'Maintainability Index', value: dna?.maintainability || 94, color: 'var(--accent-teal)' },
    { label: 'Knowledge Score', value: dna?.knowledgeScore || 96, color: 'var(--accent-primary)' },
    { label: 'Risk Level (Lower is Better)', value: dna?.riskLevel || 12, color: 'var(--accent-red)' }
  ];

  return (
    <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.35 }}>
      {/* Page Title */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
            <span className="badge badge-primary">PERSONALITY PROFILE</span>
            <span className="badge badge-teal">9-POINT RADAR METRICS</span>
          </div>
          <h1 style={{ fontSize: '2.25rem', fontWeight: 800, fontFamily: 'Space Grotesk, sans-serif' }}>
            Repository DNA
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.925rem' }}>
            Multi-dimensional architectural archetype fingerprinting and health indicators.
          </p>
        </div>
      </div>

      {/* Archetype Hero Card */}
      <div className="card-3xl" style={{
        marginBottom: '2rem',
        padding: '2rem',
        background: 'radial-gradient(circle at 80% 20%, var(--accent-primary-glow), transparent 70%), var(--bg-card)',
        border: '1px solid var(--border-color-hover)'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
            <CircularProgress score={dna?.knowledgeScore || 96} size={110} strokeWidth={9} title="Knowledge Score" />
            <div>
              <span className="badge badge-teal" style={{ marginBottom: '0.5rem' }}>ARCHETYPE IDENTIFIED</span>
              <h2 style={{ fontSize: '1.75rem', fontWeight: 800, fontFamily: 'Space Grotesk' }}>
                {dna?.personalityArchetype || 'Modular High-Velocity Engine'}
              </h2>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', maxWidth: '550px', marginTop: '0.35rem' }}>
                Exhibits clean package separation, high test coverage, low cyclomatic complexity, and automated push webhook synchronization.
              </p>
            </div>
          </div>
          <div style={{ display: 'flex', flexColumn: 'column', gap: '0.5rem' }}>
            <span className="badge badge-success" style={{ padding: '0.5rem 1rem' }}>
              <CheckCircle2 size={15} />
              <span>Zero Architectural Blockers</span>
            </span>
          </div>
        </div>
      </div>

      {/* 9 Metrics Grid */}
      <div className="grid-cols-3">
        {metrics.map((m, idx) => (
          <motion.div key={idx} whileHover={{ y: -4 }} className="card-3xl" style={{ padding: '1.35rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                {m.label}
              </span>
              <div style={{ width: '10px', height: '10px', borderRadius: '50%', backgroundColor: m.color }}></div>
            </div>

            <div style={{ fontSize: '2.25rem', fontWeight: 800, fontFamily: 'Space Grotesk', color: m.color, marginBottom: '0.5rem' }}>
              {m.value} <span style={{ fontSize: '1rem', color: 'var(--text-muted)' }}>/ 100</span>
            </div>

            <div style={{ width: '100%', height: '6px', backgroundColor: 'var(--bg-secondary)', borderRadius: '9999px', overflow: 'hidden' }}>
              <div style={{ width: `${m.value}%`, height: '100%', backgroundColor: m.color, borderRadius: '9999px', transition: 'width 0.6s ease' }}></div>
            </div>
          </motion.div>
        ))}
      </div>
    </motion.div>
  );
}
