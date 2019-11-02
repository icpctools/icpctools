package org.icpc.tools.contest.model.internal;

import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IRun;
import org.icpc.tools.contest.model.feed.Decimal;
import org.icpc.tools.contest.model.feed.JSONEncoder;

public class Run extends TimedEvent implements IRun {
	private static final String JUDGEMENT_ID = "judgement_id";
	private static final String JUDGEMENT_TYPE_ID = "judgement_type_id";
	private static final String ORDINAL = "ordinal";
	private static final String RUN_TIME = "run_time";

	private String judgementId;
	private String judgementTypeId;
	private int ordinal;
	private int runTime;

	@Override
	public ContestType getType() {
		return ContestType.RUN;
	}

	@Override
	public String getJudgementId() {
		return judgementId;
	}

	@Override
	public String getJudgementTypeId() {
		return judgementTypeId;
	}

	@Override
	public int getOrdinal() {
		return ordinal;
	}

	@Override
	public int getRunTime() {
		return runTime;
	}

	@Override
	protected boolean addImpl(String name, Object value) throws Exception {
		if (JUDGEMENT_ID.equals(name)) {
			judgementId = (String) value;
			return true;
		} else if (ORDINAL.equals(name)) {
			ordinal = parseInt(value);
			return true;
		} else if (JUDGEMENT_TYPE_ID.equals(name)) {
			judgementTypeId = (String) value;
			return true;
		} else if (RUN_TIME.equals(name)) {
			runTime = Decimal.parse((String) value);
			return true;
		}

		return super.addImpl(name, value);
	}

	@Override
	public IContestObject clone() {
		Run r = new Run();
		r.id = id;
		r.judgementId = judgementId;
		r.judgementTypeId = judgementTypeId;
		r.ordinal = ordinal;
		r.runTime = runTime;
		r.contestTime = contestTime;
		r.time = time;
		return r;
	}

	@Override
	protected void getPropertiesImpl(Map<String, Object> props) {
		super.getPropertiesImpl(props);
		props.put(JUDGEMENT_ID, judgementId);
		props.put(JUDGEMENT_TYPE_ID, judgementTypeId);
		props.put(ORDINAL, ordinal);
		if (runTime > 0)
			props.put(RUN_TIME, Decimal.format(runTime));
	}

	@Override
	public void writeBody(JSONEncoder je) {
		je.encode(ID, id);
		je.encode(JUDGEMENT_ID, judgementId);
		je.encode(JUDGEMENT_TYPE_ID, judgementTypeId);
		je.encode(ORDINAL, ordinal);
		if (runTime > 0)
			je.encodePrimitive(RUN_TIME, Decimal.format(runTime));
		encodeTimeProperties(je);
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = super.validate(c);

		if (c.getJudgementById(judgementId) == null)
			errors.add("Invalid judgement " + judgementId);

		if (c.getJudgementTypeById(judgementTypeId) == null)
			errors.add("Invalid judgement type " + judgementTypeId);

		if (errors.isEmpty())
			return null;
		return errors;
	}
}