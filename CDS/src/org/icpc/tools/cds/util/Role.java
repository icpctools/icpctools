package org.icpc.tools.cds.util;

import javax.servlet.http.HttpServletRequest;

public class Role {
	public static final String ADMIN = "admin";
	public static final String PRES_ADMIN = "presAdmin";
	public static final String BLUE = "blue";
	public static final String BALLOON = "balloon";
	public static final String TRUSTED = "trusted";
	public static final String PUBLIC = "public";

	public static boolean isAdmin(HttpServletRequest request) {
		return request.isUserInRole(ADMIN);
	}

	public static boolean isPresAdmin(HttpServletRequest request) {
		return request.isUserInRole(PRES_ADMIN) || request.isUserInRole(ADMIN);
	}

	public static boolean isBlue(HttpServletRequest request) {
		return request.isUserInRole(BLUE) || request.isUserInRole(ADMIN) || request.isUserInRole(PRES_ADMIN);
	}

	public static boolean isTrusted(HttpServletRequest request) {
		return request.isUserInRole(TRUSTED) || request.isUserInRole(BLUE) || request.isUserInRole(ADMIN)
				|| request.isUserInRole(PRES_ADMIN);
	}

	public static boolean isBalloon(HttpServletRequest request) {
		return request.isUserInRole(BALLOON);
	}
}