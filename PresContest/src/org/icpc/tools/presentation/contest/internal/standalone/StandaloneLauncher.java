package org.icpc.tools.presentation.contest.internal.standalone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.presentation.core.IPresentationHandler;
import org.icpc.tools.presentation.core.IPresentationHandler.DeviceMode;
import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.PresentationWindow;
import org.icpc.tools.presentation.core.internal.PresentationInfo;

/**
 * Display a single presentation, typically connecting to a contest.
 */
public class StandaloneLauncher {
	public static void main(String[] args) {
		Trace.init("ICPC Standalone Presentations", "standalone", args);
		System.setProperty("apple.awt.application.name", "Presentation Client");

		if (args == null || args.length == 0 || args[0].equals("--help")) {
			showHelp();
			return;
		}

		// load presentation(s)
		StringTokenizer st = new StringTokenizer(args[0], "|");
		List<PresentationInfo> list = new ArrayList<>();
		while (st.hasMoreTokens())
			list.add(findPresentation(st.nextToken()));

		PresentationInfo[] pres = list.toArray(new PresentationInfo[list.size()]);

		// connect to contest source
		parseSource(args);

		ContestSource.getInstance().outputValidation();

		String displayStr = null;
		if (args.length > 3 && "--display".equals(args[args.length - 2]))
			displayStr = args[args.length - 1];

		launch(pres, displayStr);
	}

	public static void showHelp() {
		System.out.println("Usage: standalone.bat/sh presentations contestSource [user/host] [password/port] [options]");
		System.out.println();
		System.out.println("   presentations");
		System.out.println("      one or more presentation names or ids, separated by |");
		System.out.println("   contestSource [user/host] [password/port]");
		System.out.println("      \"http://\" to connect to a CDS, or");
		System.out.println("      \"https:// [user] [password] to connect to a secure CDS, or");
		System.out.println("      \"ccs [host] [port]\" to connect to CCS, or");
		System.out.println("      \"[folder]\" to load from a contest data package archive folder");
		System.out.println("   options");
		System.out.println("      \"--display X\" display on screen X");
		System.out.println();
		System.out.println("Examples: standalone logo|photos https://cds tim pwd");
		System.out.println("          standalone timeline ccs ccsServer 4713");
		System.out.println("          standalone 1|3|16 c:\\myContestCDPfolder");
		System.out.println();

		List<PresentationInfo> list = PresentationHelper.getPresentations();
		if (list.isEmpty()) {
			Trace.trace(Trace.USER, "No presentations found");
			System.exit(0);
		}

		Trace.trace(Trace.USER, "Available presentations:");
		sortPresentationsByCategory(list);
		int count = 1;
		String lastCategory = null;
		for (PresentationInfo pw : list) {
			StringBuilder sb = new StringBuilder();
			String cat = pw.getCategory();
			if (cat != null && !cat.equals(lastCategory)) {
				if (lastCategory != null)
					sb.append("\n");
				sb.append("  -- " + cat + " --\n");
				lastCategory = cat;
			}
			sb.append("    " + count + ". ");
			if (count < 10)
				sb.append(" ");
			sb.append(pw.getName() + " (" + pw.getId() + ")");
			if (pw.getDescription() != null) {
				String s = pw.getDescription();
				if (s.contains("\n"))
					s = s.substring(0, s.indexOf("\n")) + "...";
				sb.append("\n      " + s);
			}
			Trace.trace(Trace.USER, sb.toString());
			count++;
		}
	}

	private static void sortPresentationsByCategory(List<PresentationInfo> presentations) {
		int size = presentations.size();
		for (int i = 0; i < size - 1; i++) {
			for (int j = i + 1; j < size; j++) {
				PresentationInfo a = presentations.get(i);
				PresentationInfo b = presentations.get(j);
				boolean swap = false;
				boolean compareNames = false;
				if (a.getCategory() == null && b.getCategory() == null)
					compareNames = true;
				else if (a.getCategory() != null && a.getCategory().equals(b.getCategory()))
					compareNames = true;
				else if (a.getCategory().compareToIgnoreCase(b.getCategory()) > 0) {
					swap = true;
				}

				if (compareNames && a.getName().compareToIgnoreCase(b.getName()) > 0) {
					swap = true;
				}
				if (swap) {
					presentations.set(i, b);
					presentations.set(j, a);
				}
			}
		}
	}

	protected static PresentationInfo findPresentation(String s) {
		PresentationInfo pw = null;
		try {
			int num = Integer.parseInt(s);
			List<PresentationInfo> list = PresentationHelper.getPresentations();
			if (num < 1 || num > list.size()) {
				Trace.trace(Trace.ERROR, "Invalid # ('" + s + "')");
				return null;
			}
			pw = list.get(num - 1);
		} catch (NumberFormatException nfe) {
			try {
				pw = PresentationHelper.matchPresentation(s);
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Presentation '" + s + "' is not unique");
				return null;
			}
		}

		if (pw != null)
			return pw;

		Trace.trace(Trace.ERROR, "Could not match '" + s + "' to a known presentation");
		System.exit(1);
		return null;
	}

	protected static void parseSource(String[] args) {
		try {
			if (args.length > 2)
				ContestSource.parseSource(args[1], args[2], args[3]);
			else
				ContestSource.parseSource(args[1]);
		} catch (IOException e) {
			Trace.trace(Trace.ERROR, "Invalid contest source: " + e.getMessage());
		}
	}

	protected static void launch(PresentationInfo[] pres, String displayStr) {
		Trace.trace(Trace.INFO, "Launching presentation");
		Trace.trace(Trace.INFO, "Source: " + ContestSource.getInstance());
		Trace.trace(Trace.INFO, "Presentations:");

		Presentation[] presentation = new Presentation[pres.length];
		for (int i = 0; i < pres.length; i++) {
			PresentationInfo info = pres[i];
			Trace.trace(Trace.INFO, "   " + info.getId() + " - " + info.getName());

			try {
				String className = info.getClassName();
				Class<?> c = pres.getClass().getClassLoader().loadClass(className);
				presentation[i] = (Presentation) c.newInstance();
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "      Could not load presentation");
				return;
			}
		}

		IPresentationHandler window = PresentationWindow.open();
		try {
			window.setWindow(new DeviceMode(displayStr));
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Invalid display option: " + displayStr + " " + e.getMessage());
		}
		window.setPresentations(0, presentation, null);
	}
}