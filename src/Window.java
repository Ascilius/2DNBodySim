// Java 2D N-Body Simulation Version 4.0.0 by Jason Kim

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

public class Window extends JPanel {

	// debug
	private boolean debug = false; // toggleable within running program
	
	// screen
	private final int screenWidth, screenHeight;
	private double screenScale = 1.0; // m per pixel
	
	// frames
	private final double targetFPS = 60.0;
	private final double targetTime = 1000.0 / targetFPS; // ms
	private long totalTime = 0;
	private long totalFrames = 0;
	private double currentFPS = 0.0;
	private boolean text = true;
	private boolean help = false;

	// camera
	private double cameraX = 0.0;
	private double cameraY = 0.0;
	private Body selected = null;
	private final double zoom_speed = 1.05; // how fast the screen zooms in/out

	// inputs
	private ArrayList<Integer> heldKeys = new ArrayList<>();
	private Point click = null; // location of the last click

	// body locator
	private ArrayList<Body> sortedBodies; // sorted bodies based on mass
	private Rectangle[] bodyBounds = {}; // bounds for clickable areas to select bodies
	
	// simulation management
	private Manager manager;
	private Simulation sim;
	private int sim_i; // index of current sim within manager
	
	// TOFIX: simulation
	private boolean paused = false;
	private int physicsMode = -1;
	private final double stepTime = 60.0; // seconds per second
	private final double timeStep = stepTime / targetFPS; // seconds per frame
	private int timeMult = 1;
	/*
	// TOFIX: multithreading
	private int n = Runtime.getRuntime().availableProcessors() - 1;
	private int num, rem;
	private ArrayList<Thread> threads;
	*/
	
	// barycenter
	private boolean barycenter = false; // show/hide barycenter
	private final int barySize = 5; // size of the barycenter on the screen
	private boolean barySelected = false; // center camera on barycenter
	private Rectangle baryBounds = null; // clickable bounds of the barycenter;
	
	public Window(Manager manager, int screenWidth, int screenHeight) {
		// TOFIX: idk what this does
		setFocusable(true);
		
		// screen
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;

		// keyboard inputs
		KeyHandler keyHandler = new KeyHandler();
		addKeyListener(keyHandler);
		setFocusTraversalKeysEnabled(false); // TOFIX: idk what this does
		// mouse inputs
		addMouseListener(new MouseHandler());
		addMouseWheelListener(new MouseWheelHandler());

		// assigning manager
		this.manager = manager;
		
		// TOFIX: creating new sim
		sim = new Simulation(this);
		manager.addSim(sim);
		
		// starting sim
		reset(); // resets both window and sim
		scaleTime(1024); // TOFIX
		paused = !paused;
	}
	
	// assigns a new sim to this window
	public void assignSim(Simulation newSim, int new_i) {
		sim = newSim;
		sim_i = new_i;
		sim.assignWindow(this);
	}
	
	// request a new sim assigned to this window
	public void requestSim(int di) {manager.reassignSim(this, sim_i, di);}
	
	// reset both window and simulation
	public void reset() {
		// time
		paused = true;
		timeMult = 1;
		// camera
		cameraX = 0.0;
		cameraY = 0.0;
		selected = null;
		// barycenter
		barySelected = false;
		
		// simulation
		sim.reset();
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// painting
	public void paintComponent(Graphics graphics) {
		// graphics stuff
		Graphics2D g = (Graphics2D) graphics;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // anti-aliasing

		// frame start
		final long startTime = System.currentTimeMillis();
		
		// ---------------------------------------------------------------------------
		// drawing stuff
		
		// background
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, screenWidth, screenHeight);
		
		// drawing bodies
		drawBodies(g);
		
		// drawing text and debug stuff
		if (text)
			drawText(g);
		
		// pause border
		if (paused && text)
			drawPauseBorder(g);

		// ---------------------------------------------------------------------------
		// TOFIX: updating
		/*
		// TOFIX: divvying up work between cores
		num = bodies.size() / n;
		rem = bodies.size() % n; // last core
		*/
		if (!paused) {
	
			// TOFIX: saving previous frame
			// addFrame();
			
			// TOFIX: precise mode with multithreading
			if (physicsMode == 0) {
				/*
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
			 	*/
			}
			
			// precise mode
			else if (physicsMode == 1) {
				// multiple steps in one frame
				for (int steps = 0; steps < timeMult; steps++) {
						sim.step(timeStep);
				}
			}
			
			// fast mode
			else if (physicsMode == 2)
				sim.step(timeStep * timeMult);
		}
		
		// updating camera
		updateCamera();
		
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
		if (totalTime >= 1000) // update FPS at ~1s intervals
			calculateFPS();

		// next frame
		repaint();
		
	}
	
	// -----------------------------------------------------------------------------------------------------------------------------
	// time management
	
	public void resetTimeData() {
		totalTime = 0;
		totalFrames = 0;
	}
	
	public void calculateFPS() {
		currentFPS = totalFrames / (totalTime / 1000.0);
		resetTimeData();
	}
	
	// -----------------------------------------------------------------------------------------------------------------------------
	// simulation functions
	
	public void pause() {
		paused = true;
	}
	
	// incrementing timeMult
	public void scaleTime(double x) { // x should be either 2 or 0.5
		timeMult *= x;
		if (heldKeys.contains(KeyEvent.VK_SHIFT)) // shift mode
			timeMult *= x;
		if (timeMult < 1) // no sub-second time speeds
			timeMult = 1;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// helper painting functions
	
	/*
	// TOFIX: saving frame for rewinding
	public void addFrame() {
		ArrayList<Body> oldFrame = new ArrayList<Body>();
		for (Body body : bodies)
			oldFrame.add(body.copy());
		frames.add(oldFrame);
		frame++;
		if (frames.size() > max_frames) { // deleting oldest frame
			frames.remove(0);
			frame--;
		}
	}
	*/

	// drawing bodies (and barycenter)
	public void drawBodies(Graphics2D g) {
		// bodies
		ArrayList<Body> bodies = sim.getBodies();
		for (Body body : bodies) {
			g.setColor(body.getColor());

			// calculating coords relative to camera
			int[] screen_coords = convert(body.getSX(), body.getSY());
			int screenX = screen_coords[0];
			int screenY = screen_coords[1];
			int screenR = (int) (body.getRadius() / screenScale);
			if (screenR < 1)
				screenR = 1;
			g.fillOval(screenX - screenR, screenY - screenR, screenR * 2, screenR * 2);
			if (body.getColor() == Color.BLACK) {
				g.setColor(Color.WHITE);
				g.drawOval(screenX - screenR, screenY - screenR, screenR * 2, screenR * 2);
			}

			// TODO: trails

			// debugging
			if (debug == true) {
				g.setColor(Color.RED);
				// bounds
				g.drawRect(screenX - screenR, screenY - screenR, screenR * 2, screenR * 2);
				// center
				screen_coords = convert(body.getSX(), body.getSY());
				screenX = screen_coords[0];
				screenY = screen_coords[1];
				screenR = 1;
				g.drawOval(screenX - screenR, screenY - screenR, screenR * 2, screenR * 2);
				// arrow
				int x1 = screenX;
				int y1 = screenY;
				int x2 = (int) (x1 + (body.getVX() / screenScale * 10000));
				int y2 = (int) (y1 - (body.getVY() / screenScale * 10000));
				g.drawLine(x1, y1, x2, y2);
			}
		}
		// barycenter
		if (barycenter == true) {
			// getting barycenter data
			double bx = sim.getBX();
			double by = sim.getBY();
			
			// calculating on-screen barycenter stuff
			int[] screen_coords = convert(bx, by);
			int screenX = screen_coords[0];
			int screenY = screen_coords[1];
			g.setColor(Color.WHITE);
			g.drawLine(screenX - barySize, screenY, screenX + barySize, screenY); // horizontal line
			g.drawLine(screenX, screenY - barySize, screenX, screenY + barySize); // vertical line
			// clickable bounds of barcenter
			if (debug) {
				screenX -= barySize;
				screenY -= barySize;
				g.setColor(Color.RED);
				g.drawRect(screenX, screenY, barySize * 2, barySize * 2);
			}
		}
	}

	// TOFIX: converts actual coords to screen coords
	public int[] convert(double x, double y) {
		int screenX = (int) ((x - cameraX) / screenScale) + (screenWidth / 2);
		int screenY = (int) ((y - cameraY) / screenScale) * -1 + (screenHeight / 2);
		int[] screen_coords = { screenX, screenY };
		return screen_coords;
	}
	
	// zooming camera; adjusting screenScale
	public void scaleScreen(double x) {
		if (heldKeys.contains(KeyEvent.VK_SHIFT)) // shift mode
			x *= Math.pow(x, 4); // magnifies shift
		screenScale *= x;
	}

	// updating camera
	public void updateCamera() {
		if (heldKeys.size() > 0) {
			for (int key : heldKeys) {
				// translating camera
				if (key == KeyEvent.VK_W)
					translateCamera(0, screenScale * 10);
				else if (key == KeyEvent.VK_A)
					translateCamera(screenScale * -10, 0);
				else if (key == KeyEvent.VK_S)
					translateCamera(0, screenScale * -10);
				else if (key == KeyEvent.VK_D)
					translateCamera(screenScale * 10, 0);
				// zooming camera
				else if (key == KeyEvent.VK_F)
					scaleScreen(1 / zoom_speed);
				else if (key == KeyEvent.VK_V)
					scaleScreen(zoom_speed);
			}
		}
		if (selected != null) { // follows object
			cameraX = selected.getSX();
			cameraY = selected.getSY();
		}
		if (barySelected) { // follows barycenter
			cameraX = sim.getBX();
			cameraY = sim.getBY();
		}
	}

	// translating camera
	public void translateCamera(double dx, double dy) {
		// stop tracking objects
		selected = null;
		barySelected = false;
		// move camera
		if (heldKeys.contains(KeyEvent.VK_SHIFT)) { // shift mode
			dx *= 4; // magnifies shift
			dy *= 4;
		}
		cameraX += dx;
		cameraY += dy;
	}
	
	// updating bounds
	public void updateBaryBounds() {
		int[] screen_coords = convert(sim.getBX(), sim.getBY());
		int screenX = screen_coords[0] - barySize;
		int screenY = screen_coords[1] - barySize;
		baryBounds = new Rectangle(screenX, screenY, barySize * 2, barySize * 2);
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

	// -----------------------------------------------------------------------------------------------------------------------------
	// drawing text (regular and/or debug)
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
			g.drawString("v.3.6.1", 145, 20); // version
			g.drawString("by Jason Kim", 10, 35); // by me
			g.setFont(new Font("Dialog", Font.PLAIN, 12)); // regular font
			if (help) {
				// starting position is moved because of title
				text_start_pos = 60;

				// regular text
				menu.add("Esc - Exit all");
				menu.add("F3 - Debug Mode");
				menu.add("F2 - Screenshot Mode");
				menu.add("F1 - Does \"nothing\"");
				menu.add("");
				menu.add("Current Location: (" + round(cameraX, 1) + ", " + round(cameraY, 1) + ")");
				menu.add("WASD - Move camera");
				menu.add("Zoom - Shift/Control or Scroll");
				menu.add(screenScale + " meters per pixel");
				menu.add("");
				if (paused)
					menu.add("Paused");
				menu.add("Space - Toggle Pause");
				menu.add((stepTime) + " seconds per second");
				menu.add("Period - Increase Time Speed");
				menu.add("Comma - Decrease Time Speed");
				if (paused)
					menu.add("Left Arrow - Rewind");
				menu.add("");
				if (barySelected)
					menu.add("Barycenter Selected");
				if (barycenter)
					menu.add("B - Hide Barycenter");
				else
					menu.add("B - Show Barycenter");
				menu.add("");
				menu.add("Physics Mode: " + physicsMode);
				// menu.add("Alt+0: Multithreading (Experimental)");
				menu.add("Alt+1: Precise");
				menu.add("Alt+2: Fast");
				menu.add("");
				menu.add("Scenario: " + sim.getScenario());
				menu.add("1: Lone Planet");
				menu.add("2: Binary System");
				menu.add("3: Three Bodies");
				menu.add("4: Random");
				menu.add("5: Earth and Moon(s)");
				menu.add("6: Primordial");
				menu.add("7: Trojans");
				menu.add("8: Solar System");
				menu.add("9: Sagittarius A");
				// menu.add("0: Double Saturn Encounter");
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
			} else
				g.drawString("H - Help", 10, 60);
		}

		// debugging text
		else {
			menu.add("Debug Mode On");
			menu.add("");
			menu.add("Window: " + this);
			menu.add("Simulation: " + sim);
			menu.add("sim_i: " + sim_i);
			menu.add("");
			menu.add("Screen: " + screenWidth + " x " + screenHeight);
			menu.add("Screen Scale: " + screenScale + " m/pixel");
			menu.add("");
			menu.add("Target FPS: " + targetFPS);
			menu.add("Total Time: " + totalTime);
			menu.add("Total Frames: " + totalFrames);
			menu.add("Current FPS: " + round(currentFPS, 1));
			menu.add("");
			// menu.add("Frames: " + frames.size());
			// menu.add("Frame: " + frame);
			// menu.add("");
			menu.add("Paused: " + paused);
			menu.add("");
			menu.add("Physics Mode: " + physicsMode);
			menu.add("");
			/*
			menu.add("Multithreading:");
			menu.add("Processors: " + (n + 1));
			menu.add("Bodies/Processor: " + num);
			menu.add("Remainder: " + rem);
			if (threads != null)
				menu.add("Threads: " + threads.size());
			menu.add("");
			*/
			menu.add("Step Time: " + stepTime + " seconds per second");
			menu.add("Time Step: " + round(timeStep, 5) + " seconds per frame");
			menu.add("Time Multiplier: " + timeMult + "x");
			menu.add("");
			menu.add("Collisions: " + sim.isCollisionEnabled());
			menu.add("Tidal Forces: " + sim.isTidalForcesEnabled());
			menu.add("Minimum Mass: " + sim.getMinMass());
			menu.add("");
			menu.add("Softening Parameter (S): " + sim.getS());
			menu.add("");
			menu.add("# of Bodies: " + sim.getBodies().size());
			menu.add("");
			menu.add("Barycenter: " + barycenter);
			menu.add("barySize: " + barySize);
			menu.add("barySelected: " + barySelected);
			menu.add("bx: " + sim.getBX());
			menu.add("by: " + sim.getBY());
			menu.add("");
			menu.add("Meters/Pixel: " + screenScale);
			menu.add("Current Location: (" + round(cameraX, 1) + ", " + round(cameraY, 1) + ")");
			menu.add("");
			menu.add("Held Keys: " + heldKeys.toString());
			menu.add("shift: " + heldKeys.contains(KeyEvent.VK_SHIFT));
			menu.add("ctrl: " + heldKeys.contains(KeyEvent.VK_CONTROL));
			menu.add("alt: " + heldKeys.contains(KeyEvent.VK_ALT));
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
			ArrayList<Body> bodies = sim.getBodies();
			sortedBodies = new ArrayList<Body>();
			for (Body body : bodies)
				sortedBodies.add(body);
			for (int i = 1; i < sortedBodies.size(); i++) {
				Body body = sortedBodies.get(i);
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
				Body body = sortedBodies.get(i);
				bodyStrings[i] = body.toString() + ": " + body.getMass() + " kg, " + body.getRadius() + " m";
				bodyLengths[i] = g.getFontMetrics().stringWidth(bodyStrings[i]);
			}
			// body locator text
			menu.add("");
			menu.add("Body Locator:");
			for (int i = 0; i < bodyStrings.length; i++) {
				Body body = sortedBodies.get(i);
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
	
	//-----------------------------------------------------------------------------------------------------------------------------
	// access/modify methods
	
	boolean debugState() {return debug;}
	
	// screen
	int getScreenWidth() {return screenWidth;}
	int getScreenHeight() {return screenHeight;}
	void setScreenScale(double newScreenScale) {screenScale = newScreenScale;}
	double getScreenScale() {return screenScale;}
	
	// camera
	void setSelected(Body newSelected) {selected = newSelected;}
	Body getSelected() {return selected;}
	
	// simulation
	void setPhysicsMode(int newPhysicsMode) {physicsMode = newPhysicsMode;}
	
	//-----------------------------------------------------------------------------------------------------------------------------
	// keyboard and mouse inputs

	// clears held keys
	public void clear_held_keys() {
		heldKeys.clear();
	}
	
	// keyboard inputs
	class KeyHandler extends KeyAdapter {

		public void keyPressed(KeyEvent e) {
			int keyCode = e.getKeyCode();

			// general key input
			if (!heldKeys.contains(keyCode))
				heldKeys.add(keyCode);

			// time
			if (e.getKeyCode() == KeyEvent.VK_PERIOD) {
				scaleTime(2);
			} else if (e.getKeyCode() == KeyEvent.VK_COMMA) {
				scaleTime(0.5);
			}
			
			// TOFIX: rewinding
			/*
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
			*/

			// TOFIX: trails
			/*
			else if (keyCode == KeyEvent.VK_Z) {
				trailLen++;
			} else if (keyCode == KeyEvent.VK_X) {
				trailLen--;
				if (trailLen < -1)
					trailLen = -1;
			} else if (selected != null && keyCode == KeyEvent.VK_BACK_SLASH) {
				relative = !relative;
			}
			*/
			
			// TOREMOVE: softening parameter
			if (debug)
				if (e.getKeyCode() == KeyEvent.VK_O) {
					if (sim.getS() == 0.0)
						sim.setS(1.0);
					else
						sim.setS(sim.getS() * 10.0);
				} else if (e.getKeyCode() == KeyEvent.VK_L) {
					if (sim.getS() == 1.0)
						sim.setS(0.0);
					else
						sim.setS(sim.getS() / 10.0);
				}

		}

		public void keyReleased(KeyEvent e) {
			int keyCode = e.getKeyCode();

			// TOFIX: alt+tab fix
			/*
			if (keyCode == KeyEvent.VK_TAB) {
				int i = heldKeys.indexOf(KeyEvent.VK_ALT);
				if (i != -1)
					heldKeys.remove(i);
			}
			*/

			// removing held key
			int i = heldKeys.indexOf(keyCode);
			if (i != -1)
				heldKeys.remove(i);

			// ---------------------------------------------------------------------------
			// regardless of alt

			// sim
			if (keyCode == KeyEvent.VK_OPEN_BRACKET) { // switching sims
				requestSim(-1);
			} else if (keyCode == KeyEvent.VK_CLOSE_BRACKET) {
				requestSim(1);
			} else if (keyCode == KeyEvent.VK_SPACE) { // pause
				paused = !paused;
				// TOFIX: rewinding stuff
				/*
				if (paused == false) // if the user rewinded and unpaused, future frames are deleted
					while (frame < frames.size() - 1)
						frames.remove(frame + 1);
				*/
			} else if (keyCode == KeyEvent.VK_R) { // reset
				reset();
			} else if (keyCode == KeyEvent.VK_ESCAPE) { // exit
				System.exit(0);
			} else if (keyCode == KeyEvent.VK_B) // barycenter
				barycenter = !barycenter;

			// ui
			else if (text && !debug && keyCode == KeyEvent.VK_H) { // help view
				help = !help;
			}
			
			// TOFIX
			/*
			else if (keyCode == KeyEvent.VK_F1) { // does nothing
				nothing++;
				if (nothing == nothing_limit)
					doNothing();
			}
			*/
			
			else if (keyCode == KeyEvent.VK_F2) { // ui view
				text = !text;
			} else if (keyCode == KeyEvent.VK_F3) { // debug
				debug = !debug;
			}

			// ---------------------------------------------------------------------------
			// alt mode
			else if (heldKeys.contains(KeyEvent.VK_ALT)) {
				// physics
				if (keyCode == KeyEvent.VK_0) // physics mode 0
					physicsMode = 0;
				else if (keyCode == KeyEvent.VK_1) // physics mode 1
					physicsMode = 1;
				else if (keyCode == KeyEvent.VK_2) // physics mode 2
					physicsMode = 2;
			}
			
			// ---------------------------------------------------------------------------
			// not alt mode
			else {
				// scenarios
				if (48 <= keyCode && keyCode <= 57) {
					sim.setScenario(keyCode - 48);
					reset();
				}
			}
			
			// ---------------------------------------------------------------------------
			// ctrl mode
			if (heldKeys.contains(KeyEvent.VK_CONTROL)) {
				if (keyCode == KeyEvent.VK_N)
					manager.newWindow("2D N-Body Simulation");
			}
		}
	}

	// mouse inputs
	class MouseHandler extends MouseAdapter {

		public void mouseClicked(MouseEvent e) {

			// converting coords
			click = e.getLocationOnScreen();

			// selecting bodies
			selected = null;
			barySelected = false;
			ArrayList<Body> bodies = sim.getBodies();
			for (Body body : bodies) {
				// calculating bounds
				int[] screen_coords = convert(body.getSX(), body.getSY());
				int boundsX = screen_coords[0];
				int boundsY = screen_coords[1];
				if (debug) {
					System.out.println("Debug: boundsX: " + boundsX);
					System.out.println("Debug: boundsY: " + boundsY + "\n");
				}
				int boundsR = (int) (body.getRadius() / screenScale);
				Rectangle bounds = new Rectangle(boundsX - boundsR, boundsY - boundsR, boundsR * 2, boundsR * 2);
				// checking
				if (bounds.contains(click)) {
					// splitting bodies (debug only)
					if (debug == true && e.getButton() == MouseEvent.BUTTON3) { // right click
						sim.splitBody(body);
						break;
					} else {
						selected = body;
						break;
					}
				}
			}
			// selecting barycenter
			if (selected == null && baryBounds.contains(click))
				barySelected = true;

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
			if (e.getWheelRotation() > 0)
				scaleScreen(1.5 * zoom_speed);
			// zoom out
			else if (e.getWheelRotation() < 0)
				scaleScreen(1 / (1.5 * zoom_speed));
		}

	}
	
}