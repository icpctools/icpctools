package org.icpc.tools.presentation.contest.internal.scoreboard;

import java.awt.FontMetrics;
import java.awt.Graphics2D;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ITeam;

/**
 * Single team presentation
 */
public class SingleTeamPresentation extends AbstractScoreboardPresentation {
	private String teamId;

	@Override
	public void setProperty(String value) {
		if (value == null || value.isEmpty())
			return;

		try {
			ITeam[] teams = getContest().getTeams();
			for (ITeam t : teams) {
				if (value.equals(t.getId()))
					teamId = t.getId();
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not parse team id: " + value);
		}
	}

	@Override
	public void paint(Graphics2D g2) {
		IContest contest = getContest();
		if (contest == null || contest.getNumTeams() == 0)
			return;

		ITeam team = contest.getTeamById(teamId);
		if (team == null)
			return;

		Graphics2D g = (Graphics2D) g2.create();
		drawBackground(g, 0, true);
		drawTeamAndProblemGrid(g, team);

		g.translate(0, -80);

		g.setFont(problemFont);
		String coach = null; // TODO TeamMemberHelper.getCoach(team.getICPCId());
		g.drawString("Coach: " + coach, 10, 0);

		String ss = "Contestants: ";
		FontMetrics fm = g.getFontMetrics();
		g.drawString(ss, 500 - fm.stringWidth(ss), 0);

		/*String[] members = TeamMemberHelper.getContestants(team.getICPCId()); // TODO
		if (members != null) {
			int i = 0;
			for (String s : members) {
				g.drawString(s, 500, i);
				i += 30;
			}
		}*/

		g.dispose();
	}
}