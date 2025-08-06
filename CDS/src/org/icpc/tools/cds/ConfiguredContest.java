package org.icpc.tools.cds;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.icpc.tools.cds.service.ExecutorListener;
import org.icpc.tools.cds.util.PlaybackContest;
import org.icpc.tools.cds.video.VideoAggregator;
import org.icpc.tools.cds.video.VideoAggregator.ConnectionMode;
import org.icpc.tools.cds.video.VideoStream.StreamType;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IClarification;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IPerson;
import org.icpc.tools.contest.model.IRun;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.ContestSource.ConnectionState;
import org.icpc.tools.contest.model.feed.ContestSource.ContestSourceListener;
import org.icpc.tools.contest.model.feed.DiskContestSource;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.contest.model.feed.Timestamp;
import org.icpc.tools.contest.model.internal.Account;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.Info;
import org.icpc.tools.contest.model.internal.State;
import org.icpc.tools.contest.model.internal.YamlParser;
import org.icpc.tools.contest.model.internal.account.AccountHelper;
import org.w3c.dom.Element;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.websocket.Session;

public class ConfiguredContest {
	private static final IAccount PUBLIC_ACCOUNT = new Account();

	static {
		Account ac = (Account) PUBLIC_ACCOUNT;
		ac.add("id", "public");
		ac.add("username", "public");
		ac.add("type", "public");
	}

	public enum Mode {
		ARCHIVE, PLAYBACK, LIVE
	}

	private String id;
	private String path;
	private boolean recordReactions;
	private boolean hidden;
	private CCS ccs;
	private List<Video> videos = new ArrayList<>(3);
	private Test test;
	private Boolean isTesting;
	private View view;

	private DiskContestSource contestSource;

	private Contest contest;

	private Map<String, Contest> accountContests = new HashMap<>();

	private Map<Object, String> clients = new HashMap<>();
	private long[] metrics = new long[11]; // REST, feed, ws, web, download, scoreboard, XML,
														// desktop, webcam, audio, total

	private static Map<String, Map<StreamType, List<Integer>>> streamMap = new HashMap<>();

	public static class Video {
		private Element video;

		protected Video(Element e) {
			video = e;
		}

		public String getId() {
			return CDSConfig.getString(video, "id");
		}

		public String getWebcam() {
			return CDSConfig.getString(video, "webcam");
		}

		public String getWebcamMode() {
			return CDSConfig.getString(video, "webcamMode");
		}

		public String getAudio() {
			return CDSConfig.getString(video, "audio");
		}

		public String getAudioMode() {
			return CDSConfig.getString(video, "audioMode");
		}

		public String getDesktop() {
			return CDSConfig.getString(video, "desktop");
		}

		public String getDesktopMode() {
			return CDSConfig.getString(video, "desktopMode");
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Video [");
			if (getId() != null)
				sb.append(getId() + "/");
			sb.append(getWebcam());
			if (getWebcamMode() != null)
				sb.append(":" + getWebcamMode());
			sb.append("/");
			sb.append(getDesktop());
			if (getDesktopMode() != null)
				sb.append(":" + getDesktopMode());
			sb.append("/");
			sb.append(getAudio());
			if (getAudioMode() != null)
				sb.append(":" + getAudioMode());
			sb.append("]");
			return sb.toString();
		}
	}

	public static class Test {
		private int countdown = -1;
		private long startTime = -1;
		private double multiplier = 1.0;

		protected Test(Element e) {
			Integer in = CDSConfig.getInteger(e, "countdown");
			if (in != null)
				countdown = in.intValue();

			Double d = CDSConfig.getDouble(e, "multiplier");
			if (d != null)
				multiplier = d.doubleValue();

			String st = CDSConfig.getString(e, "startTime");
			if (st != null)
				try {
					startTime = Timestamp.parse(st);
				} catch (Exception ex) {
					Trace.trace(Trace.USER, "Could not parse start time");
				}
		}

		public int getCountdown() {
			return countdown;
		}

		public long getStartTime() {
			return startTime;
		}

		public double getMultiplier() {
			return multiplier;
		}

		@Override
		public int hashCode() {
			return countdown + (int) startTime * 31 + (int) (multiplier * 31.0 * 31.0);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Test))
				return false;

			Test t = (Test) obj;
			if (t.countdown != countdown)
				return false;
			if (t.startTime != startTime)
				return false;
			if (t.multiplier != multiplier)
				return false;
			return true;
		}

		@Override
		public String toString() {
			if (startTime > 0)
				return "Playback at " + ContestUtil.formatStartTime(startTime) + " at " + getMultiplier() + "x speed";
			return "Playback in " + getCountdown() + "s at " + getMultiplier() + "x speed";
		}
	}

	public static class View {
		private String[] groups;
		private String[] problems;

		private Pattern[] groupPatterns;

		protected View(Element e) {
			String groupStr = CDSConfig.getString(e, "groupIds");
			String problemStr = CDSConfig.getString(e, "problemLabels");

			if (groupStr != null) {
				groups = groupStr.split(",");
				groupPatterns = new Pattern[groups.length];
				for (int i = 0; i < groups.length; i++)
					groupPatterns[i] = Pattern.compile(groups[i].trim());
			}
			if (problemStr != null)
				problems = problemStr.split(",");
		}

		public boolean matchesGroup(String groupId) {
			for (Pattern p : groupPatterns)
				if (p.matcher(groupId).matches())
					return true;
			return false;
		}

		public boolean matchesProblem(String problemLabel) {
			for (String pLabel : problems)
				if (pLabel.contains(problemLabel))
					return true;
			return false;
		}

		public String[] getGroups() {
			return groups;
		}

		public String[] getProblems() {
			return problems;
		}

		@Override
		public String toString() {
			if (problems != null)
				return "View[groups=" + Arrays.toString(groups) + ",problems=" + Arrays.toString(problems) + "]";

			return "View[groups=" + Arrays.toString(groups) + "]";
		}
	}

	public static class CCS {
		private String url;
		private String user;
		private String password;

		protected CCS(Element e) {
			url = CDSConfig.getString(e, "url");
			user = CDSConfig.getString(e, "user");
			password = CDSConfig.getString(e, "password");
		}

		public String getURL() {
			return url;
		}

		public String getUser() {
			return user;
		}

		public String getPassword() {
			return password;
		}

		@Override
		public String toString() {
			return getUser() + "@" + getURL();
		}
	}

	protected ConfiguredContest(Element e) {
		id = CDSConfig.getString(e, "id");

		path = CDSConfig.getString(e, "path");
		if (path == null)
			path = CDSConfig.getString(e, "location");

		recordReactions = CDSConfig.getBoolean(e, "recordReactions");
		hidden = CDSConfig.getBoolean(e, "hidden");

		Element ee = CDSConfig.getChild(e, "ccs");
		if (ee != null) {
			ccs = new CCS(ee);
			// if no id, default to last segment of url
			if (id == null && ccs.getURL() != null) {
				String url = ccs.getURL();
				int ind = url.lastIndexOf("/");
				if (ind >= 0)
					id = url.substring(ind + 1);
			}
		}

		// if no id, default to id from contest.yaml. We could wait for the contest to load,
		// but by then some data could be accessible to clients
		if (id == null && path != null) {
			try {
				File f = new File(path, "contest.yaml");
				if (f.exists()) {
					Info info = YamlParser.importContestInfo(f, false);
					id = info.getId();
				}
			} catch (Exception ex) {
				Trace.trace(Trace.WARNING, "Could not read default contest id from contest yaml", ex);
			}
		}

		// if no id, default to last folder in location
		if (id == null && path != null) {
			int ind = Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"));
			if (ind >= 0)
				id = path.substring(ind + 1);
		}

		Element[] vee = CDSConfig.getChildren(e, "video");
		if (vee != null) {
			for (Element ve : vee)
				videos.add(new Video(ve));
		}

		ee = CDSConfig.getChild(e, "test");
		if (ee != null)
			test = new Test(ee);

		ee = CDSConfig.getChild(e, "view");
		if (ee != null)
			view = new View(ee);
	}

	public static boolean isTeamOrSpare(IContest contest2, ITeam team) {
		if (team == null)
			return false;

		if (!contest2.isTeamHidden(team))
			return true;

		String[] groupIds = team.getGroupIds();
		int hidden = 0;
		for (String groupId : groupIds) {
			IGroup group = contest2.getGroupById(groupId);
			if (group != null) {
				if (group.getId() != null && group.getId().contains("spare"))
					return true;
				if (group.isHidden())
					hidden++;
			}
		}
		if (hidden == 0)
			return true;
		if (hidden == groupIds.length)
			return false;

		// some hidden and some non-hidden groups, assume this is a public team
		return true;
	}

	private static void mapStreams(String teamId, String urlPattern, ConnectionMode mode, List<String> hosts,
			Map<StreamType, List<Integer>> map, StreamType type) {
		VideoAggregator va = VideoAggregator.getInstance();
		List<Integer> in = new ArrayList<Integer>();
		map.put(type, in);

		String name = type.name() + " " + teamId;
		String url = urlPattern.replace("{0}", teamId);
		if (!hosts.isEmpty() && url.contains("{host}")) {
			for (String host : hosts) {
				String url2 = url.replace("{host}", host);
				in.add(va.addReservation(name + " " + host, url2, mode, type, teamId));
			}
		} else
			in.add(va.addReservation(name, url, mode, type, teamId));
	}

	private void setupTeamStreams(String teamId) {
		if (teamId == null || streamMap.containsKey(teamId))
			return;

		Map<StreamType, List<Integer>> map = new HashMap<StreamType, List<Integer>>();
		streamMap.put(teamId, map);

		List<String> hosts = CDSConfig.getInstance().getHostsForTeamId(teamId);

		for (Video video : videos) {
			if (video.getId() == null || video.getId().equals(teamId)) {
				String urlPattern = video.getDesktop();
				if (urlPattern != null) {
					ConnectionMode mode = VideoAggregator.getConnectionMode(video.getDesktopMode());
					mapStreams(teamId, urlPattern, mode, hosts, map, StreamType.DESKTOP);
				}

				urlPattern = video.getWebcam();
				if (urlPattern != null) {
					ConnectionMode mode = VideoAggregator.getConnectionMode(video.getWebcamMode());
					mapStreams(teamId, urlPattern, mode, hosts, map, StreamType.WEBCAM);
				}

				urlPattern = video.getAudio();
				if (urlPattern != null) {
					ConnectionMode mode = VideoAggregator.getConnectionMode(video.getAudioMode());
					mapStreams(teamId, urlPattern, mode, hosts, map, StreamType.AUDIO);
				}
			}
		}
	}

	public Map<String, Map<StreamType, List<Integer>>> getStreams() {
		return streamMap;
	}

	public Map<StreamType, List<Integer>> getStreams(IContest c, ITeam team) {
		String teamId = team.getId();
		if (videos != null && streamMap.get(teamId) == null && isTeamOrSpare(c, team))
			setupTeamStreams(teamId);
		return streamMap.get(teamId);
	}

	public List<Integer> getStreams(String teamId, StreamType type) {
		Map<StreamType, List<Integer>> map = streamMap.get(teamId);
		if (map == null)
			return null;
		return map.get(type);
	}

	public void close() {
		try {
			getContestSource().close();
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Could not close contest", e);
		}
	}

	public String getPath() {
		return path;
	}

	public String getId() {
		return id;
	}

	public boolean isRecordingReactions() {
		return recordReactions;
	}

	public boolean isHidden() {
		return hidden;
	}

	public CCS getCCS() {
		return ccs;
	}

	public String getCCSString() {
		if (ccs == null)
			return "Not configured";
		return ccs.toString();
	}

	public String getVideo() {
		if (videos.isEmpty())
			return "Not configured";
		if (videos.size() == 1)
			return videos.get(0).toString();
		return videos.size() + " configured";
	}

	public Test getTest() {
		return test;
	}

	public String getTestString() {
		if (test == null)
			return "Not configured";
		return test.toString();
	}

	public View getView() {
		return view;
	}

	public boolean isTesting() {
		if (isTesting == null) {
			isTesting = (test != null);
			if (isTesting)
				Trace.trace(Trace.USER, "----- Test mode enabled -----");
		}

		return isTesting;
	}

	public Contest getContest() {
		if (contest == null)
			loadContest();

		return contest;
	}

	private static String getKey(IAccount account) {
		if (IAccount.TEAM.equals(account.getAccountType()))
			return IAccount.TEAM + account.getTeamId();

		return account.getAccountType();
	}

	public Contest getContestByRole(HttpServletRequest request) {
		if (contest == null)
			loadContest();

		IAccount account = PUBLIC_ACCOUNT;
		String user = request.getRemoteUser();
		if (user != null) {
			List<IAccount> accounts = CDSConfig.getInstance().getAccounts();
			for (IAccount acc : accounts) {
				if (user.equals(acc.getUsername())) {
					account = acc;
				}
			}
		}

		return getContestForAccount(account);
	}

	private Contest getContestForAccount(IAccount account) {
		// return the full contest for administrators
		if (IAccount.ADMIN.equals(account.getAccountType()))
			return contest;

		// find the associated contest for every other account
		String key = getKey(account);
		Contest ac = accountContests.get(key);
		if (ac != null)
			return ac;

		// first login - create a new contest for the account type, populate the existing contest
		// data, and cache it
		synchronized (accountContests) {
			ac = accountContests.get(key);
			if (ac != null)
				return ac;

			ac = AccountHelper.createAccountContest(account);
			ac.setHashCode(contest.hashCode());

			IContestObject[] objs = contest.getObjects();
			for (IContestObject co : objs)
				ac.add(co);

			accountContests.put(key, ac);
		}
		return ac;
	}

	public Contest getContestByRole(boolean isAdmin) {
		if (contest == null)
			loadContest();

		if (isAdmin)
			return contest;

		return getContestForAccount(PUBLIC_ACCOUNT);
	}

	public String getError() {
		if (getMode() != Mode.LIVE)
			return null;

		ConnectionState conState = getContestState();
		if (conState == ConnectionState.RECONNECTING || conState == ConnectionState.FAILED)
			return "CCS connection error";
		return null;
	}

	public ConnectionState getContestState() {
		ContestSource source = getContestSource();
		if (source == null)
			return null;

		return source.getConnectionState();
	}

	public DiskContestSource getContestSource() {
		if (contestSource != null)
			return contestSource;

		if (path == null)
			return null;

		File folder = new File(path);

		if (ccs == null) {
			contestSource = new DiskContestSource(folder);
		} else {
			try {
				String name = System.getProperty("CDS-name");
				contestSource = new RESTContestSource(folder, ccs.getURL(), ccs.getUser(), ccs.getPassword(), name);
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Could not configure contest source", e);
				contestSource = new DiskContestSource(folder);
			}
		}

		contestSource.setContestId(id);
		return contestSource;
	}

	private synchronized void loadContest() {
		if (contest != null)
			return;

		try {
			ContestSource source = getContestSource();
			PlaybackContest pc = new PlaybackContest(this);
			pc.addModifier((cont, obj) -> {
				if (obj instanceof Info) {
					Info info2 = (Info) obj;
					info2.setId(id);
				}
			});
			if (isTesting())
				pc.setTestMode();

			source.addListener(new ContestSourceListener() {
				@Override
				public void stateChanged(ConnectionState state2) {
					if (ContestSource.ConnectionState.CONNECTED.equals(state2))
						pc.setConfigurationLoaded();
					if (ContestSource.ConnectionState.CONNECTING.equals(state2))
						Trace.trace(Trace.USER, "Reading contest: " + id);
					else if (ContestSource.ConnectionState.COMPLETE.equals(state2))
						Trace.trace(Trace.USER, "Done reading contest: " + id);
				}
			});

			contestSource.setInitialContest(pc);
			contest = contestSource.getContest();
			contestSource.setExecutor(ExecutorListener.getExecutor());

			if (isTesting())
				contest.setHashCode(contest.hashCode() + (int) (Math.random() * 500.0));

			// if you're an admin, staff, analyst, spectator, or balloon on the CDS, give equivalent
			// access on all contests
			List<IAccount> userAccounts = CDSConfig.getInstance().getAccounts();
			for (IAccount account : userAccounts) {
				if (IAccount.ADMIN.equals(account.getAccountType()) || IAccount.STAFF.equals(account.getAccountType())
						|| IAccount.ANALYST.equals(account.getAccountType())
						|| IAccount.SPECTATOR.equals(account.getAccountType())
						|| IAccount.BALLOON.equals(account.getAccountType()))
					contest.add(account);
			}

			State[] currentState = new State[1];
			currentState[0] = new State();
			contest.addListenerFromStart((contest2, obj, d) -> {
				synchronized (accountContests) {
					for (Contest ac : accountContests.values()) {
						ac.add(obj);
					}
				}

				if (obj instanceof ITeam) {
					ITeam team = (ITeam) obj;
					if (videos != null && streamMap.get(team.getId()) == null && isTeamOrSpare(contest, team)) {
						setupTeamStreams(team.getId());
					}
				}

				if (obj instanceof State) {
					State state2 = (State) obj;
					if (!Objects.equals(currentState[0].getStarted(), state2.getStarted()))
						Trace.trace(Trace.USER, "Contest started: " + id);
					if (!Objects.equals(currentState[0].getFrozen(), state2.getFrozen()))
						Trace.trace(Trace.USER, "Contest frozen: " + id);
					if (!Objects.equals(currentState[0].getThawed(), state2.getThawed()))
						Trace.trace(Trace.USER, "Contest thawed: " + id);
					if (!Objects.equals(currentState[0].getEnded(), state2.getEnded()))
						Trace.trace(Trace.USER, "Contest ended: " + id);
					if (!Objects.equals(currentState[0].getFinalized(), state2.getFinalized()))
						Trace.trace(Trace.USER, "Contest finalized: " + id);
					if (!Objects.equals(currentState[0].getEndOfUpdates(), state2.getEndOfUpdates()))
						Trace.trace(Trace.USER, "Contest end of updates: " + id);
					currentState[0] = state2;
				}
			});

			// wait up to 2s to connect
			if (contestSource instanceof RESTContestSource)
				contestSource.waitForContestLoad();
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error reading event feed: " + e.getMessage());
		}
	}

	protected boolean isJudgementHidden(IJudgement j) {
		if (j == null)
			return false;

		ISubmission sub = contest.getSubmissionById(j.getSubmissionId());
		if (sub == null)
			return false;

		ITeam team = contest.getTeamById(sub.getTeamId());
		return contest.isTeamHidden(team);
	}

	protected IContestObject filterHidden(IContestObject obj) {
		if (obj instanceof IGroup) {
			IGroup group = (IGroup) obj;
			if (group.isHidden())
				return null;
		} else if (obj instanceof ITeam) {
			ITeam team = (ITeam) obj;
			if (contest.isTeamHidden(team))
				return null;
		} else if (obj instanceof IPerson) {
			IPerson person = (IPerson) obj;
			ITeam team = contest.getTeamById(person.getTeamId());
			if (contest.isTeamHidden(team))
				return null;
		} else if (obj instanceof ISubmission) {
			ISubmission sub = (ISubmission) obj;
			ITeam team = contest.getTeamById(sub.getTeamId());
			if (contest.isTeamHidden(team))
				return null;
		} else if (obj instanceof IRun) {
			IRun run = (IRun) obj;
			IJudgement j = contest.getJudgementById(run.getJudgementId());
			if (isJudgementHidden(j))
				return null;
		} else if (obj instanceof IJudgement) {
			IJudgement j = (IJudgement) obj;
			if (isJudgementHidden(j))
				return null;
		} else if (obj instanceof IClarification) {
			IClarification clar = (IClarification) obj;
			ITeam team = contest.getTeamById(clar.getFromTeamId());
			if (contest.isTeamHidden(team))
				return null;
			if (clar.getToTeamIds() != null) {
				for (String teamId : clar.getToTeamIds()) {
					team = contest.getTeamById(teamId);
					if (contest.isTeamHidden(team))
						return null;
				}
			}
			if (clar.getToGroupIds() != null) {
				for (String groupId : clar.getToGroupIds()) {
					IGroup group = contest.getGroupById(groupId);
					if (group != null && group.isHidden())
						return null;
				}
			}
		}

		return obj;
	}

	public void incrementRest() {
		metrics[0]++;
		incrementTotal();
	}

	public void incrementFeed() {
		metrics[1]++;
		incrementTotal();
	}

	public void incrementWS() {
		metrics[2]++;
		incrementTotal();
	}

	public void incrementWeb() {
		metrics[3]++;
		incrementTotal();
	}

	public void incrementDownload() {
		metrics[4]++;
		incrementTotal();
	}

	public void incrementScoreboard() {
		metrics[5]++;
		incrementTotal();
	}

	public void incrementDesktop() {
		metrics[7]++;
		incrementTotal();
	}

	public void incrementWebcam() {
		metrics[8]++;
		incrementTotal();
	}

	public void incrementAudio() {
		metrics[9]++;
		incrementTotal();
	}

	private void incrementTotal() {
		metrics[10]++;
		if (metrics[10] > 500) {
			metrics[10] = 0;
			logMetrics();
		}
	}

	public long[] getMetrics() {
		return metrics;
	}

	public void logMetrics() {
		StringBuilder sb = new StringBuilder();
		sb.append("Metrics for contest " + getId() + " [");
		sb.append("REST:" + metrics[0] + ",");
		sb.append("Feed:" + metrics[1] + ",");
		sb.append("WS:" + metrics[2] + ",");
		sb.append("Web:" + metrics[3] + ",");
		sb.append("Dwnld:" + metrics[4] + ",");
		sb.append("Scr:" + metrics[5] + ",");
		sb.append("XML:" + metrics[6] + ",");
		sb.append("Desktop:" + metrics[7] + ",");
		sb.append("Webcam:" + metrics[8] + ",");
		sb.append("Audio:" + metrics[9] + ",");
		sb.append("Total:" + metrics[10] + "]");
		Trace.trace(Trace.INFO, sb.toString());

		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileWriter("stats.txt", true));
			pw.println(sb.toString());
		} catch (Exception e) {
			// ignore
		} finally {
			try {
				pw.close();
			} catch (Exception ex) {
				// ignore
			}
		}
	}

	public IAccount getAccount(String username) {
		if (username == null)
			return null;

		IAccount[] accounts = contest.getAccounts();
		if (accounts.length == 0)
			return null;

		// find matching account
		for (IAccount account : accounts) {
			if (username.equals(account.getUsername())) {
				return account;
			}
		}

		return null;
	}

	public boolean isAdmin(HttpServletRequest request) {
		IAccount account = getAccount(request.getRemoteUser());
		if (account == null)
			return false;
		String type = account.getAccountType();
		return IAccount.ADMIN.equals(type);
	}

	public boolean isStaff(HttpServletRequest request) {
		IAccount account = getAccount(request.getRemoteUser());
		if (account == null)
			return false;
		String type = account.getAccountType();
		return IAccount.ADMIN.equals(type) || IAccount.STAFF.equals(type) || IAccount.JUDGE.equals(type);
	}

	public boolean isJudge(HttpServletRequest request) {
		IAccount account = getAccount(request.getRemoteUser());
		if (account == null)
			return false;
		String type = account.getAccountType();
		return IAccount.ADMIN.equals(type) || IAccount.JUDGE.equals(type);
	}

	public boolean isAnalyst(HttpServletRequest request) {
		IAccount account = getAccount(request.getRemoteUser());
		if (account == null)
			return false;
		String type = account.getAccountType();
		return IAccount.ADMIN.equals(type) || IAccount.STAFF.equals(type) || IAccount.JUDGE.equals(type)
				|| IAccount.ANALYST.equals(type);
	}

	public boolean isBalloon(HttpServletRequest request) {
		IAccount account = getAccount(request.getRemoteUser());
		if (account == null)
			return false;
		String type = account.getAccountType();
		return IAccount.BALLOON.equals(type);
	}

	public boolean isTeam(HttpServletRequest request) {
		IAccount account = getAccount(request.getRemoteUser());
		if (account == null)
			return false;
		return IAccount.TEAM.equals(account.getAccountType());
	}

	public void add(Session session) {
		synchronized (clients) {
			try {
				String user = session.getUserPrincipal() + " @ " + session.getId();
				clients.put(session, user);
			} catch (Exception e) {
				// ignore
			}
		}
	}

	public void add(AsyncContext asyncCtx) {
		synchronized (clients) {
			try {
				HttpServletRequest request = (HttpServletRequest) asyncCtx.getRequest();
				String user = request.getRemoteUser() + " @ " + request.getRemoteHost() + " / " + request.getRemoteAddr();
				clients.put(asyncCtx, user);
			} catch (Exception e) {
				// ignore
			}
		}
	}

	public void remove(Object obj) {
		synchronized (clients) {
			clients.remove(obj);
		}
	}

	public List<String> getClients() {
		List<String> list = new ArrayList<>();
		List<Object> remove = new ArrayList<>();
		synchronized (clients) {
			for (Object obj : clients.keySet()) {
				try {
					list.add(clients.get(obj));
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error getting clients", e);
					remove(obj);
				}
			}
			for (Object obj : remove) {
				remove(obj);
			}
		}
		return list;
	}

	public Mode getMode() {
		if (test != null)
			return Mode.PLAYBACK;
		else if (ccs != null)
			return Mode.LIVE;
		return Mode.ARCHIVE;
	}

	/**
	 * Expose a contest object from during the freeze (typically judgements) to the publicly visible
	 * contests (staff, balloon, and public roles).
	 *
	 * @param co
	 */
	public void exposeContestObject(IContestObject co) {
		synchronized (accountContests) {
			for (Contest ac : accountContests.values())
				ac.add(co);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(id);
		sb.append(" on disk at " + path + " - ");
		if (ccs != null)
			sb.append("CCS configured (" + ccs + "), ");
		else
			sb.append("No CCS configured. ");
		if (test != null)
			sb.append(test + ". ");
		if (recordReactions)
			sb.append("Recording reaction videos");
		else
			sb.append("Not recording reactions");
		return sb.toString();
	}
}
