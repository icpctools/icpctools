package org.icpc.tools.presentation.contest.internal.presentations.resolver;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.FileReference;
import org.icpc.tools.contest.model.internal.FileReferenceList;
import org.icpc.tools.contest.model.resolver.SelectType;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;
import org.icpc.tools.presentation.contest.internal.Animator3D;
import org.icpc.tools.presentation.contest.internal.ICPCColors;

public class OrgsPresentation extends AbstractICPCPresentation {
	protected static final Movement SIZE_MOVEMENT = new Movement(30, 30);
	protected static final Movement FADE_MOVEMENT = new Movement(220, 220);

	private Map<String, BufferedImage> map = new HashMap<>();
	private Map<String, Animator3D> anim = new HashMap<>();
	private Animator sizeAnim = new Animator(25.0, SIZE_MOVEMENT);
	private int numTeams = Integer.MAX_VALUE;
	private int lastImgSize;
	private SelectType selectType = SelectType.NORMAL;
	private List<ITeam> selectedTeams = null;

	private void setTargets() {
		IContest contest = getContest();
		if (contest == null)
			return;

		if (numTeams == 0 || width == 0 || height == 0)
			return;

		// find size
		int size = (int) (Math.sqrt(width * height / numTeams) * 1.0);

		int num2 = width / size * (height / size) - ((height / size) / 2);
		while (num2 < numTeams && size > 5) {
			size--;
			num2 = (width / size) * (height / size) - ((height / size) / 2);
		}
		sizeAnim.setTarget(size * 0.9);

		int dx = (width - (width / size) * size + size) / 2;
		int dy = (height - (height / size) * size + size) / 2;

		int i = dx;
		int j = dy;
		boolean oddRow = false;
		ITeam[] teams = contest.getOrderedTeams();
		int count = 0;
		for (int ii = 0; ii < teams.length; ii++) {
			Animator3D an = anim.get(teams[ii].getId());
			if (count >= numTeams) {
				an.setTarget(an.getXTarget(), an.getYTarget(), 0);
				continue;
			}
			count++;

			an.setTarget(i, j, 255);
			i += size;
			if (i > width - size / 2) {
				i = dx;
				if (!oddRow)
					i += size / 2;

				oddRow = !oddRow;
				j += size;
			}
		}
	}

	@Override
	public void setContest(IContest newContest) {
		super.setContest(newContest);

		if (anim.isEmpty()) {
			Dimension d = getSize();
			double mx = Math.min(d.width, d.height) / 10.0;
			mx = 250;
			Movement mov = new Movement(mx, mx);

			ITeam[] teams = newContest.getOrderedTeams();
			for (ITeam team : teams) {
				anim.put(team.getId(), new Animator3D(0, mov, 0, mov, 0, FADE_MOVEMENT));
			}

			numTeams = teams.length + 1;
		}
		setTargets();
	}

	public void setSelectedTeams(List<ITeam> teams, SelectType type) {
		selectType = type;
		selectedTeams = teams;

		if (selectedTeams == null)
			return;

		int last = 0;
		ITeam[] ordTeams = getContest().getOrderedTeams();
		for (int i = 0; i < ordTeams.length; i++) {
			if (teams.contains(ordTeams[i]))
				last = i;
		}
		if (last > 0)
			numTeams = last + 1;

		setTargets();
	}

	@Override
	public void aboutToShow() {
		IContest contest = getContest();
		if (contest == null)
			return;

		IOrganization[] orgs = contest.getOrganizations();
		if (orgs.length == 0)
			return;

		// set initial positions
		setTargets();

		if (sizeAnim.getValue() == 0)
			sizeAnim.resetToTarget();

		loadLogos();
	}

	protected void loadLogos() {
		IContest contest = getContest();
		if (contest == null)
			return;

		// load logo images
		int size2 = (int) (sizeAnim.getTarget() * 1.3);
		if (lastImgSize == size2)
			return;
		lastImgSize = size2;

		execute(new Runnable() {
			@Override
			public void run() {
				ITeam[] teams = contest.getOrderedTeams();
				for (int i = 0; i < teams.length; i++) {
					ITeam team = teams[i];
					IOrganization org = contest.getOrganizationById(team.getOrganizationId());
					if (org == null)
						continue;

					BufferedImage img = map.get(org.getId());

					// don't load a new image if we already have one and it is big enough or the team is
					// already out
					if (img != null && (img.getWidth() > size2 || img.getHeight() > size2 || i >= numTeams))
						continue;

					FileReference bestRef = null;
					FileReferenceList list = org.getLogo();
					if (list != null) {
						for (FileReference ref : list) {
							if (bestRef == null)
								bestRef = ref;
							else {
								if (bestRef.width < width && bestRef.height < height) {
									// current best image is too small - is this one better (larger than
									// current)?
									if (ref.width > bestRef.width || ref.height > bestRef.height)
										bestRef = ref;
								} else if (bestRef.width > width && bestRef.height > height) {
									// current image is too big - is this one better (smaller but still big
									// enough)?
									if (ref.width < bestRef.width || ref.height < bestRef.height) {
										if (ref.width >= width || ref.height >= height)
											bestRef = ref;
									}
								}
							}
						}
					}

					if (bestRef != null) {
						if (img == null || bestRef.width != img.getWidth() || bestRef.height != img.getHeight()) {
							map.put(org.getId(), org.getLogoImage(bestRef.width, bestRef.height, getModeTag(), true, false));
							if (img != null)
								img.flush();
						}
					}
				}
			}
		});
	}

	@Override
	public void incrementTimeMs(long dt) {
		for (Animator3D an : anim.values())
			an.incrementTimeMs(dt);

		sizeAnim.incrementTimeMs(dt);

		super.incrementTimeMs(dt);
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		setTargets();
	}

	@Override
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		IContest contest = getContest();
		if (contest == null)
			return;

		double size = sizeAnim.getValue();

		ITeam[] teams = contest.getOrderedTeams();
		for (int i = teams.length - 1; i >= 0; i--) {
			ITeam team = teams[i];
			IOrganization org = contest.getOrganizationById(team.getOrganizationId());
			if (org == null)
				continue;

			BufferedImage img = map.get(org.getId());
			if (img != null) {
				Animator3D an = anim.get(team.getId());
				if (an == null)
					continue;

				Graphics2D gg = (Graphics2D) g.create();
				if ((selectedTeams != null && selectedTeams.contains(team))) {
					if (selectType == SelectType.FTS || selectType == SelectType.FTS_HIGHLIGHT)
						gg.setColor(ICPCColors.FIRST_TO_SOLVE_COLOR);
					else
						gg.setColor(ICPCColors.SELECTION_COLOR);

					gg.fillRect((int) (an.getXValue() - size / 2) - 4, (int) (an.getYValue() - size / 2) - 4, (int) size + 8,
							(int) size + 8);
				}

				int w = img.getWidth();
				int h = img.getHeight();
				double scale = Math.min(size / w, size / h);
				int nw = (int) Math.round(w * scale);
				int nh = (int) Math.round(h * scale);

				if (an.getZValue() < 252)
					gg.setComposite(AlphaComposite.SrcOver.derive((float) an.getZValue() / 255f));

				gg.drawImage(img, (int) (an.getXValue() - nw / 2), (int) (an.getYValue() - nh / 2), nw, nh, null);
				gg.dispose();
			}
		}
	}
}