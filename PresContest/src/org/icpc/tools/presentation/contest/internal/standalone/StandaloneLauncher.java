package org.icpc.tools.presentation.contest.internal.standalone;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.util.ArgumentParser;
import org.icpc.tools.contest.model.util.ArgumentParser.OptionParser;
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

		List<PresentationInfo> presentations = PresentationHelper.getPresentations();
		if (presentations.isEmpty()) {
			Trace.trace(Trace.USER, "No presentations found");
			System.exit(0);
		}
		sortPresentationsByCategory(presentations);

		List<String> presList = new ArrayList<String>();
		String[] displayStr = new String[1];
		ContestSource source = ArgumentParser.parse(args, new OptionParser() {
			@Override
			public boolean setOption(String option, List<Object> options) throws IllegalArgumentException {
				if ("--p".equals(option)) {
					ArgumentParser.expectOptions(option, options, "pres:string", "*");
					for (Object o : options)
						presList.add((String) o);
					return true;
				} else if ("--display".equals(option)) {
					ArgumentParser.expectOptions(option, options, "#:string");
					displayStr[0] = (String) options.get(0);
					return true;
				}
				return false;
			}

			@Override
			public void showHelp() {
				StandaloneLauncher.showHelp(presentations);
			}
		});

		if (source == null) {
			Trace.trace(Trace.ERROR, "Must provide a contest source");
			return;
		}

		if (presList.isEmpty()) {
			Trace.trace(Trace.ERROR, "Must provide one or more presentations");
			return;
		}

		// load presentation(s)
		Iterator<String> iter = presList.iterator();
		List<PresentationInfo> list = new ArrayList<>();
		while (iter.hasNext()) {
			PresentationInfo pi = findPresentation(presentations, iter.next());
			if (pi == null)
				System.exit(0);
			list.add(pi);
		}

		PresentationInfo[] pres = list.toArray(new PresentationInfo[0]);

		ContestSource.getInstance().outputValidation();

		launch(pres, displayStr[0]);
	}

	protected static void showHelp(List<PresentationInfo> presentations) {
		System.out.println("Usage: standalone.bat/sh contestURL user password [options]");
		System.out.println("   or: standalone.bat/sh contestPath [options]");
		System.out.println();
		System.out.println("  Options:");
		System.out.println("     --p pres1 pres2 ...");
		System.out.println("         Loop through showing the specified presentation");
		System.out.println("         names, ids, or numbers in order");
		System.out.println("     --display #");
		System.out.println("         Use the specified display");
		System.out.println("         1 = primary display, 2 = secondary display, etc.");
		System.out.println("     --help");
		System.out.println("         Shows this message");
		System.out.println("     --version");
		System.out.println("         Displays version information");
		System.out.println();

		Trace.trace(Trace.USER, "Available presentations:");
		int count = 1;
		String lastCategory = null;
		for (PresentationInfo pw : presentations) {
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

	protected static PresentationInfo findPresentation(List<PresentationInfo> presentations, String s) {
		PresentationInfo pw = null;
		try {
			int num = Integer.parseInt(s);
			List<PresentationInfo> list = PresentationHelper.getPresentations();
			sortPresentationsByCategory(list);
			if (num < 1 || num > list.size()) {
				Trace.trace(Trace.ERROR, "Invalid # ('" + s + "')");
				return null;
			}
			pw = list.get(num - 1);
		} catch (NumberFormatException nfe) {
			try {
				pw = PresentationHelper.matchPresentation(s);
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Presentation '" + s + "' is ambiguous");
				return null;
			}
		}

		if (pw != null)
			return pw;

		Trace.trace(Trace.ERROR, "Could not match '" + s + "' to a known presentation");
		System.exit(1);
		return null;
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