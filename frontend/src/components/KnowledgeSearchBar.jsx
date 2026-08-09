import React, { useState, useEffect } from 'react';
import { Search, X, Sparkles, BookOpen, Cpu, ShieldCheck, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export function KnowledgeSearchBar({ selectedRepoId }) {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setOpen((prev) => !prev);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const handleSearch = async (val) => {
    setQuery(val);
    if (!val.trim()) {
      setResults([]);
      return;
    }
    setLoading(true);
    try {
      const repoId = selectedRepoId || 1;
      const res = await fetch(`/api/memory/${repoId}/search?query=${encodeURIComponent(val)}`);
      if (res.ok) {
        const data = await res.json();
        setResults(data.items || []);
      }
    } catch (e) {
      console.error("Search failed:", e);
    } finally {
      setLoading(false);
    }
  };

  if (!open) {
    return (
      <button
        onClick={() => setOpen(true)}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.65rem',
          backgroundColor: 'var(--bg-card)',
          border: '1px solid var(--border-color)',
          borderRadius: '9999px',
          padding: '0.45rem 1rem',
          color: 'var(--text-muted)',
          fontSize: '0.85rem',
          cursor: 'pointer',
          boxShadow: 'var(--shadow-sm)'
        }}
      >
        <Search size={15} style={{ color: 'var(--accent-primary)' }} />
        <span>Search Repository Knowledge...</span>
        <kbd style={{
          backgroundColor: 'var(--bg-secondary)',
          padding: '0.15rem 0.45rem',
          borderRadius: '0.4rem',
          fontSize: '0.725rem',
          border: '1px solid var(--border-color)',
          fontFamily: 'JetBrains Mono'
        }}>⌘K</kbd>
      </button>
    );
  }

  return (
    <div style={{
      position: 'fixed',
      inset: 0,
      backgroundColor: 'rgba(0,0,0,0.6)',
      backdropFilter: 'blur(8px)',
      zIndex: 999,
      display: 'flex',
      alignItems: 'flex-start',
      justifyContent: 'center',
      paddingTop: '10vh'
    }}>
      <div style={{
        width: '100%',
        maxWidth: '650px',
        backgroundColor: 'var(--bg-card-solid)',
        borderRadius: '1.5rem',
        border: '1px solid var(--border-color-hover)',
        boxShadow: 'var(--shadow-lg), var(--shadow-glow)',
        overflow: 'hidden'
      }}>
        {/* Search Header Input */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          padding: '1.15rem 1.35rem',
          borderBottom: '1px solid var(--border-color)',
          gap: '0.85rem'
        }}>
          <Search size={20} style={{ color: 'var(--accent-primary)' }} />
          <input
            type="text"
            autoFocus
            placeholder="Search decisions, onboarding steps, modules, concepts (e.g. JWT, OAuth, Docker)..."
            value={query}
            onChange={(e) => handleSearch(e.target.value)}
            style={{
              flex: 1,
              background: 'none',
              border: 'none',
              color: 'var(--text-primary)',
              fontSize: '1rem',
              outline: 'none',
              fontFamily: 'Inter, sans-serif'
            }}
          />
          <button
            onClick={() => setOpen(false)}
            style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}
          >
            <X size={20} />
          </button>
        </div>

        {/* Search Results List */}
        <div style={{ maxHeight: '420px', overflowY: 'auto', padding: '1rem' }}>
          {loading && (
            <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.875rem' }}>
              Searching repository memory...
            </div>
          )}

          {!loading && results.length === 0 && query && (
            <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.875rem' }}>
              No engineering knowledge matches found for "{query}".
            </div>
          )}

          {!loading && results.length === 0 && !query && (
            <div style={{ padding: '1.5rem', fontSize: '0.825rem', color: 'var(--text-muted)' }}>
              <strong>Popular Engineering Memory Topics:</strong>
              <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginTop: '0.75rem' }}>
                {['Authentication', 'Docker', 'Flyway', 'Webhooks', 'OAuth2', 'Caching'].map((tag) => (
                  <span
                    key={tag}
                    onClick={() => handleSearch(tag)}
                    style={{
                      padding: '0.35rem 0.75rem',
                      backgroundColor: 'var(--bg-secondary)',
                      borderRadius: '9999px',
                      cursor: 'pointer',
                      border: '1px solid var(--border-color)',
                      color: 'var(--text-primary)',
                      fontSize: '0.8rem'
                    }}
                  >
                    {tag}
                  </span>
                ))}
              </div>
            </div>
          )}

          {results.map((item, idx) => (
            <div
              key={idx}
              onClick={() => {
                setOpen(false);
                if (item.linkUrl) navigate(item.linkUrl);
              }}
              style={{
                padding: '0.9rem 1.15rem',
                borderRadius: '1rem',
                backgroundColor: 'var(--bg-secondary)',
                marginBottom: '0.65rem',
                border: '1px solid var(--border-color)',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between'
              }}
            >
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
                  <span className="badge badge-teal" style={{ fontSize: '0.7rem' }}>{item.type}</span>
                  <strong style={{ fontSize: '0.95rem', color: 'var(--text-primary)' }}>{item.title}</strong>
                </div>
                <p style={{ fontSize: '0.825rem', color: 'var(--text-secondary)', margin: 0, lineHeight: 1.5 }}>
                  {item.snippet}
                </p>
              </div>
              <ArrowRight size={16} style={{ color: 'var(--accent-primary)', flexShrink: 0, marginLeft: '1rem' }} />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
