package org.icpc.tools.contest.model.internal;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IGroup;

public class Group extends ContestObject implements IGroup {
	private static final String ICPC_ID = "icpc_id";
	private static final String NAME = "name";
	private static final String TYPE = "type";
	private static final String HIDDEN = "hidden";
	private static final String LOCATION = "location";
	private static final String LOGO = "logo";

	private String icpcId;
	private String name;
	private String type;
	private boolean isHidden;
	private Location location;
	private FileReferenceList logo;

	@Override
	public ContestType getType() {
		return ContestType.GROUP;
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
	public String getGroupType() {
		return type;
	}

	@Override
	public boolean isHidden() {
		return isHidden;
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
			case TYPE: {
				type = (String) value;
				return true;
			}
			case HIDDEN: {
				isHidden = parseBoolean(value);
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
		}

		return false;
	}

	@Override
	public IContestObject clone() {
		Group g = new Group();
		g.id = id;
		g.icpcId = icpcId;
		g.name = name;
		g.type = type;
		g.isHidden = isHidden;
		g.location = location;
		g.logo = logo;
		return g;
	}

	@Override
	protected void getProperties(Properties props) {
		props.addLiteralString(ID, id);
		props.addLiteralString(ICPC_ID, icpcId);
		props.addString(NAME, name);
		props.addLiteralString(TYPE, type);
		if (isHidden)
			props.add(HIDDEN, "true");
		if (location != null)
			props.add(LOCATION, location.getJSON());
		if (logo != null)
			props.addFileRef(LOGO, logo);
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = super.validate(c);

		if (icpcId == null)
			errors.add("Missing external id");

		if (name == null || name.isEmpty())
			errors.add("Name missing");

		if (errors.isEmpty())
			return null;
		return errors;
	}
}