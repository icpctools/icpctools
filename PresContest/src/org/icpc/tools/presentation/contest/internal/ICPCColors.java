package org.icpc.tools.presentation.contest.internal;

import java.awt.Color;

import org.icpc.tools.contest.model.Status;

public class ICPCColors {
	public static final Color BLUE = new Color(92, 138, 221);
	public static final Color YELLOW = new Color(255, 223, 54);
	public static final Color RED = new Color(196, 58, 36);

	public static final Color PENDING_COLOR = new Color(230, 230, 0);
	public static final Color SOLVED_COLOR = new Color(0, 230, 0);
	public static final Color FIRST_TO_SOLVE_COLOR = new Color(0, 100, 0);
	public static final Color FAILED_COLOR = new Color(240, 0, 0);

	public static final Color GOLD2 = new Color(205, 127, 50, 96);
	public static final Color SILVER2 = new Color(230, 232, 250, 96);
	public static final Color BRONZE2 = new Color(166, 125, 61, 96);

	public static final Color GOLD = new Color(205, 127, 50);
	public static final Color SILVER = new Color(230, 232, 250);
	public static final Color BRONZE = new Color(166, 125, 61);

	public static final Color SELECTION_COLOR = new Color(92, 138, 231, 240);

	public static final int CCOUNT = 15;
	public static Color[] PENDING = Utility.getColorsBetween(PENDING_COLOR,
			Utility.alphaDarker(PENDING_COLOR, 128, 0.5f), CCOUNT);
	public static Color[] SOLVED = Utility.getColorsBetween(SOLVED_COLOR, Utility.alphaDarker(SOLVED_COLOR, 128, 0.5f),
			CCOUNT);
	public static Color[] FIRST_TO_SOLVE = Utility.getColorsBetween(FIRST_TO_SOLVE_COLOR,
			Utility.alphaDarker(FIRST_TO_SOLVE_COLOR, 128, 0.5f), CCOUNT);
	public static Color[] FAILED = Utility.getColorsBetween(FAILED_COLOR, Utility.alphaDarker(FAILED_COLOR, 128, 0.5f),
			CCOUNT);

	public static final int COUNT2 = 30;
	public static Color[] PENDING2 = Utility.getColorsBetween(PENDING_COLOR,
			Utility.alphaDarker(PENDING_COLOR, 32, 0.5f), COUNT2);
	public static Color[] SOLVED2 = Utility.getColorsBetween(SOLVED_COLOR, Utility.alphaDarker(SOLVED_COLOR, 32, 0.5f),
			COUNT2);
	public static Color[] FAILED2 = Utility.getColorsBetween(FAILED_COLOR, Utility.alphaDarker(FAILED_COLOR, 32, 0.5f),
			COUNT2);

	public static Color[] PENDING3 = Utility.getColorsBetween(PENDING_COLOR, Utility.darker(PENDING_COLOR, 0.5f),
			COUNT2);
	public static Color[] SOLVED3 = Utility.getColorsBetween(SOLVED_COLOR, Utility.darker(SOLVED_COLOR, 0.5f), COUNT2);
	public static Color[] FIRST_TO_SOLVE3 = Utility.getColorsBetween(FIRST_TO_SOLVE_COLOR,
			Utility.darker(FIRST_TO_SOLVE_COLOR, 0.5f), COUNT2);
	public static Color[] FAILED3 = Utility.getColorsBetween(FAILED_COLOR, Utility.darker(FAILED_COLOR, 0.5f), COUNT2);

	public static Color[] colorText = Utility.getColorsBetween(Color.darkGray, Color.white, CCOUNT);

	public static Color getStatusColor(Status status, boolean recent, float flash) {
		int k = 0;
		if (recent) {
			k = (int) ((flash * 1.5f) % (ICPCColors.CCOUNT * 2)); // flash more than once per second
			if (k > (ICPCColors.CCOUNT - 1))
				k = (ICPCColors.CCOUNT * 2 - 1) - k;
		}
		if (status == Status.SOLVED)
			return ICPCColors.SOLVED[k];
		else if (status == Status.FAILED)
			return ICPCColors.FAILED[k];
		else if (status != null)
			return ICPCColors.PENDING[k];
		return null;
	}

	/**
	 * Return a status color that fades over 30s
	 *
	 * @param status
	 * @param age the age in ms
	 * @return
	 */
	public static Color getStatusColor(Status status, long age) {
		if (age < 0 || age > 30000)
			return null;

		int k = (int) ((COUNT2 - 1) * age / 30000.0);
		if (k >= COUNT2)
			k = COUNT2 - 1;
		if (status == Status.SOLVED)
			return ICPCColors.SOLVED2[k];
		else if (status == Status.FAILED)
			return ICPCColors.FAILED2[k];

		return ICPCColors.PENDING2[k];
	}
}