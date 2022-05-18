package org.icpc.tools.contest.model.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ILanguage;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;

public class Language extends ContestObject implements ILanguage {
	private static final String NAME = "name";
	private static final String ENTRY_POINT_REQUIRED = "entry_point_required";
	private static final String ENTRY_POINT_NAME = "entry_point_name";
	private static final String EXTENSIONS = "extensions";
	private static final String COMPILER = "compiler";
	private static final String RUNNER = "runner";
	private static final String COMMAND = "command";
	private static final String ARGS = "args";
	private static final String VERSION = "version";
	private static final String VERSION_COMMAND = "version-command";

	private String name;
	private boolean entryPointRequired;
	private String entryPointName;
	private String[] extensions;
	private Command compiler;
	private Command runner;

	private static class Command {
		String command;
		String args;
		String version;
		String versionCommand;

		public Command(Object value) {
			JsonObject obj = JSONParser.getOrReadObject(value);
			command = obj.getString(COMMAND);
			args = obj.getString(ARGS);
			version = obj.getString(VERSION);
			versionCommand = obj.getString(VERSION_COMMAND);
		}

		public String getJSON() {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			PrintWriter pw = new PrintWriter(bout);
			JSONEncoder je = new JSONEncoder(pw);
			je.open();
			if (command != null)
				je.encode(COMMAND, command);
			if (args != null)
				je.encode(ARGS, args);
			if (version != null)
				je.encode(VERSION, version);
			if (versionCommand != null)
				je.encode(VERSION_COMMAND, versionCommand);
			je.close();
			pw.flush();
			return bout.toString();
		}
	}

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
			case COMPILER:
				compiler = new Command(value);
				return true;
			case RUNNER:
				runner = new Command(value);
				return true;
		}

		return false;
	}

	@Override
	protected void getProperties(Properties props) {
		props.addLiteralString(ID, id);
		props.addString(NAME, name);
		props.add(ENTRY_POINT_REQUIRED, entryPointRequired);
		props.addString(ENTRY_POINT_NAME, entryPointName);
		props.addArray(EXTENSIONS, extensions);

		if (compiler != null)
			props.add(COMPILER, compiler.getJSON());
		if (runner != null)
			props.add(RUNNER, runner.getJSON());
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