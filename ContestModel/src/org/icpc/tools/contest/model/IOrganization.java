package org.icpc.tools.contest.model;

import java.awt.image.BufferedImage;
import java.io.File;

import org.icpc.tools.contest.model.internal.FileReferenceList;

/**
 * An organization.
 */
public interface IOrganization extends IContestObject {
	/**
	 * The ICPC id of the organization.
	 *
	 * @return the id
	 */
	String getICPCId();

	/**
	 * The name of the organization.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * The formal name of the organization.
	 *
	 * @return the name
	 */
	String getFormalName();

	/**
	 * The formal name of the organization, falls back to name.
	 *
	 * @return the name
	 */
	String getActualFormalName();

	/**
	 * The nationality of the organization.
	 *
	 * @return the nationality
	 */
	String getCountry();

	/**
	 * The url of the organization.
	 *
	 * @return the url
	 */
	String getURL();

	/**
	 * The hashtag of the organization.
	 *
	 * @return the hashtag
	 */
	String getHashtag();

	/**
	 * The latitude of the organization.
	 *
	 * @return the latitude
	 */
	double getLatitude();

	/**
	 * The longitude of the organization.
	 *
	 * @return the longitude
	 */
	double getLongitude();

	/**
	 * The file references for the logo, which clients can use to see exactly what resolutions of
	 * logos are available.
	 *
	 * @return the file reference list
	 */
	FileReferenceList getLogo();

	/**
	 * The logo of the organization.
	 *
	 * @return the logo
	 */
	File getLogo(int width, int height, boolean force);

	/**
	 * The logo of the organization.
	 *
	 * @return the logo
	 */
	BufferedImage getLogoImage(int width, int height, boolean forceLoad, boolean resizeToFit);
}