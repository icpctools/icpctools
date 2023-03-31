package org.icpc.tools.contest.model;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * Utility methods for the contest model and used by presentations.
 */
public class ContestUtil {
	private static final int RECENT_MS = 5 * 60 * 1000; // 5 minutes

	private static final SimpleDateFormat SDF = new SimpleDateFormat("h:mm.ss aa MMMM dd, yyyy");

	public static boolean flashPending = true;

	public static boolean isRecent(IContest c, long contestTime) {
		Long l = c.getStartTime();
		if (l == null)
			return false;

		return contestTime > (((System.currentTimeMillis() - l.longValue()) * c.getTimeMultiplier() - RECENT_MS));
	}

	/**
	 * Returns <code>true</code> if the result is recent (i.e. "interesting" - is awaiting judgement
	 * or recently changed).
	 *
	 * @param c
	 * @param r
	 * @return
	 */
	public static boolean isRecent(IContest c, IResult r) {
		if (r.getNumSubmissions() == 0)
			return false;

		return (flashPending && (r.getStatus() == Status.SUBMITTED || isRecent(c, r.getContestTime())));
	}

	/**
	 * Returns <code>true</code> if the submission is recent (i.e. "interesting" - is awaiting
	 * judgement or recently changed).
	 *
	 * @param c
	 * @param r
	 * @return
	 */
	public static boolean isRecent(IContest c, ISubmission s) {
		return (flashPending && (!c.isJudged(s) || isRecent(c, s.getContestTime())));
	}

	/**
	 * Returns true if r1 is less than r2
	 *
	 * @param r1
	 * @param r2
	 */
	public static boolean isHigherRank(String oldRank, String newRank) {
		int r1n = 999;
		int r2n = 999;
		try {
			r1n = Integer.parseInt(oldRank);
		} catch (Exception e) {
			// ignore
		}
		try {
			r2n = Integer.parseInt(newRank);
		} catch (Exception e) {
			// ignore
		}
		return r2n < r1n;
	}

	public static long getTimeInMin(long timeMs) {
		return timeMs / 60000L;
	}

	/**
	 * Format contest time as a string.
	 *
	 * @param time contest time, in ms
	 * @return
	 */
	public static String getTime(long timeMs) {
		return getTimeInMin(timeMs) + "";
	}

	/**
	 * Format contest start time as a string.
	 *
	 * @param time contest start time, in seconds
	 * @return
	 */
	public static String formatStartTime(IContest contest) {
		return formatStartTime(contest.getStartStatus());
	}

	/**
	 * Format contest start time as a string.
	 *
	 * @param time contest start time, in seconds
	 * @return
	 */
	public static String formatStartTime(Long startTime) {
		if (startTime == null)
			return "undefined";

		if (startTime < 0) {
			long countdown = startTime / 1000L;
			int hours = (int) Math.floor(-countdown / 3600);
			int mins = (int) Math.floor(-countdown / 60) % 60;
			int secs = (int) -countdown % 60;
			return "Paused at " + hours + "h" + mins + "m" + secs + "s";
		}

		return SDF.format(new Date(startTime));
	}

	/**
	 * Format a contest duration (length) or relative time as a string.
	 *
	 * @param duration a duration, in ms
	 * @return
	 */
	public static String formatDuration(Long duration) {
		if (duration == null)
			return "None";
		return formatDuration((long) duration);
	}

	/**
	 * Format a contest duration (length) or relative time as a string.
	 *
	 * @param duration a duration, in ms
	 * @return
	 */
	public static String formatDuration(long duration2) {
		long duration = duration2 / 1000;
		if (duration < 0)
			return "unknown";

		if (duration == 0)
			return "0s";

		StringBuilder sb = new StringBuilder();
		long hours = (long) Math.floor(duration / 3600);
		if (hours > 0)
			sb.append(hours + "h");

		long mins = (long) Math.floor(duration / 60) % 60;
		if (mins > 0)
			sb.append(mins + "m");

		long secs = duration % 60;
		if (secs > 0)
			sb.append(secs + "s");
		return sb.toString();
	}

	/**
	 * Format a time (duration) or relative time as a string.
	 *
	 * @param a time period, in ms
	 * @return
	 */
	public static String formatTime(long time2) {
		if (time2 >= 0 && time2 < 1000)
			return "0s";

		StringBuilder sb = new StringBuilder();
		long time = time2 / 1000;
		if (time < 0) {
			sb.append("-");
			time = -time;
		}
		int days = (int) Math.floor(time / 86400.0);
		if (days > 0)
			sb.append(days + "d");

		int hours = (int) Math.floor(time / 3600.0) % 24;
		if (hours > 0) {
			sb.append(hours + "h");
			if (days > 0)
				return sb.toString();
		}

		int mins = (int) Math.floor(time / 60.0) % 60;
		if (mins > 0)
			sb.append(mins + "m");

		int secs = (int) (time % 60);
		if (secs > 0)
			sb.append(secs + "s");
		return sb.toString();
	}

	/**
	 * Utility to sort teams by id. Ids will be sorted numerically if they are numbers, and by
	 * string compare otherwise.
	 *
	 * @param teams
	 * @return an array sorted by team id
	 */
	public static ITeam[] sort(ITeam[] teams) {
		Arrays.sort(teams, new Comparator<ITeam>() {
			@Override
			public int compare(ITeam t1, ITeam t2) {
				try {
					Integer in1 = Integer.parseInt(t1.getId());
					Integer in2 = Integer.parseInt(t2.getId());
					return in1.compareTo(in2);
				} catch (Exception e) {
					// ignore
				}
				return t1.getId().compareTo(t2.getId());
			}
		});
		return teams;
	}
}