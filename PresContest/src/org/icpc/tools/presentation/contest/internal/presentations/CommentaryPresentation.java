package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.AlphaComposite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.ICommentary;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.TextHelper;
import org.icpc.tools.presentation.contest.internal.nls.Messages;

/**
 * Shows incoming commentary.
 */
public class CommentaryPresentation extends TitledPresentation {
	private static final long TIME_TO_KEEP_RECENT = 20000;
	private static final long TIME_TO_FADE_RECENT = 2500;
	private static final long LINES_PER_SCREEN = 16;

	private static final Movement COMMENTARY_MOVEMENT = new Movement(5, 9);

	enum Action {
		MOVE_OUT
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
		titleFont = ICPCFont.deriveFont(Font.BOLD, size * 2.2f);

		float tempRowHeight = height / (float) LINES_PER_SCREEN;
		size = tempRowHeight * 36f * 0.95f / dpi;
		textFont = ICPCFont.deriveFont(Font.PLAIN, size * 1.7f);
		textFont2 = ICPCFont.deriveFont(Font.ITALIC, size * 1.7f);
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

			comments.add(comm);

			updateTargets(false);
			return comm;
		}
	}

	private void createImage(Graphics2D gg, RecentCommentary comm) {
		TextHelper text = new TextHelper(gg, false);

		List<Object> list = parseCommentary(comm.commentary.getMessage());
		for (Object o : list) {
			if (o instanceof String)
				text.addPlainText((String) o);
			else if (o instanceof ITeam)
				text.addTeam(getContest(), (ITeam) o);
			else if (o instanceof IProblem)
				text.addProblem((IProblem) o);
		}
		TextHelper.Layout layout = new TextHelper.Layout();
		layout.wrapWidth = width;
		layout.indent = width / 20;
		text.layout(layout);

		comm.img = new BufferedImage(width, text.getHeight() + 12, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = comm.img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		g.setFont(gg.getFont());
		text.setGraphics(g);
		text.draw(0, 6);

		g.dispose();
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
	protected void paintImplTitled(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

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
				if (comm.action == Action.MOVE_OUT) {
					float tr = (float) (1.0 - comm.actionAge / (double) TIME_TO_FADE_RECENT);
					g2.setComposite(AlphaComposite.SrcOver.derive(tr));
				}

				if (comm.img == null)
					createImage(g2, comm);
				g2.drawImage(comm.img, 0, -6, null);

				g2.dispose();
			}
		}
	}

	private static int firstTag(String s) {
		int t = s.indexOf("#t");
		int p = s.indexOf("#p");
		if (t >= 0 && (p == -1 || t < p))
			return t;
		if (p >= 0 && (t == -1 || p < t))
			return p;
		return -1;
	}

	protected List<Object> parseCommentary(String ss) {
		List<Object> list = new ArrayList<>();
		IContest contest = getContest();

		String s = ss;
		int i = firstTag(s);
		while (i >= 0) {
			if (i > 0)
				list.add(s.substring(0, i));

			int j = i + 2;
			char c = s.charAt(j);
			while (j < s.length() && (Character.isLetterOrDigit(c) || c == '_' || c == '.')) {
				j++;
				if (j < s.length())
					c = s.charAt(j);
			}

			String id = s.substring(i + 2, j);

			// if the last char was a period, it isn't part of the id
			if (id.endsWith(".")) {
				id = s.substring(i + 2, j - 1);
				j -= 1;
			}

			Object o = null;
			if (s.charAt(i + 1) == 't')
				o = contest.getTeamById(id);
			else
				o = contest.getProblemById(id);
			if (o != null)
				list.add(o);
			s = s.substring(j);

			// if the next char is an open bracket, remove the text until close bracket
			if (s.length() > 0 && s.charAt(0) == '(') {
				j = 1;
				while (j < s.length() && s.charAt(j) != ')') {
					j++;
				}
				if (j < s.length())
					j++;
				s = s.substring(j);
			}

			i = firstTag(s);
		}
		if (!s.isEmpty())
			list.add(s);
		return list;
	}

	protected void updateTargets(boolean force) {
		if (comments == null)
			return;

		int GAP = height / 35;
		double yy = GAP / 2;
		synchronized (comments) {
			for (RecentCommentary comment : comments) {
				double target = yy * LINES_PER_SCREEN / height;
				if (comment.action == Action.MOVE_OUT && comment.actionAge > TIME_TO_FADE_RECENT / 3) {
					// don't move count
				} else if (target < height * 2) {
					if (comment.img != null)
						yy += comment.img.getHeight() + GAP;
					else
						yy += height * 1.3 / LINES_PER_SCREEN;
				}

				if (force)
					comment.anim.reset(target);
				else
					comment.anim.setTarget(target);
			}
		}
	}

	protected void updateRecords(long dt) {
		List<RecentCommentary> remove = new ArrayList<>();

		synchronized (comments) {
			for (RecentCommentary comment : comments) {
				comment.anim.incrementTimeMs(dt);
				comment.fullAge += dt;

				if (comment.action == null) {
					if (comment.fullAge > TIME_TO_KEEP_RECENT)
						comment.action = Action.MOVE_OUT;
				} else
					comment.actionAge += dt;

				if (comment.action == Action.MOVE_OUT && comment.actionAge > TIME_TO_FADE_RECENT)
					remove.add(comment);
			}

			for (RecentCommentary s : remove)
				comments.remove(s);
		}
	}
}