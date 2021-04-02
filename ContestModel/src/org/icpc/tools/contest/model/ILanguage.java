package org.icpc.tools.contest.model;

/**
 * A language.
 */
public interface ILanguage extends IContestObject {
	/**
	 * The name of the language.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Returns whether this language requires an entrypoint.
	 *
	 * @return whether an entry point is required
	 */
	boolean getEntryPointRequired();

	/**
	 * Returns the entry point name for this language, if any.
	 *
	 * @return the entry point or null
	 */
	String getEntryPointName();

	/**
	 * Returns the extensions for this language
	 *
	 * @return the extensions
	 */
	String[] getExtensions();
}