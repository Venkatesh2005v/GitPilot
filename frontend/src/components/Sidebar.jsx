import React, { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, BarChart2, Cpu, Settings, LogOut, ChevronLeft, ChevronRight } from 'lucide-react';

export function Sidebar({ user }) {
  const [collapsed, setCollapsed] = useState(false);

  const navItems = [
    { name: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
    { name: 'Analytics', path: '/analytics', icon: BarChart2 },
    { name: 'AI Insights', path: '/insights', icon: Cpu },
    { name: 'Settings', path: '/settings', icon: Settings },
  ];

  return (
    <aside style={{
      width: collapsed ? 'var(--sidebar-collapsed-width)' : 'var(--sidebar-width)',
      backgroundColor: 'var(--bg-secondary)',
      borderRight: '1px solid var(--border-color)',
      display: 'flex',
      flexDirection: 'column',
      transition: 'width 0.2s cubic-bezier(0.16, 1, 0.3, 1)',
      flexShrink: 0,
      zIndex: 100
    }}>
      {/* Brand Header */}
      <div style={{
        height: 'var(--header-height)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: collapsed ? 'center' : 'space-between',
        padding: '0 1.25rem',
        borderBottom: '1px solid var(--border-color)'
      }}>
        {!collapsed && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
            <span style={{
              background: 'linear-gradient(135deg, var(--accent-indigo), var(--accent-sky))',
              color: '#000',
              fontWeight: 800,
              padding: '0.25rem 0.5rem',
              borderRadius: '6px',
              fontSize: '0.9rem',
              fontFamily: 'Outfit, sans-serif'
            }}>GP</span>
            <span style={{ fontFamily: 'Outfit, sans-serif', fontWeight: 700, fontSize: '1.15rem', letterSpacing: '-0.02em' }}>GitPilot</span>
          </div>
        )}
        {collapsed && (
          <span style={{
            background: 'linear-gradient(135deg, var(--accent-indigo), var(--accent-sky))',
            color: '#000',
            fontWeight: 800,
            padding: '0.25rem 0.5rem',
            borderRadius: '6px',
            fontSize: '0.9rem',
            fontFamily: 'Outfit, sans-serif'
          }}>GP</span>
        )}
        <button 
          onClick={() => setCollapsed(!collapsed)}
          style={{
            background: 'none',
            border: 'none',
            color: 'var(--text-muted)',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            padding: '0.25rem',
            borderRadius: '4px'
          }}
          title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        >
          {collapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
        </button>
      </div>

      {/* Navigation */}
      <nav style={{ flexGrow: 1, padding: '1rem 0.75rem', display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.name}
              to={item.path}
              className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
              style={({ isActive }) => ({
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                padding: '0.75rem 0.875rem',
                borderRadius: '8px',
                color: isActive ? 'var(--accent-sky)' : 'var(--text-secondary)',
                backgroundColor: isActive ? 'var(--accent-sky-glow)' : 'transparent',
                textDecoration: 'none',
                fontSize: '0.875rem',
                fontWeight: 500,
                transition: 'all 0.15s ease',
                whiteSpace: 'nowrap',
                overflow: 'hidden'
              })}
            >
              <Icon size={18} style={{ flexShrink: 0 }} />
              {!collapsed && <span>{item.name}</span>}
            </NavLink>
          );
        })}
      </nav>

      {/* User Footer Profile */}
      <div style={{
        padding: '1rem',
        borderTop: '1px solid var(--border-color)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: collapsed ? 'center' : 'flex-start',
        gap: '0.75rem',
        overflow: 'hidden'
      }}>
        <img 
          src={user.avatarUrl || "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=80&h=80"}
          alt="Avatar" 
          style={{ width: '32px', height: '32px', borderRadius: '50%', border: '1.5px solid var(--border-color)' }}
        />
        {!collapsed && (
          <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0, flexGrow: 1 }}>
            <span style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-primary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {user.name || user.username}
            </span>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              @{user.username}
            </span>
          </div>
        )}
        {!collapsed && (
          <button 
            onClick={() => window.location.href = '/logout'}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--text-muted)',
              cursor: 'pointer',
              display: 'flex',
              padding: '0.25rem',
              borderRadius: '4px'
            }}
            title="Log out"
          >
            <LogOut size={16} />
          </button>
        )}
      </div>
    </aside>
  );
}
