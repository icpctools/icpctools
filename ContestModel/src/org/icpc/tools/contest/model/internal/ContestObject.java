package org.icpc.tools.contest.model.internal;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.ParseException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.feed.RelativeTime;
import org.icpc.tools.contest.model.feed.Timestamp;
import org.w3c.dom.svg.SVGDocument;

public abstract class ContestObject implements IContestObject {
	public static final String ID = "id";
	protected static final BufferedImage MISSING_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
	protected static final String SINGLETON_ID = "main";

	protected String id;
	protected static List<String> ignoredProps = new ArrayList<>();

	interface Properties {
		public void addString(String key, String value);

		public void addLiteralString(String key, String value);

		public void addInt(String key, int value);

		public void addDouble(String key, double value);

		public void add(String key, Object value);

		public void addFileRef(String key, FileReferenceList value);

		public void addFileRefSubs(String key, FileReferenceList value);

		public void addArray(String key, String[] value);
	}

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

	public ContestObject() {
		// default constructor
	}

	public ContestObject(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		if (isSingleton())
			return SINGLETON_ID;
		return id;
	}

	protected void setId(String id) {
		this.id = id;
	}

	public boolean isSingleton() {
		return false;
	}

	@Override
	public int hashCode() {
		if (isSingleton())
			return getType().hashCode();
		if (id == null)
			return super.hashCode();
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !getClass().equals(obj.getClass()))
			return false;

		if (isSingleton() || obj == this)
			return true;

		return id != null && id.equals(((IContestObject) obj).getId());
	}

	@Override
	public IContestObject clone() {
		try {
			@SuppressWarnings("unchecked")
			Class<ContestObject> cl = (Class<ContestObject>) getClass();
			ContestObject co = cl.getDeclaredConstructor().newInstance();
			Map<String, Object> props = getProperties();
			for (String key : props.keySet())
				co.add(key, props.get(key));

			return co;
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error cloning object", e);
			return null;
		}
	}

	protected static boolean parseBoolean(Object value) {
		if (value instanceof Boolean)
			return (Boolean) value;
		return Boolean.parseBoolean((String) value);
	}

	protected static int parseInt(Object value) throws NumberFormatException {
		if (value instanceof Integer)
			return (Integer) value;
		return Integer.parseInt((String) value);
	}

	protected static long parseLong(Object value) throws NumberFormatException {
		if (value instanceof Long)
			return (Long) value;
		return Long.parseLong((String) value);
	}

	protected static double parseDouble(Object value) throws NumberFormatException {
		if (value instanceof Double)
			return (Double) value;
		return Double.parseDouble((String) value);
	}

	protected static Long parseTimestamp(Object value) throws DateTimeParseException {
		return Timestamp.parse((String) value);
	}

	protected static FileReferenceList parseFileReference(Object value) {
		if (value == null)
			return null;
		return new FileReferenceList(value);
	}

	protected static Location parseLocation(Object value) {
		Location loc = new Location(value);
		if (loc.isValid())
			return loc;
		return null;
	}

	public static long getTime(IContestObject obj) {
		if (obj instanceof TimedEvent) {
			return ((TimedEvent) obj).getTime();
		} else if (obj instanceof IJudgement) {
			IJudgement j = (IJudgement) obj;
			if (j.getEndContestTime() != null)
				return j.getEndTime();
			return j.getStartTime();
		}
		return 0;
	}

	public static long getContestTime(IContestObject obj) {
		if (obj instanceof TimedEvent) {
			return ((TimedEvent) obj).getContestTime();
		} else if (obj instanceof IJudgement) {
			IJudgement j = (IJudgement) obj;
			if (j.getEndContestTime() != null)
				return j.getEndContestTime();
			return j.getStartContestTime();
		}
		return 0;
	}

	protected static Long parseRelativeTime(Object value) throws ParseException {
		if (value == null || "null".equals(value))
			return null;
		return RelativeTime.parse((String) value);
	}

	protected boolean addImpl(String name, Object value) throws Exception {
		return false;
	}

	public final void add(String name, Object value) {
		try {
			if (ID.equals(name)) {
				id = (String) value;
				return;
			}

			if (addImpl(name, value))
				return;
		} catch (Exception e) {
			Trace.trace(Trace.ERROR,
					"Error adding property: " + IContestObject.getTypeName(getType()) + "/" + name + ":" + value, e);
			return;
		}

		String message = "Unknown property ignored: " + IContestObject.getTypeName(getType()) + "/" + name;
		if (ignoredProps.contains(message))
			return;
		Trace.trace(Trace.WARNING, message);
		ignoredProps.add(message);
	}

	protected void getProperties(Properties props) {
		props.addString(ID, id);
	}

	@Override
	public final Map<String, Object> getProperties() {
		Map<String, Object> props = new SimpleMap();
		getProperties(new Properties() {
			@Override
			public void addString(String key, String value) {
				if (value != null)
					props.put(key, value);
			}

			@Override
			public void addLiteralString(String key, String value) {
				if (value != null)
					props.put(key, value);
			}

			@Override
			public void addInt(String key, int value) {
				props.put(key, value);
			}

			@Override
			public void addDouble(String key, double value) {
				props.put(key, value);
			}

			@Override
			public void add(String key, Object value) {
				if (value != null)
					props.put(key, value);
			}

			@Override
			public void addFileRef(String key, FileReferenceList value) {
				if (value != null)
					props.put(key, value);
			}

			@Override
			public void addFileRefSubs(String key, FileReferenceList value) {
				if (value != null)
					props.put(key, value);
			}

			@Override
			public void addArray(String key, String[] value) {
				if (value != null)
					props.put(key, value);
			}
		});
		return props;
	}

	public final void writeBody(final JSONEncoder je) {
		getProperties(new Properties() {
			@Override
			public void addString(String key, String o) {
				if (o != null)
					je.encode(key, o);
			}

			@Override
			public void addLiteralString(String key, String o) {
				if (o != null)
					je.encodeString(key, o);
			}

			@Override
			public void addInt(String key, int i) {
				je.encode(key, i);
			}

			@Override
			public void addDouble(String key, double d) {
				je.encode(key, d);
			}

			@Override
			public void addFileRef(String key, FileReferenceList refList) {
				if (refList == null)
					return;

				if (refList.isEmpty())
					je.encodePrimitive(key, "[]");
				else
					je.encodePrimitive(key, "[" + String.join(",", refList.getRefs()) + "]");
			}

			@Override
			public void addFileRefSubs(String key, FileReferenceList refList) {
				if (refList == null)
					return;

				if (refList.isEmpty())
					je.encodePrimitive(key, "[]");
				else {
					String[] s = refList.getRefs();
					for (int i = 0; i < s.length; i++)
						s[i] = je.replace(s[i]);
					je.encodePrimitive(key, "[" + String.join(",", s) + "]");
				}
			}

			@Override
			public void add(String key, Object o) {
				if (o != null)
					je.encodePrimitive(key, o.toString());
			}

			@Override
			public void addArray(String key, String[] value) {
				if (value == null)
					return;

				if (value.length == 0)
					je.encodePrimitive(key, "[]");
				else
					je.encodePrimitive(key, "[\"" + String.join("\",\"", value) + "\"]");
			}
		});
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = new ArrayList<>();

		if ((id == null || id.isEmpty()) && !isSingleton())
			errors.add("Invalid id " + id);

		return errors;
	}

	protected File getFile(FileReference ref, String property, boolean force) {
		if (ref == null)
			return null;

		if (ref.file != null && ref.file.exists()) {
			return ref.file;
		}

		if (!force)
			return null;

		try {
			synchronized (ref) {
				if (ref.file != null && ref.file.exists())
					return ref.file;
				return ContestSource.getInstance().getFile(this, ref, property);
			}
		} catch (Exception e) {
			return null;
		}
	}

	protected File[] getFiles(FileReferenceList list, String property, boolean force) {
		int size = list.size();
		File[] files = new File[size];
		for (int i = 0; i < size; i++) {
			files[i] = getFile(list.get(i), property, force);
		}
		return files;
	}

	public interface ReferenceMatcher {
		public FileReference getBestMatch(FileReferenceList list);
	}

	/**
	 * Finds the best image where at least one dimension is as big as the given width and height. If
	 * there is no image big enough, the next largest image is returned. For now, assume all images
	 * have similar aspect ratio.
	 */
	protected class ImageSizeFit implements ReferenceMatcher {
		private int width;
		private int height;

		public ImageSizeFit(int width, int height) {
			this.width = width;
			this.height = height;
		}

		@Override
		public FileReference getBestMatch(FileReferenceList list) {
			FileReference bestRef = null;
			for (FileReference ref : list) {
				if (bestRef == null)
					bestRef = ref;
				else {
					if (bestRef.width < width && bestRef.height < height) {
						// current best image is too small - is this one better (larger than current)?
						if (ref.width > bestRef.width || ref.height > bestRef.height)
							bestRef = ref;
					} else if (bestRef.width > width && bestRef.height > height) {
						// current image is too big - is this one better (smaller but still big enough)?
						if (ref.width < bestRef.width || ref.height < bestRef.height) {
							if (ref.width >= width || ref.height >= height)
								bestRef = ref;
						}
					}
				}
			}
			/*if (list.size() > 1) {
				System.out.println("Wanted: " + width + "x" + height + " found: " + bestRef.width + "x" + bestRef.height);
				for (FileReference ref : list) {
					System.out.println("   " + ref.width + "x" + ref.height);
				}
			}*/
			return bestRef;
		}
	}

	protected FileReference getBestFileReference(FileReferenceList list, ReferenceMatcher fit) {
		if (list == null || list.isEmpty())
			return null;

		if (list.size() == 1)
			return list.first();

		if (fit != null)
			return fit.getBestMatch(list);

		return list.first();
	}

	public BufferedImage getRefImage(String property, FileReferenceList list, int width, int height, boolean forceLoad,
			boolean resizeToFit) {
		Object data = null;

		FileReference ref = getBestFileReference(list, new ImageSizeFit(width, height));
		if (ref == null)
			return null;

		if (ref.data != null)
			data = ref.data;
		else if (forceLoad) {
			data = loadImage(getFile(ref, property, true));
			if (data == null)
				data = MISSING_IMAGE;
			ref.data = data;
		}

		if (data == MISSING_IMAGE)
			return null;
		if (resizeToFit) {
			if (data instanceof BufferedImage)
				return ImageScaler.scaleImage((BufferedImage) data, width, height);
			// else if (data instanceof SVGDocument)
			return resizeSVG((SVGDocument) data, width, height);
		}
		if (data instanceof BufferedImage)
			return (BufferedImage) data;
		// else if (data instanceof SVGDocument)
		return resizeSVG((SVGDocument) data, width, height);
	}

	private static Object loadImage(File f) {
		if (f == null || !f.exists())
			return null;

		try {
			if (f.getName().endsWith(".svg"))
				return loadSVG(f);
			return ImageIO.read(f);
		} catch (Exception e) {
			return null;
		}
	}

	public static BufferedImage readSVG2BufferedImage(File svgFile) throws Exception {
		SVGDocument svg = loadSVG(svgFile);
		BufferedImage image = resizeSVG(svg, 10000, 10000);
		return image;
	}

	private static SVGDocument loadSVG(File svgFile) throws Exception {
		String parser = XMLResourceDescriptor.getXMLParserClassName();
		SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
		return factory.createSVGDocument(svgFile.getAbsolutePath());
	}

	private static BufferedImage resizeSVG(SVGDocument svg, int width, int height) {
		try {
			String viewBox = svg.getDocumentElement().getAttribute("viewBox");
			String[] viewBoxValues = viewBox.split(" ");
			if (viewBoxValues.length < 4)
				return null;

			float w = Float.parseFloat(viewBoxValues[2]);
			float h = Float.parseFloat(viewBoxValues[3]);
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(IContestObject.getTypeName(getType()) + "\n");
		Map<String, Object> props = getProperties();
		for (String key : props.keySet())
			sb.append("  " + key + ": " + props.get(key) + "\n");

		sb.deleteCharAt(sb.length() - 1);

		return sb.toString();
	}
}