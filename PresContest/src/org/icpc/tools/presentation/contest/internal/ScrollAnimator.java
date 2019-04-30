package org.icpc.tools.presentation.contest.internal;

public class ScrollAnimator {
	private long initialDelay;
	private long timePerPage;
	private long scrollTime;

	public ScrollAnimator(long initialDelay, long timePerPage, long scrollTime) {
		this.initialDelay = initialDelay;
		this.timePerPage = timePerPage;
		this.scrollTime = scrollTime;
	}

	private static double smoothImpl(double x) {
		return f(x) / (f(x) + f(1 - x));
	}

	private static double f(double x) {
		if (x < 0)
			return -Math.exp(1.0 / x);
		if (x == 0)
			return 0;
		return Math.exp(-1.0 / x);
	}

	public double getScroll(long time) {
		if (time < initialDelay + scrollTime / 2)
			return 0;

		long time2 = time - initialDelay;
		int page = (int) (time2 / timePerPage);
		time2 -= page * timePerPage;

		if (time2 < scrollTime / 2)
			return page - smoothImpl(0.5 - time2 / (scrollTime / 1.0));

		if (time2 > timePerPage - scrollTime / 2)
			return page + smoothImpl(0.5 - (timePerPage - time2) / (scrollTime / 1.0));

		return page;
	}
}