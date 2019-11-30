package org.icpc.tools.client.core;

public interface IPropertyListener {
	/**
	 * Set a local property, e.g. on a presentation. Only called on non-admin clients.
	 */
	void propertyUpdated(String key, String value);
}