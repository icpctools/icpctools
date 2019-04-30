package org.icpc.tools.presentation.admin.internal;

import java.util.List;

import org.icpc.tools.presentation.core.internal.PresentationInfo;

public class CompositePresentationInfo extends PresentationInfo {
	protected List<PresentationInfo> infos;

	public CompositePresentationInfo(String name, String category, String command, String description,
			List<PresentationInfo> infos) {
		super(name, name, category, command, description, null, null, false);
		this.infos = infos;
	}
}