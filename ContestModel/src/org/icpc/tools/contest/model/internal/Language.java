package org.icpc.tools.contest.model.internal;

import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ILanguage;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.feed.JSONParser;

public class Language extends ContestObject implements ILanguage {
	private static final String NAME = "name";
	private static final String ENTRY_POINT_REQUIRED = "entry_point_required";
	private static final String ENTRY_POINT_NAME = "entry_point_name";
	private static final String EXTENSIONS = "extensions";

	private String name;
	private boolean entryPointRequired;
	private String entryPointName;
	private String[] extensions;

	@Override
	public ContestType getType() {
		return ContestType.LANGUAGE;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean getEntryPointRequired() {
		return entryPointRequired;
	}

	@Override
	public String getEntryPointName() {
		return entryPointRequired ? entryPointName : null;
	}

	@Override
	public String[] getExtensions() {
		return extensions;
	}

	@Override
	protected boolean addImpl(String name2, Object value) throws Exception {
		switch (name2) {
			case NAME:
				name = (String) value;
				return true;
			case ENTRY_POINT_REQUIRED:
				entryPointRequired = parseBoolean(value);
				return true;
			case ENTRY_POINT_NAME:
				entryPointName = (String) value;
				return true;
			case EXTENSIONS:
				Object[] ob = JSONParser.getOrReadArray(value);
				extensions = new String[ob.length];
				for (int i = 0; i < ob.length; i++)
					extensions[i] = (String) ob[i];
				return true;
		}

		return false;
	}

	@Override
	protected void getPropertiesImpl(Map<String, Object> props) {
		super.getPropertiesImpl(props);
		props.put(NAME, name);
		props.put(ENTRY_POINT_REQUIRED, entryPointRequired);
		if (entryPointName != null) {
			props.put(ENTRY_POINT_NAME, entryPointName);
		}
		if (extensions != null) {
			if (extensions.length == 0)
				props.put(EXTENSIONS, "[]");
			else
				props.put(EXTENSIONS, "[\"" + String.join("\",\"", extensions) + "\"]");
		}
	}

	@Override
	public void writeBody(JSONEncoder je) {
		je.encode(ID, id);
		je.encode(NAME, name);
		je.encode(ENTRY_POINT_REQUIRED, entryPointRequired);
		je.encode(ENTRY_POINT_NAME, entryPointName);
		if (extensions != null) {
			if (extensions.length == 0)
				je.encodePrimitive(EXTENSIONS, "[]");
			else
				je.encodePrimitive(EXTENSIONS, "[\"" + String.join("\",\"", extensions) + "\"]");
		}
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