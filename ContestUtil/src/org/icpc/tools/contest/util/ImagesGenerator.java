package org.icpc.tools.contest.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.DiskContestSource;
import org.icpc.tools.contest.model.internal.Contest;

/**
 * Helps convert logos and other images from raw source (from the CMS export or hand-built contest
 * folder) to usable contest data. Copies are made when an image is found but doesn't have the
 * correct name, warnings are output when an image has issues, and smaller versions of photos and
 * logos are created to reduce network load / improve performance.
 *
 * Arguments: contestRoot - a contest location, i.e. CDP/CAF root folder
 */
public class ImagesGenerator {
	private static final int MIN_SIZE = 250;
	private static final int HD_MARGIN = 15;
	private static final int HD_TOP = 30;
	private static final double FUDGE = 0.075; // 7.5%

	private static final String[] IMAGE_EXTENSIONS = new String[] { "svg", "png", "jpg", "jpeg" };

	private static final boolean DEBUG = false;
	private static final Rectangle LOGO = new Rectangle(95, 795, 230, 230);
	private static final Rectangle TEXT = new Rectangle(370, 795, 1275, 230);

	private static final Dimension DESKTOP = new Dimension(1920, 1080);
	private static final Dimension ICON = new Dimension(56, 56);
	private static final Dimension DOMJUDGE_ICON = new Dimension(64, 64);
	private static final Dimension TILE = new Dimension(160, 160);

	private static BufferedImage icpcLogo, logo;

	static class ImageSpec {
		Dimension d;
		boolean pad;

		public ImageSpec(Dimension d, boolean p) {
			this.d = d;
			this.pad = p;
		}
	}

	class ImageType {
		File folder;
		List<File> files = new ArrayList<>();

		public ImageType(String folderName) {
			folder = new File(contestRoot, "images" + File.separator + folderName);
			if (!folder.exists())
				folder.mkdir();
		}
	}

	private final File contestRoot;
	private Contest contest;
	private Font masterFont;
	private Font[] fonts;

	public static void main(String[] args) {
		Trace.init("ICPC Image Generator", "imageGenerator", args);

		if (args == null || args.length != 1) {
			Trace.trace(Trace.ERROR, "Missing argument, must point to a contest location");
			System.exit(0);
			return;
		}

		File contestRoot = new File(args[0]);
		if (!contestRoot.exists()) {
			Trace.trace(Trace.ERROR, "Contest location could not be found: " + contestRoot.getAbsolutePath());
			System.exit(0);
			return;
		}

		long time = System.currentTimeMillis();
		ImagesGenerator generator = new ImagesGenerator(contestRoot);

		Trace.trace(Trace.USER, "----- Generating organization logos -----");
		generator.generateOrganizationLogos();

		Trace.trace(Trace.USER, "----- Generating team desktop & overlays -----");
		generator.generateTeamDesktop();

		Trace.trace(Trace.USER, "----- Generating team photos -----");
		generator.generateTeamPhotos();

		// temp - rename logo files
		/*try {
			Contest c = new Contest();
			TSVImporter.importTeams(contestRoot, c);
			ITeam[] teams = c.getTeams();
			for (ITeam t : teams) {
				File from = new File(args[0], "images" + File.separator + "logo" + File.separator + t.getId() + ".png");
				File to = new File(args[0],
						"images" + File.separator + "logo" + File.separator + t.getOrganizationId() + ".png");

				if (from.exists())
					Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES);

				from = new File(args[0], "images" + File.separator + "tile" + File.separator + t.getId() + ".png");
				to = new File(args[0],
						"images" + File.separator + "tile" + File.separator + t.getOrganizationId() + ".png");

				if (from.exists())
					Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}*/

		File file = new File(contestRoot, "images");
		if (!file.exists())
			file.mkdir();

		// reload contest to pick up all images we just generated
		generator.reload();

		/*Trace.trace(Trace.USER, "----- Generating ribbon -----");
		try {
			generator.createRibbon();
		} catch (Exception e) {
			e.printStackTrace();
		}*/

		Trace.trace(Trace.USER, "----- Generating preview -----");
		try {
			generator.createPreview();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			generator.createContestPreview();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Trace.trace(Trace.USER, "----- Generating person photos -----");
		generator.generatePersonPhotos();

		Trace.trace(Trace.USER, "----- Done generating -----");
		Trace.trace(Trace.USER, (System.currentTimeMillis() - time) + " ms");

		Trace.trace(Trace.USER, "----- Missing data report -----");

		generator.missingDataReport();
	}

	protected ImagesGenerator(File contestRoot) {
		this.contestRoot = contestRoot;
		init();
	}

	public void generateTeamPhotos() {
		ImageSpec[] spec = new ImageSpec[] { new ImageSpec(DESKTOP, false) };
		generateImages("teams", "photo", "jpg", spec);
	}

	private static BufferedImage removeBorders(BufferedImage img) throws Exception {
		if (img == null)
			return null;

		int w = img.getWidth();
		int h = img.getHeight();

		if (!img.getColorModel().hasAlpha())
			return img;

		WritableRaster raster = img.getAlphaRaster();
		if (raster == null)
			return img;

		int[] pix = raster.getPixels(0, 0, w, h, (int[]) null);

		int left = 0;
		boolean clear = true;
		while (clear && left < w) {
			for (int i = 0; i < h; i++)
				if (pix[left + i * w] != 0) {
					clear = false;
					break;
				}
			left++;
		}

		clear = true;
		int right = w - 1;
		while (clear && right > left) {
			for (int i = 0; i < h; i++)
				if (pix[right + i * w] != 0) {
					clear = false;
					break;
				}
			right--;
		}
		if (left == right)
			throw new Exception("No image left after removing borders");

		int top = 0;
		clear = true;
		while (clear && top < h) {
			for (int i = 0; i < w; i++)
				if (pix[top * w + i] != 0) {
					clear = false;
					break;
				}
			top++;
		}

		clear = true;
		int bottom = h - 1;
		while (clear && bottom > top) {
			for (int i = 0; i < w; i++)
				if (pix[bottom * w + i] != 0) {
					clear = false;
					break;
				}
			bottom--;
		}

		Rectangle r = new Rectangle(left, top, right - left, bottom - top);
		if (r.width == w && r.height == h)
			return img;

		// System.out.println("Removing borders: " + w + "x" + h + " -> " + r.width + "x" +
		// r.height);

		BufferedImage newImg = null;
		if (img.getType() == BufferedImage.TYPE_CUSTOM)
			newImg = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_ARGB);
		else
			newImg = new BufferedImage(r.width, r.height, img.getType());
		Graphics g = newImg.getGraphics();
		g.drawImage(img, -r.x, -r.y, null);
		g.dispose();
		return newImg;
	}

	public void generateTeamDesktop() {
		File desktopFolder = new File(contestRoot, "images" + File.separator + "desktop");
		if (!desktopFolder.exists())
			desktopFolder.mkdirs();

		File overlayFolder = new File(contestRoot, "images" + File.separator + "overlay");
		if (!overlayFolder.exists())
			overlayFolder.mkdir();

		File orgRootFolder = new File(contestRoot, "organizations");

		ITeam[] teams = contest.getTeams();
		for (ITeam team : teams) {
			Trace.trace(Trace.USER,
					"Generating desktop background for: " + team.getLabel() + " - " + team.getActualDisplayName());

			String teamId = team.getId();
			String name = team.getActualDisplayName();

			try {
				BufferedImage logoImg = null;
				long mod = System.currentTimeMillis();
				IOrganization org = contest.getOrganizationById(team.getOrganizationId());
				if (org != null) {
					if (contest.getNumTeams() == contest.getNumOrganizations())
						name = org.getActualFormalName();

					File orgFolder = new File(orgRootFolder, org.getId());
					File imgFile = getDefaultFile(orgFolder, "logo");
					if (imgFile.exists())
						mod = imgFile.lastModified();

					logoImg = org.getLogoImage(1920, 1080, true, true);
				}

				// generate desktop
				String teamLabel = teamId;
				if (team.getLabel() != null)
					teamLabel = team.getLabel();
				File file = new File(desktopFolder, teamLabel + ".jpg");
				if (!file.exists() || file.lastModified() != mod) {
					createDesktop(logoImg, name, fonts, file);
					file.setLastModified(mod);
				}

				// generate overlay
				file = new File(overlayFolder, teamLabel + ".png");
				if (!file.exists() || file.lastModified() != mod) {
					createOverlay(logoImg, name, fonts, file);
					file.setLastModified(mod);
				}
			} catch (Exception e) {
				Trace.trace(Trace.USER, "Error generating desktop for " + team.getActualDisplayName(), e);
			}
		}

		// generate desktop for spare team machines
		try {
			File file = new File(desktopFolder, "spare.jpg");
			if (!file.exists()) {
				Trace.trace(Trace.USER, "Generating spare desktop");
				createDesktop(null, null, fonts, file);
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating desktop for missing teams", e);
		}
	}

	private static boolean hasExtension(String filename, String[] extensions) {
		if (filename == null || extensions == null)
			return false;

		for (String ext : extensions) {
			if (filename.endsWith("." + ext))
				return true;
		}
		return false;
	}

	private static File getDefaultFile(File folder, String property) {
		// first look for default names, in order of extension preference
		for (String s : IMAGE_EXTENSIONS) {
			File f = new File(folder, property + "." + s);
			if (f.exists())
				return f;
		}

		File imgFile = null;
		File[] files = folder.listFiles();
		for (File ff : files) {
			// skip generated files
			String name = ff.getName();
			if (name.startsWith(property + ".") && hasExtension(name, IMAGE_EXTENSIONS) && name.contains("x")) {
				// skip over generated logos (should really use regex to look for logo.<w>x<h>.)
				continue;
			}
			imgFile = ff;
		}
		return imgFile;
	}

	public void generateOrganizationLogos() {
		ImageSpec[] spec = new ImageSpec[] { new ImageSpec(DESKTOP, false), new ImageSpec(ICON, true),
				new ImageSpec(DOMJUDGE_ICON, true), new ImageSpec(TILE, true) };
		generateImages("organizations", "logo", "png", spec);
	}

	private static void generateImageSpec(BufferedImage img, ImageSpec[] spec, File folder, String property,
			String preferredExtension, long mod) throws IOException {
		for (ImageSpec is : spec) {
			if (img.getWidth() < is.d.width || img.getHeight() < is.d.height)
				continue;

			BufferedImage scImg = ImageScaler.scaleImage(img, is.d.width, is.d.height);
			if (!is.pad) {
				writeImage(property, scImg, folder, mod, true, preferredExtension);
			} else {
				double aspect = scImg.getWidth() / (double) scImg.getHeight();
				if (aspect < 1.0 - FUDGE || aspect > 1.0 + FUDGE)
					writeImage(property, scImg, folder, mod, true, preferredExtension);

				scImg = ImageScaler.padImage(scImg);
				writeImage(property, scImg, folder, mod, true, preferredExtension);
			}
		}
	}

	public void generateImages(String objectName, String property, String preferredExtension, ImageSpec[] spec) {
		File rootFolder = new File(contestRoot, objectName);
		if (!rootFolder.exists()) {
			Trace.trace(Trace.ERROR, "Couldn't find /" + objectName + " folder. Exiting");
			return;
		}

		boolean transparency = "png".equals(preferredExtension);

		int numWarnings = 0;

		File[] folders = rootFolder.listFiles();
		for (File folder : folders) {
			if (folder.isDirectory()) {
				String folderName = folder.getName();
				File imgFile = getDefaultFile(folder, property);

				if (imgFile == null) {
					Trace.trace(Trace.ERROR, "Warning: no image found (" + folder.getName() + ")");
					numWarnings++;
					continue;
				}

				try {
					Trace.trace(Trace.USER, "Updating " + objectName + " " + property + ": " + folderName);

					long mod = imgFile.lastModified();
					BufferedImage img = ImageIO.read(imgFile);
					if (img == null) {
						Trace.trace(Trace.WARNING, "Couldn't read image");
						continue;
					}

					img = removeBorders(img);

					// clean up old generated files
					File[] files2 = folder.listFiles();
					for (File ff : files2) {
						if (ff.getName().startsWith(property + ".") && hasExtension(ff.getName(), IMAGE_EXTENSIONS)
								&& ff.lastModified() != mod)
							ff.delete();
					}

					numWarnings = checkForWarnings(folderName, img, numWarnings, transparency);

					String name = imgFile.getName().toLowerCase();
					if (!name.equals(property + "." + preferredExtension)) {
						// there's no filename with the spec extension, create one
						writeImage(property, img, folder, mod, false, preferredExtension);
					}

					generateImageSpec(img, spec, folder, property, preferredExtension, mod);
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error generating image: " + imgFile.getAbsolutePath(), e);
				}
			}
		}

		Trace.trace(Trace.USER, numWarnings + " warnings out of " + contest.getNumOrganizations());
	}

	public void generatePersonPhotos() {
		ImageSpec[] spec = new ImageSpec[] { new ImageSpec(DESKTOP, false), new ImageSpec(TILE, false) };
		generateImages("persons", "photo", "jpg", spec);
	}

	private static void createDesktop(BufferedImage img, String name, Font[] fonts, File file) throws IOException {
		BufferedImage newImg = new BufferedImage(DESKTOP.width, DESKTOP.height, Transparency.OPAQUE);
		Graphics2D g = (Graphics2D) newImg.getGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, DESKTOP.width, DESKTOP.height);

		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g.setFont(fonts[2]);
		FontMetrics fm = g.getFontMetrics();

		if (img != null) {
			BufferedImage tempImg = ImageScaler.scaleImage(img, DESKTOP.width * 0.7,
					(DESKTOP.height - HD_MARGIN * 2) * 0.75);
			g.drawImage(tempImg, (DESKTOP.width - tempImg.getWidth()) / 2,
					(DESKTOP.height - tempImg.getHeight() - fm.getHeight()) / 2, null);
			tempImg.flush();
		}

		if (logo != null) {
			BufferedImage tempImg = ImageScaler.scaleImage(logo, 250, 250);
			g.drawImage(tempImg, HD_MARGIN, HD_MARGIN + HD_TOP + 125 - tempImg.getHeight() / 2, null);
			tempImg.flush();
		}

		if (icpcLogo != null) {
			BufferedImage tempImg = ImageScaler.scaleImage(icpcLogo, 250, 250);
			g.drawImage(tempImg, DESKTOP.width - tempImg.getWidth() - HD_MARGIN,
					HD_MARGIN + HD_TOP + 125 - tempImg.getHeight() / 2, null);
			tempImg.flush();
		}

		g.setColor(Color.WHITE);
		if (name != null) {
			String[] s = splitString(g, name, DESKTOP.width - HD_MARGIN * 2);
			for (int i = 0; i < s.length; i++) {
				g.drawString(s[i], (DESKTOP.width - fm.stringWidth(s[i])) / 2,
						DESKTOP.height - fm.getDescent() - HD_MARGIN - (s.length - i - 1) * fm.getHeight());
			}
		}

		g.dispose();

		final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
		FileImageOutputStream fout = new FileImageOutputStream(file);
		writer.setOutput(fout);

		JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
		jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpegParams.setCompressionQuality(0.975f);
		writer.write(null, new IIOImage(newImg, null, null), jpegParams);
		fout.close();

		// ImageIO.write(newImg, "jpg", hdFile);
	}

	protected static String[] splitString(Graphics g, String str, int width) {
		if (str == null)
			return new String[0];

		String s = str;
		FontMetrics fm = g.getFontMetrics();
		List<String> list = new ArrayList<>();

		while (fm.stringWidth(s) > width) {
			// find spot
			int x = s.length() - 1;
			while (x > 0 && fm.stringWidth(s.substring(0, x)) > width)
				x--;

			if (x == 0) // too narrow, can't even crop a char!
				return new String[] { s };

			// try to find space a few chars back
			int y = x;
			while (y > x * 0.6f && s.charAt(y) != ' ')
				y--;

			// otherwise crop anyway
			if (s.charAt(y) != ' ') {
				list.add(s.substring(0, x));
				s = "-" + s.substring(x);
			} else {
				list.add(s.substring(0, y));
				s = s.substring(y + 1);
			}
		}
		list.add(s);
		return list.toArray(new String[0]);
	}

	private static void writeImage(String prefix, BufferedImage img, File folder, long mod, boolean includeSize,
			String preferredExtension) throws IOException {
		String extension = img.getTransparency() != Transparency.OPAQUE ? "png" : preferredExtension;
		String filename = prefix + "." + extension;

		if (includeSize) {
			int ind = filename.lastIndexOf(".");
			filename = filename.substring(0, ind) + "." + img.getWidth() + "x" + img.getHeight() + filename.substring(ind);
		}

		File file = new File(folder, filename);
		if (file.exists())
			return;

		ImageIO.write(img, extension, file);
		file.setLastModified(mod);
	}

	private static boolean checkForTransparency(BufferedImage img) {
		return img.getTransparency() != Transparency.TRANSLUCENT && img.getTransparency() != Transparency.BITMASK;
	}

	private static boolean checkForSmallSize(BufferedImage img) {
		return img.getWidth() < MIN_SIZE || img.getHeight() < MIN_SIZE;
	}

	private static boolean checkForBadRatio(BufferedImage img) {
		double scale = (double) img.getWidth() / (double) img.getHeight();
		return (scale < 0.333 || scale > 3.0);
	}

	private static int checkForWarnings(String name, BufferedImage img, int numWarnings, boolean transparency) {
		int warn = numWarnings;
		if (transparency && checkForTransparency(img)) {
			warn(name, "no transparency");
			warn++;
		}

		if (checkForSmallSize(img)) {
			warn(name, "small image (" + img.getWidth() + "x" + img.getHeight() + ")");
			warn++;
		}

		if (checkForBadRatio(img))  {
			warn(name, "bad aspect ratio (" + img.getWidth() + "x" + img.getHeight() + ")");
			warn++;
		}
		return warn;
	}

	protected static void warn(String name, String s) {
		Trace.trace(Trace.ERROR, "Warning: " + s + " (" + name + ")");
	}

	private void init() {
		DiskContestSource source = new DiskContestSource(contestRoot);
		contest = source.loadContest(null);
		source.waitForContest(5000);

		try {
			InputStream in = getClass().getClassLoader().getResourceAsStream("font/HELV.PFB");
			masterFont = Font.createFont(Font.TYPE1_FONT, in);
			in.close();
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not load font", e);
		}

		fonts = getFonts(masterFont);

		try {
			icpcLogo = ImageIO.read(getClass().getClassLoader().getResource("images/icpc-logo.png"));
			File logoFile = new File(contestRoot, "contest/logo.png");
			if (!logoFile.exists())
				logoFile = new File(contestRoot, "config/logo.png");
			logo = ImageIO.read(logoFile);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not load logo image", e);
		}
	}

	private void reload() {
		DiskContestSource source = new DiskContestSource(contestRoot);
		contest = source.loadContest(null);
		source.waitForContest(5000);
	}

	private static Font[] getFonts(Font masterFont) {
		Font font1 = masterFont.deriveFont(Font.BOLD, 65f);
		Font font2 = masterFont.deriveFont(Font.BOLD, 67.5f);

		return new Font[] { masterFont, font1, font2 };
	}

	private static void createOverlay(BufferedImage img, String name, Font[] fonts, File file) throws IOException {
		BufferedImage newImg = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) newImg.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

		// new boxes
		if (DEBUG) {
			g.setColor(Color.DARK_GRAY);
			g.drawRect(LOGO.x, LOGO.y, LOGO.width, LOGO.height);
			g.drawRect(TEXT.x, TEXT.y, TEXT.width, TEXT.height);
		}

		if (img != null) {
			BufferedImage tempImg = ImageScaler.scaleImage(img, LOGO.width, LOGO.height);
			g.drawImage(tempImg, LOGO.x + (LOGO.width - tempImg.getWidth()) / 2,
					LOGO.y + (LOGO.height - tempImg.getHeight()) / 2, null);
		}

		int height = 180;
		float fontHeight = height * 72f / 96f;

		if (DEBUG) {
			Font font1 = fonts[0].deriveFont(Font.BOLD, (int) (fontHeight * 0.1f));
			g.setFont(font1);
			FontMetrics fm1 = g.getFontMetrics();

			String s = LOGO.width + " x " + LOGO.height;
			g.drawString(s, LOGO.x + 5, LOGO.y + fm1.getAscent() + 5);
			s = TEXT.width + " x " + TEXT.height;
			g.drawString(s, TEXT.x + 5, TEXT.y + fm1.getAscent() + 5);
		}

		g.setColor(Color.BLACK);
		g.setFont(fonts[1]);
		FontMetrics fm = g.getFontMetrics();

		String[] s = splitString(g, name, TEXT.width);
		int y = TEXT.y + (TEXT.height + fm.getAscent()) / 2 - (s.length - 1) * fm.getHeight() / 2;
		for (int i = 0; i < s.length; i++) {
			g.drawString(s[i], TEXT.x, y + fm.getHeight() * i);
		}

		g.dispose();
		ImageIO.write(newImg, "png", file);
	}

	protected void createRibbon() throws IOException {
		IOrganization[] organizations = contest.getOrganizations();
		int numOrganizations = organizations.length;
		if (numOrganizations == 0)
			return;

		int teamGap = 6;
		BufferedImage img = new BufferedImage(numOrganizations * 48 + 12, 24, BufferedImage.TYPE_INT_BGR);

		int gap = 4;
		Graphics2D g = (Graphics2D) img.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, numOrganizations * 48 + 12, 24);
		g.setColor(new Color(200, 200, 200));
		Font font = masterFont.deriveFont(Font.BOLD, 12f);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		int baseline = 12 + fm.getAscent() / 2;
		int x = teamGap;
		for (int i = 0; i < numOrganizations; i++) {
			String s = (i + 1) + "";
			g.drawString(s, x, baseline);
			x += fm.stringWidth(s) + gap;

			BufferedImage bImg = organizations[i].getLogoImage(24, 24, true, true);
			if (bImg != null)
				g.drawImage(bImg, x, 12 - bImg.getHeight() / 2, null);

			x += 24 + teamGap / 2;

			g.drawLine(x, 3, x, 20);
			x += teamGap / 2;
		}

		/*int dx = (6250 - x) / 3;
		BufferedImage bImg = ImageScaler.scaleImage(logo2, 240, 24);
		g.drawImage(bImg, x + dx - bImg.getWidth() / 2, 0, null);
		bImg = ImageScaler.scaleImage(sponsor, 240, 24);
		g.drawImage(bImg, x + dx * 2 - bImg.getWidth() / 2, 0, null);*/

		g.dispose();

		File file = new File(contestRoot, "images" + File.separator + "ribbon.jpg");

		// ImageIO.write(img, "jpg", file);
		final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
		FileImageOutputStream fout = new FileImageOutputStream(file);
		writer.setOutput(fout);

		JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
		jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpegParams.setCompressionQuality(0.975f);
		writer.write(null, new IIOImage(img, null, null), jpegParams);
		fout.close();
	}

	private void createPreview() throws IOException {
		int sq = 200;
		IOrganization[] organizations = contest.getOrganizations();
		Arrays.sort(organizations, new Comparator<IOrganization>() {
			@Override
			public int compare(IOrganization o1, IOrganization o2) {
				try {
					Integer i1 = Integer.parseInt(o1.getId());
					Integer i2 = Integer.parseInt(o2.getId());
					return Integer.compare(i1, i2);
				} catch (Exception e) {
					// ignore
				}
				return o1.getId().compareTo(o2.getId());
			}
		});
		int numOrganizations = organizations.length;
		if (numOrganizations == 0)
			return;

		int pad = 3;
		int th = 25;
		int w = (sq + pad * 2) * numOrganizations;
		int h = (sq + pad * 2) * 3 + th * 2;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);

		Graphics2D g = (Graphics2D) img.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, w, sq + pad * 2 + th * 2);
		g.setColor(Color.GRAY);
		g.fillRect(0, th + sq + pad * 2, w, sq + pad * 2);
		g.setColor(Color.WHITE);
		g.fillRect(0, th + sq * 2 + pad * 4, w, sq + pad * 2);
		g.setColor(new Color(200, 200, 200));
		Font font = masterFont.deriveFont(Font.BOLD, 16f);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		int baseline = (th + fm.getAscent()) / 2;
		int baseline2 = (th + fm.getAscent()) / 2 + (sq + pad * 2) * 3 + th;
		int x = 0;
		for (int i = 0; i < numOrganizations; i++) {
			String s = (i + 1) + "";
			x += pad;
			g.drawString(s, x + (sq - fm.stringWidth(s)) / 2, baseline);
			s = organizations[i].getId();
			g.drawString(s, x + (sq - fm.stringWidth(s)) / 2, baseline2);

			BufferedImage logoImg = organizations[i].getLogoImage(sq, sq, true, true);
			if (logoImg != null) {
				for (int j = 0; j < 3; j++)
					g.drawImage(logoImg, x + (sq - logoImg.getWidth()) / 2,
							th + pad + (sq + pad * 2) * j + (sq - logoImg.getHeight()) / 2, null);
			}
			x += pad + sq;
		}

		g.dispose();

		File file = new File(contestRoot, "images" + File.separator + "preview.png");

		ImageIO.write(img, "png", file);
	}

	private void createContestPreview() throws IOException {
		int sq = 300;

		BufferedImage lImg = contest.getLogoImage(sq * 5, sq, true, true);
		BufferedImage bImg = contest.getBannerImage(sq * 5, sq, true, true);

		int pad = 25;
		int w = (pad * 3);
		if (lImg != null)
			w += lImg.getWidth();
		if (bImg != null)
			w += bImg.getWidth();
		int h = (sq + pad * 2) * 3;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);

		Graphics2D g = (Graphics2D) img.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, w, sq + pad * 2);
		g.setColor(Color.GRAY);
		g.fillRect(0, sq + pad * 2, w, sq + pad * 2);
		g.setColor(Color.WHITE);
		g.fillRect(0, sq * 2 + pad * 4, w, sq + pad * 2);

		int xx = 0;
		if (lImg != null) {
			for (int j = 0; j < 3; j++)
				g.drawImage(lImg, pad, pad + (sq + pad * 2) * j + (sq - lImg.getHeight()) / 2, null);
			xx += lImg.getWidth();
		}

		if (bImg != null)
			for (int j = 0; j < 3; j++)
				g.drawImage(bImg, xx + pad * 2, pad + (sq + pad * 2) * j + (sq - bImg.getHeight()) / 2, null);

		g.dispose();

		File file = new File(contestRoot, "images" + File.separator + "contest-preview.png");

		ImageIO.write(img, "png", file);
	}

	private void missingDataReport() {
		// TODO include logo status - e.g. too small, missing, etc

		int count = 0;
		File f = new File(contestRoot + File.separator + "missing-data.tsv");

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));

			String[] h = new String[] { "Id", "Name", "Formal name", "Logo", "Location", "Twitter hashtag",
					"Twitter account" };
			bw.write(String.join("\t", h));
			bw.newLine();

			for (IOrganization org : contest.getOrganizations()) {
				String[] s = new String[7];
				s[0] = org.getId();
				s[1] = org.getName();
				s[2] = org.getFormalName();

				if (org.getLogo() == null || org.getLogo().isEmpty()) {
					s[3] = "X";
				} else {
					s[3] = "";
					BufferedImage logo = ImageIO.read(org.getLogo().first().file);
					if (checkForTransparency(logo)) {
						s[3] += "T";
					}
					if (checkForSmallSize(logo)) {
						s[3] += "S";
					}
					if (checkForBadRatio(logo)) {
						s[3] += "R";
					}
					if (s[3].isEmpty()) {
						s[3] = null;
					}
				}

				if (Double.isNaN(org.getLongitude()) || Double.isNaN(org.getLatitude())) {
					s[4] = "X";
				}
				if (org.getTwitterHashtag() == null) {
					s[5] = "X";
				}
				if (org.getTwitterAccount() == null) {
					s[6] = "X";
				}

				if (s[3] != null || s[4] != null || s[5] != null || s[6] != null) {
					count++;
					for (int i = 3; i < s.length; i++) {
						if (s[i] == null)
							s[i] = "";
					}
					bw.write(String.join("\t", s));
					bw.newLine();
				}
			}

			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (count == 0)
			f.delete();
		else
			System.out.println(count + " organizations with missing data logged to missing-data.tsv");
	}
}