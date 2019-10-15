package org.icpc.tools.contest.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EventFeedSplitter {

	private static boolean debugMode;
	private Document doc;
	private int divNum;
	private int problemStart;
	private int problemEnd;
	private HashMap<String, XPathExpression> xpaths = new HashMap<>();
	private Transformer xmlTransformer;
	private boolean ndjsonFlag;
	HashSet<Integer> validTeams = new HashSet<Integer>();

	public EventFeedSplitter(String contestFile, String division, int probStart, int probEnd) throws Exception {
		problemStart = probStart;
		problemEnd = probEnd;
		if (division.equals("div1")) {
			divNum = 1;
		} else if (division.equals("div2")) {
			divNum = 2;
		} else {
			usage();
			incorrectUsage();
		}

		if (contestFile.endsWith("json")) {
			ndjsonFlag = true;
			InputStream in = null;
			try {
				in = new BufferedInputStream(new FileInputStream(contestFile));
				parseNDJSON(in);
			} catch (Exception e) {
				Trace.trace(Trace.WARNING, "Could not read event feed", e);
				throw e;
			}
		} else {
			ndjsonFlag = false;
			XPath xpath = XPathFactory.newInstance().newXPath();
			xpaths.put("id", xpath.compile("number(id)"));
			xpaths.put("teamId", xpath.compile("number(@team-id)"));
			xmlTransformer = TransformerFactory.newInstance().newTransformer();
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.parse(new File(contestFile));
		}
	}

	void parseNDJSON(InputStream in) throws IOException {
		BufferedReader br = null;
		String s = null;
		HashSet<String> validProblem = new HashSet<String>();
		HashSet<String> validSubmissions = new HashSet<String>();
		HashSet<String> validJudgements = new HashSet<String>();
		HashSet<String> validGroups = new HashSet<String>();
		try {
			br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			s = br.readLine();
			while (s != null) {
				if (s.isEmpty()) // heartbeat
					continue;

				JSONParser parser = new JSONParser(s);
				JsonObject obj = parser.readObject();
				String type = obj.getString("type");
				String op = obj.getString("op");
				JsonObject data = obj.getJsonObject("data");
				boolean skip = false;

				if ("delete".equals(op)) {
					// TODO should probably consider emitting the delete object
					skip = true;
				} else {
					ContestObject co = (ContestObject) IContestObject.createByName(type);
					if (co == null) {
						Trace.trace(Trace.WARNING, "Unrecognized (ignored) type in event feed: " + type);
						s = br.readLine();
						continue;
					}
					if (type.equals("groups") && data.containsKey("name")) {
						String value = data.getString("name");
						if (value.endsWith("D2") && divNum != 2) {
							if (debugMode) {
								System.err.println("skipping D2 value " + value + " with divnum=" + divNum);
							}
							skip = true;
						}
						if (value.endsWith("D1") && divNum != 1) {
							if (debugMode) {
								System.err.println("skipping D1 value " + value + " with divnum=" + divNum);
							}
							skip = true;
						}
						if (!skip) {
							String id = data.getString("id");
							if (debugMode) {
								System.err.println("adding group with id='" + id + "'");
							}
							validGroups.add(id);
						}
					}

					String teamIdKey = null;
					if (data.containsKey("team_id")) {
						teamIdKey = "team_id";
					}
					if (data.containsKey("to_team_id") && !data.isNull("to_team_id")) {
						teamIdKey = "to_team_id";
					}
					if (data.containsKey("from_team_id") && !data.isNull("from_team_id")) {
						teamIdKey = "from_team_id";
					}
					if (type.equals("teams") && data.containsKey("id")) {
						teamIdKey = "id";
					}
					if (type.equals("teams") && data.containsKey("group_ids")) {
						Object[] group_ids = data.getArray("group_ids");
						for (int i = 0; i < group_ids.length; i++) {
							String group_id = (String) group_ids[i];
							if (!validGroups.contains(group_id)) {
								skip = true;
							} else {
								validTeams.add(data.getInt("id"));
							}
						}
					}
					if (teamIdKey != null && !isDesiredTeam(Integer.valueOf(data.getString(teamIdKey)))) {
						skip = true;
					}
					if (!skip) {
						if (data.containsKey("problem_id") && !data.isNull("problem_id")
								&& !validProblem.contains(data.getString("problem_id"))) {
							if (debugMode) {
								System.err.println("skipping problem_id id='" + data.getString("problem_id") + "'");
							}
							skip = true;
						}
						if (type.equals("problems")) {
							String value = data.getString("ordinal");
							if (!isDesiredProblem(Integer.valueOf(value))) {
								if (debugMode) {
									System.err.println("skipping problems with value " + value);
								}
								skip = true;
							} else {
								String id = data.getString("id");
								if (debugMode) {
									System.err.println("saving problems with  id=" + id + "'");
								}
								validProblem.add(id);
							}
						}
						if (data.containsKey("group_id")) {
							String id = data.getString("group_id");
							if (!validGroups.contains(id)) {
								if (debugMode) {
									System.err.println("skipping group_id='" + id + "'");
								}
								skip = true;
							}
						}
						if (!skip && type.equals("submissions")) {
							String id = data.getString("id");
							if (debugMode) {
								System.err.println("adding submission with id='" + id + "'");
							}
							validSubmissions.add(id);
						}
						if (data.containsKey("submission_id")) {
							String id = data.getString("submission_id");
							if (!validSubmissions.contains(id)) {
								if (debugMode) {
									System.err.println("skipping judgements with submission_id='" + id + "'");
								}
								skip = true;
							} else {
								if (debugMode) {
									System.err.println("savinging judgement with id='" + id + "'");
								}
								validJudgements.add(id);
							}
						}
						if (type.equals("judgements") && data.containsKey("submission_id")) {
							String id = data.getString("submission_id");
							if (!validSubmissions.contains(id)) {
								if (debugMode) {
									System.err.println("skipping judgements with submission_id=`" + id + "'");
								}
								skip = true;
							} else {
								if (debugMode) {
									System.err.println("savinging judgement with id=`" + id + "`");
								}
								validJudgements.add(id);
							}
						}
						if (type.equals("runs") && data.containsKey("judgement_id")) {
							String id = data.getString("judgement_id");
							if (!validJudgements.contains(id)) {
								if (debugMode) {
									System.err.println("skipping run with judgement_id=`" + id + "`");
								}
								skip = true;
							}
						}
					}
					if (!skip) {
						System.out.println(s);
					} else {
						if (debugMode) {
							System.err.println("Skipping " + s);
						}
					}
				}

				// read next line
				s = br.readLine();
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not parse contest feed: " + s, e);
			throw new IOException("Error parsing contest feed");
		}

	}

	public static void main(String[] args) throws Exception {
		Trace.init("#ICPC Event Feed Splitter", "eventFeedSplitter", args);

		// TODO should consider saving output to a file (stdout also contains the Trace.init output)
		if (args.length < 2) {
			usage();
			if (args.length == 1 && (args[0] == "-h" || args[0] == "--help")) {
				System.exit(0);
			}
			System.exit(1);
		}
		String[] args2;
		List<String> argSet = new ArrayList<String>(Arrays.asList(args));
		if (argSet.contains("-h") || argSet.contains("--help")) {
			usage();
			System.exit(0);
		}
		if (argSet.contains("-d")) {
			debugMode = true;
			argSet.remove("-d");
		} else {
			debugMode = false;
		}
		args2 = argSet.toArray(new String[] {});
		if (args2.length < 2) {
			usage();
			System.exit(1);
		}
		int probStart = 1;
		int probEnd = 500;
		if (args2.length > 2) {
			probEnd = Integer.parseInt(args2[2]);
			if (args2.length > 3) {
				probStart = Integer.parseInt(args2[3]);
			}
		}

		new EventFeedSplitter(args2[0], args2[1], probStart, probEnd).run();

	}

	private void run() throws Exception {
		if (ndjsonFlag) {
			return;
		}
		// Remove undesired nodes
		Element contest_node = doc.getDocumentElement();
		// contest_node.normalize();
		NodeList nodes = contest_node.getChildNodes();
		for (int i = 0; i < nodes.getLength(); ++i) {
			Node node = nodes.item(i);
			if (!isDesiredNode(node)) {
				contest_node.removeChild(node);
				// } else {
				// if ("problem".equals(node.getNodeName())) {
				// NodeList childNodes = node.getChildNodes();
				// for (int k = 0; k < childNodes.getLength(); k++) {
				// Node childNode = childNodes.item(k);
				// if ("id".equals(childNode.getNodeName())) {
				// int origId = Integer.parseInt(childNode.getTextContent());
				// childNode.setTextContent(new Integer(origId - (problemStart - 1)).toString());
				// break;
				// }
				// }
				// } else if ("run".equals(node.getNodeName())) {
				// NodeList childNodes = node.getChildNodes();
				// for (int k = 0; k < childNodes.getLength(); k++) {
				// Node childNode = childNodes.item(k);
				// if ("problem".equals(childNode.getNodeName())) {
				// int origId = Integer.parseInt(childNode.getTextContent());
				// childNode.setTextContent(new Integer(origId - (problemStart - 1)).toString());
				// break;
				// }
				// }
				//
				// }
			}
		}
		// Output document
		DOMSource domSource = new DOMSource(doc);
		StreamResult outResult = new StreamResult(System.out);
		xmlTransformer.transform(domSource, outResult);
	}

	private static void incorrectUsage() {
		throw new RuntimeException("Incorrect usage");
	}

	private boolean isDesiredNode(Node node) throws Exception {
		String nodeName = node.getNodeName();
		if ("team".equals(nodeName)) {
			NodeList nodes = node.getChildNodes();
			for (int i = 0; i < nodes.getLength(); ++i) {
				Node childNode = nodes.item(i);
				if ("id".equals(childNode.getNodeName())) {
					return isDesiredTeam(Integer.parseInt(childNode.getTextContent()));
				}
			}
		} else if ("run".equals(nodeName)) {
			NodeList nodes = node.getChildNodes();
			int match = 0;
			for (int i = 0; i < nodes.getLength(); ++i) {
				Node childNode = nodes.item(i);
				if ("team".equals(childNode.getNodeName())) {
					if (isDesiredTeam(Integer.parseInt(childNode.getTextContent()))) {
						match++;
					}
				}
				if ("problem".equals(childNode.getNodeName())) {
					if (isDesiredProblem(Integer.parseInt(childNode.getTextContent()))) {
						match++;
					}
				}
			}
			return (match == 2);
		} else if ("problem".equals(nodeName)) {
			NodeList nodes = node.getChildNodes();
			for (int i = 0; i < nodes.getLength(); ++i) {
				Node childNode = nodes.item(i);
				if ("id".equals(childNode.getNodeName())) {
					return isDesiredProblem(Integer.parseInt(childNode.getTextContent()));
				}
			}
		} else if ("clar".equals(nodeName)) {
			NodeList nodes = node.getChildNodes();
			for (int i = 0; i < nodes.getLength(); ++i) {
				Node childNode = nodes.item(i);
				if ("id".equals(childNode.getNodeName())) {
					return isDesiredTeam(Integer.parseInt(childNode.getTextContent()));
				}
			}
		}
		return true;
	}

	private boolean isDesiredTeam(int teamId) {
		return (validTeams.contains(teamId));
	}

	private boolean isDesiredProblem(int problemId) {
		boolean desiredProblem = problemId >= problemStart && problemId <= problemEnd;
		return desiredProblem;
	}

	private static void usage() {
		System.out.println("Usage:");
		System.out.println();
		System.out.println("eventFeedSplitter XML_or_NDJSON { div1 | div2 } [problemEndIndex [problemStartIndex]]");
	}
}
