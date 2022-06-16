package org.icpc.tools.contest.model.feed;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.icpc.tools.contest.Trace;

/**
 * CDS helper class to confirm CDS connection and check for client updates.
 */
public class CDSUtil {
	protected static final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
	static {
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(1);
		nf.setGroupingUsed(true);
	}

	private String url;
	private String user = null;
	private String password = null;

	// Based on https://stackoverflow.com/a/11024200
	static class Version implements Comparable<Version> {
		private String version;

		public Version(String version) {
			if (version == null)
				throw new IllegalArgumentException("Version can not be null");
			if (!version.matches("[0-9]+(\\.[0-9]+)*"))
				throw new IllegalArgumentException("Invalid version format '" + version + "'");
			this.version = version;
		}

		@Override
		public int compareTo(Version that) {
			if (that == null)
				return 1;
			String[] thisParts = this.version.split("\\.");
			String[] thatParts = that.version.split("\\.");
			int length = Math.max(thisParts.length, thatParts.length);
			for (int i = 0; i < length; i++) {
				int thisPart = i < thisParts.length ? Integer.parseInt(thisParts[i]) : 0;
				int thatPart = i < thatParts.length ? Integer.parseInt(thatParts[i]) : 0;
				if (thisPart < thatPart)
					return -1;
				if (thisPart > thatPart)
					return 1;
			}
			return 0;
		}

		@Override
		public boolean equals(Object that) {
			if (this == that)
				return true;
			if (that == null)
				return false;
			if (!(that instanceof Version))
				return false;
			return compareTo((Version) that) == 0;
		}

		@Override
		public String toString() {
			return version;
		}

		@Override
		public int hashCode() {
			return version.hashCode();
		}
	}

	/**
	 * Creates a REST contest source with a local caching policy.
	 *
	 * @param url
	 * @param user
	 * @param password
	 * @throws MalformedURLException
	 */
	public CDSUtil(String url, String user, String password) {
		this.url = url;

		if (user != null && user.trim().length() > 0)
			this.user = user;

		if (password != null && password.trim().length() > 0)
			this.password = password;
	}

	public void verifyCDS() throws Exception {
		if (url == null)
			throw new IOException("Invalid url");

		try {
			HttpURLConnection conn = HTTPSSecurity.createConnection(new URL(url), user, password);
			int response = conn.getResponseCode();
			if ("CDS".equals(conn.getHeaderField("ICPC-Tools")))
				return;

			if (response == HttpURLConnection.HTTP_UNAUTHORIZED)
				throw new IOException("User or password is incorrect");
			else if (response != HttpURLConnection.HTTP_OK)
				throw new IOException("Invalid HTTP response code " + response);

			throw new IOException("Server is not a CDS");
		} catch (Exception e) {
			throw e;
		}
	}

	private HttpURLConnection createConnection(String href) throws IOException {
		try {
			URL url3 = new URL(url);
			String url2 = url3.getProtocol() + "://" + url3.getAuthority() + href;
			return HTTPSSecurity.createConnection(new URL(url2), user, password);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Connection error", e);
		}
	}

	private String[] getDirectory(String path) {
		try {
			String path2 = path;
			if (!path2.endsWith("/"))
				path2 += "/";
			File file = download(path2);
			if (!file.exists())
				return null;

			return LinkParser.parse(file);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error reading filenames", e);
		}
		return null;
	}

	private static Class<?> getCallerClass() {
		try {
			StackTraceElement[] stes = Thread.currentThread().getStackTrace();
			for (int i = 1; i < stes.length; i++) {
				String className = stes[i].getClassName();
				if (className.indexOf("CDSUtil") < 0) {
					return Class.forName(className);
				}
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	private static String getVersion(String ver) {
		if (ver == null)
			return "dev";
		return ver;
	}

	private static String getVersion() {
		Class<?> c = getCallerClass();
		Package pack = c.getPackage();
		String spec = pack.getSpecificationVersion();
		String impl = pack.getImplementationVersion();
		if (spec == null && impl == null)
			try {
				java.util.Properties prop = new java.util.Properties();
				prop.load(c.getResourceAsStream("/META-INF/MANIFEST.MF"));
				spec = prop.getProperty("Specification-Version");
				impl = prop.getProperty("Implementation-Version");
			} catch (Exception e) {
				// ignore
			}

		return getVersion(spec) + "." + getVersion(impl);
	}

	private File download(String href) throws IOException {
		File localFile = File.createTempFile("cds", "tmp");
		StringBuilder sb = new StringBuilder("Download " + href + " to " + localFile);
		if (localFile.exists())
			localFile.delete();
		if (!localFile.getParentFile().exists())
			localFile.getParentFile().mkdirs();

		long time = System.currentTimeMillis();
		HttpURLConnection conn = createConnection(href);
		conn.setReadTimeout(10000);

		int status = conn.getResponseCode();
		sb.append(" (" + status + ")");

		if (status == HttpURLConnection.HTTP_NOT_FOUND) {
			Trace.trace(Trace.ERROR, sb.toString());
			return null;
		}

		if (status == HttpURLConnection.HTTP_UNAUTHORIZED) {
			Trace.trace(Trace.ERROR, sb.toString() + " not authorized!");
			return null;
		}

		InputStream in = conn.getInputStream();
		FileOutputStream out = new FileOutputStream(localFile);

		byte[] buf = new byte[8096];
		int n = in.read(buf);
		while (n >= 0) {
			out.write(buf, 0, n);
			n = in.read(buf);
		}
		in.close();
		out.close();
		long mod = conn.getLastModified();
		if (mod != 0)
			localFile.setLastModified(mod);
		time = System.currentTimeMillis() - time;
		String size = nf.format(localFile.length() / 1024.0);
		sb.append(" (" + size + "kb in " + time + "ms)");
		Trace.trace(Trace.INFO, sb.toString());
		localFile.deleteOnExit();
		return localFile;
	}

	/**
	 * Checks for existence of a specific zip file pattern on the server. If one or more exist, the
	 * newest copy's version will be compared with the local version. If the remote version is
	 * newer, it will be downloaded to the /update folder, and the process will exit with code 254.
	 * The calling script must support replacing the folders contents with the update and
	 * restarting.
	 *
	 * @param prefix the file pattern, e.g. "presentations-".
	 */
	public void checkForUpdates(String prefix) {
		try {
			// remove any previous updates
			File updateDir = new File("update");
			if (updateDir.exists()) {
				Files.walkFileTree(updateDir.toPath(), new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					}
				});
			}

			// check on CDS for which versions are available
			String[] files = getDirectory("/presentation");
			List<Version> presVersions = new ArrayList<>();
			if (files != null && files.length > 0)
				for (String f : files) {
					if (f.startsWith(prefix) && f.endsWith(".zip")) {
						String version = f.substring(prefix.length(), f.length() - 4);
						if (version.startsWith("v"))
							version = version.substring(1);
						presVersions.add(new Version(version));
					}
				}

			Trace.trace(Trace.INFO, "Updates found on CDS: " + presVersions.size());
			if (presVersions.size() > 0) {
				// pick latest version and compare with local
				presVersions.sort((s1, s2) -> -s1.compareTo(s2));
				String localVersionString = getVersion();
				Version remoteVersion = presVersions.get(0);
				Trace.trace(Trace.INFO,
						"Version check: " + localVersionString + " (local) vs " + remoteVersion + " (remote)");
				if (localVersionString.contains("dev"))
					return;

				Version localVersion = new Version(localVersionString);
				if (localVersion.compareTo(remoteVersion) < 0) {
					// download and unzip new version, restart
					Trace.trace(Trace.USER,
							"Newer version found on CDS (" + remoteVersion + "). Downloading and restarting...");
					File f = download("/presentation/" + prefix + remoteVersion + ".zip");

					// unzip to /update
					unzip(f, updateDir);

					System.exit(254);
				}
			}
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Failure while checking for updates", e);
		}
	}

	private static void unzip(File zipFile2, File folder) throws IOException {
		ZipFile zipFile = new ZipFile(zipFile2);
		Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		boolean commonRootFolder = true;
		String rootFolder = null;
		while (zipEntries.hasMoreElements()) {
			ZipEntry zipEntry = zipEntries.nextElement();
			String name = zipEntry.getName();
			int ind = name.indexOf("/");
			if (ind < 0) {
				commonRootFolder = false;
				break;
			}
			name = name.substring(ind + 1);
			if (rootFolder == null)
				rootFolder = name;
			else if (!name.equals(rootFolder)) {
				commonRootFolder = false;
				break;
			}
		}

		zipFile = new ZipFile(zipFile2);
		zipEntries = zipFile.entries();
		while (zipEntries.hasMoreElements()) {
			ZipEntry zipEntry = zipEntries.nextElement();

			if (!zipEntry.isDirectory()) {
				String name = zipEntry.getName();
				if (commonRootFolder)
					name = name.substring(rootFolder.length() + 1);
				File f = new File(folder, name);
				if (!f.getParentFile().exists())
					f.getParentFile().mkdirs();
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
				BufferedInputStream bin = new BufferedInputStream(zipFile.getInputStream(zipEntry));
				byte[] b = new byte[1024 * 8];
				int n = bin.read(b);
				while (n != -1) {
					out.write(b, 0, n);
					n = bin.read(b);
				}

				out.close();
				bin.close();
				f.setLastModified(zipEntry.getTime());
				if (f.getName().endsWith(".sh") || f.getName().endsWith(".bat"))
					f.setExecutable(true, false);
			}
		}
	}

	@Override
	public String toString() {
		return "CDSUtil[" + user + "|" + password + "@" + url + "]";
	}
}