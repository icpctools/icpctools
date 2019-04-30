package org.icpc.tools.cds.service;

import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObjectFilter;

public class CompositeFilter implements IContestObjectFilter {
	private List<IContestObjectFilter> filters = new ArrayList<>();

	public CompositeFilter() {
		// do nothing
	}

	public void addFilter(IContestObjectFilter filter) {
		if (filter == null)
			return;

		filters.add(filter);
	}

	@Override
	public IContestObject filter(IContestObject co) {
		IContestObject obj = co;
		for (IContestObjectFilter filter : filters) {
			obj = filter.filter(obj);
			if (obj == null)
				return null;
		}
		return obj;
	}
}