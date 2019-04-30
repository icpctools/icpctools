package org.icpc.tools.contest.model.internal;

import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContestObject;

public class BufferedContest extends Contest {
	private List<IContestObject> buffer = new ArrayList<>();

	private Thread t;
	private boolean more;

	@Override
	public void add(IContestObject obj) {
		synchronized (buffer) {
			buffer.add(obj);
		}

		if (t != null) {
			more = true;
			return;
		}

		t = new Thread("Buffered contest") {
			@Override
			public void run() {
				int loop = 0;
				// wait for 200ms. loop again if there are more objects still coming in, but no more
				// than 2000 objects or 800ms
				while (loop == 0 || (more && loop < 4 && buffer.size() < 2000)) {
					more = false;
					loop++;
					try {
						Thread.sleep(200);
					} catch (Exception e) {
						// ignore
					}
				}
				if (loop > 1)
					Trace.trace(Trace.INFO, "Contest buffering: " + loop + "." + buffer.size());
				flush();
			}
		};
		t.setDaemon(true);
		t.start();
	}

	public boolean isBuffering() {
		return buffer.size() > 0 || t != null;
	}

	private void flush() {
		t = null;
		synchronized (buffer) {
			for (IContestObject co : buffer) {
				super.add(co);
			}
			buffer.clear();
		}
	}
}