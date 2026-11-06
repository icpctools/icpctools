package org.icpc.tools.contest.model.resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.DelayStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.JudgementStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.PauseStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ResolutionStep;

public class ResolutionControl {
	private List<ResolutionStep> steps;
	private int currentPause = -1;
	private int currentStep = -1;
	private double speedFactor = 1;
	private double scrollSpeedFactor = 1;
	private boolean stepping;

	// pausing and cancelling a running step sequence (e.g. auto-resolving); these are set from other
	// threads while the stepping thread holds the lock, so they are not synchronized
	private volatile boolean paused;
	private volatile boolean cancelled;
	private volatile Thread steppingThread;

	private final List<IResolutionListener> listeners = new ArrayList<>();

	public interface IResolutionListener {
		void toPause(int pause, boolean includeDelays);

		void step(ResolutionStep step, boolean forward);

		void atPause(int pause);
	}

	public ResolutionControl(List<ResolutionStep> steps) {
		this.steps = steps;
	}

	public void addListener(IResolutionListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	public void removeListener(IResolutionListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	public List<ResolutionStep> getSteps() {
		return steps;
	}

	public int getCurrentPause() {
		return currentPause;
	}

	public int getCurrentStep() {
		return currentStep;
	}

	public double getSpeedFactor() {
		return speedFactor;
	}

	public void setSpeedFactor(double d) {
		speedFactor = d;
	}

	public double getScrollSpeedFactor() {
		return scrollSpeedFactor;
	}

	public void setScrollSpeedFactor(double d) {
		scrollSpeedFactor = d;
	}

	private void notifyListenersAtStep(ResolutionStep step, boolean forward) {
		IResolutionListener[] list = null;
		synchronized (listeners) {
			list = listeners.toArray(new IResolutionListener[0]);
		}

		for (IResolutionListener listener : list) {
			try {
				listener.step(step, forward);
			} catch (Throwable t) {
				Trace.trace(Trace.ERROR, "Error notifying listener", t);
			}
		}
	}

	private void notifyListenersToPause(int pause, boolean includeDelays) {
		IResolutionListener[] list = null;
		synchronized (listeners) {
			list = listeners.toArray(new IResolutionListener[0]);
		}

		for (IResolutionListener listener : list) {
			try {
				listener.toPause(pause, includeDelays);
			} catch (Throwable t) {
				Trace.trace(Trace.ERROR, "Error notifying listener", t);
			}
		}
	}

	private boolean processStep(long[] startTime, boolean forward) {
		ResolutionStep step = steps.get(currentStep);
		if (step instanceof PauseStep) {
			PauseStep pause = (PauseStep) step;
			currentPause = pause.num;
			return true;
		} else if (step instanceof DelayStep) {
			DelayStep delay = (DelayStep) step;
			if (startTime != null)
				wait(startTime, delay.type.ordinal());
		} else {
			if (!forward)
				step = findPrevious(step);
			Trace.trace(Trace.INFO, "Step " + currentStep + " <  " + step);
			notifyListenersAtStep(step, forward);
		}
		return false;
	}

	public boolean isStepping() {
		return stepping;
	}

	/**
	 * Pause or resume a running step sequence. While paused, the stepping thread holds at the
	 * current step, and the remaining time of a delay is kept for when it resumes.
	 */
	public void setPaused(boolean pause) {
		paused = pause;
		Thread t = steppingThread;
		if (t != null)
			LockSupport.unpark(t);
	}

	public boolean isPaused() {
		return paused;
	}

	/**
	 * Returns true if a cancelled step sequence stopped between two pauses.
	 */
	public boolean isBetweenPauses() {
		return !atPauseStep();
	}

	/**
	 * Stop a running step sequence at the current step, which may be between two pauses. Returns
	 * immediately; the stepping method returns soon after. The next move goes from where it stopped.
	 */
	public void cancel() {
		Thread t = steppingThread;
		if (t == null)
			return;
		cancelled = true;
		paused = false;
		LockSupport.unpark(t);
	}

	/**
	 * Step forward or backward to the given pause, optionally including delays. This method is
	 * blocking and will not return until it is complete, run on non-critical thread if you are
	 * using delays!
	 *
	 * @param includeDelays
	 */
	public synchronized void moveToPause(int pause, boolean includeDelays) {
		stepToPause(pause, includeDelays);
	}

	/**
	 * Step forward to the next pause, optionally including delays. This method is blocking and will
	 * not return until it is complete, run on non-critical thread if you are using delays!
	 *
	 * @param includeDelays
	 */
	public synchronized void forward(boolean includeDelays) {
		if (currentStep == steps.size() - 1)
			return;

		stepToPause(currentPause + 1, includeDelays);
	}

	/**
	 * Rewind to the previous pause, optionally including delays. This method is blocking and will
	 * not return until it is complete, run on non-critical thread if you are using delays!
	 *
	 * @param includeDelays
	 */
	public synchronized void rewind(boolean includeDelays) {
		if (currentPause <= 0 || currentStep == 1000)
			return;

		stepToPause(currentPause - 1, includeDelays);
	}

	/**
	 * Reset resolution to the beginning.
	 */
	public synchronized void reset() {
		if (currentStep == 0)
			return;

		stepToPause(0, false);
	}

	private synchronized void stepToPause(int toPause, boolean includeDelays) {
		boolean betweenPauses = !atPauseStep();
		if (toPause == currentPause && !betweenPauses)
			return;

		Trace.trace(Trace.INFO, "Resolving to: " + toPause + " / " + includeDelays);
		stepping = true;
		cancelled = false;
		steppingThread = Thread.currentThread();
		try {
			long[] startTime = new long[] { System.nanoTime() };
			if (!includeDelays)
				startTime = null;

			// notify listener which pause we're going to
			notifyListenersToPause(toPause, includeDelays);

			// a cancelled sequence may have stopped between two pauses; going back starts by undoing
			// the steps since the last pause
			if (betweenPauses && toPause <= currentPause) {
				while (!atPauseStep()) {
					processStep(null, false);
					currentStep--;
				}
				if (toPause == currentPause)
					return;
			}

			boolean forward = toPause > currentPause;
			while (currentPause != toPause) {
				holdWhilePaused(startTime);
				if (cancelled)
					return;

				if (forward)
					currentStep++;
				else
					currentStep--;

				if (processStep(startTime, forward)) {
					if (currentPause == toPause)
						return;
				}
				if (cancelled)
					return;
			}
		} finally {
			stepping = false;
			cancelled = false;
			steppingThread = null;
		}
	}

	private boolean atPauseStep() {
		return currentStep < 0 || currentStep >= steps.size() || steps.get(currentStep) instanceof PauseStep;
	}

	/**
	 * Hold while paused. The time spent paused is added to the delay timeline, so the steps after
	 * resuming are not rushed to catch up.
	 */
	private void holdWhilePaused(long[] startTime) {
		if (!paused)
			return;
		long t0 = System.nanoTime();
		while (paused && !cancelled)
			LockSupport.parkNanos(50_000_000L);
		if (startTime != null)
			startTime[0] += System.nanoTime() - t0;
	}

	private ResolutionStep findPrevious(ResolutionStep step) {
		if (step instanceof JudgementStep)
			return step;

		Class<? extends ResolutionStep> cl = step.getClass();
		int num = currentStep;
		while (num > 0) {
			num--;
			ResolutionStep step2 = steps.get(num);
			if (cl.isInstance(step2))
				return step2;
		}
		return step;
	}

	/**
	 * Wait the specified time in seconds. The specified time is adjusted by the current
	 * "speedfactor" before waiting (for example, if a wait of 1.5 seconds is requested and the
	 * current speedfactor is 0.5, the actual wait time will be 1.5*0.5 = 0.75 seconds.
	 *
	 * @param startTime - an array of 1 containing the system nano time to start the delay from (the
	 *           current nano time may be later)
	 * @param type - the type of pause to take
	 */
	private void wait(long[] startTime, int type) {
		long delay = Math.round(ResolutionUtil.DELAY_TIMES[type] * speedFactor * 1_000_000_000.0);
		startTime[0] += delay;
		long remaining;
		while (!cancelled && (remaining = startTime[0] - System.nanoTime()) > 0) {
			if (paused)
				holdWhilePaused(startTime);
			else
				LockSupport.parkNanos(remaining);
		}
	}
}