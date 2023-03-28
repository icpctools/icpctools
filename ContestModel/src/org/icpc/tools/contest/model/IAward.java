package org.icpc.tools.contest.model;

public interface IAward extends IContestObject {
	class AwardType {
		private String name;
		private String regEx;

		public AwardType(String name, String regEx) {
			this.name = name;
			this.regEx = regEx;
		}

		public String getName() {
			return name;
		}

		public String getRexEx() {
			return regEx;
		}

		public String getPattern(String id) {
			return regEx.replace(".*", id);
		}

		public String getPattern(int id) {
			return regEx.replace(".*", id + "");
		}

		@Override
		public String toString() {
			return "AwardType [" + name + "/" + regEx + "]";
		}
	}

	// An optional attribute that controls how to display the award in the resolver.
	// The default 'detail' (or null/missing display mode) will stop to show the team photo and
	// details.
	// 'Pause' will pause but then move on.
	// 'List' will stop to show a list, but only after all teams have been resolved.
	// 'Ignore' will skip award (but will be listed if you stop for detail for another award).
	public enum DisplayMode {
		DETAIL, PAUSE, LIST, IGNORE
	}

	AwardType WINNER = new AwardType("Winner", "winner");
	AwardType RANK = new AwardType("Rank", "rank-.*");
	AwardType MEDAL = new AwardType("Medal", ".*-medal");
	AwardType FIRST_TO_SOLVE = new AwardType("First to Solve", "first-to-solve-.*");
	AwardType GROUP = new AwardType("Group Winner", "group-winner-.*");
	AwardType ORGANIZATION = new AwardType("Organization Winner", "organization-winner-.*");
	AwardType GROUP_HIGHLIGHT = new AwardType("Group Highlight", "group-highlight-.*");
	AwardType SOLVED = new AwardType("Solved", "solved-.*");
	AwardType TOP = new AwardType("Top", "top-.*");
	AwardType HONORS = new AwardType("Honors", "honors-.*");
	// AwardType HONORABLE_MENTION = new AwardType("Honorable Mention", "honorable-mention");
	AwardType EXPECTED_TO_ADVANCE = new AwardType("Expected to Advance", "expected-to-advance");
	AwardType OTHER = new AwardType("Other", ".*");

	AwardType[] KNOWN_TYPES = new AwardType[] { WINNER, RANK, MEDAL, FIRST_TO_SOLVE, GROUP, ORGANIZATION,
			GROUP_HIGHLIGHT, SOLVED, TOP, HONORS, EXPECTED_TO_ADVANCE, OTHER };

	/**
	 * Returns the ids of the teams that this award is for.
	 *
	 * @return the team id
	 */
	String[] getTeamIds();

	/**
	 * Returns the type of award using the award type constants.
	 *
	 * @return an award type constant, e.g. one of the MEDALs
	 */
	AwardType getAwardType();

	/**
	 * Return the awards for an award template, e.g. number of awards given.
	 *
	 * @return
	 */
	String getParameter();

	/**
	 * Return the citation for this award.
	 *
	 * @return
	 */
	String getCitation();

	/**
	 * Returns true if the display mode has been set.
	 *
	 * @return
	 */
	boolean hasDisplayMode();

	/**
	 * Returns the resolver display mode.
	 *
	 * @return
	 */
	DisplayMode getDisplayMode();
}