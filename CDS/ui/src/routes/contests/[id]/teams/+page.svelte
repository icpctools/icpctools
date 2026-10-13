<script lang="ts">
	import Logo from '@icpctools/contest-ui/Logo';

	let { data } = $props();

	let filter = $state('');

	let filtered = $derived.by(() => {
		const f = filter.trim().toLowerCase();
		if (!f) return data.teams;
		return data.teams.filter(({ team, org, groups }) =>
			[team.label, team.display_name, team.name, org?.formal_name, org?.name, ...(groups?.map((g) => g.name) ?? [])]
				.some((s) => s?.toLowerCase().includes(f))
		);
	});
</script>

<svelte:head>
	<title>Teams - {data.contestName}</title>
</svelte:head>

<div class="mb-4 flex flex-wrap items-center gap-4">
	<h1 class="text-2xl font-semibold">Teams</h1>
	<span class="text-gray-500 dark:text-gray-400">{data.contestName}</span>
	<input
		type="search"
		placeholder="Filter"
		bind:value={filter}
		class="ml-auto rounded border border-gray-300 bg-white px-3 py-1 text-sm dark:border-gray-600 dark:bg-gray-800" />
</div>

<div class="overflow-x-auto rounded border border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-800">
	<table class="w-full text-sm">
		<thead class="bg-gray-100 text-left dark:bg-gray-700">
			<tr>
				<th class="px-3 py-2 text-right">Label</th>
				<th class="w-8 px-3 py-2"></th>
				<th class="px-3 py-2">Name</th>
				<th class="px-3 py-2">Organization</th>
				<th class="px-3 py-2">Groups</th>
			</tr>
		</thead>
		<tbody class="divide-y divide-gray-200 dark:divide-gray-700">
			{#each filtered as { team, org, groups } (team.id)}
				<tr class="hover:bg-gray-50 dark:hover:bg-gray-700/50">
					<td class="px-3 py-1.5 text-right tabular-nums">{team.label}</td>
					<td class="px-3 py-1.5"><Logo ref={org?.logo} /></td>
					<td class="px-3 py-1.5">
						{team.display_name ?? team.name}
						{#if team.hidden}<span class="ml-2 rounded bg-gray-200 px-1.5 text-xs text-gray-600 dark:bg-gray-600 dark:text-gray-300">hidden</span>{/if}
					</td>
					<td class="px-3 py-1.5">{org?.formal_name ?? org?.name ?? ''}</td>
					<td class="px-3 py-1.5">{groups?.map((g) => g.name).join(', ')}</td>
				</tr>
			{:else}
				<tr><td colspan="5" class="px-3 py-6 text-center text-gray-500">No teams</td></tr>
			{/each}
		</tbody>
	</table>
</div>

<p class="mt-2 text-sm text-gray-500 dark:text-gray-400">
	{filtered.length} of {data.teams.length} teams
</p>
