import { loadContests } from '#lib/contests.js';

export const load = async () => {
	const contests = await loadContests();
	return { contests: contests.contests ?? [] };
};
