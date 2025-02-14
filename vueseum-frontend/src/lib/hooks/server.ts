import type { Handle } from '@sveltejs/kit';

export const handle: Handle = async ({ event, resolve }) => {
	// Force HTTPS in production
	if (process.env.NODE_ENV === 'production' && event.url.protocol === 'http:') {
		return new Response(null, {
			status: 301,
			headers: {
				Location: event.url.href.replace('http:', 'https:')
			}
		});
	}

	return resolve(event);
};