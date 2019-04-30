package org.icpc.tools.contest.model.feed;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class LinkParser {

	public interface ILinkListener {
		public void linkFound(String s);
	}

	public static void parse(final ILinkListener listener, InputStream in) throws Exception {
		if (in == null)
			return;

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);
		SAXParser sp = factory.newSAXParser();

		try {
			sp.parse(in, new DefaultHandler() {
				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes)
						throws SAXException {
					if ("a".endsWith(qName)) {
						String href = attributes.getValue("href");
						if (href != null && href.length() > 0) {
							listener.linkFound(href);
						}
					}
				}
			});
		} catch (SAXParseException e) {
			// Trace.trace(Trace.ERROR, "Could not parse xml: '" + e.getMessage() + "' on line " +
			// e.getLineNumber());
			throw new IOException("Error reading xml", e);
		}
	}
}