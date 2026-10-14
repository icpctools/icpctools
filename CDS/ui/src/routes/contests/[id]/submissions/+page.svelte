<script lang="ts">
	import { parseRelTime, timeToMin } from '@icpctools/contest-api';
	import { JudgementUI, ProblemColumn, SimpleColumn, Table, TeamColumn, type Column } from '@icpctools/contest-ui';
	import type { PageData } from './$types';

	type Row = PageData['submissions'][number];

	let { data } = $props();

	const compare = (a?: string, b?: string) => (a ?? '').localeCompare(b ?? '', undefined, { numeric: true });

	const hasLanguage = () => data.hasLanguage;

	const columns: Column<Row>[] = [
		{
			title: 'Time',
			width: '5rem',
			align: 'center',
			renderer: SimpleColumn,
			rendererProps: ({ submission }) => ({ object: timeToMin(submission.contest_time) }),
			comparator: (a, b) => (parseRelTime(a.submission.contest_time) ?? 0) - (parseRelTime(b.submission.contest_time) ?? 0),
			initialOrder: 'descending'
		},
		{
			title: 'Team',
			width: '3fr',
			renderer: TeamColumn,
			rendererProps: ({ team, org }) => ({ team, logo: org?.logo, noGap: true }),
			comparator: (a, b) => compare(a.team?.display_name ?? a.team?.name, b.team?.display_name ?? b.team?.name)
		},
		{
			title: 'Problem',
			width: '6rem',
			renderer: ProblemColumn,
			rendererProps: ({ problem }) => ({ problem }),
			comparator: (a, b) => (a.problem?.ordinal ?? 0) - (b.problem?.ordinal ?? 0)
		},
		{
			title: 'Language',
			width: '1fr',
			hidden: !hasLanguage(),
			renderer: SimpleColumn,
			rendererProps: ({ language, submission }) => ({ object: language?.name ?? submission.language_id }),
			comparator: (a, b) => compare(a.language?.name, b.language?.name)
		},
		{
			title: 'Result',
			width: '5rem',
			renderer: JudgementUI,
			rendererProps: ({ judgement, judgementType }) => ({ judgement, judgementType }),
			comparator: (a, b) => compare(a.judgementType?.id, b.judgementType?.id)
		}
	];

	let filter = $state('');

	let filtered = $derived.by(() => {
		const f = filter.trim().toLowerCase();
		if (!f) return data.submissions;
		return data.submissions.filter(({ submission, team, problem, language, judgementType }) =>
			[submission.id, team?.label, team?.display_name, team?.name, problem?.label, problem?.name, language?.name, judgementType?.id, judgementType?.name]
				.some((s) => s?.toLowerCase().includes(f))
		);
	});
</script>

<svelte:head>
	<title>Submissions - {data.contestName}</title>
</svelte:head>

<div class="mb-4 flex flex-wrap items-center gap-4">
	<h1 class="text-2xl font-semibold">Submissions</h1>
	<span class="text-gray-500 dark:text-gray-400">{data.contestName}</span>
	<input
		type="search"
		placeholder="Filter"
		bind:value={filter}
		class="ml-auto rounded border border-gray-300 bg-white px-3 py-1 text-sm dark:border-gray-600 dark:bg-gray-800" />
</div>

{#if filtered.length > 0}
	<Table kind="submissions" data={filtered} {columns} defaultSortColumn="Time" />
{:else}
	<p class="py-6 text-center text-gray-500">No submissions</p>
{/if}

<p class="mt-2 text-sm text-gray-500 dark:text-gray-400">
	{filtered.length} of {data.submissions.length} submissions
</p>
