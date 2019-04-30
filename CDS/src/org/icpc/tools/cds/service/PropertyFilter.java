package org.icpc.tools.cds.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObjectFilter;

public class PropertyFilter implements IContestObjectFilter {
	protected List<PFilter> filters = new ArrayList<>();

	private static class PFilter {
		String name;
		String value;
	}

	public PropertyFilter() {
		// no filter
	}

	public void addFilter(String name, String value) {
		PFilter pf = new PFilter();
		pf.name = name;
		pf.value = value;
		filters.add(pf);
	}

	public boolean hasProperties() {
		return !filters.isEmpty();
	}

	@Override
	public IContestObject filter(IContestObject co) {
		if (filters == null)
			return co;

		Map<String, Object> props = co.getProperties();
		for (PFilter pf : filters) {
			boolean found = false;
			for (String key : props.keySet()) {
				if (key.equals(pf.name) && props.get(key).equals(pf.value)) {
					found = true;
					continue;
				}
			}
			if (!found)
				return null;
		}

		return co;
	}
}