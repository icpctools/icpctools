package org.icpc.tools.contest.model.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.internal.Contest;

/**
 * Contest comparison utility. Provides summary info or full contest compare.
 */
public class ContestComparator {
	public static class Difference {
		public String id;
		public List<Map<String, String>> diff;

		protected List<Map<String, String>> ensureSize(int size) {
			if (diff == null) {
				diff = new ArrayList<Map<String, String>>(size);
				for (int k = 0; k < size; k++)
					diff.add(null);
			}
			return diff;
		}
	}

	public static class TypeComparison {
		public String type;
		public int[] count;
		public int[] diff;
		public int[] unique;
		public int total;
		public int identical;
		public int different;
		public List<Difference> rows = new ArrayList<>();

		public Difference getOrCreateDifference(String id) {
			for (Difference cr : rows) {
				if (cr.id.equals(id))
					return cr;
			}
			Difference cr = new Difference();
			cr.id = id;
			rows.add(cr);
			return cr;
		}

		protected void ensureSize(int size) {
			if (diff == null) {
				count = new int[size];
				unique = new int[size];
				diff = new int[size];
			}
		}

		protected boolean isOk() {
			return different == 0 && identical == total;
		}

		public void encode(JSONEncoder je, boolean summaryOnly) {
			je.openChild(type);
			je.openChildArray("count");
			for (int i = 0; i < count.length; i++) {
				je.encodeValue(count[i]);
			}
			je.closeArray();
			je.openChildArray("different");
			for (int i = 0; i < diff.length; i++) {
				je.encodeValue(diff[i]);
			}
			je.closeArray();
			je.openChildArray("unique");
			for (int i = 0; i < diff.length; i++) {
				je.encodeValue(diff[i]);
			}
			je.closeArray();
			je.encode("identical", 0);
			if (!summaryOnly) {
				je.openChildArray("data");
				for (Difference cr : rows) {
					je.writeSeparator();
					je.open();

					je.encode("id", cr.id);
					je.openChildArray("details");
					for (Map<String, String> m : cr.diff) {
						if (m == null)
							je.encodeNull();
						else {
							je.open();
							for (String key : m.keySet()) {
								je.encode(key, m.get(key));
							}
							je.close();
						}
					}

					je.closeArray();
					je.close();
				}
				je.closeArray();
			}
			je.close();
		}

		public String printHTMLSummary() {
			StringBuilder sb = new StringBuilder();

			if (!isOk())
				sb.append("<span class=\"text-danger\">");

			String st = printImpl(null, true);
			String[] ss = st.split("\n");
			for (String s2 : ss)
				sb.append(s2 + "<br/>");
			if (!isOk())
				sb.append("</span>");
			return sb.toString();
		}

		public void printSummary(String s, boolean includeDetails) {
			Trace.trace(Trace.USER, printImpl(s, includeDetails));
		}

		private String printImpl(String s, boolean includeDetails) {
			StringBuilder sb = new StringBuilder();
			if (s != null)
				sb.append(s + ": ");

			if (isOk()) {
				if (total == 0)
					sb.append("None in source or target.\n");
				else
					sb.append("All " + total + " are identical.\n");
			} else {
				sb.append(count[0] + " in source, " + count[1] + " in target. " + identical + " identical, " + unique[0]
						+ " source only, " + unique[1] + " target only, " + different + " different.\n");

				if (includeDetails) {
					for (Difference cr : rows) {
						Map<String, String> d1 = cr.diff.get(0);
						Map<String, String> d2 = cr.diff.get(1);
						if (d1 != null && d2 == null)
							sb.append("  Source only: " + cr.id + "\n");
						else if (d1 == null && d2 != null)
							sb.append("  Target only: " + cr.id + "\n");
						else {
							// build all the keys
							Set<String> keys = new HashSet<String>();
							keys.addAll(d1.keySet());
							for (String key : d2.keySet()) {
								if (!keys.contains(key))
									keys.add(key);
							}
							for (String key : keys) {
								String v1 = d1.get(key);
								String v2 = d2.get(key);
								if (v1 == null)
									sb.append("  Different " + cr.id + ": property in source only: " + key + "\n");
								else if (v2 == null)
									sb.append("  Different " + cr.id + ": property in target only: " + key + "\n");
								else
									sb.append("  Different " + cr.id + ": property " + key + " (" + v1 + " vs " + v2 + ")\n");
							}
						}
					}
				}
			}
			return sb.toString();
		}
	}

	public static class ContestComparison {
		public int numContests;
		public List<TypeComparison> types = new ArrayList<>();

		public TypeComparison getType(String type) {
			for (TypeComparison ty : types) {
				if (ty.type.equals(type)) {
					return ty;
				}
			}
			TypeComparison ty = new TypeComparison();
			ty.type = type;
			types.add(ty);
			return ty;
		}

		public void add(TypeComparison type) {
			types.add(type);
		}

		public void encode(JSONEncoder je, boolean summaryOnly) {
			je.open();
			je.encode("num_contests", numContests); // TODO - include names and ids?
			for (TypeComparison ty : types) {
				je.writeSeparator();
				ty.encode(je, summaryOnly);
			}
			je.close();
		}

		public boolean print(boolean summaryOnly) {
			boolean ok = true;
			for (TypeComparison ty : types) {
				if (!ty.isOk())
					ok = false;

				ty.printSummary(ty.type, !summaryOnly);
			}
			return ok;
		}
	}

	/**
	 * Returns true if two arrays contain the same objects, and false otherwise.
	 *
	 * @param a
	 * @param b
	 * @return
	 */
	private static boolean compareArrays(Object[] a, Object[] b) {
		if (a.length != b.length)
			return false;

		for (Object aa : a) {
			boolean found = false;
			for (Object bb : b) {
				if (aa != null && aa.equals(bb))
					found = true;
			}
			if (!found)
				return false;
		}

		for (Object bb : b) {
			boolean found = false;
			for (Object aa : a) {
				if (bb != null && bb.equals(aa))
					found = true;
			}
			if (!found)
				return false;
		}

		return true;
	}

	private static void compareContestObjects(TypeComparison type, String id, IContestObject... co) {
		Set<String> keys = new HashSet<String>();

		boolean identical = true;

		// mark any that aren't null
		int numContests = co.length;
		int numNull = 0;
		for (int i = 0; i < numContests; i++) {
			if (co[i] == null)
				numNull++;
		}
		if (numNull > 0) {
			Difference cr = type.getOrCreateDifference(id);
			List<Map<String, String>> diff = cr.ensureSize(numContests);

			for (int i = 0; i < numContests; i++) {
				if (co[i] != null)
					diff.set(i, new HashMap<String, String>(2));
			}
		}

		List<Map<String, Object>> allProps = new ArrayList<Map<String, Object>>(numContests);
		for (int i = 0; i < numContests; i++) {
			if (co[i] == null)
				allProps.add(null);
			else
				allProps.add(co[i].getProperties());
		}

		// compare properties
		Object[] comp = new Object[numContests];
		for (int i = 0; i < numContests; i++) {
			if (co[i] == null)
				continue;

			Map<String, Object> props = allProps.get(i);

			for (String key : props.keySet()) {
				if (keys.contains(key))
					continue;
				keys.add(key);

				Arrays.fill(comp, null);
				comp[i] = props.get(key);
				for (int j = i + 1; j < numContests; j++) {
					if (co[j] == null)
						continue;
					comp[j] = allProps.get(j).get(key);
				}

				boolean samePropValue = true;
				for (int j = 0; j < numContests - 1; j++) {
					if (co[j] != null && co[j + 1] != null && ((comp[j] == null && comp[j + 1] != null)
							|| (comp[j] != null && !comp[j].equals(comp[j + 1])))) {
						if (comp[j] != null && comp[j] instanceof Object[] && comp[j + 1] != null
								&& comp[j + 1] instanceof Object[]) {
							Object[] a = (Object[]) comp[j];
							Object[] b = (Object[]) comp[j + 1];
							if (!compareArrays(a, b))
								samePropValue = false;
						} else
							samePropValue = false;
					}
				}

				if (!samePropValue) {
					identical = false;
					Difference cr = type.getOrCreateDifference(id);
					List<Map<String, String>> d = cr.ensureSize(numContests);

					for (int j = 0; j < numContests; j++) {
						if (co[j] == null)
							continue;

						if (d.get(j) == null)
							d.set(j, new HashMap<String, String>(2));

						Map<String, String> m = d.get(j);
						if (comp[j] == null)
							m.put(key, "null");
						else if (comp[j] instanceof Object[]) {
							Object[] a = (Object[]) comp[j];
							String[] s = new String[a.length];
							for (int k = 0; k < a.length; k++) {
								if (a[k] == null)
									s[k] = "null";
								else
									s[k] = a[k].toString();
							}
							m.put(key, "[" + String.join(",", s) + "]");
						} else
							m.put(key, comp[j].toString());
					}
				}
			}
		}

		// boolean unique = false;
		boolean diff = false;

		for (int i = 0; i < numContests; i++) {
			if (co[i] != null) {
				if (identical && numNull == numContests - 1) {
					type.unique[i]++;
					// unique = true;
				}
				if (!identical) {
					type.diff[i]++;
					diff = true;
				}
			}
		}
		// if (!identical && numNull == numContests - 1)
		if (identical && numContests - numNull > 1)
			type.identical++;
		if (diff)
			// (numNull != numContests - 1)
			type.different++;
	}

	public static ContestComparison compareContests(Contest... contests) {
		ContestComparison cf = new ContestComparison();
		cf.add(compareInfo(contests));
		cf.add(compareGroups(contests));
		cf.add(compareTeams(contests));
		cf.add(compareOrganizations(contests));
		cf.add(compareLanguages(contests));
		cf.add(compareProblems(contests));
		cf.add(compareJudgementTypes(contests));
		cf.add(compareSubmissions(contests));
		cf.add(compareJudgements(contests));
		cf.add(compareRuns(contests));
		cf.add(compareAwards(contests));
		return cf;
	}

	public static TypeComparison compareInfo(Contest... c) {
		return compareTypes(IContestObject.ContestType.CONTEST, c);
	}

	public static TypeComparison compareGroups(Contest... c) {
		return compareTypes(IContestObject.ContestType.GROUP, c);
	}

	public static TypeComparison compareOrganizations(Contest... c) {
		return compareTypes(IContestObject.ContestType.ORGANIZATION, c);
	}

	public static TypeComparison compareTeams(Contest... c) {
		return compareTypes(IContestObject.ContestType.TEAM, c);
	}

	public static TypeComparison compareLanguages(Contest... c) {
		return compareTypes(IContestObject.ContestType.LANGUAGE, c);
	}

	public static TypeComparison compareProblems(Contest... c) {
		return compareTypes(IContestObject.ContestType.PROBLEM, c);
	}

	public static TypeComparison compareJudgementTypes(Contest... c) {
		return compareTypes(IContestObject.ContestType.JUDGEMENT_TYPE, c);
	}

	public static TypeComparison compareSubmissions(Contest... c) {
		return compareTypes(IContestObject.ContestType.SUBMISSION, c);
	}

	public static TypeComparison compareJudgements(Contest... c) {
		return compareTypes(IContestObject.ContestType.JUDGEMENT, c);
	}

	public static TypeComparison compareRuns(Contest... c) {
		return compareTypes(IContestObject.ContestType.RUN, c);
	}

	public static TypeComparison comparePersons(Contest... c) {
		return compareTypes(IContestObject.ContestType.PERSON, c);
	}

	public static TypeComparison compareAccounts(Contest... c) {
		return compareTypes(IContestObject.ContestType.ACCOUNT, c);
	}

	public static TypeComparison compareClarifications(Contest... c) {
		return compareTypes(IContestObject.ContestType.CLARIFICATION, c);
	}

	public static TypeComparison compareCommentary(Contest... c) {
		return compareTypes(IContestObject.ContestType.COMMENTARY, c);
	}

	public static TypeComparison compareAwards(Contest... c) {
		return compareTypes(IContestObject.ContestType.AWARD, c);
	}

	private static TypeComparison compareTypes(IContestObject.ContestType type, Contest... c) {
		int size = c.length;
		TypeComparison ty = new TypeComparison();
		ty.type = IContestObject.getTypeName(type);
		ty.ensureSize(size);

		Set<String> ids = new HashSet<String>(c[0].getObjects(type).length + 10);
		IContestObject[] comp = new IContestObject[size];

		for (int i = 0; i < size; i++) {
			IContestObject[] objs = c[i].getObjects(type);
			ty.count[i] = objs.length;

			for (IContestObject co : objs) {
				String id = co.getId();
				if (ids.contains(id))
					continue;
				ids.add(id);

				Arrays.fill(comp, null);
				comp[i] = co;
				for (int j = i + 1; j < size; j++)
					comp[j] = c[j].getObjectByTypeAndId(type, id);

				compareContestObjects(ty, id, comp);
			}
		}
		ty.total = ids.size();
		return ty;
	}
}