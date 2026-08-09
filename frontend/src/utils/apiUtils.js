/**
 * Safe fetch helper that guarantees returning JSON or null.
 * Prevents "Unexpected token '<'" errors when API endpoints return HTML (e.g. 404 SPA fallback).
 */
export async function safeFetchJson(url, options = {}) {
  try {
    const res = await fetch(url, options);
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
export function getSummaryText(intel, fallbackText = "") {
  if (typeof intel?.summary === 'string' && intel.summary.trim().length > 0) {
    return intel.summary;
  }
  if (typeof intel?.summary === 'object' && intel.summary !== null) {
    return intel.summary.projectPurpose || intel.summary.mainFunctionality || intel.summary.highLevelDescription || fallbackText;
  }
  if (typeof intel?.projectPurpose === 'string' && intel.projectPurpose.trim().length > 0) {
    return intel.projectPurpose;
  }
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
