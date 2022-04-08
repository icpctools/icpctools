package org.icpc.tools.cds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.feed.JSONEncoder;

/**
 * Very first attempt at an /access service, only provides a couple capabilities for now, no
 * endpoints.
 */
public class AccessService {

	public static void write(HttpServletRequest request, HttpServletResponse response, ConfiguredContest cc)
			throws IOException {
		JSONEncoder je = new JSONEncoder(response.getWriter());
		je.open();
		List<String> caps = new ArrayList<String>();

		IAccount account = cc.getAccount(request.getRemoteUser());

		if (account != null) {
			if ("team".equals(account.getAccountType())) {
				caps.add("team_submit");
				caps.add("team_clar");
			}
			if ("admin".equals(account.getAccountType())) {
				caps.add("contest_start");
				caps.add("admin_submit");
				caps.add("admin_clar");
			}
		}

		if (caps.isEmpty())
			je.encodePrimitive("capabilities", "[]");
		else
			je.encodePrimitive("capabilities", "[\"" + String.join("\",\"", caps) + "\"]");

		/*je.encodePrimitive("endpoints", "x" + contest.getId());
		
		{
			je.open();
			IContestObject.ContestType ct = null;
			je.encode("type", IContestObject.getTypeName(ct));
			je.encodePrimitive("properties", "x");
			je.close();
		}*/

		je.close();
	}
}