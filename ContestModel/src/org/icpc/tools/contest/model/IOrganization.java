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
	 * An audio recording of the university name.
	 *
	 * @return a file reference list
	 */
	FileReferenceList getAudio();

	/**
	 * The audio recording of the university name.
	 *
	 * @return an audio file
	 */
	File getAudio(boolean force);

	/**
	 * The country of the organization.
	 *
	 * @return the country
	 */
	String getCountry();

	/**
	 * The file references for the country flag, which clients can use to see exactly what
	 * resolutions of country flags are available.
	 *
	 * @return the file reference list
	 */
	FileReferenceList getCountryFlag();

	/**
	 * The country flag of the organization.
	 *
	 * @return the country flag
	 */
	File getCountryFlag(int width, int height, boolean force);

	/**
	 * The country flag of the organization.
	 *
	 * @return the country flag
	 */
	BufferedImage getCountryFlagImage(int width, int height, boolean forceLoad, boolean resizeToFit);

	/**
	 * The country subdivision of the organization.
	 *
	 * @return the country subdivision
	 */
	String getCountrySubdivision();

	/**
	 * The file references for the country subdivision flag, which clients can use to see exactly
	 * what resolutions of country subdivision flags are available.
	 *
	 * @return the file reference list
	 */
	FileReferenceList getCountrySubdivisionFlag();

	/**
	 * The country subdivision flag of the organization.
	 *
	 * @return the country flag
	 */
	File getCountrySubdivisionFlag(int width, int height, boolean force);

	/**
	 * The country subdivision flag of the organization.
	 *
	 * @return the country flag
	 */
	BufferedImage getCountrySubdivisionFlagImage(int width, int height, boolean forceLoad, boolean resizeToFit);

	/**
	 * The url of the organization.
	 *
	 * @return the url
	 */
	String getURL();

	/**
	 * The twitter hashtag of the organization.
	 *
	 * @return the hashtag
	 */
	String getTwitterHashtag();

	/**
	 * The twitter account of the organization.
	 *
	 * @return the account
	 */
	String getTwitterAccount();

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
	File getLogo(int width, int height, String tag, boolean force);

	/**
	 * The logo of the organization.
	 *
	 * @return the logo
	 */
	BufferedImage getLogoImage(int width, int height, String tag, boolean forceLoad, boolean resizeToFit);
}