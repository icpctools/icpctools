package org.icpc.tools.contest.model;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * A group.
 */
public interface IGroup extends IContestObject {
	/**
	 * The external/icpc id of the group.
	 *
	 * @return the id
	 */
	String getICPCId();

	/**
	 * The name of the group.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * The type of the group.
	 *
	 * @return the type
	 */
	String getGroupType();

	/**
	 * Returns <code>true</code> if the group is hidden.
	 *
	 * @return <code>true</code> if the group is hidden, <code>false</code> otherwise
	 */
	boolean isHidden();

	/**
	 * The latitude of the group.
	 *
	 * @return the latitude
	 */
	double getLatitude();

	/**
	 * The longitude of the group.
	 *
	 * @return the longitude
	 */
	double getLongitude();

	/**
	 * The logo of the group.
	 *
	 * @return the logo
	 */
	File getLogo(int width, int height, boolean force);

	/**
	 * The logo of the group.
	 *
	 * @return the logo
	 */
	BufferedImage getLogoImage(int width, int height, boolean forceLoad, boolean resizeToFit);
}