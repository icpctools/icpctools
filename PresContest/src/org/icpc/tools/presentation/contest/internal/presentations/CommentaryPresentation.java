package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.ICPCColors;
import org.icpc.tools.contest.model.ICommentary;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.ShadedRectangle;
import org.icpc.tools.presentation.contest.internal.nls.Messages;

/**
 * Shows incoming commentary.
 */
public class CommentaryPresentation extends TitledPresentation {
	private static final long TIME_TO_KEEP_SOLVED = 11000;
	private static final long TIME_TO_KEEP_FAILED = 8000;
	private static final long TIME_TO_KEEP_RECENT = 14000;
	private static final long TIME_TO_FADE_RECENT = 2000;
	private static final long LINES_PER_SCREEN = 15;

	private static final int GAP = 5;

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
	protected Font textFont2;

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

		float tempRowHeight = height / (float) LINES_PER_SCREEN;
		size = tempRowHeight * 36f * 0.95f / dpi;
		textFont = ICPCFont.getMasterFont().deriveFont(Font.PLAIN, size * 1.5f);
		textFont2 = ICPCFont.getMasterFont().deriveFont(Font.ITALIC, size * 1.5f);
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

			g.setFont(textFont);
			FontMetrics fm = g.getFontMetrics();
			g.setFont(textFont2);
			FontMetrics fm2 = g.getFontMetrics();
			int fh = fm.getAscent();
			int baseLine = (h + fh) / 2;
			int gh = h - GAP * 3;
			List<Object> list = parseCommentary(commentary.getMessage());
			int x = 0;
			for (Object o : list) {
				if (o instanceof String) {
					String s = (String) o;
					g.setColor(Color.LIGHT_GRAY);
					g.setFont(textFont);
					g.drawString(s, x, baseLine);
					x += fm.getStringBounds(s, g).getWidth();
				} else if (o instanceof ITeam) {
					ITeam team = (ITeam) o;

					IOrganization org = getContest().getOrganizationById(team.getOrganizationId());
					if (org != null) {
						BufferedImage img = org.getLogoImage(gh, gh, true, true);
						if (img != null) {
							g.drawImage(img, x + GAP, (h - img.getWidth()) / 2, null);
							x += img.getWidth() + GAP * 2;
						} /* else { // for testing logo spacing
							g.setColor(Color.GREEN);
							g.drawRect(x + GAP, (h - gh) / 2, gh, gh);
							x += gh + GAP * 2;
							}*/
					}

					String s = team.getActualDisplayName();
					g.setColor(Color.WHITE);
					g.setFont(textFont2);
					g.drawString(s, x, baseLine);
					x += fm2.getStringBounds(s, g).getWidth();
				} else if (o instanceof IProblem) {
					IProblem p = (IProblem) o;

					Color c = p.getColorVal();
					Color cc = ICPCColors.getContrastColor(c);
					ShadedRectangle.drawRoundRect(g, x + GAP, (h - gh) / 2, fh * 3, gh, c, cc, "");

					g.setColor(cc);
					g.setFont(textFont);
					g.drawString(p.getLabel(), x + GAP + fh * 3 / 2 - fm.stringWidth(p.getLabel()) / 2, baseLine);
					x += fh * 3 + GAP * 2;

					String s = p.getName();
					g.setColor(Color.WHITE);
					g.setFont(textFont2);
					g.drawString(s, x, baseLine);
					x += fm2.getStringBounds(s, g).getWidth();
				}
			}

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

	protected static int findEndOfToken(String s, int ii) {
		int i = ii + 1;
		while (true) {
			if (i >= s.length())
				return s.length();
			if (Character.isWhitespace(s.charAt(i)))
				return i;
			if (s.charAt(i) == '(')
				i++;
		}
	}

	protected List<Object> parseCommentary(String ss) {
		List<Object> list = new ArrayList<>();
		IContest contest = getContest();

		String s = ss;
		while (true) {
			int i = s.indexOf("#");
			if (i < 0) {
				list.add(s);
				return list;
			}
			if (s.length() > i + 1) {
				if (s.charAt(i + 1) == 't' || s.charAt(i + 1) == 'p') {
					if (i > 0)
						list.add(s.substring(0, i));

					// find the id and the end of the token (usually the same thing, but there could be
					// (brackets)
					String id = null;

					int j = i + 2;
					int e = 0;
					while (id == null) {
						if (j >= s.length()) {
							id = s.substring(i + 2, j);
							e = j;
						} else if (Character.isWhitespace(s.charAt(j))) {
							id = s.substring(i + 2, j);
							e = j;
						} else if (s.charAt(j) == '(') {
							id = s.substring(i + 2, j);
							while (j < s.length() && s.charAt(j) != ')') {
								j++;
							}
							e = j + 1;
						}
						j++;
					}
					if (id.endsWith(".")) {
						id = id.substring(0, id.length() - 1);
						e -= 1;
					}

					Object o = null;
					if (s.charAt(i + 1) == 't')
						o = contest.getTeamById(id);
					else
						o = contest.getProblemById(id);
					if (o != null)
						list.add(o);
					// if (e < s.length() && s.charAt(e + 1) == ' ')
					// e++;
					s = s.substring(e);
				} else {
					i = s.indexOf("#", i + 1);
				}
			}
		}

		// return list;
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