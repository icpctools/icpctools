package org.icpc.tools.presentation.contest.internal.chart;

import java.awt.Color;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ILanguage;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.presentation.contest.internal.ContestData;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.Utility;
import org.icpc.tools.presentation.core.chart.AbstractChartPresentation;
import org.icpc.tools.presentation.core.chart.Series;

public class LanguageSummaryChart extends AbstractChartPresentation {
	protected static final Color[] clr = new Color[] { ICPCColors.BLUE, ICPCColors.YELLOW, ICPCColors.RED };

	protected IContest contest = ContestData.getContest();

	public LanguageSummaryChart() {
		super(Type.BAR, "Attempts & Solutions by Language", "# of submissions");
		setFont(ICPCFont.getMasterFont());
	}

	@Override
	protected void setupChart() {
		if (contest == null)
			return;

		ILanguage[] langs = contest.getLanguages();
		if (langs == null)
			return;

		int numLangs = langs.length;
		String[] hLabels = new String[numLangs];
		Color[] colors = new Color[numLangs];
		Color[] colors2 = new Color[numLangs];

		for (int i = 0; i < numLangs; i++) {
			colors[i] = clr[i % clr.length];
			colors2[i] = Utility.alphaDarker(colors[i], 200, 0.85f);
			hLabels[i] = langs[i].getName();
		}

		setHorizontalLabels(hLabels);
		setSeries(new Series(colors), new Series(colors2));

		getSeries()[0].setTitle("# of submissions");
		getSeries()[1].setTitle("# of solutions");
	}

	@Override
	protected void updateData() {
		if (contest == null)
			return;

		ILanguage[] langs = contest.getLanguages();
		if (langs == null)
			return;

		int numLangs = langs.length;
		int[] totalAttempts = new int[numLangs];
		int[] totalSolved = new int[numLangs];

		ISubmission[] submissions = contest.getSubmissions();
		for (ISubmission submission : submissions) {
			String langId = submission.getLanguageId();
			int n = -1;
			for (int i = 0; i < numLangs; i++)
				if (langs[i].getId().equals(langId))
					n = i;

			if (n >= 0) {
				totalAttempts[n]++;
				if (contest.isSolved(submission))
					totalSolved[n]++;
			}
		}

		getSeries()[0].setValues(totalAttempts);
		getSeries()[1].setValues(totalSolved);
	}
}