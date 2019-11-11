package org.icpc.tools.cds.service;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IJudgementType;
import org.icpc.tools.contest.model.ILanguage;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IRun;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.feed.JSONEncoder;

public class ReportGenerator {
	public enum ReportType {
		LANGS, RUNS, PROBLEMS
	}

	private static final String ID = "id";
	private static final String NAME = "name";
	private static final String FAILED = "failed";
	private static final String SOLVED = "solved";
	private static final String TOTAL = "total";

	public static void report(PrintWriter pw, IContest contest, String reportType) {
		if (reportType == null)
			return;

		ReportType type = ReportType.valueOf(reportType.toUpperCase());
		if (type == null)
			return;

		report(pw, contest, type);
	}

	public static void report(PrintWriter pw, IContest contest, ReportType report) {
		JSONEncoder en = new JSONEncoder(pw);
		if (report == ReportType.LANGS)
			languagesReport(contest, en);
		else if (report == ReportType.RUNS)
			runsReport(contest, en);
		else if (report == ReportType.PROBLEMS)
			problemsReport(contest, en);
	}

	public ReportGenerator() {
		//
	}

	private static void increment(Map<String, Object> map2, String key) {
		Object obj = map2.get(key);
		if (!(obj instanceof Integer))
			return;

		Integer in = (Integer) obj;
		map2.put(key, new Integer(in.intValue() + 1));
	}

	// { {"name":"Catch the Plane", "solved":5, "failed":2, "total":7 }, ... }
	public static void problemsReport(IContest contest, JSONEncoder en) {
		List<Map<String, Object>> list = new ArrayList<>();
		IJudgementType[] jts = contest.getJudgementTypes();

		Map<String, Object> map2 = new LinkedHashMap<>();
		map2.put(ID, "ID");
		map2.put(NAME, "Name");
		for (IJudgementType jt : jts)
			map2.put(jt.getId(), jt.getId());
		map2.put(SOLVED, "Solved");
		map2.put(FAILED, "Failed");
		map2.put(TOTAL, "Total");
		list.add(map2);

		for (IProblem p : contest.getProblems()) {
			String id = p.getId();
			String name = p.getName();
			Map<String, Object> map = new LinkedHashMap<>();
			map.put(ID, id);
			map.put(NAME, name);
			for (IJudgementType jt : jts)
				map.put(jt.getId(), new Integer(0));
			map.put(SOLVED, new Integer(0));
			map.put(FAILED, new Integer(0));
			map.put(TOTAL, new Integer(0));
			list.add(map);
		}

		for (ISubmission s : contest.getSubmissions()) {
			String id = s.getProblemId();
			Map<String, Object> map = null;
			for (Map<String, Object> m : list) {
				if (id.equals(m.get(ID)))
					map = m;
			}

			if (map != null) {
				IJudgement[] j = contest.getJudgementsBySubmissionId(s.getId());
				if (j != null) {
					String jtId = j[j.length - 1].getJudgementTypeId();
					increment(map, jtId);
					updateForJudgement(contest, map, jtId);
				}

				increment(map, TOTAL);
			}
		}

		writeList(en, list);
	}

	// { {"name":"Java", "solved":5, "failed":2, "total":7 }, ... }
	public static void languagesReport(IContest contest, JSONEncoder en) {
		List<Map<String, Object>> list = new ArrayList<>();
		IJudgementType[] jts = contest.getJudgementTypes();
		Map<String, Object> map2 = new LinkedHashMap<>();
		map2.put(ID, "ID");
		map2.put(NAME, "Name");
		for (IJudgementType jt : jts)
			map2.put(jt.getId(), jt.getId());
		map2.put(SOLVED, "Solved");
		map2.put(FAILED, "Failed");
		map2.put(TOTAL, "Total");
		list.add(map2);

		for (ILanguage l : contest.getLanguages()) {
			String id = l.getId();
			String name = l.getName();
			Map<String, Object> map = new LinkedHashMap<>();
			map.put(ID, id);
			map.put(NAME, name);
			for (IJudgementType jt : jts)
				map.put(jt.getId(), new Integer(0));
			map.put(SOLVED, new Integer(0));
			map.put(FAILED, new Integer(0));
			map.put(TOTAL, new Integer(0));
			list.add(map);
		}

		for (ISubmission s : contest.getSubmissions()) {
			String id = s.getLanguageId();
			Map<String, Object> map = null;
			for (Map<String, Object> m : list) {
				if (id.equals(m.get(ID)))
					map = m;
			}
			if (map != null) {
				IJudgement[] j = contest.getJudgementsBySubmissionId(s.getId());
				if (j != null) {
					String jtId = j[j.length - 1].getJudgementTypeId();
					increment(map, jtId);
					updateForJudgement(contest, map, jtId);
				}

				increment(map, TOTAL);
			}
		}

		writeList(en, list);
	}

	protected static void updateForJudgement(IContest contest, Map<String, Object> map, String jtId) {
		if (map == null || jtId == null)
			return;

		IJudgementType jt = contest.getJudgementTypeById(jtId);
		if (jt == null)
			return;

		if (jt.isSolved())
			increment(map, SOLVED);
		else if (jt.isPenalty())
			increment(map, FAILED);
	}

	public static void runsReport(IContest contest, JSONEncoder en) {
		List<Map<String, Object>> list = new ArrayList<>();
		IJudgementType[] jts = contest.getJudgementTypes();

		Map<String, Object> map2 = new LinkedHashMap<>();
		map2.put(ID, "ID");
		map2.put("Id", "Id");
		map2.put(NAME, "Name");
		map2.put(TOTAL, "Total");
		list.add(map2);

		for (IJudgementType jt : jts) {
			String id = jt.getId();
			String name = jt.getName();
			Map<String, Object> map = new LinkedHashMap<>();
			map.put(ID, id);
			map.put("Id", id);
			map.put(NAME, name);
			map.put(TOTAL, new Integer(0));
			list.add(map);
		}

		for (IRun r : contest.getRuns()) {
			String id = r.getJudgementTypeId();
			Map<String, Object> map = null;
			for (Map<String, Object> m : list) {
				if (id.equals(m.get(ID)))
					map = m;
			}
			if (map != null)
				increment(map, TOTAL);
		}

		writeList(en, list);
	}

	protected static void writeList(JSONEncoder en, List<Map<String, Object>> list) {
		en.openArray();
		for (Map<String, Object> map : list)
			writeMap(en, map);

		en.closeArray();
	}

	protected static void writeMap(JSONEncoder en, Map<String, Object> map) {
		en.open();
		for (String key : map.keySet()) {
			// if (!ID.equals(key)) {
			Object value = map.get(key);
			if (value != null)
				en.encode(key, value.toString());
			// }
		}
		en.close();
	}

	/*public void writeStatus(PrintWriter pw, JSONEncoder en) {
		en.open();
		for (String teamId : map.keySet()) {
			// int stream = map.get(teamId);
			Status s = Status.ACTIVE;
			String st = "-";
			if (Status.ACTIVE == s)
				st = "A";
			en.encode(teamId, st);
		}
		en.close();
	}*/
}