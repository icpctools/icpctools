package org.icpc.tools.cds.presentations;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.icpc.tools.contest.Trace;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class PresentationsParser {
	private static final String PRESENTATION = "presentation";
	private static final String TRANSITION = "transition";

	protected List<PresentationInfo> presentations;
	protected List<PresentationInfo> transitions;

	public void load(InputStream in) throws Exception {
		if (in == null)
			return;

		presentations = new ArrayList<>();
		transitions = new ArrayList<>();

		SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
		try {
			sp.parse(in, new DefaultHandler() {
				protected Map<String, String> map;
				protected int level;
				private String value = null;

				@Override
				public void characters(char[] ch, int start, int length) {
					String v = new String(ch, start, length);
					if (value == null)
						value = v;
					else
						value += v;
				}

				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes) {
					if (level == 0) {
						if (!qName.equals("presentations"))
							Trace.trace(Trace.ERROR, "Invalid format for presentation.xml!");
					} else if (level == 1) {
						if (PRESENTATION.equals(qName)) {
							String id = attributes.getValue("id");
							String name = attributes.getValue("name");
							String category = attributes.getValue("category");
							String classN = attributes.getValue("class");
							String description = attributes.getValue("description");
							String[] properties = tokenize(attributes.getValue("properties"));
							String image = attributes.getValue("image");
							presentations.add(
									new PresentationInfo(id, name, category, classN, description, properties, image, false));
						}
						if (TRANSITION.equals(qName)) {
							String id = attributes.getValue("id");
							String name = attributes.getValue("name");
							String category = attributes.getValue("category");
							String classN = attributes.getValue("class");
							String description = attributes.getValue("description");
							String[] properties = tokenize(attributes.getValue("properties"));
							String image = attributes.getValue("image");
							transitions
									.add(new PresentationInfo(id, name, category, classN, description, properties, image, true));
						}

						map = new HashMap<>();
					} else {
						Trace.trace(Trace.ERROR, qName);
					}
					level++;
				}

				@Override
				public void endElement(String uri, String localName, String qName) {
					if (level == 3) {
						map.put(qName, value.trim());
						value = null;
					}
					level--;
				}
			});
		} catch (SAXParseException e) {
			Trace.trace(Trace.ERROR, "Could not read presentations:" + e.getLineNumber(), e);
			throw new IOException("Could not read presentations");
		}

		sortPresentations(presentations);
		sortPresentations(transitions);
	}

	private static String[] tokenize(String value) {
		if (value == null)
			return null;
		StringTokenizer st = new StringTokenizer(value, ",");
		List<String> list = new ArrayList<>();
		while (st.hasMoreTokens())
			list.add(st.nextToken().trim());
		return list.toArray(new String[list.size()]);
	}

	private static void sortPresentations(List<PresentationInfo> presentations) {
		 presentations.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
	}

	/**
	 * Returns the presentations.
	 *
	 * @return the presentations
	 */
	public List<PresentationInfo> getPresentations() {
		return presentations;
	}

	/**
	 * Returns the presentations.
	 *
	 * @return the presentations
	 */
	public List<PresentationInfo> getTransitions() {
		return transitions;
	}

	public static List<String> loadImages(InputStream in) throws Exception {
		if (in == null)
			return null;

		final List<String> images = new ArrayList<>();

		SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
		try {
			sp.parse(in, new DefaultHandler() {
				protected int level;

				@Override
				public void startElement(String uri, String localName, String name, Attributes attributes) {
					if (level == 1) {
						if (PRESENTATION.equals(name) || TRANSITION.equals(name)) {
							String image = attributes.getValue("image");
							if (image != null)
								images.add(image);
						}
					}
					level++;
				}

				@Override
				public void endElement(String uri, String localName, String qName) {
					level--;
				}
			});
		} catch (SAXParseException e) {
			Trace.trace(Trace.ERROR, "Could not read presentations:" + e.getLineNumber(), e);
			throw new IOException("Could not read presentations");
		}

		return images;
	}
}