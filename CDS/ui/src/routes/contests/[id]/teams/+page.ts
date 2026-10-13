import { error } from '@sveltejs/kit';
import { findById, findManyById } from '@icpctools/contest-api';
import { loadContest } from '$lib/contests';

export const load = async ({ params }) => {
	const cc = await loadContest(params.id);
	if (!cc) error(404, `Contest ${params.id} not found`);

	await Promise.all([cc.loadContest(), cc.loadTeams(), cc.loadOrganizations(), cc.loadGroups()]);

	const orgs = cc.getOrganizations();
	const groups = cc.getGroups();
	const teams = cc.getTeams().map((team) => ({
		team,
		org: findById(orgs, team.organization_id),
		groups: findManyById(groups, team.group_ids)
	}));

	return {
		contestId: params.id,
		contestName: cc.getContest()?.formal_name ?? cc.getContest()?.name ?? params.id,
		teams
	};
};
