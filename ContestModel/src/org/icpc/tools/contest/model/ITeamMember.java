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
	 * The person's name.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * The person's email.
	 *
	 * @return the email
	 */
	String getEmail();

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

	/**
	 * The registration photo.
	 *
	 * @return the photo file
	 */
	File getPhoto(int width, int height, boolean force);

	/**
	 * The registration photo.
	 *
	 * @return the photo
	 */
	BufferedImage getPhotoImage(int width, int height, boolean forceLoad, boolean resizeToFit);

	/**
	 * The desktop stream.
	 *
	 * @return the desktop stream
	 */
	String getDesktopURL();

	/**
	 * The webcam stream.
	 *
	 * @return the webcam stream
	 */
	String getWebcamURL();

	/**
	 * The audio stream.
	 *
	 * @return the audio stream
	 */
	String getAudioURL();

	/**
	 * The disk backup.
	 *
	 * @return the backup file
	 */
	File getBackup(boolean force);

	/**
	 * The key log.
	 *
	 * @return the key log file
	 */
	File getKeyLog(boolean force);

	/**
	 * The tool usage data.
	 *
	 * @return the tool usage data file
	 */
	File getToolData(boolean force);
}