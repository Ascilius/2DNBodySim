import java.awt.Color;
import java.awt.Rectangle;

public class Body2D {

	private final double G = 6.67408 * Math.pow(10, -11);

	private String name = ""; // body name

	// physical properties
	private double mass, radius, diameter;
	private double sx, sy; // m
	private double vx, vy; // m/s
	private double ax = 0.0; // m/s^2
	private double ay = 0.0;

	// color
	private Color c = Color.WHITE; // default color
	private final double white_shift = 0.75; // prevents colors from blending into black

	// simulation
	private boolean locked;
	boolean toSplit = false;
	// private Rectangle bounds;
	private Trail trail = new Trail();

	// TOFIX: constructors

	// bodies without name or color
	public Body2D(double mass, double radius, double sx, double sy, double vx, double vy) {
		set_pp(mass, radius, sx, sy, vx, vy);
	}

	// bodies with name without color
	public Body2D(String name, double mass, double radius, double sx, double sy, double vx, double vy) {
		this.name = name;
		set_pp(mass, radius, sx, sy, vx, vy);
	}

	// bodies without name with color
	public Body2D(Color color, double mass, double radius, double sx, double sy, double vx, double vy) {
		this.c = color;
		set_pp(mass, radius, sx, sy, vx, vy);
	}

	// bodies with name and color
	public Body2D(String name, Color color, double mass, double radius, double sx, double sy, double vx, double vy) {
		this.name = name;
		this.c = color;
		set_pp(mass, radius, sx, sy, vx, vy);
	}

	// setting initial physical properties
	public void set_pp(double mass, double radius, double sx, double sy, double vx, double vy) {
		this.mass = mass;
		this.radius = radius;
		this.diameter = 2 * this.radius;

		this.sx = sx;
		this.sy = sy;
		this.vx = vx;
		this.vy = vy;
	}

	public void move(Body2D otherBody, double timeStep) {
		// getting distances
		double x = otherBody.getSX() - this.sx;
		double y = otherBody.getSY() - this.sy;
		double z = Math.sqrt(x * x + y * y);

		// calculating acceleration
		double F = G * ((mass * otherBody.getMass()) / Math.pow(z, 2));
		double a = F / mass;

		// vectors
		ax = a * x / z;
		ay = a * y / z;
		vx += ax * timeStep;
		vy += ay * timeStep;
	}

	public void actuallyMove(double timeStep) {
		// step
		sx += vx * timeStep;
		sy += vy * timeStep;
	}

	// add a point to the trail (TODO)
	public void addTrail(int trailLen) {

	}

	public Body2D collision(Body2D otherBody) {

		// getting distances
		double x = otherBody.getSX() - this.sx;
		double y = otherBody.getSY() - this.sy;
		double z = Math.sqrt(x * x + y * y);
		double r = this.radius + otherBody.getRadius();

		// confirmed collision
		if (z < r) {

			// black holes
			Color newC = null;
			if (this.c == Color.BLACK || otherBody.getColor() == Color.BLACK) {
				newC = Color.BLACK;
			}
			// blending color
			else {
				int r1 = c.getRed();
				int g1 = c.getGreen();
				int b1 = c.getBlue();
				double m1 = mass;
				int r2 = otherBody.getColor().getRed();
				int g2 = otherBody.getColor().getGreen();
				int b2 = otherBody.getColor().getBlue();
				double m2 = otherBody.getMass();
				double newR = (r1 * m1 + r2 * m2) / (m1 + m2);
				double newG = (g1 * m1 + g2 * m2) / (m1 + m2);
				double newB = (b1 * m1 + b2 * m2) / (m1 + m2);
				// white shifting
				if (newR <= 255 - white_shift)
					newR += white_shift;
				if (newG <= 255 - white_shift)
					newG += white_shift;
				if (newB <= 255 - white_shift)
					newB += white_shift;
				newC = new Color((int) newR, (int) newG, (int) newB);
			}

			// mass/volume conservation
			double newVolume = (4 / 3.0) * Math.PI * Math.pow(this.radius, 3) + (4 / 3.0) * Math.PI * Math.pow(otherBody.getRadius(), 3);
			double newRadius = Math.pow((3 / 4.0) * newVolume / Math.PI, 1 / 3.0);
			double newMass = mass + otherBody.getMass();

			// centroid
			double newSX = (this.sx * this.mass + otherBody.getSX() * otherBody.getMass()) / newMass;
			double newSY = (this.sy * this.mass + otherBody.getSY() * otherBody.getMass()) / newMass;

			// momentum conservation
			double thisPX = mass * vx;
			double otherPX = otherBody.getMass() * otherBody.getVX();
			double thisPY = mass * vy;
			double otherPY = otherBody.getMass() * otherBody.getVY();
			double combinedMass = mass + otherBody.getMass();
			double newVX = (thisPX + otherPX) / combinedMass;
			double newVY = (thisPY + otherPY) / combinedMass;

			// creating new body
			String newName = "";
			if (this.mass > otherBody.getMass())
				newName = this.name;
			else
				newName = otherBody.getName();
			Body2D newBody = new Body2D(newName, newC, newMass, newRadius, newSX, newSY, newVX, newVY);

			// debugging
			// System.out.println("Debug: " + this.toString() + " colliding with " + otherBody.toString() + " to create " + newBody.toString());
			return newBody;

		}

		// no collision
		else {
			return null;
		}

	}

	public void setColor(Color c) {
		this.c = c;
	}

	public String getName() {
		if (name.equals(""))
			return "" + this; // no name; return obj address
		else
			return name;
	}

	public Color getColor() {
		return c;
	}

	public double getRadius() {
		return radius;
	}

	public double getDiameter() {
		return diameter;
	}

	public double getMass() {
		return mass;
	}

	public double getSX() {
		return sx;
	}

	public double getSY() {
		return sy;
	}

	public double getVX() {
		return vx;
	}

	public double getVY() {
		return vy;
	}

	public double getAX() {
		return ax;
	}

	public double getAY() {
		return ay;
	}

	/*
	public Rectangle getBounds() {
		return bounds;
	}
	
	public Trail getTrail() {
		return trail;
	}
	*/

	public Body2D copy() {
		return new Body2D(name, c, mass, radius, sx, sy, vx, vy);
	}

}
