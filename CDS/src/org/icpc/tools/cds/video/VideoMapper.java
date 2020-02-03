package org.icpc.tools.cds.video;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.cds.video.VideoAggregator.ConnectionMode;
import org.icpc.tools.cds.video.VideoAggregator.Stats;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.JSONEncoder;

/**
 * Maps team video (desktop or webcam) to specific threads on the shared video aggregator.
 */
public class VideoMapper {
	private static VideoAggregator va = VideoAggregator.getInstance();
	private String namePattern;
	private int order;
	private Map<String, Integer> map = new HashMap<>();

	public static final VideoMapper WEBCAM = new VideoMapper("Webcam {0}", 1000);
	public static final VideoMapper DESKTOP = new VideoMapper("Desktop {0}", 2000);
	public static final VideoMapper AUDIO = new VideoMapper("Audio {0}", 3000);

	private VideoMapper(String namePattern, int order) {
		this.namePattern = namePattern;
		this.order = order;
	}

	public void writeStatus(JSONEncoder je) {
		je.open();
		je.openChildArray("streams");

		Stats st = new Stats();
		for (String teamId : map.keySet()) {
			int stream = map.get(teamId);
			VideoStream vi = va.getVideoInfo().get(stream);
			je.open();
			je.encode("id", teamId);
			je.encode("name", vi.getName());
			je.encode("mode", vi.getMode().name());
			je.encode("status", vi.getStatus().name());
			Stats s = vi.getStats();
			st.totalListeners += s.totalListeners;
			st.totalTime += s.totalTime;
			st.concurrentListeners += s.concurrentListeners;
			je.encode("current", s.concurrentListeners);
			je.encode("max_current", s.maxConcurrentListeners);
			je.encode("total_listeners", s.totalListeners);
			je.encode("total_time", ContestUtil.formatTime(s.totalTime));
			je.close();
		}

		je.closeArray();
		je.encode("current", st.concurrentListeners);
		je.encode("total_listeners", st.totalListeners);
		je.encode("total_time", ContestUtil.formatTime(st.totalTime));
		// je.encode("mode", "-"); // TODO
		je.close();
	}

	private static boolean isTeamOrSpare(IContest contest, ITeam team) {
		if (team == null)
			return false;

		if (!contest.isTeamHidden(team))
			return true;

		String[] groupIds = team.getGroupIds();
		int hidden = 0;
		for (String groupId : groupIds) {
			IGroup group = contest.getGroupById(groupId);
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

	public boolean isTeamMapped(String id) {
		return map.containsKey(id);
	}

	public void mapTeam(String id, String urlPattern, ConnectionMode mode) {
		synchronized (map) {
			Integer in = map.get(id);
			if (in == null) {
				int ord = order;
				try {
					ord += Integer.parseInt(id);
				} catch (Exception e) {
					// ignore
				}
				in = va.addReservation(namePattern.replace("{0}", id), urlPattern.replace("{0}", id), mode, ord);
				map.put(id, in);
				Trace.trace(Trace.INFO, "Mapped video: " + id + " -> " + in);
			}
		}
	}

	public void mapAllTeams(ConfiguredContest cc, String urlPattern, ConnectionMode mode) {
		IContest contest = cc.getContest();
		contest.addListener(new IContestListener() {
			@Override
			public void contestChanged(IContest contest2, IContestObject obj, Delta delta) {
				if (delta != Delta.ADD)
					return;

				if (obj instanceof ITeam) {
					ITeam team = (ITeam) obj;
					if (isTeamOrSpare(contest, team)) {
						mapTeam(team.getId(), urlPattern, mode);
					}
				}
			}
		});

		ITeam[] teams = contest.getTeams();
		teams = ContestUtil.sort(Arrays.copyOf(teams, teams.length));
		for (ITeam team : teams) {
			if (isTeamOrSpare(contest, team)) {
				mapTeam(team.getId(), urlPattern, mode);
			}
		}
	}

	public boolean hasVideo(String teamId) {
		return map.containsKey(teamId);
	}

	protected int getVideoStream(String teamId) throws IllegalArgumentException {
		Integer in = map.get(teamId);
		if (in == null)
			throw new IllegalArgumentException("Unrecognized team: " + teamId);
		return in.intValue();
	}

	public void setConnectionMode(ConnectionMode mode) {
		Trace.trace(Trace.INFO, "Setting connection mode: " + mode);

		for (Integer in : map.values()) {
			va.setConnectionMode(in.intValue(), mode);
		}
	}

	public void reset(int i) {
		Trace.trace(Trace.INFO, "Resetting listener " + i);
		va.reset(i);
	}

	public void resetAll() {
		Trace.trace(Trace.INFO, "Resetting listeners");

		for (Integer in : map.values()) {
			va.reset(in.intValue());
		}
	}
}