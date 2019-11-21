package org.icpc.tools.presentation.contest.internal;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;

public class TeamUtil {
	public enum Style {
		TEAM_NAME, ORGANIZATION_NAME, ORGANIZATION_FORMAL_NAME, TEAM_AND_ORG_NAME
	}

	private static final String[] STYLE_STR = new String[] { "team_name", "org_name", "org_formal_name",
			"team_and_org_name" };

	protected static Style defaultStyle;

	public static Style getDefaultStyle(IContest contest) {
		// set default style
		String styleSt = System.getProperty("ICPC_PRESENTATION_STYLE");
		if (styleSt != null)
			return getStyleByString(styleSt);

		if (contest.getNumOrganizations() == contest.getNumTeams())
			return Style.ORGANIZATION_FORMAL_NAME;

		return Style.TEAM_AND_ORG_NAME;
	}

	public static String getTeamName(IContest contest, ITeam team) {
		if (defaultStyle == null)
			defaultStyle = getDefaultStyle(contest);

		return getTeamName(defaultStyle, contest, team);
	}

	public static String getTeamName(Style style, IContest contest, ITeam team) {
		String s = team.getActualDisplayName();
		Style style2 = style;
		if (style2 == null) {
			if (defaultStyle == null)
				defaultStyle = getDefaultStyle(contest);
			style2 = defaultStyle;
		}
		if (Style.TEAM_NAME == style2)
			return s;

		IOrganization org = contest.getOrganizationById(team.getOrganizationId());
		if (org == null)
			return s;

		if (style2 == Style.TEAM_AND_ORG_NAME)
			s = s + " (" + org.getName() + ")";
		else if (style2 == Style.ORGANIZATION_NAME)
			s = org.getName();
		else if (style2 == Style.ORGANIZATION_FORMAL_NAME)
			s = org.getFormalName();
		return s;
	}

	public static Style getStyleByString(String styleSt) {
		for (int i = 0; i < STYLE_STR.length; i++)
			if (styleSt.equalsIgnoreCase(STYLE_STR[i]))
				return Style.values()[i];
		return null;
	}
}