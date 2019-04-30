package org.icpc.tools.cds.service;

import java.util.HashMap;
import java.util.Map;

import org.icpc.tools.contest.model.IJudgementType;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.Judgement;

public class OptimisticScoreboardService {
	public static void beOptimistic(Contest contest) {
		IJudgementType solvedJT = null;
		IJudgementType failedJT = null;

		for (IJudgementType type : contest.getJudgementTypes()) {
			if (!type.isSolved() && type.isPenalty())
				failedJT = type;
			else if (type.isSolved())
				solvedJT = type;
		}

		Map<String, ISubmission> submissions = new HashMap<>();
		for (ISubmission submission : contest.getSubmissions()) {
			if (contest.isJudged(submission))
				continue;

			String key = submission.getTeamId() + "-" + submission.getProblemId();
			ISubmission existing = submissions.get(key);
			if (existing != null)
				contest.add(new Judgement(existing.getId(), existing, failedJT.getId()));

			submissions.put(key, submission);
		}

		for (String key : submissions.keySet()) {
			ISubmission submission = submissions.get(key);
			contest.add(new Judgement(submission.getId(), submission, solvedJT.getId()));
		}
	}
}