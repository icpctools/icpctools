package org.icpc.tools.contest.util.cms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.util.cms.CMSDownloadHelper.ContestInfo;

public class CMSLogin {
	private static ContestInfo[] contests;

	public synchronized static ContestInfo[] getContests() {
		if (contests == null) {
			try {
				loadContests();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return contests;
	}

	private static void loadContests() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(CMSLogin.class.getResourceAsStream("login.tsv")));

		List<ContestInfo> list = new ArrayList<>();
		try {
			// read header
			br.readLine();

			String s = br.readLine();
			while (s != null) {
				if (!s.startsWith("#") && s.trim().length() > 0) {
					String[] st = s.split("\\t");
					if (st != null && st.length > 0) {
						try {
							list.add(new ContestInfo(st[0], st[1], st[2]));
						} catch (Exception e) {
							Trace.trace(Trace.ERROR, "Error parsing login.tsv: " + s, e);
						}
					}
				}
				s = br.readLine();
			}
		} finally {
			try {
				br.close();
			} catch (Exception e) {
				// ignore
			}
		}
		contests = list.toArray(new ContestInfo[list.size()]);
	}
}