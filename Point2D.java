import java.awt.Point;

public class Point2D {

	private final double x, y;

	public Point2D(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public Point2D(Point point) {
		this.x = point.getX();
		this.y = point.getY();
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

}
