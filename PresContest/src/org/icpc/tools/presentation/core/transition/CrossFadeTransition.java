package org.icpc.tools.presentation.core.transition;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;

import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.Transition;

public class CrossFadeTransition extends Transition {
	@Override
	public void paint(Graphics2D g, double x, Presentation p1, Presentation p2) {
		paint(g, p1);

		g.setComposite(AlphaComposite.SrcOver.derive((float) x));

		paint(g, p2);
	}
}