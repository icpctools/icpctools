package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.ICommentary;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.nls.Messages;

/**
 * Shows incoming commentary.
 */
public class CommentaryPresentation extends TitledPresentation {
	private static final long TIME_TO_KEEP_SOLVED = 11000;
	private static final long TIME_TO_KEEP_FAILED = 8000;
	private static final long TIME_TO_KEEP_RECENT = 14000;
	private static final long TIME_TO_FADE_RECENT = 2000;
	private static final long LINES_PER_SCREEN = 20;

	private static final Movement COMMENTARY_MOVEMENT = new Movement(5, 9);

	enum Action {
		MOVE_UP, MOVE_DOWN, MOVE_OUT
	}

	protected class RecentCommentary {
		public ICommentary commentary;
		protected BufferedImage img;
		protected Animator anim;
		protected long fullAge;
		protected long actionAge;

		protected Action action;

		@Override
		public String toString() {
			return "Commentary: " + commentary.getId();
		}
	}

	protected List<RecentCommentary> comments = new ArrayList<>();
	protected long timeToKeepFailed = TIME_TO_KEEP_FAILED;
	protected long timeToKeepSolved = TIME_TO_KEEP_SOLVED;
	protected Font titleFont;
	protected Font textFont;

	protected IContestListener listener = (contest, obj, d) -> {
		if (obj instanceof ICommentary)
			handleCommentary((ICommentary) obj);
	};

	@Override
	public void init() {
		super.init();

		getContest().addListener(listener);

		final float dpi = 96;
		float size = (int) (height * 72.0 * 0.028 / dpi);
		titleFont = ICPCFont.getMasterFont().deriveFont(Font.BOLD, size * 2.2f);
		textFont = ICPCFont.getMasterFont().deriveFont(Font.PLAIN, size * 1.25f);
	}

	@Override
	public void dispose() {
		super.dispose();

		getContest().removeListener(listener);
	}

	@Override
	public void aboutToShow() {
		super.aboutToShow();

		synchronized (comments) {
			comments.clear();

			IContest contest = getContest();
			ICommentary[] comm2 = contest.getCommentary();
			for (ICommentary commentary : comm2) {
				handleCommentary(commentary);
			}
		}
		updateTargets(true);
	}

	protected void handleCommentary(ICommentary commentary) {
		// check for existing record first
		synchronized (comments) {
			for (RecentCommentary comm : comments) {
				if (comm.commentary.getId().equals(commentary.getId())) {
					comm.commentary = commentary;
					return;
				}
			}
			createRecentCommentary(commentary);
		}
	}

	protected RecentCommentary createRecentCommentary(ICommentary commentary) {
		synchronized (comments) {
			RecentCommentary comm = new RecentCommentary();
			comm.commentary = commentary;
			double initalX = 0;
			if (comments.size() > 0)
				initalX = Math.min(comments.get(comments.size() - 1).anim.getValue() + 1, LINES_PER_SCREEN * 2);
			comm.anim = new Animator(Math.max(initalX, LINES_PER_SCREEN + 2), COMMENTARY_MOVEMENT);

			// create image
			int h = (int) (height / LINES_PER_SCREEN);
			comm.img = new BufferedImage(width, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = comm.img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			g.setColor(Color.WHITE);
			g.setFont(textFont);
			g.drawString(commentary.getMessage(), 0, h - 4);

			g.dispose();

			comments.add(comm);

			updateTargets(false);
			return comm;
		}
	}

	@Override
	public void incrementTimeMs(long dt) {
		updateRecords(dt);
		updateTargets(false);
		super.incrementTimeMs(dt);
	}

	@Override
	protected String getTitle() {
		return Messages.titleCommentary;
	}

	@Override
	protected void paintImpl(Graphics2D g) {
		RecentCommentary[] submissions2 = null;
		synchronized (comments) {
			submissions2 = comments.toArray(new RecentCommentary[0]);
		}

		g.setFont(textFont);
		for (RecentCommentary comm : submissions2) {
			double yy = comm.anim.getValue() * height / LINES_PER_SCREEN;
			if (yy < height - headerHeight) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.translate(0, (int) yy);
				if (comm.action == Action.MOVE_DOWN) {
					float tr = (float) Math.min(1.0,
							Math.max(0.1, 1.0 - (comm.actionAge - timeToKeepFailed * 0.75) * 2.0 / timeToKeepFailed));
					g2.setComposite(AlphaComposite.SrcOver.derive(tr));
				} else if (comm.action == Action.MOVE_OUT) {
					float tr = (float) (1.0 - comm.actionAge / (double) TIME_TO_FADE_RECENT);
					g2.setComposite(AlphaComposite.SrcOver.derive(tr));
				}
				paintCommentary(g2, comm);
				g2.dispose();
			}
		}
	}

	protected void paintCommentary(Graphics2D g, RecentCommentary comm) {
		g.drawImage(comm.img, 0, 0, null);
	}

	protected void updateTargets(boolean force) {
		if (comments == null)
			return;

		int count = 0;
		// IContest contest = getContest();
		synchronized (comments) {
			for (RecentCommentary comment : comments) {
				double target = 0;
				target = count;
				if (comment.action == Action.MOVE_UP && (comment.actionAge > timeToKeepSolved))
					target = -LINES_PER_SCREEN;
				else if (comment.action == Action.MOVE_DOWN && comment.actionAge > timeToKeepFailed)
					target = LINES_PER_SCREEN * 2;
				else if (comment.action == Action.MOVE_OUT && comment.actionAge > TIME_TO_FADE_RECENT / 3) {
					// don't move count
				} else if (count < LINES_PER_SCREEN * 2)
					count++;

				if (force)
					comment.anim.reset(target);
				else
					comment.anim.setTarget(target);
			}
		}
	}

	protected void updateRecords(long dt) {
		List<RecentCommentary> remove = new ArrayList<>();

		// IContest contest = getContest();
		synchronized (comments) {
			for (RecentCommentary comment : comments) {
				comment.anim.incrementTimeMs(dt);
				comment.fullAge += dt;

				if (comment.action == null) {
					if (comment.fullAge > TIME_TO_KEEP_RECENT)
						comment.action = Action.MOVE_OUT;

					/*if (contest.getState().isFrozen() && contest.getState().isRunning()) {
						if (comment.fullAge > TIME_TO_KEEP_RECENT)
							comment.action = Action.MOVE_OUT;
					} else {
						if (contest.isJudged(comment.commentary)) {
							if (contest.isSolved(comment.commentary))
								comment.action = Action.MOVE_UP;
							else
								comment.action = Action.MOVE_DOWN;
						}
					}*/
				} else
					comment.actionAge += dt;

				if (comment.action == Action.MOVE_UP && comment.anim.getValue() < -1)
					remove.add(comment);
				else if (comment.action == Action.MOVE_DOWN && comment.anim.getValue() > LINES_PER_SCREEN)
					remove.add(comment);
				else if (comment.action == Action.MOVE_OUT && comment.actionAge > TIME_TO_FADE_RECENT)
					remove.add(comment);
			}

			for (RecentCommentary s : remove)
				comments.remove(s);
		}
	}
}