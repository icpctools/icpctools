package org.icpc.tools.cds.service;

import org.icpc.tools.contest.model.IContestListener.Delta;
import org.icpc.tools.contest.model.IContestObject;

public class ContestObjectQueue {
	private static final int ARRAY_SIZE = 1000;
	private static final int NUM_ARRAYS = 2000;

	public static class ContestObjectDelta {
		IContestObject obj;
		Delta d;

		public ContestObjectDelta(IContestObject co, Delta d) {
			this.obj = co;
			this.d = d;
		}
	}

	private ContestObjectDelta[][] objs = new ContestObjectDelta[NUM_ARRAYS][];
	private int start;
	private int end;

	public ContestObjectQueue() {
		// do nothing
	}

	public synchronized void add(IContestObject obj, Delta d) {
		int arr = end / ARRAY_SIZE;
		int ind = end % ARRAY_SIZE;
		if (objs[arr] == null)
			objs[arr] = new ContestObjectDelta[ARRAY_SIZE];

		objs[arr][ind] = new ContestObjectDelta(obj, d);
		end++;
	}

	public synchronized boolean isEmpty() {
		return end == start;
	}

	public synchronized ContestObjectDelta poll() {
		if (start == end)
			return null;

		int arr = start / ARRAY_SIZE;
		int ind = start % ARRAY_SIZE;
		start++;

		if (start % ARRAY_SIZE == 0) {
			// clean up array elements that aren't needed anymore
			ContestObjectDelta cod = objs[arr][ind];
			objs[arr] = null;
			return cod;
		}

		return objs[arr][ind];
	}
}