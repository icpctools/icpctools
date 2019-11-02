package org.icpc.tools.contest.model.internal;

import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ILanguage;
import org.icpc.tools.contest.model.feed.JSONEncoder;

public class Language extends ContestObject implements ILanguage {
	private static final String NAME = "name";

	private String name;

	@Override
	public ContestType getType() {
		return ContestType.LANGUAGE;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	protected boolean addImpl(String name2, Object value) throws Exception {
		if (NAME.equals(name2)) {
			name = (String) value;
			return true;
		}

		return false;
	}

	@Override
	protected void getPropertiesImpl(Map<String, Object> props) {
		super.getPropertiesImpl(props);
		props.put(NAME, name);
	}

	@Override
	public void writeBody(JSONEncoder je) {
		je.encode(ID, id);
		je.encode(NAME, name);
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