import adapter from '@sveltejs/adapter-static';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';
import { sveltekit } from '@sveltejs/kit/vite';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig } from 'vite';

// in production the app is served by the CDS; in development, proxy the Contest API to a running CDS
// so it appears on the same origin
const apiProxy = {
	'/api': { target: process.env.CDS_URL || 'https://localhost:8443', changeOrigin: true, secure: false }
};

export default defineConfig({
	plugins: [
		tailwindcss(),
		sveltekit({
			preprocess: vitePreprocess(),
			// single-page app served by the CDS under /ui; all data is loaded in the browser from the
			// Contest API. Built into WebContent so it is included in the war
			adapter: adapter({
				pages: '../WebContent/ui',
				assets: '../WebContent/ui',
				fallback: 'index.html'
			}),
			paths: { base: '/ui' }
		})
	],
	server: { proxy: apiProxy },
	preview: { proxy: apiProxy }
});
