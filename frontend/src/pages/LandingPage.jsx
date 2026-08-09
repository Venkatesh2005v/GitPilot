import React, { useState } from 'react';
import { motion } from 'framer-motion';
import {
  Sparkles,
  Github,
  ArrowRight,
  Zap,
  ShieldCheck,
  Cpu,
  Terminal,
  CheckCircle2,
  TrendingUp,
  Activity,
  Code2,
  FileText,
  Lock,
  ChevronRight
} from 'lucide-react';
import { ThemeToggle } from '../components/ThemeToggle';

export function LandingPage() {
  const [activeTab, setActiveTab] = useState('health');

  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.12
      }
    }
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: [0.16, 1, 0.3, 1] } }
  };

  return (
    <div style={{ minHeight: '100vh', backgroundColor: 'var(--bg-app)', color: 'var(--text-primary)', overflowX: 'hidden' }}>

      {/* Top Navbar */}
      <header style={{
        position: 'sticky',
        top: 0,
        zIndex: 90,
        backdropFilter: 'blur(20px)',
        WebkitBackdropFilter: 'blur(20px)',
        backgroundColor: 'var(--bg-topnav)',
        borderBottom: '1px solid var(--border-color)',
        padding: '1rem 2rem'
      }}>
        <div style={{ maxWidth: '1280px', margin: '0 auto', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          {/* Logo */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div style={{
              width: '38px',
              height: '38px',
              borderRadius: '1rem',
              background: 'linear-gradient(135deg, var(--accent-primary), #7c3aed)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 4px 15px var(--accent-primary-glow)'
            }}>
              <Sparkles size={20} style={{ color: '#fff' }} />
            </div>
            <span style={{ fontFamily: 'Space Grotesk, sans-serif', fontWeight: 800, fontSize: '1.35rem', letterSpacing: '-0.02em', color: 'var(--text-primary)' }}>
              Git<span style={{ color: 'var(--accent-primary)' }}>Pilot</span>
            </span>
          </div>

          {/* Navigation Links */}
          <nav style={{ display: 'flex', alignItems: 'center', gap: '2rem' }} className="hidden-mobile">
            <a href="#features" style={{ color: 'var(--text-secondary)', textDecoration: 'none', fontSize: '0.9rem', fontWeight: 500 }}>Features</a>
            <a href="#workflow" style={{ color: 'var(--text-secondary)', textDecoration: 'none', fontSize: '0.9rem', fontWeight: 500 }}>Workflow</a>
            <a href="#insights" style={{ color: 'var(--text-secondary)', textDecoration: 'none', fontSize: '0.9rem', fontWeight: 500 }}>Insights</a>
            <a href="#intelligence" style={{ color: 'var(--text-secondary)', textDecoration: 'none', fontSize: '0.9rem', fontWeight: 500 }}>AI Intelligence</a>
          </nav>

          {/* Header Action Buttons */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <ThemeToggle />

            <button
              onClick={() => window.location.href = '/oauth2/authorization/github'}
              className="btn btn-primary"
              style={{ fontSize: '0.85rem' }}
            >
              <Github size={16} />
              <span>Sign In with GitHub</span>
            </button>
          </div>
        </div>
      </header>

      {/* 1. Hero Section */}
      <section style={{ position: 'relative', padding: '6rem 2rem 4rem 2rem', maxWidth: '1280px', margin: '0 auto', textAlign: 'center' }}>
        {/* Soft Ambient Background Glows */}
        <div className="bg-glow-purple" style={{ top: '-100px', left: '50%', transform: 'translateX(-50%)' }}></div>
        <div className="bg-glow-teal" style={{ top: '200px', right: '10%' }}></div>

        <motion.div
          initial="hidden"
          animate="visible"
          variants={containerVariants}
          style={{ position: 'relative', zIndex: 1 }}
        >
          {/* Eyebrow Badge */}
          <motion.div variants={itemVariants} style={{ display: 'inline-flex', marginBottom: '1.5rem' }}>
            <span className="badge badge-primary" style={{ padding: '0.5rem 1rem', fontSize: '0.825rem', gap: '0.5rem' }}>
              <Sparkles size={14} />
              <span>GITPILOT 2.0 • AI-POWERED REPOSITORY TELEMETRY</span>
              <span className="badge badge-teal" style={{ fontSize: '0.7rem', padding: '0.15rem 0.5rem' }}>NEW</span>
            </span>
          </motion.div>

          {/* Headline */}
          <motion.h1
            variants={itemVariants}
            style={{
              fontSize: 'clamp(2.5rem, 5.5vw, 4.25rem)',
              fontWeight: 800,
              lineHeight: 1.1,
              letterSpacing: '-0.03em',
              marginBottom: '1.5rem',
              maxWidth: '900px',
              margin: '0 auto 1.5rem auto'
            }}
          >
            Navigate Repository Intelligence with <span style={{
              background: 'linear-gradient(135deg, var(--accent-primary), var(--accent-teal))',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent'
            }}>AI Precision</span>
          </motion.h1>

          {/* Subtitle */}
          <motion.p
            variants={itemVariants}
            style={{
              fontSize: '1.15rem',
              color: 'var(--text-secondary)',
              maxWidth: '680px',
              margin: '0 auto 2.5rem auto',
              lineHeight: 1.6
            }}
          >
            Real-time push telemetry, automated code health scoring, technology stack detection, and actionable AI recommendations for your engineering workflow.
          </motion.p>

          {/* CTA Group */}
          <motion.div
            variants={itemVariants}
            style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap', marginBottom: '4rem' }}
          >
            <button
              onClick={() => window.location.href = '/oauth2/authorization/github'}
              className="btn btn-primary"
              style={{ padding: '0.9rem 2rem', fontSize: '1rem', borderRadius: '1.25rem' }}
            >
              <Github size={20} />
              <span>Sign in with GitHub</span>
              <ArrowRight size={18} style={{ marginLeft: '0.25rem' }} />
            </button>
          </motion.div>

          {/* Hero Product Teaser / Mockup */}
          <motion.div
            variants={itemVariants}
            className="card-3xl"
            style={{
              maxWidth: '1100px',
              margin: '0 auto',
              padding: '1.5rem',
              boxShadow: 'var(--shadow-lg), var(--shadow-glow)',
              border: '1px solid var(--border-color-hover)',
              position: 'relative'
            }}
          >
            {/* Mock Header Controls */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '1rem', marginBottom: '1.5rem' }}>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <div style={{ width: '12px', height: '12px', borderRadius: '50%', backgroundColor: '#ef4444' }}></div>
                <div style={{ width: '12px', height: '12px', borderRadius: '50%', backgroundColor: '#f59e0b' }}></div>
                <div style={{ width: '12px', height: '12px', borderRadius: '50%', backgroundColor: '#10b981' }}></div>
              </div>
              <div style={{ fontSize: '0.8rem', fontFamily: 'JetBrains Mono', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Lock size={12} />
                <span>gitpilot.internal/dashboard/telemetry</span>
              </div>
              <span className="badge badge-teal" style={{ fontSize: '0.75rem' }}>
                <Activity size={12} />
                <span>LIVE SYNC ACTIVE</span>
              </span>
            </div>

            {/* Inner Dashboard Preview Content */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.25rem', textAlign: 'left' }}>

              {/* Teaser Metric Card 1 */}
              <div style={{ padding: '1.25rem', background: 'var(--bg-secondary)', borderRadius: '1.25rem', border: '1px solid var(--border-color)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>OVERALL REPOSITORY HEALTH</span>
                  <Sparkles size={16} style={{ color: 'var(--accent-primary)' }} />
                </div>
                <div style={{ fontSize: '2rem', fontWeight: 800, fontFamily: 'Space Grotesk', color: 'var(--text-primary)' }}>
                  94<span style={{ fontSize: '1rem', color: 'var(--text-muted)' }}>/100</span>
                </div>
                <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
                  <span className="badge badge-success" style={{ fontSize: '0.7rem' }}>+6 pts this week</span>
                  <span className="badge badge-teal" style={{ fontSize: '0.7rem' }}>A+ Grade</span>
                </div>
              </div>

              {/* Teaser Metric Card 2 */}
              <div style={{ padding: '1.25rem', background: 'var(--bg-secondary)', borderRadius: '1.25rem', border: '1px solid var(--border-color)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>PUSH EVENT TELEMETRY</span>
                  <Zap size={16} style={{ color: 'var(--accent-teal)' }} />
                </div>
                <div style={{ fontSize: '1.4rem', fontWeight: 700, fontFamily: 'Space Grotesk', color: 'var(--text-primary)' }}>
                  main branch updated
                </div>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
                  Commit <code>a8f93e1</code> by @dev-lead • 12 mins ago
                </p>
              </div>

              {/* Teaser Metric Card 3 */}
              <div style={{ padding: '1.25rem', background: 'var(--bg-secondary)', borderRadius: '1.25rem', border: '1px solid var(--border-color)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>AI RECOMMENDATIONS</span>
                  <ShieldCheck size={16} style={{ color: 'var(--accent-green)' }} />
                </div>
                <div style={{ fontSize: '1.4rem', fontWeight: 700, fontFamily: 'Space Grotesk', color: 'var(--accent-primary)' }}>
                  3 Actions Pending
                </div>
                <span className="badge badge-primary" style={{ fontSize: '0.7rem', marginTop: '0.5rem' }}>
                  Add database index on commit_hash
                </span>
              </div>
            </div>
          </motion.div>
        </motion.div>
      </section>

      {/* 2. Feature Highlights Section */}
      <section id="features" style={{ padding: '5rem 2rem', maxWidth: '1280px', margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: '3.5rem' }}>
          <span className="badge badge-primary" style={{ marginBottom: '1rem' }}>ENTERPRISE-GRADE CAPABILITIES</span>
          <h2 style={{ fontSize: '2.5rem', fontWeight: 800, marginBottom: '1rem' }}>Built for High-Velocity Engineering Teams</h2>
          <p style={{ color: 'var(--text-secondary)', maxWidth: '600px', margin: '0 auto', fontSize: '1rem' }}>
            GitPilot transforms raw code updates into deep architectural intelligence and continuous repository insights.
          </p>
        </div>

        <div className="grid-cols-3">
          {[
            {
              icon: Zap,
              color: 'var(--accent-primary)',
              bg: 'var(--accent-primary-bg)',
              title: 'Real-Time Webhook Sync',
              desc: 'Instant push event indexing via GitHub Webhooks. Changes are processed in sub-seconds without polling delays.'
            },
            {
              icon: Cpu,
              color: 'var(--accent-teal)',
              bg: 'var(--accent-teal-bg)',
              title: 'AI Code Health Scoring',
              desc: 'Algorithmic health scores measuring maintainability, security risks, documentation completeness, and test coverage.'
            },
            {
              icon: Code2,
              color: 'var(--accent-green)',
              bg: 'var(--accent-green-bg)',
              title: 'Tech Stack Fingerprinting',
              desc: 'Automated technology detection scanning languages, frameworks, ORMs, and build tools across every repository.'
            },
            {
              icon: TrendingUp,
              color: 'var(--accent-indigo)',
              bg: 'var(--accent-indigo-bg)',
              title: 'Commit Impact Analysis',
              desc: 'Track commit velocity, line diff ratios, and contributor distributions with interactive timeline charts.'
            },
            {
              icon: FileText,
              color: 'var(--accent-yellow)',
              bg: 'var(--accent-yellow-bg)',
              title: 'Automated README Summaries',
              desc: 'LLM-generated repository overviews that synthesize core purpose, entry points, and dependency trees automatically.'
            },
            {
              icon: ShieldCheck,
              color: 'var(--accent-primary)',
              bg: 'var(--accent-primary-bg)',
              title: 'Prioritized AI Recommendations',
              desc: 'Ranked, actionable suggestions for refactoring, security hardening, performance indexing, and code hygiene.'
            }
          ].map((feature, idx) => {
            const Icon = feature.icon;
            return (
              <motion.div
                key={idx}
                whileHover={{ y: -6, transition: { duration: 0.2 } }}
                className="card-3xl"
              >
                <div style={{
                  width: '48px',
                  height: '48px',
                  borderRadius: '1.25rem',
                  backgroundColor: feature.bg,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '1.25rem'
                }}>
                  <Icon size={24} style={{ color: feature.color }} />
                </div>
                <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.65rem' }}>{feature.title}</h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: 1.6 }}>{feature.desc}</p>
              </motion.div>
            );
          })}
        </div>
      </section>

      {/* 3. GitHub Integration Workflow */}
      <section id="workflow" style={{ padding: '5rem 2rem', background: 'var(--bg-secondary)', borderTop: '1px solid var(--border-color)', borderBottom: '1px solid var(--border-color)' }}>
        <div style={{ maxWidth: '1280px', margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '3.5rem' }}>
            <span className="badge badge-teal" style={{ marginBottom: '1rem' }}>SEAMLESS WORKFLOW</span>
            <h2 style={{ fontSize: '2.5rem', fontWeight: 800, marginBottom: '1rem' }}>From Git Push to AI Intelligence in Seconds</h2>
            <p style={{ color: 'var(--text-secondary)', maxWidth: '600px', margin: '0 auto', fontSize: '1rem' }}>
              Connect your repositories once and let GitPilot handle continuous analysis automatically.
            </p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '2rem', marginBottom: '3rem' }}>
            {[
              {
                step: '01',
                title: 'Connect GitHub Repository',
                desc: 'Authorize with 1-click GitHub OAuth. Select specific repos or entire organization workspaces.'
              },
              {
                step: '02',
                title: 'Automated Push Webhooks',
                desc: 'GitPilot listens for push events, instantly fetching commit diffs, tree metadata, and code structure.'
              },
              {
                step: '03',
                title: 'AI Deep Intelligence',
                desc: 'LLM engines analyze repository health, generate architectural summaries, and calculate actionable scores.'
              }
            ].map((st, i) => (
              <div key={i} className="card-3xl" style={{ position: 'relative' }}>
                <span style={{
                  fontSize: '3rem',
                  fontWeight: 800,
                  fontFamily: 'Space Grotesk',
                  color: 'var(--accent-primary-bg)',
                  position: 'absolute',
                  top: '1.25rem',
                  right: '1.5rem'
                }}>
                  {st.step}
                </span>
                <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.75rem', marginTop: '0.5rem' }}>{st.title}</h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>{st.desc}</p>
              </div>
            ))}
          </div>

          {/* Simulated Webhook Code Window */}
          <div className="card-3xl" style={{ background: '#090b10', padding: '1.5rem', border: '1px solid var(--border-color)' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Terminal size={16} style={{ color: 'var(--accent-teal)' }} />
                <span style={{ fontSize: '0.85rem', fontFamily: 'JetBrains Mono', color: 'var(--text-secondary)' }}>webhook-listener.log</span>
              </div>
              <span className="badge badge-success" style={{ fontSize: '0.7rem' }}>SYNC SUCCESS</span>
            </div>
            <pre style={{ fontSize: '0.825rem', fontFamily: 'JetBrains Mono', color: 'var(--accent-teal)', overflowX: 'auto', lineHeight: 1.7 }}>
              {`[2026-07-26T09:24:00Z] POST /api/v1/webhooks/github -> HTTP 200 OK
[EVENT] push payload received: repository="Venkatesh2005v/GitPilot", branch="main", commits=3
[ANALYSIS] Indexing commit #a8f93e1 (refactor: enhance repository intelligence DTO)
[AI ENGINE] Deep scan completed in 142ms. Health score recalculated: 94/100
[INTELLIGENCE] Detected stack: [Java, Spring Boot, React, Vite, Postgres, Docker]
[READY] Telemetry broadcasted to live developer dashboard.`}
            </pre>
          </div>
        </div>
      </section>

      {/* 4. GitPilot Insights Interactive Preview */}
      <section id="insights" style={{ padding: '5rem 2rem', maxWidth: '1280px', margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: '3rem' }}>
          <span className="badge badge-primary" style={{ marginBottom: '1rem' }}>INTERACTIVE PREVIEW</span>
          <h2 style={{ fontSize: '2.5rem', fontWeight: 800, marginBottom: '1rem' }}>Explore GitPilot Telemetry</h2>
          <p style={{ color: 'var(--text-secondary)', maxWidth: '600px', margin: '0 auto', fontSize: '1rem' }}>
            Switch tabs below to inspect how GitPilot visualizes repository health, risk metrics, and architecture.
          </p>
        </div>

        {/* Tab Controls */}
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '2rem' }}>
          <div className="tabs-container">
            <button
              className={`tab-btn ${activeTab === 'health' ? 'active' : ''}`}
              onClick={() => setActiveTab('health')}
            >
              Repository Health Score
            </button>
            <button
              className={`tab-btn ${activeTab === 'stack' ? 'active' : ''}`}
              onClick={() => setActiveTab('stack')}
            >
              Tech Stack Detection
            </button>
            <button
              className={`tab-btn ${activeTab === 'recommendations' ? 'active' : ''}`}
              onClick={() => setActiveTab('recommendations')}
            >
              AI Recommendations
            </button>
          </div>
        </div>

        {/* Active Tab Preview Display */}
        <div className="card-3xl" style={{ padding: '2rem' }}>
          {activeTab === 'health' && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.3 }} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '2rem', alignItems: 'center' }}>
              <div>
                <span className="badge badge-primary" style={{ marginBottom: '0.75rem' }}>HEALTH RADAR</span>
                <h3 style={{ fontSize: '1.75rem', fontWeight: 800, marginBottom: '1rem' }}>Comprehensive Quality Matrix</h3>
                <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem', fontSize: '0.95rem' }}>
                  Evaluates test coverage, code duplication, maintainability index, and dependency vulnerability alerts on every commit.
                </p>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {[
                    { label: 'Security & Vulnerabilities', score: '100/100', color: 'var(--accent-green)' },
                    { label: 'Maintainability & Debt', score: '92/100', color: 'var(--accent-primary)' },
                    { label: 'Documentation & Readme', score: '95/100', color: 'var(--accent-teal)' }
                  ].map((m, i) => (
                    <div key={i} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.75rem 1rem', background: 'var(--bg-secondary)', borderRadius: '1rem' }}>
                      <span style={{ fontSize: '0.875rem', fontWeight: 500 }}>{m.label}</span>
                      <strong style={{ color: m.color, fontFamily: 'JetBrains Mono' }}>{m.score}</strong>
                    </div>
                  ))}
                </div>
              </div>
              <div style={{ textAlign: 'center', padding: '2rem', background: 'var(--bg-secondary)', borderRadius: '1.5rem', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '3.5rem', fontWeight: 800, fontFamily: 'Space Grotesk', color: 'var(--accent-primary)' }}>94</div>
                <span style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', display: 'block', marginBottom: '1rem' }}>Aggregated Health Index</span>
                <span className="badge badge-success">EXCELLENT CONDITION</span>
              </div>
            </motion.div>
          )}

          {activeTab === 'stack' && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.3 }}>
              <span className="badge badge-teal" style={{ marginBottom: '0.75rem' }}>AUTOMATED SCAN</span>
              <h3 style={{ fontSize: '1.5rem', fontWeight: 800, marginBottom: '1rem' }}>Detected Technologies</h3>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem' }}>
                {[
                  { name: 'Java 21', confidence: '98%', category: 'Backend' },
                  { name: 'Spring Boot 3', confidence: '95%', category: 'Framework' },
                  { name: 'React 18', confidence: '96%', category: 'Frontend' },
                  { name: 'Vite', confidence: '92%', category: 'Build Tool' },
                  { name: 'PostgreSQL', confidence: '90%', category: 'Database' },
                  { name: 'Docker', confidence: '88%', category: 'DevOps' }
                ].map((tech, i) => (
                  <div key={i} className="tech-pill tech-pill-primary" style={{ padding: '0.6rem 1.1rem', fontSize: '0.85rem' }}>
                    <strong>{tech.name}</strong>
                    <span style={{ opacity: 0.7, fontSize: '0.75rem', fontFamily: 'JetBrains Mono' }}>• {tech.confidence}</span>
                  </div>
                ))}
              </div>
            </motion.div>
          )}

          {activeTab === 'recommendations' && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.3 }} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {[
                { title: 'Add database index on repository_id in commits table', impact: 'HIGH IMPACT', desc: 'Accelerates query execution time by up to 64% during commit history filtering.' },
                { title: 'Upgrade Spring Boot dependency patch version', impact: 'SECURITY', desc: 'Resolves minor CVE advisory in upstream HTTP client library.' },
                { title: 'Enable Gzip compression on static frontend assets', impact: 'PERFORMANCE', desc: 'Reduces initial page payload size for dashboard visitors.' }
              ].map((rec, i) => (
                <div key={i} style={{ padding: '1rem 1.25rem', background: 'var(--bg-secondary)', borderRadius: '1.15rem', border: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginBottom: '0.35rem' }}>
                      <strong style={{ fontSize: '0.95rem' }}>{rec.title}</strong>
                      <span className="badge badge-primary" style={{ fontSize: '0.7rem' }}>{rec.impact}</span>
                    </div>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>{rec.desc}</p>
                  </div>
                  <ChevronRight size={18} style={{ color: 'var(--accent-primary)', flexShrink: 0 }} />
                </div>
              ))}
            </motion.div>
          )}
        </div>
      </section>

      {/* 5. AI-Powered Repository Intelligence Section */}
      <section id="intelligence" style={{ padding: '5rem 2rem', background: 'var(--bg-secondary)', borderTop: '1px solid var(--border-color)', borderBottom: '1px solid var(--border-color)' }}>
        <div style={{ maxWidth: '1280px', margin: '0 auto', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '3rem', alignItems: 'center' }}>
          <div>
            <span className="badge badge-primary" style={{ marginBottom: '1rem' }}>AI CORE ENGINE</span>
            <h2 style={{ fontSize: '2.5rem', fontWeight: 800, marginBottom: '1.25rem', lineHeight: 1.2 }}>
              Intelligent Repository Insights at Your Fingertips
            </h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '1rem', marginBottom: '1.75rem', lineHeight: 1.6 }}>
              GitPilot continuously ingests repository commits, pull requests, and file trees to deliver real-time architectural clarity and proactive refactoring guidance.
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {[
                'Automated LLM README generation & summaries',
                'Predictive code smell detection before release',
                'Contributor activity & commit velocity charts',
                'Zero-overhead background webhook processing'
              ].map((point, i) => (
                <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <CheckCircle2 size={18} style={{ color: 'var(--accent-teal)' }} />
                  <span style={{ fontSize: '0.95rem', fontWeight: 500 }}>{point}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="card-3xl" style={{ padding: '2rem', background: 'var(--bg-card-solid)', border: '1px solid var(--border-color-hover)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
              <Sparkles size={24} style={{ color: 'var(--accent-primary)' }} />
              <div>
                <h4 style={{ fontSize: '1.1rem', fontWeight: 700 }}>AI Intelligence Summary</h4>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Generated for GitPilot Core</span>
              </div>
            </div>
            <div style={{ background: 'var(--bg-secondary)', padding: '1rem', borderRadius: '1rem', fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.6, marginBottom: '1rem' }}>
              "Repository exhibits high modularity with Spring Boot backend services and React Vite frontend. Code quality score is 94/100 with zero critical security vulnerabilities detected."
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span className="badge badge-teal">AI Analysis Active</span>
              <button onClick={() => window.location.href = '/oauth2/authorization/github'} className="btn btn-ghost" style={{ fontSize: '0.8rem', gap: '0.25rem' }}>
                <span>Inspect Repository</span>
                <ArrowRight size={14} />
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* 6. Call to Action (CTA) Banner */}
      <section style={{ padding: '6rem 2rem', maxWidth: '1280px', margin: '0 auto', textAlign: 'center' }}>
        <div className="card-3xl" style={{
          padding: '4rem 2rem',
          background: 'radial-gradient(circle at 50% 50%, var(--accent-primary-bg), transparent 80%), var(--bg-secondary)',
          border: '1px solid var(--border-color-hover)',
          boxShadow: 'var(--shadow-lg), var(--shadow-glow)'
        }}>
          <span className="badge badge-teal" style={{ marginBottom: '1rem' }}>GET STARTED TODAY</span>
          <h2 style={{ fontSize: '2.75rem', fontWeight: 800, marginBottom: '1rem' }}>
            Elevate Your Engineering Visibility
          </h2>
          <p style={{ color: 'var(--text-secondary)', maxWidth: '600px', margin: '0 auto 2.5rem auto', fontSize: '1.05rem' }}>
            Join modern software development teams using GitPilot to monitor repository health and accelerate delivery.
          </p>

          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
            <button
              onClick={() => window.location.href = '/oauth2/authorization/github'}
              className="btn btn-primary"
              style={{ padding: '0.9rem 2.25rem', fontSize: '1rem', borderRadius: '1.25rem' }}
            >
              <Github size={20} />
              <span>Connect with GitHub</span>
            </button>
          </div>
        </div>
      </section>

      {/* 7. Footer Section */}
      <footer style={{
        borderTop: '1px solid var(--border-color)',
        padding: '4rem 2rem 2rem 2rem',
        backgroundColor: 'var(--bg-sidebar)',
        color: 'var(--text-secondary)'
      }}>
        <div style={{ maxWidth: '1280px', margin: '0 auto' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '2.5rem', marginBottom: '3rem' }}>
            {/* Column 1: Brand */}
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem' }}>
                <div style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: '0.75rem',
                  background: 'linear-gradient(135deg, var(--accent-primary), #7c3aed)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <Sparkles size={16} style={{ color: '#fff' }} />
                </div>
                <span style={{ fontFamily: 'Space Grotesk, sans-serif', fontWeight: 800, fontSize: '1.2rem', color: 'var(--text-primary)' }}>
                  GitPilot
                </span>
              </div>
              <p style={{ fontSize: '0.85rem', lineHeight: 1.6, marginBottom: '1rem' }}>
                AI-Powered Repository Intelligence & Real-Time Push Telemetry Platform.
              </p>
              <span className="badge badge-success" style={{ fontSize: '0.75rem' }}>
                <Activity size={12} />
                <span>All Systems Operational</span>
              </span>
            </div>

            {/* Column 2: Product */}
            <div>
              <h4 style={{ color: 'var(--text-primary)', fontSize: '0.9rem', fontWeight: 700, marginBottom: '1rem' }}>PRODUCT</h4>
              <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '0.65rem', fontSize: '0.875rem' }}>
                <li><a href="#features" style={{ color: 'inherit', textDecoration: 'none' }}>Feature Highlights</a></li>
                <li><a href="#workflow" style={{ color: 'inherit', textDecoration: 'none' }}>GitHub Sync</a></li>
                <li><a href="#insights" style={{ color: 'inherit', textDecoration: 'none' }}>Health Telemetry</a></li>
                <li><a href="#intelligence" style={{ color: 'inherit', textDecoration: 'none' }}>AI Recommendations</a></li>
              </ul>
            </div>

            {/* Column 3: Resources */}
            <div>
              <h4 style={{ color: 'var(--text-primary)', fontSize: '0.9rem', fontWeight: 700, marginBottom: '1rem' }}>RESOURCES</h4>
              <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '0.65rem', fontSize: '0.875rem' }}>
                <li><a href="#" style={{ color: 'inherit', textDecoration: 'none' }}>Documentation</a></li>
                <li><a href="#" style={{ color: 'inherit', textDecoration: 'none' }}>API Webhooks</a></li>
                <li><a href="#" style={{ color: 'inherit', textDecoration: 'none' }}>Security & Privacy</a></li>
                <li><a href="#" style={{ color: 'inherit', textDecoration: 'none' }}>Status Dashboard</a></li>
              </ul>
            </div>

            {/* Column 4: Preferences */}
            <div>
              <h4 style={{ color: 'var(--text-primary)', fontSize: '0.9rem', fontWeight: 700, marginBottom: '1rem' }}>THEME & APPEARANCE</h4>
              <p style={{ fontSize: '0.85rem', marginBottom: '1rem' }}>Select your preferred interface theme:</p>
              <ThemeToggle />
            </div>
          </div>

          <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', fontSize: '0.825rem' }}>
            <span>&copy; {new Date().getFullYear()} GitPilot Inc. All rights reserved.</span>
            <div style={{ display: 'flex', gap: '1.5rem' }}>
              <a href="#" style={{ color: 'inherit', textDecoration: 'none' }}>Privacy Policy</a>
              <a href="#" style={{ color: 'inherit', textDecoration: 'none' }}>Terms of Service</a>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
