package org.icpc.tools.contest.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.internal.Account;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.icpc.tools.contest.model.internal.Group;
import org.icpc.tools.contest.model.internal.Organization;
import org.icpc.tools.contest.model.internal.Person;
import org.icpc.tools.contest.model.internal.Team;

public class TSVImporter {
	private static final String ID = "id";
	private static final String NAME = "name";
	private static final String ICPC_ID = "icpc_id";
	private static final String ORGANIZATION_ID = "organization_id";
	private static final String FORMAL_NAME = "formal_name";
	private static final String URL = "url";
	private static final String HASHTAG = "twitter_hashtag";
	private static final String GROUP_IDS = "group_ids";
	private static final String COUNTRY = "country";
	private static final String LOCATION = "location";
	private static final String TEAM_ID = "team_id";
	private static final String SEX = "sex";
	private static final String ROLE = "role";
	private static final String TYPE = "type";
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";

	private static void add(ContestObject co, String name, Object value) {
		if (value == null)
			return;

		if (value instanceof String) {
			String s = ((String) value).trim();
			if (s.isEmpty() || "null".equals(s))
				return;
			co.add(name, s);
			return;
		}
		co.add(name, value);
	}

	public static void importGroups(Contest contest, File f) throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			// read header
			br.readLine();

			String s = br.readLine();
			while (s != null) {
				String[] st = s.split("\\t");
				if (st != null && st.length > 0) {
					try {
						Group g = new Group();
						add(g, ID, st[0]);
						add(g, ICPC_ID, st[0]);
						add(g, NAME, st[1]);
						contest.add(g);
					} catch (Exception e) {
						Trace.trace(Trace.ERROR, "Error parsing groups.tsv", e);
					}
				}
				s = br.readLine();
			}
		}
	}

	public static void importTeams(Contest contest, File f) throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			// read header
			br.readLine();

			String s = br.readLine();
			while (s != null) {
				String[] st = s.split("\\t");
				if (st != null && st.length > 0) {
					Team t = new Team();
					try {
						add(t, ContestObject.ID, st[0]);
						if (st.length < 7)
							Trace.trace(Trace.WARNING, "Team missing columns: " + s);
						else {
							add(t, ICPC_ID, st[1]);
							add(t, GROUP_IDS, new Object[] { st[2] });
							add(t, NAME, st[3].trim());
							if (st.length > 7) {
								String id = realOrgId(st[7]);
								add(t, ORGANIZATION_ID, id);
							}
							// TODO: group_id

							contest.add(t);
						}
					} catch (Exception e) {
						Trace.trace(Trace.ERROR, "Error parsing teams.tsv, team " + t.getId(), e);
					}
				}
				s = br.readLine();
			}
		}
	}

	private static String realOrgId(String id) {
		if (id == null)
			return null;

		String id2 = id;
		if (id.startsWith("INST-U-"))
			id2 = id.substring(7);
		else if (id.startsWith("INST-"))
			id2 = id.substring(5);

		while (id2.startsWith("0"))
			id2 = id2.substring(1);

		return id2;
	}

	public static void importInstitutions(Contest contest, File f) throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			// read header
			br.readLine();

			String s = br.readLine();
			while (s != null) {
				String[] st = s.split("\\t");
				if (st != null && st.length > 0) {
					Organization org = new Organization();
					String id = realOrgId(st[0]);

					// Note: the CMS seems to change formats quite often, having extra or less columns
					// for institutions
					// Verify the numbers below, especially the ones from URL and up

					add(org, ID, id);
					add(org, ICPC_ID, id);
					add(org, FORMAL_NAME, st[1]);
					add(org, NAME, st[2]);

					if (st.length > 4)
						add(org, COUNTRY, st[4]);

					if (st.length > 5)
						add(org, URL, st[5]);
					if (st.length > 6)
						add(org, HASHTAG, st[6]);
					if (st.length > 8) {
						JsonObject obj = new JsonObject();
						if (st[8] != null && !st[8].trim().isEmpty() && !"null".equals(st[8]))
							obj.props.put("latitude", st[8]);
						if (st[9] != null && !st[9].trim().isEmpty() && !"null".equals(st[9]))
							obj.props.put("longitude", st[9]);
						if (obj.containsKey("latitude") && obj.containsKey("longitude"))
							org.add(LOCATION, obj);
					}

					contest.add(org);
				}
				s = br.readLine();
			}
		}
	}

	public static void importTeamMembers(Contest contest, File f) throws IOException {
		int id = 1;
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			// read header
			br.readLine();

			String s = br.readLine();
			while (s != null) {
				String[] st = s.split("\\t");
				if (st != null && st.length > 0) {
					Person p = new Person();
					add(p, ID, id + "");
					id++;
					add(p, TEAM_ID, st[0]);
					String role = st[1].toLowerCase();
					if ("co-coach".equals(role))
						role = "coach";
					if ("coach".equals(role) || "contestant".equals(role)) {
						add(p, ROLE, role);
						add(p, NAME, st[2] + " " + st[3]);
						if (st.length >= 5) {
							String sex = st[4];
							if ("M".equalsIgnoreCase(sex))
								add(p, SEX, "male");
							else if ("F".equalsIgnoreCase(sex))
								add(p, SEX, "female");
						}
						if (st.length >= 6)
							add(p, ICPC_ID, st[5]);
						contest.add(p);
					}
				}
				s = br.readLine();
			}
		}
	}

	public static void importAccounts(Contest contest, File f) throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			// read header
			br.readLine();

			String s = br.readLine();
			while (s != null) {
				String[] st = s.split("\\t");
				if (st != null && st.length > 0) {
					Account a = new Account();
					try {
						add(a, ContestObject.ID, st[1]);
						if (st.length < 4)
							Trace.trace(Trace.WARNING, "Account missing columns: " + s);
						else {
							add(a, TYPE, st[0]);
							// add(a, NAME, st[1]);
							add(a, USERNAME, st[2]);
							add(a, PASSWORD, st[3]);
							contest.add(a);
						}
					} catch (Exception e) {
						Trace.trace(Trace.ERROR, "Error parsing accounts.tsv, account " + a.getId(), e);
					}
				}
				s = br.readLine();
			}
		}
	}
}