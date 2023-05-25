package org.icpc.tools.contest.util.floor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.FloorMap.Path;
import org.icpc.tools.contest.model.ITeam;

public class FloorGenerator {
	protected static void createAdjacentTeam(FloorMap floor, int teamNumber, int newId, double dx, double dy, int rot) {
		ITeam t = floor.getTeam(teamNumber);
		floor.createTeam(newId, t.getX() + dx, t.getY() + dy, rot);
	}

	protected static void createAdjacentTeam(FloorMap floor, int teamNumber, int newId, double dx, double dy) {
		ITeam t = floor.getTeam(teamNumber);
		floor.createTeam(newId, t.getX() + dx, t.getY() + dy, t.getRotation());
	}

	protected static void show(final FloorMap floor, final int teamNumber, final boolean showAisles,
			final Path... paths) {
		final Frame frame = new Frame("Floor Map") {
			private static final long serialVersionUID = 1L;

			@Override
			public void paint(Graphics g) {
				Graphics2D gg = (Graphics2D) g;
				gg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

				Dimension d = getSize();
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, d.width, d.height);
				g.setColor(Color.BLACK);
				Rectangle r = new Rectangle(0, 0, d.width, d.height);
				floor.drawFloor((Graphics2D) g, r, teamNumber + "", showAisles, paths);
			}
		};
		frame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				frame.repaint();
			}
		});
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.setBounds(0, 0, 1024, 768);
		frame.setBackground(Color.WHITE);
		frame.setVisible(true);
	}
}