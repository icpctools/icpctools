package org.icpc.tools.presentation.admin.internal;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.presentation.core.internal.PresentationInfo;
import org.icpc.tools.presentation.core.internal.PresentationsParser;

public class PresentationHelper {
	private static Display display;
	private static Map<String, Image> images = new HashMap<>();

	protected PresentationHelper() {
		super();
	}

	protected static void setDisplay(Display d) {
		display = d;
	}

	protected static void dispose() {
		for (Image img : images.values()) {
			img.dispose();
		}
	}

	protected static Image getPresentationImage(PresentationInfo info) {
		Image img = images.get(info.getId());
		if (img != null)
			return img;

		if (info.isTransition())
			return ImageResource.getImage(ImageResource.IMG_MISSING_TRANSITION);

		return ImageResource.getImage(ImageResource.IMG_MISSING_PRESENTATION);
	}

	/**
	 * Load the presentations from an archive.
	 */
	protected static PresentationsParser loadPresentation(File f) {
		Trace.trace(Trace.INFO, "Looking for presentations in " + f);

		PresentationsParser parser = new PresentationsParser();
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(f);
			ZipEntry entry = zipFile.getEntry("META-INF/presentations.xml");
			if (entry != null) {
				InputStream in = zipFile.getInputStream(entry);
				parser.load(in);

				List<PresentationInfo> presentations = parser.getPresentations();
				if (!presentations.isEmpty())
					Trace.trace(Trace.INFO, "   Found " + presentations.size() + " presentations");
				for (PresentationInfo info : presentations) {
					String imgStr = info.getImage();
					if (display != null && imgStr != null && imgStr.length() > 1) {
						InputStream in2 = null;
						try {
							ZipEntry imgEntry = zipFile.getEntry(info.getImage());
							in2 = zipFile.getInputStream(imgEntry);

							Image img = new Image(display, in2);
							images.put(info.getId(), img);
						} catch (Exception e) {
							Trace.trace(Trace.ERROR, "Error loading image: " + info.getImage() + ": " + e.getMessage());
						} finally {
							try {
								if (in2 != null)
									in2.close();
							} catch (Exception e) {
								// ignore
							}
						}
					}
				}

				List<PresentationInfo> transitions = parser.getTransitions();
				if (!transitions.isEmpty())
					Trace.trace(Trace.INFO, "   Found " + transitions.size() + " transitions");
				for (PresentationInfo info : transitions) {
					String imgStr = info.getImage();
					if (display != null && imgStr != null && imgStr.length() > 1) {
						InputStream in2 = null;
						try {
							ZipEntry imgEntry = zipFile.getEntry(info.getImage());
							in2 = zipFile.getInputStream(imgEntry);

							Image img = new Image(display, in2);
							images.put(info.getId(), img);
						} catch (Exception e) {
							Trace.trace(Trace.ERROR, "Error loading image: " + info.getImage() + ": " + e.getMessage());
						} finally {
							try {
								if (in2 != null)
									in2.close();
							} catch (Exception e) {
								// ignore
							}
						}
					}
				}
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error loading presentation list", e);
		} finally {
			try {
				if (zipFile != null)
					zipFile.close();
			} catch (Exception e) {
				// ignore
			}
		}

		return parser;
	}
}