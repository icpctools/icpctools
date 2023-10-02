package org.icpc.tools.cds.video;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class VideoServingHandler extends VideoHandler {

	/**
	 * Optional HTTP method for video serving handlers. Responds to "/stream/<id>*".
	 *
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response, int stream, IStore store,
			String subpath) throws IOException {
		response.sendError(404, "unsupported");
	}
}