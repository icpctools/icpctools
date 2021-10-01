package org.icpc.tools.presentation.contest.internal.standalone;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.internal.NetworkUtil;

public class TeamUtil {

	/**
	 * Find a good default for which team is using the local machine. This is decided by looking for
	 * team-identifying information in the following order:
	 * <ol>
	 * <li>The team-id environment variable (e.g. "team-id=37" -> team "37")</li>
	 * <li>The team-id system property (e.g. "team-id=37" -> team "37")</li>
	 * <li>The hostname. (e.g. "host73a" -> team "73")</li>
	 * <li>The last segment of the IP v4 host address. (e.g. "192.168.0.45" -> team "45")</li>
	 * </ol>
	 *
	 * @return the team label
	 */
	public static String getTeamId() {
		String prop = System.getenv("team-id");
		if (prop != null)
			return prop;

		prop = System.getProperty("team-id");
		if (prop != null)
			return prop;

		try {
			String num = getTeamNumber(NetworkUtil.getHostName());
			if (num != null)
				return num;
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Could not determine host", e);
		}

		try {
			String num = getTeamNumber(NetworkUtil.getLocalAddress());
			if (num != null)
				return num;
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Could not determine host", e);
		}

		return null;
	}

	/**
	 * Find a good default for which team member is using the local machine. This is decided by looking for
	 * team-identifying information in the following order:
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
	 * Utility method that returns team number from a string, e.g.
	 * "some36string82a" returns "82".
	 *
	 * @param s
	 * @return
	 */
	public static String getTeamNumber(String s) {
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
	 * Utility method that returns team member from a string, e.g.
	 * "some36string82a" returns "a".
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