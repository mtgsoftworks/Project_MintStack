import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig(() => {
  const isVitest = Boolean(process.env.VITEST)

  return {
    define: {
      global: 'globalThis',
    },
    plugins: [react()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: isVitest
      ? {
        // Avoid HMR websocket startup conflicts during vitest runs.
        hmr: false,
      }
      : {
        port: 3000,
        host: true,
        proxy: {
          '/api': {
            target: 'http://localhost:18080',
            changeOrigin: true,
          },
        },
      },
    build: {
      outDir: 'dist',
      sourcemap: true,
      rollupOptions: {
        output: {
          manualChunks: {
            vendor: ['react', 'react-dom', 'react-router-dom'],
            charts: ['recharts'],
            redux: ['@reduxjs/toolkit', 'react-redux'],
            radix: [
              '@radix-ui/react-dialog',
              '@radix-ui/react-dropdown-menu',
              '@radix-ui/react-select',
              '@radix-ui/react-tabs',
              '@radix-ui/react-tooltip',
            ],
          },
        },
      },
    },
    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: './src/setupTests.ts',
      css: true,
      fileParallelism: false,
      poolOptions: {
        forks: {
          singleFork: true,
        },
      },
      exclude: ['node_modules', 'e2e'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'html', 'lcov'],
        exclude: [
          'node_modules/',
          'src/setupTests.ts',
          'src/mocks/**',
          'e2e/**',
        ],
        // Gradual increase goal: target 80% coverage across all metrics
        // Current milestones: lines: 60%, branches: 50%, functions: 55%, statements: 60%
        thresholds: {
          lines: 60,
          branches: 50,
          functions: 55,
          statements: 60,
        },
      },
    },
  }
})
