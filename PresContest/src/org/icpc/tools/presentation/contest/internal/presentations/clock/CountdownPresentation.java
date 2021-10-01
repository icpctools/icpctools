package org.icpc.tools.presentation.contest.internal.presentations.clock;

import java.awt.Color;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;
import org.icpc.tools.presentation.contest.internal.ICPCColors;

public class CountdownPresentation extends ClockPresentation {
	private static final Movement TIME_ANIM = new Movement(25000, 2000000);
	private static final Color EOC_COLOR = ICPCColors.RED.brighter().brighter().brighter();

	protected static long targetTime;
	protected static Animator clock = new Animator(0, TIME_ANIM);
	protected Color clockColor;

	public CountdownPresentation() {
		//
	}

	@Override
	public void setProperty(String value) {
		if (value == null || value.isEmpty())
			return;

		if ("reset".equals(value)) {
			targetTime = -1;
		} else {
			try {
				targetTime = 1000 * Integer.parseInt(value);
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error setting properties", e);
			}
		}
		setClockTarget();
		clock.resetToTarget();
	}

	@Override
	public void aboutToShow() {
		super.aboutToShow();
		setClockTarget();
		clock.resetToTarget();
	}

	private void setClockTarget() {
		IContest contest = getContest();
		if (contest != null) {
			Long startStatus = contest.getStartStatus();
			if (startStatus == null)
				return;

			if (startStatus < 0)
				clock.setTarget(-getRemainingMillis(), 0);
			else
				clock.setTarget(-getRemainingMillis(), 1000.0 * contest.getTimeMultiplier());
		}
	}

	@Override
	public void incrementTimeMs(long dt) {
		setClockTarget();
		clock.incrementTimeMs(dt);
		super.incrementTimeMs(dt);
	}

	@Override
	public Color getTextForegroundColor() {
		return clockColor;
	}

	private long getRemainingMillis() {
		long now = getTimeMs();
		if (targetTime > 0)
			return targetTime - now;

		IContest contest = getContest();
		if (contest == null)
			return 0;

		Long startTime = contest.getStartStatus();
		if (startTime == null)
			return 0;

		double timeMultiplier = contest.getTimeMultiplier();
		if (startTime < 0)
			return -Math.round(startTime * timeMultiplier);

		// where should we switch countdown between the start and end of a contest?
		// switch at the contest freeze, or (if there is no freeze) 2/3 of the contest
		int duration = contest.getDuration();
		long switchPoint = duration * 2 / 3;

		int freezeDuration = contest.getFreezeDuration();
		if (freezeDuration > 0)
			switchPoint = duration - freezeDuration;

		if ((startTime - now) < -(switchPoint / timeMultiplier)) {
			clockColor = EOC_COLOR;
			return -Math.round((startTime - now) * timeMultiplier + duration);
		}

		clockColor = isLightMode() ? Color.BLACK : Color.WHITE;
		return Math.round((startTime - now) * timeMultiplier);
	}

	@Override
	protected Long getClock() {
		return (long) clock.getValue();
	}
}