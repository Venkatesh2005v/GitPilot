/**
 * Centralized API base URL.
 *
 * Local development: VITE_API_BASE_URL is unset/empty, so apiUrl() returns the
 *   path unchanged (e.g. "/ai/..."), and the existing Vite dev proxy forwards it
 *   to the local backend. Behavior is identical to before.
 * Production (Vercel): set VITE_API_BASE_URL=https://<render-backend> so the same
 *   relative paths become absolute calls to the Render backend.
 */
export const API_BASE_URL = (import.meta.env?.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

/**
 * Build a full API URL from a relative path.
 * Absolute URLs are returned untouched.
 */
export function apiUrl(path) {
  if (typeof path !== 'string') return path;
  if (/^https?:\/\//i.test(path)) return path;
  if (!API_BASE_URL) return path; // local: use relative path (Vite proxy / same origin)
  return `${API_BASE_URL}${path.startsWith('/') ? '' : '/'}${path}`;
}

/**
 * fetch wrapper that applies the API base URL and always sends credentials.
 * credentials:'include' is required so the session cookie is sent cross-origin
 * (Vercel frontend -> Render backend). Locally it is same-origin, so it is a no-op.
 */
export function apiFetch(path, options = {}) {
  return fetch(apiUrl(path), { credentials: 'include', ...options });
}

/**
 * Safe fetch helper that guarantees returning JSON or null.
 * Prevents "Unexpected token '<'" errors when API endpoints return HTML (e.g. 404 SPA fallback).
 */
export async function safeFetchJson(url, options = {}) {
  try {
    const res = await fetch(apiUrl(url), { credentials: 'include', ...options });
    const contentType = res.headers.get("content-type");
    if (res.ok && contentType && contentType.includes("application/json")) {
      return await res.json();
    }
    return null;
  } catch (e) {
    if (e.name !== 'AbortError') {
      console.warn(`safeFetchJson failed for ${url}:`, e);
    }
    return null;
  }
}

/**
 * Safely extracts a numeric health score (0 - 100) from diverse DTO shapes.
 */
export function getNumericHealthScore(intel, fallbackScore = 0) {
  if (typeof intel?.healthScore === 'number' && !isNaN(intel.healthScore)) {
    return intel.healthScore;
  }
  if (typeof intel?.healthScore?.overallHealthScore === 'number' && !isNaN(intel.healthScore.overallHealthScore)) {
    return intel.healthScore.overallHealthScore;
  }
  if (typeof intel?.overallHealthScore === 'number' && !isNaN(intel.overallHealthScore)) {
    return intel.overallHealthScore;
  }
  if (typeof fallbackScore === 'number' && !isNaN(fallbackScore)) {
    return fallbackScore;
  }
  return 0;
}

/**
 * Safely extracts a string summary from diverse DTO shapes.
 */
// Placeholder/fallback phrases the backend emits when an individual sub-analysis
// could not be produced. These are not real summaries, so the UI should skip them
// and fall through to the other populated fields of the same (successfully retrieved) report.
const SUMMARY_PLACEHOLDER_PATTERNS = [
  'ai analysis unavailable',
  'ai analysis temporarily unavailable',
  'unable to generate summary',
  'unable to determine',
  'analysis failed',
];

function isPlaceholderSummary(text) {
  if (typeof text !== 'string') return true;
  const trimmed = text.trim();
  if (trimmed.length === 0) return true;
  const lower = trimmed.toLowerCase();
  return SUMMARY_PLACEHOLDER_PATTERNS.some((p) => lower.includes(p));
}

function firstMeaningful(...candidates) {
  for (const c of candidates) {
    if (typeof c === 'string' && !isPlaceholderSummary(c)) {
      return c;
    }
  }
  return null;
}

export function getSummaryText(intel, fallbackText = "") {
  if (typeof intel?.summary === 'string' && !isPlaceholderSummary(intel.summary)) {
    return intel.summary;
  }
  if (typeof intel?.summary === 'object' && intel.summary !== null) {
    // Prefer the summary's own fields, but skip backend placeholder text. When the
    // primary summary field is a placeholder, fall through to other genuinely
    // populated parts of the same report so a retrieved report is never rendered
    // as "unavailable".
    const meaningful = firstMeaningful(
      intel.summary.projectPurpose,
      intel.summary.mainFunctionality,
      intel.summary.highLevelDescription,
      intel?.readmeSummary?.projectOverview,
      intel?.commitSummary?.highLevelSummary
    );
    if (meaningful) return meaningful;
  }
  if (typeof intel?.projectPurpose === 'string' && !isPlaceholderSummary(intel.projectPurpose)) {
    return intel.projectPurpose;
  }
  // Last resort: any populated overview from other sections of the retrieved report.
  const crossSection = firstMeaningful(
    intel?.readmeSummary?.projectOverview,
    intel?.commitSummary?.highLevelSummary
  );
  if (crossSection) return crossSection;
  return fallbackText;
}

/**
 * Safely extracts an array of tech stack string names from diverse DTO shapes.
 */
export function getTechStackArray(intel, fallbackStack = []) {
  if (Array.isArray(intel?.detectedStack)) {
    return intel.detectedStack;
  }
  if (Array.isArray(intel?.techStack?.detectedTechnologies)) {
    return intel.techStack.detectedTechnologies;
  }
  if (Array.isArray(intel?.techStack)) {
    return intel.techStack;
  }
  if (Array.isArray(intel?.primaryTechnologies)) {
    return intel.primaryTechnologies;
  }
  return fallbackStack;
}
