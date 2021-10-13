package org.icpc.tools.contest.util.cms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
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
	private static final String GROUP_IDS = "group_ids";
	private static final String COUNTRY = "country";
	private static final String LOCATION = "location";
	private static final String TEAM_ID = "team_id";
	private static final String FIRST_NAME = "first_name";
	private static final String LAST_NAME = "last_name";
	private static final String SEX = "sex";
	private static final String ROLE = "role";

	private static void add(ContestObject co, String name, Object value) {
		if (value == null)
			return;

		if (value instanceof String) {
			String s = (String) value;
			if (s.trim().isEmpty() || "null".equals(s))
				return;
		}
		co.add(name, value);
	}

	public static void importGroups(List<Group> list, File f) throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(new File(f, "groups.tsv")))) {
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
						list.add(g);
					} catch (Exception e) {
						Trace.trace(Trace.ERROR, "Error parsing groups.tsv", e);
					}
				}
				s = br.readLine();
			}
		}
	}

	public static void importTeams(List<Team> list, File f) throws IOException {
		File file = new File(f, "teams2.tsv");
		if (!file.exists())
			file = new File(f, "teams.tsv");
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			// read header
			br.readLine();

			String s = br.readLine();
			while (s != null) {
				String[] st = s.split("\\t");
				if (st != null && st.length > 0) {
					Team t = new Team();
					try {
						if ("null".equals(st[0]))
							add(t, ContestObject.ID, "0");
						else
							add(t, ContestObject.ID, st[0]);
						if (st.length < 7)
							Trace.trace(Trace.WARNING, "Team missing columns: " + s);
						else {
							add(t, ICPC_ID, st[1]);
							add(t, GROUP_IDS, new Object[] { st[2] });
							add(t, NAME, st[3]);
							if (st.length > 7) {
								String id = realOrgId(st[7]);
								add(t, ORGANIZATION_ID, id);

								Organization o = JsonToTSVConverter.getOrganizationById(id);
								o.add(FORMAL_NAME, st[4]);
								o.add(COUNTRY, st[6]);
							}

							list.add(t);
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
		if (id.startsWith("INST-U-"))
			return id.substring(7);
		else if (id.startsWith("INST-"))
			return id.substring(5);
		return id;
	}

	public static void importInstitutions(List<Organization> list, File f) throws IOException {
		File file = new File(f, "institutions2.tsv");
		if (!file.exists())
			file = new File(f, "institutions.tsv");
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			// read header
			br.readLine();

			String s = br.readLine();
			while (s != null) {
				String[] st = s.split("\\t");
				if (st != null && st.length > 0) {
					Organization org = new Organization();
					String id = realOrgId(st[0]);
					add(org, ID, id);
					add(org, ICPC_ID, id);
					add(org, FORMAL_NAME, st[1]);
					add(org, NAME, st[2]);

					// TODO temp for Moscow - completely new format
					// add(inst, GROUP_ID, st[3]);
					// if (st.length > 4)
					// add(org, COUNTRY, st[4]);

					add(org, URL, st[3]);
					add(org, HASHTAG, st[6]);
					JsonObject obj = new JsonObject();
					if (st[7] != null && !st[7].trim().isEmpty() && !"null".equals(st[7]))
						obj.props.put("latitude", st[7]);
					if (st[8] != null && !st[8].trim().isEmpty() && !"null".equals(st[8]))
						obj.props.put("longitude", st[8]);
					if (obj.containsKey("latitude") && obj.containsKey("longitude"))
						org.add(LOCATION, obj);

					list.add(org);
				}
				s = br.readLine();
			}
		}
	}

	public static void importTeamMembers(List<TeamMember> list, File f) throws IOException {
		int id = 1;
		try (BufferedReader br = new BufferedReader(new FileReader(new File(f, "members.tsv")))) {
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
						list.add(p);
					}
				}
				s = br.readLine();
			}
		}
	}

	public static void importTeamMembersTab(List<TeamMember> list, List<Team> teams, File f) throws IOException {
		List<TeamMember> temp = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(new File(f, "Person.tab")))) {
			// read header
			br.readLine();

			String s = br.readLine();
			while (s != null) {
				String[] st = s.split("\\t");
				if (st != null && st.length > 0) {
					TeamMember p = new TeamMember();
					add(p, ID, st[0]);
					add(p, ICPC_ID, st[0]);
					// add(p, TEAM_ID, st[0]);
					add(p, FIRST_NAME, st[2]);
					add(p, LAST_NAME, st[3]);
					if (st.length > 26) {
						String sex = st[27];
						if (sex != null)
							add(p, SEX, sex.toLowerCase());
					} else {
						Trace.trace(Trace.USER, "Sex not known: " + st[0] + " - " + st[2] + " " + st[3]);
					}
					temp.add(p);
				}
				s = br.readLine();
			}
		}
		System.out.println("People: " + temp.size());

		try (BufferedReader br = new BufferedReader(new FileReader(new File(f, "TeamPerson.tab")))) {
			// read header
			br.readLine();

			String s = br.readLine();
			while (s != null) {
				String[] st = s.split("\\t");
				if (st != null && st.length > 0) {
					String id = st[0];

					TeamMember p = null;
					for (TeamMember tm : temp)
						if (id.equals(tm.getICPCId()))
							p = tm;

					if (p == null) {
						System.err.println("Warning: unknown team person");
					} else {
						String teamId = st[1];
						Team t = null;
						for (Team tm : teams)
							if (teamId.equals(tm.getICPCId()))
								t = tm;

						if (t == null) {
							System.err.println("Warning: person " + p.getFirstName() + " " + p.getLastName() + " in unknown team " + teamId);
						} else {
							add(p, TEAM_ID, t.getId());
							String role = st[3];
							add(p, ROLE, role.toLowerCase());
							list.add(p);
						}
					}
				}
				s = br.readLine();
			}
		}
	}
}