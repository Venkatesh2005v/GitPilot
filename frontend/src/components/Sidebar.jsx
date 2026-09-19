import React, { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, BookOpen, ShieldCheck, Settings, LogOut, ChevronLeft, ChevronRight, Sparkles, Network, Users } from 'lucide-react';
import { apiUrl } from '../utils/apiUtils';

export function Sidebar({ user }) {
  const [collapsed, setCollapsed] = useState(false);

  const navItems = [
    { name: 'Overview', path: '/dashboard', icon: LayoutDashboard },
    { name: 'Developer Onboarding', path: '/onboarding', icon: BookOpen },
    { name: 'Architecture View', path: '/architecture', icon: Network },
    { name: 'Team Intelligence', path: '/team-intelligence', icon: Users },
    { name: 'Recommendations', path: '/recommendations', icon: ShieldCheck },
    { name: 'Settings', path: '/settings', icon: Settings },
  ];

  return (
    <aside style={{
      width: collapsed ? '84px' : '270px',
      backgroundColor: 'var(--bg-sidebar)',
      backdropFilter: 'blur(20px)',
      WebkitBackdropFilter: 'blur(20px)',
      borderRight: '1px solid var(--border-color)',
      display: 'flex',
      flexDirection: 'column',
      transition: 'all 0.3s cubic-bezier(0.16, 1, 0.3, 1)',
      flexShrink: 0,
      zIndex: 100,
      position: 'relative'
    }}>
      {/* Brand Header */}
      <div style={{
        height: '76px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: collapsed ? 'center' : 'space-between',
        padding: '0 1.35rem',
        borderBottom: '1px solid var(--border-color)'
      }}>
        {!collapsed ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
            <div style={{
              width: '38px',
              height: '38px',
              borderRadius: '1.1rem',
              background: 'linear-gradient(135deg, var(--accent-primary), var(--accent-teal))',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 4px 16px var(--accent-primary-glow)'
            }}>
              <Sparkles size={20} style={{ color: '#fff' }} />
            </div>
            <span style={{ fontFamily: 'Space Grotesk, sans-serif', fontWeight: 800, fontSize: '1.3rem', letterSpacing: '-0.025em', color: 'var(--text-primary)' }}>
              Git<span style={{ color: 'var(--accent-primary)' }}>Pilot</span>
            </span>
          </div>
        ) : (
          <div style={{
            width: '38px',
            height: '38px',
            borderRadius: '1.1rem',
            background: 'linear-gradient(135deg, var(--accent-primary), var(--accent-teal))',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 4px 16px var(--accent-primary-glow)'
          }}>
            <Sparkles size={20} style={{ color: '#fff' }} />
          </div>
        )}

        <button 
          onClick={() => setCollapsed(!collapsed)}
          className="btn btn-ghost"
          style={{
            padding: '0.4rem',
            borderRadius: '0.85rem'
          }}
          title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        >
          {collapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
        </button>
      </div>

      {/* Navigation */}
      <nav style={{ flexGrow: 1, padding: '1.5rem 0.95rem', display: 'flex', flexDirection: 'column', gap: '0.5rem', overflowY: 'auto' }}>
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
                gap: '0.9rem',
                padding: '0.8rem 1.1rem',
                borderRadius: '1.25rem',
                color: isActive ? 'var(--accent-primary)' : 'var(--text-secondary)',
                backgroundColor: isActive ? 'var(--accent-primary-bg)' : 'transparent',
                border: isActive ? '1px solid var(--border-color-hover)' : '1px solid transparent',
                boxShadow: isActive ? 'var(--shadow-sm), var(--shadow-glow)' : 'none',
                textDecoration: 'none',
                fontSize: '0.925rem',
                fontWeight: isActive ? 600 : 500,
                transition: 'all 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
                whiteSpace: 'nowrap',
                overflow: 'hidden'
              })}
            >
              <Icon size={19} style={{ flexShrink: 0 }} />
              {!collapsed && <span>{item.name}</span>}
            </NavLink>
          );
        })}
      </nav>

      {/* User Footer Profile */}
      <div style={{
        padding: '1.15rem 1.25rem',
        borderTop: '1px solid var(--border-color)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: collapsed ? 'center' : 'flex-start',
        gap: '0.85rem',
        overflow: 'hidden'
      }}>
        <img 
          src={user.avatarUrl || "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=80&h=80"}
          alt="Avatar" 
          style={{ width: '38px', height: '38px', borderRadius: '50%', border: '2px solid var(--accent-primary-glow)' }}
        />
        {!collapsed && (
          <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0, flexGrow: 1 }}>
            <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {user.name || user.username}
            </span>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              @{user.username}
            </span>
          </div>
        )}
        {!collapsed && (
          <button 
            onClick={() => window.location.href = apiUrl('/logout')}
            className="btn btn-ghost"
            style={{ padding: '0.4rem', borderRadius: '0.65rem' }}
            title="Log out"
          >
            <LogOut size={16} />
          </button>
        )}
      </div>
    </aside>
  );
}
