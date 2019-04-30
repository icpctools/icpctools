package org.icpc.tools.cds.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.ConfiguredContest;

public class VideoStatusServlet {
	// private static final Dimension SIZE = new Dimension(800, 600);

	// private static final Color[] STATUS_COLORS = new Color[] { Color.WHITE, new Color(230, 63,
	// 63),
	// new Color(95, 95, 230), new Color(63, 230, 63) };

	public static void doGet(ConfiguredContest cc, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setAttribute("cc", cc);
		request.getRequestDispatcher("/WEB-INF/jsps/video.jsp").forward(request, response);
	}

	/*private static void outputStatus(PrintWriter w) {
		VideoAggregator va = VideoAggregator.getInstance();
		w.println(va.getConcurrent());
		w.println(va.getMaxConcurrent());
		w.println(va.getTotal());
		w.println(ContestUtil.formatTime(va.getTotalTime()));
	}*/

	/*public static void writeStatusImage(ConfiguredContest cc, VideoMapper agg, OutputStream out) throws IOException {
		final Status[] status = agg.getStatus();

		FloorMap map = FloorMap.getInstance(); // TODO
		Rectangle r = new Rectangle(SIZE);
		BufferedImage image = new BufferedImage(SIZE.width, SIZE.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) image.getGraphics();
		final IContest contest = cc.getContest();
		map.drawFloor(g, r, new FloorMap.FloorColors() {
			@Override
			public Color getDeskFillColor(String teamId) {
				int ind = -1;
				ITeam[] teams = contest.getTeams();
				for (int i = 0; i < teams.length; i++) {
					if (teams[i].getId().equals(teamId))
						ind = i;
				}
				if (ind < 0)
					return STATUS_COLORS[0];

				return STATUS_COLORS[status[ind].ordinal()];
			}
		}, false);
		g.dispose();

		ImageIO.write(image, "png", out);
	}*/
}