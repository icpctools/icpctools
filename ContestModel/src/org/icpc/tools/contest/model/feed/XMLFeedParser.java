package org.icpc.tools.contest.model.feed;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IJudgementType;
import org.icpc.tools.contest.model.ILanguage;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Award;
import org.icpc.tools.contest.model.internal.Clarification;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.icpc.tools.contest.model.internal.Group;
import org.icpc.tools.contest.model.internal.Info;
import org.icpc.tools.contest.model.internal.Judgement;
import org.icpc.tools.contest.model.internal.JudgementType;
import org.icpc.tools.contest.model.internal.Language;
import org.icpc.tools.contest.model.internal.Organization;
import org.icpc.tools.contest.model.internal.Problem;
import org.icpc.tools.contest.model.internal.Run;
import org.icpc.tools.contest.model.internal.State;
import org.icpc.tools.contest.model.internal.Submission;
import org.icpc.tools.contest.model.internal.Team;
import org.icpc.tools.contest.model.util.AwardUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLFeedParser implements Closeable {
	private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

	protected static final String INFO = "info";
	protected static final String REGION = "region";
	protected static final String LANGUAGE = "language";
	protected static final String PROBLEM = "problem";
	protected static final String TEAM = "team";
	protected static final String RUN = "run";
	protected static final String JUDGEMENT = "judgement";
	protected static final String TESTCASE = "testcase";
	protected static final String CLAR = "clar";
	protected static final String FINALIZED = "finalized";
	protected static final String AWARD = "award";

	private static final String ID = "id";
	private static final String ICPC_ID = "icpc_id";
	private static final String NAME = "name";
	private static final String LABEL = "label";
	private static final String LETTER = "letter";
	private static final String TIME = "time";
	private static final String CONTEST_TIME = "contest_time";
	private static final String START_TIME = "start_time";
	private static final String START_CONTEST_TIME = "start_contest_time";
	private static final String END_TIME = "end_time";
	private static final String END_CONTEST_TIME = "end_contest_time";
	private static final String TIMESTAMP = "timestamp";

	protected boolean inferJudgementTypes = true;
	protected InputStream feedIn;
	protected boolean closed;

	static class Property {
		public String name;
		public String value;

		public Property(String name, String value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public String toString() {
			return name + " = " + value;
		}
	}

	/**
	 * Helper method to read a new contest from the given stream and attach a listener.
	 *
	 * @param in an input stream
	 * @param listener a contest listener
	 * @return a new Contest
	 * @throws Exception
	 */
	public static Contest importFromStream(InputStream in, IContestListener listener) throws Exception {
		Contest contest = new Contest();
		if (listener != null)
			contest.addListener(listener);

		XMLFeedParser parser = new XMLFeedParser();
		parser.parse(contest, in);
		return contest;
	}

	public void parse(final Contest contest, InputStream in) throws Exception {
		if (in == null)
			return;

		feedIn = in;

		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser sp = factory.newSAXParser();

		try {
			sp.parse(in, new DefaultHandler() {
				protected List<Property> list = new ArrayList<>(10);
				protected int level;
				private StringBuilder val = new StringBuilder();

				@Override
				public void characters(char[] ch, int start, int length) throws SAXException {
					val.append(ch, start, length);
				}

				@Override
				public void startElement(String uri, String localName, String name, Attributes attributes)
						throws SAXException {
					if (level == 0 && !name.equals("contest"))
						Trace.trace(Trace.ERROR, "Not a contest!!");
					else if (level == 1)
						list.clear();

					level++;
				}

				@Override
				public void endElement(String uri, String localName, String name) throws SAXException {
					try {
						if (level == 2)
							createContestObject(contest, name, list);
						else if (level == 3) {
							list.add(new Property(name, val.toString().trim()));
							val = new StringBuilder();
						}
						level--;
					} catch (Exception e) {
						Trace.trace(Trace.WARNING, "Error parsing", e);
					}
				}
			});
		} catch (SAXParseException e) {
			if (closed)
				return;
			Trace.trace(Trace.ERROR, "Could not parse event feed: '" + e.getMessage() + "' on line " + e.getLineNumber());
			throw new IOException("Error parsing event feed");
		}
	}

	@Override
	public void close() {
		closed = true;
		if (feedIn != null) {
			try {
				feedIn.close();
			} catch (Exception e) {
				// ignore
			}
			feedIn = null;
		}
	}

	private static void add(ContestObject obj, String name, String value) {
		obj.add(name, value);
	}

	protected void createContestObject(Contest contest, String name, List<Property> list) {
		if (INFO.equals(name)) {
			Info info = new Info();
			for (Property p : list) {
				if ("contest-id".equals(p.name))
					add(info, ID, p.value);
				else if ("title".equals(p.name))
					add(info, "formal_name", p.value);
				else if ("short-title".equals(p.name))
					add(info, "name", p.value);
				else if ("starttime".equals(p.name)) {
					try {
						double d = Double.parseDouble(p.value);
						add(info, "start_time", Timestamp.format((long) (d * 1000.0)));
					} catch (Exception e) {
						// ignore
					}
				} else if ("length".equals(p.name))
					add(info, "duration", p.value);
				else if ("scoreboard-freeze-length".equals(p.name))
					add(info, "scoreboard_freeze_duration", p.value);
				else if ("penalty".equals(p.name))
					add(info, "penalty_time", p.value);
			}

			if (info.getId() == null)
				add(info, ID, "id-" + Math.random());

			if (info.getName() == null)
				add(info, "name", info.getActualFormalName());

			contest.add(info);
		} else if (TEAM.equals(name)) {
			String instId = null;
			Organization org = new Organization();
			for (Property p : list) {
				if (ID.equals(p.name)) {
					add(org, ID, p.value);
					instId = p.value;
				} else if ("university".equals(p.name)) {
					add(org, "formal_name", p.value);
				} else if ("university-short-name".equals(p.name)) {
					add(org, "name", p.value);
				} else if ("nationality".equals(p.name)) {
					add(org, "country", p.value);
				}
			}

			if (org.getName() == null)
				add(org, "name", org.getActualFormalName());

			boolean exists = false;
			for (IOrganization org2 : contest.getOrganizations()) {
				if (org2.getActualFormalName().equals(org.getActualFormalName())) {
					exists = true;
					instId = org2.getId();
				}
			}

			if (org.getName() == null || org.getName().isEmpty())
				add(org, "name", org.getActualFormalName());
			if (!exists)
				contest.add(org);

			Team team = new Team();
			for (Property p : list) {
				if (ID.equals(p.name)) {
					add(team, ID, p.value);
				} else if ("external-id".equals(p.name))
					add(team, ICPC_ID, p.value);
				else if (NAME.equals(p.name))
					add(team, NAME, p.value);
				else if ("region".equals(p.name)) {
					IGroup[] groups = contest.getGroups();
					for (IGroup g : groups) {
						if (g.getName().equals(p.value))
							add(team, "group_id", g.getId());
					}
				}
			}

			add(team, "organization_id", instId);

			// if we already have a team name, let it stand
			ITeam existing = contest.getTeamById(team.getId());
			if (existing != null && existing.getName() != null)
				add(team, NAME, existing.getName());

			contest.add(team);
		} else if (PROBLEM.equals(name)) {
			Problem problem = new Problem();
			for (Property p : list) {
				if (ID.equals(p.name)) {
					try {
						add(problem, "ordinal", (Integer.parseInt(p.value) - 1) + "");
					} catch (Exception e) {
						add(problem, "ordinal", p.value);
					}
				} else if (LABEL.equals(p.name))
					add(problem, LABEL, p.value);
				else if (LETTER.equals(p.name))
					add(problem, LABEL, p.value);
				else if (NAME.equals(p.name))
					add(problem, NAME, p.value);
				else if ("color".equals(p.name))
					add(problem, "color", p.value);
				else if ("rgb".equals(p.name))
					add(problem, "rgb", p.value);
				else if ("test_data_count".equals(p.name))
					add(problem, "test_data_count", p.value);
			}

			// find a problem with matching ordinal
			IProblem[] probs = contest.getProblems();
			for (IProblem p : probs) {
				if (p.getOrdinal() == problem.getOrdinal()) {
					add(problem, ID, p.getId());
					if (problem.getLabel() == null)
						add(problem, LABEL, p.getLabel());
				}
			}
			if (problem.getId() == null) {
				// assume ordinal is an index
				try {
					// if no id, assume ordinal A = 0, B = 1, etc.
					int i = problem.getOrdinal();
					if (i >= 0 && i < LETTERS.length()) {
						add(problem, ID, LETTERS.charAt(i) + "");
						if (problem.getLabel() == null)
							add(problem, LABEL, LETTERS.charAt(i) + "");
					}
				} catch (Exception e) {
					// ignore
				}
			}

			// last attempt: if no label, use the id
			if (problem.getLabel() == null)
				add(problem, LABEL, problem.getId());

			contest.add(problem);
		} else if (REGION.equals(name)) {
			Group group = new Group();
			for (Property p : list) {
				if ("external-id".equals(p.name)) {
					add(group, ICPC_ID, p.value);
					add(group, ID, p.value);
				} else if (NAME.equals(p.name))
					add(group, NAME, p.value);
			}

			contest.add(group);
		} else if (JUDGEMENT.equals(name)) {
			JudgementType type = new JudgementType();
			for (Property p : list) {
				if ("acronym".equals(p.name)) {
					add(type, ID, p.value);
				} else if (NAME.equals(p.name)) {
					add(type, NAME, p.value);
				} else if ("penalty".equals(p.name)) {
					add(type, "penalty", p.value);
					inferJudgementTypes = false;
				} else if ("solved".equals(p.name)) {
					add(type, "solved", p.value);
					inferJudgementTypes = false;
				}
			}
			contest.add(type);
		} else if (RUN.equals(name)) {
			Submission s = new Submission();
			for (Property p : list) {
				if (ID.equals(p.name)) {
					add(s, ID, p.value);
				} else if ("language".equals(p.name)) {
					ILanguage[] langs = contest.getLanguages();
					for (ILanguage l : langs) {
						if (l.getName().equals(p.value))
							add(s, "language_id", l.getId());
					}
				} else if ("problem".equals(p.name)) {
					String pId = p.value;
					try {
						pId = (Integer.parseInt(pId) - 1) + "";
					} catch (Exception e) {
						//
					}
					IProblem[] probs = contest.getProblems();
					for (IProblem pp : probs) {
						if (pId.equals(pp.getOrdinal() + "")) {
							pId = pp.getId();
							break;
						}
					}
					add(s, "problem_id", pId);
				} else if ("team".equals(p.name)) {
					add(s, "team_id", p.value);
				} else if (TIME.equals(p.name)) {
					add(s, CONTEST_TIME, RelativeTime.format(RelativeTime.parseOld(p.value)));
				} else if (TIMESTAMP.equals(p.name)) {
					add(s, TIME, Timestamp.format(Timestamp.parseOld(p.value)));
				}
			}

			// don't change submission time
			ISubmission oldS = contest.getSubmissionById(s.getId());
			if (oldS != null)
				add(s, TIME, Timestamp.format(oldS.getTime()));

			checkContestState(contest, s.getContestTime());
			contest.add(s);

			for (Property pp : list) {
				if ("judged".equals(pp.name) && "true".equalsIgnoreCase(pp.value)) {
					Judgement sj = new Judgement();
					boolean solved = false;
					boolean penalty = false;
					IJudgementType type = null;
					for (Property p : list) {
						if (ID.equals(p.name)) {
							add(sj, ID, p.value);
							add(sj, "submission_id", p.value);
						} else if (TIME.equals(p.name)) {
							add(sj, START_CONTEST_TIME, RelativeTime.format(RelativeTime.parseOld(p.value)));
							add(sj, END_CONTEST_TIME, RelativeTime.format(RelativeTime.parseOld(p.value)));
						} else if (TIMESTAMP.equals(p.name)) {
							add(sj, START_TIME, Timestamp.format(Timestamp.parseOld(p.value)));
							add(sj, END_TIME, Timestamp.format(Timestamp.parseOld(p.value)));
						} else if ("result".equals(p.name)) {
							add(sj, "judgement_type_id", p.value);
							type = contest.getJudgementTypeById(p.value);
						} else if ("solved".equals(p.name)) {
							solved = "true".equalsIgnoreCase(p.value);
						} else if ("penalty".equals(p.name)) {
							penalty = "true".equalsIgnoreCase(p.value);
						}
					}
					if (inferJudgementTypes && type != null && (solved || penalty)) {
						boolean update = false;
						JudgementType typeMatch = (JudgementType) ((JudgementType) type).clone();
						if (solved && !typeMatch.isSolved()) {
							add(typeMatch, "solved", "true");
							update = true;
						}
						if (penalty && !typeMatch.isPenalty()) {
							add(typeMatch, "penalty", "true");
							update = true;
						}
						if (update)
							contest.add(typeMatch);
					}
					checkContestState(contest, sj.getStartContestTime());
					checkContestState(contest, sj.getEndContestTime());
					contest.add(sj);
				}
			}
		} else if (LANGUAGE.equals(name)) {
			Language l = new Language();
			for (Property p : list) {
				if (ID.equals(p.name)) {
					add(l, ID, p.value);
				} else if (NAME.equals(p.name)) {
					add(l, NAME, p.value);
				}
			}
			contest.add(l);
		} else if (TESTCASE.equals(name)) {
			Run run = new Run();
			String i = null;
			String n = null;
			String runId = null;
			for (Property p : list) {
				if ("i".equals(p.name)) {
					i = p.value;
					add(run, "ordinal", p.value);
					// add(run, "i", p.value);
				} else if ("n".equals(p.name)) {
					n = p.value;
					// add(run, "n", p.value);
				} else if ("run-id".equals(p.name)) {
					runId = p.value;
					add(run, "judgement_id", p.value);
				} else if ("judgement".equals(p.name)) {
					add(run, "judgement_type_id", p.value);
				} else if (TIME.equals(p.name)) {
					add(run, CONTEST_TIME, RelativeTime.format(RelativeTime.parseOld(p.value)));
				} else if (TIMESTAMP.equals(p.name)) {
					add(run, TIME, Timestamp.format(Timestamp.parseOld(p.value)));
				}
			}
			checkContestState(contest, run.getContestTime());
			add(run, ID, runId + "-" + i);

			ISubmission s = contest.getSubmissionById(runId);
			if (s != null) {
				IProblem p = contest.getProblemById(s.getProblemId());
				int nn = Integer.parseInt(n);
				if (p != null && p.getTestDataCount() < nn) {
					Problem pp = (Problem) ((Problem) p).clone();
					add(pp, "test_data_count", n);
					contest.add(pp);
				}
			}

			// make sure judgement exists before runs that refer to it
			IJudgement j = contest.getJudgementById(runId);
			if (j == null) {
				Judgement sj = new Judgement();
				add(sj, "id", runId);
				add(sj, "submission_id", runId);

				ISubmission ss = contest.getSubmissionById(runId);
				if (ss != null) {
					add(sj, START_CONTEST_TIME, RelativeTime.format(ss.getContestTime()));
					add(sj, START_TIME, Timestamp.format(ss.getTime()));
				}
				contest.add(sj);
			}

			contest.add(run);
		} else if (CLAR.equals(name)) {
			Clarification clar = new Clarification();

			String id = null;
			String question = null;
			String answer = null;
			String team = null;
			boolean toAll = false;
			for (Property p : list) {
				if (ID.equals(p.name)) {
					id = p.value;
				} else if ("team".equals(p.name)) {
					team = p.value;
				} else if ("problem".equals(p.name)) {
					String pId = p.value;
					try {
						pId = (Integer.parseInt(p.value) - 1) + "";
					} catch (Exception e) {
						// ignore
					}

					IProblem[] probs = contest.getProblems();
					for (IProblem pr : probs)
						if ((pr.getOrdinal() + "").equals(pId))
							add(clar, "problem_id", pr.getId());
					if (clar.getProblemId() == null)
						add(clar, "problem_id", pId);
				} else if ("question".equals(p.name)) {
					question = p.value;
				} else if ("answer".equals(p.name)) {
					answer = p.value;
				} else if ("to-all".equals(p.name)) {
					toAll = Boolean.valueOf(p.value);
				} else if (TIME.equals(p.name)) {
					add(clar, CONTEST_TIME, RelativeTime.format(RelativeTime.parseOld(p.value)));
				} else if (TIMESTAMP.equals(p.name)) {
					add(clar, TIME, Timestamp.format(Timestamp.parseOld(p.value)));
				}
			}
			if (answer != null && !answer.trim().isEmpty()) {
				add(clar, ID, id + "-reply");
				add(clar, "reply_to_id", id);
				add(clar, "text", answer);
				if (!toAll)
					add(clar, "to_team_id", team);
			} else {
				add(clar, ID, id);
				add(clar, "text", question);
				add(clar, "from_team_id", team);
			}
			contest.add(clar);
		} else if (FINALIZED.equals(name)) {
			int gold = 4;
			int silver = 4;
			int bronze = 4;
			for (Property p : list) {
				if ("last-gold".equals(p.name))
					gold = Integer.parseInt(p.value);
				else if ("last-silver".equals(p.name))
					silver = Integer.parseInt(p.value);
				else if ("last-bronze".equals(p.name))
					bronze = Integer.parseInt(p.value);
			}

			// convert to actual numbers, not last
			bronze -= silver;
			silver -= gold;

			State state = (State) contest.getState();
			Long startTime = contest.getStartTime();
			if (state != null && !state.isFinal() && startTime != null) {
				// end the contest
				state = (State) state.clone();
				Long time = startTime + contest.getDuration();
				state.setEnded(time);
				state.setThawed(time);
				state.setFinalized(time);
				contest.add(state);

				// add awards
				AwardUtil.createMedalAwards(contest, gold, silver, bronze);

				// end of updates
				state = (State) state.clone();
				state.setEndOfUpdates(time);
				contest.add(state);
			}
			// contest.add(f);
		} else if (AWARD.equals(name)) {
			Award award = new Award();
			List<String> teamIds = new ArrayList<>();
			for (Property p : list) {
				if ("teamId".equals(p.name))
					teamIds.add(p.value);
				else if (ID.equals(p.name))
					add(award, ID, p.value);
				else if ("citation".equals(p.name))
					add(award, p.name, p.value);
				else if ("show".equals(p.name))
					add(award, "show", p.value);
			}
			award.add("team_ids", "[\"" + String.join("\",\"", teamIds) + "\"]");

			contest.add(award);
		}
	}

	private static void checkContestState(Contest contest, long time) {
		State state = (State) contest.getState();
		if (state == null)
			state = new State();
		long freezeTime = contest.getDuration() - contest.getFreezeDuration();

		Long startTime = contest.getStartTime();
		if (startTime != null) {
			if (!state.isRunning()) {
				state = (State) state.clone();
				state.setStarted(startTime);
				contest.add(state);
			} else if (contest.getFreezeDuration() > 0 && !state.isFrozen() && time >= (freezeTime)) {
				state = (State) state.clone();
				state.setFrozen(startTime + freezeTime);
				contest.add(state);
			}
		}
	}
}