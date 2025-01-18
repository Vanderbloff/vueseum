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
			'/api': {
				target: 'http://localhost:3001',
				changeOrigin: true,
				rewrite: (path) => path.replace(/^\/api/, '')
			}
		}
	}
});