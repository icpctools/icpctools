package org.icpc.tools.contest.model;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * A team member.
 */
public interface ITeamMember extends IContestObject {
	/**
	 * The ICPC id of the person.
	 *
	 * @return the id
	 */
	String getICPCId();

	/**
	 * The person's first name.
	 *
	 * @return the first name
	 */
	String getFirstName();

	/**
	 * The person's last name.
	 *
	 * @return the last name
	 */
	String getLastName();

	/**
	 * The person's sex (male or female).
	 *
	 * @return the sex
	 */
	String getSex();

	/**
	 * The id of the team they belong to.
	 *
	 * @return the id
	 */
	String getTeamId();

	/**
	 * The role of the person on the team.
	 *
	 * @return the role
	 */
	String getRole();

	File getPhoto(int width, int height, boolean force);

	BufferedImage getPhotoImage(int width, int height, boolean forceLoad, boolean resizeToFit);
}