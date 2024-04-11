package org.icpc.tools.presentation.contest.internal.scoreboard;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;
import org.icpc.tools.presentation.contest.internal.ScrollAnimator;

public class AbstractScrollingScoreboardPresentation extends AbstractScoreboardPresentation {
	private static final int MS_PER_PAGE = 12 * 1000; // scroll a page every 12 seconds
	private static final int FADE_IN_MS = 1000; // initial fade in, 1 second
	private static final int INITIAL_DELAY_MS = 3000; // extra time on first screen, 3 seconds
	private static final int FADE_OUT_MS = 1000; // fade out at end, 1 second

	private final Animator rowScroll = new Animator(0, new Movement(4, 6));
	private final ScrollAnimator pageScroll = new ScrollAnimator(INITIAL_DELAY_MS, MS_PER_PAGE, 1500);

	private Integer scrollToRow = null;
	private boolean scrollPause = false;
	private double scrollSpeed = 1.0;

	private int getNumPages() {
		IContest contest = getContest();
		if (contest == null)
			return 1;

		return (contest.getOrderedTeams().length + teamsPerScreen) / teamsPerScreen;
	}

	@Override
	public long getRepeat() {
		if (scrollToRow != null)
			return 0;

		return MS_PER_PAGE * getNumPages() + INITIAL_DELAY_MS + FADE_OUT_MS;
	}

	@Override
	public void keyEvent(KeyEvent e, int type) {
		if (type != KeyEvent.KEY_TYPED)
			return;

		char c = e.getKeyChar();
		if (Character.isDigit(c)) {
			int page = Integer.parseInt(c + "");
			if (page == 0)
				page = 9;
			else
				page--;
			setScrollToRow(page * teamsPerScreen);
		} else if ('r' == c)
			setScrollToRow(null);
	}

	public void setScrollSpeed(double d) {
		scrollSpeed = d;
	}

	@Override
	public void aboutToShow() {
		super.aboutToShow();

		rowScroll.resetToTarget();
	}

	@Override
	public void incrementTimeMs(long dt) {
		if (!scrollPause)
			rowScroll.incrementTimeMs((long) (dt * scrollSpeed));

		super.incrementTimeMs(dt);
	}

	protected int paintBarsAndScroll(IContest contest, Graphics2D g) {
		int toScroll = getScroll();

		g.translate(0, -toScroll);

		if (focusOnTeamId == null && scrollToRow == null) {
			long time = getRepeatTimeMs();
			if (time < FADE_IN_MS)
				g.setComposite(AlphaComposite.SrcOver.derive(time / (float) FADE_IN_MS));

			long endTime = MS_PER_PAGE * getNumPages() + INITIAL_DELAY_MS;
			if (time > endTime)
				g.setComposite(AlphaComposite.SrcOver.derive(1.0f - (time - endTime) / (float) FADE_OUT_MS));
		}

		// draw backgrounds - alternating light/dark bars as defined by the parent class
		// method drawBackground()
		int numTeams = contest.getOrderedTeams().length + 1;

		for (int i = 0; i < numTeams; i++) {
			float y = i * rowHeight;
			if ((y + rowHeight - toScroll) > 0 && (y - toScroll) < (height - headerHeight)) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.translate(0, (int) y);
				drawBackground(g2, i, i % 2 == 0);
				g2.dispose();
			}
		}
		return toScroll;
	}

	protected int getScroll() {
		if (focusOnTeamId != null)
			return (int) (focusOnTeamScroll.getValue() * rowHeight);

		if (scrollToRow != null)
			return (int) (rowScroll.getValue() * rowHeight);

		return (int) (pageScroll.getScroll(getRepeatTimeMs()) * teamsPerScreen * rowHeight);
	}

	public Integer getScrollToRow() {
		return scrollToRow;
	}

	public void setScrollToRow(Integer row) {
		if (scrollToRow == null && row != null)
			rowScroll.reset(getScroll() / rowHeight);
		scrollToRow = row;
		if (scrollToRow != null)
			rowScroll.setTarget(scrollToRow);
	}

	public void setScrollPause(boolean pause) {
		scrollPause = pause;
	}

	@Override
	public void setProperty(String value) {
		super.setProperty(value);

		if (value.startsWith("scroll:")) {
			try {
				String val = value.substring(7);
				if (val == null || val.isEmpty())
					setScrollToRow(null);
				setScrollToRow(Integer.parseInt(val));
			} catch (Exception e) {
				// ignore
			}
		}
	}
}