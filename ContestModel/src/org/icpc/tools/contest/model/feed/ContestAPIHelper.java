package org.icpc.tools.contest.model.feed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.internal.Info;

public class ContestAPIHelper {
	private static String listContest(URL url, Info info) {
		return url.toExternalForm() + " - " + info.getName() + " starting at "
				+ ContestUtil.formatStartTime(info.getStartTime());
	}

	private static URL getChildURL(URL url, String path) throws MalformedURLException {
		String u = url.toExternalForm();
		if (u.endsWith("/"))
			return new URL(u + path);

		return new URL(u + "/" + path);
	}

	/**
	 * Validate the given url and output feedback to the user. If the url is a valid contest it will
	 * be returned. If the URL points to a Contest API root and there is either one contest or
	 * several contests but one obvious 'best' contest it will return that contest. If the URL is
	 * invalid it will try to find and suggest alternate URLs and throw an exception.
	 *
	 * @param url
	 * @param user
	 * @param password
	 * @return
	 * @throws IllegalArgumentException if the URL was invalid
	 */
	public static String validateContestURL(String url2, String user, String password) throws IllegalArgumentException {
		URL url = null;
		try {
			url = new URL(url2);
			String content = getContent(url, user, password, 0);
			String name = isContestURL(content);
			if (name != null) {
				Trace.trace(Trace.USER, "Connecting to " + name + " at " + url);
				return url2;
			}

			URL theURL = url;
			if (isBaseURL(content)) {
				theURL = getChildURL(theURL, "contests");
				content = getContent(theURL, user, password, 0);
			}

			Info[] infos = null;
			try {
				infos = readContests(content, theURL);
			} catch (Exception e) {
				// could not parse
			}
			if (infos != null) {
				// only one option
				if (infos.length == 1) {
					URL newURL = getChildURL(theURL, infos[0].getId());
					Trace.trace(Trace.USER, "Only 1 contest found, connecting to " + listContest(newURL, infos[0]));
					return newURL.toExternalForm();
				}

				// otherwise try to find the best fit
				Info bestContest = pickBestContest(infos);
				if (bestContest != null) {
					URL newURL = getChildURL(theURL, bestContest.getId());
					Trace.trace(Trace.USER,
							infos.length + " contests found, auto-connecting to " + listContest(newURL, bestContest));
					return newURL.toExternalForm();
				}

				Trace.trace(Trace.USER,
						"Contest API found, but couldn't decide which contest to connect to. Try one of the following URLs:");
				for (Info info : infos)
					Trace.trace(Trace.USER, "  " + listContest(getChildURL(theURL, info.getId()), info));

				throw new IllegalArgumentException("Multiple contests found at " + url);
			}
		} catch (IllegalArgumentException e) {
			Trace.trace(Trace.INFO, "Invalid contest URL: " + e.getMessage());
			throw e;
		} catch (Exception e) {
			Trace.trace(Trace.INFO, "Invalid contest URL: " + e.getMessage());
		}

		// try alternate URLs on the same host
		String[] paths = new String[] { "api/contests", "contests", "/api/contests", "/contests",
				"/domjudge/api/contests", "/clics-api/contests" };
		for (String path : paths) {
			try {
				URL testURL = null;
				if (path.startsWith("/"))
					testURL = new URL(url.getProtocol() + "://" + url.getAuthority() + path);
				else
					testURL = getChildURL(url, path);
				String content = getContent(testURL, user, password, 0);
				Info[] infos = readContests(content, testURL);

				Trace.trace(Trace.USER, "No contest found at the given URL, but I found these instead:");
				for (Info info : infos)
					Trace.trace(Trace.USER, "  " + listContest(getChildURL(testURL, info.getId()), info));

				break;
			} catch (Exception e) {
				Trace.trace(Trace.INFO, "Check for " + path + " URL failed: " + e.getMessage());
			}
		}

		throw new IllegalArgumentException("Could not detect Contest API at " + url);
	}

	private static String getContent(URL url, String user, String password, int redirects) throws Exception {
		HttpURLConnection conn = null;

		try {
			conn = HTTPSSecurity.createConnection(url, user, password);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Connection error", e);
		}

		int response = conn.getResponseCode();

		if (response == HttpURLConnection.HTTP_UNAUTHORIZED)
			throw new IllegalArgumentException("Invalid user or password (401)");
		else if (RESTContestSource.hasMoved(response)) {
			if (redirects > 3)
				throw new IllegalArgumentException("Too many URL redirects");
			URL newURL = new URL(conn.getHeaderField("Location"));
			return getContent(newURL, user, password, redirects + 1);
		} else if (response == HttpURLConnection.HTTP_NOT_FOUND) {
			throw new IllegalArgumentException("Invalid, URL not found (404)");
		} else if (response == HttpURLConnection.HTTP_OK) {
			InputStream in = conn.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			StringBuilder sb = new StringBuilder();
			String s = br.readLine();
			while (s != null) {
				sb.append(s);
				s = br.readLine();
			}

			return sb.toString();
		}

		throw new IllegalArgumentException("Invalid response code (" + response + ")");
	}

	private static String isContestURL(String s) {
		try {
			JSONParser parser2 = new JSONParser(s);
			JsonObject obj = parser2.readObject();
			if (obj.getString("id") != null && obj.getString("name") != null && obj.getString("duration") != null)
				return obj.getString("name"); // confirmed good contest, return name
		} catch (Exception e) {
			// ignore, not an object
		}

		return null;
	}

	private static boolean isBaseURL(String s) {
		try {
			JSONParser parser2 = new JSONParser(s);
			JsonObject obj = parser2.readObject();
			if (obj.getString("version") != null && obj.getString("version_url") != null)
				return true; // confirmed good / Contest API endpoint
		} catch (Exception e) {
			// ignore, not an object
		}

		return false;
	}

	private static Info[] readContests(String s, URL theURL) throws Exception {
		JSONParser parser2 = new JSONParser(s);
		Object[] arr = parser2.readArray();

		if (arr.length == 0)
			throw new IllegalArgumentException("Possible Contest API at " + theURL + " but no contests found");

		Info[] infos = new Info[arr.length];
		for (int i = 0; i < arr.length; i++) {
			JsonObject obj = (JsonObject) arr[i];
			Info info = new Info();
			infos[i] = info;
			for (String key : obj.props.keySet())
				info.add(key, obj.props.get(key));
		}
		return infos;
	}

	public static Info pickBestContest(Info[] infos) {
		if (infos == null || infos.length == 0)
			return null;

		// if there's only one contest, pick it
		int numContests = infos.length;
		if (numContests == 1)
			return infos[0];

		// TODO: pick any paused contest. since we can't tell yet which contests
		// are paused, in the meantime pick any contest with no start time, as that's likely paused
		/*
		for (int i = 0; i < infos.length; i++) {
			// if paused, pick this one
		}*/
		Info paused = null;
		for (int i = 0; i < numContests; i++) {
			if (infos[i].getStartTime() == null) {
				if (paused != null) {
					Trace.trace(Trace.INFO, "Multiple contests don't have a start time, could not pick one");
					return null;
				}
				paused = infos[i];
			}
		}
		if (paused != null)
			return paused;

		// ok, so there are multiple contests, and none of them are paused.
		// let's start by figuring out the best contest(s) that are before, during, or
		// after the current time
		Info next = null;
		long timeUntilNext = Long.MAX_VALUE;
		boolean nextIsDup = false;
		Info during = null;
		Info previous = null;
		long timeSincePrevious = Long.MAX_VALUE;
		boolean previousIsDup = false;

		long now = System.currentTimeMillis();
		for (int i = 0; i < numContests; i++) {
			Info info = infos[i];

			if (now < info.getStartTime()) {
				// before the contest
				long timeUntilStart = info.getStartTime() - now;
				Trace.trace(Trace.INFO, "Next contest: " + timeUntilStart + " " + info.getId());
				if (timeUntilStart == timeUntilNext)
					nextIsDup = true;
				else if (timeUntilStart < timeUntilNext) {
					next = info;
					timeUntilNext = timeUntilStart;
					nextIsDup = false;
				}
			} else if (now < info.getStartTime() + info.getDuration()) {
				// during
				Trace.trace(Trace.INFO, "During contest: " + info.getId());
				if (during != null) {
					Trace.trace(Trace.ERROR, "Multiple contests are running, can't pick between them");
					return null;
				}
				during = info;
			} else {
				// after the contest
				long timeSince = now - (info.getStartTime() + info.getDuration());
				Trace.trace(Trace.INFO, "Previous contest: " + timeSince + " " + info.getId());
				if (timeSince == timeSincePrevious)
					previousIsDup = true;
				else if (timeSince < timeSincePrevious) {
					previous = info;
					timeSincePrevious = timeSince;
					previousIsDup = false;
				}
			}
		}

		// if we're during the one and only running contest, pick it
		if (during != null)
			return during;

		// if we're before all contests, pick the first one
		if (next != null && previous == null) {
			if (nextIsDup) { // unless the first two start at the same time
				Trace.trace(Trace.INFO, "The next two contests start at the same time, can't pick between them");
				return null;
			}
			return next;
		}

		// if we're after all contests, pick the last one
		if (previous != null && next == null) {
			if (previousIsDup) { // unless the previous two ended at the same time
				Trace.trace(Trace.INFO, "The previous two contests ended at the same time, can't pick between them");
				return null;
			}
			return previous;
		}

		// ok, so we're between two (or more) contests. if the previous contest ended more than 2h
		// ago or we're closer (weighted x 2) to the next one, switch to the next contest.
		// if the direction we chose to go has more than one contest, give up
		if (timeSincePrevious > 120 * 1000 || timeUntilNext < timeSincePrevious * 2) {
			// pick the next contest
			if (nextIsDup) { // unless the next two start at the same time
				Trace.trace(Trace.INFO, "The next two contests start at the same time, can't pick between them");
				return null;
			}
			return next;
		}

		// not close to the next contest, stick with the previous one
		if (previousIsDup) { // unless the previous two ended at the same time
			Trace.trace(Trace.INFO, "The previous two contests ended at the same time, can't pick between them");
			return null;
		}
		return previous;
	}
}