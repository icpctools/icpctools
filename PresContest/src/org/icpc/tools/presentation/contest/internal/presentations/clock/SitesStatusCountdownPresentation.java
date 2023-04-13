package org.icpc.tools.presentation.contest.internal.presentations.clock;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;
import java.awt.image.BufferedImage;
import java.util.function.Predicate;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IStartStatus;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;
import org.icpc.tools.presentation.contest.internal.nls.Messages;

/**
 * Presentation that shows the clock countdown with a row of 3 switches
 * and the state of different sites in a multi-site contest.
 * 
 * The status of the sites is composed by two different areas:
 *    - The logo area (equivalent to 3 rows of switches in the
 * StatusCountdownPresentation): a logo for each site. The size of the
 * logo represents its status: 10% = NO, 65% = UNDECIDED, 100% = YES.
 *    - A "progress bar" that shows the number of ready sites (status YES).
 * 
 * The sites are standard IStartStatus with an special name: the prefix
 * "site_" followed by the id (or ICPC id) of the affiliation/organization
 * in the contest.
 */
public class SitesStatusCountdownPresentation extends StatusCountdownPresentation {

	// Number of sites (= number of start status that represent sites)
	int numSites;

	// Number of ready sites (= number of switches with value 2).
	int numReadySites;

	// Cache of the position and size of the area with all the site logos.
	// Calculated at cacheLogosAreaSize() (called from setSize()).
	private int logosTopPosition;
	private int logosLeftPosition;
	private int logosWidth;
	private int logosHeight;

	// The logos of the sites are squares. The entire area (including
	// margins) depends on the number of sites and the screen resolution.
	int logoAreaSize;

	// Real size of each logo at full size. It is 90% of the logoAreaSize in
	// order to provide a separation between them.
	int logoImageSize;

	// Vertical padding for logos in order to center them in the designated
	// area
	private int logosVerticalPadding;
	// Horizontal padding for rows of logos that are complete.
	private int logosHorizontalPadding;
	// Horizontal padding for the last row of logos
	private int logosLastRowHorizontalPadding;

	// Logos of every site. The key is the switch id ("site_XXX")
	private Map<String, BufferedImage> siteLogo = new HashMap<>();

	// Animators of the logo sizes
	private Map<String, Animator> logoSizeAnim = new HashMap<>();

	// Sizes (relatives to logoImageSize) of the images depending
	// on the site status (NO, UNDECIDED, YES)
	private static final float NO_LOGO_SIZE = 0.1f;
	private static final float UNDECIDED_LOGO_SIZE = 0.65f;
	private static final float YES_LOGO_SIZE = 1.0f;

	// Logo's size animator properties
	private static final Movement LOGO_ANIM = new Movement(1, 1);

	// Ready sites label text
	private static String READY_SITES_LABEL = Messages.numReadySites;

	// Progress bar ready sites properties
	private static final Movement PROGRESS_BAR_ANIM = new Movement(0.05, 0.8);

	// Animator of the progress bar with the ready sites
	private Animator readySitesAnim;


	public SitesStatusCountdownPresentation() {
		// Instruct our base class to only render switches that are not
		// site switches.
		super(((Predicate<IStartStatus>)SitesStatusCountdownPresentation::isSiteSwitch).negate());
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		// Cancel clock vertical offset to ensure the same position
		// even when there is a banner (https://github.com/icpctools/icpctools/issues/770)
		verticalOffset = 0;

		cacheLogosAreaSize();
	}

	@Override
	public void aboutToShow() {
		super.aboutToShow();
		cacheSites();
	}

	@Override
	public void incrementTimeMs(long dt) {
		super.incrementTimeMs(dt);
		updateSites(dt);
	}	

	@Override
	public void paint(Graphics2D g) {
		super.paint(g);

		// Debug: border in the logos area
		//g.drawRect(logosLeftPosition, logosTopPosition, logosWidth, logosHeight);

		if (numSites > 0) {
			drawSiteLogos(g);
			drawReadySitesProgressBar(g);
		}
	}

	/**
	 * Cache position and dimensions of the area with the site status
	 * information.
	 */
	private void cacheLogosAreaSize() {

		// Horizontal margin 1/20
		logosWidth = width - 2*width/20;
		// Area equivalent to three rows of switches
		logosHeight = height * 3/18; 

		// The area starts after a (maybe empty) row of switches
		logosTopPosition = 40 + (int)(0.5*height / 18);
		logosLeftPosition = width / 20;
	}

	private static boolean isSiteSwitch(IStartStatus ss) {
		return ss.getId().startsWith("site_");
	}

	/**
	 * Search the organization in the contest with the given id or
	 * ICPC ID
	 * @param contest
	 * @param id
	 * @return
	 */
	private static IOrganization searchOrganization(IContest contest,
										String id) {

		IOrganization org = contest.getOrganizationById(id);
		if (org != null)
			return org;

		for (IOrganization organization : contest.getOrganizations())
			if (organization.getICPCId().equals(id))
			return organization;

		return null;
	}

	private static float getExpectedLogoSizeRatio(IStartStatus ss) {
		switch(ss.getStatus()) {
			case 0: return NO_LOGO_SIZE;
			case 1: return UNDECIDED_LOGO_SIZE;
			case 2: return YES_LOGO_SIZE;
			default: return 0;
		}
	}

	private void updateSites(long dt) {

		IContest contest = getContest();
		if (contest == null)
			return;
		IStartStatus[] startStatus = getContest().getStartStatuses();
		if (startStatus == null)
			return;

		// First step: check whether the number of sites did not change
		// and if we have information about all of them.
		// In other case, we start from scratch using cacheSites().
			
		int currentSites = 0;
		for (IStartStatus ss : startStatus) {
			if (!isSiteSwitch(ss))
				continue;
			if (!logoSizeAnim.containsKey(ss.getId())) {
				// New site. Reload all
				cacheSites();
				return;
			}
			currentSites++;
		}
		if (currentSites != numSites) {
			// Some sites has gone. Recache to recalculate logo sizes
			cacheSites();
			return;
		}

		// Sites remains the same. Adjust and tick their animators
		// and count ready sites
		numReadySites = 0;
		for (IStartStatus ss : startStatus) {
			if (!isSiteSwitch(ss))
				continue;

			Animator anim = logoSizeAnim.get(ss.getId());
			anim.setTarget(getExpectedLogoSizeRatio(ss));
			anim.incrementTimeMs(dt);
			if (ss.getStatus() == 2)
				numReadySites++;
		}
		// Adjust and tick the ready sites animator
		if (readySitesAnim == null) {
			readySitesAnim = new Animator(0, PROGRESS_BAR_ANIM);
			readySitesAnim.reset(numSites != 0 ? (float)numReadySites / numSites : 0);
		} else {
			readySitesAnim.setTarget((float)numReadySites / numSites);
		}
		readySitesAnim.incrementTimeMs(dt);
	}

	private void cacheSites() {

		IContest contest = getContest();
		if (contest == null)
			return;

		IStartStatus[] startStatus = getContest().getStartStatuses();
		if (startStatus == null)
			return;

		// First step: count the number of sites to calculate the logo sizes
		numSites = 0;
		for (IStartStatus ss : startStatus) {
			if (!isSiteSwitch(ss))
				continue;

			numSites++;
		}
		if (numSites == 0)
			return;
		
		// Next step: find out the size of the logos that ensures that
		// there will be space for all of them.
		logoAreaSize = (int)(Math.sqrt(logosHeight*logosWidth / numSites));
		int logosCapacity = (logosWidth / logoAreaSize) * (logosHeight / logoAreaSize);
		while ((logosCapacity < numSites) && (logoAreaSize > 5)) {
			logoAreaSize--;
			logosCapacity = (logosWidth / logoAreaSize) * (logosHeight / logoAreaSize);			
		}
		logoImageSize = (int)(logoAreaSize * 0.9);

		// Calculate padding
		int logosPerRow = logosWidth / logoAreaSize;
		int numRows = (numSites + logosPerRow - 1) / logosPerRow;
		logosVerticalPadding = (logosHeight - (numRows * logoAreaSize)) / 2;
		logosHorizontalPadding = (logosWidth - (logosPerRow * logoAreaSize)) / 2;
		int logosLastRow = numSites - ((numRows-1) * logosPerRow);
		logosLastRowHorizontalPadding = (logosWidth - (logosLastRow * logoAreaSize)) / 2;
		if (numRows == 1)
			logosHorizontalPadding = logosLastRowHorizontalPadding;

		// Precache logos
		for (IStartStatus ss : startStatus) {
			if (!isSiteSwitch(ss))
				continue;

			String switchId = ss.getId();
			String siteId = switchId.substring(5); // 5=="site_".length()

			if (!siteLogo.containsKey(switchId) || (siteLogo.get(switchId).getWidth() == logoImageSize)) {

				IOrganization org = searchOrganization(contest, siteId);
				if (org == null) {
					Trace.trace(Trace.WARNING,
							"Site start status switch " + switchId + " does not have an organization.");
					continue;
				}
				BufferedImage logo = org.getLogoImage(logoImageSize, logoImageSize, true, true);
				if (logo == null) {
					Trace.trace(Trace.WARNING,
							"Organization " + siteId + " without logo. A mockup image will be used.");
				} else
					siteLogo.put(switchId, logo);
			}
		}

		// Create (or set) animators of the logo sizes and count the number
		// of ready sites
		numReadySites = 0;
		for (IStartStatus ss : startStatus) {
			if (!isSiteSwitch(ss))
				continue;

			String switchId = ss.getId();

			if (!logoSizeAnim.containsKey(switchId)) {
				Animator anim = new Animator(0, LOGO_ANIM);
				anim.reset(getExpectedLogoSizeRatio(ss));
				logoSizeAnim.put(switchId, anim);
			} else
				logoSizeAnim.get(switchId).setTarget(getExpectedLogoSizeRatio(ss));

			if (ss.getStatus() == 2)
				numReadySites++;
		}

		// Create (or set) the progress bar animator
		if (readySitesAnim == null) {
			readySitesAnim = new Animator(0, PROGRESS_BAR_ANIM);
			readySitesAnim.reset((float)numReadySites / numSites);
		} else {
			readySitesAnim.setTarget((float)numReadySites / numSites);
		}

	}

	private void drawSiteLogos(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		IContest contest = getContest();
		if (contest == null)
			return;
		IStartStatus[] startStatus = getContest().getStartStatuses();
		if (startStatus == null)
			return;

		int currentX = logosLeftPosition + logosHorizontalPadding;
		int currentY = logosTopPosition + logosVerticalPadding;
		for (IStartStatus ss : startStatus) {
			if (!isSiteSwitch(ss))
				continue;

			String switchId = ss.getId();

			if (!logoSizeAnim.containsKey(switchId))
				// Unexepected. The animator should have been created
				// in incrementTimeMs. Maybe the organization/switch
				// is brand new. Delay its rendering to the next paint.
				continue;

			int basePosX = currentX + (logoAreaSize - logoImageSize) / 2;
			int basePosY = currentY + (logoAreaSize - logoImageSize) / 2;

			double mult = logoSizeAnim.get(switchId).getValue();
			int size =  (int)(mult * logoImageSize);
			int padding = (logoImageSize - size) / 2; 
			basePosX += padding;
			basePosY += padding;

			BufferedImage img = siteLogo.get(switchId);
			if (img != null) {
				g.drawImage(img, basePosX, basePosY, size, size, null);
			} else {
				g.setColor(Color.RED);
				g.fillRect(basePosX, basePosY, size, size);
			}
			currentX += logoAreaSize;
			if (currentX - logosLeftPosition > logosWidth - logoAreaSize) {
				// Row complete
				currentX = logosLeftPosition + logosHorizontalPadding;
				currentY += logoAreaSize;
				if (currentY - logosTopPosition > logosHeight - logoAreaSize * 2) {
					// Last row
					currentX = logosLeftPosition + logosLastRowHorizontalPadding;
				}
			}
		}		
	}


	private void drawReadySitesProgressBar(Graphics2D g) {

		FontMetrics fm = g.getFontMetrics();

		String leftLabel = READY_SITES_LABEL + ": 0 ";
		String rightLabel = " " + numSites;

		int startY = logosTopPosition + logosHeight + fm.getHeight() / 2;
		int leftLabelWidth = fm.stringWidth(leftLabel);
		int rightLabelWidth = fm.stringWidth(rightLabel);
		int lineWidth = logosWidth - leftLabelWidth - rightLabelWidth;

		g.setColor(Color.WHITE);
		g.drawString(leftLabel, logosLeftPosition, startY + (int) (fm.getAscent() / 2.2f));
		g.drawString(rightLabel, logosLeftPosition + leftLabelWidth + lineWidth, startY + (int) (fm.getAscent() / 2.2f));
		g.drawLine(logosLeftPosition + leftLabelWidth, startY, logosLeftPosition + leftLabelWidth + lineWidth, startY);

		int r = height / 42;
		int cx = logosLeftPosition + leftLabelWidth;
		int cy = startY - r / 2;

		if (readySitesAnim != null)
			cx += (lineWidth - r) * readySitesAnim.getValue();

		g.setColor(Color.GREEN);
		g.fillOval(cx, cy, r, r);
		g.setColor(Color.WHITE);
		g.drawOval(cx, cy, r, r);
	}

}
