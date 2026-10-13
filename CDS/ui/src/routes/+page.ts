import { loadContests } from '$lib/contests';

export const load = async () => {
	const contests = await loadContests();
	return { contests: contests.contests ?? [] };
};
