package org.icpc.tools.contest.model.feed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;

public class NDJSONFeedLogParser {
	protected String lastId;
	protected String firstComment;

	public void parse(InputStream in) throws Exception {
		if (in == null)
			return;

		// {"event": "<event type>", "id": "<id>", "op":"create/update/delete" "data": <data from
		// endpoint> }
		String s = null;
		String lastJson = null;
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			s = br.readLine();
			while (s != null) {
				if (s.isEmpty() || s.startsWith("!")) { // heart beat or REST connector log message
					if (firstComment == null)
						firstComment = s;
				} else
					lastJson = s;

				// read next line
				s = br.readLine();
			}
			try {
				if (lastJson == null)
					return;

				JSONParser rdr = new JSONParser(lastJson);
				JsonObject obj = rdr.readObject();
				lastId = obj.getString("id");
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Could not parse event feed log " + s, e);
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not parse event feedlog " + s, e);
			throw new IOException("Error parsing event feed");
		}
	}

	public String getLastEventId() {
		return lastId;
	}

	public String getFirstComment() {
		return firstComment;
	}
}