import { invalidate } from '$app/navigation';
import { Contests, type ContestAPI, type ContestEvent } from '@icpctools/contest-api';

// The app is served by the CDS, so the Contest API is on the same origin and requests use the
// logged-in user's session
let contests: Contests | undefined;
let loading: Promise<Contests> | undefined;

export function loadContests(): Promise<Contests> {
	if (contests) return Promise.resolve(contests);

	if (!loading) {
		const c = new Contests(new URL('/api/', location.href).href);
		loading = c.loadContests().then(() => {
			contests = c;
			return c;
		});
		loading.catch(() => (loading = undefined));
	}
	return loading;
}

// The contest currently being viewed. It watches the event feed so its data is always current, and
// changes invalidate the pages that depend on them (via depends('app:<type>') in load functions)
let current: { id: string; contest: Promise<ContestAPI | undefined> } | undefined;

export function loadContest(id: string): Promise<ContestAPI | undefined> {
	if (current?.id === id) return current.contest;

	current?.contest.then((cc) => {
		cc?.removeContestListener(scheduleInvalidate);
		cc?.unwatch();
	});

	const contest = (async () => {
		const c = await loadContests();
		if (!c.contests?.some((contest) => contest.id === id)) return undefined;

		const cc = c.getContest(id);
		if (!cc) return undefined;
		await cc.watch();
		// listen only once loaded: the initial data is part of this load, and invalidating while the
		// first navigation is still loading blocks it
		cc.addContestListener(scheduleInvalidate);
		return cc;
	})();
	const entry = { id, contest };
	current = entry;
	contest.catch(() => {
		if (current === entry) current = undefined;
	});
	return contest;
}

// Invalidations are throttled (not debounced), so a steady stream of events still refreshes
// regularly instead of being starved by the timer restarting on every event
const pending = new Set<string>();
let timer: ReturnType<typeof setTimeout> | undefined;

function scheduleInvalidate(event: ContestEvent) {
	pending.add('app:' + event.type);
	if (event.id) pending.add('app:' + event.type + '/' + event.id);

	if (timer) return;
	timer = setTimeout(() => {
		timer = undefined;
		for (const key of pending) invalidate(key);
		pending.clear();
	}, 500);
}
