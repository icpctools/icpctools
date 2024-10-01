package org.icpc.tools.cds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.icpc.tools.cds.CDSConfig.Auth;
import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.feed.JSONEncoder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

		// write capabilities
		if (account != null) {
			String type = account.getAccountType();
			if ("team".equals(type)) {
				caps.add("team_submit");
				caps.add("team_clar");
			}
			if ("judge".equals(type)) {
				caps.add("admin_clar");
				caps.add("commentary_submit");
			}
			if ("admin".equals(type)) {
				caps.add("contest_start");
				caps.add("admin_submit");
				caps.add("admin_clar");
				caps.add("commentary_submit");
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

		// write endpoints
		je.writeSeparator();
		je.openChildArray("endpoints");
		Set<String>[] allKnownProperties = cc.getContestByRole(request).getKnownProperties();
		for (IContestObject.ContestType ct : IContestObject.ContestType.values()) {
			Set<String> properties = allKnownProperties[ct.ordinal()];
			if (properties != null) {
				je.writeSeparator();
				je.open();
				if (ct == IContestObject.ContestType.CONTEST)
					je.encode("type", "contest");
				else
					je.encode("type", IContestObject.getTypeName(ct));

				String s = String.join("\",\"", properties);
				je.encodePrimitive("properties", "[\"" + s + "\"]");
				je.close();
			}
		}
		je.closeArray();

		je.close();
	}
}