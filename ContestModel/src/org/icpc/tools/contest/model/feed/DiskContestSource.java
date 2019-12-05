package org.icpc.tools.contest.model.feed;

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
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IProblem;
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
import org.icpc.tools.contest.model.internal.Problem;
import org.icpc.tools.contest.model.internal.Submission;
import org.icpc.tools.contest.model.internal.Team;
import org.icpc.tools.contest.model.internal.TeamMember;
import org.icpc.tools.contest.model.internal.YamlParser;

/**
 * A contest source that reads from a Contest Data Package exploded on disk.
 */
public class DiskContestSource extends ContestSource {
	private static final String CACHE_PREFIX = "org.icpc.tools.cache.";
	private static final String CACHE_FILE = ".cache";
	private static final String CACHE_VERSION = "ICPC Tools Cache v1.0";
	private static final Map<String, List<FileReference>> cache = new HashMap<>();

	private File root;
	private boolean isCache;
	private String contestId;
	private boolean expectFeed = true;
	private Closeable parser;
	private Validation configValidation;

	/**
	 * Create a disk contest source at the specified folder.
	 *
	 * @param folder
	 */
	public DiskContestSource(File folder, boolean expectFeed) {
		this(folder);
		this.expectFeed = expectFeed;
	}

	/**
	 * Create a disk contest source at the specified folder.
	 *
	 * @param folder
	 */
	public DiskContestSource(File folder) {
		root = folder;

		if (root == null) {
			throw new IllegalArgumentException("Contest archive not set.");
		}
		if (!root.exists()) {
			if (!root.mkdirs()) {
				throw new IllegalArgumentException(
						"Contest archive (" + root.toString() + ") did not exist and directory creation failed.");
			}
		}
		if (root.isFile()) {
			throw new IllegalArgumentException(
					"Contest archive (" + root.toString() + ") should point to directory, points to file instead.");
		}

		instance = this;
	}

	/**
	 * Create a disk contest source that's backed by a temp folder.
	 */
	public DiskContestSource(String hash) {
		root = createTempDir(getSafeHash(hash));
		isCache = true;
		expectFeed = false;

		if (root == null || !root.exists() || root.isFile())
			throw new IllegalArgumentException("Could not create temp folder");
		cleanUpTempDir(root);
		instance = this;
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
		return isCache;
	}

	private static void cleanUpTempDir(File cacheTempDir) {
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		File[] files = tempDir.listFiles((dir, name) -> name != null && name.startsWith(CACHE_PREFIX));

		if (files == null)
			return;

		for (File f : files) {
			if (f.isDirectory() && !f.equals(cacheTempDir)) {
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

	public File getRootFolder() {
		return root;
	}

	private int getDiskHash() {
		File hashFile = new File(root, "hash.txt");
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
		if (ref.file != null)
			return ref.file;

		// use the pattern to find the right folder
		String[] fileRef = getLocalPattern(obj, property);
		File rootFolder = getRootFolder();
		File folder = rootFolder;
		if (fileRef[0] != null)
			folder = new File(rootFolder, fileRef[0]);

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

		// otherwise, assume the default file name or try a new one if that's taken
		return getNewFile(obj, ref, property);
	}

	protected File getNewFile(IContestObject obj, FileReference ref2, String property) {
		String[] fileRef = getLocalPattern(obj, property);
		if (fileRef == null)
			return null;
		File rootFolder = getRootFolder();
		File folder = rootFolder;
		if (fileRef[0] != null)
			folder = new File(rootFolder, fileRef[0]);

		File file = new File(folder, fileRef[1].replace("{0}", ""));
		int n = 2;
		while (file.exists()) {
			file = new File(folder, fileRef[1].replace("{0}", "" + n++));
		}
		return file;
	}

	@Override
	public File getFile(String path) throws IOException {
		String path2 = path;
		if (path.startsWith("submissions/")) {
			if (path.endsWith("files"))
				path2 += ".zip";
			if (path.endsWith("reaction"))
				path2 += ".mts";
		}
		if (path.startsWith("organizations/")) {
			if (path.endsWith("logo"))
				path2 += ".png";
		}
		if (path.startsWith("teams/")) {
			if (path.endsWith("photo"))
				path2 += ".jpg";
			if (path.endsWith("video"))
				path2 += ".mts";
			if (path.endsWith("backup"))
				path2 += ".tar.gz";
		}
		if (path2.endsWith("/"))
			path2 += "listing.dir";
		return new File(root, path2);
	}

	@Override
	public String[] getDirectory(String path) throws IOException {
		File f = new File(root, path);

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

	private static List<FileReference> readCache(File folder) throws IOException {
		List<FileReference> list = new ArrayList<>();
		BufferedReader br = null;

		String s = null;
		try {
			File f = new File(folder, CACHE_FILE);
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

	protected static List<FileReference> getCache(File folder) {
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

	private static void writeCache(File folder, List<FileReference> list) {
		BufferedWriter bw = null;
		FileOutputStream fout = null;

		try {
			File f = new File(folder, CACHE_FILE);
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
		else if (name.endsWith(".m2ts"))
			return "video/MP2T";
		return null;
	}

	protected static FileReference readMetadata(File file) {
		FileReference ref = new FileReference();
		ref.mime = getMimeType(file.getName());

		ref.file = file;
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
		return ref;
	}

	protected FileReference getOrgImage(String base, String orgId, String teamId) {
		FileReference ref = getFileWithPattern("images" + File.separator + base, orgId + ".png",
				"organizations/" + orgId + "/" + base);
		if (ref != null)
			return ref;

		if (teamId == null)
			return null;

		ref = getFileWithPattern("images" + File.separator + base, teamId + ".png",
				"organizations/" + orgId + "/" + base);
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

		// Trace.trace(Trace.INFO, "Time to load EF 0: " + (System.currentTimeMillis() - time) +
		// "ms");

		// load event feed
		File file = getRootFolder();
		File contestFile = new File(file, "event-feed.json");
		if (!contestFile.exists())
			contestFile = new File(file, "events.xml");
		if (!contestFile.exists()) {
			if (expectFeed)
				Trace.trace(Trace.WARNING, "No local event feed found");
			contest.removeModifier(mod);
			return;
		}

		InputStream in;
		try {
			in = new FileInputStream(contestFile);
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Could not read event feed", e);
			contest.removeModifier(mod);
			throw e;
		}
		try {
			if (contestFile.getName().endsWith("json")) {
				NDJSONFeedParser jsonParser = new NDJSONFeedParser();
				jsonParser.parse(contest, in);
				parser = jsonParser;
			} else {
				XMLFeedParser xmlParser = new XMLFeedParser();
				xmlParser.parse(contest, in);
				parser = xmlParser;
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

	public FileReferenceList getFilesWithPattern(IContestObject obj, String property) {
		final String[] s = getLocalPattern(obj, property);
		if (s == null) {
			Trace.trace(Trace.ERROR, "No file pattern: " + obj.getType() + " " + property);
			return null;
		}
		return getFilesWithPattern(s[0], s[1], s[2]);
	}

	public FileReference getFileWithPattern(String folderName, String filename, String url) {
		File folder = getRootFolder();
		if (folderName != null)
			folder = new File(folder, folderName);

		File[] files = folder.listFiles((dir, name) -> (name.equalsIgnoreCase(filename)));
		if (files == null || files.length == 0)
			return null;

		return getMetadata(url, files[0]);
	}

	public FileReferenceList getFilesWithPattern(String folderName, String filename, String url) {
		File folder = getRootFolder();
		if (folderName != null)
			folder = new File(folder, folderName);

		FileReferenceList refList = new FileReferenceList();

		int ind = filename.indexOf("{0}");
		if (ind < 0) { // no substitutions
			FileReference ref = getFileWithPattern(folderName, filename, url);
			if (ref != null)
				refList.add(ref);
			return refList;
		}

		String start = filename.substring(0, ind);
		String end = filename.substring(ind + 3);
		File[] files = folder
				.listFiles((dir, name) -> (name.toLowerCase().startsWith(start) && name.toLowerCase().endsWith(end)));

		if (files == null)
			return null;

		for (File file : files) {
			String subs = file.getName();
			subs = subs.substring(start.length(), subs.length() - end.length());
			refList.add(getMetadata(url.replace("{0}", subs), file));
		}
		return refList;
	}

	/**
	 * Returns an array containing 3 strings: a local folder, a file pattern, and the remote URL
	 * pattern.
	 *
	 * @param obj
	 * @param property
	 * @return
	 */
	protected String[] getLocalPattern(IContestObject obj, String property) {
		if (obj instanceof Info) {
			if ("banner".equals(property))
				return new String[] { null, "banner{0}.png", "banner{0}" };
			else if ("logo".equals(property))
				return new String[] { null, "logo{0}.png", "logo{0}" };
		} else if (obj instanceof Team) {
			if ("photo".equals(property))
				return new String[] { "teams" + File.separator + obj.getId(), "photo{0}.jpg",
						"teams/" + obj.getId() + "/photo{0}" };
			if ("video".equals(property))
				return new String[] { "teams" + File.separator + obj.getId(), "video.m2ts",
						"teams/" + obj.getId() + "/video" };
			if ("backup".equals(property))
				return new String[] { "teams" + File.separator + obj.getId(), "backup.zip",
						"teams/" + obj.getId() + "/backup" };
			if ("logkeys".equals(property))
				return new String[] { "teams" + File.separator + obj.getId(), "logkeys.txt",
						"teams/" + obj.getId() + "/logkeys" };
		} else if (obj instanceof TeamMember) {
			if ("photo".equals(property))
				return new String[] { "team-members" + File.separator + obj.getId(), "photo{0}.jpg",
						"team-members/" + obj.getId() + "/photo{0}" };
		} else if (obj instanceof Organization) {
			if ("logo".equals(property))
				return new String[] { "organizations" + File.separator + obj.getId(), "logo{0}.png",
						"organizations/" + obj.getId() + "/logo{0}" };
		} else if (obj instanceof Submission) {
			if ("files".equals(property))
				return new String[] { "submissions" + File.separator + obj.getId(), "files.zip",
						"submissions/" + obj.getId() + "/files" };
			if ("reaction".equals(property))
				return new String[] { "submissions" + File.separator + obj.getId(), "reaction.m2ts",
						"submissions/" + obj.getId() + "/reaction" };
		} else if (obj instanceof Group) {
			if ("logo".equals(property))
				return new String[] { "groups" + File.separator + obj.getId(), "logo{0}.png",
						"groups/" + obj.getId() + "/logo{0}" };
		}
		return null;
	}

	public void attachLocalResources(IContestObject obj) {
		if (obj instanceof Info) {
			Info info = (Info) obj;
			info.setBanner(getFilesWithPattern(obj, "banner"));
			info.setLogo(getFilesWithPattern(obj, "logo"));
		} else if (obj instanceof Organization) {
			FileReferenceList list = getFilesWithPattern(obj, "logo");
			if (list == null)
				list = new FileReferenceList();

			Organization org = (Organization) obj;
			String orgId = org.getId();
			String teamId = null;
			for (ITeam t : contest.getTeams()) {
				if (orgId.equals(t.getOrganizationId())) {
					teamId = t.getId();
				}
			}
			FileReference ref = getOrgImage("logo", orgId, teamId);
			if (ref != null)
				list.add(ref);
			ref = getOrgImage("tile", orgId, teamId);
			if (ref != null)
				list.add(ref);
			ref = getOrgImage("icon", orgId, teamId);
			if (ref != null)
				list.add(ref);

			if (!list.isEmpty())
				org.setLogo(list);
			else
				org.setLogo(null);
		} else if (obj instanceof Team) {
			Team team = (Team) obj;
			FileReferenceList list = getFilesWithPattern(obj, "photo");
			if (list == null)
				list = new FileReferenceList();
			FileReference ref = getFileWithPattern("images" + File.separator + "team", obj.getId() + ".jpg",
					"teams/" + obj.getId() + "/photo");
			if (ref != null)
				list.add(ref);
			if (list.isEmpty())
				team.setPhoto(null);
			else
				team.setPhoto(list);

			ref = getFileWithPattern("video" + File.separator + "team", obj.getId() + ".m2ts",
					"teams/" + obj.getId() + "/video");
			if (ref != null)
				team.setVideo(new FileReferenceList(ref));
			else
				team.setVideo(null);

			list = getFilesWithPattern(obj, "backup");
			if (list == null || list.isEmpty())
				team.setBackup(null);
			else
				team.setBackup(list);

			list = getFilesWithPattern(obj, "logkeys");
			if (list == null || list.isEmpty())
				team.setKeyLog(null);
			else
				team.setKeyLog(list);
		} else if (obj instanceof TeamMember) {
			TeamMember member = (TeamMember) obj;
			FileReference ref = getFileWithPattern("images" + File.separator + "team-member", obj.getId() + ".jpg",
					"team-members/" + obj.getId() + "/photo");
			if (ref != null)
				member.setPhoto(new FileReferenceList(ref));
			else
				member.setPhoto(null);
		} else if (obj instanceof Submission) {
			Submission s = (Submission) obj;
			FileReferenceList refList = getFilesWithPattern(obj, "files");
			if (refList == null) {
				FileReference ref = getFileWithPattern("submissions", obj.getId() + ".zip",
						"submissions/" + obj.getId() + "/files");
				if (ref != null) {
					refList = new FileReferenceList();
					refList.add(ref);
				}
			}

			if (refList == null || refList.isEmpty())
				s.setFiles(null);
			else
				s.setFiles(refList);

			refList = getFilesWithPattern(obj, "reaction");
			if (refList == null || refList.isEmpty())
				s.setReaction(null);
			else
				s.setReaction(refList);
		} else if (obj instanceof Group) {
			FileReferenceList list = getFilesWithPattern(obj, "logo");

			Group group = (Group) obj;
			if (list != null && !list.isEmpty())
				group.setLogo(list);
			else
				group.setLogo(null);
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

	private static File getConfigFile(File root, String file) {
		return new File(root, "config" + File.separator + file);
	}

	protected void loadConfigFiles() {
		if (isCache())
			return;

		configValidation = new Validation();
		try {
			Trace.trace(Trace.INFO, "Importing contest info");
			Info info = YamlParser.importContestInfo(root);
			contest.add(info);
			configValidation.ok("Contest info loaded");
		} catch (FileNotFoundException e) {
			configValidation.ok("Contest info not found");
			Trace.trace(Trace.INFO, e.getMessage());
		} catch (Exception e) {
			configValidation.err("Error importing contest info: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing contest info", e);
		}

		try {
			Trace.trace(Trace.INFO, "Importing problem set");
			List<IProblem> problems = YamlParser.importProblems(root);
			for (IProblem p : problems)
				contest.add(p);
		} catch (FileNotFoundException e) {
			Trace.trace(Trace.INFO, e.getMessage());
		} catch (Exception e) {
			configValidation.err("Error importing problem set: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing problem set", e);
		}

		try {
			Trace.trace(Trace.INFO, "Importing groups");
			File f = getConfigFile(root, "groups.json");
			if (f.exists())
				loadFile(contest, f, "groups");
			else {
				f = getConfigFile(root, "groups.tsv");
				if (f.exists())
					TSVImporter.importGroups(contest, f);
				else
					Trace.trace(Trace.INFO, "Group config file (groups.json/groups.tsv) not found");
			}
		} catch (Exception e) {
			configValidation.err("Error importing groups: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing groups", e);
		}

		try {
			Trace.trace(Trace.INFO, "Importing institutions");
			File f = getConfigFile(root, "institutions.json");
			if (f.exists())
				loadFile(contest, f, "institutions");
			else {
				f = getConfigFile(root, "institutions2.tsv");
				if (!f.exists())
					f = getConfigFile(root, "institutions.tsv");

				if (f.exists())
					TSVImporter.importInstitutions(contest, f);
				else
					Trace.trace(Trace.INFO,
							"Institutions config file (institutions.json/institutions2.tsv/institutions.tsv) not found");
			}
		} catch (Exception e) {
			configValidation.err("Error importing institutions: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing institutions", e);
		}

		try {
			Trace.trace(Trace.INFO, "Importing teams");
			File f = getConfigFile(root, "teams.json");
			if (f.exists())
				loadFile(contest, f, "teams");
			else {
				f = getConfigFile(root, "teams2.tsv");

				if (!f.exists())
					f = getConfigFile(root, "teams.tsv");

				if (f.exists())
					TSVImporter.importTeams(contest, f);
				else
					Trace.trace(Trace.INFO, "Team config file (teams.json/teams2.tsv/teams.tsv) not found");
			}
		} catch (Exception e) {
			configValidation.err("Error importing teams: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing teams", e);
		}

		try {
			Trace.trace(Trace.INFO, "Importing team-members");
			File f = getConfigFile(root, "members.json");
			if (f.exists())
				loadFile(contest, f, "members");
			else {
				f = getConfigFile(root, "members.tsv");
				if (f.exists())
					TSVImporter.importTeamMembers(contest, f);
				else
					Trace.trace(Trace.INFO, "Team member config file (members.json/members.tsv) not found");
			}
		} catch (Exception e) {
			configValidation.err("Error importing team-members: " + e.getMessage());
			Trace.trace(Trace.ERROR, "Error importing team-members", e);
		}

		try {
			Trace.trace(Trace.INFO, "Importing contest floor map");
			FloorMap map = FloorMap.importMap(root);
			if (map != null) {
				List<ITeam> teams = map.getTeams();
				for (ITeam t : teams) {
					ITeam tt = contest.getTeamById(t.getId());
					if (tt != null) {
						((ContestObject) tt).add("x", t.getX() + "");
						((ContestObject) tt).add("y", t.getY() + "");
						((ContestObject) tt).add("rotation", t.getRotation() + "");
					}
				}

				List<IProblem> problems = map.getProblems();
				for (IProblem p : problems) {
					for (IProblem pp : contest.getProblems()) {
						if (pp.getLabel().equals(p.getLabel())) {
							((Problem) pp).add("x", p.getX() + "");
							((Problem) pp).add("y", p.getY() + "");
						}
					}
				}
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
		return "DiskContestSource[" + root + "]";
	}
}