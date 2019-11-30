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
	}

	AwardType WINNER = new AwardType("Winner", "winner");
	AwardType RANK = new AwardType("Rank", "rank-.*");
	AwardType MEDAL = new AwardType("Medal", ".*-medal");
	AwardType FIRST_TO_SOLVE = new AwardType("First to Solve", "first-to-solve-.*");
	AwardType GROUP = new AwardType("Group Winner", "group-winner-.*");
	AwardType ORGANIZATION = new AwardType("Organization Winner", "organization-winner-.*");
	AwardType GROUP_HIGHLIGHT = new AwardType("Group Highlight", "group-highlight-.*");
	AwardType SOLUTION = new AwardType("Solution", "solution-.*");
	AwardType OTHER = new AwardType("Other", ".*");

	AwardType[] KNOWN_TYPES = new AwardType[] { WINNER, RANK, MEDAL, FIRST_TO_SOLVE, GROUP,
			ORGANIZATION, GROUP_HIGHLIGHT, SOLUTION, OTHER };

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
	 * Return the citation for this award.
	 *
	 * @return
	 */
	String getCitation();

	/**
	 * Returns <code>true</code> if the award should be recognized by showing on a separate screen
	 * with the teams photo or other graphics, and <code>false</code> otherwise.
	 *
	 * @return
	 */
	boolean showAward();
}