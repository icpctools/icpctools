package org.icpc.tools.contest.model;

import java.awt.Color;
import java.io.File;

/**
 * A problem.
 */
public interface IProblem extends IContestObject, IPosition {
	/**
	 * Return the ordinal, used for sorting problems (e.g. on a scoreboard).
	 *
	 * @return the ordinal
	 */
	int getOrdinal();

	/**
	 * The problem label, typically a simple letter or number like "A".
	 *
	 * @return the label
	 */
	String getLabel();

	/**
	 * The full name of the problem.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * The uuid.
	 *
	 * @return the uuid
	 */
	String getUUID();

	/**
	 * The name of the problem color, e.g. blue.
	 *
	 * @return the color name
	 */
	String getColor();

	/**
	 * A 3 or 6 character hex string representing the rgb color of the problem, e.g. FF0000 or 0F0.
	 *
	 * @return the rgb value
	 */
	String getRGB();

	/**
	 * A Java Color object representation of the RGB value, e.g. Color.RED or Color(0,0,255).
	 *
	 * @return the color
	 */
	Color getColorVal();

	/**
	 * Return the number of testcases for this problem.
	 *
	 * @return
	 */
	int getTestDataCount();

	/**
	 * Returns the time limit, in ms.
	 *
	 * @return the time limit
	 */
	int getTimeLimit();

	/**
	 * Returns the maximum expected score for scoring contests.
	 *
	 * @return the max score
	 */
	Double getMaxScore();

	/**
	 * The problem package.
	 *
	 * @return the package
	 */
	File getPackage(boolean force);

	/**
	 * The problem statement.
	 *
	 * @return the statement
	 */
	File getStatement(boolean force);
}