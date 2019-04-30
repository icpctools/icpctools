package org.icpc.tools.presentation.admin.internal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.presentation.core.internal.PresentationInfo;

public class PresentationListIO {
	private static final String TEMP_PREFIX = "org.icpc.tools.presentation.admin";
	private static final String FILENAME = "presentationPlans.txt";

	private static File getTempDir() {
		File tempDir = new File(System.getProperty("java.io.tmpdir"), TEMP_PREFIX);
		if (!tempDir.exists() && !tempDir.mkdir())
			return null;

		return tempDir;
	}

	protected static void save(List<CompositePresentationInfo> list) throws IOException {
		File f = new File(getTempDir(), FILENAME);

		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(f));
			bw.write(Integer.toString(list.size()));
			bw.newLine();
			for (CompositePresentationInfo plan : list) {
				bw.write(plan.getName());
				bw.newLine();
				if (plan.getDescription() != null)
					bw.write(plan.getDescription());
				else
					bw.write("");
				bw.newLine();
				bw.write(plan.getCategory());
				bw.newLine();
				bw.write(plan.getClassName());
				bw.newLine();

				for (PresentationInfo info : plan.infos) {
					bw.write(info.getId());
					bw.newLine();
					String[] data = info.getData();
					if (data != null && data.length > 0) {
						for (String s : data) {
							bw.write("-" + s);
							bw.newLine();
						}
					}
				}
				bw.newLine();
			}
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (bw != null)
					bw.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}

	protected static List<CompositePresentationInfo> load() throws IOException {
		File f = new File(getTempDir(), FILENAME);
		if (!f.exists())
			return new ArrayList<>();

		List<CompositePresentationInfo> list2 = new ArrayList<>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(f));
			String id = br.readLine();
			int num = Integer.parseInt(id);
			for (int i = 0; i < num; i++) {
				String name = br.readLine();
				String description = br.readLine();
				String category = br.readLine();
				String command = br.readLine();

				id = br.readLine();
				List<PresentationInfo> list = new ArrayList<>();
				while (id != null && !id.isEmpty()) {
					String s = br.readLine();
					List<String> prop = new ArrayList<>();
					while (s != null && s.startsWith("-")) {
						prop.add(s.substring(1));
						s = br.readLine();
					}
					String[] data = null;
					if (!prop.isEmpty())
						data = prop.toArray(new String[prop.size()]);
					PresentationInfo info = new PresentationInfo(id, null, null, null, null, null, null, false);
					info.setData(data);
					list.add(info);

					id = s;
				}
				CompositePresentationInfo pl = new CompositePresentationInfo(name, category, command, description, list);
				list2.add(pl);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (Exception e) {
				// ignore
			}
		}
		return list2;
	}
}