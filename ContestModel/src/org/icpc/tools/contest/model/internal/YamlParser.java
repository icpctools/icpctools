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
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.feed.RelativeTime;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Uses SnakeYAML to parse the contest YAML files.
 */
public class YamlParser {
	private static File getFile(File root, String file) {
		return new File(root, "config" + File.separator + file);
	}

	public static Info importContestInfo(File root) throws IOException {
		File f = getFile(root, "contest.yaml");
		if (f == null || !f.exists())
			throw new FileNotFoundException("Contest config file (contest.yaml) not found");

		BufferedReader br = new BufferedReader(new FileReader(f));
		Info info =  parseInfo(br);
		br.close();
		return info;
	}

	 private static int parseTime(String value) {
		  int ind = value.indexOf(":");
		  int h = Integer.parseInt(value.substring(0, ind));
		  int ind2 = value.indexOf(":", ind + 1);
		  int m = Integer.parseInt(value.substring(ind + 1, ind2));
		  int s = Integer.parseInt(value.substring(ind2 + 1));
		  int length = s + 60 * m + 60 * 60 * h;
		  return length;
	 }

	 public static Info parseInfo(Reader reader) throws IOException {
		  Yaml yaml = new Yaml(new Constructor(), new Representer(), new DumperOptions(), new CustomYamlResolver());
		  Object obj = yaml.load(reader);

		  // the file should have a top-level map of problems, which contains a list of problems
		  if (!(obj instanceof Map<?, ?>))
			 throw new IOException("Contest config file (contest.yaml) not imported: invalid format");

		  Map<?, ?> map = (Map<?, ?>) obj;

		  Info info = new Info();
		  info.add("id", "1");
		  boolean defaultPenaltyTime = true;
		  for (Object ob : map.keySet()) {
			 if (ob instanceof String) {
				  String key = (String) ob;
				  Object val = map.get(key);
				  String value = null;
				  if (val != null)
				  value = val.toString();
				  if ("name".equals(key))
				  info.add("formal_name", value);
				  else if ("short-name".equals(key))
				  info.add("name", value);
				  else if ("length".equals(key) || "duration".equals(key)) {
				  try {
						int length = parseTime(value);
						if (length >= 0)
							 info.add("duration", RelativeTime.format(length * 1000));
				  } catch (Exception ex) {
						Trace.trace(Trace.ERROR, "Could not parse duration: " + value);
				  }
				  } else if ("scoreboard-freeze".equals(key)) {
				  try {
						int length = parseTime(value);
						int d = info.getDuration();
						if (length >= 0 && d > 0)
							 info.add("scoreboard_freeze_duration", RelativeTime.format((d / 1000 - length) * 1000));
				  } catch (Exception ex) {
						Trace.trace(Trace.ERROR, "Could not parse freeze: " + value);
				  }
				  } else if ("scoreboard-freeze-length".equals(key)) {
				  try {
						int length = parseTime(value);
						if (length >= 0)
							 info.add("scoreboard_freeze_duration", RelativeTime.format(length * 1000));
				  } catch (Exception ex) {
						Trace.trace(Trace.ERROR, "Could not parse freeze length: " + value);
				  }
				  } else if ("penalty-time".equals(key)) {
						info.add("penalty_time", value);
						defaultPenaltyTime = false;
				  } else if ("start-time".equals(key)) {
						try {
							 info.add("start_time", value);
						} catch (Exception e) {
							 Trace.trace(Trace.ERROR, "Couldn't parse start time: " + value);
						}
				  }
			 }
		}

		  if (defaultPenaltyTime) {
		  	 info.add("penalty_time", "20");
		  }

		  return info;
	 }

	 // .timelimit
	public static List<IProblem> importProblems(File root) throws IOException {
		File f = getFile(root, "problemset.yaml");
		if (f == null || !f.exists())
			throw new FileNotFoundException("Problem config file (problemset.yaml) not found");

		BufferedReader br = new BufferedReader(new FileReader(f));

		Yaml yaml = new Yaml();
		Object obj = yaml.load(br);

		// the file should have a top-level map of problems, which contains a list of problems
		if (!(obj instanceof Map<?, ?>))
			throw new IOException("Problem config file (problemset.yaml) not imported: invalid format");

		Map<?, ?> map = (Map<?, ?>) obj;
		obj = map.get("problems");
		if (obj == null || !(obj instanceof List<?>))
			throw new IOException("Problem config file (problemset.yaml) not imported: no problems");

		List<?> list = (List<?>) obj;

		int i = 0;
		List<IProblem> problems = new ArrayList<>();

		for (Object o : list) {
			if (o instanceof Map<?, ?>) {
				map = (Map<?, ?>) o;

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
						else if ("color".equals(key))
							problem.add("color", value);
						else if ("rgb".equals(key))
							problem.add("rgb", value);
						else if ("short-name".equals(key)) {
							problem.add("id", value);
							problem.add("name", value);
						}
					}
				}

				File problemFolder = new File(root, "config" + File.separator + problem.getId());
				if (problemFolder.exists()) {
					addProblemTestDataCount(problemFolder, problem);
					addProblemTimeLimit(problemFolder, problem);
					try {
						importProblemYaml(problemFolder, problem);
					} catch (Exception e) {
						// ignore for now
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
	protected static int countTestCases(File folder) {
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

	protected static void addProblemTestDataCount(File problemFolder, Problem p) {
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

	protected static void addProblemTimeLimit(File problemFolder, Problem p) {
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

	protected static void importProblemYaml(File problemFolder, Problem p) throws IOException {
		File f = new File(problemFolder, "problem.yaml");
		if (!f.exists())
			throw new FileNotFoundException("Problem file (problem.yaml) not found: " + f.getAbsolutePath());

		BufferedReader br = new BufferedReader(new FileReader(f));

		Yaml yaml = new Yaml();
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
}