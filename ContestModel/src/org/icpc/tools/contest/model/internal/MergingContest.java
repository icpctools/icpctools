package org.icpc.tools.contest.model.internal;

import java.util.Map;

import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObject.ContestType;

/**
 * A contest that stores no history and merges all equal config objects, e.g. if I add team 27 with
 * 2 attributes and then add team 27 later with 3 attributes (but one is already there), team 27
 * will have 4 attributes and the one attribute will have the latest value.
 */
public class MergingContest extends Contest {
	public MergingContest() {
		super(false);
	}

	@Override
	public void add(IContestObject obj) {
		if (obj == null)
			return;

		if (!isConfigElement(obj)) {
			super.add(obj);
			return;
		}

		IContestObject currentObj = getObjectByTypeAndId(obj.getType(), obj.getId());
		if (currentObj == null) {
			super.add(obj);
			return;
		}

		Map<String, Object> props = obj.getProperties();
		for (String key : props.keySet())
			((ContestObject) currentObj).add(key, props.get(key));

		super.add(obj);
	}

	private static boolean isConfigElement(IContestObject obj) {
		ContestType type = obj.getType();
		return (type == ContestType.CONTEST || type == ContestType.PROBLEM || type == ContestType.GROUP
				|| type == ContestType.LANGUAGE || type == ContestType.JUDGEMENT_TYPE || type == ContestType.TEAM
				|| type == ContestType.PERSON || type == ContestType.ORGANIZATION);
	}
}