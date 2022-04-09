package org.icpc.tools.contest.model;

/**
 * An account.
 */
public interface IAccount extends IContestObject {
	public String ADMIN = "admin";
	public String STAFF = "staff";
	public String JUDGE = "judge";
	public String ANALYST = "analyst";
	public String TEAM = "team";
	public String BALLOON = "balloon";
	public String PRES_ADMIN = "presAdmin";

	/**
	 * The username
	 *
	 * @return the username
	 */
	String getUsername();

	/**
	 * The password.
	 *
	 * @return the password
	 */
	String getPassword();

	/**
	 * The type of account.
	 *
	 * @return the type
	 */
	String getAccountType();

	/**
	 * The ip address.
	 *
	 * @return the ip
	 */
	String getIp();

	/**
	 * The id of the team they belong to.
	 *
	 * @return the id
	 */
	String getTeamId();

	/**
	 * The person this account is for.
	 *
	 * @return the id
	 */
	String getPersonId();
}