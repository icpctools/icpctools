package org.icpc.tools.contest.model.feed;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.icpc.tools.contest.model.internal.Deletion;

/**
 * Helper class to parse an event feed and read it into a contest model.
 *
 * Supports the event feed notification format, which defines a type, id, and the current value of
 * the corresponding endpoint, which may be in any of the following forms:
 * <ul>
 * <li>{ "type": "<type>", "id": "<id>", "data": { <data from endpoint> } } (new or updated
 * object)</li>
 * <li>{ "type": "<type>", "id": "<id>", "data": null } (deleted object)</li>
 * <li>{ "type": "<type>", "id": null, "data": [ <data from endpoint> ] } (new or updated singleton
 * object, or entire collection changed)</li>
 * <li>{ "type": "<type>", "id": null, "data": [] } (deleted singleton object)</li>
 * </ul>
 *
 * This class also supports the previous event feed format from spec version 2020-03 and before:
 *
 * <ul>
 * <li>{ "id": "<id>", "type": "<event type>", "op":"create/update/delete", "data": { <data from
 * endpoint> } }</li>
 * </ul>
 */
public class NDJSONFeedParser implements Closeable {
	protected String lastId;
	protected String lastToken;
	protected String readUntilId;
	protected BufferedReader br;
	protected boolean closed;

	public void parse(final Contest contest, InputStream in) throws Exception {
		if (in == null)
			return;

		String s = null;
		try {
			br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			s = br.readLine();
			while (s != null) {
				if (s.isEmpty() || s.startsWith("!")) { // heart beat or REST connector log message
					s = br.readLine();
					continue;
				}

				try {
					JSONParser rdr = new JSONParser(s);
					JsonObject obj = rdr.readObject();
					String op = obj.getString("op");
					if (op != null)
						parseOldFormat(s, contest, obj);
					else
						parseNewFormat(s, contest, obj);
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Could not parse event feed line: " + s, e);
				}

				if (closed)
					return;

				// read next line
				s = br.readLine();
			}
		} catch (Exception e) {
			if (closed)
				return;
			Trace.trace(Trace.ERROR, "Could not parse event feed: " + s, e);
			throw new IOException("Error parsing event feed");
		}
	}

	protected void parseOldFormat(String s, Contest contest, JsonObject obj) {
		String type = obj.getString("type");
		String op = obj.getString("op");
		JsonObject data = obj.getJsonObject("data");

		// backwards compatibility
		if ("team-members".equals(type))
			type = "persons";

		if ("delete".equals(op)) {
			String id = data.getString("id");
			IContestObject.ContestType cType = IContestObject.getTypeByName(type);
			Deletion d = new Deletion(id, cType);
			contest.add(d);
		} else {
			IContestObject.ContestType cType = IContestObject.getTypeByName(type);
			if (cType == null) {
				Trace.trace(Trace.WARNING, "Unrecognized (ignored) type in event feed: " + type);
				return;
			}

			try {
				parseAndAdd(contest, cType, data);
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Could not add event to contest! (" + lastId + "): " + s, e);
			}
		}
		lastId = obj.getString("id");
	}

	protected void parseNewFormat(String s, Contest contest, JsonObject obj) {
		String type = obj.getString("type");
		String id = obj.getString("id");
		Object data = obj.get("data");
		IContestObject.ContestType cType = IContestObject.getTypeByName(type);
		if (cType == null) {
			Trace.trace(Trace.WARNING, "Unrecognized (ignored) type in event feed: " + type);
			return;
		}

		if (data == null) {
			if (id != null)
				contest.add(new Deletion(id, cType));
			else {
				IContestObject[] objs = contest.getObjects(cType);
				for (IContestObject co : objs)
					contest.add(new Deletion(co.getId(), cType));
			}
		} else {
			if (id != null || IContestObject.isSingleton(cType) || cType.equals(IContestObject.ContestType.CONTEST)) {
				try {
					parseAndAdd(contest, cType, (JsonObject) data);
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Could not add event to contest: " + s, e);
				}
			} else {
				// add/replace set
				Object[] objs = (Object[]) data;
				List<String> ids = new ArrayList<>();
				for (Object o : objs) {
					try {
						JsonObject jo = (JsonObject) o;
						ids.add(jo.getString("id"));
						parseAndAdd(contest, cType, jo);
					} catch (Exception e) {
						Trace.trace(Trace.ERROR, "Could not add event to contest: " + s, e);
					}
				}

				// delete any objects not included
				IContestObject[] allObjs = contest.getObjects(cType);
				for (IContestObject co : allObjs) {
					if (!ids.contains(co.getId())) {
						Deletion d = new Deletion(co.getId(), cType);
						contest.add(d);
					}
				}
			}
		}

		if (obj.getString("token") != null)
			lastToken = obj.getString("token");
	}

	private static void parseAndAdd(Contest contest, IContestObject.ContestType cType, JsonObject data)
			throws Exception {
		ContestObject co = (ContestObject) IContestObject.createByType(cType);

		for (String key : data.props.keySet())
			co.add(key, data.props.get(key));

		contest.add(co);
	}

	@Override
	public void close() {
		closed = true;
		if (br != null) {
			try {
				br.close();
			} catch (Exception e) {
				// ignore
			}
			br = null;
		}
	}

	public String getLastEventId() {
		return lastId;
	}

	public String getLastToken() {
		return lastToken;
	}
}