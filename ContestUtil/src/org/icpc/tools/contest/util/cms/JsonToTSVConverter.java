package org.icpc.tools.contest.util.cms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.feed.JSONArrayWriter;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.internal.Group;
import org.icpc.tools.contest.model.internal.Info;
import org.icpc.tools.contest.model.internal.Organization;
import org.icpc.tools.contest.model.internal.Team;
import org.icpc.tools.contest.model.internal.TeamMember;
import org.icpc.tools.contest.util.cms.CMSDownloadHelper.ContestInfo;

/**
 * Converter from ICPC CMS JSON files to both TSV and JSON formats.
 *
 * Arguments: cmsLocation contestRoot [--finals]
 *
 * cmsLocation - the folder containing downloaded CMS files (typically output of CMSDownloadHelper)
 *
 * contestRoot - a contest location, i.e. CDP/CAF root folder
 *
 * Use --finals to include a display_name in teams.tsv as used at ICPC World Finals
 */
public class JsonToTSVConverter {
	private static boolean FINALS_NAMING = false;
	private static final boolean OUTPUT_MISSING_NAMES = true;

	public static class CMSGroup {
		String name;
		String id;
	}

	public static class CMSInstitution {
		CMSGroup group;
		String id;
		String name;
		String shortName;
		String country;
		String url;
		String hashtag;
		String latitude;
		String longitude;
	}

	public static class CMSTeam {
		CMSGroup group;
		CMSInstitution inst;
		String name;
		String id;
		String number;
	}

	public static class CMSMember {
		CMSTeam team;
		String id;
		String firstName;
		String lastName;
		String sex;
		String role;
	}

	public static class CMSWinner {
		CMSInstitution inst;
		String year;
	}

	public static class CMSStaffMember {
		String id;
		String firstName;
		String lastName;
		String role;
	}

	protected static List<CMSGroup> groupList = new ArrayList<>();
	protected static List<CMSInstitution> institutionList = new ArrayList<>();
	protected static List<CMSTeam> teamList = new ArrayList<>();
	protected static List<CMSMember> memberList = new ArrayList<>();
	protected static List<CMSStaffMember> staffMemberList = new ArrayList<>();
	protected static List<CMSWinner> winnerList = new ArrayList<>();
	protected static List<String> contestIdList = new ArrayList<>();

	public static void main(String[] args) {
		if (args == null || args.length < 1 || args.length > 3) {
			System.err.println("Usage: command [cmsRoot] [contestRoot] [option]");
			System.exit(1);
		}

		File cmsRoot = new File(args[0]);
		File contestRoot = new File(args[1]);

		if (!cmsRoot.exists()) {
			System.err.println("CMS root folder does not exist");
			System.exit(1);
		}

		if (args.length == 3 && "--finals".equals(args[2]))
			FINALS_NAMING = true;

		generateContest(cmsRoot, contestRoot);
	}

	protected static int compare(String s1, String s2) {
		if (s1 == null || s2 == null)
			return 0;

		try {
			return Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2));
		} catch (Exception e) {
			// ignore
		}
		return s1.compareTo(s2);
	}

	protected static void generateHistory(File root) {
		groupList = new ArrayList<>();
		institutionList = new ArrayList<>();
		teamList = new ArrayList<>();
		memberList = new ArrayList<>();
		winnerList = new ArrayList<>();
		contestIdList = new ArrayList<>();

		try {
			for (int i = 0; i < CMSLogin.getContests().length; i++) {
				staffMemberList = new ArrayList<>();
				ContestInfo contestInfo = CMSLogin.getContests()[i];
				File folder = new File(root, contestInfo.shortName);
				readInstitutions(folder);
				readTeams(folder);
				readContest(folder);
			}

			for (CMSTeam t : teamList)
				readTeamResults(t.id);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void generateContest(File cmsRoot, File contestRoot) {
		groupList = new ArrayList<>();
		institutionList = new ArrayList<>();
		teamList = new ArrayList<>();
		memberList = new ArrayList<>();
		staffMemberList = new ArrayList<>();

		try {
			readInstitutions(cmsRoot);
			readTeams(cmsRoot);
			Info info = readContest(cmsRoot);
			readStaff(cmsRoot);

			teamList.removeIf(t -> t.number == null);
			memberList.removeIf(m -> m.team.number == null);

			groupList.sort((g1, g2) -> compare(g1.id, g2.id));
			teamList.sort((t1, t2) -> compare(t1.number, t2.number));
			institutionList.sort((i1, i2) -> compare(i1.id, i2.id));
			memberList.sort((m1, m2) -> compare(m1.team.number, m2.team.number));

			System.out.println("Writing to: " + contestRoot);

			File configFolder = new File(contestRoot, "config");
			configFolder.mkdirs();

			// older tsvs
			writeGroupsTSV(groupList, configFolder);
			writeTeamsTSV(teamList, configFolder);
			writeTeams2TSV(teamList, configFolder);
			writeInstitutionsTSV(institutionList, configFolder);
			writeInstitutions2TSV(institutionList, configFolder);
			writeMembersTSV(memberList, configFolder);
			writeStaffMembersTSV(staffMemberList, configFolder);

			writeContestJSON(info, configFolder);

			File regFolder = new File(contestRoot, "registration");
			regFolder.mkdirs();

			writeGroupsJSON(groupList, regFolder);
			writeOrganizationsJSON(institutionList, regFolder);
			writeTeamsJSON(teamList, regFolder);
			writeTeamMembersJSON(memberList, regFolder);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void writeGroupsTSV(List<CMSGroup> groups, File folder) throws Exception {
		File f = new File(folder, "groups.tsv");
		FileWriter fw = new FileWriter(f);
		fw.write("File_Version\t1\n");
		for (CMSGroup group : groups) {
			fw.write(group.id);
			fw.write("\t");
			fw.write(group.name);
			fw.write("\n");
		}
		fw.close();
	}

	protected static void writeTeamsTSV(List<CMSTeam> teams, File folder) throws Exception {
		File f = new File(folder, "teams.tsv");
		FileWriter fw = new FileWriter(f);
		fw.write("File_Version\t2\n");
		for (CMSTeam team : teams) {
			CMSInstitution inst = team.inst;
			if (team.number != null)
				fw.write(team.number);
			fw.write("\t");
			fw.write(team.id);
			fw.write("\t");
			if (team.group != null)
				fw.write(team.group.id);
			else
				System.err.println("Team with no group: " + team.id + " - " + team.name);
			fw.write("\t");
			if (inst != null) {
				fw.write(team.name);
				fw.write("\t");
				fw.write(inst.name);
				fw.write("\t");
				fw.write(inst.shortName);
				fw.write("\t");
				if (inst.country == null)
					System.err.println("No country: " + team.name);
				else
					fw.write(inst.country);
				fw.write("\n");
			} else
				System.err.println("Team with no institution: " + team.id + " - " + team.name);
		}
		fw.close();
	}

	protected static void writeTeams2TSV(List<CMSTeam> teams, File folder) throws Exception {
		File f = new File(folder, "teams2.tsv");
		FileWriter fw = new FileWriter(f);
		fw.write("File_Version\t2\n");
		for (CMSTeam team : teams) {
			CMSInstitution inst = team.inst;
			if (team.number != null)
				fw.write(team.number);
			fw.write("\t");
			fw.write(team.id);
			fw.write("\t");
			if (team.group != null)
				fw.write(team.group.id);
			else
				System.err.println("Team with no group: " + team.id + " - " + team.name);
			fw.write("\t");

			if (inst != null) {
				fw.write(team.name);
				fw.write("\t");
				fw.write(inst.name);
				fw.write("\t");
				fw.write(inst.shortName);
				fw.write("\t");
				if (inst.country == null)
					System.err.println("No country: " + team.name);
				else
					fw.write(inst.country);
				fw.write("\t");
				fw.write("INST-" + pad(inst.id));
				fw.write("\n");
				// if (inst.latitude == null || inst.longitude == null)
				// System.out.println(team.id + "\t" + team.number + "\t" + inst.id + "\t" +
				// inst.name);
			} else
				System.err.println("Team with no institution: " + team.id + " - " + team.name);
		}
		fw.close();
	}

	private static final String PAD = "0000";

	protected static String pad(String s) {
		if (s.length() < 4)
			return PAD.substring(0, 4 - s.length()) + s;
		return s;
	}

	protected static void writeInstitutionsTSV(List<CMSInstitution> teams, File folder) throws Exception {
		File f = new File(folder, "institutions.tsv");
		FileWriter fw = new FileWriter(f);
		fw.write("File_Version\t0.5\n");
		for (CMSInstitution inst : teams) {
			fw.write("INST-U-" + pad(inst.id));
			fw.write("\t");
			fw.write(inst.name);
			fw.write("\t");
			if (!inst.shortName.isEmpty())
				fw.write(inst.shortName);
			else
				System.err.println("Institution without shortname: " + inst.name);
			fw.write("\t");
			if (inst.group != null)
				fw.write(inst.group.id);
			else
				System.err.println("Institution not in a group: " + inst.name);
			fw.write("\t");
			if (inst.country != null)
				fw.write(inst.country);
			else
				System.err.println("Institution not in a country: " + inst.name);
			fw.write("\n");
		}
		fw.close();
	}

	protected static void writeInstitutions2TSV(List<CMSInstitution> teams, File folder) throws Exception {
		File f = new File(folder, "institutions2.tsv");
		FileWriter fw = new FileWriter(f);
		fw.write("File_Version\t0.5\n");
		int hashCount = 0;
		int locCount = 0;
		for (CMSInstitution inst : teams) {
			fw.write("INST-U-" + pad(inst.id));
			fw.write("\t");
			fw.write(inst.name);
			fw.write("\t");
			fw.write(inst.shortName);
			fw.write("\t");
			if (inst.group != null)
				fw.write(inst.group.id);
			else
				System.err.println("Institution not in a group: " + inst.name);
			fw.write("\t");
			if (inst.country != null)
				fw.write(inst.country);
			else
				System.err.println("Institution not in a country: " + inst.name);
			fw.write("\t");
			if (inst.url != null && !inst.url.isEmpty())
				fw.write(inst.url);
			// else
			// System.err.println("No URL: " + inst.id + " - " + inst.name);
			fw.write("\t");
			if (inst.hashtag != null && !inst.hashtag.isEmpty())
				fw.write(inst.hashtag);
			else {
				if (OUTPUT_MISSING_NAMES)
					System.out.println("No hashtag: " + inst.id + " - " + inst.name);
				hashCount++;
			}
			fw.write("\t");
			if (inst.latitude != null)
				fw.write(inst.latitude);
			fw.write("\t");
			if (inst.longitude != null)
				fw.write(inst.longitude);
			if (inst.latitude == null || inst.longitude == null) {
				if (OUTPUT_MISSING_NAMES)
					System.out.println("No location: " + inst.id + " - " + inst.name);
				locCount++;
			}
			fw.write("\n");
		}
		if (hashCount > 0)
			System.err.println(hashCount + " missing hashtags");
		if (locCount > 0)
			System.err.println(locCount + " missing location");
		fw.close();
	}

	protected static void writeContestJSON(Info info, File folder) throws Exception {
		File f = new File(folder, "contest.json");
		PrintWriter pw = new PrintWriter(f);
		JSONEncoder je = new JSONEncoder(pw);

		je.open();
		info.writeBody(je);
		je.close();

		pw.close();
	}

	protected static void writeGroupsJSON(List<CMSGroup> groups, File folder) throws Exception {
		File f = new File(folder, "groups.json");
		writeJSON(f, groups, (obj) -> {
			CMSGroup group = (CMSGroup) obj;
			Group g = new Group();
			g.add("id", group.id);
			g.add("icpc_id", group.id);
			g.add("name", group.name);
			return g;
		});
	}

	protected static void writeOrganizationsJSON(List<CMSInstitution> teams, File folder) throws Exception {
		File f = new File(folder, "organizations.json");
		writeJSON(f, teams, (obj) -> {
			CMSInstitution inst = (CMSInstitution) obj;
			Organization org = new Organization();
			org.add("id", inst.id);
			org.add("icpc_id", inst.id);
			org.add("formal_name", inst.name);
			org.add("name", inst.shortName);
			org.add("country", inst.country);
			org.add("twitter_hashtag", inst.hashtag);
			org.add("url", inst.url);
			if (inst.latitude != null && inst.longitude != null)
				org.add("location", "{\"latitude\":" + inst.latitude + ",\"longitude\":" + inst.longitude + "}");
			return org;
		});
	}

	protected static void writeTeamsJSON(List<CMSTeam> teams, File folder) throws Exception {
		File f = new File(folder, "teams.json");
		writeJSON(f, teams, (obj) -> {
			CMSTeam team = (CMSTeam) obj;
			Team t = new Team();
			if (team.number != null)
				t.add("id", team.number); // TODO: 0?
			else
				t.add("id", team.id);
			t.add("icpc_id", team.id);
			t.add("name", team.name);
			if (FINALS_NAMING && team.inst != null)
				t.add("display_name", team.inst.name);

			t.add("group_id", team.group.id);
			if (team.inst.id != null)
				t.add("organization_id", team.inst.id);
			return t;
		});
	}

	protected static void writeTeamMembersJSON(List<CMSMember> members, File folder) throws Exception {
		File f = new File(folder, "team-members.json");
		writeJSON(f, members, (obj) -> {
			CMSMember member = (CMSMember) obj;
			TeamMember m = new TeamMember();
			String role = member.role.toLowerCase();
			if ("co-coach".equals(role))
				role = "coach";
			if ("coach".equals(role) || "contestant".equals(role)) {
				m.add("id", member.id);
				m.add("icpc_id", member.id);
				m.add("first_name", member.firstName);
				m.add("last_name", member.lastName);
				m.add("role", role);
				m.add("team_id", member.team.id);
				if ("M".equalsIgnoreCase(member.sex))
					m.add("sex", "male");
				else if ("F".equalsIgnoreCase(member.sex))
					m.add("sex", "female");
				return m;
			}
			return null;
		});
	}

	interface Converter {
		IContestObject convert(Object iter);
	}

	protected static void writeJSON(File f, List<?> iter, Converter c) throws Exception {
		PrintWriter pw = new PrintWriter(f);
		JSONArrayWriter writer = new JSONArrayWriter(pw);
		writer.writePrelude();
		boolean first = true;
		for (Object obj : iter) {
			IContestObject co = c.convert(obj);
			if (co == null)
				continue;
			if (!first)
				writer.writeSeparator();
			else
				first = false;
			writer.write(co);
		}

		writer.writePostlude();
		pw.close();
	}

	protected static void writeAttr(FileWriter fw, boolean first, String name, String value) throws IOException {
		if (value == null || value.isEmpty())
			return;

		if (!first)
			fw.write(",");
		fw.write("\"" + name + "\":" + "\"" + value + "\"");
	}

	protected static void writeSimpleAttr(FileWriter fw, boolean first, String name, String value) throws IOException {
		if (!first)
			fw.write(",");
		fw.write("\"" + name + "\":" + "" + value + "");
	}

	protected static void writeMembersTSV(List<CMSMember> members, File folder) throws Exception {
		File f = new File(folder, "members.tsv");
		FileWriter fw = new FileWriter(f);
		fw.write("File_Version\t1\n");
		for (CMSMember member : members) {
			fw.write(member.team.number);
			fw.write("\t");
			fw.write(member.role);
			fw.write("\t");
			fw.write(member.firstName);
			fw.write("\t");
			fw.write(member.lastName);
			fw.write("\t");
			if (member.sex != null)
				fw.write(member.sex);
			fw.write("\n");
		}
		fw.close();
	}

	protected static void writeStaffMembersTSV(List<CMSStaffMember> members, File folder) throws Exception {
		File f = new File(folder, "staff-members.tsv");
		FileWriter fw = new FileWriter(f);
		fw.write("File_Version\t1\n");
		for (CMSStaffMember member : members) {
			if (member.role != null)
				fw.write(member.role);
			fw.write("\t");
			fw.write(member.firstName);
			fw.write("\t");
			fw.write(member.lastName);
			fw.write("\n");
		}
		fw.close();
	}

	protected static void readTeams(File folder) throws Exception {
		// File f = new File("/Users/deboer/ICPC/cms/2017/teams.json");
		File f = new File(folder, "teams.json");

		InputStream is = new FileInputStream(f);
		JSONParser parser = new JSONParser(is);
		JsonObject obj = parser.readObject();
		Object[] teams = obj.getArray("teams");
		for (Object teamObj : teams) {
			JsonObject team = (JsonObject) teamObj;
			// String name = team.getString("name");
			String tId = team.getInt("externalReservationId") + "";
			// int instId = team.getInt("institutionId");
			// System.out.println("team: " + name + " (" + instId + ")");
			CMSTeam t = getTeam(tId);

			if (team.containsKey("persons")) {
				Object[] persons = (Object[]) team.get("persons");
				for (Object personObj : persons) {
					JsonObject person = (JsonObject) personObj;
					String pId = person.getInt("personId") + "";
					CMSMember m = getMember(pId);
					m.team = t;
					m.firstName = person.getString("firstname");
					m.lastName = person.getString("lastname");
					if (person.containsKey("sex"))
						m.sex = person.getString("sex");
					m.role = person.getString("role");
					// System.out.println(" " + role + ": " + first + " " + last);
				}
			}

			// CMSDownloadHelper.downloadTeam(instId+"", token);

			/*System.out.print(result.getJsonObject("from").getString("name"));
			System.out.print(": ");
			System.out.println(result.getString("message", ""));
			System.out.println("-----------");*/
		}
	}

	protected static void readTeamResults(String id) throws Exception {
		// File f = new File(CMSDownloadHelper.TEAM_FOLDER, id + "-past-results.json");
		/*{
		  "externalContestId": 56,
		  "rank": 3,
		  "problemssolved": 7,
		  "totaltime": 1215,
		  "lastproblemtime": 0
		    }*/
		/*InputStream is = new FileInputStream(f);
		JsonReader rdr = Json.createReader(is);
		JsonArray arr = rdr.readArray();
		for (JsonObject result : arr.getValuesAs(JsonObject.class)) {
			// String name = team.getString("name");
			// String cId = result.getInt("externalContestId") + "";
		
			// String rank = result.getInt("rank") + "";
			// String problemssolved = result.getInt("problemssolved") + "";
			// String totaltime = result.getInt("totaltime") + "";
			// String lastproblemtime = result.getInt("lastproblemtime") + "";
			// CMSTeam t = getTeam(tId);
		
			// System.out.println(id + " " + cId + " " + rank + " " + contestIdList.contains(cId));
		}*/
	}

	protected static void readStaff(File folder) throws Exception {
		File f = new File(folder, "staff-members.json");

		InputStream is = new FileInputStream(f);
		JSONParser parser = new JSONParser(is);
		JsonObject obj = parser.readObject();
		Object[] persons = obj.getArray("persons");
		for (Object personObj : persons) {
			JsonObject person = (JsonObject) personObj;
			String pId = person.getInt("personId") + "";
			CMSStaffMember m = getStaffMember(pId);
			m.firstName = person.getString("firstname");
			if (m.firstName != null)
				m.firstName = m.firstName.trim();
			m.lastName = person.getString("lastname");
			if (m.lastName != null)
				m.lastName = m.lastName.trim();
			m.role = person.getString("badgeRole");
			if (m.role != null)
				m.role = m.role.trim();
			// System.out.println(" " + m.role + ": " + m.firstName + " " + m.lastName);
		}
	}

	protected static void readInstitutions(File folder) throws Exception {
		File f = new File(folder, "institutions.json");
		JSONParser parser = new JSONParser(f);
		JsonObject obj = parser.readObject();
		Object[] insts = obj.getArray("institutions");
		for (Object instObj : insts) {
			JsonObject inst = (JsonObject) instObj;
			String id = inst.getInt("institutionId") + "";
			// String id = inst.getInt("institutionUnitAliasId") + "";
			CMSInstitution i = getInstitution(id);
			i.name = inst.getString("name");
			if (i.name != null)
				i.name = i.name.trim();
			// if (i.name.contains("Moscow"))
			// System.out.println("here");
			i.shortName = inst.getString("shortName");
			i.url = inst.getString("homepageurl");

			// String hashtag = null;
			if (inst.containsKey("twitterhash"))
				i.hashtag = inst.getString("twitterhash");
			// System.out.println(name + " (" + shortName + ") " + url + " - " + hashtag);
			if (inst.containsKey("latitude"))
				i.latitude = inst.getString("latitude");
			if (inst.containsKey("longitude"))
				i.longitude = inst.getString("longitude");

			// CMSDownloadHelper.downloadInstitution(id);

			/*JsonArray persons = team.getJsonArray("persons");
			if (persons != null) {
				for (JsonObject person : persons.getValuesAs(JsonObject.class)) {
					System.out.println("   firstname: " + person.getString("firstname"));
					System.out.println("   lastname: " + person.getString("lastname"));
					System.out.println("   role: " + person.getString("role"));
				}
			}*/
		}
	}

	protected static Info readContest(File folder) throws Exception {
		File f = new File(folder, "wf.json");

		InputStream is = new FileInputStream(f);
		JSONParser parser = new JSONParser(is);
		JsonObject obj = parser.readObject();
		JsonObject contest = obj.getJsonObject("contest");
		if (contest == null)
			contest = obj;

		Info info = new Info();
		String name = contest.getString("contestName");
		info.add("formal_name", name);
		String cId = contest.getString("contestId");
		contestIdList.add(cId);
		info.add("id", cId);
		String shortName = contest.getString("contestShortName");
		info.add("name", shortName);

		String start = contest.getString("contestStartDate");
		info.add("start_time", start + "T01:00:00+00:00");
		System.out.println(name + " (" + shortName + " - " + cId + ")");

		Object[] groups = contest.getArray("group");
		for (Object groupObj : groups) {
			JsonObject group = (JsonObject) groupObj;
			String id = group.getString("groupId");
			CMSGroup g = getGroup(id);
			g.name = group.getString("groupName");

			Object[] teams = group.getArray("team");
			if (teams == null)
				teams = group.getArray("teams");
			for (Object teamObj : teams) {
				JsonObject team = (JsonObject) teamObj;
				id = team.getString("teamId");

				// CMSDownloadHelper.downloadTeam(id);

				CMSTeam t = getTeam(id);
				t.name = team.getString("teamName");
				if (t.name != null)
					t.name = t.name.trim();

				t.number = team.getString("teamNumber");

				String iname = team.getString("institutionName");
				if (iname == null)
					iname = team.getString("instName");
				if (iname != null)
					iname = iname.trim();
				CMSInstitution i = getInstitutionByName(iname);
				if (i == null)
					System.err.println("no inst: " + iname);
				else {
					i.country = team.getString("country");
					i.group = g;
					t.inst = i;
				}
				t.group = g;

				/*JsonArray teamMembers = team.getJsonArray("teamMember");
				for (JsonObject member : teamMembers.getValuesAs(JsonObject.class)) {
					String firstName = member.getString("firstName");
					String lastName = member.getString("lastName");
				
					CMSMember m = getMember(firstName, lastName);
					m.role = member.getString("teamRole");
					m.team = t;
				}*/
			}
		}
		return info;
	}

	protected static CMSGroup getGroup(String id) {
		for (CMSGroup g : groupList) {
			if (g.id.equals(id))
				return g;
		}

		CMSGroup g = new CMSGroup();
		g.id = id;
		groupList.add(g);
		return g;
	}

	protected static CMSInstitution getInstitution(String id) {
		for (CMSInstitution i : institutionList) {
			if (i.id.equals(id))
				return i;
		}

		CMSInstitution i = new CMSInstitution();
		i.id = id;
		institutionList.add(i);
		return i;
	}

	protected static CMSInstitution getInstitutionByName(String name) {
		for (CMSInstitution i : institutionList) {
			if (i.name.equals(name))
				return i;
		}
		return null;
	}

	protected static CMSTeam getTeam(String id) {
		for (CMSTeam t : teamList) {
			if (t.id.equals(id))
				return t;
		}

		CMSTeam t = new CMSTeam();
		t.id = id;
		teamList.add(t);
		return t;
	}

	protected static CMSMember getMember(String id) {
		for (CMSMember m : memberList) {
			if (m.id.equals(id))
				return m;
		}

		CMSMember m = new CMSMember();
		m.id = id;
		memberList.add(m);
		return m;
	}

	protected static CMSStaffMember getStaffMember(String id) {
		for (CMSStaffMember m : staffMemberList) {
			if (m.id.equals(id))
				return m;
		}

		CMSStaffMember m = new CMSStaffMember();
		m.id = id;
		staffMemberList.add(m);
		return m;
	}

	protected static CMSMember getMember(String firstName, String lastName) {
		for (CMSMember m : memberList) {
			if (m.firstName.equals(firstName) && m.lastName.equals(lastName))
				return m;
		}

		CMSMember m = new CMSMember();
		m.firstName = firstName;
		m.lastName = lastName;
		memberList.add(m);
		return m;
	}
}