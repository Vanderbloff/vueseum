import adapter from '@sveltejs/adapter-node';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

/** @type {import('@sveltejs/kit').Config} */
const config = {
	preprocess: vitePreprocess(),

	kit: {
		adapter: adapter(),
		server: {
			https: {
				cert: '/etc/letsencrypt/live/vueseum.io/fullchain.pem',
				key: '/etc/letsencrypt/live/vueseum.io/privkey.pem'
			}
		}
	}
};

export default config;
