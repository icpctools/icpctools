package org.icpc.tools.presentation.admin.internal;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.CDSUtil;
import org.icpc.tools.contest.model.util.ArgumentParser;
import org.icpc.tools.contest.model.util.ArgumentParser.OptionParser;
import org.icpc.tools.contest.model.util.ArgumentParser.Source;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.Color;
import java.awt.Component;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MultiAdmin {
	protected static void showHelp() {
		System.out.println();
		System.out.println("Usage: multiAdmin.bat/sh cdsURL user password . . . [options]");
		System.out.println();
		System.out.println("  Options:");
		System.out.println("     --help");
		System.out.println("         Shows this message");
		System.out.println("     --version");
		System.out.println("         Displays version information");
	}

	public static void main(String[] args) {
		Trace.init("ICPC Presentation Multi Admin", "multiAdmin", args);
		Trace.trace(Trace.INFO, "ICPC Presentation Multi Admin " + getVersion(Trace.getVersion()) + " starting");

		Source source = ArgumentParser.parseSource(args, new OptionParser() {
			@Override
			public boolean setOption(String option, List<Object> options) throws IllegalArgumentException {
				return false;
			}

			@Override
			public void showHelp() {
				MultiAdmin.showHelp();
			}
		});

		if (source == null) {
			showHelp();
			return;
		}

		String url = source.src[0];

		try {
			URL url2 = new URL(url);
			if (url2.getPath() != null && url2.getPath().length() > 1) {
				Trace.trace(Trace.ERROR, "URL should not contain a path");
				return;
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not parse URL");
			return;
		}

		final boolean CDSCheck = false;
		if (CDSCheck) {
			Trace.trace(Trace.INFO, "CDSUtil url " + url + " user " + source.user);
			CDSUtil util = new CDSUtil(url, source.user, source.password);
			try {
				util.verifyCDS();
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "CDS connection failure: " + e.getMessage());
				return;
			}

			Trace.trace(Trace.INFO, "checkForUpdates");
			util.checkForUpdates("presentationAdmin-");
		}

		Trace.trace(Trace.INFO, "MultiView");
		MultiView v = new MultiView(source.src, source.user, source.password);

		SwingUtilities.invokeLater(() -> createAndShowGui(v));
	}

	private static void createAndShowGui(MultiView v) {
		Trace.trace(Trace.INFO, "JFrame");
		JFrame frame = new JFrame("Presentation MultiAdmin");
		swingDark(frame);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel main = v.createMainPanel();
		frame.getContentPane().add(main);
		frame.pack();
		SwingUtilities.invokeLater(() -> v.connect());
		frame.setVisible(true);
		Trace.trace(Trace.INFO, "JFrame after");
	}

	private static String getVersion(String ver) {
		if (ver == null)
			return "dev";
		return ver;
	}

	public static void swingDark(Component c) {
		// Dark LAF, from
		// https://stackoverflow.com/questions/36128291/how-to-make-a-swing-application-have-dark-nimbus-theme-netbeans
		try {
			UIManager.setLookAndFeel(new NimbusLookAndFeel());
			UIManager.put("control", new Color(128, 128, 128));
			UIManager.put("info", new Color(128, 128, 128));
			UIManager.put("nimbusBase", new Color(18, 30, 49));
			UIManager.put("nimbusAlertYellow", new Color(248, 187, 0));
			UIManager.put("nimbusDisabledText", new Color(128, 128, 128));
			UIManager.put("nimbusFocus", new Color(115, 164, 209));
			UIManager.put("nimbusGreen", new Color(176, 179, 50));
			UIManager.put("nimbusInfoBlue", new Color(66, 139, 221));
			UIManager.put("nimbusLightBackground", new Color(18, 30, 49));
			UIManager.put("nimbusOrange", new Color(191, 98, 4));
			UIManager.put("nimbusRed", new Color(169, 46, 34));
			UIManager.put("nimbusSelectedText", new Color(255, 255, 255));
			UIManager.put("nimbusSelectionBackground", new Color(104, 93, 156));
			UIManager.put("text", new Color(230, 230, 230));
			SwingUtilities.updateComponentTreeUI(c);
		} catch (UnsupportedLookAndFeelException exc) {
			System.err.println("Nimbus: Unsupported Look and feel!");
		}
	}
}
