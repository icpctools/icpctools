package org.icpc.tools.cds.presentations;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.icpc.tools.cds.CDSConfig;
import org.icpc.tools.contest.Trace;

public class PresentationCache {
	private static File root;
	private static final String ADMIN_ZIP = "admin.zip";
	private static final String JAR_NAME = "presentContest.jar";

	private static File getPresentationZip() {
		if (root != null)
			return root;

		File folder = new File(CDSConfig.getFolder(), "present");
		if (!folder.exists()) {
			Trace.trace(Trace.ERROR, "Presentation folder (/present) missing");
			return null;
		}

		final File[] files = folder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				String name = pathname.getName();
				return name.startsWith("presentations-") && name.endsWith(".zip");
			}
		});

		if (files != null && files.length > 0) {
			// pick latest version and compare with last unzipped
			Arrays.sort(files, new Comparator<File>() {
				@Override
				public int compare(File f1, File f2) {
					String s1 = f1.getName();
					String s2 = f2.getName();
					return -s1.compareTo(s2);
				}
			});
			return files[0];
		}
		Trace.trace(Trace.WARNING, "No presentation clients found in /present");
		return null;
	}

	public static File getPresentationCacheAdmin() {
		File cache = getPresentationCache();
		if (cache == null)
			return null;

		File fAdmin = new File(cache, ADMIN_ZIP);
		File ff = new File(cache, JAR_NAME);
		createAdminArchive(ff, fAdmin);
		return fAdmin;
	}

	public static File getPresentationCache() {
		File f = getPresentationZip();
		if (f == null)
			return null;

		File cache = new File(f.getParentFile(), f.getName() + "-cache");
		if (!cache.exists())
			cache.mkdirs();

		unzipJar(f, cache);
		return cache;
	}

	private static File unzipJar(File from, File toCache) {
		ZipFile zipFileFrom = null;
		File target = new File(toCache, JAR_NAME);
		if (target.exists())
			return target;

		try {
			zipFileFrom = new ZipFile(from);
			Enumeration<? extends ZipEntry> zipEntries = zipFileFrom.entries();
			while (zipEntries.hasMoreElements()) {
				ZipEntry zipEntry = zipEntries.nextElement();
				if (zipEntry.getName().endsWith("lib/" + JAR_NAME)) {
					InputStream in = zipFileFrom.getInputStream(zipEntry);
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target));
					byte[] b = new byte[8096];
					int n = in.read(b);
					while (n > 0) {
						out.write(b, 0, n);
						n = in.read(b);
					}
					in.close();
					out.close();
					target.setLastModified(from.lastModified());
					return target;
				}
			}
			Trace.trace(Trace.ERROR, "Could not find presentation jar");
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error extracting presentation jar", e);
		} finally {
			try {
				if (zipFileFrom != null)
					zipFileFrom.close();
			} catch (Exception e) {
				// ignore
			}
		}
		return null;
	}

	private static void createAdminArchive(File from, File toAdmin) {
		File toCache = from.getParentFile();
		ZipFile zipFileFrom = null;
		ZipOutputStream zipOutAdmin = null;
		try {
			List<String> images = null;

			zipFileFrom = new ZipFile(from);
			ZipEntry pEntry = zipFileFrom.getEntry("META-INF/presentations.xml");
			if (pEntry != null) {
				InputStream in = zipFileFrom.getInputStream(pEntry);
				images = PresentationsParser.loadImages(in);
				in.close();
			}

			zipOutAdmin = new ZipOutputStream(new FileOutputStream(toAdmin));
			Enumeration<? extends ZipEntry> entries = zipFileFrom.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String name = entry.getName();
				if ("META-INF/presentations.xml".equals(name) || images.contains(name)) {
					InputStream in = zipFileFrom.getInputStream(entry);
					File toFile = new File(toCache, name);
					if (!toFile.getParentFile().exists())
						toFile.getParentFile().mkdirs();
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(toFile));
					zipOutAdmin.putNextEntry(new ZipEntry(name));

					byte[] b = new byte[8096];
					int n = in.read(b);
					while (n > 0) {
						zipOutAdmin.write(b, 0, n);
						out.write(b, 0, n);
						n = in.read(b);
					}
					zipOutAdmin.closeEntry();
					out.close();
				}
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error loading presentation list", e);
		} finally {
			try {
				if (zipFileFrom != null)
					zipFileFrom.close();
			} catch (Exception e) {
				// ignore
			}
			try {
				if (zipOutAdmin != null)
					zipOutAdmin.close();
			} catch (Exception e) {
				// ignore
			}
		}
		toAdmin.setLastModified(from.lastModified());
	}
}