package org.icpc.tools.contest.model.internal;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URI;

import org.icpc.tools.contest.Trace;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.DocumentLimits;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;

public class SVGUtil {
	private static SVGLoader loader = new SVGLoader();
	private static DocumentLimits docLimits = new DocumentLimits(DocumentLimits.DEFAULT_MAX_NESTING_DEPTH,
			DocumentLimits.DEFAULT_MAX_USE_NESTING_DEPTH, DocumentLimits.DEFAULT_MAX_PATH_COUNT + 1000);
	private static LoaderContext context = LoaderContext.builder().documentLimits(docLimits).build();

	private SVGUtil() {
		// use static methods
	}

	public static SVGDocument loadSVG(File svgFile) throws Exception {
		SVGDocument document = loader.load(svgFile.toURI().toURL(), context);

		if (document == null)
			throw new IllegalArgumentException("Invalid SVG file " + svgFile.getAbsolutePath());

		return document;
	}

	public static SVGDocument loadSVG(String svgFile, InputStream in) throws Exception {
		URI baseUri = URI.create("file:///");
		SVGDocument document = loader.load(in, baseUri, context);

		if (document == null)
			throw new IllegalArgumentException("Invalid SVG input stream " + svgFile);

		return document;
	}

	public static BufferedImage convertSVG(SVGDocument svg, int width, int height) {
		try {
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

			Graphics2D g = image.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			ViewBox viewBox = new ViewBox(0, 0, width, height);
			svg.render(null, g, viewBox);
			g.dispose();

			return image;
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Invalid SVG", e);
			return null;
		}
	}
}