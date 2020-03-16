package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.icpc.tools.presentation.core.Presentation;

/**
 * A rough and incomplete Javascript to Java port of https://codepen.io/MillerTime/pen/XgpNwb by
 * Caleb Miller.
 */
public class BetterFireworksPresentation extends Presentation {
	private static final double GRAVITY = 12.0; // 0.9; // Acceleration in px/s
	private static final Color GOLD = new Color(255, 191, 54);
	protected static final Color RED = new Color(255, 0, 67);
	protected static final Color GREEN = new Color(20, 252, 86);
	protected static final Color BLUE = new Color(30, 127, 255);
	protected static final Color PURPLE = new Color(230, 10, 255);
	protected static final Color WHITE = Color.WHITE;
	private static final Color INVISIBLE = new Color(0, 0, 0, 0);
	private static final Color SPARK = new Color(255, 255, 100, 75);

	protected static final Color[] COLORS = new Color[] { RED, GREEN, BLUE, PURPLE, GOLD, WHITE };

	protected static final Class<?>[] SHELLS = new Class<?>[] { CrackleShell.class, CrossetteShell.class,
			FallingLeavesShell.class, FloralShell.class, HorsetailShell.class, WillowShell.class, CrysanthemumShell.class,
			RingShell.class, GhostShell.class, StrobeShell.class, PalmShell.class };

	enum Glitter {
		LIGHT, MEDIUM, HEAVY, THICK, STREAMER, WILLOW
	}

	interface IParticleFactory {
		void create(double angle, double speed);
	}

	abstract class Particle {
		public double x;
		public double y;
		public int lastx;
		public int lasty;
		public double dx;
		public double dy;
		public double airDrag;
		public long life;
		public long fullLife;
		public Color color;
		public boolean visible;
		public Runnable onDeath;

		public boolean move(long dt) {
			double speed = 1; // 60; // 0.2;
			double dtd = dt / 1000.0;
			// double drag = 1 - (1 - airDrag) * speed * (1 - dtd);
			double drag = 1 - (1 - airDrag) * dtd;
			// double gAcc = dtd * GRAVITY;
			// if (life > 2000)
			// System.err.println("! " + this);
			life -= dt;
			if (life <= 0) {
				particles.remove(this);
				if (onDeath != null)
					onDeath.run();
				return false;
			}
			// System.out.println(dt + " " + dtd + " " + drag);

			lastx = (int) x;
			lasty = (int) y;
			x += dx * speed * dtd;
			y += dy * speed * dtd;
			dx *= drag;
			dy *= drag;
			dy += GRAVITY * dtd;

			return true;
		}

		public void paintBackground(Graphics2D g) {
			//
		}

		public void paintForeground(Graphics2D g) {
			//
		}
	}

	class Star extends Particle {
		public double sparkFreq = 8;
		public double sparkSpeed = 0.7;
		public long sparkLife = 320;
		public double sparkLifeVariation = 2;
		public long sparkTimer;
		public Color sparkColor = SPARK;
		public boolean strobe;
		public double strobeFreq;
		public long transitionTime;

		protected Star(double x, double y, Color c, double angle, double speed, long life) {
			init(x, y, c, angle, speed, life);
		}

		private void init(double xx, double yy, Color c, double angle, double speed, long life2) {
			x = xx;
			y = height;
			lastx = (int) xx;
			lasty = height;
			dx = Math.sin(angle) * speed;
			dy = Math.cos(angle) * speed;
			life = life2;
			fullLife = life2;
			airDrag = 0.992;
			color = c;

			sparkFreq = 8;
			sparkSpeed = 0.7;
			sparkLife = 320;
			sparkLifeVariation = 2;
			sparkTimer = 0;
			sparkColor = SPARK;
			strobe = false;
			strobeFreq = 0;
			transitionTime = 0;

			visible = true;
			onDeath = null;
		}

		@Override
		public boolean move(long dt) {
			if (!super.move(dt)) {
				oldStars.add(this);
				return false;
			}

			/*double speed = 0.2;
			double dtd = dt / 1000.0;
			double starDrag = 1 - (1 - airDrag) * speed * (1 - dtd);
			// double starDragHeavy = 1 - (1 - airDragHeavy) * speed;
			// double gAcc = dt / 1000.0 * GRAVITY;
			
			life -= dt;
			if (life <= 0) {
				// stars.splice(i, 1);
				particles.remove(this);
				if (onDeath != null)
					onDeath.run();
			} else {*/
			double burnRate = Math.pow((double) life / fullLife, 0.5);
			double burnRateInverse = 1 - burnRate;

			// star.prevX = star.x;
			// star.prevY = star.y;
			/*x += dx * speed * dtd;
			y += dy * speed * dtd;
			// Apply air drag if star isn't "heavy". The heavy property is used for the shell
			// comets.
			// if (!heavy) {
			dx *= starDrag;
			dy *= starDrag;
			dy += GRAVITY * dtd;*/

			/*if (spinRadius) {
				star.spinAngle += star.spinSpeed * speed;
				star.x += Math.sin(star.spinAngle) * star.spinRadius * speed;
				star.y += Math.cos(star.spinAngle) * star.spinRadius * speed;
			}*/

			if (sparkFreq > 0) {
				sparkTimer -= dt;
				while (sparkTimer < 0) {
					// System.out.println((life * 2.0 / fullLife) + " " + burnRate);
					sparkTimer += sparkFreq * 0.75 + sparkFreq * burnRateInverse * 4;
					Spark s = createOrReuseSpark(x, y, sparkColor, Math.random() * Math.PI * 2,
							Math.random() * sparkSpeed * burnRate * 0.005,
							(int) (sparkLife * 0.8 + Math.random() * sparkLifeVariation * sparkLife));
					particles.add(s);
				}
			}

			// Handle star transitions
			if (life < transitionTime) {
				/*if (secondColor && !colorChanged) {
					colorChanged = true;
					color = secondColor;
					//stars.splice(i, 1);
					Star.active[star.secondColor].push(star);
					if (secondColor == INVISIBLE) {
						sparkFreq = 0;
					}
				}*/

				if (strobe) {
					// Strobes in the following pattern: on:off:off:on:off:off in increments of
					// `strobeFreq` ms.
					visible = Math.floor(life / strobeFreq) % 3 == 0;
				}
				// }
			}
			return true;
		}

		@Override
		public void paintBackground(Graphics2D bg) {
			Composite oldComp = bg.getComposite();
			bg.setComposite(AlphaComposite.SrcOver);
			bg.setColor(color);
			Stroke oldStroke = bg.getStroke();
			bg.setStroke(new BasicStroke(3));
			bg.drawLine((int) x, (int) y, (int) (x - dx / 20), (int) (y - dy / 20));
			bg.setStroke(oldStroke);
			bg.setComposite(oldComp);
		}

		@Override
		public void paintForeground(Graphics2D fg) {
			fg.setColor(color);
			fg.drawLine((int) x, (int) y, (int) (x - dx / 40), (int) (y - dy / 40));
		}
	}

	class Spark extends Particle {
		protected Spark(double x, double y, Color c, double angle, double speed, long life) {
			init(x, y, c, angle, speed, life);
		}

		private void init(double xx, double yy, Color c, double angle, double speed, long life2) {
			airDrag = 0.9;
			x = xx;
			y = yy;
			lastx = (int) xx;
			lasty = (int) y;
			color = c;
			dx = Math.sin(angle) * speed; // * 0.001;
			dy = Math.cos(angle) * speed; // * 0.001;
			life = life2;
			fullLife = life2;

			visible = true;
			onDeath = null;
		}

		/*@Override
		public boolean move(long dt) {
			double speed = 0.2;
			double dtd = dt / 1000.0;
			double sparkDrag = 1 - (1 - airDrag) * speed * (1 - dtd);
			// System.out.println("drag: " + sparkDrag);
			// double gAcc = dtd * GRAVITY;
			life -= dt;
			if (life <= 0) {
				//particles.remove(this);
				return false;
			} else {
				// prevX = x;
				// prevY = y;
				x += dx * speed * dtd;
				y += dy * speed * dtd;
				dx *= sparkDrag;
				dy *= sparkDrag;
				dy += GRAVITY * dtd;
			}
		}*/

		@Override
		public boolean move(long dt) {
			if (!super.move(dt)) {
				oldSparks.add(this);
				return false;
			}
			return true;
		}

		@Override
		public void paintBackground(Graphics2D bg) {
			if (color == null)
				return;

			bg.setColor(color);
			Stroke oldStroke = bg.getStroke();
			bg.setStroke(new BasicStroke(0.75f));
			bg.drawLine((int) x, (int) y, lastx, lasty);
			bg.setStroke(oldStroke);
		}
	}

	class BurstFlash extends Particle {
		public double radius;

		public BurstFlash(double x, double y, double radius) {
			this.x = x;
			this.y = y;
			this.radius = radius;
			particles.add(this);
		}

		@Override
		public boolean move(long dt) {
			// TODO Auto-generated method stub
			return false;
		}
	}

	class CrossetteShell extends Shell {
		public CrossetteShell(double w, double h, double size) {
			super(w, h);
			// shellSize = size;
			spreadSize = 300 + size * 100;
			starLife = (int) (750 + size * 160);
			starLifeVariation = 0.4;
			starDensity = 0.85;
			// pistil: Math.random() < 0.5;
			// pistilColor: makePistilColor(color);
		}
	}

	class HorsetailShell extends Shell {
		public HorsetailShell(double w, double h, double size) {
			super(w, h);
			color = randomColor(null, false, false);
			// shellSize: size,
			spreadSize = 250 + size * 38;
			starDensity = 0.9;
			starLife = (int) (2500 + size * 300);
			glitter = Glitter.MEDIUM;
			glitterColor = Math.random() < 0.5 ? whiteOrGold() : color;
			// Add strobe effect to white horsetails, to make them more interesting
			strobe = (color == WHITE);
		}
	}

	class FallingLeavesShell extends Shell {
		public FallingLeavesShell(double w, double h, double size) {
			super(w, h);
			// shellSize: size,
			color = INVISIBLE;
			spreadSize = 300 + size * 120;
			starDensity = 0.12;
			starLife = (int) (500 + size * 50);
			starLifeVariation = 0.5;
			glitter = Glitter.MEDIUM;
			glitterColor = GOLD;
		}
	}

	class WillowShell extends Shell {
		public WillowShell(double w, double h, double size) {
			super(w, h);
			// shellSize: size,
			spreadSize = 300 + size * 100;
			starDensity = 0.6;
			starLife = (int) (3000 + size * 300);
			glitter = Glitter.WILLOW;
			glitterColor = GOLD;
			color = INVISIBLE;
		}
	}

	class FloralShell extends Shell {
		public FloralShell(double w, double h, double size) {
			super(w, h);
			// shellSize: size,
			spreadSize = 300 + size * 120;
			starDensity = 0.12;
			starLife = (int) (500 + size * 50);
			starLifeVariation = 0.5;
			color = randomColorSimple();
			// color: Math.random() < 0.65 ? 'random' : (Math.random() < 0.15 ? randomColor() :
			// [randomColor(), randomColor({ notSame: true })]),
		}
	}

	class CrackleShell extends Shell {
		public CrackleShell(double w, double h, double size) {
			super(w, h);
			// favor gold
			color = Math.random() < 0.75 ? GOLD : randomColor(null, false, false);
			// shellSize: size,
			spreadSize = 380 + size * 75;
			starDensity = 1;
			starLife = (int) (600 + size * 100);
			starLifeVariation = 0.32;
			glitter = Glitter.LIGHT;
			glitterColor = GOLD;
			// pistil: Math.random() < 0.65,
			// pistilColor: makePistilColor(color)
		}
	}

	class PalmShell extends Shell {
		public PalmShell(double w, double h, double size) {
			super(w, h);
			color = randomColor(null, false, false);
			boolean thick = Math.random() < 0.5;
			// shellSize: size,
			spreadSize = 250 + size * 75;
			starDensity = thick ? 0.15 : 0.4;
			starLife = (int) (1800 + size * 200);
			glitter = thick ? Glitter.THICK : Glitter.HEAVY;
		}
	}

	class RingShell extends Shell {
		public RingShell(double w, double h, double size) {
			super(w, h);
			color = randomColor(null, false, false);
			// pistil = Math.random() < 0.75;
			// shellSize: size,
			spreadSize = 300 + size * 100;
			starLife = (int) (900 + size * 200);
			starCount = (int) (2.2 * Math.PI * 2 * (size + 1));
			// pistil,
			// pistilColor: makePistilColor(color),
			// glitter = !pistil ? Glitter.LIGHT : null;
			glitter = Glitter.LIGHT;
			glitterColor = color == GOLD ? GOLD : WHITE;
			// streamers= Math.random() < 0.3;
		}
	}

	class CrysanthemumShell extends Shell {
		public CrysanthemumShell(double w, double h, double size) {
			super(w, h);
			glitter = ((Math.random() < 0.25) ? Glitter.MEDIUM : null);
			// singleColor = Math.random() < 0.72;
			// TODO color = singleColor ? randomColor(null, false, true) : [randomColor(),
			// randomColor(null, true, false)];
			// pistil = singleColor && Math.random() < 0.42;
			// pistilColor = pistil && makePistilColor(color);
			// secondColor = singleColor && (Math.random() < 0.2 || color == WHITE) ? pistilColor ||
			// randomColor(color, false, true) : null;
			// streamers = !pistil && color != WHITE && Math.random() < 0.42;
			starDensity = 1.2;
			spreadSize = 300 + size * 100;
			starLife = (int) (900 + size * 200);
			// glitter= glitter ? Glitter.LIGHT : null;
			glitterColor = whiteOrGold();
		}
	}

	class GhostShell extends CrysanthemumShell {
		public GhostShell(double w, double h, double size) {
			super(w, h, size);
			// Ghost effect can be fast, so extend star life
			starLife *= 1.5;
			// Ensure we always have a single color other than white
			// ghostColor = randomColor(WHITE, false, false);
			// Always use streamers, and sometimes a pistil
			// streamers = true;
			// pistil = Math.random() < 0.42;
			// pistilColor = pistil && makePistilColor(ghostColor);
			// Ghost effect - transition from invisible to chosen color
			color = INVISIBLE;
			// secondColor = ghostColor;
			// We don't want glitter to be spewed by invisible stars, and we don't currently
			// have a way to transition glitter state. So we'll disable it.
			glitter = null;
		}
	}

	class StrobeShell extends Shell {
		public StrobeShell(double w, double h, double size) {
			super(w, h);
			color = randomColor(null, false, true);
			// shellSize: size,
			spreadSize = 280 + size * 92;
			starLife = (int) (1100 + size * 200);
			starLifeVariation = 0.40;
			starDensity = 1.1;
			glitter = Glitter.LIGHT;
			glitterColor = WHITE;
			strobe = true;
			// strobeColor = Math.random() < 0.5 ? WHITE : null;
			// pistil = Math.random() < 0.5;
			// pistilColor = makePistilColor(color);
		}
	}

	class Shell extends Star {
		public double spreadSize = 400;
		public int starCount = 35; // 75
		public Glitter glitter;
		public Color glitterColor;
		public int starLife;
		public double starLifeVariation;
		public double starDensity;

		public double spinAngle = Math.random() * Math.PI * 2;
		public double spinSpeed = 0.8;
		public double spinRadius = 0;

		public Shell(double w, double h) {
			// super(w, h, Color.RED, Math.PI, Math.pow(h * 200, 0.64), 2000);
			super(w, h, Color.RED, Math.PI, Math.pow(h * 15.0, 0.67), 2000);
			// System.out.println(dx + " " + dy);
			airDrag = 0.98;
			spinRadius = 0.32 + Math.random() * (0.85 - 0.32);
			// double dy = 200; //
			// double launchSpeed = Math.pow(h * 300, 0.64);
			// star = new Star(w, h, Math.PI, launchSpeed, 2000, true);

			if (Math.random() > 0.4) { // && !this.horsetail) {
				// comet.secondColor = INVISIBLE;
				// star.transitionTime = Math.pow(Math.random(), 1.5) * 700 + 500;
			}

			onDeath = new Runnable() {
				@Override
				public void run() {
					burst();
				}
				// (burst);
			};
			// particles.add(star);
		}

		@Override
		public boolean move(long dt) {
			if (!super.move(dt))
				return false;

			// double speed = 0.2 * dt / 1000.0;
			double speed = 160 * dt / 1000.0;
			spinAngle += spinSpeed * speed * 10;
			x += Math.sin(spinAngle) * spinRadius * speed;
			y += Math.cos(spinAngle) * spinRadius * speed;
			return true;
		}

		public void burst() {
			double speed2 = spreadSize / 96;

			if (glitter == Glitter.LIGHT) {
				sparkFreq = 400;
				sparkSpeed = 0.3;
				sparkLife = 300;
				sparkLifeVariation = 2;
			} else if (glitter == Glitter.MEDIUM) {
				sparkFreq = 200;
				sparkSpeed = 0.44;
				sparkLife = 700;
				sparkLifeVariation = 2;
			} else if (glitter == Glitter.HEAVY) {
				sparkFreq = 80;
				sparkSpeed = 0.8;
				sparkLife = 1400;
				sparkLifeVariation = 2;
			} else if (glitter == Glitter.THICK) {
				sparkFreq = 16;
				sparkSpeed = 1.65;
				sparkLife = 1400;
				sparkLifeVariation = 3;
			} else if (glitter == Glitter.STREAMER) {
				sparkFreq = 32;
				sparkSpeed = 1.05;
				sparkLife = 620;
				sparkLifeVariation = 2;
			} else if (glitter == Glitter.WILLOW) {
				sparkFreq = 120;
				sparkSpeed = 0.34;
				sparkLife = 1400;
				sparkLifeVariation = 3.8;
			}

			// Apply quality to spark count
			sparkFreq /= 3;

			// BurstFlash bf = new BurstFlash(star.x, star.y, spreadSize / 4);
			// System.out.println(bf);
			IParticleFactory pf = new IParticleFactory() {
				@Override
				public void create(double angle, double speedMult) {
					Star star = createOrReuseStar(x, y, color, angle, speed2 * speedMult * 15.0,
							life + 1000 + (int) (Math.random() * 200.0));
					// star.speed);
					star.y = y;
					star.lasty = (int) y;
					particles.add(star);

					if (Shell.this.glitter != null) {
						star.sparkFreq = sparkFreq;
						star.sparkSpeed = sparkSpeed;
						star.sparkLife = sparkLife;
						star.sparkLifeVariation = sparkLifeVariation;
						star.sparkColor = Shell.this.glitterColor;
						star.sparkTimer = (int) (Math.random() * star.sparkFreq);
					}
					// System.out.println("new " + angle + " " + speed);

				}
			};
			createBurst(starCount, pf);
		}
	}

	private List<Particle> particles = new ArrayList<>();

	private static Queue<Star> oldStars = new LinkedList<>();
	private static Queue<Spark> oldSparks = new LinkedList<>();

	private Star createOrReuseStar(double x, double y, Color c, double angle, double speed, long life) {
		if (oldStars.isEmpty())
			return new Star(x, y, c, angle, speed, life);

		Star s = oldStars.poll();
		s.init(x, y, c, angle, speed, life);
		return s;
	}

	private Spark createOrReuseSpark(double x, double y, Color c, double angle, double speed, long life) {
		if (oldSparks.isEmpty())
			return new Spark(x, y, c, angle, speed, life);

		Spark s = oldSparks.poll();
		s.init(x, y, c, angle, speed, life);
		return s;
	}

	// current: 3 - 0 to 2.9999
	// want: -0.49 to 2.4999
	protected static Color randomColorSimple() {
		int c = (int) (Math.random() * COLORS.length - 0.5);
		if (c < 0)
			c = 0;
		// System.out.println(COLORS[c]);
		return COLORS[c];
	}

	protected static Color lastColor;

	protected static Color randomColor(Color notColor, boolean notSame, boolean limitWhite) {
		Color color = randomColorSimple();

		// limit the amount of white chosen randomly
		if (limitWhite && color == WHITE && Math.random() < 0.6) {
			color = randomColorSimple();
		}

		if (notSame) {
			while (color == lastColor) {
				color = randomColorSimple();
			}
		} else if (notColor != null) {
			while (color == notColor) {
				color = randomColorSimple();
			}
		}

		lastColor = color;
		// System.out.println(color + " " + lastColor);
		return color;
	}

	protected static Color whiteOrGold() {
		return Math.random() < 0.5 ? GOLD : WHITE;
	}

	protected void createBurst(int count, IParticleFactory particleFactory) {
		createBurst(count, particleFactory, 0, Math.PI * 2.0);
	}

	protected void createBurst(int count, IParticleFactory particleFactory, double startAngle, double arcLength) {
		// double startAngle=0, arcLength=PI_2

		// Assuming sphere with surface area of `count`, calculate various
		// properties of said sphere (unit is stars).
		// Radius
		double R = 0.5 * Math.sqrt(count / Math.PI);
		// Circumference
		double C = 2 * R * Math.PI;
		// Half Circumference
		double C_HALF = C / 2;

		// System.out.println("chalf: " + C_HALF + " " + R + " " + count);
		// Make a series of rings, sizing them as if they were spaced evenly
		// along the curved surface of a sphere.
		for (int i = 0; i <= C_HALF; i++) {
			double ringAngle = i / C_HALF * Math.PI / 2;
			double ringSize = Math.cos(ringAngle);
			// double ringSize = Math.cos(ringAngle) * 50;
			double partsPerFullRing = C * ringSize;
			double partsPerArc = partsPerFullRing * (arcLength / (Math.PI * 2));

			double angleInc = Math.PI * 2 / partsPerFullRing;
			double angleOffset = Math.random() * angleInc + startAngle;
			// Each particle needs a bit of randomness to improve appearance.
			double maxRandomAngleOffset = angleInc * 0.33;
			// System.out.println("partsPerArc: " + partsPerArc + " " + ringSize);

			for (int j = 0; j < partsPerArc; j++) {
				double randomAngleOffset = Math.random() * maxRandomAngleOffset;
				double angle = angleInc * j + angleOffset + randomAngleOffset;
				particleFactory.create(angle, ringSize);
			}
		}
	}

	// Helper used to semi-randomly spread particles over an arc
	// Values are flexible - `start` and `arcLength` can be negative, and `randomness` is
	// simply a
	// multiplier for random addition.
	protected void createParticleArc(double start, double arcLength, int count, double randomness, IParticleFactory pf) {
		double angleDelta = arcLength / count;
		// Sometimes there is an extra particle at the end, too close to the start. Subtracting
		// half
		// the angleDelta ensures that is skipped.
		// Would be nice to fix this a better way.
		double end = start + arcLength - (angleDelta * 0.5);

		if (end > start) {
			// Optimization: `angle=angle+angleDelta` vs. angle+=angleDelta
			// V8 deoptimises with let compound assignment
			for (double angle = start; angle < end; angle = angle + angleDelta) {
				pf.create(angle + Math.random() * angleDelta * randomness, 0);
			}
		} else {
			for (double angle = start; angle > end; angle = angle + angleDelta) {
				pf.create(angle + Math.random() * angleDelta * randomness, 0);
			}
		}
	}

	@Override
	public void init() {
		//
	}

	private long lastStar = 0;
	private int f = 0;
	private BufferedImage bgImg;
	private long lastDt = 0;

	@Override
	public void incrementTimeMs(final long dt) {
		Particle[] list = particles.toArray(new Particle[0]);
		for (Particle p : list) {
			if (!p.move(dt))
				particles.remove(p);
		}

		if (lastStar + 300 < getTimeMs()) {
			try {
				Class<?> c = SHELLS[f++];
				if (f >= SHELLS.length)
					f = 0;
				Constructor<?>[] con = c.getConstructors();
				Object o = con[0].newInstance(
						new Object[] { this, 50 + Math.random() * (width - 100), 100 + Math.random() * (height / 2), 1 });
				Shell s = (Shell) o;
				particles.add(s);
			} catch (Exception e) {
				e.printStackTrace();
			}
			/*Shell s = new HorsetailShell(50 + Math.random() * (width - 100), 50 + Math.random() * (height / 2), 5);
			particles.add(s);
			s = new CrossetteShell(50 + Math.random() * (width - 100), 50 + Math.random() * (height / 2), 5);
			particles.add(s);*/
			// Trace.trace(Trace.INFO, "Launched: " + s);
			lastStar = getTimeMs();
			// launch one
		}
		lastDt = dt;
	}

	@Override
	public void paint(Graphics2D g) {
		if (bgImg == null) {
			if (width == 0)
				return;
			// ColorModel cm = g.getDeviceConfiguration().getColorModel();
			bgImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
		}
		Graphics2D bg = bgImg.createGraphics();
		int a = (int) (lastDt * 255 * 3 / 1000);
		if (a < 0)
			a = 0;
		if (a > 255)
			a = 255;
		bg.setColor(new Color(0, 0, 0, a));
		bg.fillRect(0, 0, width, height);
		for (Particle p : particles) {
			p.paintBackground(bg);
		}

		bg.dispose();
		g.drawImage(bgImg, 0, 0, null);

		for (Particle p : particles) {
			p.paintForeground(g);
		}
	}

	// Various star effects.
	// These are designed to be attached to a star's `onDeath` event.

	// Crossette breaks star into four same-color pieces which branch in a cross-like shape.
	protected void crossetteEffect(Star star) {
		double startAngle = Math.random() * Math.PI / 2.0;
		createParticleArc(startAngle, Math.PI * 2, 4, 0.5, (angle, speed) -> {
			particles.add(createOrReuseStar(star.x, star.y, star.color, angle, Math.random() * 0.6 + 0.75, 600));
		});
	}

	// Flower is like a mini shell
	protected void floralEffect(Star star) {
		int count = 12 + 6 * 3;
		createBurst(count, (angle, speedMult) -> {
			particles.add(createOrReuseStar(star.x, star.y, star.color, angle, speedMult * 2.4,
					(int) (1000 + Math.random() * 300)));
			// , star.speedX, star.speedY);
		});
		// Queue burst flash render
		particles.add(new BurstFlash(star.x, star.y, 46));
	}

	// Floral burst with willow stars
	protected void fallingLeavesEffect(Star star) {
		createBurst(7, (angle, speedMult) -> {
			Star newStar = createOrReuseStar(star.x, star.y, INVISIBLE, angle, speedMult * 2.4,
					(int) (2400 + Math.random() * 600));
			// , star.speedX, star.speedY);

			// newStar.sparkColor = GOLD;
			newStar.sparkFreq = 144 / 3;
			newStar.sparkSpeed = 0.28;
			newStar.sparkLife = 750;
			newStar.sparkLifeVariation = 3.2;
		});
		// Queue burst flash render
		particles.add(new BurstFlash(star.x, star.y, 46));
	}

	// Crackle pops into a small cloud of golden sparks.
	protected void crackleEffect(Star star) {
		int count = 32;
		createParticleArc(0, Math.PI * 2, count, 1.8, (angle, speed) -> {
			particles.add(createOrReuseSpark(star.x, star.y, GOLD, angle,
					// apply near cubic falloff to speed (places more particles towards outside)
					Math.pow(Math.random(), 0.45) * 2.4, (int) (300 + Math.random() * 200)));
		});
	}

	protected Color makePistilColor(Color shellColor) {
		return (shellColor == WHITE || shellColor == GOLD) ? randomColor(shellColor, false, false) : whiteOrGold();
	}
}