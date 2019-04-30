package org.icpc.tools.contest.model;

import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IContestObject.ContestType;

public class TypeFilter implements IContestObjectFilter {
	protected List<ContestType> types = new ArrayList<>();

	public TypeFilter() {
		// do nothing
	}

	public TypeFilter(ContestType type) {
		types.add(type);
	}

	public TypeFilter(List<ContestType> types) {
		this.types = types;
	}

	public void addType(ContestType type) {
		types.add(type);
	}

	public boolean hasTypes() {
		return !types.isEmpty();
	}

	@Override
	public IContestObject filter(IContestObject co) {
		for (ContestType type : types) {
			if (type.equals(co.getType()))
				return co;
		}

		return null;
	}
}