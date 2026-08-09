import React from 'react';
import { useTheme } from '../context/ThemeContext';
import { Sun, Moon, Monitor } from 'lucide-react';

export function ThemeToggle() {
  const { theme, setTheme } = useTheme();

  return (
    <div style={{
      display: 'inline-flex',
      alignItems: 'center',
      gap: '0.2rem',
      background: 'var(--bg-secondary)',
      padding: '0.25rem',
      borderRadius: '9999px',
      border: '1px solid var(--border-color)'
    }}>
      <button
        type="button"
        className={`btn btn-ghost ${theme === 'light' ? 'active' : ''}`}
        onClick={() => setTheme('light')}
        title="Light Theme"
        style={{
          padding: '0.35rem 0.55rem',
          borderRadius: '9999px',
          backgroundColor: theme === 'light' ? 'var(--bg-card)' : 'transparent',
          color: theme === 'light' ? 'var(--accent-primary)' : 'var(--text-muted)',
          boxShadow: theme === 'light' ? 'var(--shadow-sm)' : 'none'
        }}
      >
        <Sun size={14} />
      </button>

      <button
        type="button"
        className={`btn btn-ghost ${theme === 'dark' ? 'active' : ''}`}
        onClick={() => setTheme('dark')}
        title="Dark Theme"
        style={{
          padding: '0.35rem 0.55rem',
          borderRadius: '9999px',
          backgroundColor: theme === 'dark' ? 'var(--bg-card)' : 'transparent',
          color: theme === 'dark' ? 'var(--accent-primary)' : 'var(--text-muted)',
          boxShadow: theme === 'dark' ? 'var(--shadow-sm)' : 'none'
        }}
      >
        <Moon size={14} />
      </button>

      <button
        type="button"
        className={`btn btn-ghost ${theme === 'system' ? 'active' : ''}`}
        onClick={() => setTheme('system')}
        title="System OS Theme"
        style={{
          padding: '0.35rem 0.55rem',
          borderRadius: '9999px',
          backgroundColor: theme === 'system' ? 'var(--bg-card)' : 'transparent',
          color: theme === 'system' ? 'var(--accent-primary)' : 'var(--text-muted)',
          boxShadow: theme === 'system' ? 'var(--shadow-sm)' : 'none'
        }}
      >
        <Monitor size={14} />
      </button>
    </div>
  );
}
