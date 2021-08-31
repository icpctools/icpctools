package org.icpc.tools.contest.model.feed;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class LinkParser {

	public interface ILinkListener {
		void linkFound(String s);
	}

	public static void parse(final ILinkListener listener, InputStream in) throws IOException {
		if (in == null)
			return;

		try {
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			inputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
			inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
			XMLEventReader reader = inputFactory.createXMLEventReader(in);

			boolean inTd = false;
			while (reader.hasNext()) {
				XMLEvent event = reader.nextEvent();
				if (event.isStartElement()) {
					StartElement ref = (StartElement) event;
					String name = ref.getName().toString();
					if (inTd && "a".equals(name)) {
						String href = ref.getAttributeByName(new QName("href")).getValue();
						if (href != null && href.length() > 0)
							listener.linkFound(href);
					} else if ("td".equals(name)) {
						inTd = true;
					}
				}
				if (event.isEndElement()) {
					EndElement ref = (EndElement) event;
					String name = ref.getName().toString();
					if ("td".equals(name)) {
						inTd = false;
					}
				}
			}
		} catch (XMLStreamException e) {
			throw new IOException("Error reading xml", e);
		}
	}

	public static String[] parse(File file) throws IOException {
		final List<String> list = new ArrayList<>();

		// We need to remove the doctype from the file, if it has any
		InputStream fs = new FileInputStream(file);
		BufferedReader reader = new BufferedReader(new InputStreamReader(fs));
		StringBuilder sb = new StringBuilder();
		String line;

		do {
			line = reader.readLine();
			if (line != null && !line.startsWith("<!doctype")) {
				sb.append(line).append("\n");
			}
		} while (line != null);

		String contents = sb.toString();

		LinkParser.parse(new ILinkListener() {
			@Override
			public void linkFound(String s) {
				list.add(s);
			}
		}, new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)));

		return list.toArray(new String[0]);
	}
}