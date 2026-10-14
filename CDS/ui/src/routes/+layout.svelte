<script lang="ts">
	import { resolve } from '$app/paths';
	import { ModeWatcher } from 'mode-watcher';
	import '@fortawesome/fontawesome-free/css/all.min.css';
	import '../app.css';

	let { children } = $props();
</script>

<ModeWatcher />

<div class="min-h-screen bg-gray-50 text-gray-900 dark:bg-gray-900 dark:text-gray-100">
	<header class="border-b border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-800">
		<div class="mx-auto flex max-w-6xl items-center gap-4 px-4 py-3">
			<a href={resolve('/')} class="text-lg font-semibold">CDS</a>
			<a href="/" class="ml-auto text-sm text-gray-500 hover:underline dark:text-gray-400">Classic UI</a>
		</div>
	</header>

	<main class="mx-auto max-w-6xl px-4 py-6">
		{@render children()}
	</main>
</div>
