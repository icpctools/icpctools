package org.icpc.tools.contest.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.icpc.tools.contest.model.internal.Group;
import org.icpc.tools.contest.model.internal.Organization;
import org.icpc.tools.contest.model.internal.Team;
import org.icpc.tools.contest.model.internal.TeamMember;

public class TSVImporter {
	private static final String ID = "id";
	private static final String NAME = "name";
	private static final String ICPC_ID = "icpc_id";
	private static final String ORGANIZATION_ID = "organization_id";
	private static final String FORMAL_NAME = "formal_name";
	private static final String URL = "url";
	private static final String HASHTAG = "twitter_hashtag";
	private static final String GROUP_ID = "group_id";
	private static final String COUNTRY = "country";
	private static final String LOCATION = "location";
	private static final String TEAM_ID = "team_id";
	private static final String FIRST_NAME = "first_name";
	private static final String LAST_NAME = "last_name";
	private static final String SEX = "sex";
	private static final String ROLE = "role";

	private static void add(ContestObject co, String name, String value) {
		if (value == null || value.trim().isEmpty())
			return;
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
							add(t, GROUP_ID, st[2]);
							add(t, NAME, st[3]);
							if (st.length > 7) {
								String id = st[7];
								if (id != null && id.startsWith("INST-"))
									id = id.substring(5);
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

	public static void importInstitutions(Contest contest, File f) throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			// read header
			br.readLine();

			String s = br.readLine();
			while (s != null) {
				String[] st = s.split("\\t");
				if (st != null && st.length > 0) {
					Organization org = new Organization();
					String id = st[0];
					if (id != null && id.startsWith("INST-U-"))
						id = id.substring(7);
					add(org, ID, id);
					add(org, ICPC_ID, id);
					add(org, FORMAL_NAME, st[1]);
					add(org, NAME, st[2]);
					// add(inst, GROUP_ID, st[3]);
					if (st.length > 4)
						add(org, COUNTRY, st[4]);

					if (st.length > 5)
						add(org, URL, st[5]);
					if (st.length > 6)
						add(org, HASHTAG, st[6]);
					if (st.length > 8) {
						JsonObject obj = new JsonObject();
						obj.props.put("latitude", st[7]);
						obj.props.put("longitude", st[8]);
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
					TeamMember p = new TeamMember();
					add(p, ID, id + "");
					id++;
					add(p, TEAM_ID, st[0]);
					String role = st[1].toLowerCase();
					if ("co-coach".equals(role))
						role = "coach";
					if ("coach".equals(role) || "contestant".equals(role)) {
						add(p, ROLE, role);
						add(p, FIRST_NAME, st[2]);
						add(p, LAST_NAME, st[3]);
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
}