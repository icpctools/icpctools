package org.icpc.tools.presentation.contest.internal.standalone;

import java.awt.geom.Point2D;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.NetworkUtil;

public class TeamUtil {

	/**
	 * Returns the team id of the team using the local machine, by looking for known system
	 * properties, environment variables, or using heuristics on the host name or IP address.
	 * Team-identifying information is checked in the following order:
	 * <ol>
	 * <li>The TEAM_ID environment variable (e.g. "TEAM_ID=37" -> team "37")</li>
	 * <li>The TEAM_ID system property (e.g. "TEAM_ID=37" -> team "37")</li> *
	 * <li>The TEAM_LABEL environment variable (e.g. "TEAM_LABEL=37" -> team "37")</li>
	 * <li>The TEAM_LABEL system property (e.g. "TEAM_LABEL=37" -> team "37")</li>
	 * <li>The hostname. (e.g. "host73a" -> team or label "73")</li>
	 * <li>The last segment of the IP v4 host address. (e.g. "192.168.0.45" -> team or label
	 * "45")</li>
	 * </ol>
	 *
	 * @return the team id
	 */
	public static String getTeamId(IContest contest) {
		String teamId = System.getenv("TEAM_ID");
		if (teamId != null) {
			Trace.trace(Trace.INFO, "Team id found via environment variable: " + teamId);
			return teamId;
		}

		teamId = System.getProperty("TEAM_ID");
		if (teamId != null) {
			Trace.trace(Trace.INFO, "Team id found via system property: " + teamId);
			return teamId;
		}

		String teamLabel = System.getenv("TEAM_LABEL");
		if (teamLabel != null) {
			Trace.trace(Trace.INFO, "Team label found via environment variable: " + teamId);
			teamId = getTeamIdByLabel(contest, teamLabel);
			if (teamId != null)
				return teamId;
		}

		teamLabel = System.getProperty("TEAM_LABEL");
		if (teamLabel != null) {
			Trace.trace(Trace.INFO, "Team label found via system property: " + teamId);
			teamId = getTeamIdByLabel(contest, teamLabel);
			if (teamId != null)
				return teamId;
		}

		try {
			String num = getNumber(NetworkUtil.getHostName());
			Trace.trace(Trace.INFO, "Hostname: " + num);
			if (doesTeamExist(contest, num)) {
				Trace.trace(Trace.INFO, "Team id found via hostname: " + num);
				return num;
			}
			num = getTeamIdByLabel(contest, num);
			if (num != null) {
				Trace.trace(Trace.INFO, "Team label found via hostname: " + num);
				return num;
			}
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Could not determine hostname", e);
		}

		try {
			String num = getNumber(NetworkUtil.getLocalAddress());
			Trace.trace(Trace.INFO, "Local address: " + num);
			if (doesTeamExist(contest, num)) {
				Trace.trace(Trace.INFO, "Team id found via local address: " + num);
				return num;
			}
			num = getTeamIdByLabel(contest, num);
			if (num != null) {
				Trace.trace(Trace.INFO, "Team label found via local address: " + num);
				return num;
			}
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Could not determine host", e);
		}

		Trace.trace(Trace.WARNING, "Could not determine team id");
		return null;
	}

	/**
	 * Utility that bumps the location of a team to account for individual team members.
	 */
	public static Point2D getLocation(ITeam team) {
		double x = team.getX();
		double y = team.getY();

		String member = getTeamMember();
		if (member == null || (!"a".equals(member) && !"c".equals(member)))
			return new Point2D.Double(x, y);

		double r = team.getRotation();
		double rad = 0;
		if ("a".equals(member))
			rad = Math.toRadians(r + 90);
		else
			rad = Math.toRadians(r + 270);
		x += Math.cos(rad) * 1.05;
		y -= Math.sin(rad) * 1.05;
		return new Point2D.Double(x, y);
	}

	/**
	 * Find a good default for which team member is using the local machine. This is decided by
	 * looking for team-identifying information in the following order:
	 * <ol>
	 * <li>The team-member environment variable (e.g. "team-member=a" -> team "a")</li>
	 * <li>The team-member system property (e.g. "team-member37" -> team "a")</li>
	 * <li>The hostname. (e.g. "host73a" -> team "73")</li>
	 * </ol>
	 *
	 * @return the team member label
	 */
	public static String getTeamMember() {
		String prop = System.getenv("team-member");
		if (prop != null)
			return prop;

		prop = System.getProperty("team-member");
		if (prop != null)
			return prop;

		try {
			String member = getTeamMember(NetworkUtil.getHostName());
			if (member != null)
				return member;
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Could not determine host", e);
		}

		return null;
	}

	/**
	 * Returns true if we can absolutely
	 *
	 * @param contest
	 * @param teamId
	 * @return
	 */
	private static boolean doesTeamExist(IContest contest, String teamId) {
		if (contest == null)
			return true;

		if (teamId == null)
			return false;

		for (ITeam t : contest.getTeams()) {
			if (teamId.equals(t.getId()))
				return true;
		}

		return false;
	}

	private static String getTeamIdByLabel(IContest contest, String teamLabel) {
		if (contest == null || teamLabel == null)
			return null;

		for (ITeam t : contest.getTeams()) {
			if (teamLabel.equals(t.getLabel()))
				return t.getId();
		}

		return null;
	}

	/**
	 * Utility method that finds the last number within a string, by stripping alphabetic characters
	 * from either side. e.g. "some36string82a" returns "82".
	 *
	 * @param s
	 * @return a
	 */
	public static String getNumber(String s) {
		if (s == null || s.isEmpty())
			return null;
		int end = s.length() - 1;
		while (end >= 0 && Character.isAlphabetic(s.charAt(end))) {
			end--;
		}
		int start = end - 1;
		while (start >= 0 && Character.isDigit(s.charAt(start))) {
			start--;
		}

		if (start == end)
			return null;
		if (start == -1)
			return s.substring(0, end + 1);
		return s.substring(start + 1, end + 1);
	}

	/**
	 * Utility method that returns team member from a string, e.g. "some36string82a" returns "a".
	 *
	 * @param s
	 * @return
	 */
	public static String getTeamMember(String s) {
		if (s == null || s.isEmpty())
			return null;
		int start = s.length() - 1;
		while (start >= 0 && Character.isAlphabetic(s.charAt(start))) {
			start--;
		}

		if (start == s.length() - 1)
			return null;
		if (start == -1)
			return s;
		return s.substring(start + 1);
	}
}