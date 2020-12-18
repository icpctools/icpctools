package org.icpc.tools.balloon;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;

public class BalloonContest {
	private static final Collator collator = Collator.getInstance(Locale.US);

	// these need to match the columns from BalloonUtility
	public static final int SORT_ID = 0;
	public static final int SORT_SUBMISSION_ID = 1;
	public static final int SORT_PROBLEM = 2;
	public static final int SORT_TEAM_ID = 3;
	public static final int SORT_TEAM = 4;
	public static final int SORT_GROUP = 5;
	public static final int SORT_TIME = 6;
	public static final int SORT_STATUS = 7;
	public static final int SORT_PRINTED = 8;
	public static final int SORT_DELIVERED = 9;

	public static final byte YES = 0;
	public static final byte NO = 1;
	public static final byte UNKNOWN = -1;

	private IContest contest;
	private List<Balloon> balloons = new ArrayList<>();

	public synchronized Balloon getBalloon(String submissionId) {
		if (submissionId == null)
			return null;

		for (Balloon b : balloons) {
			if (submissionId.equals(b.getSubmissionId()))
				return b;
		}
		return null;
	}

	private static boolean groupsMatch(String[] groupIds1, String[] groupIds2) {
		if (groupIds1 == null || groupIds2 == null)
			return false;

		for (String id1 : groupIds1) {
			for (String id2 : groupIds2) {
				if (id1 != null && id1.equals(id2))
					return true;
			}
		}
		return false;
	}

	public synchronized byte[] getFlags(Balloon b) {
		ISubmission submission = contest.getSubmissionById(b.getSubmissionId());
		if (submission == null)
			return new byte[] { NO, NO, NO, NO };

		String tId = submission.getTeamId();
		String pId = submission.getProblemId();
		ITeam team = contest.getTeamById(tId);
		if (team == null)
			return new byte[] { NO, NO, NO, NO };
		String[] groupIds = team.getGroupIds();

		// first in contest, first in group, first for problem, first for team
		byte[] flags = new byte[] { YES, YES, YES, YES };

		ISubmission[] submissions = contest.getSubmissions();
		for (ISubmission s : submissions) {
			if (s.getContestTime() < submission.getContestTime()) {
				ITeam team2 = contest.getTeamById(s.getTeamId());
				if (team2 != null && contest.isTeamHidden(team2))
					continue;

				if (!contest.isJudged(s)) {
					if (flags[0] == YES)
						flags[0] = UNKNOWN;
					ITeam st = contest.getTeamById(s.getTeamId());
					if (flags[1] == YES && contest.getTeamById(s.getTeamId()) != null
							&& groupsMatch(groupIds, st.getGroupIds()))
						flags[1] = UNKNOWN;
					if (flags[2] == YES && s.getProblemId().equals(pId))
						flags[2] = UNKNOWN;
					if (flags[3] == YES && s.getTeamId().equals(tId))
						flags[3] = UNKNOWN;
				}
				if (contest.isSolved(s)) {
					flags[0] = NO;
					ITeam st = contest.getTeamById(s.getTeamId());
					if (contest.getTeamById(s.getTeamId()) != null && groupsMatch(groupIds, st.getGroupIds()))
						flags[1] = NO;
					if (s.getProblemId().equals(pId))
						flags[2] = NO;
					if (s.getTeamId().equals(tId))
						flags[3] = NO;
				}
			}
		}
		return flags;
	}

	interface FlagListener {
		void updatedFlags(Balloon b);
	}

	public synchronized void updateFlags(FlagListener fl) {
		for (Balloon b : balloons) {
			if (b.getFlags() < 0) {
				if (updateFlags(b)) {
					fl.updatedFlags(b);
				}
			}
		}
	}

	private boolean updateFlags(Balloon b) {
		byte[] fl = getFlags(b);
		if (fl[0] == UNKNOWN || fl[1] == UNKNOWN || fl[2] == UNKNOWN || fl[3] == UNKNOWN)
			return false;
		int flags = 0;
		if (fl[0] == YES)
			flags = Balloon.FIRST_IN_CONTEST;
		if (fl[1] == YES)
			flags |= Balloon.FIRST_IN_GROUP;
		if (fl[2] == YES)
			flags |= Balloon.FIRST_FOR_PROBLEM;
		if (fl[3] == YES)
			flags |= Balloon.FIRST_FOR_TEAM;

		if (flags == b.getFlags())
			return false;

		b.setFlags(flags);
		return true;
	}

	public synchronized void add(Balloon b) {
		balloons.add(b);
	}

	public IContest getContest() {
		return contest;
	}

	public void setContest(IContest c) {
		contest = c;
	}

	public synchronized int getNumBalloons() {
		return balloons.size();
	}

	public synchronized Balloon[] getBalloons() {
		return balloons.toArray(new Balloon[0]);
	}

	public synchronized void save() {
		try {
			BalloonFileUtil.saveBalloons(contest, balloons);
		} catch (Exception e) {
			ErrorHandler.error("Error saving balloon list", e);
		}
	}

	public synchronized void load() {
		try {
			balloons = BalloonFileUtil.loadBalloons(contest);
		} catch (Exception e) {
			ErrorHandler.error("Error loading balloon list", e);
		}
	}

	private static String getGroupLabel(IContest contest, ITeam team) {
		IGroup[] groups = contest.getGroupsByIds(team.getGroupIds());
		if (groups == null || groups.length == 0)
			return "";

		String groupName = "";
		boolean first = true;
		for (IGroup g : groups) {
			if (!first)
				groupName += ", ";
			groupName += g.getName();
			first = false;
		}
		return groupName;
	}

	public synchronized void sort(final int col, boolean increasing) {
		int in = 1;
		if (increasing)
			in = -1;
		final int inc = in;
		Comparator<Balloon> c = new Comparator<Balloon>() {
			@Override
			public int compare(Balloon b1, Balloon b2) {
				if (col == SORT_ID) {
					if (b1.getId() > b2.getId())
						return inc;
					if (b1.getId() < b2.getId())
						return -inc;
					return 0;
				} else if (col == SORT_SUBMISSION_ID) {
					return b1.getSubmissionId().compareTo(b2.getSubmissionId());
				} else if (col == SORT_PROBLEM) {
					ISubmission s1 = contest.getSubmissionById(b1.getSubmissionId());
					ISubmission s2 = contest.getSubmissionById(b2.getSubmissionId());
					if (s1 == null || s2 == null)
						return 0;

					IProblem p1 = contest.getProblemById(s1.getProblemId());
					IProblem p2 = contest.getProblemById(s2.getProblemId());
					if (p1 == null || p2 == null)
						return 0;

					return p1.getId().compareTo(p2.getId()) * inc;
				} else if (col == SORT_TEAM_ID) {
					ISubmission s1 = contest.getSubmissionById(b1.getSubmissionId());
					ISubmission s2 = contest.getSubmissionById(b2.getSubmissionId());
					if (s1 == null || s2 == null)
						return 0;

					try {
						Integer in1 = Integer.parseInt(s1.getTeamId());
						Integer in2 = Integer.parseInt(s2.getTeamId());
						return in1.compareTo(in2) * inc;
					} catch (Exception e) {
						// ignore
					}
					return s1.getTeamId().compareTo(s2.getTeamId()) * inc;
				} else if (col == SORT_TEAM) {
					ISubmission s1 = contest.getSubmissionById(b1.getSubmissionId());
					ISubmission s2 = contest.getSubmissionById(b2.getSubmissionId());
					if (s1 == null || s2 == null)
						return 0;

					ITeam t1 = contest.getTeamById(s1.getTeamId());
					ITeam t2 = contest.getTeamById(s2.getTeamId());
					if (t1 == null || t2 == null)
						return 0;

					String n1 = t1.getActualDisplayName();
					String n2 = t2.getActualDisplayName();
					return collator.compare(n1, n2) * inc;
				} else if (col == SORT_GROUP) {
					ISubmission s1 = contest.getSubmissionById(b1.getSubmissionId());
					ISubmission s2 = contest.getSubmissionById(b2.getSubmissionId());
					if (s1 == null || s2 == null)
						return 0;

					ITeam t1 = contest.getTeamById(s1.getTeamId());
					ITeam t2 = contest.getTeamById(s2.getTeamId());
					if (t1 == null || t2 == null)
						return 0;

					String g1 = getGroupLabel(contest, t1);
					String g2 = getGroupLabel(contest, t2);
					if (g1 == null || g2 == null)
						return 0;

					return collator.compare(g1, g2) * inc;
				} else if (col == SORT_TIME) {
					ISubmission s1 = contest.getSubmissionById(b1.getSubmissionId());
					ISubmission s2 = contest.getSubmissionById(b2.getSubmissionId());
					if (s1 == null || s2 == null)
						return 0;

					int t1 = s1.getContestTime();
					int t2 = s2.getContestTime();
					if (t1 > t2)
						return inc;
					if (t1 < t2)
						return -inc;
					return 0;
				} else if (col == SORT_STATUS) {
					return b1.getStatus().compareTo(b2.getStatus()) * inc;
				} else if (col == SORT_PRINTED) {
					if (b1.isPrinted() && !b2.isPrinted())
						return inc;
					if (!b1.isPrinted() && b2.isPrinted())
						return -inc;
				} else if (col == SORT_DELIVERED) {
					if (b1.isDelivered() && !b2.isDelivered())
						return inc;
					if (!b1.isDelivered() && b2.isDelivered())
						return -inc;
					return 0;
				}
				return 0;
			}
		};

		try {
			int size = balloons.size();
			for (int i = 0; i < size - 1; i++) {
				for (int j = i + 1; j < size; j++) {
					Balloon b1 = balloons.get(i);
					Balloon b2 = balloons.get(j);
					if (c.compare(b1, b2) < 0) {
						balloons.set(i, b2);
						balloons.set(j, b1);
					}
				}
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error sorting column " + col, e);
		}
	}
}