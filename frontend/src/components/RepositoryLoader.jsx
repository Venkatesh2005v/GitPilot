import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

const loadingMessages = [
  { emoji: '☕', text: 'Brewing fresh repository insights...' },
  { emoji: '🤖', text: 'Teaching AI to understand your code...' },
  { emoji: '🔍', text: 'Exploring commit history...' },
  { emoji: '📚', text: 'Reading the README...' },
  { emoji: '🧩', text: 'Mapping project architecture...' },
  { emoji: '⚡', text: 'Building onboarding guide...' },
  { emoji: '🐞', text: 'Looking for interesting patterns...' },
  { emoji: '🚀', text: 'Almost ready...' },
];

// Stage-aware messages for repo switching
const stageMessages = {
  metadata: { emoji: '📋', text: 'Fetching repository metadata...' },
  commits: { emoji: '🔍', text: 'Loading commit history...' },
  contributors: { emoji: '👥', text: 'Analyzing contributors...' },
  technologies: { emoji: '🧩', text: 'Detecting technology stack...' },
  ai: { emoji: '🤖', text: 'Generating AI insights...' },
  onboarding: { emoji: '📚', text: 'Preparing onboarding guide...' },
  health: { emoji: '💚', text: 'Calculating health score...' },
  finishing: { emoji: '🚀', text: 'Almost done...' },
};

export function RepositoryLoader({ message, stage, repoName }) {
  const [currentIndex, setCurrentIndex] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentIndex(prev => (prev + 1) % loadingMessages.length);
    }, 2500);
    return () => clearInterval(interval);
  }, []);

  const stageMsg = stage && stageMessages[stage] ? stageMessages[stage] : null;
  const current = stageMsg || loadingMessages[currentIndex];

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '4rem 2rem',
        minHeight: '320px',
        gap: '2rem'
      }}
    >
      {/* Repo name if provided */}
      {repoName && (
        <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontFamily: 'JetBrains Mono, monospace', letterSpacing: '0.02em' }}>
          Analyzing <strong style={{ color: 'var(--text-primary)' }}>{repoName}</strong>...
        </p>
      )}

      {/* Animated spinner */}
      <motion.div
        animate={{ rotate: 360 }}
        transition={{ duration: 2, repeat: Infinity, ease: 'linear' }}
        style={{
          width: '56px',
          height: '56px',
          borderRadius: '1.25rem',
          background: 'linear-gradient(135deg, var(--accent-primary), var(--accent-teal))',
          boxShadow: '0 8px 32px var(--accent-primary-glow)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}
      >
        <div style={{
          width: '40px',
          height: '40px',
          borderRadius: '0.85rem',
          backgroundColor: 'var(--bg-app)'
        }} />
      </motion.div>

      {/* Rotating messages */}
      <div style={{ height: '32px', position: 'relative', width: '100%', maxWidth: '400px' }}>
        <AnimatePresence mode="wait">
          <motion.div
            key={stage || currentIndex}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -12 }}
            transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
            style={{
              position: 'absolute',
              width: '100%',
              textAlign: 'center',
              fontSize: '0.95rem',
              fontWeight: 500,
              color: 'var(--text-secondary)',
              fontFamily: 'Inter, sans-serif'
            }}
          >
            <span style={{ marginRight: '0.5rem', fontSize: '1.1rem' }}>{current.emoji}</span>
            {current.text}
          </motion.div>
        </AnimatePresence>
      </div>

      {/* Optional custom subtitle */}
      {message && (
        <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '-0.5rem' }}>
          {message}
        </p>
      )}
    </motion.div>
  );
}

/**
 * Top progress bar that shows during repository data loading.
 * Place at the top of the page wrapper.
 */
export function TopLoadingBar({ loading }) {
  return (
    <AnimatePresence>
      {loading && (
        <motion.div
          initial={{ scaleX: 0, opacity: 0 }}
          animate={{ scaleX: 1, opacity: 1 }}
          exit={{ scaleX: 0, opacity: 0 }}
          transition={{ duration: 0.3 }}
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            height: '3px',
            zIndex: 9999,
            background: 'linear-gradient(90deg, var(--accent-primary), var(--accent-teal), var(--accent-primary))',
            backgroundSize: '200% 100%',
            transformOrigin: 'left',
            animation: 'shimmer 1.5s ease-in-out infinite'
          }}
        />
      )}
    </AnimatePresence>
  );
}

/**
 * Wrapper for page content that fades/slides cards with stagger.
 */
export function AnimatedPageContent({ children, loading }) {
  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.08,
        delayChildren: 0.1
      }
    }
  };

  return (
    <AnimatePresence mode="wait">
      {!loading && (
        <motion.div
          key="content"
          variants={containerVariants}
          initial="hidden"
          animate="visible"
          exit="hidden"
        >
          {children}
        </motion.div>
      )}
    </AnimatePresence>
  );
}

/**
 * Individual card wrapper with fade + slide up animation.
 * Use inside AnimatedPageContent for stagger effect.
 */
export function AnimatedCard({ children, style = {}, className = '' }) {
  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: {
      opacity: 1,
      y: 0,
      transition: { duration: 0.45, ease: [0.16, 1, 0.3, 1] }
    }
  };

  return (
    <motion.div
      variants={itemVariants}
      className={className}
      style={style}
    >
      {children}
    </motion.div>
  );
}
