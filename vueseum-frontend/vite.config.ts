import { defineConfig } from 'vitest/config';
import { sveltekit } from '@sveltejs/kit/vite';

export default defineConfig({
	plugins: [sveltekit()],
	test: {
		environment: 'jsdom',
		globals: true,
		include: ['src/**/*.{test,spec}.{js,ts}']
	},
	server: {
		proxy: {
			'/api/v1': {
				target: 'http://localhost:3001',
				changeOrigin: true,
			}
		},
		host: '0.0.0.0',
		port: 3000,
		strictPort: true,
		hmr: {
			host: 'localhost',
			port: 24678,
			protocol: 'ws'
		}
	},
	build: {
		target: 'esnext',
		minify: 'esbuild',
		rollupOptions: {
			output: {
				manualChunks: {
					vendor: ['svelte']
				}
			}
		}
	}
});