<script lang="ts">
	import { SimpleColumn, Table, TeamColumn, type Column } from '@icpctools/contest-ui';
	import type { PageData } from './$types';

	type Row = PageData['teams'][number];

	let { data } = $props();

	const compare = (a?: string, b?: string) => (a ?? '').localeCompare(b ?? '', undefined, { numeric: true });

	const columns: Column<Row>[] = [
		{
			title: 'Label',
			width: '5rem',
			align: 'center',
			renderer: SimpleColumn,
			rendererProps: ({ team }) => ({ object: team.label }),
			comparator: (a, b) => compare(a.team.label, b.team.label)
		},
		{
			title: 'Name',
			width: '3fr',
			renderer: TeamColumn,
			rendererProps: ({ team, org }) => ({ team, logo: org?.logo, noGap: true }),
			comparator: (a, b) => compare(a.team.display_name ?? a.team.name, b.team.display_name ?? b.team.name)
		},
		{
			title: '',
			width: '4rem',
			renderer: SimpleColumn,
			rendererProps: ({ team }) => ({ object: team.hidden ? 'hidden' : '' })
		},
		{
			title: 'Organization',
			width: '3fr',
			renderer: SimpleColumn,
			rendererProps: ({ org }) => ({ object: org?.formal_name ?? org?.name ?? '' }),
			comparator: (a, b) => compare(a.org?.formal_name ?? a.org?.name, b.org?.formal_name ?? b.org?.name)
		},
		{
			title: 'Groups',
			width: '2fr',
			renderer: SimpleColumn,
			rendererProps: ({ groups }) => ({ object: groups?.map((g) => g.name).join(', ') ?? '' })
		}
	];

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

{#if filtered.length > 0}
	<Table kind="teams" data={filtered} {columns} defaultSortColumn="Label" />
{:else}
	<p class="py-6 text-center text-gray-500">No teams</p>
{/if}

<p class="mt-2 text-sm text-gray-500 dark:text-gray-400">
	{filtered.length} of {data.teams.length} teams
</p>
