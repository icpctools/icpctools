package org.icpc.tools.contest.model.internal;

import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IResolverPresentation;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ResolutionStep;

/**
 * A hint presentation in the resolver.
 *
 * withX() methods change the existing object (clearing any old data, e.g. if you select a team
 * then the previous problem and submission id are reset) and then return a clone(), so you can do:
 * <ol>
 * <li>steps.add(new
 * PresentationStep(PresentationStep.Presentations.TEAM_LIST).withAwardId(award.getId())
 * .withScroll(true));</li>
 * </ol>
 * and not worry about creating new objects each time.
 */
public class ResolverPresentation extends ContestObject implements IResolverPresentation, ResolutionStep {
	private static final String PRESENTATION = "presentation";
	private static final String TEAM_ID = "team_id";
	private static final String MESSAGE = "message";
	private static final String SCROLL = "scroll";
	private static final String AWARD_ID = "award_id";
	private static final String SUBMISSION_IDS = "submission_ids";

	public Presentations p;
	public String kind;
	public String message;
	public String teamId;
	public Boolean scroll;
	public String awardId;
	public String[] submissionIds;

	@Override
	public ContestType getType() {
		return ContestType.RESOLVER_PRESENTATION;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public ResolverPresentation() {
		super();
	}

	public ResolverPresentation(Presentations p) {
		super();
		this.kind = p.name().toLowerCase();
	}

	@Override
	public String getKind() {
		return kind;
	}

	@Override
	public String getTeamId() {
		return teamId;
	}

	@Override
	public String getAwardId() {
		return awardId;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public Boolean getScroll() {
		return scroll;
	}

	@Override
	public String[] getSubmissionIds() {
		return submissionIds;
	}

	public Presentations getPKind() {
		if (kind == null)
			return null;

		try {
			return Presentations.valueOf(kind);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public IContestObject clone() {
		ResolverPresentation s = new ResolverPresentation();
		s.teamId = teamId;
		s.kind = kind;
		s.awardId = awardId;
		s.message = message;
		s.submissionIds = submissionIds;
		return s;
	}

	@Override
	protected boolean addImpl(String name, Object value) throws Exception {
		if (TEAM_ID.equals(name)) {
			teamId = (String) value;
			return true;
		} else if (SUBMISSION_IDS.equals(name)) {
			try {
				Object[] ob = (Object[]) value;
				submissionIds = new String[ob.length];
				for (int i = 0; i < ob.length; i++)
					submissionIds[i] = (String) ob[i];
			} catch (Exception e) {
				// ignore
			}
			return true;
		} else if (PRESENTATION.equals(name)) {
			kind = (String) value;
			return true;
		} else if (AWARD_ID.equals(name)) {
			awardId = (String) value;
			return true;
		} else if (MESSAGE.equals(name)) {
			message = (String) value;
			return true;
		} else if (SCROLL.equals(name)) {
			scroll = Boolean.valueOf((String) value);
			return true;
		}
		return super.addImpl(name, value);
	}

	@Override
	protected void getProperties(Properties props) {
		props.addLiteralString(PRESENTATION, kind); // TODO
		if (kind != null) {
			props.addLiteralString(PRESENTATION, kind.toLowerCase());
		}
		if (teamId != null) {
			props.addLiteralString(TEAM_ID, teamId);
		}
		if (awardId != null) {
			props.addLiteralString(AWARD_ID, awardId);
		}
		if (message != null) {
			props.addLiteralString(MESSAGE, message);
		}
		if (scroll != null) {
			props.addLiteralString(SCROLL, scroll ? "true" : "false");
		}
		props.addArray(SUBMISSION_IDS, submissionIds);
		super.getProperties(props);
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = super.validate(c);

		if (errors.isEmpty())
			return null;
		return errors;
	}

	public ResolverPresentation withTeamId(String newTeamId) {
		teamId = newTeamId;
		return this;
	}

	public ResolverPresentation withSubmissionIds(String[] newSubIds) {
		submissionIds = newSubIds;
		return this;
	}

	public ResolverPresentation withMessage(String newMessage) {
		message = newMessage;
		return this;
	}

	public ResolverPresentation withScroll(Boolean newScroll) {
		scroll = newScroll;
		return this;
	}

	public ResolverPresentation withAwardId(String newAwardId) {
		awardId = newAwardId;
		return this;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ResolverPresentation [");
		sb.append(kind);

		if (submissionIds != null || submissionIds.length == 1) {
			sb.append(" with submission " + submissionIds[0]);
		} else {
			sb.append(" with " + submissionIds.length + " submissions");
		}

		if (teamId != null) {
			sb.append(" with team " + teamId);
		}
		if (awardId != null) {
			sb.append(" with award " + awardId);
		}
		if (message != null) {
			sb.append(" with message " + message);
		}
		if (scroll != null) {
			sb.append(" with scroll " + scroll);
		}
		sb.append("]");
		return sb.toString();
	}
}