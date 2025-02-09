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
		// Development proxy configuration
		proxy: {
			'/api': {
				target: 'http://localhost:3001',
				changeOrigin: true,
				rewrite: (path) => path.replace(/^\/api/, '')
			}
		},
		// Production settings
		port: 3000,
		strictPort: true,
	},
	build: {
		target: 'esnext',
		minify: 'esbuild',
		rollupOptions: {
			output: {
				manualChunks: (id) => {
					if (id.includes('node_modules')) {
						if (id.includes('@sveltejs/kit')) {
							return 'vendor-sveltekit';
						}
						return 'vendor';
					}
				}
			}
		},
		chunkSizeWarningLimit: 1000,
		assetsInlineLimit: 4096,
		reportCompressedSize: true,
		sourcemap: true
	}
});