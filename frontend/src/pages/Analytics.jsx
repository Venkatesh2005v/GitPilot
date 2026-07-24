import React, { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell, PieChart, Pie, Legend } from 'recharts';
import { BarChart2, TrendingUp, Users, Calendar, AlertTriangle } from 'lucide-react';
import { Link } from 'react-router-dom';

export function Analytics() {
  const [repos, setRepos] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchAnalytics = async () => {
    try {
      const response = await fetch('/dashboard/repositories');
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
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <div className="pulse-indicator running" style={{ width: '24px', height: '24px' }}></div>
      </div>
    );
  }

  if (repos.length === 0) {
    return (
      <div className="fade-in">
        <h1 style={{ fontSize: '2rem', fontWeight: 700, marginBottom: '0.5rem', fontFamily: 'Outfit, sans-serif' }}>Overall Analytics</h1>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>Analyze commit frequency across selected codebases.</p>
        
        <div className="card" style={{ textAlign: 'center', padding: '4rem 2rem' }}>
          <AlertTriangle size={36} style={{ color: 'var(--accent-yellow)', marginBottom: '1.5rem' }} />
          <h3 style={{ marginBottom: '0.5rem' }}>No data available</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '2rem' }}>
            Please select at least one repository in Settings to analyze its commit activity.
          </p>
          <Link to="/settings" className="btn btn-primary">
            Configure Tracking
          </Link>
        </div>
      </div>
    );
  }

  // Predefined chart colors
  const COLORS = ['#38bdf8', '#6366f1', '#10b981', '#f59e0b', '#ef4444', '#a855f7'];

  return (
    <div className="fade-in">
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '2rem', fontWeight: 700, marginBottom: '0.5rem', fontFamily: 'Outfit, sans-serif' }}>Overall Analytics</h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Analyze commit frequencies and contribution metrics across selected codebases.</p>
      </div>

      <div className="grid-cols-2">
        {/* Commits per repository Bar Chart */}
        <div className="card" style={{ padding: '2rem' }}>
          <h3 style={{ fontSize: '1.15rem', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <BarChart2 size={18} style={{ color: 'var(--accent-sky)' }} />
            <span>Commits by Repository</span>
          </h3>
          <div style={{ width: '100%', height: 280 }}>
            <ResponsiveContainer>
              <BarChart data={repos} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <XAxis dataKey="repositoryName" stroke="var(--text-muted)" fontSize={11} tickLine={false} />
                <YAxis stroke="var(--text-muted)" fontSize={11} tickLine={false} />
                <Tooltip contentStyle={{ backgroundColor: 'var(--bg-card)', borderColor: 'var(--border-color)', borderRadius: '8px' }} />
                <Bar dataKey="totalCommits" radius={[4, 4, 0, 0]}>
                  {repos.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Commits distribution percentage Pie Chart */}
        <div className="card" style={{ padding: '2rem', display: 'flex', flexDirection: 'column' }}>
          <h3 style={{ fontSize: '1.15rem', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <TrendingUp size={18} style={{ color: 'var(--accent-indigo)' }} />
            <span>Commit Volume Split</span>
          </h3>
          <div style={{ display: 'flex', flexGrow: 1, alignItems: 'center', justifyContent: 'center' }}>
            <div style={{ width: '100%', height: 230 }}>
              <ResponsiveContainer>
                <PieChart>
                  <Pie
                    data={repos}
                    dataKey="totalCommits"
                    nameKey="repositoryName"
                    cx="50%"
                    cy="50%"
                    outerRadius={80}
                    label={({ name, percent }) => `${name.substring(0, 10)} (${(percent * 100).toFixed(0)}%)`}
                    style={{ fontSize: '10px', fill: 'var(--text-primary)' }}
                  >
                    {repos.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      </div>

      {/* Analytics Summary Table */}
      <div className="card" style={{ padding: '2rem' }}>
        <h3 style={{ fontSize: '1.25rem', marginBottom: '1rem', fontFamily: 'Outfit, sans-serif' }}>
          Repository Activity Statistics
        </h3>
        <div className="table-container">
          <table className="custom-table">
            <thead>
              <tr>
                <th>Repository Name</th>
                <th>Total Commits</th>
                <th>Contributors</th>
                <th>Health Score</th>
                <th>Last Synced At</th>
                <th>Sync Status</th>
              </tr>
            </thead>
            <tbody>
              {repos.map(r => (
                <tr key={r.id}>
                  <td style={{ fontWeight: 600 }}>
                    <Link to={`/repositories/${r.id}`} style={{ color: 'var(--text-primary)', textDecoration: 'none' }} className="hover-sky">
                      {r.repositoryName}
                    </Link>
                  </td>
                  <td>{r.totalCommits || 0}</td>
                  <td>{r.uniqueContributorCount || 0}</td>
                  <td style={{ fontWeight: 700, color: r.healthScore >= 80 ? 'var(--accent-green)' : r.healthScore >= 50 ? 'var(--accent-yellow)' : r.healthScore ? 'var(--accent-red)' : 'var(--text-muted)' }}>
                    {r.healthScore !== null ? `${r.healthScore}/100` : '-'}
                  </td>
                  <td>
                    {r.lastSyncedAt ? new Date(r.lastSyncedAt).toLocaleString('en-US', {
                      month: 'short',
                      day: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit'
                    }) : 'Never'}
                  </td>
                  <td>
                    <span className={`badge ${r.lastSyncStatus === 'SUCCESS' ? 'badge-success' : r.lastSyncStatus === 'RUNNING' ? 'badge-info' : 'badge-danger'}`}>
                      {r.lastSyncStatus || 'Pending'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
