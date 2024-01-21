import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class Simulation {

	// window
	Window parentWindow;
	
	// simulation
	/*
	// TOFIX
	private ArrayList<ArrayList<Body>> frames = new ArrayList<ArrayList<Body>>();
	private int frame = -1;
	private final int max_frames = 1000; // to prevent memory overuse
	*/
	private boolean collisions = true;
	private boolean tidalForces = true;
	private double minMass = Math.pow(10, 28); // kg

	// physics
	private final double G = 6.6743 * Math.pow(10, -11); // m^3 kg^-1 s^-2
	private double S = 0; // softening parameter
	
	// bodies
	private ArrayList<Body> bodies = new ArrayList<Body>();
	private int scenario = 2;
	
	/*
	// TOFIX: trails
	private int trailLen = 0; // -1 = infinite trail
	private boolean relative = false;
	*/
	
	// barycenter
	private double bx = 0.0; // actual coordinates
	private double by = 0.0; // not screen coordinates
	private Rectangle baryBounds = null; // clickable bounds of the barycenter;

	// TOREMOVE
	/*
	// nothing
	private int nothing = 0;
	private int nothing_limit = 10;
	// private boolean nothing_mode = false;
	 */
	
	public Simulation() {}
	public Simulation(Window window) {parentWindow = window;}
	
	public void assignWindow(Window newWin) {parentWindow = newWin;}
	
	void setScenario(int newScenario) {
		scenario = newScenario;
	}
	
	// -----------------------------------------------------------------------------------------------------------------------------
	// TOFIX: reset sequence and hardcoded scenarios
	public void reset() {
		// simulation
		collisions = true;
		tidalForces = true;
		// bodies
		bodies.clear();
		// TOREMOVE: nothing
		// nothing_limit = 10;

		if (scenario == 0) {
			// debug scenario
			if (parentWindow.debugState()) {
				parentWindow.setScreenScale(5000000);
				parentWindow.setPhysicsMode(2);
				bodies.add(new Body(1.989 * Math.pow(10, 30), 696340000.0, 0.0, 0.0, 0.0, 0.0));
				bodies.add(new Body(5.972 * Math.pow(10, 24), 10378140.0, 5000000 * 500, 0, 0.0, 160000.0));
			}
			// colliding saturns
			else {
				// simulation
				minMass = Math.pow(10, 22);
				parentWindow.setScreenScale(2500000);
				parentWindow.setPhysicsMode(2);

				// Saturn 1
				double saturnM = 568 * Math.pow(10, 24);
				double saturnR = 120536000.0 / 2;
				double saturnSX = -500000000;
				double saturnSY = -125000000;
				double saturnVX = 10000;
				double saturnVY = 0;
				bodies.add(new Body(Color.WHITE, saturnM, saturnR, saturnSX, saturnSY, saturnVX, saturnVY));
				// Rings
				for (int i = 0; i < 500; i++) {
					double mass = Math.pow(10, 10);
					double radius = 1000.0;
					double t = Math.random() * 2 * Math.PI;
					double r = 75000000.0 * 2 + Math.random() * (137000000.0 - 75000000.0);
					double sx = r * Math.cos(t);
					double sy = r * Math.sin(t);
					double v = Math.sqrt(G * saturnM / r);
					double vx = v * Math.cos(t + Math.PI / 2);
					double vy = v * Math.sin(t + Math.PI / 2);
					bodies.add(new Body(mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));
				}

				// Saturn 2
				saturnM = 568 * Math.pow(10, 24);
				saturnR = 120536000.0 / 2;
				saturnSX *= -1;
				saturnSY *= -1;
				saturnVX *= -1;
				saturnVY = 0;
				bodies.add(new Body(Color.WHITE, saturnM, saturnR, saturnSX, saturnSY, saturnVX, saturnVY));
				// Rings
				for (int i = 0; i < 500; i++) {
					double mass = Math.pow(10, 10);
					double radius = 1000.0;
					double t = Math.random() * 2 * Math.PI;
					double r = 75000000.0 * 2 + Math.random() * (137000000.0 - 75000000.0);
					double sx = r * Math.cos(t);
					double sy = r * Math.sin(t);
					double v = Math.sqrt(G * saturnM / r);
					double vx = v * Math.cos(t + Math.PI / 2);
					double vy = v * Math.sin(t + Math.PI / 2);
					bodies.add(new Body(mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));
				}
			}
		}

		// lone planet
		if (scenario == 1) {
			parentWindow.setScreenScale(25000.0);
			parentWindow.setPhysicsMode(0);
			minMass = Math.pow(10, 22);
			bodies.add(new Body(Color.WHITE, 5.97 * Math.pow(10, 24), 12756000.0 / 2, 0.0, 0.0, 0.0, 0.0));
		}

		// two-body system
		else if (scenario == 2) {
			parentWindow.setScreenScale(10000000.0);
			parentWindow.setPhysicsMode(1);
			minMass = Math.pow(10, 28);
			bodies.add(new Body(1.989 * Math.pow(10, 30), 696340000.0, 10000000.0 * -500, 0, 0.0, -50000.0));
			bodies.add(new Body(1.989 * Math.pow(10, 30), 696340000.0, 10000000.0 * 500, 0, 0.0, 50000.0));
		}

		// three-bodies
		else if (scenario == 3) {
			collisions = false;
			tidalForces = false;
			parentWindow.setScreenScale(50000000.0);
			parentWindow.setPhysicsMode(1);
			minMass = Math.pow(10, 28);
			for (int i = 0; i < 3; i++) {
				double sx = (Math.random() - 0.5) * parentWindow.getScreenWidth() * 50000000.0;
				double sy = (Math.random() - 0.5) * parentWindow.getScreenHeight() * 50000000.0;
				double vx = (Math.random() - 0.5) * parentWindow.getScreenHeight() * 100;
				double vy = (Math.random() - 0.5) * parentWindow.getScreenHeight() * 100;
				bodies.add(new Body(1.989 * Math.pow(10, 30), 696340000.0, sx, sy, vx, vy));
			}
		}

		// generates sun-sized objects
		else if (scenario == 4) {
			parentWindow.setScreenScale(250000000.0);
			parentWindow.setPhysicsMode(2);
			minMass = Math.pow(10, 28);
			for (int i = 0; i < 500; i++) {
				double sx = (Math.random() - 0.5) * parentWindow.getScreenWidth() * parentWindow.getScreenScale();
				double sy = (Math.random() - 0.5) * parentWindow.getScreenHeight() * parentWindow.getScreenScale();
				bodies.add(new Body(1.989 * Math.pow(10, 30), 696340000.0, sx, sy, 0.0, 0.0));
			}
		}

		// Earth and Moon(s)
		else if (scenario == 5) {
			parentWindow.setScreenScale(125000.0);
			parentWindow.setPhysicsMode(2);
			minMass = Math.pow(10, 19.5);
			double m = 5.972 * Math.pow(10, 24);
			bodies.add(new Body(m, 6378140.0, 0.0, 0.0, 0.0, 0.0));
			double r = 10000000.0;
			double v = Math.sqrt(G * m / r);
			bodies.add(new Body(7.342 * Math.pow(10, 22), 1737400.0, r, 0.0, 0.0, v));
			r += 20000000.0;
			v = Math.sqrt(G * m / r);
			bodies.add(new Body(7.342 * Math.pow(10, 22), 1737400.0, 0.0, r, v * -1, 0.0));
			r += 20000000.0;
			v = Math.sqrt(G * m / r);
			bodies.add(new Body(7.342 * Math.pow(10, 22), 1737400.0, r * -1, 0.0, 0.0, v * -1));
			r += 20000000.0;
			v = Math.sqrt(G * m / r);
			bodies.add(new Body(7.342 * Math.pow(10, 22), 1737400.0, 0.0, r * -1, v, 0.0));
		}

		// primordial system consisting of Earth and Moon sized objects
		else if (scenario == 6) {
			minMass = Math.pow(10, 21);
			parentWindow.setScreenScale(10000000.0);
			parentWindow.setPhysicsMode(2);
			bodies.add(new Body(1.989 * Math.pow(10, 30), 696340000.0, 0.0, 0.0, 0.0, 0.0));
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
				double r = (Math.random() * (parentWindow.getScreenHeight() / 2 * parentWindow.getScreenScale() - buffer)) + buffer;
				double sx = r * Math.cos(t);
				double sy = r * Math.sin(t);
				double v = Math.sqrt(G * bodies.get(0).getMass() / r);
				double vx = v * Math.cos(t + Math.PI / 2);
				double vy = v * Math.sin(t + Math.PI / 2);
				// new body
				bodies.add(new Body(mass, radius, sx, sy, vx, vy));
			}
		}

		// Jupiter's Trojans
		else if (scenario == 7) {
			parentWindow.setScreenScale(100000000.0);
			parentWindow.setPhysicsMode(2);
			double m = 1.989 * Math.pow(10, 30);
			bodies.add(new Body(m, 696340000.0, 0.0, 0.0, 0.0, 0.0));
			for (int i = 0; i < 1000; i++) {
				double mass = 6.687 * Math.pow(10, 15); // Eros mass	
				double radius = 1000000.0;
				double t = Math.random() * 2 * Math.PI;
				double r = (parentWindow.getScreenHeight() / 2 * parentWindow.getScreenScale()) + (Math.random() - 0.5) * (100 * parentWindow.getScreenScale());
				double sx = r * Math.cos(t);
				double sy = r * Math.sin(t);
				double v = Math.sqrt(G * m / r);
				double vx = v * Math.cos(t + Math.PI / 2);
				double vy = v * Math.sin(t + Math.PI / 2);
				bodies.add(new Body(mass, radius, sx, sy, vx, vy));
			}
			// Jupiter
			double mass = 1898 * Math.pow(10, 24);
			double radius = 142984000.0 / 2;
			double t = Math.random() * 2 * Math.PI;
			double r = (parentWindow.getScreenHeight() / 2 * parentWindow.getScreenScale());
			double sx = r * Math.cos(t);
			double sy = r * Math.sin(t);
			double v = Math.sqrt(G * m / r);
			double vx = v * Math.cos(t + Math.PI / 2);
			double vy = v * Math.sin(t + Math.PI / 2);
			bodies.add(new Body(Color.WHITE, mass, radius, sx, sy, vx, vy));
		}

		// basic solar system model
		else if (scenario == 8) {
			parentWindow.setScreenScale(1250000000.0);
			parentWindow.setPhysicsMode(2);

			// Sun
			double m = 1.989 * Math.pow(10, 30);
			bodies.add(new Body(m, 696340000.0, 0.0, 0.0, 0.0, 0.0));

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
			bodies.add(new Body(Color.GRAY, mass, radius, sx, sy, vx, vy));

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
			bodies.add(new Body(Color.YELLOW, mass, radius, sx, sy, vx, vy));

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
			bodies.add(new Body(Color.BLUE, mass, radius, sx, sy, vx, vy));

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
			bodies.add(new Body(Color.GRAY, mass, radius, earthSX + sx, earthSY + sy, earthVX + vx, earthVY + vy));

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
			bodies.add(new Body(Color.RED, mass, radius, sx, sy, vx, vy));
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
			bodies.add(new Body(mass, radius, marsSX + sx, marsSY + sy, marsVX + vx, marsVY + vy));

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
			bodies.add(new Body(mass, radius, marsSX + sx, marsSY + sy, marsVX + vx, marsVY + vy));

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
				bodies.add(new Body(mass, radius, sx, sy, vx, vy));
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
			bodies.add(new Body(Color.WHITE, mass, radius, sx, sy, vx, vy));
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
			bodies.add(new Body(Color.YELLOW, mass, radius, jupiterSX + sx, jupiterSY + sy, jupiterVX + vx, jupiterVY + vy));

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
			bodies.add(new Body(Color.WHITE, mass, radius, jupiterSX + sx, jupiterSY + sy, jupiterVX + vx, jupiterVY + vy));

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
			bodies.add(new Body(Color.WHITE, mass, radius, jupiterSX + sx, jupiterSY + sy, jupiterVX + vx, jupiterVY + vy));

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
			bodies.add(new Body(Color.GRAY, mass, radius, jupiterSX + sx, jupiterSY + sy, jupiterVX + vx, jupiterVY + vy));

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
			bodies.add(new Body(Color.WHITE, mass, radius, sx, sy, vx, vy));
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
				bodies.add(new Body(mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));
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
			bodies.add(new Body(Color.GRAY, mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));

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
			bodies.add(new Body(Color.WHITE, mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));

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
			bodies.add(new Body(Color.WHITE, mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));

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
			bodies.add(new Body(Color.WHITE, mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));

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
			bodies.add(new Body(Color.WHITE, mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));

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
			bodies.add(new Body(Color.YELLOW, mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));

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
			bodies.add(new Body(Color.WHITE, mass, radius, saturnSX + sx, saturnSY + sy, saturnVX + vx, saturnVY + vy));

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
			bodies.add(new Body(Color.BLUE, mass, radius, sx, sy, vx, vy));

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
			bodies.add(new Body(Color.BLUE, mass, radius, sx, sy, vx, vy));
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
			bodies.add(new Body(Color.WHITE, mass, radius, neptuneSX + sx, neptuneSY + sy, neptuneVX + vx, neptuneVY + vy));

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
			bodies.add(new Body(Color.WHITE, mass, radius, sx, sy, vx, vy));
		}

		// tidal forces demonstration
		else if (scenario == 9) {
			minMass = Math.pow(10, 28);
			parentWindow.setScreenScale(125000000.0);
			parentWindow.setPhysicsMode(2);

			// Sagittarius A
			double holeMass = 8.26 * Math.pow(10, 36);
			double holeRadius = 12000000000.0;
			bodies.add(new Body(Color.BLACK, holeMass, holeRadius, 0.0, 0.0, 0.0, 0.0));

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
					bodies.add(new Body(Color.WHITE, mass, radius, sx, sy, vx, vy));
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
		updateBarycenter();
		// addFrame(); // TOFIX
	}
	
	// -----------------------------------------------------------------------------------------------------------------------------
	// actual physics stuff
	
	// TODO: main physics loop
	public void loop() {

	}
	
	// integrating simulation
	public void step(double deltaTime) {
		// calculating motion
		for (Body body : bodies) {
			for (Body otherBody : bodies) {
				if (body != otherBody) {
					body.move(otherBody, deltaTime, S);
				}
			}
		}
		// actually moving
		for (Body body : bodies) {
			body.actuallyMove(deltaTime);
		}
		// split check
		splitCheck();
		// collision check
		colCheck();
		// TOFIX: updating barycenter
		updateBarycenter();
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
					Body body = bodies.get(i);
					for (int j = i + 1; j < bodies.size(); j++) {
						Body otherBody = bodies.get(j);
						Body newBody = body.collision(otherBody);
						if (newBody != null) {
							Body selected = parentWindow.getSelected(); // getting currently selected body
							if (selected == body || selected == otherBody)
								parentWindow.setSelected(newBody);

							// attaching old trails to new trail
							/*
							body.getTrail().attachTo(newBody.getTrail());
							otherBody.getTrail().attachTo(newBody.getTrail());
							newBody.getTrail().attachFrom(body.getTrail());
							newBody.getTrail().attachFrom(otherBody.getTrail());
							*/

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
	public boolean splitBody(Body body) {
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
					bodies.add(new Body(body.getColor(), nm, nr, nx, ny, body.getVX(), body.getVY()));
				}
			}
			if (parentWindow.getSelected() == body) {
				Body newSelected = bodies.get(bodies.size() - 1 - (int) (Math.random() * 4));
				parentWindow.setSelected(newSelected);
			}
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
				Body body = bodies.get(i);
				for (int j = i + 1; j < bodies.size(); j++) {
					Body otherBody = bodies.get(j);
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
				Body body = bodies.get(i);
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

	// TOFIX: multithreading
	/*
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
				Body body = bodies.get(i);
				for (int j = 0; j < bodies.size(); j++) {
					Body otherBody = bodies.get(j);
					if (body != otherBody) {
						body.move(otherBody, timeStep);
					}
				}
			}
		}
	}
	*/
	
	// updating barycenter
	public void updateBarycenter() {
		double m = 0; // total mass of all bodies
		for (Body body : bodies) {
			bx += body.getMass() * body.getSX();
			by += body.getMass() * body.getSY();
			m += body.getMass();
		}
		bx /= m;
		by /= m;
		parentWindow.updateBaryBounds();
	}
	
	// TOREMOVE: i dont want to be sued
	/*
	// nothing to see here
	public void doNothing() {
		nothing = 0;
		nothing_limit = 1;
		// nothing_mode = !nothing_mode;
		for (Body body : bodies)
			body.setColor(random_color());
	}
	
	// generates random color
	public Color random_color() {
		return new Color((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255));
	}
	*/
	
	// -----------------------------------------------------------------------------------------------------------------------------
	// get/set methods
	
	// simulation
	boolean isCollisionEnabled() {return collisions;}
	boolean isTidalForcesEnabled() {return tidalForces;}
	double getMinMass() {return minMass;}
	
	// physics
	void setS(double newS) {S = newS;}
	double getS() {return S;}
	
	// bodies
	ArrayList<Body> getBodies() {return bodies;}
	int getScenario() {return scenario;}
	
	// barycenter
	double getBX() {return bx;}
	double getBY() {return by;}
}
