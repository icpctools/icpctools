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
	String getName();

	/**
	 * The display name of the team, or falls back to team name.
	 *
	 * @return the display name
	 */
	String getActualDisplayName();

	/**
	 * The display name of the team.
	 *
	 * @return the display name
	 */
	String getDisplayName();

	/**
	 * The groups that this team is part of, or <code>null</code> if it is not in any group.
	 *
	 * @return the group ids
	 */
	String[] getGroupIds();

	/**
	 * The organization that the team is part of.
	 *
	 * @return the organization id
	 */
	String getOrganizationId();

	/**
	 * The ICPC id of the team.
	 *
	 * @return the id
	 */
	String getICPCId();

	/**
	 * The integer rotation of the team's desk in degrees, increasing counter-clockwise from 0 to
	 * 359. 0 is a desk facing E, 90 for table facing N, 180 for table facing W, etc.
	 *
	 * @return the rotation
	 */
	int getRotation();

	File getPhoto(int width, int height, boolean force);

	BufferedImage getPhotoImage(int width, int height, boolean forceLoad, boolean resizeToFit);

	File getVideo(boolean force);

	File getBackup(boolean force);

	File getKeyLog(boolean force);

	String getDesktopURL();

	String getWebcamURL();
}