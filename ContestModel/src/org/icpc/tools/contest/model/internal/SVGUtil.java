package org.icpc.tools.contest.model.internal;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.icpc.tools.contest.Trace;
import org.w3c.dom.svg.SVGDocument;

public class SVGUtil {
	private static class BufferedImageTranscoder extends ImageTranscoder {
		private BufferedImage img = null;

		@Override
		public BufferedImage createImage(int w, int h) {
			return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		}

		@Override
		public void writeImage(BufferedImage img2, TranscoderOutput output) {
			this.img = img2;
		}

		public BufferedImage getBufferedImage() {
			return img;
		}
	}

	private SVGUtil() {
		// use static methods
	}

	public static SVGDocument loadSVG(File svgFile) throws Exception {
		String parser = XMLResourceDescriptor.getXMLParserClassName();
		SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
		return factory.createSVGDocument(svgFile.getAbsolutePath());
	}

	public static SVGDocument loadSVG(String svgFile, InputStream in) throws Exception {
		String parser = XMLResourceDescriptor.getXMLParserClassName();
		SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
		return factory.createSVGDocument(svgFile, in);
	}

	public static BufferedImage convertSVG(SVGDocument svg, int width, int height) {
		try {
			String ws;
			String hs;
			String viewBox = svg.getDocumentElement().getAttribute("viewBox");
			String[] viewBoxValues = viewBox.split(" ");
			if (viewBoxValues.length == 4) {
				ws = viewBoxValues[2];
				hs = viewBoxValues[3];
			} else {
				ws = svg.getDocumentElement().getAttribute("width");
				hs = svg.getDocumentElement().getAttribute("height");
				if (ws.isBlank() || hs.isBlank())
					return null;
			}

			float w = Float.parseFloat(ws);
			float h = Float.parseFloat(hs);
			float scale = Math.min(width / w, height / h);

			BufferedImageTranscoder imageTranscoder = new BufferedImageTranscoder();
			imageTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, w * scale);
			imageTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, h * scale);

			TranscoderInput input = new TranscoderInput(svg);
			imageTranscoder.transcode(input, null);

			return imageTranscoder.getBufferedImage();
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Invalid SVG", e);
			return null;
		}
	}
}