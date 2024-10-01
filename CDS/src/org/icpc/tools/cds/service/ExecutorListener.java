package org.icpc.tools.cds.service;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import org.icpc.tools.cds.CDSConfig;
import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.cds.presentations.PresentationServer;
import org.icpc.tools.cds.video.ReactionVideoRecorder;
import org.icpc.tools.cds.video.VideoAggregator;
import org.icpc.tools.contest.Trace;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class ExecutorListener implements ServletContextListener {
	private static ScheduledThreadPoolExecutor executor;

	public static ScheduledThreadPoolExecutor getExecutor() {
		return executor;
	}

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		Trace.initSysout("CDS", servletContextEvent.getServletContext().getResourceAsStream("META-INF/MANIFEST.MF"));
		executor = new ScheduledThreadPoolExecutor(400, new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "CDS Worker");
				t.setPriority(Thread.NORM_PRIORITY);
				t.setDaemon(true);
				return t;
			}
		});
		servletContextEvent.getServletContext().setAttribute("executor", executor);

		new ContestFeedExecutor().start(executor);

		PresentationServer.getInstance().setExecutor(executor);

		ReactionVideoRecorder.getInstance().start(executor);
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		Trace.trace(Trace.USER, "Shutting down");
		ConfiguredContest[] ccs = CDSConfig.getContests();
		for (ConfiguredContest cc : ccs)
			cc.logMetrics();

		executor.shutdownNow();

		if (VideoAggregator.isRunning())
			VideoAggregator.getInstance().shutdownNow();
	}
}