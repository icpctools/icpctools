package org.icpc.tools.contest.model.internal;

import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.feed.Decimal;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.feed.RelativeTime;
import org.icpc.tools.contest.model.feed.Timestamp;

public class Judgement extends ContestObject implements IJudgement {
	private static final String START_CONTEST_TIME = "start_contest_time";
	private static final String START_TIME = "start_time";
	private static final String SUBMISSION_ID = "submission_id";
	private static final String JUDGEMENT_TYPE_ID = "judgement_type_id";
	private static final String END_CONTEST_TIME = "end_contest_time";
	private static final String END_TIME = "end_time";
	private static final String MAX_RUN_TIME = "max_run_time";

	private String submissionId;
	private String judgementTypeId;
	protected int startContestTime = Integer.MIN_VALUE;
	protected long startTime = Long.MIN_VALUE;
	protected Integer endContestTime;
	protected Long endTime;
	protected int maxRunTime;

	public Judgement() {
		// default constructor
	}

	public Judgement(String id, ISubmission submission, String judgementTypeId) {
		super(id);
		this.submissionId = submission.getId();
		this.judgementTypeId = judgementTypeId;
		this.startContestTime = submission.getContestTime();
		this.startTime = submission.getTime();
		this.endContestTime = submission.getContestTime();
		this.endTime = submission.getTime();
	}

	@Override
	public ContestType getType() {
		return ContestType.JUDGEMENT;
	}

	@Override
	public String getSubmissionId() {
		return submissionId;
	}

	@Override
	public String getJudgementTypeId() {
		return judgementTypeId;
	}

	public void setJudgementTypeId(String id) {
		judgementTypeId = id;
	}

	@Override
	public int getStartContestTime() {
		return startContestTime;
	}

	@Override
	public long getStartTime() {
		return startTime;
	}

	@Override
	public Integer getEndContestTime() {
		return endContestTime;
	}

	@Override
	public Long getEndTime() {
		return endTime;
	}

	@Override
	public int getMaxRunTime() {
		return maxRunTime;
	}

	@Override
	protected boolean addImpl(String name, Object value) throws Exception {
		if (SUBMISSION_ID.equals(name)) {
			submissionId = (String) value;
			return true;
		} else if (JUDGEMENT_TYPE_ID.equals(name)) {
			judgementTypeId = (String) value;
			return true;
		} else if (MAX_RUN_TIME.equals(name)) {
			maxRunTime = Decimal.parse((String) value);
			return true;
		} else if (START_CONTEST_TIME.equals(name)) {
			startContestTime = parseRelativeTime(value);
			return true;
		} else if (START_TIME.equals(name)) {
			startTime = parseTimestamp(value);
			return true;
		} else if (END_CONTEST_TIME.equals(name)) {
			endContestTime = parseRelativeTime(value);
			return true;
		} else if (END_TIME.equals(name)) {
			endTime = parseTimestamp(value);
			return true;
		}
		return super.addImpl(name, value);
	}

	@Override
	public IContestObject clone() {
		Judgement j = new Judgement();
		j.id = id;
		j.submissionId = submissionId;
		j.judgementTypeId = judgementTypeId;
		j.maxRunTime = maxRunTime;
		j.startContestTime = startContestTime;
		j.startTime = startTime;
		j.endContestTime = endContestTime;
		j.endTime = endTime;
		return j;
	}

	@Override
	protected void getPropertiesImpl(Map<String, Object> props) {
		super.getPropertiesImpl(props);
		props.put(SUBMISSION_ID, submissionId);
		props.put(JUDGEMENT_TYPE_ID, judgementTypeId);
		if (maxRunTime > 0)
			props.put(MAX_RUN_TIME, Decimal.format(maxRunTime));
		try {
			if (startContestTime != Integer.MIN_VALUE)
				props.put(START_CONTEST_TIME, RelativeTime.format(startContestTime));
			if (startTime != Long.MIN_VALUE)
				props.put(START_TIME, Timestamp.format(startTime));
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Invalid time: " + startContestTime + " / " + startTime, e);
		}
		try {
			if (endContestTime != null)
				props.put(END_CONTEST_TIME, RelativeTime.format(endContestTime));
			if (endTime != null)
				props.put(END_TIME, Timestamp.format(endTime));
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Invalid time: " + endContestTime + " / " + endTime, e);
		}
	}

	@Override
	public void writeBody(JSONEncoder je) {
		je.encode(ID, id);
		je.encode(SUBMISSION_ID, submissionId);
		je.encode(JUDGEMENT_TYPE_ID, judgementTypeId);
		if (maxRunTime > 0)
			je.encodePrimitive(MAX_RUN_TIME, Decimal.format(maxRunTime));
		try {
			if (startContestTime == Integer.MIN_VALUE)
				je.encode(START_CONTEST_TIME);
			else
				je.encodeString(START_CONTEST_TIME, RelativeTime.format(startContestTime));
			if (startTime == Long.MIN_VALUE)
				je.encode(START_TIME);
			else
				je.encodeString(START_TIME, Timestamp.format(startTime));
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Invalid time: " + startContestTime + " / " + startTime, e);
		}
		try {
			if (endContestTime == null)
				je.encode(END_CONTEST_TIME);
			else
				je.encodeString(END_CONTEST_TIME, RelativeTime.format(endContestTime));
			if (endTime == null)
				je.encode(END_TIME);
			else
				je.encodeString(END_TIME, Timestamp.format(endTime));
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Invalid time: " + endContestTime + " / " + endTime, e);
		}
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = super.validate(c);

		if (c.getSubmissionById(submissionId) == null)
			errors.add("Invalid submission " + submissionId);

		if (endContestTime != null || endTime != null) {
			if (judgementTypeId == null)
				errors.add("Missing judgement");

			if (c.getJudgementTypeById(judgementTypeId) == null)
				errors.add("Invalid judgementType " + judgementTypeId);

			if (endContestTime == null)
				errors.add("Missing end contest time");

			if (endTime == null)
				errors.add("Missing end time");
		}

		if (errors.isEmpty())
			return null;
		return errors;
	}
}