package org.icpc.tools.contest.model.util;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.Team;

public class TeamDisplay {
	public static void overrideDisplayName(IContest contest, String template) {
		if (template == null)
			return;

		((Contest) contest).addModifier((c, obj) -> {
			if (obj instanceof Team) {
				Team team = (Team) obj;
				String name = getTeamName(c, team, template);
				team.setDisplayName(name);
			}
		});
	}

	private static String getTeamName(IContest contest, ITeam team, String template) {
		String s = template.replace("{team.display_name}", team.getActualDisplayName());
		s = s.replace("{team.name}", team.getName());

		if (!s.contains("{org."))
			return s;

		IOrganization org = contest.getOrganizationById(team.getOrganizationId());
		if (org == null) {
			s = s.replace("{org.name}", "");
			return s = s.replace("{org.formal_name}", "");
		}
		s = s.replace("{org.name}", org.getName());
		return s.replace("{org.formal_name}", org.getActualFormalName());
	}
}