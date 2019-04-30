package org.icpc.tools.contest.model.internal;

import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.resolver.Resolver;

public class CustomYamlResolver extends Resolver {
	@Override
	protected void addImplicitResolvers() {
		addImplicitResolver(Tag.BOOL, BOOL, "yYnNtTfFoO");
		// CDS parses it's own ints & floats
		/*
		 * INT must be before FLOAT because the regular expression for FLOAT
		 * matches INT (see issue 130)
		 * http://code.google.com/p/snakeyaml/issues/detail?id=130
		 */
		// addImplicitResolver(Tag.INT, INT, "-+0123456789");
		// addImplicitResolver(Tag.FLOAT, FLOAT, "-+0123456789.");
		addImplicitResolver(Tag.MERGE, MERGE, "<");
		addImplicitResolver(Tag.NULL, NULL, "~nN\0");
		addImplicitResolver(Tag.NULL, EMPTY, null);
		// CDS parses it's own dates
		// addImplicitResolver(Tag.TIMESTAMP, TIMESTAMP, "0123456789");
		// The following implicit resolver is only for documentation
		// purposes.
		// It cannot work
		// because plain scalars cannot start with '!', '&', or '*'.
		addImplicitResolver(Tag.YAML, YAML, "!&*");
	}
}