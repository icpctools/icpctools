package org.icpc.tools.contest.model.feed;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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
						String[] viewBoxValues = viewBox.split(" ");
						if (viewBoxValues.length > 3) {
							double w = (int) Double.parseDouble(viewBoxValues[2]);
							double h = (int) Double.parseDouble(viewBoxValues[3]);
							return new Dimension((int) w, (int) h);
						}
					}
				}
			}
		} catch (Exception e) {
			throw new IOException("Error reading svg", e);
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