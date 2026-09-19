import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  // Build to the standard local `dist/` directory so Vercel can deploy the frontend
  // independently. (Previously this emitted into the backend's static folder for the
  // combined local setup; that is no longer needed for the split deployment.)
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/me': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/dashboard': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/repositories': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ai': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/architecture': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/github': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/login': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/oauth2': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/logout': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/webhooks': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});
