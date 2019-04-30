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
	public String getICPCId();

	/**
	 * The name of the group.
	 *
	 * @return the name
	 */
	public String getName();

	/**
	 * The type of the group.
	 *
	 * @return the type
	 */
	public String getGroupType();

	/**
	 * Returns <code>true</code> if the group is hidden.
	 *
	 * @return <code>true</code> if the group is hidden, <code>false</code> otherwise
	 */
	public boolean isHidden();

	/**
	 * The logo of the organization.
	 *
	 * @return the logo
	 */
	public File getLogo(int width, int height, boolean force);

	/**
	 * The logo of the organization.
	 *
	 * @return the logo
	 */
	public BufferedImage getLogoImage(int width, int height, boolean forceLoad, boolean resizeToFit);
}