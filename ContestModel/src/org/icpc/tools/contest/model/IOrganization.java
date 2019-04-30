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
	public String getICPCId();

	/**
	 * The name of the organization.
	 *
	 * @return the name
	 */
	public String getName();

	/**
	 * The formal name of the organization.
	 *
	 * @return the name
	 */
	public String getFormalName();

	/**
	 * The nationality of the organization.
	 *
	 * @return the nationality
	 */
	public String getCountry();

	/**
	 * The url of the organization.
	 *
	 * @return the url
	 */
	public String getURL();

	/**
	 * The hashtag of the organization.
	 *
	 * @return the hashtag
	 */
	public String getHashtag();

	/**
	 * The latitude of the organization.
	 *
	 * @return the latitude
	 */
	public double getLatitude();

	/**
	 * The longitude of the organization.
	 *
	 * @return the longitude
	 */
	public double getLongitude();

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