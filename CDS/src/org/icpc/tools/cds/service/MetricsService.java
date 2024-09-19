package org.icpc.tools.cds.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.CDSConfig;
import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.contest.model.IContest;

/**
 * An /api/metrics service for Prometheus.
 */
public class MetricsService {
	protected enum MetricType {
		GAUGE
	}

	// helper class to store contest metrics
	static class Metrics {
		Map<String, Integer> jtCount = new HashMap<String, Integer>();
		int queued;
	}

	public static void write(HttpServletRequest request, HttpServletResponse response) throws IOException {
		PrintWriter pw = response.getWriter();

		ConfiguredContest[] ccs = CDSConfig.getContests();
		for (ConfiguredContest cc : ccs) {
			IContest contest = cc.getContestByRole(request);
			String contestDetail = "{contest=\"" + contest.getId() + "\"}";

			long[] metrics = cc.getMetrics();
			outputMetric(pw, "rest_total", "Total number of REST requests", MetricType.GAUGE, contestDetail, metrics[0]);
			outputMetric(pw, "feed_total", "Total number of contest event feed requests", MetricType.GAUGE, contestDetail,
					metrics[1]);
			outputMetric(pw, "websocket_total", "Total number of Web Socket connections", MetricType.GAUGE, contestDetail,
					metrics[2]);
			outputMetric(pw, "webpage_total", "Total number of webpage requests", MetricType.GAUGE, contestDetail,
					metrics[3]);
			outputMetric(pw, "download_total", "Total number of downloads", MetricType.GAUGE, contestDetail, metrics[4]);
			outputMetric(pw, "scoreboard_total", "Total number of scoreboard requests", MetricType.GAUGE, contestDetail,
					metrics[5]);
			outputMetric(pw, "desktop_total", "Total number of desktop requests", MetricType.GAUGE, contestDetail,
					metrics[7]);
			outputMetric(pw, "webcam_total", "Total number of webcam requests", MetricType.GAUGE, contestDetail,
					metrics[8]);
			outputMetric(pw, "audio_total", "Total number of audio requests", MetricType.GAUGE, contestDetail, metrics[9]);

			int numTeams = contest.getNumTeams();
			outputMetric(pw, "teams_total", "Total number of teams", MetricType.GAUGE, contestDetail, numTeams);

			int numProblems = contest.getNumProblems();
			outputMetric(pw, "problems_total", "Total number of problems", MetricType.GAUGE, contestDetail, numProblems);

			int numSubmissions = contest.getNumSubmissions();
			outputMetric(pw, "submissions_total", "Total number of submissions", MetricType.GAUGE, contestDetail,
					numSubmissions);
		}
	}

	private static void outputMetric(PrintWriter pw, String name, String title, MetricType type, String detail,
			long value) {
		outputMetric(pw, name, title, type, detail, value + "");
	}

	private static void outputMetric(PrintWriter pw, String name, String title, MetricType type, String detail,
			String value) {
		pw.println("# HELP " + name + " " + title);
		pw.println("# TYPE " + name + " " + type.name().toLowerCase());
		pw.println(name + detail + " " + value);
	}
}