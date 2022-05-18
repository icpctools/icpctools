package org.icpc.tools.contest.model.internal;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IOrganization;

public class Organization extends ContestObject implements IOrganization {
	private static final String ICPC_ID = "icpc_id";
	private static final String NAME = "name";
	private static final String FORMAL_NAME = "formal_name";
	private static final String COUNTRY = "country";
	private static final String URL = "url";
	private static final String HASHTAG = "twitter_hashtag";
	private static final String LOCATION = "location";
	private static final String LOGO = "logo";
	private static final String COUNTRY_FLAG = "country_flag";

	private String icpcId;
	private String name;
	private String formalName;
	private String country;
	private String url;
	private String hashtag;
	private Location location;
	private FileReferenceList logo;
	private FileReferenceList countryFlag;

	@Override
	public ContestType getType() {
		return ContestType.ORGANIZATION;
	}

	@Override
	public String getICPCId() {
		return icpcId;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getFormalName() {
		return formalName;
	}

	@Override
	public String getActualFormalName() {
		if (formalName == null)
			return name;
		return formalName;
	}

	@Override
	public String getCountry() {
		return country;
	}

	@Override
	public String getURL() {
		return url;
	}

	@Override
	public String getHashtag() {
		return hashtag;
	}

	@Override
	public double getLatitude() {
		if (location == null)
			return Double.NaN;
		return location.latitude;
	}

	@Override
	public double getLongitude() {
		if (location == null)
			return Double.NaN;
		return location.longitude;
	}

	@Override
	public File getLogo(int width, int height, boolean force) {
		return getFile(getBestFileReference(logo, new ImageSizeFit(width, height)), LOGO, force);
	}

	@Override
	public BufferedImage getLogoImage(int width, int height, boolean forceLoad, boolean resizeToFit) {
		return getRefImage(LOGO, logo, width, height, forceLoad, resizeToFit);
	}

	@Override
	public FileReferenceList getLogo() {
		return logo;
	}

	public void setLogo(FileReferenceList list) {
		logo = list;
	}

	@Override
	public File getCountryFlag(int width, int height, boolean force) {
		return getFile(getBestFileReference(countryFlag, new ImageSizeFit(width, height)), COUNTRY_FLAG, force);
	}

	@Override
	public BufferedImage getCountryFlagImage(int width, int height, boolean forceLoad, boolean resizeToFit) {
		return getRefImage(COUNTRY_FLAG, countryFlag, width, height, forceLoad, resizeToFit);
	}

	@Override
	public FileReferenceList getCountryFlag() {
		return countryFlag;
	}

	public void setCountryFlag(FileReferenceList list) {
		countryFlag = list;
	}

	@Override
	public Object resolveFileReference(String url2) {
		return FileReferenceList.resolve(url2, logo, countryFlag);
	}

	@Override
	protected boolean addImpl(String name2, Object value) throws Exception {
		switch (name2) {
			case ICPC_ID: {
				icpcId = (String) value;
				return true;
			}
			case NAME: {
				name = (String) value;
				return true;
			}
			case FORMAL_NAME: {
				formalName = (String) value;
				return true;
			}
			case COUNTRY: {
				country = (String) value;
				return true;
			}
			case URL: {
				url = (String) value;
				return true;
			}
			case HASHTAG: {
				hashtag = (String) value;
				return true;
			}
			case LOCATION: {
				location = new Location(value);
				return true;
			}
			case LOGO: {
				logo = new FileReferenceList(value);
				return true;
			}
			case COUNTRY_FLAG: {
				countryFlag = new FileReferenceList(value);
				return true;
			}
		}

		return false;
	}

	@Override
	public IContestObject clone() {
		Organization o = new Organization();
		o.id = id;
		o.logo = logo;
		o.icpcId = icpcId;
		o.name = name;
		o.formalName = formalName;
		o.country = country;
		o.countryFlag = countryFlag;
		o.url = url;
		o.hashtag = hashtag;
		o.location = location;
		return o;
	}

	@Override
	protected void getProperties(Properties props) {
		props.addLiteralString(ID, id);
		props.addLiteralString(ICPC_ID, icpcId);
		props.addString(NAME, name);
		props.addString(FORMAL_NAME, formalName);
		props.addString(COUNTRY, country);
		props.addFileRef(COUNTRY_FLAG, countryFlag);
		props.addString(URL, url);
		props.addString(HASHTAG, hashtag);
		if (location != null)
			props.add(LOCATION, location.getJSON());
		props.addFileRef(LOGO, logo);
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = super.validate(c);

		if (name == null || name.isEmpty())
			errors.add("Name missing");

		if (errors.isEmpty())
			return null;
		return errors;
	}
}