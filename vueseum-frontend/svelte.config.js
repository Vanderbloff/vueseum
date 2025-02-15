import adapter from '@sveltejs/adapter-node';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

/** @type {import('@sveltejs/kit').Config} */
const config = {
	preprocess: vitePreprocess(),
	kit: {
		adapter: adapter()
	},
	files: {
		lib: 'src/lib',
		routes: 'src/routes',
		appTemplate: 'src/app.html'
	}
};

export default config;
