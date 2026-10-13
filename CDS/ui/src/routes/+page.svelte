<script lang="ts">
	import { resolve } from '$app/paths';

	let { data } = $props();
</script>

<h1 class="mb-4 text-2xl font-semibold">Contests</h1>

{#if data.contests.length === 0}
	<p class="text-gray-500">No contests are configured.</p>
{:else}
	<ul class="divide-y divide-gray-200 rounded border border-gray-200 bg-white dark:divide-gray-700 dark:border-gray-700 dark:bg-gray-800">
		{#each data.contests as contest (contest.id)}
			<li class="flex items-center gap-4 px-4 py-3">
				<span class="font-medium">{contest.formal_name ?? contest.name}</span>
				<span class="text-sm text-gray-500 dark:text-gray-400">{contest.id}</span>
				<a href={resolve('/contests/[id]/teams', { id: contest.id })} class="ml-auto text-sm text-blue-600 hover:underline dark:text-blue-400">Teams</a>
			</li>
		{/each}
	</ul>
{/if}
