package org.icpc.tools.presentation.contest.internal.standalone;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.presentation.core.internal.PresentationInfo;
import org.icpc.tools.presentation.core.internal.PresentationsParser;

public class PresentationHelper {
	protected static PresentationsParser parser;

	private PresentationHelper() {
		// do not create, use static methods
	}

	/**
	 * Returns the presentations with the given id.
	 *
	 * @return a presentation, or null if none was found
	 */
	public static PresentationInfo findPresentation(String id) {
		if (parser == null)
			loadPresentations();

		Iterator<PresentationInfo> iterator = parser.getPresentations().iterator();
		while (iterator.hasNext()) {
			PresentationInfo pw = iterator.next();
			if (id.equals(pw.getId()))
				return pw;
		}

		return null;
	}

	/**
	 * Returns the presentations that contain the given partial id.
	 *
	 * @return a presentation, or null if none was found
	 */
	public static List<PresentationInfo> findPresentations(String id) {
		if (id == null || id.length() < 2)
			return null;

		if (parser == null)
			loadPresentations();

		Iterator<PresentationInfo> iterator = parser.getPresentations().iterator();
		List<PresentationInfo> list = new ArrayList<>();
		while (iterator.hasNext()) {
			PresentationInfo pw = iterator.next();
			if (pw.getId().contains(id))
				list.add(pw);
		}

		return list;
	}

	/**
	 * Returns the presentations.
	 *
	 * @return the presentations
	 */
	public static List<PresentationInfo> getPresentations() {
		if (parser == null)
			loadPresentations();

		return parser.getPresentations();
	}

	/**
	 * Load the presentations.
	 */
	private static synchronized void loadPresentations() {
		if (parser != null)
			return;

		Trace.trace(Trace.INFO, "->- Loading presentations ->-");

		// add 'local' presentations
		try {
			PresentationHelper ph = new PresentationHelper();
			ClassLoader cl = ph.getClass().getClassLoader();
			InputStream in = cl.getResourceAsStream("META-INF/presentations.xml");

			parser = new PresentationsParser();
			parser.load(in);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not load presentations", e);
		}

		Trace.trace(Trace.INFO, "-<- Done loading presentations -<-");
	}
}