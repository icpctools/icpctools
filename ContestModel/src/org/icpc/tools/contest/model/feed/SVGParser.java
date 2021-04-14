package org.icpc.tools.contest.model.feed;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Helper class to read an SVG file and grab the (integer) viewBox width and height.
 */
public class SVGParser {
	public static Dimension parse(File file) throws IOException {
		if (file == null)
			return null;

		InputStream in = null;
		try {
			in = new FileInputStream(file);
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			inputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
			inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
			XMLEventReader reader = inputFactory.createXMLEventReader(in);

			while (reader.hasNext()) {
				XMLEvent event = reader.nextEvent();
				if (event.isStartElement()) {
					StartElement element = (StartElement) event;
					String name = element.getName().getLocalPart();
					if ("svg".equals(name)) {
						String viewBox = element.getAttributeByName(new QName("viewBox")).getValue();
						StringTokenizer st = new StringTokenizer(viewBox);
						st.nextToken(); // x
						st.nextToken(); // y
						Double w = Double.parseDouble(st.nextToken());
						Double h = Double.parseDouble(st.nextToken());
						return new Dimension(w.intValue(), h.intValue());
					}
				}
			}
		} catch (Exception e) {
			throw new IOException("Error reading xml", e);
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (Exception e) {
					// ignore
				}
		}
		return null;
	}
}