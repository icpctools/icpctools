package org.icpc.tools.presentation.contest.internal;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;

public class TeamUtil {
	protected static String defaultStyle;

	private static final String DEFAULT_TEAM = "{team.display_name}";
	private static final String DEFAULT_TEAM_AND_ORG = "{team.display_name} ({org.name})";

	public static void setDefaultStyle(String s) {
		System.setProperty("ICPC_PRESENTATION_STYLE", s);
	}

	protected static String getDefaultStyle(IContest contest) {
		// set default style
		String styleSt = System.getProperty("ICPC_PRESENTATION_STYLE");
		if (styleSt != null)
			return styleSt;

		if (contest.getNumOrganizations() == contest.getNumTeams())
			return DEFAULT_TEAM;

		return DEFAULT_TEAM_AND_ORG;
	}

	public static String getTeamName(IContest contest, ITeam team) {
		return getTeamName(null, contest, team);
	}

	public static String getTeamName(String style, IContest contest, ITeam team) {
		String style2 = style;
		if (style == null) {
			if (defaultStyle == null)
				defaultStyle = getDefaultStyle(contest);
			style2 = defaultStyle;
		}

		String s = style2.replace("{team.display_name}", team.getActualDisplayName());
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