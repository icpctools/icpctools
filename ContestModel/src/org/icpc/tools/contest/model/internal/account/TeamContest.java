package org.icpc.tools.contest.model.internal.account;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IClarification;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IDelete;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IPerson;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;

/**
 * Filter that adds things teams can see compared to public:
 * <ul>
 * <li>Full details of persons that are on the team</li>
 * <li>Their own submissions and judgements (even if outside contest time)</li>
 * <li>Clarifications to or from the team, broadcasts</li>
 * </ul>
 */
public class TeamContest extends PublicContest {
	private String teamId;

	public TeamContest(IAccount account) {
		super();
		username = account.getUsername();
		teamId = account.getTeamId();

		if (teamId == null)
			throw new IllegalArgumentException("Team account can only be created for a team");
	}

	@Override
	public void add(IContestObject obj) {
		if (obj instanceof IDelete) {
			addIt(obj);
			return;
		}

		IContestObject.ContestType cType = obj.getType();

		switch (cType) {
			case PERSON: {
				IPerson person = (IPerson) obj;

				// teams see full details for anyone on the team
				String[] teamIds = person.getTeamIds();
				if (teamIds == null) {
					return;
				}

				for (String ids : teamIds) {
					if (teamId.equals(ids)) {
						super.add(person);
						return;
					}
				}

				break;
			}
			case SUBMISSION: {
				ISubmission sub = (ISubmission) obj;

				// teams always see their own submissions
				if (teamId.equals(sub.getTeamId())) {
					addIt(sub);
					return;
				}

				break;
			}
			case JUDGEMENT: {
				IJudgement j = (IJudgement) obj;

				// teams always see their own judgements
				ISubmission s = super.getSubmissionById(j.getSubmissionId());
				if (s == null)
					return;

				if (teamId.equals(s.getTeamId())) {
					addIt(j);
					return;
				}

				break;
			}
			case CLARIFICATION: {
				IClarification clar = (IClarification) obj;

				boolean isVisible = false;

				// teams see clarifications they've sent
				if (clar.getFromTeamId() != null && teamId.equals(clar.getFromTeamId())) {
					isVisible = true;
				}

				// ones sent to them
				if (!isVisible && clar.getToTeamIds() != null) {
					for (String clarTeamId : clar.getToTeamIds()) {
						if (teamId.equals(clarTeamId)) {
							isVisible = true;
							continue;
						}
					}
				}

				// and ones sent to a group they are in
				if (!isVisible && clar.getToGroupIds() != null) {
					ITeam team = getTeamById(teamId);
					String[] groupIds = team.getGroupIds();

					for (String groupa : clar.getToGroupIds()) {
						for (String groupb : groupIds) {
							if (groupa != null && groupa.equals(groupb)) {
								isVisible = true;
								continue;
							}
						}
					}
				}

				if (isVisible) {
					clar = filterClarification(clar);
					addIt(clar);
					return;
				}

				break;
			}
			default:
				break;
		}
		super.add(obj);
	}

	@Override
	public boolean allowProperty(IContestObject obj, String property) {
		switch (obj.getType()) {
			case TEAM: {
				ITeam team = (ITeam) obj;
				if (property.startsWith("desktop") || property.startsWith("webcam") || property.startsWith("audio"))
					return teamId.equals(team.getId());

				return super.allowProperty(obj, property);
			}
			case SUBMISSION: {
				ISubmission s = (ISubmission) obj;
				if (teamId.equals(s.getTeamId()))
					return true;

				return super.allowProperty(obj, property);
			}
			default:
				return super.allowProperty(obj, property);
		}
	}

	@Override
	public boolean canAccessProperty(IContestObject.ContestType type, String property) {
		switch (type) {
			case TEAM: {
				if (property.startsWith("desktop") || property.startsWith("webcam") || property.startsWith("audio"))
					return true;

				return super.canAccessProperty(type, property);
			}
			case SUBMISSION: {
				return true;
			}
			default:
				return super.canAccessProperty(type, property);
		}
	}
}