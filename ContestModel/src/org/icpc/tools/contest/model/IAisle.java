package org.icpc.tools.contest.model;

/**
 * An aisle.
 */
public interface IAisle {
	/**
	 * The x position of one end of the aisle.
	 *
	 * @return the x position
	 */
	double getX1();

	/**
	 * The y position of one end of the aisle.
	 *
	 * @return the y position
	 */
	double getY1();

	/**
	 * The x position of the other end of the aisle.
	 *
	 * @return the x position
	 */
	double getX2();

	/**
	 * The y position of the other end of the aisle.
	 *
	 * @return the y position
	 */
	double getY2();
}