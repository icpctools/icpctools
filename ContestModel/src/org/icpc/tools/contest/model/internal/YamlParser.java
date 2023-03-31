package org.icpc.tools.contest.model.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.feed.RelativeTime;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Uses SnakeYAML to parse the contest YAML files.
 */
public class YamlParser {
	public static Info importContestInfo(File f, boolean oldFormat) throws IOException {
		if (f == null || !f.exists())
			throw new FileNotFoundException("Contest config file not found");

		BufferedReader br = new BufferedReader(new FileReader(f));
		Info info = parseInfo(br, oldFormat);
		br.close();

		return info;
	}

	public static Info parseInfo(Reader br, boolean oldFormat) throws IOException {
		Yaml yaml = new Yaml(new SafeConstructor(), new Representer(), new DumperOptions(), new CustomYamlResolver());
		Object obj = yaml.load(br);

		// the file should have a top-level map of problems, which contains a list of problems
		if (!(obj instanceof Map<?, ?>))
			throw new IOException("Contest config file not imported: invalid format");

		Map<?, ?> map = (Map<?, ?>) obj;

		Info info = new Info();
		info.add("id", "1");
		for (Object ob : map.keySet()) {
			if (ob instanceof String) {
				String key = (String) ob;
				Object val = map.get(key);
				String value = null;
				if (val != null)
					value = val.toString();

				try {
					if ("name".equals(key) && oldFormat)
						info.add("formal_name", value);
					else if ("short-name".equals(key))
						info.add("name", value);
					else if ("length".equals(key) || "duration".equals(key)) {
						long length = RelativeTime.parse(value);
						if (length >= 0)
							info.add("duration", RelativeTime.format(length));
					} else if ("scoreboard-freeze".equals(key)) {
						long length = RelativeTime.parse(value);
						long d = info.getDuration();
						if (length >= 0 && d > 0)
							info.add("scoreboard_freeze_duration", RelativeTime.format(d / 1000 - length));
					} else if ("scoreboard-freeze-length".equals(key)) {
						long length = RelativeTime.parse(value);
						if (length >= 0)
							info.add("scoreboard_freeze_duration", RelativeTime.format(length));
					} else if ("penalty-time".equals(key)) {
						info.add("penalty_time", value);
					} else if ("start-time".equals(key)) {
						info.add("start_time", value);
					} else if ("banner".equals(key)) {
						info.setBanner(parseFileReferenceList((List<?>) val));
					} else if ("logo".equals(key)) {
						info.setLogo(parseFileReferenceList((List<?>) val));
					} else
						info.add(key, value);
				} catch (Exception ex) {
					Trace.trace(Trace.ERROR, "Could not parse " + key + ": " + value);
				}
			}
		}

		return info;
	}

	private static FileReferenceList parseFileReferenceList(List<?> list) {
		FileReferenceList refList = new FileReferenceList();

		for (Object obj : list) {
			Map<?, ?> map = (Map<?, ?>) obj;

			JsonObject jo = new JsonObject();
			for (Object ob : map.keySet()) {
				if (ob instanceof String) {
					String key = (String) ob;
					Object val = map.get(key);
					String value = null;
					if (val != null)
						value = val.toString();

					try {
						jo.put(key, val);
					} catch (Exception ex) {
						Trace.trace(Trace.ERROR, "Could not parse " + key + ": " + value);
					}
				}
			}

			FileReference ref = new FileReference(jo);
			refList.add(ref);
		}
		return refList;
	}

	// .timelimit
	public static List<IProblem> importProblems(File f) throws IOException {
		if (f == null || !f.exists())
			throw new FileNotFoundException("Problem config file not found");

		BufferedReader br = new BufferedReader(new FileReader(f));

		Yaml yaml = new Yaml(new SafeConstructor());
		Object obj = yaml.load(br);

		// problemset.yaml has a top-level problems element
		if (obj instanceof Map<?, ?>) {
			Map<?, ?> map = (Map<?, ?>) obj;
			Object probs = map.get("problems");
			if (probs == null)
				throw new IOException("Problem config file invalid top-level element");

			obj = probs;
		}

		if (!(obj instanceof List<?>))
			throw new IOException("Problem config file not imported: no problems");

		List<?> list = (List<?>) obj;

		int i = 0;
		List<IProblem> problems = new ArrayList<>();

		for (Object o : list) {
			if (o instanceof Map<?, ?>) {
				Map<?, ?> map = (Map<?, ?>) o;

				Problem problem = new Problem();
				problem.add("ordinal", "" + i);
				i++;

				for (Object ob : map.keySet()) {
					if (ob instanceof String) {
						String key = (String) ob;
						Object val = map.get(key);
						String value = null;
						if (val != null)
							value = val.toString();

						if ("letter".equals(key))
							problem.add("label", value);
						else if ("short-name".equals(key)) {
							if (problem.getId() == null)
								problem.add("id", value);
							if (problem.getName() == null)
								problem.add("name", value);
						} else
							problem.add(key, value);
					}
				}

				if (problem.getId() != null && (problem.getTestDataCount() <= 0 || problem.getTimeLimit() <= 0)) {
					File problemFolder = new File(f.getParentFile(), problem.getId());
					if (problemFolder.exists()) {
						addProblemTestDataCount(problemFolder, problem);
						addProblemTimeLimit(problemFolder, problem);
						try {
							importProblem(problemFolder, problem);
						} catch (Exception e) {
							// ignore for now
						}
					}
				}
				problems.add(problem);
			}
		}
		br.close();
		return problems;
	}

	/**
	 * Count the pairs of .in and .ans files in the given folder. smaller number.
	 *
	 * @param folder
	 * @return
	 */
	private static int countTestCases(File folder) {
		if (folder == null || !folder.exists())
			return 0;

		File[] files = folder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".in");
			}
		});
		if (files == null)
			return 0;

		int count = 0;
		for (File inf : files) {
			String name = inf.getName();
			File f = new File(folder, name.substring(0, name.length() - 3) + ".ans");
			if (f.exists())
				count++;
		}

		return count;
	}

	private static void addProblemTestDataCount(File problemFolder, Problem p) {
		int count = 0;
		File sampleFolder = new File(problemFolder, "data" + File.separator + "sample");
		if (sampleFolder.exists())
			count += countTestCases(sampleFolder);

		File secretFolder = new File(problemFolder, "data" + File.separator + "secret");
		if (secretFolder.exists())
			count += countTestCases(secretFolder);

		if (count > 0)
			p.add("test_data_count", count + "");
	}

	private static void addProblemTimeLimit(File problemFolder, Problem p) {
		File f = new File(problemFolder, ".timelimit");
		if (!f.exists())
			return;

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(f));
			String line = br.readLine();
			p.add("time_limit", line);
		} catch (Exception e) {
			// ignore for now
		} finally {
			try {
				br.close();
			} catch (Exception ex) {
				// ignore
			}
		}
	}

	private static void importProblem(File problemFolder, Problem p) throws IOException {
		File f = new File(problemFolder, "problem.yaml");
		if (!f.exists())
			throw new FileNotFoundException("Problem file (problem.yaml) not found: " + f.getAbsolutePath());

		BufferedReader br = new BufferedReader(new FileReader(f));

		Yaml yaml = new Yaml(new SafeConstructor());
		Object obj = yaml.load(br);

		// the file should have a top-level map of problems, which contains a list of problems
		if (!(obj instanceof Map<?, ?>))
			throw new IOException("Problem file (problem.yaml) not imported: invalid format");

		Map<?, ?> map = (Map<?, ?>) obj;
		obj = map.get("name");
		// time_limit
		if (obj != null) {
			if (obj instanceof String) {
				p.add("name", obj.toString());
			} else if (obj instanceof Map<?, ?>) {
				// TODO map = (Map<?, ?>) obj;
			}
		}

		br.close();
	}

	public static List<IAccount> importAccounts(File f) throws IOException {
		if (f == null || !f.exists())
			throw new FileNotFoundException("Accounts config file not found");

		BufferedReader br = new BufferedReader(new FileReader(f));

		Yaml yaml = new Yaml(new SafeConstructor());
		Object obj = yaml.load(br);

		// the file should have a top-level list of accounts
		if (!(obj instanceof List<?>))
			throw new IOException("Accounts file (accounts.yaml) not imported: invalid format");

		List<IAccount> accounts = new ArrayList<>();

		List<?> list = (List<?>) obj;
		for (Object o : list) {
			if (o instanceof Map<?, ?>) {
				Map<?, ?> map = (Map<?, ?>) o;

				Account account = new Account();
				for (Object ob : map.keySet()) {
					if (ob instanceof String) {
						String key = (String) ob;
						Object val = map.get(key);
						if (val != null)
							account.add(key, val.toString());
					}
				}
				// if there's no id, use username
				if (account.getId() == null && account.getUsername() != null)
					account.add("id", account.getUsername());
				accounts.add(account);
			}
		}

		br.close();
		return accounts;
	}
}