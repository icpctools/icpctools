package org.icpc.tools.cds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.CDSConfig.Auth;
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
			String type = account.getAccountType();
			if ("team".equals(type)) {
				caps.add("team_submit");
				caps.add("team_clar");
			}
			if ("admin".equals(type)) {
				caps.add("contest_start");
				caps.add("admin_submit");
				caps.add("admin_clar");
			}
		}
		String user = request.getRemoteUser();
		if (user != null)
			for (Auth a : CDSConfig.getInstance().getAuths()) {
				if (user.equals(a.getUsername())) {
					if (a.getContestId() == null || cc.getContest().getId().equals(a.getContestId()))
						caps.add(a.getType() + "/" + a.getId());
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