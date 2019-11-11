package org.icpc.tools.contest.model.internal;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.feed.JSONEncoder;

public class Group extends ContestObject implements IGroup {
	private static final String ICPC_ID = "icpc_id";
	private static final String NAME = "name";
	private static final String TYPE = "type";
	private static final String HIDDEN = "hidden";
	private static final String LOGO = "logo";

	private String icpcId;
	private String name;
	private String type;
	private boolean isHidden;
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
		if (ICPC_ID.equals(name2)) {
			icpcId = (String) value;
			return true;
		} else if (NAME.equals(name2)) {
			name = (String) value;
			return true;
		} else if (TYPE.equals(name2)) {
			type = (String) value;
			return true;
		} else if (HIDDEN.equals(name2)) {
			isHidden = parseBoolean(value);
			return true;
		} else if (LOGO.equals(name2)) {
			logo = new FileReferenceList(value);
			return true;
		}

		return false;
	}

	@Override
	public IContestObject clone() {
		Group g = new Group();
		g.id = id;
		g.logo = logo;
		g.icpcId = icpcId;
		g.name = name;
		g.type = type;
		g.isHidden = isHidden;
		return g;
	}

	@Override
	protected void getPropertiesImpl(Map<String, Object> props) {
		super.getPropertiesImpl(props);
		props.put(ICPC_ID, icpcId);
		props.put(NAME, name);
		if (type != null)
			props.put(TYPE, type);
		if (isHidden)
			props.put(HIDDEN, "true");
		props.put(LOGO, logo);
	}

	@Override
	public void writeBody(JSONEncoder je) {
		je.encode(ID, id);
		je.encode(ICPC_ID, icpcId);
		je.encode(NAME, name);
		if (type != null)
			je.encode(TYPE, type);
		if (isHidden)
			je.encode(HIDDEN, true);
		je.encode(LOGO, logo, false);
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