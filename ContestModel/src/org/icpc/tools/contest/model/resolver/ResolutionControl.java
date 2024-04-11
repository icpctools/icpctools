package org.icpc.tools.contest.model.resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.DelayStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.PauseStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ResolutionStep;

public class ResolutionControl {
	private List<ResolutionStep> steps;
	private int currentPause = -1;
	private int currentStep = -1;
	private double speedFactor = 1;
	private double scrollSpeedFactor = 1;
	private boolean stepping;

	private final List<IResolutionListener> listeners = new ArrayList<>();

	public interface IResolutionListener {
		void toPause(int pause, boolean includeDelays);

		void atStep(ResolutionStep step);

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

	private void notifyListenersAtStep(ResolutionStep step) {
		IResolutionListener[] list = null;
		synchronized (listeners) {
			list = listeners.toArray(new IResolutionListener[0]);
		}

		for (IResolutionListener listener : list) {
			try {
				listener.atStep(step);
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
			notifyListenersAtStep(step);
		}
		return false;
	}

	public boolean isStepping() {
		return stepping;
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
		if (currentStep == 0 || currentStep == 1000)
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
		if (toPause == currentPause)
			return;

		Trace.trace(Trace.INFO, "Resolving to: " + toPause + " / " + includeDelays);
		stepping = true;

		long[] startTime = new long[] { System.nanoTime() };
		if (!includeDelays)
			startTime = null;

		boolean forward = toPause > currentPause;

		// notify listener which pause we're going to
		notifyListenersToPause(toPause, includeDelays);

		while (currentPause != toPause) {
			if (forward)
				currentStep++;
			else
				currentStep--;

			if (processStep(startTime, forward)) {
				if (currentPause == toPause) {
					stepping = false;
					return;
				}
			}
		}

		stepping = false;
	}

	private ResolutionStep findPrevious(ResolutionStep step) {
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
		long delay = Math.round(ResolutionUtil.DELAY_TIMES[type] * speedFactor * 1000000000.0);
		startTime[0] += delay;
		LockSupport.parkNanos(startTime[0] - System.nanoTime());
	}
}