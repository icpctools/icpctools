import { sveltekit } from '@sveltejs/kit/vite';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig } from 'vite';

// in production the app is served by the CDS; in development, proxy the Contest API to a running CDS
// so it appears on the same origin
const apiProxy = {
	'/api': { target: process.env.CDS_URL || 'https://localhost:8443', changeOrigin: true, secure: false }
};

export default defineConfig({
	plugins: [tailwindcss(), sveltekit()],
	server: {
		proxy: apiProxy
	},
	preview: {
		proxy: apiProxy
	}
});
