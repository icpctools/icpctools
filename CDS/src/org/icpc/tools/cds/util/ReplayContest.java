package org.icpc.tools.cds.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObject.ContestType;
import org.icpc.tools.contest.model.feed.Timestamp;
import org.icpc.tools.contest.model.internal.Info;
import org.icpc.tools.contest.model.internal.Judgement;
import org.icpc.tools.contest.model.internal.State;
import org.icpc.tools.contest.model.internal.TimedEvent;

/**
 * A replay contest that withholds all time-based events until the contest starts, then replays
 * them according to the correct contest time.
 */
public class ReplayContest extends CDSContest {
	protected ConfiguredContest cc;
	protected double timeMultiplier;
	protected Long startTime;
	protected boolean stopReplay = false;

	protected Thread startThread;

	protected List<IContestObject> timedObject = new ArrayList<IContestObject>();

	public ReplayContest(ConfiguredContest cc) {
		super(cc);
		this.cc = cc;
		setHashCode(hashCode() + (int) (Math.random() * 500.0));

		if (cc.getTest().getStartTime() > 0) {
			startTime = cc.getTest().getStartTime();
		} else {
			startTime = System.currentTimeMillis() + cc.getTest().getCountdown() * 1000;
		}

		timeMultiplier = cc.getTest().getMultiplier();

		createReplayThread();
	}

	private void createReplayThread() {
		startThread = new Thread("Contest replay") {
			@Override
			public void run() {
				long timeUntilContest = startTime != null ? startTime - System.currentTimeMillis() : Long.MAX_VALUE;

				// keep waiting for 5s until we're within 1s of the start time
				while (timeUntilContest > 1000 && !stopReplay) {
					try {
						Thread.sleep(Math.min(timeUntilContest, 5000));
					} catch (Exception e) {
						// ignore
					}
					timeUntilContest = startTime != null ? startTime - System.currentTimeMillis() : Long.MAX_VALUE;
				}

				if (stopReplay) {
					return;
				}

				// release objects
				releaseEvents();
			}
		};
		startThread.setDaemon(true);
		startThread.start();
	}

	public void stopReplay() {
		stopReplay = true;

		startThread.interrupt();
	}

	@Override
	public Info setContestStart(Long startTime, Long countdownTime) {
		this.startTime = startTime;
		try {
			startThread.interrupt();
		} catch (Exception e) {
			// ignore
		}
		return super.setContestStart(startTime, countdownTime);
	}

	/**
	 * Delay if we haven't reached the given contest time in ms based on the contest start time and
	 * the time multiplier.
	 *
	 * @param info
	 * @param time
	 */
	protected void waitForContestTime(long time) {
		if (startTime == null || startTime < 0)
			return;

		double contestTime = System.currentTimeMillis() - startTime;
		long dt = (long) (time / timeMultiplier - contestTime);

		// no point waiting for less that 5ms
		if (dt < 5)
			return;

		// let people know if we'll be waiting for a while
		if (dt > 5000)
			Trace.trace(Trace.USER, "Sleeping for " + (int) (dt / 100) / 10f + " seconds.");

		try {
			LockSupport.parkNanos(dt * 1_000_000);
		} catch (Exception e) {
			// ignore
		}
	}

	private static boolean isTimedEvent(IContestObject obj) {
		ContestType type = obj.getType();
		return (type == ContestType.STATE || type == ContestType.SUBMISSION || type == ContestType.JUDGEMENT
				|| type == ContestType.CLARIFICATION || type == ContestType.COMMENTARY || type == ContestType.RUN);
	}

	private void releaseEvents() {
		ensureIntermediateEvents();

		while (!timedObject.isEmpty() && !stopReplay) {
			IContestObject nextObj = null;
			long nextTime = Long.MAX_VALUE;
			for (IContestObject obj : timedObject) {
				long time = getReleaseTime(obj);
				if (time < nextTime) {
					nextObj = obj;
					nextTime = time;
				}
			}

			waitForContestTime(nextTime);

			timedObject.remove(nextObj);
			fixWallClockTime(nextObj);
			super.add(nextObj);
		}
	}

	private long getReleaseTime(IContestObject obj) {
		if (obj instanceof State) {
			State state = (State) obj;
			if (state.getEnded() != null) {
				return getDuration();
			}
			if (state.getFrozen() != null) {
				return getDuration() - getFreezeDuration();
			}
			if (state.getStarted() != null)
				waitForContestTime(0);
		}
		if (obj instanceof TimedEvent) {
			TimedEvent te = (TimedEvent) obj;
			return te.getContestTime();
		}
		if (obj instanceof Judgement) {
			Judgement j = (Judgement) obj;
			if (j.getEndContestTime() != null)
				return j.getEndContestTime();

			return j.getStartContestTime();
		}

		return 0;
	}

	@Override
	public void add(IContestObject obj) {
		if (isTimedEvent(obj)) {
			timedObject.add(obj);
			return;
		}

		if (obj instanceof Info) {
			Info info = (Info) obj;
			info.setStartTime(startTime);
			info.setTimeMultiplier(timeMultiplier);
		}

		super.add(obj);
	}

	private void fixWallClockTime(IContestObject obj) {
		if (obj instanceof State) {
			State state = (State) obj;

			long duration = getDuration();
			if (state.getStarted() != null)
				state.setStarted(startTime);
			if (state.getEnded() != null)
				state.setEnded(startTime + duration);
			if (state.getFrozen() != null)
				state.setFrozen(startTime + duration - getFreezeDuration());
			if (state.getThawed() != null)
				state.setThawed(startTime + duration);
			if (state.getFinalized() != null)
				state.setFinalized(startTime + duration);
			if (state.getEndOfUpdates() != null)
				state.setEndOfUpdates(startTime + duration);
		}

		if (obj instanceof TimedEvent) {
			TimedEvent te = (TimedEvent) obj;
			te.add("time", Timestamp.now());
		}

		if (obj instanceof Judgement) {
			Judgement j = (Judgement) obj;
			if (j.getEndContestTime() != null) {
				j.add("end_time", Timestamp.now());
			} else {
				j.add("start_time", Timestamp.now());
			}
		}
	}

	/**
	 * Make sure the contest contains 'intermediate' events, including start of each judgement and
	 * contest start state update.
	 */
	private void ensureIntermediateEvents() {
		State state = null;
		boolean hasStart = false;
		List<IContestObject> toAdd = new ArrayList<IContestObject>();
		for (IContestObject obj : timedObject) {
			// for every judgement, check if there is a corresponding 'start' event
			if (obj instanceof Judgement) {
				Judgement j = (Judgement) obj;
				if (j.getJudgementTypeId() != null) {
					// final judgement. check if there is a starting one, and if not create one
					boolean found = false;
					for (IContestObject obj2 : timedObject) {
						if (obj2 instanceof Judgement) {
							Judgement j2 = (Judgement) obj2;
							if (j2.getJudgementTypeId() == null && j2.getId().equals(j.getId())) {
								found = true;
								break;
							}
						}
					}
					if (!found) {
						// clone and clean final judgement to create an equivalent initial event
						Judgement newJ = (Judgement) j.clone();
						newJ.clearJudgement();
						toAdd.add(newJ);
					}
				}
			}
			if (obj instanceof State) {
				state = (State) obj;

				// check if there's a state with only contest started
				Map<String, Object> props = state.getProperties();
				if (props.size() == 1 && props.containsKey("started")) {
					hasStart = true;
				}
			}
		}

		for (IContestObject obj : toAdd) {
			timedObject.add(obj);
		}

		// create contest starting state
		if (!hasStart) {
			State start = new State();
			start.setStarted(startTime);
			timedObject.add(start);
		}
		if (state == null) {
			// no state at all, create a freeze and ended state too
			if (getFreezeDuration() != null) {
				State freeze = new State();
				freeze.setStarted(startTime);
				freeze.setFrozen(startTime + getDuration() - getFreezeDuration());
				freeze.setEnded(startTime + getDuration());
				timedObject.add(freeze);
			}

			State ended = new State();
			ended.setStarted(startTime);
			if (getFreezeDuration() != null) {
				ended.setFrozen(startTime + getDuration() - getFreezeDuration());
			}
			ended.setEnded(startTime + getDuration());
			timedObject.add(ended);
		}
	}
}