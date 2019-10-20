package org.icpc.tools.contest.model;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * A team. The id is typically a short label like the team number.
 */
public interface ITeam extends IContestObject, IPosition {
	/**
	 * The name of the team.
	 *
	 * @return the name
	 */
	public String getName();

	/**
	 * The groups that this team is part of, or <code>null</code> if it is not in any group.
	 *
	 * @return the group ids
	 */
	public String[] getGroupIds();

	/**
	 * The organization that the team is part of.
	 *
	 * @return the organization id
	 */
	public String getOrganizationId();

	/**
	 * The ICPC id of the team.
	 *
	 * @return the id
	 */
	public String getICPCId();

	/**
	 * The integer rotation of the team's desk in degrees, increasing counter-clockwise from 0 to
	 * 359. 0 is a desk facing E, 90 for table facing N, 180 for table facing W, etc.
	 *
	 * @return the rotation
	 */
	public int getRotation();

	public File getPhoto(int width, int height, boolean force);

	public BufferedImage getPhotoImage(int width, int height, boolean forceLoad, boolean resizeToFit);

	public File getVideo(boolean force);

	public File getBackup(boolean force);

	public File getKeyLog(boolean force);

	public String getDesktopURL();

	public String getWebcamURL();
}