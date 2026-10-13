import adapter from '@sveltejs/adapter-static';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

/** @type {import('@sveltejs/kit').Config} */
const config = {
	preprocess: vitePreprocess(),

	kit: {
		// single-page app served by the CDS under /ui; all data is loaded in the browser from the
		// Contest API. Built into WebContent so it is included in the war
		adapter: adapter({
			pages: '../WebContent/ui',
			assets: '../WebContent/ui',
			fallback: 'index.html'
		}),
		paths: {
			base: '/ui'
		}
	}
};

export default config;
