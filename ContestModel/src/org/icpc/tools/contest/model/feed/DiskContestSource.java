package org.icpc.tools.contest.model.feed;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObject.ContestType;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.TSVImporter;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.icpc.tools.contest.model.internal.FileReference;
import org.icpc.tools.contest.model.internal.FileReferenceList;
import org.icpc.tools.contest.model.internal.Group;
import org.icpc.tools.contest.model.internal.IContestModifier;
import org.icpc.tools.contest.model.internal.Info;
import org.icpc.tools.contest.model.internal.Organization;
import org.icpc.tools.contest.model.internal.Submission;
import org.icpc.tools.contest.model.internal.Team;
import org.icpc.tools.contest.model.internal.TeamMember;
import org.icpc.tools.contest.model.internal.YamlParser;

/**
 * A contest source that is backed by either a Contest Archive Format (CAF) exploded on disk or an
 * event feed (JSON or XML). A cache folder in temp is used to store metadata to improve
 * performance.
 */
public class DiskContestSource extends ContestSource {
	private static final String CACHE_PREFIX = "org.icpc.tools.cache.";
	private static final String CACHE_FILE = ".cache";
	private static final String CACHE_VERSION = "ICPC Tools Cache v1.0";

	private static final String LOGO = "logo";
	private static final String PHOTO = "photo";
	private static final String VIDEO = "video";
	private static final String BANNER = "banner";
	private static final String BACKUP = "backup";
	private static final String KEY_LOG = "key_log";
	private static final String TOOL_DATA = "tool_data";
	private static final String FILES = "files";
	private static final String REACTION = "reaction";
	private static final String COUNTRY_FLAG = "country_flag";

	private static final String[] LOGO_EXTENSIONS = new String[] { "png", "svg", "jpg", "jpeg" };
	private static final String[] PHOTO_EXTENSIONS = new String[] { "jpg", "jpeg", "png", "svg" };

	private File eventFeedFile;
	private File root;
	protected File cacheFolder;
	private String contestId;
	private Closeable parser;
	private Validation configValidation = new Validation();
	private Map<String, List<FileReference>> cache = new HashMap<>();

	static class FilePattern {
		// the folder containing the file
		protected String folder;

		// the file name, without extension
		protected String name; // e.g. "logo" or "files"

		// the file extensions
		protected String[] extensions; // e.g. "jpg" or "zip"

		// the partial url to access the file
		protected String url;

		public FilePattern(IContestObject.ContestType type, String id, String property, String folder,
				String fileExtension) {
			this(type, id, property, folder, new String[] { fileExtension });
		}

		public FilePattern(IContestObject.ContestType type, String id, String property, String folder,
				String[] fileExtensions) {
			if (type == null) {
				this.folder = folder;
				this.url = property;
			} else {
				String typeName = IContestObject.getTypeName(type);
				this.folder = folder + File.separator + typeName + File.separator + id;
				this.url = typeName + "/" + id + "/" + property;
			}

			this.name = property;
			this.extensions = fileExtensions;
		}
	}

	/**
	 * Create a disk contest source reading from the contest archive format (CAF).
	 *
	 * @param folder - a contest archive folder
	 */
	public DiskContestSource(File folder) {
		this(null, folder, folder.getAbsolutePath());
	}

	/**
	 * Create a disk contest source reading from a JSON or XML event feed.
	 *
	 * @param eventFeedFile - a JSON or XML event feed file
	 */
	public DiskContestSource(String eventFeedFile) {
		this(new File(eventFeedFile), null, eventFeedFile);
	}

	/**
	 * General constructor for a disk contest source. Typically, only one of the event feed file and
	 * CAF folder are provided. A hash must be provided to seed the temporary caching folder name
	 * (so that it is consistent across restarts).
	 *
	 * @param eventFeedFile - a JSON or XML event feed file
	 * @param folder - a contest archive folder
	 * @param hash - any uid or hash that's consistent across restarts
	 */
	protected DiskContestSource(File eventFeedFile, File folder, String hash) {
		this.eventFeedFile = eventFeedFile;
		root = folder;
		if (folder != null)
			cacheFolder = createTempDir(folder.getName() + "-" + getSafeHash(folder.getAbsolutePath()));
		else {
			cacheFolder = createTempDir(getSafeHash(hash));
			root = cacheFolder;
		}

		cleanUpTempDir();

		if (eventFeedFile != null && !eventFeedFile.exists()) {
			throw new IllegalArgumentException(
					"Event feed (" + root.toString() + ") must be valid JSON or XML event feed.");
		}

		if (root != null && !root.exists()) {
			if (!root.mkdirs()) {
				throw new IllegalArgumentException(
						"Contest location (" + root.toString() + ") did not exist and directory creation failed.");
			}
		}

		instance = this;
	}

	public static Contest loadContest(File file, IContestListener listener) throws Exception {
		DiskContestSource source = new DiskContestSource(file, null, file.getAbsolutePath());
		return source.loadContest(listener);
	}

	public File getRootFolder() {
		return root;
	}

	@Override
	public String getContestId() {
		return contestId;
	}

	public void setContestId(String contestId) {
		this.contestId = contestId;
	}

	private static String getSafeHash(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (Character.isAlphabetic(c) || Character.isDigit(c))
				sb.append(c);
		}
		return sb.toString();
	}

	public boolean isCache() {
		return root != null;
	}

	private void cleanUpTempDir() {
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		File[] files = tempDir.listFiles((dir, name) -> name != null && name.startsWith(CACHE_PREFIX));

		if (files == null)
			return;

		for (File f : files) {
			if (f.isDirectory() && !f.equals(cacheFolder)) {
				// delete cached files older than 10 days
				long tenDaysInMillis = 10 * 24 * 3600 * 1000;
				if (f.lastModified() < System.currentTimeMillis() - tenDaysInMillis)
					deleteDirectory(f);
			}
		}
	}

	/**
	 * Utility method to recursively delete a directory.
	 *
	 * @param dir a directory
	 */
	public static boolean deleteDirectory(File dir) {
		if (!dir.exists() || !dir.isDirectory())
			return false;

		try {
			File[] files = dir.listFiles();
			// cycle through files
			boolean deleteCurrent = true;
			for (File current : files) {
				if (current.isFile()) {
					if (!current.delete())
						deleteCurrent = false;
				} else if (current.isDirectory()) {
					if (!deleteDirectory(current))
						deleteCurrent = false;
				}
			}
			return !deleteCurrent || dir.delete();
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error deleting directory " + dir.getAbsolutePath(), e);
		}

		return false;
	}

	private static File createTempDir(String id) {
		File tempDir = new File(System.getProperty("java.io.tmpdir"), CACHE_PREFIX + id);
		if (!tempDir.exists() && !tempDir.mkdir())
			return null;

		return tempDir;
	}

	private int getDiskHash() {
		File hashFile = new File(cacheFolder, "hash.txt");
		if (hashFile.exists()) {
			try (BufferedReader br = new BufferedReader(new FileReader(hashFile))) {
				String s = br.readLine();
				Trace.trace(Trace.INFO, "Contest hash found: " + s);
				return Integer.parseInt(s);
			} catch (Exception e) {
				Trace.trace(Trace.WARNING, "Couldn't read contest hash file", e);
			}
			// ignore
		}
		if (root == null)
			return 0;
		File configFolder = new File(root, "config");
		File[] files = configFolder.listFiles();
		if (files == null)
			return 0;

		int hash = 0;
		for (File f : files)
			hash += (int) f.lastModified();

		return hash;
	}

	@Override
	public File getFile(IContestObject obj, FileReference ref, String property) throws IOException {
		if (obj == null)
			return null;

		if (ref.file != null)
			return ref.file;

		// use the pattern to find the right folder
		FilePattern pattern = getLocalPattern(obj.getType(), obj.getId(), property);

		File folder = root;
		if (pattern.folder != null)
			folder = new File(root, pattern.folder);

		// check if any existing files in the folder match
		synchronized (cache) {
			List<FileReference> list = getCache(folder);
			for (FileReference ref2 : list) {
				if (ref2.href != null && ref2.href.endsWith(ref.href)) {
					if (ref.width <= 0 || ref.width == ref2.width)
						return ref2.file;
				}
			}
		}

		// otherwise, assume the default file name or try a new one if that is taken
		return getNewFile(obj.getType(), obj.getId(), property, ref.mime);
	}

	/**
	 * Return a new local file name that is suitable for saving the given property of a contest
	 * object of a specific type and id to.
	 */
	public File getNewFile(IContestObject.ContestType type, String id, String property, String mimeType) {
		FilePattern pattern = getLocalPattern(type, id, property);
		if (pattern == null)
			return null;

		File folder = root;
		if (pattern.folder != null)
			folder = new File(root, pattern.folder);

		String ext = getExtension(mimeType);
		if (ext == null) // couldn't recognize mime type, so assume the default file extension
			ext = pattern.extensions[0];
		ext = "." + ext;

		File file = new File(folder, pattern.name + ext);
		int n = 2;
		while (file.exists()) {
			file = new File(folder, pattern.name + n + ext);
			n++;
		}
		return file;
	}

	@Override
	public File getFile(String path) throws IOException {
		String path2 = path;
		if (path.endsWith("/"))
			path2 += "listing.dir";

		if (root != null)
			return new File(root, path2);

		return new File(cacheFolder, path2);
	}

	@Override
	public String[] getDirectory(String path) throws IOException {
		File f = null;
		if (root != null)
			f = new File(root, path);
		else
			f = new File(cacheFolder, path);

		if (!f.isDirectory())
			return null;

		File[] files = f.listFiles();
		List<String> list = new ArrayList<>();
		for (File ff : files) {
			if (ff.isFile())
				list.add(ff.getName());
		}

		return list.toArray(new String[0]);
	}

	/**
	 * Set the remote info on a file so we know where it came from.
	 */
	protected void updateFileInfo(File file, String href, String etag) {
		if (etag == null && href == null)
			return;

		File folder = file.getParentFile();
		synchronized (cache) {
			List<FileReference> list = getCache(folder);
			for (FileReference ref : list) {
				if (ref.file.equals(file)) {
					if (etag != null && etag.equals(ref.etag))
						return;

					ref.etag = etag;
					ref.href = href;
					ref.lastModified = file.lastModified();
					try {
						FileImageInputStream fin = new FileImageInputStream(file);
						Iterator<ImageReader> iter = ImageIO.getImageReaders(fin);
						while (iter.hasNext()) {
							ImageReader ir = iter.next();
							ir.setInput(fin);
							ref.width = ir.getWidth(0);
							ref.height = ir.getHeight(0);
						}
					} catch (Exception e) {
						// ignore
					}

					writeCache(folder, list);
					return;
				}
			}

			// not found, new file
			FileReference ref = new FileReference();
			ref.etag = etag;
			ref.href = href;
			ref.file = file;
			ref.mime = getMimeType(file.getName());
			ref.lastModified = file.lastModified();
			try {
				FileImageInputStream fin = new FileImageInputStream(file);
				Iterator<ImageReader> iter = ImageIO.getImageReaders(fin);
				while (iter.hasNext()) {
					ImageReader ir = iter.next();
					ir.setInput(fin);
					ref.width = ir.getWidth(0);
					ref.height = ir.getHeight(0);
				}
			} catch (Exception e) {
				// ignore
			}
			list.add(ref);
			writeCache(folder, list);
		}
	}

	protected FileReference getFileRef(File file) {
		File folder = file.getParentFile();
		synchronized (cache) {
			List<FileReference> list = getCache(folder);
			for (FileReference ref : list) {
				if (ref.file.equals(file)) {
					return ref;
				}
			}
		}
		Trace.trace(Trace.WARNING, "No file in folder matched");
		return null;
	}

	private File getCacheForFolder(File folder) {
		// if we're already using a temp folder, just put the cache file in the folder it's
		// associated with
		if (cacheFolder == null)
			return new File(folder, CACHE_FILE);

		// otherwise, put all files in the same temp folder and name them based on subpath
		String s = folder.getAbsolutePath().substring(root.getAbsolutePath().length() + 1); // TODO
		s = s.replace("/", "-").replace("\\", "-");

		return new File(cacheFolder, s + ".cache");
	}

	private List<FileReference> readCache(File folder) throws IOException {
		List<FileReference> list = new ArrayList<>();
		BufferedReader br = null;

		String s = null;
		try {
			File f = getCacheForFolder(folder);
			if (!f.exists())
				return list;

			br = new BufferedReader(new FileReader(f));
			s = br.readLine();
			if (!CACHE_VERSION.equals(s))
				return list;

			s = br.readLine();
			while (s != null) {
				String[] st = s.split("\\t");
				FileReference ref = new FileReference();
				String name = st[0];
				if (name != null && name.length() > 0)
					ref.file = new File(folder, name);
				if (st[1] != null && !st[1].isEmpty())
					ref.href = st[1];
				ref.mime = st[2];
				ref.lastModified = Long.parseLong(st[3]);
				if (st[4] != null && !st[4].isEmpty())
					ref.etag = st[4];
				ref.width = Integer.parseInt(st[5]);
				ref.height = Integer.parseInt(st[6]);
				list.add(ref);

				s = br.readLine();
			}
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Couldn't read cache for " + folder.getAbsolutePath() + " (" + s + ")", e);
			throw e;
		} finally {
			try {
				br.close();
			} catch (Exception e) {
				// ignore
			}
		}
		return list;
	}

	private List<FileReference> getCache(File folder) {
		List<FileReference> list = cache.get(folder.getAbsolutePath());
		if (list != null)
			return list;

		try {
			list = readCache(folder);
		} catch (Exception e) {
			list = new ArrayList<>(5);
		}

		// verify cache
		List<FileReference> currentList = new ArrayList<>();
		try {
			File[] files = folder.listFiles();
			if (files == null || files.length == 0) {
				cache.put(folder.getAbsolutePath(), currentList);
				return currentList;
			}

			boolean diff = false;
			for (File f : files) {
				if (f == null || f.isDirectory() || f.getName().startsWith("."))
					continue;

				boolean found = false;
				for (FileReference ref : list) {
					if (f.equals(ref.file)) {
						if (ref.lastModified == f.lastModified()) {
							found = true;
							currentList.add(ref);
						}
					}
				}

				if (!found) {
					currentList.add(readMetadata(f));
					diff = true;
				}
			}
			if (diff || list.size() != currentList.size())
				writeCache(folder, currentList);

			cache.put(folder.getAbsolutePath(), currentList);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error getting file ref", e);
		}
		return currentList;
	}

	private void writeCache(File folder, List<FileReference> list) {
		BufferedWriter bw = null;
		FileOutputStream fout = null;

		try {
			File f = getCacheForFolder(folder);
			fout = new FileOutputStream(f);

			bw = new BufferedWriter(new OutputStreamWriter(fout));
			bw.write(CACHE_VERSION);
			bw.newLine();

			for (FileReference ref : list) {
				String name = "";
				if (ref.file != null)
					name = ref.file.getName();
				String href = "";
				if (ref.href != null)
					href = ref.href;
				String etag = "";
				if (ref.etag != null)
					etag = ref.etag;
				bw.write(String.join("\t", name, href, ref.mime, ref.lastModified + "", etag, ref.width + "",
						ref.height + ""));
				bw.newLine();
			}
		} catch (Exception e) {
			// ignore
		} finally {
			try {
				bw.close();
			} catch (Exception ex) {
				// ignore
			}
			try {
				fout.getFD().sync();
			} catch (Exception ex) {
				// ignore
			}
		}
	}

	protected FileReference getMetadata(String href, File file) {
		FileReference ref = getFileRef(file);
		if (ref == null)
			Trace.trace(Trace.ERROR, "Null file ref! " + href + " - " + file + " - " + file.exists());
		else
			ref.href = "contests/" + contestId + "/" + href;
		return ref;
	}

	private static String getMimeType(String name) {
		if (name.endsWith(".zip"))
			return "application/zip";
		else if (name.endsWith(".png"))
			return "image/png";
		else if (name.endsWith(".jpg"))
			return "image/jpg";
		else if (name.endsWith(".svg"))
			return "image/svg+xml";
		else if (name.endsWith(".m2ts"))
			return "video/MP2T";
		else if (name.endsWith(".txt"))
			return "text/plain";
		else if (name.endsWith(".log"))
			return "text/plain";
		return null;
	}

	private static String getExtension(String mimeType) {
		if (mimeType == null)
			return null;

		if (mimeType.equals("application/zip"))
			return "zip";
		else if (mimeType.equals("image/png"))
			return "png";
		else if (mimeType.equals("image/jpg"))
			return "jpg";
		else if (mimeType.equals("image/svg+xml"))
			return "svg";
		else if (mimeType.equals("video/MP2T"))
			return "m2ts";
		else if (mimeType.equals("text/plain"))
			return "txt";
		return null;
	}

	protected static FileReference readMetadata(File file) {
		FileReference ref = new FileReference();
		ref.mime = getMimeType(file.getName());
		ref.file = file;
		ref.lastModified = file.lastModified();
		try {
			String name = file.getName().toLowerCase();
			if (name.endsWith(".png") || name.endsWith(".jpg")) {
				FileImageInputStream fin = new FileImageInputStream(file);
				Iterator<ImageReader> iter = ImageIO.getImageReaders(fin);
				while (iter.hasNext()) {
					ImageReader ir = iter.next();
					ir.setInput(fin);
					ref.width = ir.getWidth(0);
					ref.height = ir.getHeight(0);
				}
			} else if (name.endsWith(".svg")) {
				Dimension d = SVGParser.parse(file);
				if (d != null) {
					ref.width = d.width;
					ref.height = d.height;
				}
			}
		} catch (Exception e) {
			// ignore
		}
		return ref;
	}

	@Override
	protected void initializeContestImpl() throws Exception {
		long time = System.currentTimeMillis();
		contest.setHashCode(getDiskHash());
		contest.addModifier((c, obj) -> {
			if (contestId != null && obj instanceof Info) {
				Info info2 = (Info) obj;
				info2.setId(contestId);
			}
			// attachLocalResources(contest, obj);
		});
		IContestModifier mod = (contest2, obj) -> attachLocalResources(obj);
		contest.addModifier(mod);

		loadConfigFiles();

		// load event feed
		File feedFile = eventFeedFile;
		if (feedFile == null && root != null) {
			feedFile = new File(root, "event-feed.json");
			if (!feedFile.exists())
				feedFile = new File(root, "events.xml");
		}
		if (!feedFile.exists()) {
			if (eventFeedFile != null || root != null)
				Trace.trace(Trace.WARNING, "No local event feed found");
			contest.removeModifier(mod);
			return;
		}

		InputStream in;
		try {
			in = new FileInputStream(feedFile);
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Could not read event feed", e);
			contest.removeModifier(mod);
			throw e;
		}
		try {
			if (feedFile.getName().endsWith("xml")) {
				XMLFeedParser xmlParser = new XMLFeedParser();
				xmlParser.parse(contest, in);
				parser = xmlParser;
			} else {
				NDJSONFeedParser jsonParser = new NDJSONFeedParser();
				jsonParser.parse(contest, in);
				parser = jsonParser;
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error reading event feed", e);
			throw e;
		} finally {
			try {
				in.close();
			} catch (Exception e) {
				// ignore
			}
			contest.removeModifier(mod);
		}

		Trace.trace(Trace.INFO, "Time to load EF: " + (System.currentTimeMillis() - time) + "ms");
	}

	@Override
	public void close() throws Exception {
		if (parser != null)
			parser.close();
	}

	/**
	 * Returns a list of files if they match the pattern, or <code>null</code> if there are no
	 * matching files.
	 *
	 * @param obj
	 * @param property
	 * @return
	 */
	public FileReferenceList getFilesWithPattern(IContestObject obj, String property) {
		if (obj == null)
			return null;

		FilePattern pattern = getLocalPattern(obj.getType(), obj.getId(), property);
		if (pattern == null) {
			Trace.trace(Trace.ERROR, "No file pattern: " + obj.getType() + " " + property);
			return null;
		}
		return getFilesWithPattern(pattern);
	}

	/**
	 * Returns a list of files if they match the pattern, or <code>null</code> if there are no
	 * matching files.
	 *
	 * @param pattern a filename pattern
	 * @return
	 */
	private FileReferenceList getFilesWithPattern(FilePattern pattern) {
		File folder = root;
		if (pattern.folder != null)
			folder = new File(root, pattern.folder);

		FileReferenceList refList = new FileReferenceList();
		for (String ext : pattern.extensions) {
			File[] files = folder.listFiles(
					(dir, name) -> (name.toLowerCase().startsWith(pattern.name) && name.toLowerCase().endsWith("." + ext)));

			if (files != null) {
				for (File file : files) {
					String diff = file.getName();
					diff = diff.substring(pattern.name.length(), diff.length() - ext.length() - 1);
					refList.add(getMetadata(pattern.url + diff, file));
					// TODO future: url currently would not be able to handle logo2.svg and logo2.png
				}
			}
		}
		return refList;
	}

	/**
	 * Returns the file pattern for a given contest object type, id, and property.
	 */
	protected FilePattern getLocalPattern(IContestObject.ContestType type, String id, String property) {
		String config = "config";
		String reg = ""; // "registration" + File.separator;
		String events = ""; // "events" + File.separator;
		if (type == ContestType.CONTEST) {
			if (LOGO.equals(property))
				return new FilePattern(null, id, property, config, LOGO_EXTENSIONS);
			if (BANNER.equals(property))
				return new FilePattern(null, id, property, config, LOGO_EXTENSIONS);
		} else if (type == ContestType.TEAM) {
			if (PHOTO.equals(property))
				return new FilePattern(type, id, property, reg, PHOTO_EXTENSIONS);
			if (VIDEO.equals(property))
				return new FilePattern(type, id, property, reg, "m2ts");
			if (BACKUP.equals(property))
				return new FilePattern(type, id, property, reg, "zip");
			if (KEY_LOG.equals(property))
				return new FilePattern(type, id, property, reg, "txt");
			if (TOOL_DATA.equals(property))
				return new FilePattern(type, id, property, reg, "txt");
		} else if (type == ContestType.TEAM_MEMBER) {
			if (PHOTO.equals(property))
				return new FilePattern(type, id, property, reg, PHOTO_EXTENSIONS);
		} else if (type == ContestType.ORGANIZATION) {
			if (LOGO.equals(property))
				return new FilePattern(type, id, property, reg, LOGO_EXTENSIONS);
			if (COUNTRY_FLAG.equals(property))
				return new FilePattern(type, id, property, reg, LOGO_EXTENSIONS);
		} else if (type == ContestType.SUBMISSION) {
			if (FILES.equals(property))
				return new FilePattern(type, id, property, events, "zip");
			if (REACTION.equals(property))
				return new FilePattern(type, id, property, events, "m2ts");
		} else if (type == ContestType.GROUP) {
			if (LOGO.equals(property))
				return new FilePattern(type, id, property, reg, LOGO_EXTENSIONS);
		}
		return null;
	}

	public void attachLocalResources(IContestObject obj) {
		if (obj instanceof Info) {
			Info info = (Info) obj;
			info.setLogo(getFilesWithPattern(obj, LOGO));
			info.setBanner(getFilesWithPattern(obj, BANNER));
		} else if (obj instanceof Organization) {
			Organization org = (Organization) obj;
			org.setLogo(getFilesWithPattern(obj, LOGO));
			org.setCountryFlag(getFilesWithPattern(obj, COUNTRY_FLAG));
		} else if (obj instanceof Team) {
			Team team = (Team) obj;
			team.setPhoto(getFilesWithPattern(obj, PHOTO));
			team.setVideo(getFilesWithPattern(obj, VIDEO));
			team.setBackup(getFilesWithPattern(obj, BACKUP));
			team.setKeyLog(getFilesWithPattern(obj, KEY_LOG));
			team.setToolData(getFilesWithPattern(obj, TOOL_DATA));
		} else if (obj instanceof TeamMember) {
			TeamMember member = (TeamMember) obj;
			member.setPhoto(getFilesWithPattern(obj, PHOTO));
		} else if (obj instanceof Submission) {
			Submission submission = (Submission) obj;
			submission.setFiles(getFilesWithPattern(obj, FILES));
			submission.setReaction(getFilesWithPattern(obj, REACTION));
		} else if (obj instanceof Group) {
			Group group = (Group) obj;
			group.setLogo(getFilesWithPattern(obj, LOGO));
		}
	}

	private static void loadFile(Contest contest, File f, String typeName) throws IOException {
		Trace.trace(Trace.INFO, "Loading " + typeName);
		if (f == null || !f.exists())
			return;

		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			JSONParser parser = new JSONParser(new FileInputStream(f));
			Object[] arr = parser.readArray();
			for (Object obj : arr) {
				JsonObject data = (JsonObject) obj;
				ContestObject co = (ContestObject) IContestObject.createByName(typeName);
				for (String key : data.props.keySet())
					co.add(key, data.props.get(key));

				contest.add(co);
			}
		}
	}

	private static void loadFileSingle(Contest contest, File f, String typeName) throws IOException {
		Trace.trace(Trace.INFO, "Loading " + typeName);
		if (f == null || !f.exists())
			return;

		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			JSONParser parser = new JSONParser(new FileInputStream(f));
			JsonObject data = parser.readObject();
			ContestObject co = (ContestObject) IContestObject.createByName(typeName);
			for (String key : data.props.keySet())
				co.add(key, data.props.get(key));

			contest.add(co);
		}
	}

	private static File getConfigFile(File root, String file) {
		if (root == null)
			return null;
		return new File(root, "config" + File.separator + file);
	}

	private static File getRegistrationFile(File root, String file) {
		if (root == null)
			return null;
		return new File(root, "registration" + File.separator + file);
	}

	protected void loadConfigFiles() {
		if (root == null) // this is a cache
			return;

		configValidation = new Validation();
		try {
			Trace.trace(Trace.INFO, "Importing contest info");
			File f = getConfigFile(root, "contest.json");
			if (f.exists())
				loadFileSingle(contest, f, "contests");
			else {
				Info info = YamlParser.importContestInfo(root);
				contest.add(info);
			}
			configValidation.ok("Contest info loaded");
		} catch (FileNotFoundException e) {
			configValidation.ok("Contest info not found");
			Trace.trace(Trace.INFO, e.getMessage());
		} catch (Exception e) {
			configValidation.err("Error importing contest info: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing contest info", e);
		}

		try {
			List<IProblem> problems = YamlParser.importProblems(root);
			for (IProblem p : problems)
				contest.add(p);

			Trace.trace(Trace.INFO, "Imported problem set");
		} catch (FileNotFoundException e) {
			Trace.trace(Trace.INFO, e.getMessage());
		} catch (Exception e) {
			configValidation.err("Error importing problem set: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing problem set", e);
		}

		try {
			String s = "Imported groups";
			File f = getRegistrationFile(root, "groups.json");
			if (f.exists())
				loadFile(contest, f, "groups");
			else {
				f = getConfigFile(root, "groups.tsv");
				if (f.exists())
					TSVImporter.importGroups(contest, f);
				else
					s = "Group config file (groups.json/groups.tsv) not found";
			}
			Trace.trace(Trace.INFO, s);
		} catch (Exception e) {
			configValidation.err("Error importing groups: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing groups", e);
		}

		try {
			String s = "Imported organizations";
			File f = getRegistrationFile(root, "organizations.json");
			if (f.exists())
				loadFile(contest, f, "organizations");
			else {
				f = getConfigFile(root, "institutions2.tsv");
				if (!f.exists())
					f = getConfigFile(root, "institutions.tsv");

				if (f.exists())
					TSVImporter.importInstitutions(contest, f);
				else
					s = "Institutions config file (institutions.json/institutions2.tsv/institutions.tsv) not found";
			}
			Trace.trace(Trace.INFO, s);
		} catch (Exception e) {
			configValidation.err("Error importing institutions: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing institutions", e);
		}

		try {
			String s = "Imported teams";
			File f = getRegistrationFile(root, "teams.json");
			if (f.exists())
				loadFile(contest, f, "teams");
			else {
				f = getConfigFile(root, "teams2.tsv");

				if (!f.exists())
					f = getConfigFile(root, "teams.tsv");

				if (f.exists())
					TSVImporter.importTeams(contest, f);
				else
					s = "Team config file (teams.json/teams2.tsv/teams.tsv) not found";
			}
			Trace.trace(Trace.INFO, s);
		} catch (Exception e) {
			configValidation.err("Error importing teams: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing teams", e);
		}

		try {
			String s = "Imported team-members";
			File f = getRegistrationFile(root, "team-members.json");
			if (f.exists())
				loadFile(contest, f, "team-members");
			else {
				f = getConfigFile(root, "members.tsv");
				if (f.exists())
					TSVImporter.importTeamMembers(contest, f);
				else
					s = "Team member config file (members.json/members.tsv) not found";
			}
			Trace.trace(Trace.INFO, s);
		} catch (Exception e) {
			configValidation.err("Error importing team-members: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing team-members", e);
		}

		try {
			File f = getConfigFile(root, "floor-map.tsv");
			if (f.exists()) {
				FloorMap map = new FloorMap(contest);
				map.load(new FileInputStream(f));
				Trace.trace(Trace.INFO, "Imported contest floor map");
			}
		} catch (Exception e) {
			configValidation.err("Error importing floor map: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing floor map", e);
		}
	}

	@Override
	public Validation validate() {
		return configValidation;
	}

	@Override
	public String toString() {
		return "DiskContestSource[" + eventFeedFile + "/" + root + "]";
	}
}