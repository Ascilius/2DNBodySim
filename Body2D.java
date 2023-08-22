import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

public class Body2D {

	final double G = 6.67408 * Math.pow(10, -11);

	Color c = Color.WHITE;
	double mass, radius;

	double sx, sy; // m
	double vx, vy; // m/s
	double ax, ay; // m/s^2

	boolean locked;
	boolean toSplit = false;

	ArrayList<double[]> trail = new ArrayList<double[]>();

	public Body2D(double mass, double radius, double sx, double sy, double vx, double vy) {
		this.mass = mass;
		this.radius = radius;
		this.sx = sx;
		this.sy = sy;
		this.vx = vx;
		this.vy = vy;
		this.ax = 0;
		this.ay = 0;
	}

	public Body2D(Color c, double mass, double radius, double sx, double sy, double vx, double vy) {
		this.c = c;
		this.mass = mass;
		this.radius = radius;
		this.sx = sx;
		this.sy = sy;
		this.vx = vx;
		this.vy = vy;
		this.ax = 0;
		this.ay = 0;
		// this.locked = locked;
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

	public void addTrail(int trailLen) {
		double[] newPoint = { sx, sy };
		trail.add(newPoint);
		while (trailLen != -1 && trail.size() > trailLen) {
			trail.remove(0);
		}
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
				int newR = (this.c.getRed() + otherBody.getColor().getRed()) / 2;
				int newG = (this.c.getGreen() + otherBody.getColor().getGreen()) / 2;
				int newB = (this.c.getBlue() + otherBody.getColor().getBlue()) / 2;
				newC = new Color(newR, newG, newB);
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
			Body2D newBody = new Body2D(newC, newMass, newRadius, newSX, newSY, newVX, newVY);
			if (this.mass > otherBody.getMass())
				newBody.trail = this.trail;
			else
				newBody.trail = otherBody.trail;

			// debugging
			// System.out.println("Debug: " + this.toString() + " colliding with " + otherBody.toString() + " to create " + newBody.toString());
			return newBody;

		}

		// no collision
		else {
			return null;
		}

	}

	public Color getColor() {
		return c;
	}

	public double getRadius() {
		return radius;
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

	public ArrayList<double[]> getTrail() {
		return trail;
	}

	// for multithreading
	public Body2D clone() {
		return new Body2D(c, mass, radius, sx, sy, vx, vy);
	}

}
