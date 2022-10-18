package org.icpc.tools.contest.util.cms;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.feed.JSONArrayWriter;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.internal.Group;
import org.icpc.tools.contest.model.internal.Info;
import org.icpc.tools.contest.model.internal.Organization;
import org.icpc.tools.contest.model.internal.Person;
import org.icpc.tools.contest.model.internal.Team;

/**
 * Converter from ICPC CMS formats to both clean TSV and Contest API JSON formats.
 *
 * Arguments: cmsClicsRoot cmsContestRoot contestRoot [--finals] [--auto-assign-team-ids]
 *
 * cmsClicsRoot - the folder containing downloaded CMS clics files
 *
 * cmsContestRoot - the folder containing downloaded CMS contest files
 *
 * contestRoot - a contest location, i.e. CDP/CAF root folder
 *
 * Use --finals to include a display_name in teams.tsv as used at ICPC World Finals. Use
 * --auto-assign-team-ids to auto assign team ids if they haven't been set yet
 */
public class JsonToTSVConverter {
	private static boolean FINALS_NAMING = false;
	private static boolean AUTO_ASSIGN_TEAM_IDS = false;
	private static final boolean OUTPUT_MISSING_NAMES = true;

	protected static List<Group> groupList = new ArrayList<>();
	protected static List<Organization> orgList = new ArrayList<>();
	protected static List<Team> teamList = new ArrayList<>();
	protected static List<Person> personList = new ArrayList<>();
	protected static List<String> contestIdList = new ArrayList<>();

	protected static GoogleMapsGeocoder geocoder;

	public static void main(String[] args) {
		if (args == null || args.length < 1 || args.length > 6) {
			System.err.println("Usage: command [cmsClicsRoot] [cmsContestRoot] [contestRoot] [googleMapsKey] [option]");
			System.exit(1);
		}

		File cmsClicsRoot = new File(args[0]);
		File cmsContestRoot = new File(args[1]);
		File contestRoot = new File(args[2]);

		if (!cmsClicsRoot.exists()) {
			System.err.println("CMS CLICS root folder does not exist");
			System.exit(1);
		}

		if (!cmsContestRoot.exists()) {
			System.err.println("CMS contest root folder does not exist");
			System.exit(1);
		}

		if (args.length >= 4)
			geocoder = new GoogleMapsGeocoder(args[3]);

		if (args.length >= 5 && "--finals".equals(args[4]))
			FINALS_NAMING = true;

		if (args.length >= 6 && "--finals".equals(args[5]))
			FINALS_NAMING = true;

		if (args.length >= 5 && "--auto-assign-team-ids".equals(args[4]))
			AUTO_ASSIGN_TEAM_IDS = true;

		if (args.length >= 6 && "--auto-assign-team-ids".equals(args[5]))
			AUTO_ASSIGN_TEAM_IDS = true;

		generateContest(cmsClicsRoot, cmsContestRoot, contestRoot);
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

	protected static void generateContest(File cmsClicsRoot, File cmsContestRoot, File contestRoot) {
		groupList = new ArrayList<>();
		orgList = new ArrayList<>();
		teamList = new ArrayList<>();
		personList = new ArrayList<>();

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
			System.out.println("Reading CMS clics data from: " + cmsClicsRoot);
			TSVImporter.importGroups(groupList, cmsClicsRoot);
			TSVImporter.importInstitutions(orgList, cmsClicsRoot);
			TSVImporter.importTeams(teamList, cmsClicsRoot, AUTO_ASSIGN_TEAM_IDS);
			TSVImporter.importTeamMembers(personList, cmsClicsRoot);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			System.out.println("Reading CMS contest persons: " + cmsContestRoot);
			TSVImporter.importPersonsTab(personList, teamList, cmsContestRoot);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			// repad team org ID's - CMS sucks
			for (Team t : teamList) {
				if (t.getOrganizationId().length() < 4)
					t.add("organization_id", t.getOrganizationId());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			System.out.println("Cleaning and sorting");
			teamList.removeIf(t -> t.getId() == null);
			// personList.removeIf(m -> m.getTeamId() == null);

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
			System.out
					.println("Removing unused orgs: " + remove.size() + " (" + (orgList.size() - remove.size()) + " left)");
			for (Organization o : remove)
				orgList.remove(o);

			// sort
			groupList.sort((g1, g2) -> compare(g1.getId(), g2.getId()));
			teamList.sort((t1, t2) -> compare(t1.getId(), t2.getId()));
			orgList.sort((i1, i2) -> compare(i1.getId(), i2.getId()));
			personList.sort((m1, m2) -> compare(m1.getTeamId(), m2.getTeamId()));
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

			writeContestJSON(info, configFolder);

			writeJSON(new File(contestRoot, "groups.json"), groupList);
			writeJSON(new File(contestRoot, "organizations.json"), orgList);
			writeJSON(new File(contestRoot, "teams.json"), teamList);
			writeJSON(new File(contestRoot, "persons.json"), personList);
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

	private static Team getTeamFromOrganization(String orgId) {
		for (Team t : teamList) {
			if (t.getOrganizationId().equals(orgId))
				return t;
		}

		return null;
	}
}