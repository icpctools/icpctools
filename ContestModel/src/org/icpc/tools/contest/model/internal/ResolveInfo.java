package org.icpc.tools.contest.model.internal;

import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IResolveInfo;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;

public class ResolveInfo extends ContestObject implements IResolveInfo {
	private static final String CLICKS = "clicks";
	private static final String SPEED_FACTOR = "speed_factor";
	private static final String SCROLL_SPEED_FACTOR = "scroll_speed_factor";
	private static final String ANIMATION_PAUSE = "animation_pause";
	private static final String SINGLE_STEP_ROW = "single_step_row";
	private static final String ROW_OFFSET = "row_offset";
	private static final String PREDETERMINED_STEPS = "predetermined_steps";
	private static final String TEAM_LABEL = "team_label";
	private static final String PROBLEM_LABEL = "problem_label";

	private int clicks = Integer.MIN_VALUE;
	private double speedFactor = Double.NaN;
	private double scrollSpeedFactor = Double.NaN;
	private boolean animationPause = false;
	private int singleStepRow = Integer.MIN_VALUE;
	private int rowOffset = Integer.MIN_VALUE;
	private List<PredeterminedStep> predeterminedSteps;

	public ResolveInfo() {
		// create an default resolve info
	}

	@Override
	public ContestType getType() {
		return ContestType.RESOLVE_INFO;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public int getClicks() {
		return clicks;
	}

	@Override
	public double getSpeedFactor() {
		return speedFactor;
	}

	@Override
	public double getScrollSpeedFactor() {
		return scrollSpeedFactor;
	}

	@Override
	public boolean isAnimationPaused() {
		return animationPause;
	}

	@Override
	public int getSingleStepRow() {
		return singleStepRow;
	}

	@Override
	public int getRowOffset() {
		return rowOffset;
	}

	@Override
	public List<PredeterminedStep> getPredeterminedSteps() {
		return predeterminedSteps;
	}

	@Override
	protected boolean addImpl(String name, Object value) throws Exception {
		switch (name) {
			case CLICKS: {
				try {
					clicks = Integer.parseInt((String) value);
				} catch (Exception e) {
					// ignore
				}
				return true;
			}
			case SPEED_FACTOR: {
				try {
					speedFactor = Double.parseDouble((String) value);
				} catch (Exception e) {
					// ignore
				}
				return true;
			}
			case SCROLL_SPEED_FACTOR: {
				try {
					scrollSpeedFactor = Double.parseDouble((String) value);
				} catch (Exception e) {
					// ignore
				}
				return true;
			}
			case ANIMATION_PAUSE: {
				try {
					animationPause = Boolean.parseBoolean((String) value);
				} catch (Exception e) {
					// ignore
				}
				return true;
			}
			case SINGLE_STEP_ROW: {
				try {
					singleStepRow = Integer.parseInt((String) value);
				} catch (Exception e) {
					// ignore
				}
				return true;
			}
			case ROW_OFFSET: {
				try {
					rowOffset = Integer.parseInt((String) value);
				} catch (Exception e) {
					// ignore
				}
				return true;
			}
			case PREDETERMINED_STEPS: {
				try {
					predeterminedSteps = new ArrayList<>();
					Object[] steps = JSONParser.getOrReadArray(value);
					for (Object step : steps) {
						JsonObject obj = (JsonObject) step;
						String teamLabel = obj.getString(TEAM_LABEL);
						String problemLabel = obj.getString(PROBLEM_LABEL);
						predeterminedSteps.add(new PredeterminedStep(teamLabel, problemLabel));
					}
				} catch (Exception e) {
					// ignore
				}
				return true;
			}
		}
		return false;
	}

	@Override
	protected void getProperties(Properties props) {
		if (clicks != Integer.MIN_VALUE)
			props.addInt(CLICKS, clicks);
		if (!Double.isNaN(speedFactor))
			props.addDouble(SPEED_FACTOR, speedFactor);
		if (!Double.isNaN(scrollSpeedFactor))
			props.addDouble(SCROLL_SPEED_FACTOR, scrollSpeedFactor);
		if (animationPause)
			props.add(ANIMATION_PAUSE, "true");
		if (singleStepRow >= 0)
			props.addInt(SINGLE_STEP_ROW, singleStepRow);
		if (rowOffset >= 0)
			props.addInt(ROW_OFFSET, rowOffset);

		if (predeterminedSteps != null) {
			StringBuilder sb = new StringBuilder("[");
			boolean first = true;
			for (PredeterminedStep step : predeterminedSteps) {
				if (!first)
					sb.append(", ");
				first = false;
				sb.append("{ \"team_label\": \"");
				sb.append(step.teamLabel);
				sb.append(", \"problem_label\": \"");
				sb.append(step.problemLabel);
				sb.append("\" }");
			}
			sb.append("]");
			props.addLiteralString(PREDETERMINED_STEPS, sb.toString());
		}
	}

	@Override
	public IContestObject clone() {
		ResolveInfo r = new ResolveInfo();
		r.id = id;
		r.clicks = clicks;
		r.speedFactor = speedFactor;
		r.scrollSpeedFactor = scrollSpeedFactor;
		r.animationPause = animationPause;
		r.singleStepRow = singleStepRow;
		r.rowOffset = rowOffset;
		r.predeterminedSteps = predeterminedSteps;
		return r;
	}

	public void setClicks(int clicks) {
		this.clicks = clicks;
	}

	public void setSpeedFactor(double speedFactor) {
		this.speedFactor = speedFactor;
	}

	public void setScrollSpeedFactor(double scrollSpeedFactor) {
		this.scrollSpeedFactor = scrollSpeedFactor;
	}

	public void setAnimationPause(boolean animPause) {
		this.animationPause = animPause;
	}

	public void setSingleStepRow(int singleStepRow) {
		this.singleStepRow = singleStepRow;
	}

	public void setRowOffset(int rowOffset) {
		this.rowOffset = rowOffset;
	}
}