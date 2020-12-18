package org.icpc.tools.balloon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.icpc.tools.contest.model.IContest;

public class BalloonFileUtil {
	private static final String FILE_NAME = "org.icpc.tools.balloons.{0}.txt";
	private static final String TEMP_FILE_NAME = "org.icpc.tools.balloons.temp.txt";

	public static void cleanAll() {
		File temp = new File(System.getProperty("java.io.tmpdir"));
		File[] files = temp.listFiles(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.getName().startsWith("org.icpc.tools.balloons.") && f.getName().endsWith(".txt");
			}
		});

		for (File f : files)
			f.delete();
	}

	private static final File getFile(IContest contest) {
		String id = contest.getId();
		if (id == null)
			id = contest.getName();
		if (id == null)
			id = "null";

		String cleanId = id.replaceAll("[^a-zA-Z0-9.-]", "");
		String name = FILE_NAME.replace("{0}", cleanId);
		return new File(System.getProperty("java.io.tmpdir"), name);
	}

	public static boolean saveFileExists(IContest contest) {
		return getFile(contest).exists();
	}

	public static List<Balloon> loadBalloons(IContest contest) throws IOException {
		File f = getFile(contest);

		if (!f.exists())
			throw new IOException("No balloon file found at " + f.getAbsolutePath());

		BufferedReader br = null;
		List<Balloon> list = new ArrayList<>(50);
		try {
			br = new BufferedReader(new FileReader(f));
			br.readLine(); // date comment

			String s = br.readLine();
			while (s != null) {
				list.add(new Balloon(s, null));
				s = br.readLine();
			}
		} catch (Exception e) {
			ErrorHandler.error("Error loading balloon list", e);
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (Exception e) {
				// ignore
			}
		}

		return list;
	}

	public static void saveBalloons(IContest contest, List<Balloon> list) throws IOException {
		File tempFile = new File(System.getProperty("java.io.tmpdir"), TEMP_FILE_NAME);

		if (tempFile.exists() && !tempFile.delete())
			throw new IOException("Could not delete temp file: " + tempFile.getAbsolutePath());

		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(tempFile));
			bw.write("# " + new Date().toString() + "\n");
			for (Balloon b : list) {
				bw.write(b.save());
				bw.write("\n");
			}
		} catch (Exception e) {
			ErrorHandler.error("Error saving balloon list", e);
		} finally {
			try {
				if (bw != null)
					bw.close();
			} catch (Exception e) {
				// ignore
			}
		}

		File f = getFile(contest);
		if (f.exists() && !f.delete())
			throw new IOException("Could not delete primary file: " + f.getAbsolutePath());
		if (!tempFile.renameTo(f))
			throw new IOException("Could not rename temp file to: " + f.getAbsolutePath());
	}
}