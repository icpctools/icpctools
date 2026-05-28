package org.icpc.tools.cds.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.cds.ConfiguredContest.View;
import org.icpc.tools.cds.video.ReactionVideoRecorder;
import org.icpc.tools.cds.video.VideoAggregator;
import org.icpc.tools.cds.video.VideoAggregator.ConnectionMode;
import org.icpc.tools.cds.video.VideoStream;
import org.icpc.tools.cds.video.VideoStream.StreamType;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObject.ContestType;
import org.icpc.tools.contest.model.IDelete;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.contest.model.internal.Award;
import org.icpc.tools.contest.model.internal.Clarification;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.icpc.tools.contest.model.internal.FileReference;
import org.icpc.tools.contest.model.internal.FileReferenceList;
import org.icpc.tools.contest.model.internal.Group;
import org.icpc.tools.contest.model.internal.Info;
import org.icpc.tools.contest.model.internal.Judgement;
import org.icpc.tools.contest.model.internal.Organization;
import org.icpc.tools.contest.model.internal.Person;
import org.icpc.tools.contest.model.internal.Problem;
import org.icpc.tools.contest.model.internal.Run;
import org.icpc.tools.contest.model.internal.State;
import org.icpc.tools.contest.model.internal.Submission;
import org.icpc.tools.contest.model.internal.Team;

/**
 * CDS Contest enables most of the CDS capabilities and behaviours on a contest:
 * <ul>
 * <li>Applies configuration defaults</li>
 * <li>Adds video streams to teams, and cuts them off at contest freeze</li>
 * <li>Downloads file references for contest objects</li>
 * <li>Logs contest state change events (e.g. 'Contest started')</li>
 * <li>Records reactions</li>
 * <li>Does view filtering (contest subset)</li>
 * <li>Enables countdown pause times (for CCSs that don't support it)</li>
 * </ul>
 *
 */
public class CDSContest extends Contest {
	private static final String LOGO = "logo";
	private static final String PHOTO = "photo";
	private static final String VIDEO = "video";
	private static final String BANNER = "banner";
	private static final String FILES = "files";
	private static final String REACTION = "reaction";
	private static final String COUNTRY_FLAG = "country_flag";
	private static final String COUNTRY_SUBDIVISION_FLAG = "country_subdivison_flag";
	private static final String PACKAGE = "package";
	private static final String STATEMENT = "statement";
	private static final String ATTACHMENTS = "attachments";
	private static final String BACKUP = "backup";
	private static final String KEY_LOG = "key_log";
	private static final String TOOL_DATA = "tool_data";

	private ConfiguredContest cc;
	private String contestId;
	private boolean recordReactions;
	private Contest defaultConfig = new Contest(false);

	private State[] currentState = new State[1];

	public CDSContest(ConfiguredContest cc) {
		contestId = cc.getId();
		recordReactions = cc.isRecordingReactions();
		this.cc = cc;

		currentState[0] = new State();
	}

	protected void downloadAndSync(RESTContestSource src, IContestObject obj) {
		if (src == null)
			return;

		if (obj instanceof Info) {
			Info i = (Info) obj;
			i.setLogo(downloadMissingFiles(src, obj, LOGO, i.getLogo()));
			i.setBanner(downloadMissingFiles(src, obj, BANNER, i.getBanner()));
		} else if (obj instanceof Problem) {
			Problem p = (Problem) obj;
			p.setPackage(downloadMissingFiles(src, obj, PACKAGE, p.getPackage()));
			p.setStatement(downloadMissingFiles(src, obj, STATEMENT, p.getStatement()));
			p.setAttachments(downloadMissingFiles(src, obj, ATTACHMENTS, p.getAttachments()));
		} else if (obj instanceof Group) {
			Group g = (Group) obj;
			g.setLogo(downloadMissingFiles(src, obj, LOGO, g.getLogo()));
		} else if (obj instanceof Team) {
			Team t = (Team) obj;
			t.setPhoto(downloadMissingFiles(src, obj, PHOTO, t.getPhoto()));
			t.setVideo(downloadMissingFiles(src, obj, VIDEO, t.getVideo()));
			t.setBackup(downloadMissingFiles(src, obj, BACKUP, t.getBackup()));
			t.setKeyLog(downloadMissingFiles(src, obj, KEY_LOG, t.getKeyLog()));
			t.setToolData(downloadMissingFiles(src, obj, TOOL_DATA, t.getToolData()));
		} else if (obj instanceof Person) {
			Person p = (Person) obj;
			p.setPhoto(downloadMissingFiles(src, obj, PHOTO, p.getPhoto()));
		} else if (obj instanceof Organization) {
			Organization o = (Organization) obj;
			o.setLogo(downloadMissingFiles(src, obj, LOGO, o.getLogo()));
			o.setCountryFlag(downloadMissingFiles(src, obj, COUNTRY_FLAG, o.getCountryFlag()));
			o.setCountrySubdivisionFlag(
					downloadMissingFiles(src, obj, COUNTRY_SUBDIVISION_FLAG, o.getCountrySubdivisionFlag()));
		} else if (obj instanceof Submission) {
			Submission s = (Submission) obj;
			s.setFiles(downloadMissingFiles(src, obj, FILES, s.getFiles()));
			s.setReaction(downloadMissingFiles(src, obj, REACTION, s.getReaction()));
		}
	}

	protected FileReferenceList downloadMissingFiles(RESTContestSource src, IContestObject obj, String property,
			FileReferenceList sourceFiles) {
		if (src == null || sourceFiles == null || sourceFiles.isEmpty())
			return sourceFiles;

		// make sure the cache is up to date and find local files
		src.updateCache(obj.getType(), obj.getId());
		FileReferenceList localFiles = src.getFilesWithPattern(obj, property);

		// if we don't have any files and there is at least one at the source, download everything
		if (localFiles == null || localFiles.size() == 0) {
			for (FileReference sourceFile : sourceFiles) {
				try {
					src.downloadFile(obj, sourceFile, property);
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error downloading file: " + obj.getType() + ": " + obj.getId(), e);
				}
			}
		} else {
			// more complex matching
			// for now, only download new images that have a different width & height or mime type
			// than local images
			for (FileReference sourceFile : sourceFiles) {
				if (sourceFile.height <= 0 || sourceFile.width <= 0)
					continue;

				try {
					if (!localFiles.containsFile(sourceFile)) {
						src.downloadFile(obj, sourceFile, property);
					}
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error downloading file: " + obj.getType() + ": " + obj.getId(), e);
				}
			}
		}

		// strip everything that's not remote - attachLocalResources will pick up everything local
		FileReferenceList newList = new FileReferenceList();
		for (FileReference ref : sourceFiles) {
			if (ref.href.startsWith("http"))
				newList.add(ref);
		}

		return newList;
	}

	private static FileReferenceList getMediaList(List<Integer> in) {
		if (in == null || in.isEmpty())
			return null;

		FileReferenceList list = new FileReferenceList();
		for (Integer i : in) {
			FileReference ref = new FileReference();
			VideoStream vs = VideoAggregator.getInstance().getStream(i);
			if (ConnectionMode.DIRECT.equals(vs.getMode())) {
				ref.href = vs.getURL();
			} else {
				String file = vs.getFileName();
				if (file != null) {
					ref.href = "<host>/stream/" + i + "/" + file;
				} else {
					ref.href = "<host>/stream/" + i;
				}
			}
			ref.mime = vs.getMimeType();
			list.add(ref);
		}
		return list;
	}

	@Override
	public void add(IContestObject obj) {
		if (cc.getView() != null) {
			View view = cc.getView();
			if (obj instanceof Group) {
				Group g = (Group) obj;
				if (!view.matchesGroup(g.getId()))
					return;
			}
			if (obj instanceof Problem) {
				String[] problems = cc.getView().getProblems();
				if (problems != null) {
					Problem p = (Problem) obj;
					if (!view.matchesProblem(p.getLabel()))
						return;
				}
			}
			if (obj instanceof Team) {
				Team team = (Team) obj;
				if (team.getGroupIds() == null)
					return;

				boolean found = false;
				for (String gId : team.getGroupIds()) {
					if (view.matchesGroup(gId)) {
						found = true;
						break;
					}
				}
				if (!found)
					return;
			}
			if (obj instanceof Submission) {
				Submission sub = (Submission) obj;
				if (getTeamById(sub.getTeamId()) == null)
					return;
				if (getProblemById(sub.getProblemId()) == null)
					return;
			}
			if (obj instanceof Judgement) {
				Judgement jud = (Judgement) obj;
				if (getSubmissionById(jud.getSubmissionId()) == null)
					return;
			}
			if (obj instanceof Run) {
				Run run = (Run) obj;
				if (getJudgementById(run.getJudgementTypeId()) == null)
					return;
			}
			if (obj instanceof Person) {
				Person person = (Person) obj;
				ITeam team = getTeamById(person.getTeamId());
				if (team == null)
					return;
			}
			if (obj instanceof Clarification) {
				Clarification clar = (Clarification) obj;
				if (clar.getFromTeamId() != null && getTeamById(clar.getFromTeamId()) == null)
					return;

				if (clar.getToTeamIds() != null) {
					for (String teamId : clar.getToTeamIds()) {
						if (getTeamById(teamId) == null)
							return;
					}
				}
				if (clar.getToGroupIds() != null) {
					for (String groupId : clar.getToGroupIds()) {
						if (getGroupById(groupId) == null)
							return;
					}
				}
				if (clar.getProblemId() != null && getProblemById(clar.getProblemId()) == null)
					return;
			}
			if (obj instanceof Award) {
				Award a = (Award) obj;
				if (a.getAwardType() == IAward.FIRST_TO_SOLVE) {
					boolean found = false;
					for (IProblem p : getProblems()) {
						if (a.getId().endsWith("-" + p.getId()))
							found = true;
					}
					if (!found)
						return;
				}

				String[] teamIds = a.getTeamIds();
				if (teamIds != null) {
					int count = 0;
					for (String teamId : teamIds) {
						ITeam team = getTeamById(teamId);
						if (team != null)
							count++;
					}
					if (count == 0)
						return;

					if (count != teamIds.length) {
						List<String> ids = new ArrayList<String>(count);
						for (String teamId : teamIds) {
							ITeam team = getTeamById(teamId);
							if (team != null)
								ids.add(teamId);
						}
						a.setTeamIds(ids.toArray(new String[count]));
					}
				}
			}
		}

		if (cc.getContestSource() instanceof RESTContestSource)
			downloadAndSync((RESTContestSource) cc.getContestSource(), obj);

		// set team streaming references
		if (obj instanceof Team) {
			Team team = (Team) obj;
			Map<StreamType, List<Integer>> streams = cc.getStreams(this, team);
			if (streams != null) {
				team.setDesktop(getMediaList(streams.get(StreamType.DESKTOP)));
				team.setWebcam(getMediaList(streams.get(StreamType.WEBCAM)));
				team.setAudio(getMediaList(streams.get(StreamType.AUDIO)));
			}
		}

		// record reactions if not testing
		if (recordReactions && !cc.isTesting()) {
			if (obj instanceof Submission) {
				Submission sub = (Submission) obj;

				ReactionVideoRecorder.getInstance().startRecording(cc, sub);
			}

			if (obj instanceof Judgement) {
				Judgement j = (Judgement) obj;
				if (j.getJudgementTypeId() != null)
					ReactionVideoRecorder.getInstance().stopRecording(cc, j);
			}
		}

		if (obj instanceof State) {
			State state = (State) obj;

			if (obj instanceof State) {
				State state2 = (State) obj;
				if (!Objects.equals(currentState[0].getStarted(), state2.getStarted()))
					Trace.trace(Trace.USER, "Contest started: " + contestId);
				if (!Objects.equals(currentState[0].getFrozen(), state2.getFrozen()))
					Trace.trace(Trace.USER, "Contest frozen: " + contestId);
				if (!Objects.equals(currentState[0].getThawed(), state2.getThawed()))
					Trace.trace(Trace.USER, "Contest thawed: " + contestId);
				if (!Objects.equals(currentState[0].getEnded(), state2.getEnded()))
					Trace.trace(Trace.USER, "Contest ended: " + contestId);
				if (!Objects.equals(currentState[0].getFinalized(), state2.getFinalized()))
					Trace.trace(Trace.USER, "Contest finalized: " + contestId);
				if (!Objects.equals(currentState[0].getEndOfUpdates(), state2.getEndOfUpdates()))
					Trace.trace(Trace.USER, "Contest end of updates: " + contestId);
				currentState[0] = state2;
			}
			if (state.isFrozen() && state.isRunning() && VideoAggregator.isRunning())
				VideoAggregator.getInstance().dropUntrustedListeners();
		}

		ContestType type = obj.getType();
		if (type == ContestType.CONTEST || type == ContestType.PROBLEM || type == ContestType.GROUP
				|| type == ContestType.LANGUAGE || type == ContestType.JUDGEMENT_TYPE || type == ContestType.TEAM
				|| type == ContestType.PERSON || type == ContestType.ORGANIZATION) {
			applyDefaults(obj);

			if (!isConfigurationLoaded()) {
				defaultConfig.add(obj);
			}
		}

		// if the CCS says the contest is stopped but doesn't support pause time, fix it
		if (obj instanceof Info) {
			Info info = (Info) obj;
			info.setId(cc.getId());

			Info currentInfo = getInfo();
			if (info.getStartTime() == null && info.getCountdownPauseTime() == null && !info.supportsCountdownPauseTime()
					&& currentInfo != null)
				info.setCountdownPauseTime(currentInfo.getCountdownPauseTime());
		}

		super.add(obj);
	}

	private void applyDefaults(IContestObject obj) {
		// don't apply defaults to objects being deleted
		if (obj instanceof IDelete)
			return;

		IContestObject.ContestType type = obj.getType();
		IContestObject def = null;
		if (IContestObject.isSingleton(type)) {
			IContestObject[] objs = defaultConfig.getObjects(type);
			if (objs != null && objs.length == 1)
				def = objs[0];
		} else
			def = defaultConfig.getObjectByTypeAndId(type, obj.getId());

		if (def != null) {
			Map<String, Object> props = def.getProperties();
			Map<String, Object> existingProps = obj.getProperties();
			for (String key : props.keySet()) {
				boolean found = false;
				for (String key2 : existingProps.keySet()) {
					if (key2.equals(key))
						found = true;
				}

				if (!found && (type != IContestObject.ContestType.CONTEST || !key.equals("start_time"))) {
					((ContestObject) obj).add(key, props.get(key));
				}
			}
		}
	}
}