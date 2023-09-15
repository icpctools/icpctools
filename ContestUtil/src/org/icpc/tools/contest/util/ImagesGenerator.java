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
import java.io.File;
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
import org.icpc.tools.contest.model.IPerson;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.DiskContestSource;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.Organization;

/**
 * Converts logos and other images from raw source (typically from the CMS attachments) to usable
 * contest data.
 *
 * Arguments: cmsLocation contestRoot
 *
 * cmsLocation - the folder containing raw images (typically downloaded directly from CMS as
 * Attachments and unzipped)
 *
 * contestRoot - a contest location, i.e. CDP/CAF root folder
 */
public class ImagesGenerator {
	private static final String DEFAULT_NAME = "logo.png";
	private static final String DEFAULT_PHOTO_NAME = "photo.";
	private static final int MAX_LOGO_SIZE = 1080;
	private static final int MIN_SIZE = 250;
	private static final int HD_MARGIN = 15;
	private static final int HD_TOP = 30;
	private static final double FUDGE = 0.075; // 7.5%

	private static final String[] LOGO_EXTENSIONS = new String[] { "png", "svg", "jpg", "jpeg" };
	// private static final String[] PHOTO_EXTENSIONS = new String[] { "jpg", "jpeg", "png", "svg"
	// };

	private static final boolean DEBUG = false;
	private static final Rectangle LOGO = new Rectangle(95, 795, 230, 230);
	private static final Rectangle TEXT = new Rectangle(370, 795, 1275, 230);

	private static final Dimension DESKTOP = new Dimension(1920, 1080);
	private static final Dimension ICON = new Dimension(56, 56);
	private static final Dimension TILE = new Dimension(160, 160);

	private static BufferedImage icpcLogo, logo;

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
		// TODO generator.generateTeamPhotos();

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
	}

	protected ImagesGenerator(File contestRoot) {
		this.contestRoot = contestRoot;
		init();
	}

	public void generateTeamPhotos() {
		File sourceRoot = new File(contestRoot, "images" + File.separator + "teamSource");
		if (!sourceRoot.exists()) {
			Trace.trace(Trace.ERROR, "Couldn't find images/teamSource folder. Exiting");
			return;
		}

		ImageType[] types = new ImageType[] { new ImageType("teams") };
		File teamsFolder = new File(contestRoot, "teams");

		// generate files
		File[] files2 = sourceRoot.listFiles();
		for (File f : files2) {
			if (f.getName().startsWith("."))
				continue;
			File imgFile = f;

			int teamId = getTeamNum2(f.getName());
			String toFileName = teamId + File.separator + "photo.jpg";

			try {
				for (int i = 0; i < types.length; i++) {
					ImageType it = types[i];
					File file = new File(teamsFolder, toFileName);
					File folder = file.getParentFile();
					folder.mkdirs();
					it.files.add(file);
					if (file.exists()) {
						if (imgFile.lastModified() != file.lastModified())
							file.delete();
						else
							file = null;
					}

					if (file != null) {
						Trace.trace(Trace.USER, "Generating: " + file);
						BufferedImage img = ImageIO.read(imgFile);
						createTeam(img, file);
						file.setLastModified(imgFile.lastModified());
					}
				}
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error generating image", e);
			}
		}

		/*for (ImageType it : types) {
			cleanupFolder(it.folder, it.files);
		}*/

		// check output
		/*File[] files = teamsFolder.listFiles();
		List<Integer> list = new ArrayList<>();
		int max = 0;
		for (File f : files) {
			String s = f.getName().substring(0, f.getName().indexOf("."));
			int num = Integer.parseInt(s);
			max = Math.max(max, num);
			list.add(num);
		}
		Trace.trace(Trace.USER, list.size() + " images found for " + max + " teams");
		for (int i = 1; i <= max; i++) {
			if (!list.contains(i))
				Trace.trace(Trace.USER, "   Team " + i + " missing image");
		}*/
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
					File imgFile = new File(orgFolder, DEFAULT_NAME);
					if (imgFile.exists()) {
						mod = imgFile.lastModified();
						logoImg = ImageIO.read(imgFile);
					}
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

	public void generateOrganizationLogos() {
		File orgRootFolder = new File(contestRoot, "organizations");
		if (!orgRootFolder.exists()) {
			Trace.trace(Trace.ERROR, "Couldn't find /organizations folder. Exiting");
			return;
		}

		int numWarnings = 0;

		// generate files
		File[] folders = orgRootFolder.listFiles();
		for (File f : folders) {
			File imgFile = null;
			if (f.isDirectory()) {
				File[] subFolders = f.listFiles();
				boolean foundPattern = false;
				for (File ff : subFolders) {
					// skip generated files
					String name = ff.getName();
					if (name.startsWith("logo") && hasExtension(name, LOGO_EXTENSIONS)) {
						foundPattern = true;
						// skip over generated logos (should really use regex to look for logo.<w>x<h>.)
						if (name.startsWith("logo.") && name.contains("x"))
							continue;
					}

					imgFile = ff;

					// if this is the default name, use it
					if (DEFAULT_NAME.equals(name.toLowerCase()))
						break;
				}

				if (imgFile == null) {
					Trace.trace(Trace.ERROR, "Warning: no image found (" + f.getName() + ")");
					numWarnings++;
					continue;
				}

				try {
					String orgStr = imgFile.getParentFile().getName();
					Trace.trace(Trace.USER, "Updating logo for: " + orgStr);

					IOrganization org = contest.getOrganizationById(orgStr);
					if (org == null) {
						Trace.trace(Trace.WARNING, "Unknown organization: " + orgStr);
						Organization org2 = new Organization();
						org2.add("id", orgStr);
						org2.add("name", orgStr);
						contest.add(org2);
					}

					long mod = imgFile.lastModified();
					BufferedImage img = ImageIO.read(imgFile);
					if (img == null)
						continue;

					img = removeBorders(img);

					File orgFolder = new File(orgRootFolder, orgStr);
					if (!orgFolder.exists())
						orgFolder.mkdirs();
					else {
						// clean up old generated logos
						File[] files = orgFolder.listFiles();
						for (File ff : files) {
							if (ff.getName().startsWith("logo.") && hasExtension(ff.getName(), LOGO_EXTENSIONS)
									&& ff.lastModified() != mod)
								ff.delete();
						}
					}

					numWarnings = checkForWarnings(f.getName(), img, numWarnings);

					if (!foundPattern) {
						// there's no filename with the spec extension, create one
						File file = new File(orgFolder, DEFAULT_NAME);
						ImageIO.write(img, "png", file);
						file.setLastModified(mod);
					}

					// if the file dimensions are massive, start with a reasonably large one
					if (img.getWidth() > MAX_LOGO_SIZE || img.getHeight() > MAX_LOGO_SIZE) {
						BufferedImage scImg = ImageScaler.scaleImage(img, MAX_LOGO_SIZE, MAX_LOGO_SIZE);
						writeImageWithSize(scImg, orgFolder, mod);
					}

					BufferedImage scImg = ImageScaler.scaleImage(img, ICON.width, ICON.height);
					double aspect = scImg.getWidth() / (double) scImg.getHeight();
					if (aspect < 1.0 - FUDGE || aspect > 1.0 + FUDGE)
						writeImageWithSize(scImg, orgFolder, mod);

					scImg = ImageScaler.padImage(scImg);
					writeImageWithSize(scImg, orgFolder, mod);

					scImg = ImageScaler.scaleImage(img, TILE.width, TILE.height);
					aspect = scImg.getWidth() / (double) scImg.getHeight();
					if (aspect < 1.0 - FUDGE || aspect > 1.0 + FUDGE)
						writeImageWithSize(scImg, orgFolder, mod);

					scImg = ImageScaler.padImage(scImg);
					writeImageWithSize(scImg, orgFolder, mod);
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error generating image: " + imgFile.getAbsolutePath(), e);
				}
			}
		}

		Trace.trace(Trace.USER, numWarnings + " warnings out of " + contest.getNumOrganizations());
	}

	public void generatePersonPhotos() {
		File personRootFolder = new File(contestRoot, "persons-todo");
		if (!personRootFolder.exists()) {
			Trace.trace(Trace.ERROR, "Couldn't find /persons folder. Exiting");
			return;
		}

		int numWarnings = 0;

		// generate files
		File[] imageFiles = personRootFolder.listFiles();
		for (File imgFile : imageFiles) {
			try {
				String personStr = imgFile.getName();
				Trace.trace(Trace.USER, "Updating photo for: " + personStr);

				IPerson person = null;
				String pn = imgFile.getName().toLowerCase();
				for (IPerson p : contest.getPersons()) {
					int count = 0;
					String[] s = p.getName().split(" ");
					for (String ss : s)
						if (pn.contains(ss.toLowerCase()))
							count++;
					if (pn.contains(p.getRole().toLowerCase()))
						count++;
					if (count == s.length + 1)
						person = p;
				}

				if (person == null) {
					Trace.trace(Trace.USER, "  No match " + contest.getNumPersons());
					continue;
				}

				long mod = imgFile.lastModified();
				BufferedImage img = ImageIO.read(imgFile);
				if (img == null)
					continue;

				img = removeBorders(img);

				File personFolder = new File(personRootFolder, person.getId());
				if (!personFolder.exists())
					personFolder.mkdirs();
				else {
					// clean up old photos
					File[] files = personFolder.listFiles();
					for (File ff : files) {
						if (ff.getName().startsWith("photo") && ff.getName().endsWith(".png") && ff.lastModified() != mod)
							ff.delete();
					}
				}

				boolean hasAlpha = img.getTransparency() != Transparency.OPAQUE;
				File file = new File(personFolder, DEFAULT_PHOTO_NAME + "jpg");
				if (hasAlpha)
					file = new File(personFolder, DEFAULT_PHOTO_NAME + "png");
				if (!file.exists()) {
					BufferedImage scImg = img;
					if (img.getWidth() > MAX_LOGO_SIZE || img.getHeight() > MAX_LOGO_SIZE)
						scImg = ImageScaler.scaleImage(img, MAX_LOGO_SIZE, MAX_LOGO_SIZE);
					if (hasAlpha)
						ImageIO.write(scImg, "png", file);
					else
						ImageIO.write(scImg, "jpg", file);
					file.setLastModified(mod);
				}

				if (img.getWidth() > TILE.width || img.getHeight() > TILE.height) {
					BufferedImage scImg = ImageScaler.scaleImage(img, TILE.width, TILE.height);
					writePhotoWithSize(scImg, personFolder, mod);
				}
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error generating image: " + imgFile.getAbsolutePath(), e);
			}
		}

		Trace.trace(Trace.USER, numWarnings + " warnings out of " + contest.getNumPersons());
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

	private static String getFileName(BufferedImage img) {
		String name = DEFAULT_NAME;
		int ind = name.lastIndexOf(".");
		return name.substring(0, ind) + "." + img.getWidth() + "x" + img.getHeight() + name.substring(ind);
	}

	private static String getPhotoFileName(BufferedImage img) {
		String name = DEFAULT_PHOTO_NAME;
		int ind = name.lastIndexOf(".");
		return name.substring(0, ind) + "." + img.getWidth() + "x" + img.getHeight() + name.substring(ind);
	}

	private static void writeImageWithSize(BufferedImage scImg, File folder, long mod) throws IOException {
		File file = new File(folder, getFileName(scImg));
		if (file.exists())
			return;
		ImageIO.write(scImg, "png", file);
		file.setLastModified(mod);
	}

	private static void writePhotoWithSize(BufferedImage scImg, File folder, long mod) throws IOException {
		boolean hasAlpha = scImg.getTransparency() != Transparency.OPAQUE;
		File file = new File(folder, getPhotoFileName(scImg) + "jpg");
		if (hasAlpha)
			file = new File(folder, getPhotoFileName(scImg) + "png");
		if (file.exists())
			return;

		if (hasAlpha)
			ImageIO.write(scImg, "png", file);
		else
			ImageIO.write(scImg, "jpg", file);
		file.setLastModified(mod);
	}

	private static int checkForWarnings(String name, BufferedImage img, int numWarnings) {
		int warn = numWarnings;
		if ((img.getTransparency() != Transparency.TRANSLUCENT && img.getTransparency() != Transparency.BITMASK)) {
			warn(name, "no transparency");
			warn++;
		}

		if (img.getWidth() < MIN_SIZE || img.getHeight() < MIN_SIZE) {
			warn(name, "small image (" + img.getWidth() + "x" + img.getHeight() + ")");
			warn++;
		}

		double scale = (double) img.getWidth() / (double) img.getHeight();
		if (scale < 0.333 || scale > 3.0) {
			warn(name, "bad aspect ratio (" + img.getWidth() + "x" + img.getHeight() + ")");
			warn++;
		}
		return warn;
	}

	private static void createTeam(BufferedImage img, File file) throws IOException {
		ImageIO.write(ImageScaler.scaleImage(img, 1920, 1080), "jpg", file);
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
			File logoFile = new File(contestRoot, "config/logo.png");
			logo = ImageIO.read(logoFile);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not load id image", e);
		}
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

	private static int getTeamNum2(String s) {
		StringBuilder sb = new StringBuilder();
		int len = s.length();
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			if (Character.isDigit(c))
				sb.append(c);
		}

		return Integer.parseInt(sb.toString());
	}

	protected void createRibbon() throws IOException {
		IOrganization[] orgs = contest.getOrganizations();
		int numOrgs = orgs.length;
		if (numOrgs == 0)
			return;

		int teamGap = 6;
		BufferedImage img = new BufferedImage(numOrgs * 48 + 12, 24, BufferedImage.TYPE_INT_BGR);

		int gap = 4;
		Graphics2D g = (Graphics2D) img.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, numOrgs * 48 + 12, 24);
		g.setColor(new Color(200, 200, 200));
		Font font = masterFont.deriveFont(Font.BOLD, 12f);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		int baseline = 12 + fm.getAscent() / 2;
		int x = teamGap;
		for (int i = 0; i < numOrgs; i++) {
			String s = (i + 1) + "";
			g.drawString(s, x, baseline);
			x += fm.stringWidth(s) + gap;
			File logoFile = new File(contestRoot,
					"organizations" + File.separator + orgs[i].getId() + File.separator + DEFAULT_NAME);

			if (logoFile.exists()) {
				BufferedImage logoImg = ImageIO.read(logoFile);
				BufferedImage bImg = ImageScaler.scaleImage(logoImg, 24, 24);
				g.drawImage(bImg, x, 12 - bImg.getHeight() / 2, null);
			}
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
		IOrganization[] orgs = contest.getOrganizations();
		Arrays.sort(orgs, new Comparator<IOrganization>() {
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
		int numOrgs = orgs.length;
		if (numOrgs == 0)
			return;

		int pad = 3;
		int th = 25;
		int w = (sq + pad * 2) * numOrgs;
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
		for (int i = 0; i < numOrgs; i++) {
			String s = (i + 1) + "";
			x += pad;
			g.drawString(s, x + (sq - fm.stringWidth(s)) / 2, baseline);
			s = orgs[i].getId();
			g.drawString(s, x + (sq - fm.stringWidth(s)) / 2, baseline2);
			// File logoFile = new File(contestRoot, "images" + File.separator + "logo" +
			// File.separator + i + ".png");
			File logoFile = new File(contestRoot,
					"organizations" + File.separator + orgs[i].getId() + File.separator + DEFAULT_NAME);
			if (logoFile.exists()) {
				BufferedImage logoImg = ImageIO.read(logoFile);
				BufferedImage bImg = ImageScaler.scaleImage(logoImg, sq, sq);
				for (int j = 0; j < 3; j++)
					g.drawImage(bImg, x + (sq - bImg.getWidth()) / 2,
							th + pad + (sq + pad * 2) * j + (sq - bImg.getHeight()) / 2, null);
			}
			// g.drawLine(x, 3, x, 20);
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
}