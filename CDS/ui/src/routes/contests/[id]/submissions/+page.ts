import { error } from '@sveltejs/kit';
import { findById, findManyBySubmissionId, hasEndpointProperty } from '@icpctools/contest-api';
import { loadContest } from '$lib/contests';

export const load = async ({ params, depends }) => {
	depends('app:contest', 'app:submissions', 'app:judgements', 'app:judgement-types', 'app:teams', 'app:organizations', 'app:problems', 'app:languages');

	const cc = await loadContest(params.id);
	if (!cc) error(404, `Contest ${params.id} not found`);

	const teams = cc.getTeams();
	const orgs = cc.getOrganizations();
	const problems = cc.getProblems();
	const languages = cc.getLanguages();
	const judgements = cc.getJudgements();
	const judgementTypes = cc.getJudgementTypes();
	const submissions = cc
		.getSubmissions()
		.map((submission) => {
			const team = findById(teams, submission.team_id);
			// prefer the current judgement, falling back to the most recent one
			const js = findManyBySubmissionId(judgements, submission.id) ?? [];
			const judgement = js.findLast((j) => j.current) ?? js.at(-1);
			return {
				id: submission.id,
				submission,
				team,
				org: findById(orgs, team?.organization_id),
				problem: findById(problems, submission.problem_id),
				language: findById(languages, submission.language_id),
				judgement,
				judgementType: findById(judgementTypes, judgement?.judgement_type_id)
			};
		});

	return {
		contestId: params.id,
		contestName: cc.getContest()?.formal_name ?? cc.getContest()?.name ?? params.id,
		hasLanguage: hasEndpointProperty(cc.getAccess(), 'submissions', 'language_id'),
		submissions
	};
};
