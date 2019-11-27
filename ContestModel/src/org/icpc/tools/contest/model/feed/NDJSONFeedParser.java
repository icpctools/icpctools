package org.icpc.tools.contest.model.feed;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.icpc.tools.contest.model.internal.Deletion;

public class NDJSONFeedParser implements Closeable {
	protected String lastId;
	protected String readUntilId;
	protected BufferedReader br;
	protected boolean closed;

	public void parse(final Contest contest, InputStream in) throws Exception {
		if (in == null)
			return;

		// {"event": "<event type>", "id": "<id>", "op":"create/update/delete" "data": <data from
		// endpoint> }
		String s = null;
		try {
			br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			s = br.readLine();
			while (s != null) {
				if (s.isEmpty() || s.startsWith("!")) { // heart beat or REST connector log message
					s = null;
					s = br.readLine();
					continue;
				}

				try {
					JSONParser rdr = new JSONParser(s);
					JsonObject obj = rdr.readObject();
					String type = obj.getString("type");
					String op = obj.getString("op");
					JsonObject data = obj.getJsonObject("data");

					if ("delete".equals(op)) {
						String id = data.getString("id");
						IContestObject.ContestType cType = IContestObject.getTypeByName(type);
						Deletion d = new Deletion(id, cType);
						contest.add(d);
					} else {
						ContestObject co = (ContestObject) IContestObject.createByName(type);
						if (co == null) {
							Trace.trace(Trace.WARNING, "Unrecognized (ignored) type in event feed: " + type);
							s = br.readLine();
							continue;
						}

						for (String key : data.props.keySet())
							co.add(key, data.props.get(key));

						try {
							contest.add(co);
						} catch (Exception e) {
							Trace.trace(Trace.ERROR, "Could not add event to contest! (" + lastId + "): " + s, e);
						}
					}
					lastId = obj.getString("id");
					if (readUntilId != null && readUntilId.equals(lastId))
						return;
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Could not parse event feed line (" + lastId + "): " + s, e);
				}

				if (closed)
					return;

				s = null;

				// read next line
				s = br.readLine();
			}
		} catch (Exception e) {
			if (closed)
				return;
			Trace.trace(Trace.ERROR, "Could not parse event feed: (" + lastId + ") " + s, e);
			throw new IOException("Error parsing event feed");
		}
	}

	public void readUntilEventId(String id) {
		readUntilId = id;
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

	protected void handleNewObject(ContestObject obj) {
		// hook for subclasses to perform actions on new elements
	}
}