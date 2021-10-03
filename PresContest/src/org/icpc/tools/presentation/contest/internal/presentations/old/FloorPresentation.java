package org.icpc.tools.presentation.contest.internal.presentations.old;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.ICPCColors;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;

public class FloorPresentation extends AbstractICPCPresentation {
	protected FloorMap floor;
	private Font font;
	private Font tableFont;

	public FloorPresentation() {
		super();
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		final float dpi = 96;
		float size = height * 36f / 6f / dpi;
		font = ICPCFont.getMasterFont().deriveFont(Font.BOLD, size);
		size = height * 36f / 20f / dpi;
		tableFont = ICPCFont.getMasterFont().deriveFont(Font.BOLD, size);
	}

	@Override
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g.setColor(Color.WHITE);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		String s = "Team Activity";
		g.drawString(s, (width - fm.stringWidth(s)) / 2, fm.getAscent() + 10);

		final IContest contest = getContest();
		if (contest == null)
			return;

		if (floor == null)
			floor = new FloorMap(contest);

		int h = fm.getHeight() + 20;
		g.setFont(tableFont);

		floor.drawFloor(g, new Rectangle(0, h, width, height - h), new FloorMap.ScreenColors() {
			@Override
			public Color getDeskFillColor(String teamId) {
				// find last run by this team
				ISubmission[] runs = contest.getSubmissions();
				int size2 = runs.length;
				ISubmission lastRun = null;
				for (int i = size2 - 1; i >= 0; i--) {
					ITeam team = contest.getTeamById(runs[i].getTeamId());
					if (teamId.equals(team.getId())) {
						lastRun = runs[i];
						break;
					}
				}

				if (lastRun != null) {
					if (ContestUtil.isRecent(contest, lastRun)) {
						Status status = contest.getStatus(lastRun);
						return ICPCColors.getStatusColor(status, getTimeMs() * 16 + teamId.hashCode() * 9);
					}
				}
				return Color.BLACK;
			}
		});
	}
}