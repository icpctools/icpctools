import { Contests, type ContestAPI } from '@icpctools/contest-api';

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

export async function loadContest(id: string): Promise<ContestAPI | undefined> {
	const c = await loadContests();
	if (!c.contests?.some((contest) => contest.id === id)) return undefined;
	return c.getContest(id);
}
