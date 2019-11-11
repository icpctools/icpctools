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
	 * <li>The hostname. (e.g. "host73" -> team "73")</li>
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
			String num = getNumberFromEnd(NetworkUtil.getHostName());
			if (num != null)
				return num;
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Could not determine host", e);
		}

		try {
			String num = getNumberFromEnd(NetworkUtil.getLocalAddress());
			if (num != null)
				return num;
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Could not determine host", e);
		}

		return null;
	}

	/**
	 * Utility method that returns any numeric digits from the end of a string, e.g.
	 * "some36string82" returns "82".
	 *
	 * @param s
	 * @return
	 */
	public static String getNumberFromEnd(String s) {
		if (s == null || s.isEmpty())
			return null;
		int n = s.length() - 1;
		while (n >= 0 && Character.isDigit(s.charAt(n))) {
			n--;
		}

		if (n == s.length() - 1)
			return null;
		if (n == -1)
			return s;
		return s.substring(n + 1, s.length());
	}
}