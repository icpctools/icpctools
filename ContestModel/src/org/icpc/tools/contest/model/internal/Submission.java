package org.icpc.tools.contest.model.internal;

import java.io.File;
import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;

public class Submission extends TimedEvent implements ISubmission {
	private static final String PROBLEM_ID = "problem_id";
	private static final String LANGUAGE_ID = "language_id";
	private static final String TEAM_ID = "team_id";
	private static final String PERSON_ID = "person_id";
	private static final String ENTRY_POINT = "entry_point";
	private static final String FILES = "files";
	private static final String REACTION = "reaction";

	private String teamId;
	private String personId;
	private String problemId;
	private String languageId;
	private String entryPoint;
	private FileReferenceList files;
	private FileReferenceList reaction;

	@Override
	public ContestType getType() {
		return ContestType.SUBMISSION;
	}

	@Override
	public String getTeamId() {
		return teamId;
	}

	@Override
	public String getPersonId() {
		return personId;
	}

	@Override
	public String getProblemId() {
		return problemId;
	}

	@Override
	public String getLanguageId() {
		return languageId;
	}

	@Override
	public String getEntryPoint() {
		return entryPoint;
	}

	@Override
	public File getFiles(boolean force) {
		return getFile(files.first(), FILES, force);
	}

	public FileReferenceList getFiles() {
		return files;
	}

	public FileReferenceList getReaction() {
		return reaction;
	}

	@Override
	public File getReaction(boolean force) {
		return getFile(reaction.first(), REACTION, force);
	}

	@Override
	public File[] getReactions(boolean force) {
		return getFiles(reaction, REACTION, force);
	}

	@Override
	public String getReactionURL() {
		if (reaction == null || reaction.isEmpty())
			return null;

		return reaction.first().href;
	}

	@Override
	public String[] getReactionURLs() {
		if (reaction == null || reaction.isEmpty())
			return null;

		return reaction.getHrefs();
	}

	@Override
	public Object resolveFileReference(String url) {
		return FileReferenceList.resolve(url, files, reaction);
	}

	public void setFiles(FileReferenceList list) {
		files = list;
	}

	public void setReaction(FileReferenceList list) {
		reaction = list;
	}

	@Override
	public IContestObject clone() {
		Submission s = new Submission();
		s.id = id;
		s.files = files;
		s.reaction = reaction;
		s.teamId = teamId;
		s.problemId = problemId;
		s.languageId = languageId;
		s.entryPoint = entryPoint;
		s.contestTime = contestTime;
		s.time = time;
		s.personId = personId;
		return s;
	}

	@Override
	protected boolean addImpl(String name, Object value) throws Exception {
		if (TEAM_ID.equals(name)) {
			teamId = (String) value;
			return true;
		} else if (PERSON_ID.equals(name)) {
			personId = (String) value;
			return true;
		} else if (PROBLEM_ID.equals(name)) {
			problemId = (String) value;
			return true;
		} else if (LANGUAGE_ID.equals(name)) {
			languageId = (String) value;
			return true;
		} else if (ENTRY_POINT.equals(name)) {
			entryPoint = (String) value;
			return true;
		} else if (FILES.equals(name)) {
			files = parseFileReference(value);
			return true;
		} else if (REACTION.equals(name)) {
			reaction = parseFileReference(value);
			return true;
		}
		return super.addImpl(name, value);
	}

	@Override
	protected void getProperties(Properties props) {
		props.addLiteralString(ID, id);
		props.addLiteralString(PROBLEM_ID, problemId);
		props.addLiteralString(TEAM_ID, teamId);
		props.addLiteralString(PERSON_ID, personId);
		props.addLiteralString(LANGUAGE_ID, languageId);
		props.addLiteralString(ENTRY_POINT, entryPoint);
		props.addFileRef(FILES, files);
		props.addFileRef(REACTION, reaction);
		super.getProperties(props);
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = super.validate(c);

		if (c.getLanguageById(languageId) == null)
			errors.add("Invalid language " + languageId);

		if (c.getProblemById(problemId) == null)
			errors.add("Invalid problem " + problemId);

		ITeam team = c.getTeamById(teamId);
		if (team == null)
			errors.add("Invalid team " + teamId);
		else if (!c.isTeamHidden(team)) {
			// check that the submission is after the start of the contest if it is a public team)
			if (c.getStartTime() == null || c.getStartTime() > getTime())
				errors.add("Submission occured before the contest started");
		}

		if (personId != null && c.getPersonById(personId) == null)
			errors.add("Invalid person " + personId);

		if (errors.isEmpty())
			return null;
		return errors;
	}
}