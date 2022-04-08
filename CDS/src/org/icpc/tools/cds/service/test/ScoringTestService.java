package org.icpc.tools.cds.service.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.CDSAuth;
import org.icpc.tools.cds.CDSConfig;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Scoreboard;
import org.icpc.tools.contest.model.TimeFilter;
import org.icpc.tools.contest.model.internal.Contest;

@WebServlet(urlPatterns = "/test/scoring")
public class ScoringTestService extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (!CDSAuth.isStaff(request))
			return;

		long time = System.currentTimeMillis();

		IContest contest = CDSConfig.getContests()[0].getContestByRole(request);
		ITeam team = contest.getTeams()[0];
		for (int i = 0; i < 100; i++) {
			IContest c = ((Contest) contest).clone(new TimeFilter(contest, i * 5 * 60 * 1000 / 100));
			c.getStanding(team).getNumSolved();
		}

		System.out.println("Time: " + (System.currentTimeMillis() - time));

		Scoreboard.writeScoreboard(response.getWriter(), contest);
	}

	// Before change times for 100: 31495, 30996, 32002
	// After change times for 100: 70
}