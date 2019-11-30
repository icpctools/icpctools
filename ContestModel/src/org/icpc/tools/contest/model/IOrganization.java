package org.icpc.tools.contest.model;

import java.awt.image.BufferedImage;
import java.io.File;

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