// Java 2D N-Body Simulation Version 3.6 by Jason Kim

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JPanel;

public class NBody2DPanel extends JPanel {

	// debug
	private boolean debug = false;

	// screen
	private final int screenWidth, screenHeight;
	private double screenScale = 1.0; // m per pixel
	private final double targetFPS = 60.0;
	private final double targetTime = 1000.0 / targetFPS; // ms
	private long totalTime;
	private long totalFrames;
	private double currentFPS;
	private boolean text = true;
	private boolean help = false;
	// barycenter
	private boolean barycenter = false;
	private Rectangle baryBounds; // clickable bounds of the barycenter;

	// camera
	private double cameraX = 0.0;
	private double cameraY = 0.0;
	private Body2D selected = null;

	// inputs
	private ArrayList<Integer> holdableKeys = new ArrayList<>();
	private ArrayList<Integer> heldKeys = new ArrayList<>();
	private boolean alt = false; // toggles alt mode
	private Point click = null; // location of the last click

	// simulation
	private ArrayList<ArrayList<Body2D>> frames = new ArrayList<ArrayList<Body2D>>();
	private int frame;
	private final int max_frames = 1000; // to prevent memory overuse
	private boolean paused = false;
	private boolean collisions = true;
	private boolean tidalForces = true;
	private double minMass = Math.pow(10, 28); // kg

	// physics
	private final double G = 6.6743 * Math.pow(10, -11); // m^3 kg^-1 s^-2
	private int physicsMode;
	// multithreading
	private int n = Runtime.getRuntime().availableProcessors() - 1;
	private int num, rem;
	private ArrayList<Thread> threads;
	// precise
	private final double stepSize = 60.0; // seconds
	private final double timeStep = stepSize / targetFPS; // s
	private int timeMult = 1;
	// fast
	private double timeScale = stepSize / targetFPS; // s

	// bodies
	private ArrayList<Body2D> bodies = new ArrayList<Body2D>();
	private int trailLen = 0; // -1 = infinite trail
	private boolean relative = false;
	private int scenario = 2;
	// body locator
	ArrayList<Body2D> sortedBodies; // sorted bodies based on mass
	Rectangle[] bodyBounds; // bounds for clickable areas to select bodies
	
	// nothing
	private int nothing = 0;
	private int nothing_limit = 10;
	// private boolean nothing_mode = false;

	public NBody2DPanel(double screenWidth, double screenHeight) {
		// screen
		this.screenWidth = (int) screenWidth;
		this.screenHeight = (int) screenHeight;

		// keyboard inputs
		holdableKeys.add(KeyEvent.VK_W);
		holdableKeys.add(KeyEvent.VK_A);
		holdableKeys.add(KeyEvent.VK_S);
		holdableKeys.add(KeyEvent.VK_D);
		holdableKeys.add(KeyEvent.VK_SHIFT);
		holdableKeys.add(KeyEvent.VK_CONTROL);
		KeyHandler keyHandler = new KeyHandler();
		addKeyListener(keyHandler);
		// mouse inputs
		addMouseListener(new MouseHandler());
		addMouseWheelListener(new MouseWheelHandler());

		// idk what this does
		setFocusable(true);

		// load scenario
		this.reset();
		scaleTime(Math.pow(2, 8));
		paused = !paused;
	}

	public void reset() {
		// simulation
		paused = true;
		timeMult = 1;
		timeScale = stepSize / targetFPS;
		frames.clear();
		frame = -1;
		// camera
		cameraX = 0.0;
		cameraY = 0.0;
		selected = null;
		// trail
		trailLen = 0;
		bodies.clear();
		// nothing
		nothing_limit = 10;

		// debug scenario
		if (scenario == 0 && debug == true) {
			screenScale = 5000000;
			physicsMode = 2;
			bodies.add(new Body2D(1.989 * Math.pow(10, 30), 696340000.0, 0.0, 0.0, 0.0, 0.0));
			bodies.add(new Body2D(5.972 * Math.pow(10, 24), 10378140.0, screenScale * 500, 0, 0.0, 160000.0));
		}

		// lone planet
		if (scenario == 1) {
			screenScale = 25000.0;
			physicsMode = 0;
			minMass = Math.pow(10, 22);
			bodies.add(new Body2D(Color.WHITE, 5.97 * Math.pow(10, 24), 12756000.0 / 2, 0.0, 0.0, 0.0, 0.0));
		}

		// two-body system
		else if (scenario == 2) {
			screenScale = 10000000;
			physicsMode = 1;
			minMass = Math.pow(10, 28);
			bodies.add(new Body2D(1.989 * Math.pow(10, 30), 696340000.0, screenScale * -500, 0, 0.0, -50000.0));
			bodies.add(new Body2D(1.989 * Math.pow(10, 30), 696340000.0, screenScale * 500, 0, 0.0, 50000.0));
		}

		// three-bodies
		else if (scenario == 3) {
			screenScale = 50000000;
			physicsMode = 1;
			minMass = Math.pow(10, 28);
			for (int i = 0; i < 3; i++) {
				double sx = (Math.random() - 0.5) * screenWidth * screenScale;
				double sy = (Math.random() - 0.5) * screenHeight * screenScale;
				double vx = (Math.random() - 0.5) * screenHeight * 100;
				double vy = (Math.random() - 0.5) * screenHeight * 100;
				bodies.add(new Body2D(1.989 * Math.pow(10, 30), 696340000.0, sx, sy, vx, vy));
			}
		}

		// generates sun-sized objects
		else if (scenario == 4) {
			screenScale = 250000000;
			physicsMode = 2;
			minMass = Math.pow(10, 28);
			for (int i = 0; i < 500; i++) {
				double sx = (Math.random() - 0.5) * screenWidth * screenScale;
				double sy = (Math.random() - 0.5) * screenHeight * screenScale;
				bodies.add(new Body2D(1.989 * Math.pow(10, 30), 696340000.0, sx, sy, 0.0, 0.0));
			}
		}

		// Earth and Moon(s)
		else if (scenario == 5) {
			screenScale = 125000;
			physicsMode = 2;
			minMass = Math.pow(10, 19.5);
			double m = 5.972 * Math.pow(10, 24);
			bodies.add(new Body2D(m, 6378140.0, 0.0, 0.0, 0.0, 0.0));
			double r = 10000000.0;
			double v = Math.sqrt(G * m / r);
			bodies.add(new Body2D(7.342 * Math.pow(10, 22), 1737400.0, r, 0.0, 0.0, v));
			r += 20000000.0;
			v = Math.sqrt(G * m / r);
			bodies.add(new Body2D(7.342 * Math.pow(10, 22), 1737400.0, 0.0, r, v * -1, 0.0));
			r += 20000000.0;
			v = Math.sqrt(G * m / r);
			bodies.add(new Body2D(7.342 * Math.pow(10, 22), 1737400.0, r * -1, 0.0, 0.0, v * -1));
			r += 20000000.0;
			v = Math.sqrt(G * m / r);
			bodies.add(new Body2D(7.342 * Math.pow(10, 22), 1737400.0, 0.0, r * -1, v, 0.0));
		}

		// primordial system consisting of Earth and Moon sized objects
		else if (scenario == 6) {
			minMass = Math.pow(10, 21);
			screenScale = 10000000;
			physicsMode = 2;
			bodies.add(new Body2D(1.989 * Math.pow(10, 30), 696340000.0, 0.0, 0.0, 0.0, 0.0));
			double buffer = bodies.get(0).getRadius() * 3; // buffer zone around sun based on its radius
			for (int i = 0; i < 1000; i++) {
				// randomly mass and radius
				double mass = 0;
				double radius = 0;
				if (Math.random() > 0.5) {
					// earth
					mass = 5.972 * Math.pow(10, 24);
					radius = 6378140.0;
				} else {
					// moon
					mass = 7.348 * Math.pow(10, 22);
					radius = 1737400.0;
				}
				// random position and orbit
				double t = Math.random() * 2 * Math.PI;
				double r = (Math.random() * (screenHeight / 2 * screenScale - buffer)) + buffer;
				double sx = r * Math.cos(t);
				double sy = r * Math.sin(t);
				double v = Math.sqrt(G * bodies.get(0).getMass() / r);
				double vx = v * Math.cos(t + Math.PI / 2);
				double vy = v * Math.sin(t + Math.PI / 2);
				// new body
				bodies.add(new Body2D(mass, radius, sx, sy, vx, vy));
			}
		}

		// Jupiter's Trojans
		else if (scenario == 7) {
			screenScale = 100000000;
			physicsMode = 2;
			double m = 1.989 * Math.pow(10, 30);
			bodies.add(new Body2D(m, 696340000.0, 0.0, 0.0, 0.0, 0.0));
			for (int i = 0; i < 1000; i++) {
				double mass = 6.687 * Math.pow(10, 15); // Eros mass	
				double radius = 1000000.0;
				double t = Math.random() * 2 * Math.PI;
				double r = (screenHeight / 2 * screenScale) + (Math.random() - 0.5) * (100 * screenScale);
				double sx = r * Math.cos(t);
				double sy = r * Math.sin(t);
				double v = Math.sqrt(G * m / r);
				double vx = v * Math.cos(t + Math.PI / 2);
				double vy = v * Math.sin(t + Math.PI / 2);
				bodies.add(new Body2D(mass, radius, sx, sy, vx, vy));
			}
			// Jupiter
			double mass = 1898 * Math.pow(10, 24);
			double radius = 142984000.0 / 2;
			double t = Math.random() * 2 * Math.PI;
			double r = (screenHeight / 2 * screenScale);
			double sx = r * Math.cos(t);
			double sy = r * Math.sin(t);
			double v = Math.sqrt(G * m / r);
			double vx = v * Math.cos(t + Math.PI / 2);
			double vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.WHITE, mass, radius, sx, sy, vx, vy));
		}

		// really inaccurate solar system
		else if (scenario == 8) {
			screenScale = 1250000000.0;
			physicsMode = 2;

			// Sun
			double m = 1.989 * Math.pow(10, 30);
			bodies.add(new Body2D(m, 696340000.0, 0.0, 0.0, 0.0, 0.0));

			// Mercury
			double mass = 0.330 * Math.pow(10, 24);
			double radius = 4879000.0 / 2;
			double t = Math.random() * 2 * Math.PI;
			double r = 57.9 * Math.pow(10, 9);
			double sx = r * Math.cos(t);
			double sy = r * Math.sin(t);
			double v = Math.sqrt(G * m / r);
			double vx = v * Math.cos(t + Math.PI / 2);
			double vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.GRAY, mass, radius, sx, sy, vx, vy));

			// Venus
			mass = 4.87 * Math.pow(10, 24);
			radius = 12104000.0 / 2;
			t = Math.random() * 2 * Math.PI;
			r = 108.2 * Math.pow(10, 9);
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * m / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.YELLOW, mass, radius, sx, sy, vx, vy));

			// Earth
			mass = 5.97 * Math.pow(10, 24);
			radius = 12756000.0 / 2;
			t = Math.random() * 2 * Math.PI;
			r = 149.6 * Math.pow(10, 9);
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * m / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.BLUE, mass, radius, sx, sy, vx, vy));

			// Moon
			double earthM = mass;
			double earthSX = sx;
			double earthSY = sy;
			double earthVX = vx;
			double earthVY = vy;
			mass = 7.342 * Math.pow(10, 22);
			radius = 1737400.0;
			t = Math.random() * 2 * Math.PI;
			r = 384399000.0;
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * earthM / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.GRAY, mass, radius, earthSX + sx, earthSY + sy, earthVX + vx, earthVY + vy));

			// Mars
			mass = 0.642 * Math.pow(10, 24);
			radius = 6792000.0 / 2;
			t = Math.random() * 2 * Math.PI;
			r = 228.0 * Math.pow(10, 9);
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * m / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.RED, mass, radius, sx, sy, vx, vy));
			double marsM = mass;
			double marsSX = sx;
			double marsSY = sy;
			double marsVX = vx;
			double marsVY = vy;

			// Deimos
			mass = 1.0659 * Math.pow(10, 16);
			radius = 11266.7;
			t = Math.random() * 2 * Math.PI;
			r = 9376000.0;
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * marsM / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(mass, radius, marsSX + sx, marsSY + sy, marsVX + vx, marsVY + vy));

			// Phobos
			mass = 1.4762 * Math.pow(10, 15);
			radius = 6200.0;
			t = Math.random() * 2 * Math.PI;
			r = 23463200.0;
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * marsM / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(mass, radius, marsSX + sx, marsSY + sy, marsVX + vx, marsVY + vy));

			// Asteroid Belt
			for (int i = 0; i < 125; i++) {
				mass = 6.687 * Math.pow(10, 15); // Eros mass	
				radius = 1000000.0;
				t = Math.random() * 2 * Math.PI;
				r = 329 * Math.pow(10, 9) + (Math.random() * (478.7 - 329) * 1000000000.0);
				sx = r * Math.cos(t);
				sy = r * Math.sin(t);
				v = Math.sqrt(G * m / r);
				vx = v * Math.cos(t + Math.PI / 2);
				vy = v * Math.sin(t + Math.PI / 2);
				bodies.add(new Body2D(mass, radius, sx, sy, vx, vy));
			}

			// Jupiter
			mass = 1898 * Math.pow(10, 24);
			radius = 142984000.0 / 2;
			t = Math.random() * 2 * Math.PI;
			r = 778.5 * Math.pow(10, 9);
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * m / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.WHITE, mass, radius, sx, sy, vx, vy));
			double jupiterM = mass;
			double jupiterSX = sx;
			double jupiterSY = sy;
			double jupiterVX = vx;
			double jupiterVY = vy;

			// Io
			mass = 8.931938 * Math.pow(10, 22);
			radius = 1821600.0;
			t = Math.random() * 2 * Math.PI;
			r = 421700000.0;
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * jupiterM / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.YELLOW, mass, radius, jupiterSX + sx, jupiterSY + sy, jupiterVX + vx, jupiterVY + vy));

			// Europa
			mass = 4.799844 * Math.pow(10, 22);
			radius = 1560800.0;
			t = Math.random() * 2 * Math.PI;
			r = 670900000.0;
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * jupiterM / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.WHITE, mass, radius, jupiterSX + sx, jupiterSY + sy, jupiterVX + vx, jupiterVY + vy));

			// Ganymede
			mass = 1.4819 * Math.pow(10, 23);
			radius = 2634100.0;
			t = Math.random() * 2 * Math.PI;
			r = 1070400000.0;
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * jupiterM / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.WHITE, mass, radius, jupiterSX + sx, jupiterSY + sy, jupiterVX + vx, jupiterVY + vy));

			// Callisto
			mass = 1.075938 * Math.pow(10, 23);
			radius = 2410300.0;
			t = Math.random() * 2 * Math.PI;
			r = 1882700000.0;
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * jupiterM / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.GRAY, mass, radius, jupiterSX + sx, jupiterSY + sy, jupiterVX + vx, jupiterVY + vy));

			// Saturn
			mass = 568 * Math.pow(10, 24);
			radius = 120536000.0 / 2;
			t = Math.random() * 2 * Math.PI;
			r = 1432.0 * Math.pow(10, 9);
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * m / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.WHITE, mass, radius, sx, sy, vx, vy));
			double saturnM = mass;
			double saturnSX = sx;
			double saturnSY = sy;
			double saturnVX = vx;
			double saturnVY = vy;

			// Rings
			for (int i = 0; i < 125; i++) {
				mass = Math.pow(10, 10);
				radius = 1000.0;
				t = Math.random() * 2 * Math.PI;
				r = 75000000.0 * 2 + Math.random() * (137000000.0 - 75000000.0);
				sx = r * Math.cos(t);
				sy = r * Math.sin(t);
				v = Math.sqrt(G * saturnM / r);
				vx = v * Math.cos(t + Math.PI / 2);
				vy = v * Math.sin(t + Math.PI / 2);
				bodies.add(new Body2D(mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));
			}

			// Mimas
			mass = 37493.0 * Math.pow(10, 15);
			radius = 396400.0 / 2;
			t = Math.random() * 2 * Math.PI;
			r = 185404000.0;
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * saturnM / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.GRAY, mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));

			// Enceladus
			mass = 108022.0 * Math.pow(10, 15);
			radius = 504200.0 / 2;
			t = Math.random() * 2 * Math.PI;
			r = 237950000.0;
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * saturnM / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.WHITE, mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));

			// Tethys
			mass = 617449.0 * Math.pow(10, 15);
			radius = 1062200.0 / 2;
			t = Math.random() * 2 * Math.PI;
			r = 294619000.0;
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * saturnM / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.WHITE, mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));

			// Dione
			mass = 1095452 * Math.pow(10, 15);
			radius = 1122800.0 / 2;
			t = Math.random() * 2 * Math.PI;
			r = 377396000.0;
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * saturnM / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.WHITE, mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));

			// Rhea
			mass = 2306518 * Math.pow(10, 15);
			radius = 1527600.0 / 2;
			t = Math.random() * 2 * Math.PI;
			r = 527108000.0;
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * saturnM / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.WHITE, mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));

			// Titan
			mass = 1.3452 * Math.pow(10, 23);
			radius = 2574730.0;
			t = Math.random() * 2 * Math.PI;
			r = 1221870000.0;
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * saturnM / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.YELLOW, mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));

			// Iapetus
			mass = 1805635 * Math.pow(10, 15);
			radius = 1468600.0 / 2;
			t = Math.random() * 2 * Math.PI;
			r = 3560820000.0;
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * saturnM / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.WHITE, mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));

			// Uranus
			mass = 86.8 * Math.pow(10, 24);
			radius = 51118000.0 / 2;
			t = Math.random() * 2 * Math.PI;
			r = 2867.0 * Math.pow(10, 9);
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * m / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.BLUE, mass, radius, sx, sy, vx, vy));

			// Neptune
			mass = 102 * Math.pow(10, 24);
			radius = 49528000.0 / 2;
			t = Math.random() * 2 * Math.PI;
			r = 4515.0 * Math.pow(10, 9);
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * m / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.BLUE, mass, radius, sx, sy, vx, vy));
			double neptuneM = mass;
			double neptuneSX = sx;
			double neptuneSY = sy;
			double neptuneVX = vx;
			double neptuneVY = vy;

			// Triton
			mass = 2.1390 * Math.pow(10, 22);
			radius = 1353400.0;
			t = Math.random() * 2 * Math.PI;
			r = 354759000.0;
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * neptuneM / r);
			vx = v * Math.cos(t - Math.PI / 2);
			vy = v * Math.sin(t - Math.PI / 2);
			bodies.add(new Body2D(Color.WHITE, mass, radius, neptuneSX + sx, neptuneSY + sy, neptuneVX + vx, neptuneVY + vy));

			// Pluto
			mass = 0.0130 * Math.pow(10, 24);
			radius = 2376000.0 / 2;
			t = Math.random() * 2 * Math.PI;
			r = 5906.4 * Math.pow(10, 9);
			sx = r * Math.cos(t);
			sy = r * Math.sin(t);
			v = Math.sqrt(G * m / r);
			vx = v * Math.cos(t + Math.PI / 2);
			vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body2D(Color.WHITE, mass, radius, sx, sy, vx, vy));
		}

		// tidal forces demonstration
		else if (scenario == 9) {
			minMass = Math.pow(10, 28);
			screenScale = 125000000;
			physicsMode = 2;

			// Sagittarius A
			double holeMass = 8.26 * Math.pow(10, 36);
			double holeRadius = 12000000000.0;
			bodies.add(new Body2D(Color.BLACK, holeMass, holeRadius, 0.0, 0.0, 0.0, 0.0));

			// Sun(s)
			double mass = 1.989 * Math.pow(10, 30);
			double radius = 696340000.0;
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 1; j++) {
					double t = Math.random() * 2 * Math.PI;
					double r = holeRadius * 2 * (i + 1);
					double sx = r * Math.cos(t);
					double sy = r * Math.sin(t);
					double v = Math.sqrt(G * holeMass / r);
					double vx = v * Math.cos(t + Math.PI / 2);
					double vy = v * Math.sin(t + Math.PI / 2);
					bodies.add(new Body2D(Color.WHITE, mass, radius, sx, sy, vx, vy));
				}
			}
			/*
			double sx = 0.0;
			double sy = holeRadius * 12;
			double vx = Math.pow(10, 7.5) * -1;
			double vy = 0.0;
			bodies.add(new Body2D(Color.WHITE, mass, radius, sx, sy, vx, vy));
			*/
		}

		// first frame
		addFrame();
	}

	// converts actual coords to screen coords
	public int convert(double dim, double shift) {
		return (int) ((dim - shift) / screenScale);
	}

	public void step(double deltaTime) {
		// calculating motion
		for (Body2D body : bodies) {
			for (Body2D otherBody : bodies) {
				if (body != otherBody) {
					body.move(otherBody, deltaTime);
				}
			}
		}
		// actually moving
		for (Body2D body : bodies) {
			body.actuallyMove(deltaTime);
		}
		// split check
		splitCheck();
		// collision check
		colCheck();
	}

	// collision detection
	public void colCheck() {
		// checking if collisions are turned on
		if (collisions == true) {
			// actual collision stuff
			boolean pass = false;
			while (pass == false) {
				pass = true;
				for (int i = 0; i < bodies.size(); i++) {
					Body2D body = bodies.get(i);
					for (int j = i + 1; j < bodies.size(); j++) {
						Body2D otherBody = bodies.get(j);
						Body2D newBody = body.collision(otherBody);
						if (newBody != null) {
							if (selected == body || selected == otherBody)
								selected = newBody;
							// attaching old trails to new trail
							body.getTrail().attachTo(newBody.getTrail());
							otherBody.getTrail().attachTo(newBody.getTrail());
							newBody.getTrail().attachFrom(body.getTrail());
							newBody.getTrail().attachFrom(otherBody.getTrail());
							// removing old bodies
							bodies.remove(body);
							bodies.remove(otherBody);
							if (j < i)
								i--;
							bodies.add(newBody);
							pass = false;
							break;
						}
					}
				}
			}
		}
	}

	// splitting bodies
	public boolean splitBody(Body2D body) {
		if ((body.getMass() / 4) > minMass) { // lag prevention
			double v = (4 / 3.0) * Math.PI * Math.pow(body.getRadius(), 3); // volume
			double nr = Math.pow((3 / 4.0) * (v / 4) / Math.PI, 1 / 3.0); // radius
			double nm = body.getMass() / 4; // mass
			// original body's location
			double sx = body.getSX();
			double sy = body.getSY();
			// random rotation
			double rr = Math.random() * Math.PI / 2; // in radians
			// splits into four bodies
			for (int i = -1; i <= 1; i += 2) {
				for (int j = -1; j <= 1; j += 2) {
					// 2x2 square shape
					double nx = sx + (body.getRadius() - nr / 2) * i; // new x coordinate
					double ny = sy + (body.getRadius() - nr / 2) * j; // new y coordinate
					// convert to polar and rotate randomly
					double dx = nx - sx; // difference between new split body and original body
					double dy = ny - sy;
					double dr = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2)); // magnitude of difference vector
					double t = Math.acos(dx / dr); // theta
					if (dy < 0)
						t *= -1;
					t += rr; // random rotation
					// converting back to cartesian coordinates
					nx = dr * Math.cos(t) + sx;
					ny = dr * Math.sin(t) + sy;
					bodies.add(new Body2D(body.getColor(), nm, nr, nx, ny, body.getVX(), body.getVY()));
				}
			}
			if (selected == body)
				selected = bodies.get(bodies.size() - 1 - (int) (Math.random() * 4));
			bodies.remove(body);
			// paused = true; // used for debugging
			return true; // body was split
		}
		return false; // body was not split
	}

	// Roche limit detection
	public void splitCheck() {
		if (tidalForces == true) {
			// calculations
			for (int i = 0; i < bodies.size(); i++) {
				Body2D body = bodies.get(i);
				for (int j = i + 1; j < bodies.size(); j++) {
					Body2D otherBody = bodies.get(j);
					if (i != j) {
						// distance
						double x = otherBody.getSX() - body.getSX();
						double y = otherBody.getSY() - body.getSY();
						double z = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
						// calculating Roche limits
						/*
						double d1 = body.getRadius() * Math.pow(2 * (otherBody.getMass() / body.getMass()), 1 / 3.0);
						double d2 = otherBody.getRadius() * Math.pow(2 * (body.getMass() / otherBody.getMass()), 1 / 3.0);
						*/
						// more accurate roche limit calculations
						double c = 2.44;
						double d1 = body.getRadius() * c * Math.pow(otherBody.getMass() / body.getMass(), 1 / 3.0);
						double d2 = otherBody.getRadius() * c * Math.pow(body.getMass() / otherBody.getMass(), 1 / 3.0);
						// checking
						if (z < d1) {
							body.toSplit = true;
						}
						if (z < d2) {
							otherBody.toSplit = true;
						}
					}
				}
			}
			// actually splitting
			int n = bodies.size();
			for (int i = 0; i < n; i++) {
				Body2D body = bodies.get(i);
				if (body.toSplit) {
					if (splitBody(body))
						i--;
				}
			}
		}
	}

	/*
	public ArrayList<Body2D> copyBodies() {
		// copying bodies
		ArrayList<Body2D> bodiesCopy = new ArrayList<Body2D>();
		for (Body2D body : bodies) {
			bodiesCopy.add(body.clone());
		}
		return bodiesCopy;
	}
	*/

	// multithreading pog
	class SplitStep implements Runnable {

		// indexes for calculations
		int start, end;

		public SplitStep(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public void run() {
			// certain number of bodies
			for (int i = start; i < end; i++) {
				Body2D body = bodies.get(i);
				for (int j = 0; j < bodies.size(); j++) {
					Body2D otherBody = bodies.get(j);
					if (body != otherBody) {
						body.move(otherBody, timeStep);
					}
				}
			}
		}
	}

	public void paintComponent(Graphics graphics) {
		// graphics stuff
		Graphics2D g = (Graphics2D) graphics;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // anti-aliasing

		// frame start
		final long startTime = System.currentTimeMillis();

		// background
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, screenWidth, screenHeight);

		// moving origin to center of the screen (positive y is still downwards)
		g.translate(screenWidth / 2, screenHeight / 2);

		// drawing bodies
		for (Body2D body : bodies) {
			g.setColor(body.getColor());

			// calculating coords relative to camera
			int screenX = convert(body.getSX(), cameraX);
			int screenY = convert(body.getSY(), cameraY);
			int screenR = convert(body.getRadius(), 0);
			if (screenR < 1)
				screenR = 1;
			g.fillOval(screenX - screenR, screenY * -1 - screenR, screenR * 2, screenR * 2);
			if (body.getColor() == Color.BLACK) {
				g.setColor(Color.WHITE);
				g.drawOval(screenX - screenR, screenY * -1 - screenR, screenR * 2, screenR * 2);
			}

			// trails (TODO)

			// debugging
			if (debug == true) {
				g.setColor(Color.RED);
				// bounds
				g.drawRect(screenX - screenR, screenY * -1 - screenR, screenR * 2, screenR * 2);
				// center
				screenX = convert(body.getSX(), cameraX);
				screenY = convert(body.getSY(), cameraY);
				screenR = 1;
				g.drawOval(screenX - screenR, screenY * -1 - screenR, screenR * 2, screenR * 2);
				// arrow
				int x1 = screenX;
				int y1 = screenY;
				int x2 = (int) (x1 + (body.getVX() / screenScale * 10000));
				int y2 = (int) (y1 + (body.getVY() / screenScale * 10000));
				g.drawLine(x1, y1 * -1, x2, y2 * -1);
			}
		}
		// barycenter
		if (barycenter == true) {
			double bx = 0;
			double by = 0;
			double m = 0;
			for (Body2D body : bodies) {
				bx += body.getMass() * body.getSX();
				by += body.getMass() * body.getSY();
				m += body.getMass();
			}
			bx /= m;
			by /= m;
			bx = convert(bx, cameraX);
			by = convert(by, cameraY);
			g.setColor(Color.WHITE);
			g.drawLine((int) (bx - 5), (int) (by * -1), (int) (bx + 5), (int) (by * -1));
			g.drawLine((int) (bx), (int) (by * -1 - 5), (int) (bx), (int) (by * -1 + 5));
		}

		// divvying up work between cores
		num = bodies.size() / n;
		rem = bodies.size() % n; // last core
		if (!paused) {

			// saving previous frame
			addFrame();

			// precise mode with multithreading
			if (physicsMode == 0) {

				// repeat for multiple step in one frame
				for (int steps = 0; steps < timeMult; steps++) {

					// first n cores
					threads = new ArrayList<>();
					if (num != 0) {
						for (int i = 0; i < n; i++) {
							Thread t = new Thread(new SplitStep(i * num, i + 1 * num));
							t.start();
							threads.add(t);
						}
					}
					// last remainder core
					if (rem != 0) {
						Thread t = new Thread(new SplitStep(bodies.size() - rem, bodies.size()));
						t.start();
						threads.add(t);
					}

					// waits for calculations to finish
					for (Thread thread : threads) {
						try {
							thread.join();
						} catch (InterruptedException e) {
							e.printStackTrace(); // something went wrong
						}
					}

					// actual step
					for (int i = 0; i < bodies.size(); i++) {
						bodies.get(i).actuallyMove(timeStep);
					}

					// collisions
					colCheck();

					// tidal forces (Roche Limit)
					splitCheck();

				}
			}
			// precise mode
			else if (physicsMode == 1) {
				// multiple steps in one frame
				for (int steps = 0; steps < timeMult; steps++) {
					step(timeStep);
				}
			}
			// fast mode
			else if (physicsMode == 2) {
				step(timeScale);
			}
		}

		// camera
		if (heldKeys.size() > 0) {
			selected = null;
			relative = false;
			for (int key : heldKeys) {
				if (key == KeyEvent.VK_W) {
					cameraY += screenScale * 10;
				} else if (key == KeyEvent.VK_A) {
					cameraX -= screenScale * 10;
				} else if (key == KeyEvent.VK_S) {
					cameraY -= screenScale * 10;
				} else if (key == KeyEvent.VK_D) {
					cameraX += screenScale * 10;
				}
			}
		}
		if (selected != null) { // follows object
			cameraX = selected.getSX();
			cameraY = selected.getSY();
		}
		
		// aligning to top left corner
		g.translate(screenWidth / -2, screenHeight / -2);
		
		// pause border
		if (paused && text)
			drawPauseBorder(g);
		
		// drawing text and debug stuff
		if (text)
			drawText(g);

		// frame end
		final long endTime = System.currentTimeMillis();
		long frameTime = endTime - startTime;

		// maintaining fps
		if (frameTime < targetTime) { // ahead
			try {
				TimeUnit.MILLISECONDS.sleep((long) (targetTime - frameTime));
			} catch (InterruptedException e) { // something very wrong has happened
				e.printStackTrace();
			}
		}

		// frame actually ends
		final long finalTime = System.currentTimeMillis();
		frameTime = finalTime - startTime;

		// calculating fps
		totalTime += frameTime;
		totalFrames++;
		if (totalTime >= 1000) { // ~1s intervals
			currentFPS = totalFrames / (totalTime / 1000.0);
			totalTime = 0;
			totalFrames = 0;
		}

		// next frame
		repaint();

	}

	// saving frame for rewinding
	public void addFrame() {
		ArrayList<Body2D> oldFrame = new ArrayList<Body2D>();
		for (Body2D body : bodies)
			oldFrame.add(body.copy());
		frames.add(oldFrame);
		frame++;
		if (frames.size() > max_frames) { // deleting oldest frame
			frames.remove(0);
			frame--;
		}
	}
	
	// drawing pause border
	public void drawPauseBorder(Graphics2D g) {
		// red bc its noticeable
		g.setColor(Color.RED);
		int w = 5; // width of border in pixels
		
		g.fillRect(0, 0, screenWidth, w); // top
		g.fillRect(0, 0, w, screenHeight); // left
		g.fillRect(0, screenHeight - w, screenWidth, w); // bottom
		g.fillRect(screenWidth - w, 0, w, screenHeight); // right
	}
	
	// drawing text (regular or debug)
	public void drawText(Graphics2D g) {
		// setting text color
		g.setColor(Color.WHITE);

		ArrayList<String> menu = new ArrayList<>(); // list of lines of text to draw
		int text_start_pos = 20; // starting position of text
		if (debug == false) {
			// title
			g.setFont(new Font("Dialog", Font.PLAIN, 14)); // title font
			g.drawString("2D N-Body Simulator", 10, 20); // program title
			g.setFont(new Font("Dialog", Font.PLAIN, 10)); // subtitle font
			g.drawString("v.3.6", 145, 20); // version
			g.drawString("by Jason Kim", 10, 35); // by me
			
			g.setFont(new Font("Dialog", Font.PLAIN, 12)); // regular font
			if (help) {
				// starting position is moved because of title
				text_start_pos = 60;

				// regular text
				menu.add("Esc - Exit");
				menu.add("F3 - Debug Mode");
				menu.add("F2 - Screenshot Mode");
				menu.add("F1 - Literally does nothing");
				menu.add("");
				menu.add("Current Location: (" + round(cameraX, 1) + ", " + round(cameraY, 1) + ")");
				menu.add("WASD - Move camera");
				menu.add("Zoom - Shift/Control or Scroll");
				menu.add(screenScale + " meters per pixel");
				menu.add("");
				if (paused)
					menu.add("Paused");
				menu.add("Space - Toggle Pause");
				menu.add((timeScale * targetFPS) + " seconds per second");
				menu.add("Period - Increase Time Speed");
				menu.add("Comma - Decrease Time Speed");
				if (paused)
					menu.add("Left Arrow - Rewind");
				menu.add("");
				if (barycenter)
					menu.add("B - Hide Barycenter");
				else
					menu.add("B - Show Barycenter");
				menu.add("");
				menu.add("Physics Mode: " + physicsMode);
				menu.add("Alt+0: Multithreading (Experimental)");
				menu.add("Alt+1: Precise");
				menu.add("Alt+2: Fast");
				menu.add("");
				menu.add("Scenario: " + scenario);
				menu.add("1: Lone Planet");
				menu.add("2: Binary System");
				menu.add("3: Three Bodies");
				menu.add("4: Random");
				menu.add("5: Earth and Moon(s)");
				menu.add("6: Primordial");
				menu.add("7: Trojans");
				menu.add("8: Solar System");
				menu.add("9: Sagittarius A (beta)");
				/*
				menu.add("");
				menu.add("Trail Length: " + trailLen);
				menu.add("Z - Increase Trails");
				menu.add("X - Decrease Trails");
				menu.add("\\ - Switch Trail Mode");
				*/
				if (selected != null) {
					menu.add("");
					menu.add("Currently Selected: " + selected.toString());
					menu.add("Mass: " + selected.getMass());
					menu.add("Radius: " + selected.getRadius());
					menu.add("Position: (" + selected.getSX() + ", " + selected.getSY() + ")");
					menu.add("Velocity: (" + round(selected.getVX(), 1) + ", " + round(selected.getVY(), 1) + ")");
					menu.add("Acceleration: (" + round(selected.getAX(), 5) + ", " + round(selected.getAY(), 5) + ")");
				}
			}
			else
				g.drawString("H - Help", 10, 60);
		}

		// debugging text
		else {
			menu.add("Debug Mode On");
			menu.add("");
			menu.add("Screen: " + screenWidth + " x " + screenHeight);
			menu.add("Screen Scale: " + screenScale + " m/pixel");
			menu.add("");
			menu.add("Target FPS: " + targetFPS);
			menu.add("Total Time: " + totalTime);
			menu.add("Total Frames: " + totalFrames);
			menu.add("Current FPS: " + round(currentFPS, 1));
			menu.add("");
			menu.add("Frames: " + frames.size());
			menu.add("Frame: " + frame);
			menu.add("");
			menu.add("Paused: " + paused);
			menu.add("");
			menu.add("Physics Mode: " + physicsMode);
			menu.add("");
			menu.add("Multithreading:");
			menu.add("Processors: " + (n + 1));
			menu.add("Bodies/Processor: " + num);
			menu.add("Remainder: " + rem);
			if (threads != null)
				menu.add("Threads: " + threads.size());
			menu.add("");
			menu.add("Precise:");
			menu.add("Time Step: " + round(timeStep, 5) + " seconds per frame");
			menu.add("Time Multiplier: " + timeMult + "x");
			menu.add("");
			menu.add("Fast Mode:");
			menu.add("Time Scale: " + round(timeScale, 5) + " seconds per frame");
			menu.add("");
			menu.add("Barycenter: " + barycenter);
			menu.add("Collisions: " + collisions);
			menu.add("Tidal Forces: " + tidalForces);
			menu.add("");
			menu.add("Minimum Mass: " + minMass);
			menu.add("# of Bodies: " + bodies.size());
			menu.add("Trail Length: " + trailLen);
			menu.add("");
			menu.add("Meters/Pixel: " + screenScale);
			menu.add("Current Location: (" + round(cameraX, 1) + ", " + round(cameraY, 1) + ")");
			menu.add("");
			menu.add("Held Keys: " + heldKeys.toString());
			menu.add("Alt: " + alt);
			if (click != null) {
				menu.add("");
				menu.add("Last Click: " + "(" + click.getX() + ", " + click.getY() + "), (" + (click.getX() * screenScale) + ", " + (click.getY() * screenScale) + ")");
			}
			if (selected != null) {
				menu.add("");
				menu.add("Selected: " + selected.toString());
				menu.add("Mass: " + selected.getMass());
				menu.add("Radius: " + selected.getRadius());
				menu.add("Position: (" + selected.getSX() + ", " + selected.getSY() + ")");
				menu.add("Velocity: (" + round(selected.getVX(), 1) + ", " + round(selected.getVY(), 1) + ")");
				menu.add("Acceleration: (" + round(selected.getAX(), 5) + ", " + round(selected.getAY(), 5) + ")");
			}
		}
		
		if (help || debug) {
			// body locator
			String[] bodyStrings; // body details of the top 5 massive bodies
			int[] bodyLengths; // lengths of the strings in bodyStrings
			// TOFIX: sorting by mass
			sortedBodies = new ArrayList<Body2D>();
			for (Body2D body : bodies)
				sortedBodies.add(body);
			for (int i = 1; i < sortedBodies.size(); i++) {
				Body2D body = sortedBodies.get(i);
				int j = i;
				while (j > 0 && sortedBodies.get(j - 1).getMass() < body.getMass())
					j--;
				sortedBodies.remove(body);
				sortedBodies.add(j, body);
			}
			// generating body details and calculating bounds
			bodyStrings = new String[Math.min(sortedBodies.size(), 5)];
			bodyLengths = new int[bodyStrings.length];
			bodyBounds = new Rectangle[bodyLengths.length];
			for (int i = 0; i < bodyStrings.length; i++) {
				Body2D body = sortedBodies.get(i);
				bodyStrings[i] = body.toString() + ": " + body.getMass() + " kg, " + body.getRadius() + " m";
				bodyLengths[i] = g.getFontMetrics().stringWidth(bodyStrings[i]);
			}
			// body locator text
			menu.add("");
			menu.add("Body Locator:");
			for (int i = 0; i < bodyStrings.length; i++) {
				Body2D body = sortedBodies.get(i);
				menu.add(bodyStrings[i]);
			}

			// drawing text
			int i;
			for (i = 0; i < menu.size(); i++) {
				g.drawString(menu.get(i), 10, 20 * i + text_start_pos);
			}

			// bounds for body locator
			for (int j = 0; j < bodyLengths.length; j++) {
				int x = 5;
				int y = text_start_pos + (20 * (i + j)) - (20 * bodyLengths.length) + 5 - 20; // -20 for some reason
				int x_len = bodyLengths[j] + 10;
				int y_len = 20;
				bodyBounds[j] = new Rectangle(x, y, x_len, y_len);
			}
			// debugging bounds
			if (debug) {
				g.setColor(Color.RED);
				for (Rectangle bounds : bodyBounds) {
					int x = (int) bounds.getX();
					int y = (int) bounds.getY();
					int w = (int) bounds.getWidth();
					int h = (int) bounds.getHeight();
					g.drawRect(x, y, w, h);
				}
			}
		}
	}

	// rounding values so that they don't take up the entire screen
	public double round(double value, int figs) { // figs: number of places after decimal point
		return Math.round(value * Math.pow(10, figs)) / Math.pow(10, figs);
	}

	// scale time by x
	public void scaleTime(double x) {
		timeMult *= x;
		timeScale *= x;
		if (timeMult < 1) {
			timeMult = 1;
			timeScale *= x;
		}
	}
	
	// nothing to see here
	public void doNothing() {
		nothing = 0;
		nothing_limit = 1;
		// nothing_mode = !nothing_mode;
		for (Body2D body: bodies)
			body.setColor(random_color());
	}
	public Color random_color() {
		return new Color((int)(Math.random() * 256), (int)(Math.random() * 256), (int)(Math.random() * 256));
	}

	class KeyHandler extends KeyAdapter {

		public void keyPressed(KeyEvent e) {
			/*
			System.out.println(e);
			System.out.println(e.getSource());
			System.out.println(e.getID());
			System.out.println(e.getWhen());
			// System.out.println(e.getModifiers());
			System.out.println(e.getModifiersEx());
			System.out.println(e.getKeyCode());
			System.out.println(e.getKeyChar());
			System.out.println();
			*/
			int keyCode = e.getKeyCode();

			// time
			if (e.getKeyCode() == KeyEvent.VK_PERIOD) {
				scaleTime(2.0);
			} else if (e.getKeyCode() == KeyEvent.VK_COMMA) {
				scaleTime(0.5);
			}
			// rewinding
			if (paused) { // only works if paused
				if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					frame--;
					if (frame < 0)
						frame = 0;
					bodies = frames.get(frame);
				} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
					frame++;
					if (frame >= frames.size())
						frame = frames.size() - 1;
					bodies = frames.get(frame);
				}
			}

			// scale
			if (keyCode == KeyEvent.VK_SHIFT) {
				screenScale /= 2;
			} else if (keyCode == KeyEvent.VK_CONTROL) {
				screenScale *= 2;
			}

			// movement
			else if (holdableKeys.contains(keyCode) && !heldKeys.contains(keyCode)) {
				heldKeys.add(keyCode);
			} else if (keyCode == KeyEvent.VK_ALT) {
				alt = true;
			}

			// trail
			else if (keyCode == KeyEvent.VK_Z) {
				trailLen++;
			} else if (keyCode == KeyEvent.VK_X) {
				trailLen--;
				if (trailLen < -1)
					trailLen = -1;
			} else if (selected != null && keyCode == KeyEvent.VK_BACK_SLASH) {
				relative = !relative;
			}

		}

		public void keyReleased(KeyEvent e) {
			int keyCode = e.getKeyCode();

			// scenarios
			if (alt == false) {
				if (keyCode == KeyEvent.VK_0) {
					scenario = 0;
					reset();
				} else if (keyCode == KeyEvent.VK_1) {
					scenario = 1;
					reset();
				} else if (keyCode == KeyEvent.VK_2) {
					scenario = 2;
					reset();
				} else if (keyCode == KeyEvent.VK_3) {
					scenario = 3;
					reset();
				} else if (keyCode == KeyEvent.VK_4) {
					scenario = 4;
					reset();
				} else if (keyCode == KeyEvent.VK_5) {
					scenario = 5;
					reset();
				} else if (keyCode == KeyEvent.VK_6) {
					scenario = 6;
					reset();
				} else if (keyCode == KeyEvent.VK_7) {
					scenario = 7;
					reset();
				} else if (keyCode == KeyEvent.VK_8) {
					scenario = 8;
					reset();
				} else if (keyCode == KeyEvent.VK_9) {
					scenario = 9;
					reset();
				}
			}

			// physics
			/*
			if (keyCode == KeyEvent.VK_H) {
				n++;
				minMass *= 10;
			} else if (keyCode == KeyEvent.VK_N) {
				n--;
				if (n < 1)
					n = 1;
				minMass /= 10;
			}
			*/
			if (alt == true && keyCode == KeyEvent.VK_0) { // physics mode 0
				physicsMode = 0;
			} else if (alt == true && keyCode == KeyEvent.VK_1) { // physics mode 1
				physicsMode = 1;
			} else if (alt == true && keyCode == KeyEvent.VK_2) { // physics mode 2
				physicsMode = 2;
			}

			// sim
			else if (keyCode == KeyEvent.VK_SPACE) { // pause
				paused = !paused;
				if (paused == false) // if the user rewinded and unpaused, future frames are deleted
					while (frame < frames.size() - 1)
						frames.remove(frame + 1);
			} else if (keyCode == KeyEvent.VK_R) { // reset
				reset();
			} else if (keyCode == KeyEvent.VK_ESCAPE) { // exit
				System.exit(0);
			} else if (keyCode == KeyEvent.VK_B) { // barycenter
				barycenter = !barycenter;
			}

			// ui
			else if (text && !debug && keyCode == KeyEvent.VK_H) { // help view
				help = !help;
			} else if (keyCode == KeyEvent.VK_F1) { // this does nothing
				nothing++;
				if (nothing == nothing_limit)
					doNothing();
			} else if (keyCode == KeyEvent.VK_F2) { // ui view
				text = !text;
			} else if (text && keyCode == KeyEvent.VK_F3) { // debug
				debug = !debug;
			}

			// movement
			else if (holdableKeys.contains(keyCode) && heldKeys.contains(keyCode)) {
				heldKeys.remove((Integer) keyCode);
			} else if (keyCode == KeyEvent.VK_ALT) {
				alt = false;
			}
		}
	}

	// mouse inputs
	class MouseHandler extends MouseAdapter {

		public void mouseClicked(MouseEvent e) {

			// converting coords
			click = e.getLocationOnScreen();
			int newX = (int) (click.getX() - screenWidth / 2);
			int newY = (int) (click.getY() - screenHeight / 2) * -1;
			Point centered_click = new Point(newX, newY);

			// selecting bodies
			selected = null;
			for (Body2D body : bodies) {
				// calculating bounds
				int boundsX = convert(body.getSX(), cameraX);
				int boundsY = convert(body.getSY(), cameraY);
				int boundsR = convert(body.getRadius(), 0);
				Rectangle bounds = new Rectangle(boundsX - boundsR, boundsY - boundsR, boundsR * 2, boundsR * 2);
				// checking
				if (bounds.contains(centered_click)) {
					// splitting bodies (debug only)
					if (debug == true && e.getButton() == MouseEvent.BUTTON3) { // right click
						splitBody(body);
						break;
					} else {
						selected = body;
						break;
					}
				}
			}

			// body locator
			if (text) { // only available if text is visible
				for (int i = 0; i < bodyBounds.length; i++) {
					if (bodyBounds[i].contains(click)) {
						selected = sortedBodies.get(i); // selecting body
					}
				}
			}

		}

	}

	// mouse wheel inputs
	class MouseWheelHandler implements MouseWheelListener {

		public void mouseWheelMoved(MouseWheelEvent e) {
			// zoom in
			if (e.getWheelRotation() > 0) {
				screenScale *= 2;
			}
			// zoom out
			else if (e.getWheelRotation() < 0) {
				screenScale /= 2;
			}
		}

	}
}