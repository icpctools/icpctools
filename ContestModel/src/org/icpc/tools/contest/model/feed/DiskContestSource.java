package org.icpc.tools.contest.model.feed;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObject.ContestType;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
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
import org.icpc.tools.contest.model.internal.Person;
import org.icpc.tools.contest.model.internal.Problem;
import org.icpc.tools.contest.model.internal.Submission;
import org.icpc.tools.contest.model.internal.Team;
import org.icpc.tools.contest.model.internal.YamlParser;

/**
 * A contest source that is backed by either a Contest Archive Format (CAF) exploded on disk or an
 * event feed (JSON or XML). A cache folder in temp is used to store metadata to improve
 * performance.
 */
public class DiskContestSource extends ContestSource {
	private static final String CACHE_PREFIX = "org.icpc.tools.cache.";
	private static final String CACHE_FILE = ".cache";
	private static final String CACHE_VERSION = "ICPC Tools Cache v1.1";

	private static final String LOGO = "logo";
	private static final String LOGO_BACKGROUNDMODE = "prlogo";
	private static final String PHOTO = "photo";
	private static final String VIDEO = "video";
	private static final String BANNER = "banner";
	private static final String BANNER_BACKGROUNDMODE = "prbanner";
	private static final String BACKUP = "backup";
	private static final String KEY_LOG = "key_log";
	private static final String TOOL_DATA = "tool_data";
	private static final String FILES = "files";
	private static final String REACTION = "reaction";
	private static final String COUNTRY_FLAG = "country_flag";
	private static final String COUNTRY_SUBDIVISON_FLAG = "country_subdivision_flag";
	private static final String PACKAGE = "package";
	private static final String STATEMENT = "statement";

	private static final String[] LOGO_EXTENSIONS = new String[] { "png", "svg", "jpg", "jpeg" };
	private static final String[] PHOTO_EXTENSIONS = new String[] { "jpg", "jpeg", "png", "svg" };
	private static final String[] VIDEO_EXTENSIONS = new String[] { "m2ts", "ogg", "flv", };

	protected File eventFeedFile;
	private File root;
	protected File cacheFolder;
	private String contestId;
	private Closeable parser;
	private Validation configValidation = new Validation();
	private Map<String, List<FileReference>> cache = new HashMap<>();

	private Team defaultTeam = new Team();
	private Person defaultPerson = new Person();

	private ScheduledFuture<?> backgroundScanningTask;

	static class FilePattern {
		// the folder containing the file
		protected String folder;

		// the file name, without extension
		protected String name; // e.g. "logo" or "files"

		// the file extensions
		protected String[] extensions; // e.g. "jpg" or "zip"

		// the partial url to access the file
		protected String url;

		public FilePattern(IContestObject.ContestType type, String id, String property, String fileExtension) {
			this(type, id, property, new String[] { fileExtension });
		}

		public FilePattern(IContestObject.ContestType type, String id, String property, String[] fileExtensions) {
			if (type == null) {
				this.folder = "contest";
				this.url = property;
			} else {
				String typeName = IContestObject.getTypeName(type);
				this.folder = typeName + File.separator + id;
				this.url = typeName + "/" + id + "/" + property;
			}

			this.name = property;
			this.extensions = fileExtensions;
		}

		@Override
		public String toString() {
			return folder + " " + name + " " + String.join(",", extensions) + " " + url;
		}
	}

	/**
	 * Create a disk contest source reading from the contest archive format (CAF).
	 *
	 * @param folder - a contest archive folder
	 */
	public DiskContestSource(File folder) {
		this(folder, null);
	}

	/**
	 * Create a disk contest source reading from a JSON or XML event feed.
	 *
	 * @param eventFeedFile - a JSON or XML event feed file
	 */
	public DiskContestSource(String eventFeedFile) {
		this(new File(eventFeedFile), null);
	}

	/**
	 * General constructor for a disk contest source. Typically, only one of the event feed file and
	 * CAF folder are provided. If neither are provided (i.e. this class is just being used for
	 * local caching), then a hash must be provided to seed the temporary caching folder name (so
	 * that it is consistent across restarts). A hash can also be provided to distinguish between
	 * two feeds that use the same CAF but have a different hash, for example a different CCS URL
	 *
	 * @param eventFeedFile - a JSON or XML event feed file
	 * @param folder - a contest archive folder
	 * @param hash - any uid or hash that's consistent across restarts
	 */
	protected DiskContestSource(File file, String hash) {
		// if file is null we're just locally caching
		// if file exists and is a file, it's an event feed
		// if it doesn't exist, assume a directory
		if (file != null) {
			if (hash != null) {
				cacheFolder = createTempDir(getSafeHash(hash) + "-" + getSafeHash(file.getAbsolutePath()));
			} else {
				cacheFolder = createTempDir(file.getName() + "-" + getSafeHash(file.getAbsolutePath()));
			}
			if (file.isFile()) {
				eventFeedFile = file;
				root = cacheFolder;
			} else {
				root = file;
				if (!root.exists()) {
					if (!root.mkdirs()) {
						throw new IllegalArgumentException(
								"Contest location (" + root.toString() + ") did not exist and directory creation failed.");
					}
				}
			}
		} else {
			cacheFolder = createTempDir(getSafeHash(hash));
			root = cacheFolder;
		}

		cleanUpTempDir();

		FileReferenceList list = getDefaultFilesWithPattern(defaultTeam, PHOTO);
		if (list != null && !list.isEmpty())
			defaultTeam.setPhoto(list);
		list = getDefaultFilesWithPattern(defaultPerson, PHOTO);
		if (list != null && !list.isEmpty())
			defaultPerson.setPhoto(list);

		instance = this;
	}

	public static Contest loadContest(File file, IContestListener listener) throws Exception {
		DiskContestSource source = new DiskContestSource(file);
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
	public File getFile(IContestObject obj, FileReference ref, String property) throws Exception {
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
		return getNewFile(obj.getType(), obj.getId(), property, ref);
	}

	/**
	 * Return a new local file name that is suitable for saving the given property of a contest
	 * object of a specific type and id to.
	 */
	public File getNewFile(IContestObject.ContestType type, String id, String property, FileReference fileRef) {
		FilePattern pattern = getLocalPattern(type, id, property);
		if (pattern == null)
			return null;

		File folder = root;
		if (pattern.folder != null)
			folder = new File(root, pattern.folder);

		String name = null;
		String ext = null;
		if (fileRef != null) {
			// use existing filename if there is one
			if (fileRef.filename != null) {
				int ind = fileRef.filename.lastIndexOf(".");
				if (ind > 0) {
					name = fileRef.filename.substring(0, ind);
					if (ind < fileRef.filename.length() - 2)
						ext = fileRef.filename.substring(ind + 1);
				} else
					name = fileRef.filename;
			} else {
				// otherwise, add size if it exists
				if (fileRef.width > 0) {
					name = pattern.name + fileRef.width;
					if (fileRef.height > 0)
						name += "x" + fileRef.height;
				}
			}
			if (ext == null && fileRef.mime != null)
				ext = getExtension(fileRef.mime);
		}

		// fallback to default filename and extension as necessary
		if (name == null || !name.startsWith(pattern.name))
			name = pattern.name;
		if (ext == null)
			ext = pattern.extensions[0];

		ext = "." + ext;

		File file = new File(folder, name + ext);
		int n = 2;
		while (file.exists()) {
			file = new File(folder, name + "-" + n + ext);
			n++;
		}
		return file;
	}

	@Override
	public File getFile(String path) throws Exception {
		String path2 = path;
		if (path.endsWith("/"))
			path2 += "listing.dir";

		if (root != null)
			return new File(root, path2);

		return new File(cacheFolder, path2);
	}

	@Override
	public String[] getDirectory(String path) throws Exception {
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
					readImageSize(ref);

					writeCache(folder, list);
					return;
				}
			}

			// not found, new file
			FileReference ref = new FileReference();
			ref.filename = file.getName();
			ref.etag = etag;
			ref.href = href;
			ref.file = file;
			ref.mime = getMimeType(file.getName());
			ref.lastModified = file.lastModified();
			readImageSize(ref);

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
				ref.filename = name;
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

	protected List<FileReference> getCache(File folder) {
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
			Trace.trace(Trace.ERROR, "Error getting file cache", e);
		}
		return currentList;
	}

	public void updateCache(ContestType type, String id) {
		String typeName = IContestObject.getTypeName(type);
		File folder = new File(root, typeName + File.separator + id);
		List<FileReference> list = cache.get(folder.getAbsolutePath());
		if (list == null) {
			try {
				list = readCache(folder);
			} catch (Exception e) {
				list = new ArrayList<>(5);
			}
		}

		// verify cache
		List<FileReference> currentList = new ArrayList<>();
		try {
			File[] files = folder.listFiles();
			boolean diff = false;
			if (files != null) {
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
			}
			if (diff || list.size() != currentList.size())
				writeCache(folder, currentList);

			cache.put(folder.getAbsolutePath(), currentList);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error updating file cache", e);
		}
	}

	private void writeCache(File folder, List<FileReference> list) {
		BufferedWriter bw = null;
		FileOutputStream fileOut = null;

		try {
			File f = getCacheForFolder(folder);
			fileOut = new FileOutputStream(f);

			bw = new BufferedWriter(new OutputStreamWriter(fileOut));
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
						ref.height + "", ref.mode));
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
				fileOut.getFD().sync();
			} catch (Exception ex) {
				// ignore
			}
		}
	}

	protected FileReference getMetadata(String href, File file) {
		FileReference ref = getFileRef(file);
		if (ref == null) {
			Trace.trace(Trace.ERROR, "Null file ref! " + href + " - " + file + " - " + file.exists());
			ref = getFileRef(file);
			if (ref == null) {
				Trace.trace(Trace.ERROR, "Not found second pass");
				return null;
			}
		}
		ref.href = "contests/" + contestId + "/" + href;
		return ref;
	}

	/**
	 * Return mime-type based on filename.
	 *
	 * @param name
	 * @return
	 */
	private static String getMimeType(String name) {
		String name2 = name.toLowerCase();
		if (name2.endsWith(".zip"))
			return "application/zip";
		else if (name2.endsWith(".png"))
			return "image/png";
		else if (name2.endsWith(".jpg") || name2.endsWith(".jpeg"))
			return "image/jpeg";
		else if (name2.endsWith(".svg"))
			return "image/svg+xml";
		else if (name2.endsWith(".m2ts"))
			return "video/m2ts";
		else if (name2.endsWith(".ogg"))
			return "video/ogg";
		else if (name2.endsWith(".flv"))
			return "video/x-flv";
		else if (name2.endsWith(".txt"))
			return "text/plain";
		else if (name2.endsWith(".log"))
			return "text/plain";
		else if (name2.endsWith(".pdf"))
			return "application/pdf";
		return null;
	}

	/**
	 * Return filename based on mime-type.
	 *
	 * @param mimeType
	 * @return
	 */
	private static String getExtension(String mimeType) {
		if (mimeType == null)
			return null;

		if (mimeType.equals("application/zip"))
			return "zip";
		else if (mimeType.equals("image/png"))
			return "png";
		else if (mimeType.equals("image/jpeg"))
			return "jpg";
		else if (mimeType.equals("image/svg+xml"))
			return "svg";
		else if (mimeType.equals("video/MP2T") || mimeType.equals("video/m2ts"))
			return "m2ts";
		else if (mimeType.equals("video/ogg"))
			return "ogg";
		else if (mimeType.equals("video/x-flv"))
			return "flv";
		else if (mimeType.equals("text/plain"))
			return "txt";
		else if (mimeType.equals("application/pdf"))
			return "pdf";
		return null;
	}

	protected static void readImageSize(FileReference ref) {
		File file = ref.file;
		String name = file.getName().toLowerCase();
		try {
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
	}

	protected static FileReference readMetadata(File file) {
		FileReference ref = new FileReference();
		ref.filename = file.getName();
		ref.mime = getMimeType(file.getName());
		ref.file = file;
		ref.lastModified = file.lastModified();
		String tmpFilename = file.getName().split("\\.")[0];
		if (tmpFilename.contains("-")) {
			String[] parts = tmpFilename.split("-");
			if (parts.length > 1) {
				String mode = parts[parts.length - 1];
				if (List.of("light", "dark").contains(mode)) {
					ref.mode = mode;
				}
			}
		}
		readImageSize(ref);
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
		});
		IContestModifier mod = (contest2, obj) -> attachLocalResources(obj);
		contest.addModifier(mod);

		loadConfigFiles();

		// load event feed
		File feedFile = eventFeedFile;
		if (feedFile == null && root != null) {
			feedFile = new File(root, "event-feed.ndjson");
			if (!feedFile.exists())
				feedFile = new File(root, "event-feed.json");
			if (!feedFile.exists())
				feedFile = new File(root, "events.xml");
		}
		if (!feedFile.exists()) {
			if (eventFeedFile != null || root != null)
				Trace.trace(Trace.WARNING, "No local event feed found");
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
		}

		Trace.trace(Trace.INFO, "Time to load EF: " + (System.currentTimeMillis() - time) + "ms");
	}

	public void setExecutor(ScheduledExecutorService executor) {
		backgroundScanningTask = executor.scheduleWithFixedDelay(() -> scanForResourceChanges(), 15L, 15L,
				TimeUnit.SECONDS);
	}

	private static boolean hasChange(FileReferenceList list, FileReferenceList foundList, List<File> changed) {
		if (list != null) {
			for (FileReference ref : list) {
				if (ref.isDeleted()) {
					// existing file has been deleted or modified
					return true;
				} else if (ref.isChanged()) {
					changed.add(ref.file);
					ref.lastModified = ref.file.lastModified();
				}
			}
		}

		for (FileReference foundRef : foundList) {
			boolean found = false;
			if (list != null) {
				for (FileReference ref : list) {
					if (foundRef.file.equals(ref.file)) {
						found = true;
						break;
					}
				}
			}
			if (!found) {
				// new file
				return true;
			}
		}
		return false;
	}

	private static FileReferenceList deleteAndMergeFiles(FileReferenceList list, FileReferenceList foundList,
			List<File> added, List<File> removed) {
		if (list == null)
			return foundList;

		FileReferenceList newList = new FileReferenceList();
		for (FileReference ref : list) {
			if (!ref.isDeleted()) {
				newList.add(ref);
			} else {
				removed.add(ref.file);
			}
		}

		for (FileReference foundRef : foundList) {
			boolean found = false;
			for (FileReference ref : list) {
				if (foundRef.file.equals(ref.file)) {
					found = true;
					break;
				}
			}
			if (!found) {
				added.add(foundRef.file);
				newList.add(foundRef);
			}
		}

		return newList;
	}

	/**
	 * Scan for resource changes on disk. If there is a change, re-add the object to the contest to
	 * trigger an event.
	 */
	protected void scanForResourceChanges() {
		long time = System.currentTimeMillis();
		try {
			List<File> modifiedFiles = new ArrayList<>();
			List<File> addedFiles = new ArrayList<>();
			List<File> removedFiles = new ArrayList<>();
			IOrganization[] orgs = contest.getOrganizations();
			for (IOrganization org : orgs) {
				Organization newOrg = (Organization) ((Organization) org).clone();

				// update the file cache
				updateCache(ContestType.ORGANIZATION, org.getId());

				boolean changed = false;
				FileReferenceList refsOnDisk = getFilesWithPattern(newOrg, LOGO);
				if (hasChange(org.getLogo(), refsOnDisk, modifiedFiles)) {
					newOrg.setLogo(deleteAndMergeFiles(org.getLogo(), refsOnDisk, addedFiles, removedFiles));
					changed = true;
				}
				refsOnDisk = getFilesWithPattern(newOrg, COUNTRY_FLAG);
				if (hasChange(org.getCountryFlag(), refsOnDisk, modifiedFiles)) {
					newOrg.setCountryFlag(deleteAndMergeFiles(org.getCountryFlag(), refsOnDisk, addedFiles, removedFiles));
					changed = true;
				}
				refsOnDisk = getFilesWithPattern(newOrg, COUNTRY_SUBDIVISON_FLAG);
				if (hasChange(org.getCountrySubdivisionFlag(), refsOnDisk, modifiedFiles)) {
					newOrg.setCountrySubdivisionFlag(
							deleteAndMergeFiles(org.getCountrySubdivisionFlag(), refsOnDisk, addedFiles, removedFiles));
					changed = true;
				}

				if (changed)
					contest.addDirect(newOrg);
			}

			ITeam[] teams = contest.getTeams();
			for (ITeam team : teams) {
				Team newTeam = (Team) ((Team) team).clone();

				// update the file cache
				updateCache(ContestType.TEAM, team.getId());

				boolean changed = false;
				FileReferenceList refsOnDisk = getFilesWithPattern(newTeam, PHOTO);
				if (hasChange(team.getPhoto(), refsOnDisk, modifiedFiles)) {
					newTeam.setPhoto(deleteAndMergeFiles(team.getPhoto(), refsOnDisk, addedFiles, removedFiles));
					changed = true;
				}

				refsOnDisk = getFilesWithPattern(newTeam, VIDEO);
				if (hasChange(team.getVideo(), refsOnDisk, modifiedFiles)) {
					newTeam.setVideo(deleteAndMergeFiles(team.getVideo(), refsOnDisk, addedFiles, removedFiles));
					changed = true;
				}

				refsOnDisk = getFilesWithPattern(newTeam, BACKUP);
				if (hasChange(team.getBackup(), refsOnDisk, modifiedFiles)) {
					newTeam.setBackup(deleteAndMergeFiles(team.getBackup(), refsOnDisk, addedFiles, removedFiles));
					changed = true;
				}

				refsOnDisk = getFilesWithPattern(newTeam, KEY_LOG);
				if (hasChange(team.getKeyLog(), refsOnDisk, modifiedFiles)) {
					newTeam.setBackup(deleteAndMergeFiles(team.getKeyLog(), refsOnDisk, addedFiles, removedFiles));
					changed = true;
				}

				refsOnDisk = getFilesWithPattern(newTeam, TOOL_DATA);
				if (hasChange(team.getToolData(), refsOnDisk, modifiedFiles)) {
					newTeam.setToolData(deleteAndMergeFiles(team.getToolData(), refsOnDisk, addedFiles, removedFiles));
					changed = true;
				}

				if (changed)
					contest.addDirect(newTeam);
			}

			ISubmission[] subs = contest.getSubmissions();
			for (ISubmission sub : subs) {
				Submission newOrg = (Submission) ((Submission) sub).clone();

				// update the file cache
				updateCache(ContestType.SUBMISSION, sub.getId());

				boolean changed = false;
				FileReferenceList refsOnDisk = getFilesWithPattern(newOrg, FILES);
				if (hasChange(sub.getFiles(), refsOnDisk, modifiedFiles)) {
					newOrg.setFiles(deleteAndMergeFiles(sub.getFiles(), refsOnDisk, addedFiles, removedFiles));
					changed = true;
				}
				refsOnDisk = getFilesWithPattern(newOrg, REACTION);
				if (hasChange(sub.getReaction(), refsOnDisk, modifiedFiles)) {
					newOrg.setReaction(deleteAndMergeFiles(sub.getReaction(), refsOnDisk, addedFiles, removedFiles));
					changed = true;
				}

				if (changed)
					contest.addDirect(newOrg);
			}

			long numChanged = addedFiles.size() + modifiedFiles.size() + removedFiles.size();
			if (numChanged < 10) {
				for (File f : addedFiles) {
					Trace.trace(Trace.INFO, "New file: " + f);
				}
				for (File f : modifiedFiles) {
					Trace.trace(Trace.INFO, "Updated file: " + f);
				}
				for (File f : removedFiles) {
					Trace.trace(Trace.INFO, "Deleted file: " + f);
				}
			} else {
				Trace.trace(Trace.INFO, numChanged + " file changes (" + addedFiles.size() + " new, " + modifiedFiles.size()
						+ " updated, " + removedFiles.size() + " deleted)");
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Scanning failed", e);
		}
		Trace.trace(Trace.INFO,
				"Scanned " + this.contestId + " for changes in " + (System.currentTimeMillis() - time) + "ms");
	}

	@Override
	public void close() throws Exception {
		if (parser != null)
			parser.close();

		if (backgroundScanningTask != null)
			backgroundScanningTask.cancel(false);
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
	 * Returns a list of default files that match the pattern, or <code>null</code> if there are no
	 * matching files.
	 *
	 * @param obj
	 * @param property
	 * @return
	 */
	public FileReferenceList getDefaultFilesWithPattern(IContestObject obj, String property) {
		return getFilesWithPattern(getLocalPattern(obj.getType(), "default-id", property));
	}

	/**
	 * Returns a list of files if they match the pattern, or <code>null</code> if there are no
	 * matching files.
	 *
	 * @param pattern a filename pattern
	 * @return
	 */
	private FileReferenceList getFilesWithPattern(FilePattern pattern) {
		if (pattern == null)
			return null;

		File folder = root;
		if (pattern.folder != null)
			folder = new File(root, pattern.folder);

		FileReferenceList refList = new FileReferenceList();
		List<String> hrefs = new ArrayList<>();
		for (String ext : pattern.extensions) {
			File[] files = folder.listFiles(
					(dir, name) -> (name.toLowerCase().startsWith(pattern.name) && name.toLowerCase().endsWith("." + ext)));

			if (files != null) {
				for (File file : files) {
					String diff = file.getName();
					diff = diff.substring(pattern.name.length(), diff.length() - ext.length() - 1);
					FileReference ref = getMetadata(pattern.url + diff, file);
					if (ref != null) {
						// update the href if it is already in use (e.g. for logo.svg and logo.png)
						int count = 2;
						while (hrefs.contains(ref.href)) {
							ref.href = "contests/" + contestId + "/" + pattern.url + diff + count;
							count += 1;
						}
						refList.add(ref);
						hrefs.add(ref.href);
					}
				}
			}
		}
		return refList;
	}

	/**
	 * Returns the file pattern for a given contest object type, id, and property.
	 */
	protected FilePattern getLocalPattern(IContestObject.ContestType type, String id, String property) {
		if (type == ContestType.CONTEST) {
			if (LOGO.equals(property))
				return new FilePattern(null, id, property, LOGO_EXTENSIONS);
			if (LOGO_BACKGROUNDMODE.equals(property))
				return new FilePattern(null, id, property, LOGO_EXTENSIONS);
			if (BANNER.equals(property))
				return new FilePattern(null, id, property, LOGO_EXTENSIONS);
			if (BANNER_BACKGROUNDMODE.equals(property))
				return new FilePattern(null, id, property, LOGO_EXTENSIONS);
		} else if (type == ContestType.TEAM) {
			if (PHOTO.equals(property))
				return new FilePattern(type, id, property, PHOTO_EXTENSIONS);
			if (VIDEO.equals(property))
				return new FilePattern(type, id, property, VIDEO_EXTENSIONS);
			if (BACKUP.equals(property))
				return new FilePattern(type, id, property, "zip");
			if (KEY_LOG.equals(property))
				return new FilePattern(type, id, property, "txt");
			if (TOOL_DATA.equals(property))
				return new FilePattern(type, id, property, "txt");
		} else if (type == ContestType.PERSON) {
			if (PHOTO.equals(property))
				return new FilePattern(type, id, property, PHOTO_EXTENSIONS);
		} else if (type == ContestType.ORGANIZATION) {
			if (LOGO.equals(property))
				return new FilePattern(type, id, property, LOGO_EXTENSIONS);
			if (COUNTRY_FLAG.equals(property))
				return new FilePattern(type, id, property, LOGO_EXTENSIONS);
			if (COUNTRY_SUBDIVISON_FLAG.equals(property))
				return new FilePattern(type, id, property, LOGO_EXTENSIONS);
		} else if (type == ContestType.SUBMISSION) {
			if (FILES.equals(property))
				return new FilePattern(type, id, property, "zip");
			if (REACTION.equals(property))
				return new FilePattern(type, id, property, VIDEO_EXTENSIONS);
		} else if (type == ContestType.GROUP) {
			if (LOGO.equals(property))
				return new FilePattern(type, id, property, LOGO_EXTENSIONS);
		} else if (type == ContestType.PROBLEM) {
			if (PACKAGE.equals(property))
				return new FilePattern(type, id, property, "zip");
			if (STATEMENT.equals(property))
				return new FilePattern(type, id, property, "pdf");
		}
		return null;
	}

	/**
	 * Merge two file reference lists, typically the current list on an object property and locally
	 * found resources.
	 *
	 * @param curList
	 * @param localList
	 * @return
	 */
	private static FileReferenceList mergeRefs(FileReferenceList curList, FileReferenceList localList) {
		if (localList == null || localList.isEmpty())
			return curList;

		if (curList == null || curList.isEmpty())
			return localList;

		FileReferenceList list = localList;

		for (FileReference ref : curList) {
			if (ref.height <= 0 || ref.width <= 0)
				continue;

			boolean found = false;
			for (FileReference ref2 : localList) {
				if (ref.height == ref2.height && ref.width == ref2.width
						&& (ref.mime == null || ref.mime.equals(ref2.mime))) {
					found = true;
					continue;
				}
			}
			if (!found)
				list.add(ref);
		}

		return list;
	}

	/**
	 * Merge two file reference lists, typically the current list on an object property and locally
	 * found resources. If they're empty, use the default list.
	 *
	 * @param curList
	 * @param localList
	 * @return
	 */
	private static FileReferenceList mergeRefs(FileReferenceList curList, FileReferenceList localList,
			FileReferenceList defaultList) {
		if (defaultList == null || defaultList.isEmpty())
			return mergeRefs(curList, localList);

		FileReferenceList list = mergeRefs(curList, localList);
		if (curList == defaultList)
			list = localList;
		if (list != null && !list.isEmpty())
			return list;

		return defaultList;
	}

	public void attachLocalResources(IContestObject obj) {
		updateCache(obj.getType(), obj.getId());
		if (obj instanceof Info) {
			Info info = (Info) obj;
			info.setLogo(mergeRefs(info.getLogo(), getFilesWithPattern(obj, LOGO)));
			info.setLogoLightMode(mergeRefs(info.getLogoLightMode(), getFilesWithPattern(obj, LOGO_BACKGROUNDMODE)));
			info.setBanner(mergeRefs(info.getBanner(), getFilesWithPattern(obj, BANNER)));
			info.setBannerLightMode(mergeRefs(info.getBannerLightMode(), getFilesWithPattern(obj, BANNER_BACKGROUNDMODE)));
		} else if (obj instanceof Organization) {
			Organization org = (Organization) obj;
			org.setLogo(mergeRefs(org.getLogo(), getFilesWithPattern(obj, LOGO)));
			org.setCountryFlag(mergeRefs(org.getCountryFlag(), getFilesWithPattern(obj, COUNTRY_FLAG)));
			org.setCountrySubdivisionFlag(
					mergeRefs(org.getCountrySubdivisionFlag(), getFilesWithPattern(obj, COUNTRY_SUBDIVISON_FLAG)));
		} else if (obj instanceof Team) {
			Team team = (Team) obj;
			team.setPhoto(mergeRefs(team.getPhoto(), getFilesWithPattern(obj, PHOTO), defaultTeam.getPhoto()));
			team.setVideo(mergeRefs(team.getVideo(), getFilesWithPattern(obj, VIDEO)));
			team.setBackup(mergeRefs(team.getBackup(), getFilesWithPattern(obj, BACKUP)));
			team.setKeyLog(mergeRefs(team.getKeyLog(), getFilesWithPattern(obj, KEY_LOG)));
			team.setToolData(mergeRefs(team.getToolData(), getFilesWithPattern(obj, TOOL_DATA)));
		} else if (obj instanceof Person) {
			Person person = (Person) obj;
			person.setPhoto(mergeRefs(person.getPhoto(), getFilesWithPattern(obj, PHOTO), defaultPerson.getPhoto()));
		} else if (obj instanceof Submission) {
			Submission submission = (Submission) obj;
			submission.setFiles(mergeRefs(submission.getFiles(), getFilesWithPattern(obj, FILES)));
			submission.setReaction(mergeRefs(submission.getReaction(), getFilesWithPattern(obj, REACTION)));
		} else if (obj instanceof Group) {
			Group group = (Group) obj;
			group.setLogo(mergeRefs(group.getLogo(), getFilesWithPattern(obj, LOGO)));
		} else if (obj instanceof Problem) {
			Problem problem = (Problem) obj;
			problem.setPackage(mergeRefs(problem.getPackage(), getFilesWithPattern(obj, PACKAGE)));
			problem.setStatement(mergeRefs(problem.getStatement(), getFilesWithPattern(obj, STATEMENT)));
		}
	}

	private void loadFile(File f, IContestObject.ContestType type) throws IOException {
		if (f == null || !f.exists())
			return;

		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			JSONParser parser2 = new JSONParser(new FileInputStream(f));
			Object[] arr = parser2.readArray();
			for (Object obj : arr) {
				JsonObject data = (JsonObject) obj;
				String id = data.getString("id");
				ContestObject co = (ContestObject) contest.getObjectByTypeAndId(type, id);
				if (co == null)
					co = (ContestObject) IContestObject.createByType(type);
				for (String key : data.props.keySet())
					co.add(key, data.props.get(key));

				contest.add(co);
			}
		}

		Trace.trace(Trace.INFO, "Imported " + f.getName());
	}

	private void loadFileSingle(File f, IContestObject.ContestType type) throws IOException {
		if (f == null || !f.exists())
			return;

		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			JSONParser parser2 = new JSONParser(new FileInputStream(f));
			JsonObject data = parser2.readObject();
			String id = data.getString("id");
			if (type == IContestObject.ContestType.CONTEST)
				id = contestId;
			ContestObject co = (ContestObject) contest.getObjectByTypeAndId(type, id);
			if (co == null)
				co = (ContestObject) IContestObject.createByType(type);
			for (String key : data.props.keySet())
				co.add(key, data.props.get(key));

			contest.add(co);
		}

		Trace.trace(Trace.INFO, "Imported " + f.getName());
	}

	private static File getConfigFile(File root, String file) {
		if (root == null)
			return null;
		return new File(root, "config" + File.separator + file);
	}

	protected void loadConfigFiles() {
		if (root == null) // this is a cache
			return;

		// load yamls
		configValidation = new Validation();
		try {
			File f = new File(root, "contest.yaml");
			if (f.exists()) {
				contest.add(YamlParser.importContestInfo(f, false));
				Trace.trace(Trace.INFO, "Imported contest yaml");
			} else {
				f = getConfigFile(root, "contest.yaml");
				if (f.exists()) {
					contest.add(YamlParser.importContestInfo(f, true));
					Trace.trace(Trace.INFO, "Imported contest yaml");
				}
			}
		} catch (Exception e) {
			configValidation.err("Error importing contest info: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing contest info", e);
		}

		try {
			File f = new File(root, "accounts.yaml");
			if (f.exists()) {
				List<IAccount> accounts = YamlParser.importAccounts(f);
				for (IAccount a : accounts)
					contest.add(a);

				Trace.trace(Trace.INFO, "Imported accounts yaml");
			}
		} catch (Exception e) {
			configValidation.err("Error importing accounts: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing accounts", e);
		}

		try {
			File f = new File(root, "problems.yaml");
			if (!f.exists())
				f = getConfigFile(root, "problemset.yaml");

			if (f.exists()) {
				List<IProblem> problems = YamlParser.importProblems(f);
				for (IProblem p : problems)
					contest.add(p);

				Trace.trace(Trace.INFO, "Imported problems yaml");
			}
		} catch (Exception e) {
			configValidation.err("Error importing problems: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing problems", e);
		}

		// load tsvs
		try {
			File f = getConfigFile(root, "groups.tsv");
			if (f.exists()) {
				TSVImporter.importGroups(contest, f);
				Trace.trace(Trace.INFO, "Imported groups tsv");
			}
		} catch (Exception e) {
			configValidation.err("Error importing groups: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing groups", e);
		}

		try {
			File f = getConfigFile(root, "institutions2.tsv");
			if (!f.exists())
				f = getConfigFile(root, "institutions.tsv");

			if (f.exists()) {
				TSVImporter.importInstitutions(contest, f);
				Trace.trace(Trace.INFO, "Imported institutions tsv");
			}
		} catch (Exception e) {
			configValidation.err("Error importing institutions: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing institutions", e);
		}

		try {
			File f = getConfigFile(root, "teams2.tsv");
			if (!f.exists())
				f = getConfigFile(root, "teams.tsv");

			if (f.exists()) {
				TSVImporter.importTeams(contest, f);
				Trace.trace(Trace.INFO, "Imported teams tsv");
			}
		} catch (Exception e) {
			configValidation.err("Error importing teams: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing teams", e);
		}

		try {
			File f = getConfigFile(root, "members.tsv");
			if (f.exists()) {
				TSVImporter.importTeamMembers(contest, f);
				Trace.trace(Trace.INFO, "Imported persons tsv");
			}
		} catch (Exception e) {
			configValidation.err("Error importing persons: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing persons", e);
		}

		// load jsons
		loadJSONs(root);

		File[] override = root.listFiles(new FileFilter() {
			@Override
			public boolean accept(File f) {
				if (!f.isDirectory())
					return false;
				return f.getName().startsWith("extend-");
			}
		});

		if (override != null && override.length > 0) {
			for (File folder : override) {
				Trace.trace(Trace.USER, "Loading extended jsons from " + folder.getName());
				loadJSONs(folder);
			}
		}
	}

	private void loadJSONs(File folder) {
		IContestObject.ContestType[] types = IContestObject.ContestType.values();

		for (int i = 0; i < types.length; i++) {
			IContestObject.ContestType type = types[i];
			String name = IContestObject.getTypeName(type);
			try {
				File f = new File(folder, name + ".json");
				if (type == IContestObject.ContestType.CONTEST)
					f = new File(folder, "contest.json");
				if (f.exists()) {
					if (IContestObject.isSingleton(type) || type == IContestObject.ContestType.CONTEST)
						loadFileSingle(f, type);
					else
						loadFile(f, type);
				}
			} catch (Exception e) {
				configValidation.err("Error importing " + name + ": " + e.getMessage());
				Trace.trace(Trace.ERROR, "Error importing " + name, e);
			}
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
