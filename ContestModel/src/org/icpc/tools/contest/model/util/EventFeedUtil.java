package org.icpc.tools.contest.model.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IJudgementType;
import org.icpc.tools.contest.model.ILanguage;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.Info;

/**
 * Event feed utility. Provides summary info or compares two event feeds.
 */
public class EventFeedUtil {
	public enum DiffType {
		DIFF_OK, DIFF_BAD, SAME, SOURCE_ONLY, TARGET_ONLY
	}

	public static class CompareFull {
		public DiffType[][] diff;
		public String[] attrs;
		public List<String> ids = new ArrayList<String>();
		public String[][][] vals;
		public int[] num;
		public int[] only;
		public int different;
	}

	public static class CompareCount {
		List<String> messages = new ArrayList<>();
		int numSource;
		int numTarget;
		int sourceOnly;
		int targetOnly;
		int different;

		public boolean isOk() {
			return sourceOnly + targetOnly + different == 0;
		}

		public void add(String id, String message) {
			if (id != null)
				messages.add(id + ": " + message);
			else
				messages.add(message);
		}

		public void printSingletonSummary(String s, boolean includeDetails) {
			Trace.trace(Trace.USER, printSingletonSummary2(s, includeDetails));
		}

		public String printSingletonSummaryHTML() {
			StringBuilder sb = new StringBuilder();
			if (!isOk())
				sb.append("<font color=red>");

			String st = printSingletonSummary2(null, true);
			String[] ss = st.split("\n");
			for (String s2 : ss)
				sb.append(s2 + "<br/>");
			if (!isOk())
				sb.append("</font>");
			return sb.toString();
		}

		public String printSingletonSummary2(String s, boolean includeDetails) {
			StringBuilder sb = new StringBuilder();
			if (s != null)
				sb.append(s + ": ");

			if (isOk()) {
				if (numSource == 0 && numTarget == 0)
					sb.append("None in source or target.\n");
				else
					sb.append("Identical.\n");
			} else {
				sb.append(different + " different.\n");

				if (includeDetails) {
					for (String m : messages)
						sb.append("  " + m + "\n");
				}
			}
			sb.append("\n");
			return sb.toString();
		}

		public void printSummary(String s, boolean includeDetails) {
			Trace.trace(Trace.USER, printSummary2(s, includeDetails));
		}

		public String printSummaryHTML() {
			StringBuilder sb = new StringBuilder();
			if (!isOk())
				sb.append("<font color=red>");

			String st = printSummary2(null, true);
			String[] ss = st.split("\n");
			for (String s2 : ss)
				sb.append(s2 + "<br/>");
			if (!isOk())
				sb.append("</font>");
			return sb.toString();
		}

		public String printSummary2(String s, boolean includeDetails) {
			StringBuilder sb = new StringBuilder();
			if (s != null)
				sb.append(s + ": ");

			if (isOk()) {
				if (numSource == 0)
					sb.append("None in source or target.\n");
				else
					sb.append("All " + numSource + " are identical.\n");
			} else {
				int numIdentical = numSource - sourceOnly - different;
				sb.append(numSource + " in source, " + numTarget + " in target. " + (numIdentical) + " identical, "
						+ sourceOnly + " source only, " + targetOnly + " target only, " + different + " different.\n");

				if (includeDetails) {
					for (String m : messages)
						sb.append("   " + m + "\n");
				}
			}
			sb.append("\n");
			return sb.toString();
		}
	}

	private static void sourceOnly(CompareCount cc, String id) {
		sourceOnly(cc, id, "");
	}

	private static void sourceOnly(CompareCount cc, String id, String m) {
		cc.add(id, "Source only " + m);
		cc.sourceOnly++;
	}

	private static void targetOnly(CompareCount cc, String id) {
		targetOnly(cc, id, "");
	}

	private static void targetOnly(CompareCount cc, String id, String m) {
		cc.add(id, "Target only " + m);
		cc.targetOnly++;
	}

	private static void different(CompareCount cc, String id, String field, Object a, Object b) {
		cc.add(id, "Different " + field + " (" + a + " vs " + b + ")");
	}

	public static boolean compare(Contest c1, Contest c2, boolean summaryOnly) {
		boolean ok = true;

		CompareCount cc = compareInfo(c1, c2);
		cc.printSingletonSummary("Contest", !summaryOnly);
		if (!cc.isOk())
			ok = false;

		cc = compareLanguages(c1, c2);
		cc.printSummary("Languages", !summaryOnly);
		if (!cc.isOk())
			ok = false;

		cc = compareProblems(c1, c2);
		cc.printSummary("Problems", !summaryOnly);
		if (!cc.isOk())
			ok = false;

		cc = compareJudgementTypes(c1, c2);
		cc.printSummary("Judgement Types", !summaryOnly);
		if (!cc.isOk())
			ok = false;

		cc = compareGroups(c1, c2);
		cc.printSummary("Groups", !summaryOnly);
		if (!cc.isOk())
			ok = false;

		cc = compareOrganizations(c1, c2);
		cc.printSummary("Organizations", !summaryOnly);
		if (!cc.isOk())
			ok = false;

		cc = compareTeams(c1, c2);
		cc.printSummary("Teams", !summaryOnly);
		if (!cc.isOk())
			ok = false;

		cc = compareSubmissions(c1, c2);
		cc.printSummary("Submissions", !summaryOnly);
		if (!cc.isOk())
			ok = false;

		cc = compareJudgements(c1, c2);
		cc.printSummary("Submission Judgements", !summaryOnly);
		if (!cc.isOk())
			ok = false;

		cc = compareAwards(c1, c2);
		cc.printSingletonSummary("Awards", !summaryOnly);
		if (!cc.isOk())
			ok = false;

		return ok;
	}

	public static CompareCount compareInfo(Contest c1, Contest c2) {
		CompareCount cc = new CompareCount();

		Info i1 = c1.getInfo();
		Info i2 = c2.getInfo();
		if (i1 == null && i2 == null) {
			// none
		} else if (i1 == null)
			sourceOnly(cc, "Info");
		else if (i2 == null)
			targetOnly(cc, "Info");
		else {
			cc.numSource++;
			cc.numTarget++;
			compareProperties(cc, "Info", i1, i2);
		}

		return cc;
	}

	public static CompareCount compareLanguages(Contest c1, Contest c2) {
		CompareCount cc = new CompareCount();
		ILanguage[] lang1 = c1.getLanguages();
		ILanguage[] lang2 = c2.getLanguages();
		for (ILanguage l1 : lang1) {
			String id = l1.getId();
			cc.numSource++;
			ILanguage l2 = c2.getLanguageById(id);
			if (l2 == null)
				sourceOnly(cc, "Language " + id);
			else
				compareProperties(cc, "Language " + id, l1, l2);
		}
		for (ILanguage l2 : lang2) {
			String id = l2.getId();
			cc.numTarget++;
			if (c1.getLanguageById(id) == null) {
				targetOnly(cc, "Language " + id);
			}
		}

		return cc;
	}

	public static CompareCount compareGroups(Contest c1, Contest c2) {
		CompareCount cc = new CompareCount();
		IGroup[] groups1 = c1.getGroups();
		IGroup[] groups2 = c2.getGroups();
		for (IGroup g1 : groups1) {
			String id = g1.getId();
			cc.numSource++;
			IGroup g2 = c2.getGroupById(id);
			if (g2 == null)
				sourceOnly(cc, "Group " + id);
			else
				compareProperties(cc, "Group " + id, g1, g2);
		}
		for (IGroup g2 : groups2) {
			String id = g2.getId();
			cc.numTarget++;
			if (c1.getGroupById(id) == null) {
				targetOnly(cc, "Group " + id);
			}
		}

		return cc;
	}

	public static CompareCount compareProblems(Contest c1, Contest c2) {
		CompareCount cc = new CompareCount();
		IProblem[] probs1 = c1.getProblems();
		IProblem[] probs2 = c2.getProblems();
		for (IProblem p1 : probs1) {
			String id = p1.getId();
			cc.numSource++;
			IProblem p2 = c2.getProblemById(id);
			if (p2 == null)
				sourceOnly(cc, "Problem " + id);
			else
				compareProperties(cc, "Problem " + id, p1, p2);
		}
		for (IProblem p2 : probs2) {
			String id = p2.getId();
			cc.numTarget++;
			if (c1.getProblemById(id) == null) {
				targetOnly(cc, "Problem " + id);
			}
		}

		return cc;
	}

	public static CompareCount compareJudgementTypes(Contest c1, Contest c2) {
		CompareCount cc = new CompareCount();
		IJudgementType[] types1 = c1.getJudgementTypes();
		IJudgementType[] types2 = c2.getJudgementTypes();
		for (IJudgementType t1 : types1) {
			String id = t1.getId();
			cc.numSource++;
			IJudgementType t2 = c2.getJudgementTypeById(id);
			if (t2 == null)
				sourceOnly(cc, "Judgement Type " + id);
			else
				compareProperties(cc, "Judgement Type " + id, t1, t2);
		}
		for (IJudgementType p2 : types2) {
			String id = p2.getId();
			cc.numTarget++;
			if (c1.getJudgementTypeById(id) == null) {
				targetOnly(cc, "Judgement Type " + id);
			}
		}

		return cc;
	}

	public static CompareCount compareTeams(Contest c1, Contest c2) {
		CompareCount cc = new CompareCount();
		ITeam[] teams1 = c1.getTeams();
		ITeam[] teams2 = c2.getTeams();
		for (ITeam t1 : teams1) {
			String id = t1.getId();
			cc.numSource++;
			ITeam t2 = c2.getTeamById(id);
			if (t2 == null)
				sourceOnly(cc, "Team " + id);
			else
				compareProperties(cc, "Team " + id, t1, t2);
		}
		for (ITeam t2 : teams2) {
			String id = t2.getId();
			cc.numTarget++;
			if (c1.getTeamById(id) == null) {
				targetOnly(cc, "Team " + id);
			}
		}

		return cc;
	}

	public static CompareCount compareOrganizations(Contest c1, Contest c2) {
		CompareCount cc = new CompareCount();
		IOrganization[] org1 = c1.getOrganizations();
		IOrganization[] org2 = c2.getOrganizations();
		for (IOrganization o1 : org1) {
			String id = o1.getId();
			cc.numSource++;
			IOrganization o2 = c2.getOrganizationById(id);
			if (o2 == null)
				sourceOnly(cc, "Organization " + id);
			else
				compareProperties(cc, "Organization " + id, o1, o2);
		}
		for (IOrganization o2 : org2) {
			String id = o2.getId();
			cc.numTarget++;
			if (c1.getOrganizationById(id) == null) {
				targetOnly(cc, "Organization " + id);
			}
		}

		return cc;
	}

	private static void compareProperties(CompareCount cc, String key, IContestObject co1, IContestObject co2) {
		boolean same = true;
		Map<String, Object> props1 = co1.getProperties();
		Map<String, Object> props2 = co2.getProperties();
		for (String k1 : props1.keySet()) {
			if (props2.get(k1) == null) {
				sourceOnly(cc, key, " property " + k1 + "=" + props1.get(k1));
				same = false;
			}
		}
		for (String k2 : props2.keySet()) {
			if (props1.get(k2) == null) {
				targetOnly(cc, key, " property " + k2 + "=" + props2.get(k2));
				same = false;
			}
		}
		for (String k1 : props1.keySet()) {
			Object p1 = props1.get(k1);
			Object p2 = props2.get(k1);
			if (p2 != null && !props1.get(k1).equals(p2)) {
				different(cc, key, k1, p1, p2);
				same = false;
			}
		}

		if (!same)
			cc.different++;
	}

	public static CompareCount compareSubmissions(Contest c1, Contest c2) {
		CompareCount cc = new CompareCount();
		ISubmission[] submissions1 = c1.getSubmissions();
		ISubmission[] submissions2 = c2.getSubmissions();
		for (ISubmission s1 : submissions1) {
			String id = s1.getId();
			cc.numSource++;
			ISubmission s2 = c2.getSubmissionById(id);
			if (s2 == null)
				sourceOnly(cc, "Submission " + id);
			else
				compareProperties(cc, "Submission " + id, s1, s2);
		}
		for (ISubmission s2 : submissions2) {
			String id = s2.getId();
			cc.numTarget++;
			if (c1.getSubmissionById(id) == null) {
				targetOnly(cc, "Submission " + id);
			}
		}

		return cc;
	}

	public static CompareCount compareJudgements(Contest c1, Contest c2) {
		CompareCount cc = new CompareCount();
		IJudgement[] submissions1 = c1.getJudgements();
		IJudgement[] submissions2 = c2.getJudgements();
		for (IJudgement s1 : submissions1) {
			String id = s1.getId();
			cc.numSource++;
			IJudgement s2 = c2.getJudgementById(id);
			if (s2 == null)
				sourceOnly(cc, "Submission Judgement " + id);
			else
				compareProperties(cc, "Submission Judgement " + id, s1, s2);
		}
		for (IJudgement s2 : submissions2) {
			String id = s2.getId();
			cc.numTarget++;
			if (c1.getJudgementById(id) == null) {
				targetOnly(cc, "Submission Judgement " + id);
			}
		}

		return cc;
	}

	public static CompareCount compareAwards(Contest c1, Contest c2) {
		CompareCount cc = new CompareCount();
		IAward[] awards1 = c1.getAwards();
		IAward[] awards2 = c2.getAwards();
		for (IAward a1 : awards1) {
			String id = a1.getId();
			cc.numSource++;
			IAward a2 = c2.getAwardById(id);
			if (a2 == null)
				sourceOnly(cc, "Award " + id);
			else
				compareProperties(cc, "Award " + id, a1, a2);
		}
		for (IAward a2 : awards2) {
			String id = a2.getId();
			cc.numTarget++;
			if (c1.getAwardById(id) == null) {
				targetOnly(cc, "Award " + id);
			}
		}

		return cc;
	}

	interface ITemp {
		String[] getAttrs();

		IContestObject[] getContestObjs(Contest c);

		IContestObject getObj(Contest c, String id);

		String[] getVars(Contest c, IContestObject obj);
	}

	public static CompareFull compareAwardsFull2(Contest... contests) {
		return compareFull(new ITemp() {
			@Override
			public String[] getAttrs() {
				return new String[] { "Citation", "Type", "Teams" };
			}

			@Override
			public IContestObject[] getContestObjs(Contest c) {
				return c.getAwards();
			}

			@Override
			public IContestObject getObj(Contest c, String id) {
				return c.getAwardById(id);
			}

			@Override
			public String[] getVars(Contest c, IContestObject obj) {
				IAward a = (IAward) obj;
				String teamStr = "";
				// TODO sort team ids
				for (String id : a.getTeamIds()) {
					if (teamStr.length() > 0)
						teamStr += "<br/>";
					ITeam t = c.getTeamById(id);
					teamStr += t.getId() + ": " + t.getName();
				}
				return new String[] { a.getCitation(), a.getAwardType().getName(), teamStr };
			}
		}, contests);
	}

	public static CompareFull compareProblemsFull(Contest... contests) {
		return compareFull(new ITemp() {
			@Override
			public String[] getAttrs() {
				return new String[] { "Label", "Name", "Ordinal", "Color", "RGB", "Test Data Count" };
			}

			@Override
			public IContestObject[] getContestObjs(Contest c) {
				return c.getProblems();
			}

			@Override
			public IContestObject getObj(Contest c, String id) {
				return c.getProblemById(id);
			}

			@Override
			public String[] getVars(Contest c, IContestObject obj) {
				IProblem p = (IProblem) obj;
				return new String[] { p.getLabel(), p.getName(), p.getOrdinal() + "", p.getColor(), p.getRGB(),
						p.getTestDataCount() + "" };
			}
		}, contests);
	}

	public static CompareFull compareLanguagesFull(Contest... contests) {
		return compareFull(new ITemp() {
			@Override
			public String[] getAttrs() {
				return new String[] { "Name" };
			}

			@Override
			public IContestObject[] getContestObjs(Contest c) {
				return c.getLanguages();
			}

			@Override
			public IContestObject getObj(Contest c, String id) {
				return c.getLanguageById(id);
			}

			@Override
			public String[] getVars(Contest c, IContestObject obj) {
				return new String[] { ((ILanguage) obj).getName() };
			}
		}, contests);
	}

	private static CompareFull compareFull(ITemp temp, Contest... contests) {
		CompareFull cc = new CompareFull();
		cc.attrs = temp.getAttrs();
		int numAttr = cc.attrs.length;

		for (Contest c : contests) {
			for (IContestObject co : temp.getContestObjs(c))
				if (!cc.ids.contains(co.getId()))
					cc.ids.add(co.getId());
		}

		int numContests = contests.length;
		cc.vals = new String[numContests][cc.ids.size()][numAttr];
		cc.num = new int[numContests];
		cc.only = new int[numContests];
		int numVars = cc.vals[0][0].length;
		cc.diff = new DiffType[cc.ids.size()][numAttr];

		for (int j = 0; j < cc.ids.size(); j++) {
			int num = 0;
			for (int i = 0; i < numContests; i++) {
				IContestObject co = temp.getObj(contests[i], cc.ids.get(j));
				if (co != null) {
					cc.num[i]++;
					num++;
					cc.vals[i][j] = temp.getVars(contests[i], co);
				}
			}
			if (num == 1) {
				for (int i = 0; i < numContests; i++) {
					IAward a = contests[i].getAwardById(cc.ids.get(j));
					if (a != null) {
						cc.only[i]++;
					}
				}
			}
			for (int k = 0; k < numVars; k++) {
				String s = cc.vals[0][j][k];
				boolean diff = false;
				for (int i = 1; i < numContests; i++) {
					if (!s.equals(cc.vals[i][j][k]))
						diff = true;
				}
				if (diff) {
					cc.diff[j][k] = DiffType.DIFF_BAD;
					cc.different++;
				}
			}
		}

		return cc;
	}

	public static CompareFull compareAwardsFull(Contest... contests) {
		CompareFull cc = new CompareFull();
		cc.attrs = new String[] { "Citation", "Type", "Teams" };

		for (Contest c : contests) {
			for (IAward a : c.getAwards())
				if (!cc.ids.contains(a.getId()))
					cc.ids.add(a.getId());
		}

		int numContests = contests.length;
		cc.vals = new String[contests.length][cc.ids.size()][3];
		cc.num = new int[numContests];
		cc.only = new int[numContests];

		for (int j = 0; j < cc.ids.size(); j++) {
			int num = 0;
			for (int i = 0; i < numContests; i++) {
				IAward a = contests[i].getAwardById(cc.ids.get(j));
				if (a != null) {
					cc.num[i]++;
					num++;
					String teamStr = "";
					// TODO sort team ids
					for (String id : a.getTeamIds()) {
						if (teamStr.length() > 0)
							teamStr += "<br/>";
						ITeam t = contests[i].getTeamById(id);
						teamStr += t.getId() + ": " + t.getName();
					}
					cc.vals[i][j] = new String[] { a.getCitation(), a.getAwardType().getName(), teamStr };
				}
			}
			if (num == 1) {
				for (int i = 0; i < numContests; i++) {
					IAward a = contests[i].getAwardById(cc.ids.get(j));
					if (a != null) {
						cc.only[i]++;
					}
				}
			}
		}

		diff(cc);
		return cc;
	}

	private static void diff(CompareFull cf) {
		int numContests = cf.vals.length;
		int numVars = cf.vals[0][0].length;
		cf.diff = new DiffType[cf.ids.size()][3];
		for (int i = 0; i < cf.ids.size(); i++) {
			for (int k = 0; k < numVars; k++) {
				String s = cf.vals[0][i][k];
				boolean diff = false;
				for (int j = 1; j < numContests; j++) {
					if ((s == null && cf.vals[j][i][k] != null) || (s != null && !s.equals(cf.vals[j][i][k])))
						diff = true;
				}
				if (diff) {
					cf.diff[i][k] = DiffType.DIFF_BAD;
					cf.different++;
				}
			}
		}
	}

	protected static void outputValidationWarning(Contest contest) {
		if (contest.validate() != null)
			Trace.trace(Trace.WARNING, "Event feed is not valid. Run \"eventFeed --validate\" for details");
	}
}