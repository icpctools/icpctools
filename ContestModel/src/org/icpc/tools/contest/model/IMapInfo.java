package org.icpc.tools.contest.model;

import java.util.List;

/**
 * An aisle.
 */
public interface IMapInfo extends IContestObject {
	// table width (in meters). ICPC standard is 1.8
	// public double tableWidth = 1.8;

	// table depth (in meters). ICPC standard is 0.8
	// public double tableDepth = 0.8;

	// team area width (in meters). ICPC standard is 3.0
	// public double teamAreaWidth = 3.0;

	// team area depth (in meters). ICPC standard is 2.
	// public double teamAreaDepth = 2.0;

	/**
	 * The x position of one end of the aisle.
	 *
	 * @return the x position
	 */
	double getTableWidth();

	/**
	 * The y position of one end of the aisle.
	 *
	 * @return the y position
	 */
	double getTableDepth();

	/**
	 * The x position of the other end of the aisle.
	 *
	 * @return the x position
	 */
	double getTeamAreaWidth();

	/**
	 * The y position of the other end of the aisle.
	 *
	 * @return the y position
	 */
	double getTeamAreaDepth();

	List<IAisle> getAisles();

	List<ITeam> getSpareTeams();

	IPrinter getPrinter();
}