package org.icpc.tools.contest.model.resolver;

/**
 * An enumeration listing the different types of "selection" which can be displayed.
 */
public enum SelectType {
	/** Indicator for the "normal" selection type (blue by default) */
	NORMAL,
	/**
	 * Indication for "highlighted selection" - a white-box outline indicating that the next click
	 * will switch to an Award screen.
	 */
	HIGHLIGHT,
	/** Indication for "first-to-solve" selection (green by default) */
	FTS,
	/**
	 * Indication for "highlighted first-to-solve" selection (FTS selection plus a white-box outline
	 */
	FTS_HIGHLIGHT,
	/**
	 * Indication for a "team list" or multi-selected teams (bronze by default)
	 */
	TEAM_LIST
}