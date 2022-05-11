package org.icpc.tools.contest.model.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IDelete;

public class Deletion implements IDelete {
	private String id;
	private ContestType type;

	public Deletion() {
		// empty
	}

	public Deletion(String id, ContestType type) {
		this.id = id;
		this.type = type;
	}

	@Override
	public ContestType getType() {
		return type;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Map<String, Object> getProperties() {
		return null;
	}

	@Override
	public List<String> validate(IContest contest) {
		return new ArrayList<>(0);
	}

	@Override
	public Deletion clone() {
		return new Deletion(id, type);
	}
}