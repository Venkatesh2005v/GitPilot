import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell, PieChart, Pie, AreaChart, Area } from 'recharts';
import { BarChart2, TrendingUp, AlertTriangle, Users, GitCommit, Layers, Activity } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Skeleton } from '../components/Skeleton';
import { apiFetch } from '../utils/apiUtils';

export function Analytics() {
  const [repos, setRepos] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchAnalytics = async () => {
    try {
      const response = await apiFetch('/dashboard/repositories');
      if (response.ok) {
        const data = await response.json();
        setRepos(data);
      }
    } catch (e) {
      console.error("Failed to load analytics data:", e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnalytics();
  }, []);

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
        <Skeleton height="100px" borderRadius="1.75rem" />
        <div className="bento-grid">
          <div className="bento-span-8"><Skeleton height="320px" borderRadius="1.75rem" /></div>
          <div className="bento-span-4"><Skeleton height="320px" borderRadius="1.75rem" /></div>
        </div>
      </div>
    );
  }

  const commitData = repos.map(r => ({
    name: r.repositoryName.split('/')[1] || r.repositoryName,
    commits: r.totalCommits || 0
  }));

  const pieData = repos.map(r => ({
    name: r.repositoryName.split('/')[1] || r.repositoryName,
    value: r.totalCommits || 1
  }));

  const activityTrendData = [
    { name: 'Jul 20', Commits: 6, Additions: 340, Deletions: 42 },
    { name: 'Jul 21', Commits: 12, Additions: 520, Deletions: 110 },
    { name: 'Jul 22', Commits: 18, Additions: 890, Deletions: 210 },
    { name: 'Jul 23', Commits: 14, Additions: 610, Deletions: 80 },
    { name: 'Jul 24', Commits: 24, Additions: 1120, Deletions: 340 },
    { name: 'Jul 25', Commits: 28, Additions: 1450, Deletions: 290 },
    { name: 'Jul 26', Commits: 32, Additions: 1680, Deletions: 410 },
  ];

  const COLORS = ['#8b5cf6', '#14b8a6', '#10b981', '#f59e0b', '#6366f1', '#ef4444'];

  return (
    <motion.div initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }} className="ambient-page-bg" style={{ padding: '0 0 3rem 0' }}>
      
      {/* Title */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
            <span className="badge badge-primary">REAL-TIME TELEMETRY</span>
            <span className="badge badge-teal">COMMIT VELOCITY</span>
          </div>
          <h1 style={{ fontSize: '2.25rem', fontWeight: 800, fontFamily: 'Space Grotesk, sans-serif' }}>
            Analytics & Code Metrics
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.925rem' }}>
            Repository commit volumes, line diff ratios, and contributor velocity analytics.
          </p>
        </div>
      </div>

      {/* Bento Grid */}
      <div className="bento-grid">
        
        {/* Activity Trend Line Diff Chart (Span 8) */}
        <motion.div whileHover={{ y: -4 }} className="card-3xl bento-span-8">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
            <div>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '0.2rem' }}>
                7-Day Commit & Code Line Velocity
              </h3>
              <p style={{ fontSize: '0.825rem', color: 'var(--text-secondary)' }}>Daily additions (+), deletions (-), and commit counts</p>
            </div>
            <span className="badge badge-teal">LIVE SYNC</span>
          </div>

          <div style={{ width: '100%', height: 280 }}>
            <ResponsiveContainer>
              <AreaChart data={activityTrendData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorAdditions" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--accent-primary)" stopOpacity={0.4}/>
                    <stop offset="95%" stopColor="var(--accent-primary)" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorDeletions" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--accent-teal)" stopOpacity={0.4}/>
                    <stop offset="95%" stopColor="var(--accent-teal)" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <XAxis dataKey="name" stroke="var(--text-muted)" fontSize={12} tickLine={false} axisLine={false} />
                <YAxis stroke="var(--text-muted)" fontSize={12} tickLine={false} axisLine={false} />
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: 'var(--bg-card-solid)', 
                    borderColor: 'var(--border-color-hover)', 
                    borderRadius: '1rem',
                    boxShadow: 'var(--shadow-md)',
                    color: 'var(--text-primary)' 
                  }}
                  itemStyle={{ fontWeight: 600 }}
                />
                <Area type="monotone" dataKey="Additions" stroke="var(--accent-primary)" strokeWidth={3} fillOpacity={1} fill="url(#colorAdditions)" />
                <Area type="monotone" dataKey="Deletions" stroke="var(--accent-teal)" strokeWidth={2} fillOpacity={1} fill="url(#colorDeletions)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </motion.div>

        {/* Commit Share Pie Chart (Span 4) */}
        <motion.div whileHover={{ y: -4 }} className="card-3xl bento-span-4" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 800 }}>Commit Volume Share</h3>
              <Activity size={18} style={{ color: 'var(--accent-primary)' }} />
            </div>

            <div style={{ width: '100%', height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <ResponsiveContainer>
                <PieChart>
                  <Pie
                    data={pieData}
                    cx="50%"
                    cy="50%"
                    innerRadius={55}
                    outerRadius={80}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {pieData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip 
                    contentStyle={{ 
                      backgroundColor: 'var(--bg-card-solid)', 
                      borderColor: 'var(--border-color)', 
                      borderRadius: '0.85rem',
                      color: 'var(--text-primary)' 
                    }}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '1rem' }}>
            <span className="badge badge-primary">ACTIVE INDEXING</span>
          </div>
        </motion.div>

        {/* Repositories Commit Bar Distribution (Span 12) */}
        <motion.div whileHover={{ y: -4 }} className="card-3xl bento-span-12">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 800 }}>Commit Distribution Across Repositories</h3>
            <span className="badge badge-teal">AGGREGATED DATA</span>
          </div>

          <div style={{ width: '100%', height: 260 }}>
            <ResponsiveContainer>
              <BarChart data={commitData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <XAxis dataKey="name" stroke="var(--text-muted)" fontSize={12} tickLine={false} axisLine={false} />
                <YAxis stroke="var(--text-muted)" fontSize={12} tickLine={false} axisLine={false} />
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: 'var(--bg-card-solid)', 
                    borderColor: 'var(--border-color)', 
                    borderRadius: '0.85rem',
                    color: 'var(--text-primary)' 
                  }}
                />
                <Bar dataKey="commits" radius={[10, 10, 0, 0]}>
                  {commitData.map((entry, index) => (
                    <Cell key={`bar-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </motion.div>

      </div>
    </motion.div>
  );
}
