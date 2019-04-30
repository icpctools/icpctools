package org.icpc.tools.contest.model;

import java.awt.Color;

/**
 * A problem.
 */
public interface IProblem extends IContestObject, IPosition {
	/**
	 * Return the ordinal, used for sorting problems (e.g. on a scoreboard).
	 *
	 * @return the ordinal
	 */
	public int getOrdinal();

	/**
	 * The problem label, typically a simple letter or number like "A".
	 *
	 * @return the name
	 */
	public String getLabel();

	/**
	 * The full name of the problem.
	 *
	 * @return the name
	 */
	public String getName();

	/**
	 * The name of the problem color, e.g. blue.
	 *
	 * @return the color name
	 */
	public String getColor();

	/**
	 * A 3 or 6 character hex string representing the rgb color of the problem, e.g. FF0000 or 0F0.
	 *
	 * @return the rgb value
	 */
	public String getRGB();

	/**
	 * A Java Color object representation of the RGB value, e.g. Color.RED or Color(0,0,255).
	 *
	 * @return the color
	 */
	public Color getColorVal();

	/**
	 * Return the number of testcases for this problem.
	 *
	 * @return
	 */
	public int getTestDataCount();

	/**
	 * Returns the time limit, in ms.
	 *
	 * @return the time limit
	 */
	public int getTimeLimit();
}