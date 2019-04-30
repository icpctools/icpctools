package org.icpc.tools.presentation.core.transition;

import java.awt.Graphics2D;

import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.Transition;

public class NoneTransition extends Transition {
	@Override
	public void paint(Graphics2D g, double x, Presentation p1, Presentation p2) {
		if (x < 0.5)
			paint(g, p1);
		else
			paint(g, p2);
	}
}