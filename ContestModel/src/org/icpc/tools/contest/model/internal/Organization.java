package org.icpc.tools.contest.model.internal;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;

public class Organization extends ContestObject implements IOrganization {
	private static final String ICPC_ID = "icpc_id";
	private static final String NAME = "name";
	private static final String FORMAL_NAME = "formal_name";
	private static final String COUNTRY = "country";
	private static final String URL = "url";
	private static final String HASHTAG = "twitter_hashtag";
	private static final String LOCATION = "location";
	private static final String LATITUDE = "latitude";
	private static final String LONGITUDE = "longitude";
	private static final String LOGO = "logo";

	private String icpcId;
	private String name;
	private String formalName;
	private String country;
	private String url;
	private String hashtag;
	private double latitude = Double.NaN;
	private double longitude = Double.NaN;
	private FileReferenceList logo;

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
		return latitude;
	}

	@Override
	public double getLongitude() {
		return longitude;
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
	public Object resolveFileReference(String url2) {
		return FileReferenceList.resolve(url2, logo);
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
				JsonObject obj = JSONParser.getOrReadObject(value);
				latitude = obj.getDouble(LATITUDE);
				longitude = obj.getDouble(LONGITUDE);
				return true;
			}
			case LOGO: {
				logo = new FileReferenceList(value);
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
		o.url = url;
		o.hashtag = hashtag;
		o.latitude = latitude;
		o.longitude = longitude;
		return o;
	}

	@Override
	protected void getPropertiesImpl(Map<String, Object> props) {
		super.getPropertiesImpl(props);
		props.put(ICPC_ID, icpcId);
		props.put(NAME, name);
		props.put(FORMAL_NAME, formalName);
		props.put(COUNTRY, country);
		props.put(URL, url);
		props.put(HASHTAG, hashtag);
		if (!Double.isNaN(latitude) || !Double.isNaN(longitude)) {
			List<String> attrs = new ArrayList<>(2);
			if (!Double.isNaN(latitude))
				attrs.add("\"" + LATITUDE + "\":" + round(latitude));
			if (!Double.isNaN(Double.NaN))
				attrs.add("\"" + LONGITUDE + "\":" + round(longitude));
			props.put(LOCATION, "{" + String.join(",", attrs) + "}");
		}
		props.put(LOGO, logo);
	}

	@Override
	public void writeBody(JSONEncoder je) {
		je.encode(ID, id);
		je.encode(ICPC_ID, icpcId);
		je.encode(NAME, name);
		je.encode(FORMAL_NAME, formalName);
		if (country != null)
			je.encode(COUNTRY, country);
		if (url != null)
			je.encode(URL, url);
		if (hashtag != null)
			je.encode(HASHTAG, hashtag);
		if (!Double.isNaN(latitude) || !Double.isNaN(longitude)) {
			List<String> attrs = new ArrayList<>(2);
			if (!Double.isNaN(latitude))
				attrs.add("\"" + LATITUDE + "\":" + round(latitude));
			if (!Double.isNaN(longitude))
				attrs.add("\"" + LONGITUDE + "\":" + round(longitude));
			je.encodePrimitive(LOCATION, "{" + String.join(",", attrs) + "}");
		}

		je.encode(LOGO, logo, false);
	}

	private static double round(double d) {
		return Math.round(d * 10000.0) / 10000.0;
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