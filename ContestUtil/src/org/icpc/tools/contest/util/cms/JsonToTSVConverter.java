package org.icpc.tools.contest.util.cms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
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
 * Converter from ICPC CMS formats to both TSV and Contest API JSON formats.
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

	public static class CMSWinner {
		Organization inst;
		String year;
	}

	public static class CMSStaffMember {
		String id;
		String firstName;
		String lastName;
		String role;
	}

	protected static List<Group> groupList = new ArrayList<>();
	protected static List<Organization> orgList = new ArrayList<>();
	protected static List<Team> teamList = new ArrayList<>();
	protected static List<TeamMember> memberList = new ArrayList<>();
	protected static List<CMSStaffMember> staffMemberList = new ArrayList<>();
	protected static List<CMSWinner> winnerList = new ArrayList<>();
	protected static List<String> contestIdList = new ArrayList<>();

	protected static GoogleMapsGeocoder geocoder;

	public static void main(String[] args) {
		if (args == null || args.length < 1 || args.length > 4) {
			System.err.println("Usage: command [cmsRoot] [contestRoot] [googleMapsKey] [option]");
			System.exit(1);
		}

		File cmsRoot = new File(args[0]);
		File contestRoot = new File(args[1]);

		if (!cmsRoot.exists()) {
			System.err.println("CMS root folder does not exist");
			System.exit(1);
		}

		if (args.length >= 3)
			geocoder = new GoogleMapsGeocoder(args[2]);

		if (args.length == 4 && "--finals".equals(args[3]))
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
		orgList = new ArrayList<>();
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

			for (Team t : teamList)
				readTeamResults(t.getId());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void generateContest(File cmsRoot2, File contestRoot2) {
		groupList = new ArrayList<>();
		orgList = new ArrayList<>();
		teamList = new ArrayList<>();
		memberList = new ArrayList<>();
		staffMemberList = new ArrayList<>();

		// cmsRoot = new File("/Users/deboer/ICPC/cms4/2019");
		File cmsRoot = new File("/Users/deboer/Downloads/ICPC-WF/CLICS_CS_World-Finals-2020");
		File contestRoot = new File("/Users/deboer/git/wf2020-onsite/finals");
		// File instFolder = new File("/Users/deboer/ICPC/cms4/institutions");

		try {
			// load all institutions
			/*File[] files = instFolder.listFiles(new FileFilter() {
				@Override
				public boolean accept(File f) {
					try {
						Integer.parseInt(f.getName().substring(0, f.getName().length() - 5));
						return true;
					} catch (Exception e) {
						// ignore
					}
					return false;
				}
			});
			
			System.out.println("Loading " + files.length + " institutions");
			for (File f : files) {
				readInstitution(f);
			}*/
		} catch (Exception e) {
			e.printStackTrace();
		}

		Info info = null;
		try {
			System.out.println("Reading CMS data from tsvs: " + cmsRoot);
			File f = new File("/Users/deboer/Downloads/ICPC-WF/CLICS_CS_World-Finals-2020");
			TSVImporter.importGroups(groupList, f);
			TSVImporter.importInstitutions(orgList, f);
			TSVImporter.importTeams(teamList, f);
			TSVImporter.importTeamMembers(memberList, f);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			System.out.println("Reading CMS data from: " + cmsRoot);
			// File f = new File("/Users/deboer/ICPC/2020/finals/contestRoot/cms");
			// readInstitutions(f);
			/*readInstitutions(cmsRoot);
			readTeams(cmsRoot);
			info = readContest(cmsRoot);
			readStaff(cmsRoot);*/
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			// File f = new File("/Users/deboer/Downloads/ICPC-WF/contest");
			File f = new File("/Users/deboer/Downloads/ICPC-WF/World-Finals-2020");
			System.out.println("Reading CMS team members: " + f);
			TSVImporter.importTeamMembersTab(memberList, teamList, f);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			// remove 20107: invitational teams
			String invitationalId = "20107";

			// remove unused institutions
			List<Team> remove = new ArrayList<>();
			for (Team t : teamList) {
				String gid = t.getGroupIds()[0];
				if (invitationalId.equals(gid))
					remove.add(t);
			}

			System.out.println("Removing invitational: " + remove.size());
			for (Team t : remove)
				teamList.remove(t);

			Group invGroup = null;
			for (Group g : groupList) {
				if (invitationalId.equals(g.getId()))
					invGroup = g;
			}

			if (invGroup != null)
				groupList.remove(invGroup);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			// repad team org ID's - CMS sucks
			for (Team t : teamList) {
				if (t.getOrganizationId().length() < 4)
					t.add("organization_id", pad(t.getOrganizationId()));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			// repad orgs - CMS sucks
			for (Organization o : orgList) {
				if (o.getId().length() < 4)
					o.add("id", pad(o.getId()));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			System.out.println("Cleaning and sorting");
			teamList.removeIf(t -> t.getId() == null);
			memberList.removeIf(m -> m.getTeamId() == null);

			if (FINALS_NAMING) {
				for (Team t : teamList) {
					String orgId = t.getOrganizationId();
					for (Organization o : orgList) {
						if (o.getId().equals(orgId))
							t.add("display_name", o.getFormalName());
					}
				}
			}

			// remove unused institutions
			List<String> usedOrgs = new ArrayList<>();
			for (Team t : teamList) {
				String id = t.getOrganizationId();
				if (id != null)
					usedOrgs.add(id);
			}

			List<Organization> remove = new ArrayList<>();
			for (Organization o : orgList) {
				if (!usedOrgs.contains(o.getId()))
					remove.add(o);
			}
			System.out.println("Removing orgs: " + remove.size() + " (" + (orgList.size() - remove.size()) + " left)");
			for (Organization o : remove)
				orgList.remove(o);

			// sort
			groupList.sort((g1, g2) -> compare(g1.getId(), g2.getId()));
			teamList.sort((t1, t2) -> compare(t1.getId(), t2.getId()));
			orgList.sort((i1, i2) -> compare(i1.getId(), i2.getId()));
			memberList.sort((m1, m2) -> compare(m1.getTeamId(), m2.getTeamId()));
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (geocoder != null) {
			for (Organization o : orgList) {
				if (Double.isNaN(o.getLatitude()) || Double.isNaN(o.getLongitude())) {
					geocoder.geocode(o);
				}
			}
		}

		try {
			System.out.println("Writing to: " + contestRoot);

			File configFolder = new File(contestRoot, "config");
			configFolder.mkdirs();

			// older tsvs
			writeGroupsTSV(groupList, configFolder);
			writeTeamsTSV(teamList, configFolder, false);
			writeTeamsTSV(teamList, configFolder, true);
			writeInstitutionsTSV(orgList, configFolder, false);
			writeInstitutionsTSV(orgList, configFolder, true);
			writeMembersTSV(memberList, configFolder);
			writeStaffMembersTSV(staffMemberList, configFolder);

			writeContestJSON(info, configFolder);

			writeJSON(new File(contestRoot, "groups.json"), groupList);
			writeJSON(new File(contestRoot, "organizations.json"), orgList);
			writeJSON(new File(contestRoot, "teams.json"), teamList);
			writeJSON(new File(contestRoot, "team-members.json"), memberList);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void writeGroupsTSV(List<Group> groups, File folder) throws Exception {
		File f = new File(folder, "groups.tsv");
		FileWriter fw = new FileWriter(f);
		fw.write("File_Version\t1\n");
		for (Group group : groups) {
			fw.write(group.getId());
			fw.write("\t");
			fw.write(group.getName());
			fw.write("\n");
		}
		fw.close();
	}

	protected static void writeTeamsTSV(List<Team> teams, File folder, boolean two) throws Exception {
		File f = new File(folder, "teams.tsv");
		if (two)
			f = new File(folder, "teams2.tsv");
		FileWriter fw = new FileWriter(f);
		fw.write("File_Version\t2\n");
		for (Team team : teams) {
			Organization inst = getOrganizationById(team.getOrganizationId());
			if (team.getId() != null)
				fw.write(team.getId());
			fw.write("\t");
			fw.write(team.getICPCId());
			fw.write("\t");
			if (team.getGroupIds() != null && team.getGroupIds().length > 0)
				fw.write(team.getGroupIds()[0]);
			else
				System.err.println("Team with no group: " + team.getId() + " - " + team.getName());
			fw.write("\t");

			if (inst != null) {
				fw.write(team.getName());
				fw.write("\t");
				if (inst.getFormalName() != null)
					fw.write(inst.getFormalName());
				fw.write("\t");
				if (inst.getName() != null)
					fw.write(inst.getName());
				fw.write("\t");
				if (inst.getCountry() == null)
					System.err.println("No country: " + team.getName());
				else
					fw.write(inst.getCountry());
				if (two) {
					fw.write("\t");
					fw.write("INST-" + inst.getId());

					// TODO
					// fw.write("\t");
					// if (inst.latitude == null || inst.longitude == null)
					// System.out.println(team.id + "\t" + team.number + "\t" + inst.id + "\t" +
					// inst.name);
				}
				fw.write("\n");
			} else
				System.err.println("Team with no institution: " + team.getId() + " - " + team.getName());
		}
		fw.close();
	}

	private static final String PAD = "0000";

	private static String pad(String s) {
		if (s.length() < 4)
			return PAD.substring(0, 4 - s.length()) + s;
		return s;
	}

	protected static void writeInstitutionsTSV(List<Organization> teams, File folder, boolean two) throws Exception {
		File f = new File(folder, "institutions.tsv");
		if (two)
			f = new File(folder, "institutions2.tsv");
		FileWriter fw = new FileWriter(f);
		fw.write("File_Version\t0.5\n");
		int hashCount = 0;
		int locCount = 0;
		for (Organization inst : teams) {
			fw.write("INST-U-" + inst.getId());
			fw.write("\t");
			if (inst.getFormalName() != null)
				fw.write(inst.getFormalName());
			fw.write("\t");
			if (inst.getName() != null)
				fw.write(inst.getName());
			fw.write("\t");
			Team team = getTeamFromOrganization(inst.getId());
			if (team != null && team.getGroupIds() != null && team.getGroupIds().length > 0)
				fw.write(team.getGroupIds()[0]);
			else
				System.err.println("Institution not in a group: " + inst.getFormalName());
			fw.write("\t");
			if (inst.getCountry() != null)
				fw.write(inst.getCountry());
			else
				System.err.println("Institution not in a country: " + inst.getFormalName());
			if (two) {
				fw.write("\t");
				if (inst.getURL() != null && !inst.getURL().isEmpty())
					fw.write(inst.getURL());
				// else
				// System.err.println("No URL: " + inst.id + " - " + inst.name);
				fw.write("\t");
				if (inst.getHashtag() != null && !inst.getHashtag().isEmpty())
					fw.write(inst.getHashtag());
				else {
					if (OUTPUT_MISSING_NAMES)
						System.out.println("No hashtag: " + inst.getId() + " - " + inst.getFormalName());
					hashCount++;
				}
				fw.write("\t");
				if (!Double.isNaN(inst.getLatitude()))
					fw.write(inst.getLatitude() + "");
				fw.write("\t");
				if (!Double.isNaN(inst.getLongitude()))
					fw.write(inst.getLongitude() + "");
				if (Double.isNaN(inst.getLatitude()) || Double.isNaN(inst.getLongitude())) {
					if (OUTPUT_MISSING_NAMES)
						System.out.println("No location: " + inst.getId() + " - " + inst.getFormalName());
					locCount++;
				}
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
		if (info == null) {
			System.err.println("No contest info");
			return;
		}
		File f = new File(folder, "contest.json");
		PrintWriter pw = new PrintWriter(f);
		JSONEncoder je = new JSONEncoder(pw);

		je.open();
		info.writeBody(je);
		je.close();

		pw.close();
	}

	protected static void writeJSON(File f, List<?> list) throws Exception {
		PrintWriter pw = new PrintWriter(f);
		JSONArrayWriter writer = new JSONArrayWriter(pw);
		writer.writePrelude();
		boolean first = true;
		for (Object o : list) {
			if (!first)
				writer.writeSeparator();
			else
				first = false;
			writer.write((IContestObject) o);
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

	protected static void writeMembersTSV(List<TeamMember> members, File folder) throws Exception {
		File f = new File(folder, "members.tsv");
		FileWriter fw = new FileWriter(f);
		fw.write("File_Version\t1\n");
		for (TeamMember member : members) {
			fw.write(member.getTeamId());
			fw.write("\t");
			fw.write(member.getRole().substring(0, 1).toUpperCase());
			fw.write(member.getRole().substring(1));
			fw.write("\t");
			fw.write(member.getFirstName());
			fw.write("\t");
			fw.write(member.getLastName());
			fw.write("\t");
			if ("male".equals(member.getSex()))
				fw.write("M");
			else if ("female".equals(member.getSex()))
				fw.write("F");
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
			// Team t =
			getTeamByICPCId(tId);

			if (team.containsKey("persons")) {
				Object[] persons = (Object[]) team.get("persons");
				for (Object personObj : persons) {
					JsonObject person = (JsonObject) personObj;
					String pId = person.getInt("personId") + "";
					TeamMember m = getMemberByICPCId(pId);
					m.add("team_id", team.getInt("workstationId") + "");
					m.add("first_name", person.getString("firstname"));
					m.add("last_name", person.getString("lastname"));
					if (person.containsKey("sex")) {
						String sex = person.getString("sex");
						if ("M".equalsIgnoreCase(sex))
							m.add("sex", "male");
						else if ("F".equalsIgnoreCase(sex))
							m.add("sex", "female");
					}
					String role = person.getString("role").toLowerCase();
					// if ("co-coach".equals(role))
					// role = "coach";
					m.add("role", role);
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
			Organization i = getOrganizationById(id);
			if (inst.getString("name") != null)
				i.add("formal_name", inst.getString("name").trim());
			i.add("name", inst.getString("shortName"));
			i.add("url", inst.getString("homepageurl"));

			if (inst.containsKey("twitterhash"))
				i.add("twitter_hashtag", inst.getString("twitterhash"));
			if (inst.containsKey("latitude") || inst.containsKey("longitude"))
				i.add("location", "{\"latitude\":" + inst.getString("latitude") + ",\"longitude\":"
						+ inst.getString("longitude") + "}");

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

	protected static void readInstitution(File f) throws Exception {
		JSONParser parser = new JSONParser(f);
		JsonObject obj = parser.readObject();
		JsonObject inst = obj;
		String id = inst.getInt("institutionId") + "";
		// String id = inst.getInt("institutionUnitAliasId") + "";
		Organization i = getOrganizationById(id);
		if (inst.getString("name") != null)
			i.add("formal_name", inst.getString("name").trim());
		i.add("name", inst.getString("shortName"));
		i.add("url", inst.getString("homepageurl"));

		if (inst.containsKey("twitterhash"))
			i.add("twitter_hashtag", inst.getString("twitterhash"));
		if (inst.containsKey("latitude") || inst.containsKey("longitude"))
			i.add("location",
					"{\"latitude\":" + inst.getString("latitude") + ",\"longitude\":" + inst.getString("longitude") + "}");
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
			Group g = getGroup(id);
			g.add("name", group.getString("groupName"));

			Object[] teams = group.getArray("team");
			if (teams == null)
				teams = group.getArray("teams");
			for (Object teamObj : teams) {
				JsonObject team = (JsonObject) teamObj;
				id = team.getString("teamId");

				// CMSDownloadHelper.downloadTeam(id);

				Team t = getTeamByICPCId(id);
				if (team.getString("teamName") != null)
					t.add("name", team.getString("teamName").trim());

				t.add("id", team.getString("teamNumber"));

				String iname = team.getString("institutionName");
				if (iname == null)
					iname = team.getString("instName");
				if (iname != null)
					iname = iname.trim();
				Organization i = getOrganizationByFormalName(iname);
				if (i == null)
					System.err.println("no inst: " + iname);
				else {
					i.add("country", team.getString("country"));
					t.add("organization_id", i.getId());
				}
				// t.add("group_ids", "[\"" + g.getId() + "\"]");
				t.add("group_ids", new String[] { g.getId() });

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

	private static Group getGroup(String id) {
		for (Group g : groupList) {
			if (g.getId().equals(id))
				return g;
		}

		Group g = new Group();
		g.add("id", id);
		g.add("icpc_id", id);
		groupList.add(g);
		return g;
	}

	protected static Organization getOrganizationById(String id) {
		for (Organization o : orgList) {
			if (o.getId().equals(id))
				return o;
		}

		if (id == null || "null".equals(id))
			Trace.trace(Trace.ERROR, "null id");

		Organization o = new Organization();
		o.add("id", id);
		o.add("icpc_id", id);
		orgList.add(o);
		return o;
	}

	private static Organization getOrganizationByFormalName(String name) {
		for (Organization o : orgList) {
			if (o.getFormalName().equals(name))
				return o;
		}
		return null;
	}

	private static Team getTeamFromOrganization(String orgId) {
		for (Team t : teamList) {
			if (t.getOrganizationId().equals(orgId))
				return t;
		}

		return null;
	}

	private static Team getTeamByICPCId(String id) {
		for (Team t : teamList) {
			if (t.getICPCId().equals(id))
				return t;
		}

		Team t = new Team();
		t.add("id", id);
		t.add("icpc_id", id);
		teamList.add(t);
		return t;
	}

	private static CMSStaffMember getStaffMember(String id) {
		for (CMSStaffMember m : staffMemberList) {
			if (m.id.equals(id))
				return m;
		}

		CMSStaffMember m = new CMSStaffMember();
		m.id = id;
		staffMemberList.add(m);
		return m;
	}

	private static TeamMember getMemberByICPCId(String id) {
		for (TeamMember m : memberList) {
			if (m.getICPCId().equals(id))
				return m;
		}

		TeamMember m = new TeamMember();
		m.add("id", id);
		m.add("icpc_id", id);
		memberList.add(m);
		return m;
	}
}