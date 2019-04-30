package org.icpc.tools.resolver.awards;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IAward.AwardType;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.AwardUtil;

public class WorldFinalsAwardDialog extends AbstractAwardDialog {
	public WorldFinalsAwardDialog(Shell parent, Contest contest) {
		super(parent, contest);
	}

	@Override
	protected String getTitle() {
		return "World Finals";
	}

	@Override
	protected String getDescription() {
		return "Assign the same set of awards that are normally awarded at the ICPC World Finals.";
	}

	@Override
	protected void createAwardUI(Composite comp) {
		// do nothing
	}

	@Override
	protected AwardType[] getAwardTypes() {
		return new AwardType[] { IAward.WINNER, IAward.FIRST_TO_SOLVE, IAward.GROUP, IAward.MEDAL, IAward.SOLUTION };
	}

	@Override
	protected void applyAwards(Contest aContest) {
		AwardUtil.createWorldFinalsAwards(aContest);
	}
}